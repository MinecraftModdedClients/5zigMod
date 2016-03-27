package org.h2.server;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import org.h2.command.Command;
import org.h2.engine.ConnectionInfo;
import org.h2.engine.Engine;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.expression.Parameter;
import org.h2.expression.ParameterInterface;
import org.h2.expression.ParameterRemote;
import org.h2.jdbc.JdbcSQLException;
import org.h2.message.DbException;
import org.h2.result.ResultColumn;
import org.h2.result.ResultInterface;
import org.h2.store.DataHandler;
import org.h2.store.LobStorageInterface;
import org.h2.util.IOUtils;
import org.h2.util.SmallLRUCache;
import org.h2.util.SmallMap;
import org.h2.util.StringUtils;
import org.h2.value.Transfer;
import org.h2.value.Value;
import org.h2.value.ValueLobDb;

public class TcpServerThread
  implements Runnable
{
  protected final Transfer transfer;
  private final TcpServer server;
  private Session session;
  private boolean stop;
  private Thread thread;
  private Command commit;
  private final SmallMap cache = new SmallMap(SysProperties.SERVER_CACHED_OBJECTS);
  private final SmallLRUCache<Long, CachedInputStream> lobs = SmallLRUCache.newInstance(Math.max(SysProperties.SERVER_CACHED_OBJECTS, SysProperties.SERVER_RESULT_SET_FETCH_SIZE * 5));
  private final int threadId;
  private int clientVersion;
  private String sessionId;
  
  TcpServerThread(Socket paramSocket, TcpServer paramTcpServer, int paramInt)
  {
    this.server = paramTcpServer;
    this.threadId = paramInt;
    this.transfer = new Transfer(null);
    this.transfer.setSocket(paramSocket);
  }
  
  private void trace(String paramString)
  {
    this.server.trace(this + " " + paramString);
  }
  
  public void run()
  {
    try
    {
      this.transfer.init();
      trace("Connect");
      try
      {
        if (!this.server.allow(this.transfer.getSocket())) {
          throw DbException.get(90117);
        }
        int i = this.transfer.readInt();
        if (i < 6) {
          throw DbException.get(90047, new String[] { "" + this.clientVersion, "6" });
        }
        if (i > 15) {
          throw DbException.get(90047, new String[] { "" + this.clientVersion, "15" });
        }
        int j = this.transfer.readInt();
        if (j >= 15) {
          this.clientVersion = 15;
        } else {
          this.clientVersion = i;
        }
        this.transfer.setVersion(this.clientVersion);
        String str1 = this.transfer.readString();
        String str2 = this.transfer.readString();
        if ((str1 == null) && (str2 == null))
        {
          str3 = this.transfer.readString();
          int k = this.transfer.readInt();
          this.stop = true;
          if (k == 13)
          {
            m = this.transfer.readInt();
            this.server.cancelStatement(str3, m);
          }
          else if (k == 14)
          {
            str1 = this.server.checkKeyAndGetDatabaseName(str3);
            if (!str3.equals(str1)) {
              this.transfer.writeInt(1);
            } else {
              this.transfer.writeInt(0);
            }
          }
        }
        String str3 = this.server.getBaseDir();
        if (str3 == null) {
          str3 = SysProperties.getBaseDir();
        }
        str1 = this.server.checkKeyAndGetDatabaseName(str1);
        ConnectionInfo localConnectionInfo = new ConnectionInfo(str1);
        localConnectionInfo.setOriginalURL(str2);
        localConnectionInfo.setUserName(this.transfer.readString());
        localConnectionInfo.setUserPasswordHash(this.transfer.readBytes());
        localConnectionInfo.setFilePasswordHash(this.transfer.readBytes());
        int m = this.transfer.readInt();
        for (int n = 0; n < m; n++) {
          localConnectionInfo.setProperty(this.transfer.readString(), this.transfer.readString());
        }
        if (str3 != null) {
          localConnectionInfo.setBaseDir(str3);
        }
        if (this.server.getIfExists()) {
          localConnectionInfo.setProperty("IFEXISTS", "TRUE");
        }
        this.transfer.writeInt(1);
        this.transfer.writeInt(this.clientVersion);
        this.transfer.flush();
        if ((this.clientVersion >= 13) && 
          (localConnectionInfo.getFilePasswordHash() != null)) {
          localConnectionInfo.setFileEncryptionKey(this.transfer.readBytes());
        }
        this.session = Engine.getInstance().createSession(localConnectionInfo);
        this.transfer.setSession(this.session);
        this.server.addConnection(this.threadId, str2, localConnectionInfo.getUserName());
        trace("Connected");
      }
      catch (Throwable localThrowable1)
      {
        sendError(localThrowable1);
        this.stop = true;
      }
      while (!this.stop) {
        try
        {
          process();
        }
        catch (Throwable localThrowable2)
        {
          sendError(localThrowable2);
        }
      }
      trace("Disconnect");
    }
    catch (Throwable localThrowable3)
    {
      this.server.traceError(localThrowable3);
    }
    finally
    {
      close();
    }
  }
  
  private void closeSession()
  {
    if (this.session != null)
    {
      Object localObject1 = null;
      try
      {
        Command localCommand = this.session.prepareLocal("ROLLBACK");
        localCommand.executeUpdate();
      }
      catch (RuntimeException localRuntimeException1)
      {
        localObject1 = localRuntimeException1;
        this.server.traceError(localRuntimeException1);
      }
      catch (Exception localException1)
      {
        this.server.traceError(localException1);
      }
      try
      {
        this.session.close();
        this.server.removeConnection(this.threadId);
      }
      catch (RuntimeException localRuntimeException2)
      {
        if (localObject1 == null)
        {
          localObject1 = localRuntimeException2;
          this.server.traceError(localRuntimeException2);
        }
      }
      catch (Exception localException2)
      {
        this.server.traceError(localException2);
      }
      finally
      {
        this.session = null;
      }
      if (localObject1 != null) {
        throw ((Throwable)localObject1);
      }
    }
  }
  
  void close()
  {
    try
    {
      this.stop = true;
      closeSession();
    }
    catch (Exception localException)
    {
      this.server.traceError(localException);
    }
    finally
    {
      this.transfer.close();
      trace("Close");
      this.server.remove(this);
    }
  }
  
  private void sendError(Throwable paramThrowable)
  {
    try
    {
      SQLException localSQLException = DbException.convert(paramThrowable).getSQLException();
      StringWriter localStringWriter = new StringWriter();
      localSQLException.printStackTrace(new PrintWriter(localStringWriter));
      String str1 = localStringWriter.toString();
      String str2;
      String str3;
      if ((localSQLException instanceof JdbcSQLException))
      {
        JdbcSQLException localJdbcSQLException = (JdbcSQLException)localSQLException;
        str2 = localJdbcSQLException.getOriginalMessage();
        str3 = localJdbcSQLException.getSQL();
      }
      else
      {
        str2 = localSQLException.getMessage();
        str3 = null;
      }
      this.transfer.writeInt(0).writeString(localSQLException.getSQLState()).writeString(str2).writeString(str3).writeInt(localSQLException.getErrorCode()).writeString(str1).flush();
    }
    catch (Exception localException)
    {
      if (!this.transfer.isClosed()) {
        this.server.traceError(localException);
      }
      this.stop = true;
    }
  }
  
  private void setParameters(Command paramCommand)
    throws IOException
  {
    int i = this.transfer.readInt();
    ArrayList localArrayList = paramCommand.getParameters();
    for (int j = 0; j < i; j++)
    {
      Parameter localParameter = (Parameter)localArrayList.get(j);
      localParameter.setValue(this.transfer.readValue());
    }
  }
  
  private void process()
    throws IOException
  {
    int i = this.transfer.readInt();
    int j;
    Object localObject2;
    int i6;
    Object localObject3;
    int k;
    int i2;
    int i3;
    int i8;
    Command localCommand1;
    int i5;
    Object localObject1;
    ResultInterface localResultInterface;
    switch (i)
    {
    case 0: 
    case 11: 
      j = this.transfer.readInt();
      String str = this.transfer.readString();
      int i1 = this.session.getModificationId();
      localObject2 = this.session.prepareLocal(str);
      boolean bool2 = ((Command)localObject2).isReadOnly();
      this.cache.addObject(j, localObject2);
      i6 = ((Command)localObject2).isQuery();
      localObject3 = ((Command)localObject2).getParameters();
      this.transfer.writeInt(getState(i1)).writeBoolean(i6).writeBoolean(bool2).writeInt(((ArrayList)localObject3).size());
      if (i == 11) {
        for (ParameterInterface localParameterInterface : (ArrayList)localObject3) {
          ParameterRemote.writeMetaData(this.transfer, localParameterInterface);
        }
      }
      this.transfer.flush();
      break;
    case 1: 
      this.stop = true;
      closeSession();
      this.transfer.writeInt(1).flush();
      close();
      break;
    case 8: 
      if (this.commit == null) {
        this.commit = this.session.prepareLocal("COMMIT");
      }
      j = this.session.getModificationId();
      this.commit.executeUpdate();
      this.transfer.writeInt(getState(j)).flush();
      break;
    case 10: 
      j = this.transfer.readInt();
      k = this.transfer.readInt();
      Command localCommand2 = (Command)this.cache.getObject(j, false);
      localObject2 = localCommand2.getMetaData();
      this.cache.addObject(k, localObject2);
      int i4 = ((ResultInterface)localObject2).getVisibleColumnCount();
      this.transfer.writeInt(1).writeInt(i4).writeInt(0);
      for (i6 = 0; i6 < i4; i6++) {
        ResultColumn.writeColumn(this.transfer, (ResultInterface)localObject2, i6);
      }
      this.transfer.flush();
      break;
    case 2: 
      j = this.transfer.readInt();
      k = this.transfer.readInt();
      i2 = this.transfer.readInt();
      i3 = this.transfer.readInt();
      Command localCommand3 = (Command)this.cache.getObject(j, false);
      setParameters(localCommand3);
      int i7 = this.session.getModificationId();
      synchronized (this.session)
      {
        localObject3 = localCommand3.executeQuery(i2, false);
      }
      this.cache.addObject(k, localObject3);
      i8 = ((ResultInterface)localObject3).getVisibleColumnCount();
      int i9 = getState(i7);
      this.transfer.writeInt(i9).writeInt(i8);
      int i10 = ((ResultInterface)localObject3).getRowCount();
      this.transfer.writeInt(i10);
      for (int i11 = 0; i11 < i8; i11++) {
        ResultColumn.writeColumn(this.transfer, (ResultInterface)localObject3, i11);
      }
      i11 = Math.min(i10, i3);
      for (int i12 = 0; i12 < i11; i12++) {
        sendRow((ResultInterface)localObject3);
      }
      this.transfer.flush();
      break;
    case 3: 
      j = this.transfer.readInt();
      localCommand1 = (Command)this.cache.getObject(j, false);
      setParameters(localCommand1);
      i2 = this.session.getModificationId();
      synchronized (this.session)
      {
        i3 = localCommand1.executeUpdate();
      }
      if (this.session.isClosed()) {
        i5 = 2;
      } else {
        i5 = getState(i2);
      }
      this.transfer.writeInt(i5).writeInt(i3).writeBoolean(this.session.getAutoCommit());
      
      this.transfer.flush();
      break;
    case 4: 
      j = this.transfer.readInt();
      localCommand1 = (Command)this.cache.getObject(j, true);
      if (localCommand1 != null)
      {
        localCommand1.close();
        this.cache.freeObject(j);
      }
      break;
    case 5: 
      j = this.transfer.readInt();
      int m = this.transfer.readInt();
      localObject1 = (ResultInterface)this.cache.getObject(j, false);
      this.transfer.writeInt(1);
      for (i3 = 0; i3 < m; i3++) {
        sendRow((ResultInterface)localObject1);
      }
      this.transfer.flush();
      break;
    case 6: 
      j = this.transfer.readInt();
      localResultInterface = (ResultInterface)this.cache.getObject(j, false);
      localResultInterface.reset();
      break;
    case 7: 
      j = this.transfer.readInt();
      localResultInterface = (ResultInterface)this.cache.getObject(j, true);
      if (localResultInterface != null)
      {
        localResultInterface.close();
        this.cache.freeObject(j);
      }
      break;
    case 9: 
      j = this.transfer.readInt();
      int n = this.transfer.readInt();
      localObject1 = this.cache.getObject(j, false);
      this.cache.freeObject(j);
      this.cache.addObject(n, localObject1);
      break;
    case 12: 
      this.sessionId = this.transfer.readString();
      this.transfer.writeInt(1);
      this.transfer.writeBoolean(this.session.getAutoCommit());
      this.transfer.flush();
      break;
    case 15: 
      boolean bool1 = this.transfer.readBoolean();
      this.session.setAutoCommit(bool1);
      this.transfer.writeInt(1).flush();
      break;
    case 16: 
      this.transfer.writeInt(1).writeInt(this.session.hasPendingTransaction() ? 1 : 0).flush();
      
      break;
    case 17: 
      long l1 = this.transfer.readLong();
      CachedInputStream localCachedInputStream;
      if (this.clientVersion >= 11)
      {
        if (this.clientVersion >= 12)
        {
          localObject1 = this.transfer.readBytes();
          i5 = 1;
        }
        else
        {
          localObject1 = null;
          i5 = 0;
        }
        localCachedInputStream = (CachedInputStream)this.lobs.get(Long.valueOf(l1));
        if ((localCachedInputStream == null) && (i5 != 0))
        {
          localCachedInputStream = new CachedInputStream(null);
          this.lobs.put(Long.valueOf(l1), localCachedInputStream);
        }
      }
      else
      {
        i5 = 0;
        localObject1 = null;
        localCachedInputStream = (CachedInputStream)this.lobs.get(Long.valueOf(l1));
      }
      long l2 = this.transfer.readLong();
      i8 = this.transfer.readInt();
      if (i5 != 0) {
        this.transfer.verifyLobMac((byte[])localObject1, l1);
      }
      if (localCachedInputStream == null) {
        throw DbException.get(90007);
      }
      if (localCachedInputStream.getPos() != l2)
      {
        localObject4 = this.session.getDataHandler().getLobStorage();
        
        ValueLobDb localValueLobDb = ValueLobDb.create(15, null, -1, l1, (byte[])localObject1, -1L);
        InputStream localInputStream = ((LobStorageInterface)localObject4).getInputStream(localValueLobDb, (byte[])localObject1, -1L);
        localCachedInputStream = new CachedInputStream(localInputStream);
        this.lobs.put(Long.valueOf(l1), localCachedInputStream);
        localInputStream.skip(l2);
      }
      i8 = Math.min(65536, i8);
      Object localObject4 = new byte[i8];
      i8 = IOUtils.readFully(localCachedInputStream, (byte[])localObject4, i8);
      this.transfer.writeInt(1);
      this.transfer.writeInt(i8);
      this.transfer.writeBytes((byte[])localObject4, 0, i8);
      this.transfer.flush();
      break;
    case 13: 
    case 14: 
    default: 
      trace("Unknown operation: " + i);
      closeSession();
      close();
    }
  }
  
  private int getState(int paramInt)
  {
    if (this.session.getModificationId() == paramInt) {
      return 1;
    }
    return 3;
  }
  
  private void sendRow(ResultInterface paramResultInterface)
    throws IOException
  {
    if (paramResultInterface.next())
    {
      this.transfer.writeBoolean(true);
      Value[] arrayOfValue = paramResultInterface.currentRow();
      for (int i = 0; i < paramResultInterface.getVisibleColumnCount(); i++) {
        if (this.clientVersion >= 12) {
          this.transfer.writeValue(arrayOfValue[i]);
        } else {
          writeValue(arrayOfValue[i]);
        }
      }
    }
    else
    {
      this.transfer.writeBoolean(false);
    }
  }
  
  private void writeValue(Value paramValue)
    throws IOException
  {
    if (((paramValue.getType() == 16) || (paramValue.getType() == 15)) && 
      ((paramValue instanceof ValueLobDb)))
    {
      ValueLobDb localValueLobDb = (ValueLobDb)paramValue;
      if (localValueLobDb.isStored())
      {
        long l = localValueLobDb.getLobId();
        this.lobs.put(Long.valueOf(l), new CachedInputStream(null));
      }
    }
    this.transfer.writeValue(paramValue);
  }
  
  void setThread(Thread paramThread)
  {
    this.thread = paramThread;
  }
  
  Thread getThread()
  {
    return this.thread;
  }
  
  void cancelStatement(String paramString, int paramInt)
  {
    if (StringUtils.equals(paramString, this.sessionId))
    {
      Command localCommand = (Command)this.cache.getObject(paramInt, false);
      localCommand.cancel();
    }
  }
  
  static class CachedInputStream
    extends FilterInputStream
  {
    private static final ByteArrayInputStream DUMMY = new ByteArrayInputStream(new byte[0]);
    private long pos;
    
    CachedInputStream(InputStream paramInputStream)
    {
      super();
      if (paramInputStream == null) {
        this.pos = -1L;
      }
    }
    
    public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
      throws IOException
    {
      paramInt2 = super.read(paramArrayOfByte, paramInt1, paramInt2);
      if (paramInt2 > 0) {
        this.pos += paramInt2;
      }
      return paramInt2;
    }
    
    public int read()
      throws IOException
    {
      int i = this.in.read();
      if (i >= 0) {
        this.pos += 1L;
      }
      return i;
    }
    
    public long skip(long paramLong)
      throws IOException
    {
      paramLong = super.skip(paramLong);
      if (paramLong > 0L) {
        this.pos += paramLong;
      }
      return paramLong;
    }
    
    public long getPos()
    {
      return this.pos;
    }
  }
}
