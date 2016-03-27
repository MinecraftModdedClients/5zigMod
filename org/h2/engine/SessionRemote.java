package org.h2.engine;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import org.h2.api.DatabaseEventListener;
import org.h2.api.JavaObjectSerializer;
import org.h2.command.CommandInterface;
import org.h2.command.CommandRemote;
import org.h2.jdbc.JdbcSQLException;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.message.TraceSystem;
import org.h2.result.ResultInterface;
import org.h2.store.DataHandler;
import org.h2.store.FileStore;
import org.h2.store.LobStorageFrontend;
import org.h2.store.LobStorageInterface;
import org.h2.store.fs.FileUtils;
import org.h2.util.JdbcUtils;
import org.h2.util.MathUtils;
import org.h2.util.NetUtils;
import org.h2.util.New;
import org.h2.util.SmallLRUCache;
import org.h2.util.StringUtils;
import org.h2.util.TempFileDeleter;
import org.h2.value.Transfer;
import org.h2.value.Value;

public class SessionRemote
  extends SessionWithState
  implements DataHandler
{
  public static final int SESSION_PREPARE = 0;
  public static final int SESSION_CLOSE = 1;
  public static final int COMMAND_EXECUTE_QUERY = 2;
  public static final int COMMAND_EXECUTE_UPDATE = 3;
  public static final int COMMAND_CLOSE = 4;
  public static final int RESULT_FETCH_ROWS = 5;
  public static final int RESULT_RESET = 6;
  public static final int RESULT_CLOSE = 7;
  public static final int COMMAND_COMMIT = 8;
  public static final int CHANGE_ID = 9;
  public static final int COMMAND_GET_META_DATA = 10;
  public static final int SESSION_PREPARE_READ_PARAMS = 11;
  public static final int SESSION_SET_ID = 12;
  public static final int SESSION_CANCEL_STATEMENT = 13;
  public static final int SESSION_CHECK_KEY = 14;
  public static final int SESSION_SET_AUTOCOMMIT = 15;
  public static final int SESSION_HAS_PENDING_TRANSACTION = 16;
  public static final int LOB_READ = 17;
  public static final int STATUS_ERROR = 0;
  public static final int STATUS_OK = 1;
  public static final int STATUS_CLOSED = 2;
  public static final int STATUS_OK_STATE_CHANGED = 3;
  private static SessionFactory sessionFactory;
  private TraceSystem traceSystem;
  private Trace trace;
  private ArrayList<Transfer> transferList = New.arrayList();
  private int nextId;
  private boolean autoCommit = true;
  private CommandInterface autoCommitFalse;
  private CommandInterface autoCommitTrue;
  private ConnectionInfo connectionInfo;
  private String databaseName;
  private String cipher;
  private byte[] fileEncryptionKey;
  private final Object lobSyncObject = new Object();
  private String sessionId;
  private int clientVersion;
  private boolean autoReconnect;
  private int lastReconnect;
  private SessionInterface embedded;
  private DatabaseEventListener eventListener;
  private LobStorageFrontend lobStorage;
  private boolean cluster;
  private TempFileDeleter tempFileDeleter;
  private JavaObjectSerializer javaObjectSerializer;
  private volatile boolean javaObjectSerializerInitialized;
  
  public SessionRemote(ConnectionInfo paramConnectionInfo)
  {
    this.connectionInfo = paramConnectionInfo;
  }
  
  public ArrayList<String> getClusterServers()
  {
    ArrayList localArrayList = new ArrayList();
    for (int i = 0; i < this.transferList.size(); i++)
    {
      Transfer localTransfer = (Transfer)this.transferList.get(i);
      localArrayList.add(localTransfer.getSocket().getInetAddress().getHostAddress() + ":" + localTransfer.getSocket().getPort());
    }
    return localArrayList;
  }
  
  private Transfer initTransfer(ConnectionInfo paramConnectionInfo, String paramString1, String paramString2)
    throws IOException
  {
    Socket localSocket = NetUtils.createSocket(paramString2, 9092, paramConnectionInfo.isSSL());
    
    Transfer localTransfer = new Transfer(this);
    localTransfer.setSocket(localSocket);
    localTransfer.setSSL(paramConnectionInfo.isSSL());
    localTransfer.init();
    localTransfer.writeInt(6);
    localTransfer.writeInt(15);
    localTransfer.writeString(paramString1);
    localTransfer.writeString(paramConnectionInfo.getOriginalURL());
    localTransfer.writeString(paramConnectionInfo.getUserName());
    localTransfer.writeBytes(paramConnectionInfo.getUserPasswordHash());
    localTransfer.writeBytes(paramConnectionInfo.getFilePasswordHash());
    String[] arrayOfString1 = paramConnectionInfo.getKeys();
    localTransfer.writeInt(arrayOfString1.length);
    for (String str : arrayOfString1) {
      localTransfer.writeString(str).writeString(paramConnectionInfo.getProperty(str));
    }
    try
    {
      done(localTransfer);
      this.clientVersion = localTransfer.readInt();
      localTransfer.setVersion(this.clientVersion);
      if ((this.clientVersion >= 14) && 
        (paramConnectionInfo.getFileEncryptionKey() != null)) {
        localTransfer.writeBytes(paramConnectionInfo.getFileEncryptionKey());
      }
      localTransfer.writeInt(12);
      localTransfer.writeString(this.sessionId);
      done(localTransfer);
      if (this.clientVersion >= 15) {
        this.autoCommit = localTransfer.readBoolean();
      } else {
        this.autoCommit = true;
      }
      return localTransfer;
    }
    catch (DbException localDbException)
    {
      localTransfer.close();
      throw localDbException;
    }
  }
  
  public boolean hasPendingTransaction()
  {
    if (this.clientVersion < 10) {
      return true;
    }
    int i = 0;
    for (int j = 0; i < this.transferList.size(); i++)
    {
      Transfer localTransfer = (Transfer)this.transferList.get(i);
      try
      {
        traceOperation("SESSION_HAS_PENDING_TRANSACTION", 0);
        localTransfer.writeInt(16);
        
        done(localTransfer);
        return localTransfer.readInt() != 0;
      }
      catch (IOException localIOException)
      {
        removeServer(localIOException, i--, ++j);
      }
    }
    return true;
  }
  
  public void cancel() {}
  
  public void cancelStatement(int paramInt)
  {
    for (Transfer localTransfer1 : this.transferList) {
      try
      {
        Transfer localTransfer2 = localTransfer1.openNewConnection();
        localTransfer2.init();
        localTransfer2.writeInt(this.clientVersion);
        localTransfer2.writeInt(this.clientVersion);
        localTransfer2.writeString(null);
        localTransfer2.writeString(null);
        localTransfer2.writeString(this.sessionId);
        localTransfer2.writeInt(13);
        localTransfer2.writeInt(paramInt);
        localTransfer2.close();
      }
      catch (IOException localIOException)
      {
        this.trace.debug(localIOException, "could not cancel statement");
      }
    }
  }
  
  private void checkClusterDisableAutoCommit(String paramString)
  {
    if ((this.autoCommit) && (this.transferList.size() > 1))
    {
      setAutoCommitSend(false);
      CommandInterface localCommandInterface = prepareCommand("SET CLUSTER " + paramString, Integer.MAX_VALUE);
      
      localCommandInterface.executeUpdate();
      
      this.autoCommit = true;
      this.cluster = true;
    }
  }
  
  public boolean getAutoCommit()
  {
    return this.autoCommit;
  }
  
  public void setAutoCommit(boolean paramBoolean)
  {
    if (!this.cluster) {
      setAutoCommitSend(paramBoolean);
    }
    this.autoCommit = paramBoolean;
  }
  
  public void setAutoCommitFromServer(boolean paramBoolean)
  {
    if (this.cluster)
    {
      if (paramBoolean)
      {
        setAutoCommitSend(false);
        this.autoCommit = true;
      }
    }
    else {
      this.autoCommit = paramBoolean;
    }
  }
  
  private void setAutoCommitSend(boolean paramBoolean)
  {
    if (this.clientVersion >= 8)
    {
      int i = 0;
      for (int j = 0; i < this.transferList.size(); i++)
      {
        Transfer localTransfer = (Transfer)this.transferList.get(i);
        try
        {
          traceOperation("SESSION_SET_AUTOCOMMIT", paramBoolean ? 1 : 0);
          localTransfer.writeInt(15).writeBoolean(paramBoolean);
          
          done(localTransfer);
        }
        catch (IOException localIOException)
        {
          removeServer(localIOException, i--, ++j);
        }
      }
    }
    else if (paramBoolean)
    {
      if (this.autoCommitTrue == null) {
        this.autoCommitTrue = prepareCommand("SET AUTOCOMMIT TRUE", Integer.MAX_VALUE);
      }
      this.autoCommitTrue.executeUpdate();
    }
    else
    {
      if (this.autoCommitFalse == null) {
        this.autoCommitFalse = prepareCommand("SET AUTOCOMMIT FALSE", Integer.MAX_VALUE);
      }
      this.autoCommitFalse.executeUpdate();
    }
  }
  
  public void autoCommitIfCluster()
  {
    if ((this.autoCommit) && (this.cluster))
    {
      int i = 0;
      for (int j = 0; i < this.transferList.size(); i++)
      {
        Transfer localTransfer = (Transfer)this.transferList.get(i);
        try
        {
          traceOperation("COMMAND_COMMIT", 0);
          localTransfer.writeInt(8);
          done(localTransfer);
        }
        catch (IOException localIOException)
        {
          removeServer(localIOException, i--, ++j);
        }
      }
    }
  }
  
  private String getFilePrefix(String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder(paramString);
    localStringBuilder.append('/');
    for (int i = 0; i < this.databaseName.length(); i++)
    {
      char c = this.databaseName.charAt(i);
      if (Character.isLetterOrDigit(c)) {
        localStringBuilder.append(c);
      } else {
        localStringBuilder.append('_');
      }
    }
    return localStringBuilder.toString();
  }
  
  public int getPowerOffCount()
  {
    return 0;
  }
  
  public void setPowerOffCount(int paramInt)
  {
    throw DbException.getUnsupportedException("remote");
  }
  
  public SessionInterface connectEmbeddedOrServer(boolean paramBoolean)
  {
    ConnectionInfo localConnectionInfo1 = this.connectionInfo;
    if (localConnectionInfo1.isRemote())
    {
      connectServer(localConnectionInfo1);
      return this;
    }
    boolean bool = Boolean.parseBoolean(localConnectionInfo1.getProperty("AUTO_SERVER", "false"));
    
    ConnectionInfo localConnectionInfo2 = null;
    try
    {
      if (bool)
      {
        localConnectionInfo2 = localConnectionInfo1.clone();
        this.connectionInfo = localConnectionInfo1.clone();
      }
      if (paramBoolean) {
        localConnectionInfo1.setProperty("OPEN_NEW", "true");
      }
      if (sessionFactory == null) {
        sessionFactory = (SessionFactory)Class.forName("org.h2.engine.Engine").getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
      }
      return sessionFactory.createSession(localConnectionInfo1);
    }
    catch (Exception localException)
    {
      DbException localDbException = DbException.convert(localException);
      if ((localDbException.getErrorCode() == 90020) && 
        (bool))
      {
        String str = ((JdbcSQLException)localDbException.getSQLException()).getSQL();
        if (str != null)
        {
          localConnectionInfo2.setServerKey(str);
          
          localConnectionInfo2.removeProperty("OPEN_NEW", null);
          connectServer(localConnectionInfo2);
          return this;
        }
      }
      throw localDbException;
    }
  }
  
  private void connectServer(ConnectionInfo paramConnectionInfo)
  {
    String str1 = paramConnectionInfo.getName();
    if (str1.startsWith("//")) {
      str1 = str1.substring("//".length());
    }
    int i = str1.indexOf('/');
    if (i < 0) {
      throw paramConnectionInfo.getFormatException();
    }
    this.databaseName = str1.substring(i + 1);
    String str2 = str1.substring(0, i);
    this.traceSystem = new TraceSystem(null);
    String str3 = paramConnectionInfo.getProperty(10, null);
    if (str3 != null)
    {
      int j = Integer.parseInt(str3);
      String str5 = getFilePrefix(SysProperties.CLIENT_TRACE_DIRECTORY);
      try
      {
        this.traceSystem.setLevelFile(j);
        if ((j > 0) && (j < 4))
        {
          String str7 = FileUtils.createTempFile(str5, ".trace.db", false, false);
          
          this.traceSystem.setFileName(str7);
        }
      }
      catch (IOException localIOException1)
      {
        throw DbException.convertIOException(localIOException1, str5);
      }
    }
    String str4 = paramConnectionInfo.getProperty(9, null);
    if (str4 != null)
    {
      int k = Integer.parseInt(str4);
      this.traceSystem.setLevelSystemOut(k);
    }
    this.trace = this.traceSystem.getTrace("jdbc");
    String str6 = null;
    if (str2.indexOf(',') >= 0)
    {
      str6 = StringUtils.quoteStringSQL(str2);
      paramConnectionInfo.setProperty("CLUSTER", "TRUE");
    }
    this.autoReconnect = Boolean.parseBoolean(paramConnectionInfo.getProperty("AUTO_RECONNECT", "false"));
    
    boolean bool = Boolean.parseBoolean(paramConnectionInfo.getProperty("AUTO_SERVER", "false"));
    if ((bool) && (str6 != null)) {
      throw DbException.getUnsupportedException("autoServer && serverList != null");
    }
    this.autoReconnect |= bool;
    if (this.autoReconnect)
    {
      localObject = paramConnectionInfo.getProperty("DATABASE_EVENT_LISTENER");
      if (localObject != null)
      {
        localObject = StringUtils.trim((String)localObject, true, true, "'");
        try
        {
          this.eventListener = ((DatabaseEventListener)JdbcUtils.loadUserClass((String)localObject).newInstance());
        }
        catch (Throwable localThrowable)
        {
          throw DbException.convert(localThrowable);
        }
      }
    }
    this.cipher = paramConnectionInfo.getProperty("CIPHER");
    if (this.cipher != null) {
      this.fileEncryptionKey = MathUtils.secureRandomBytes(32);
    }
    Object localObject = StringUtils.arraySplit(str2, ',', true);
    int m = localObject.length;
    this.transferList.clear();
    this.sessionId = StringUtils.convertBytesToHex(MathUtils.secureRandomBytes(32));
    
    int n = 0;
    try
    {
      for (int i1 = 0; i1 < m; i1++)
      {
        String str8 = localObject[i1];
        try
        {
          Transfer localTransfer = initTransfer(paramConnectionInfo, this.databaseName, str8);
          this.transferList.add(localTransfer);
        }
        catch (IOException localIOException2)
        {
          if (m == 1) {
            throw DbException.get(90067, localIOException2, new String[] { localIOException2 + ": " + str8 });
          }
          n = 1;
        }
      }
      checkClosed();
      if (n != 0) {
        switchOffCluster();
      }
      checkClusterDisableAutoCommit(str6);
    }
    catch (DbException localDbException)
    {
      this.traceSystem.close();
      throw localDbException;
    }
  }
  
  private void switchOffCluster()
  {
    CommandInterface localCommandInterface = prepareCommand("SET CLUSTER ''", Integer.MAX_VALUE);
    localCommandInterface.executeUpdate();
  }
  
  public void removeServer(IOException paramIOException, int paramInt1, int paramInt2)
  {
    this.trace.debug(paramIOException, "removing server because of exception");
    this.transferList.remove(paramInt1);
    if ((this.transferList.size() == 0) && (autoReconnect(paramInt2))) {
      return;
    }
    checkClosed();
    switchOffCluster();
  }
  
  public synchronized CommandInterface prepareCommand(String paramString, int paramInt)
  {
    checkClosed();
    return new CommandRemote(this, this.transferList, paramString, paramInt);
  }
  
  private boolean autoReconnect(int paramInt)
  {
    if (!isClosed()) {
      return false;
    }
    if (!this.autoReconnect) {
      return false;
    }
    if ((!this.cluster) && (!this.autoCommit)) {
      return false;
    }
    if (paramInt > SysProperties.MAX_RECONNECT) {
      return false;
    }
    this.lastReconnect += 1;
    for (;;)
    {
      try
      {
        this.embedded = connectEmbeddedOrServer(false);
      }
      catch (DbException localDbException)
      {
        if (localDbException.getErrorCode() != 90135) {
          throw localDbException;
        }
        try
        {
          Thread.sleep(500L);
        }
        catch (Exception localException) {}
      }
    }
    if (this.embedded == this) {
      this.embedded = null;
    } else {
      connectEmbeddedOrServer(true);
    }
    recreateSessionState();
    if (this.eventListener != null) {
      this.eventListener.setProgress(4, this.databaseName, paramInt, SysProperties.MAX_RECONNECT);
    }
    return true;
  }
  
  public void checkClosed()
  {
    if (isClosed()) {
      throw DbException.get(90067, "session closed");
    }
  }
  
  public void close()
  {
    Object localObject1 = null;
    if (this.transferList != null)
    {
      synchronized (this)
      {
        for (Transfer localTransfer : this.transferList) {
          try
          {
            traceOperation("SESSION_CLOSE", 0);
            localTransfer.writeInt(1);
            done(localTransfer);
            localTransfer.close();
          }
          catch (RuntimeException localRuntimeException)
          {
            this.trace.error(localRuntimeException, "close");
            localObject1 = localRuntimeException;
          }
          catch (Exception localException)
          {
            this.trace.error(localException, "close");
          }
        }
      }
      this.transferList = null;
    }
    this.traceSystem.close();
    if (this.embedded != null)
    {
      this.embedded.close();
      this.embedded = null;
    }
    if (localObject1 != null) {
      throw ((Throwable)localObject1);
    }
  }
  
  public Trace getTrace()
  {
    return this.traceSystem.getTrace("jdbc");
  }
  
  public int getNextId()
  {
    return this.nextId++;
  }
  
  public int getCurrentId()
  {
    return this.nextId;
  }
  
  public void done(Transfer paramTransfer)
    throws IOException
  {
    paramTransfer.flush();
    int i = paramTransfer.readInt();
    if (i == 0)
    {
      String str1 = paramTransfer.readString();
      String str2 = paramTransfer.readString();
      String str3 = paramTransfer.readString();
      int j = paramTransfer.readInt();
      String str4 = paramTransfer.readString();
      JdbcSQLException localJdbcSQLException = new JdbcSQLException(str2, str3, str1, j, null, str4);
      if (j == 90067)
      {
        IOException localIOException = new IOException(localJdbcSQLException.toString(), localJdbcSQLException);
        throw localIOException;
      }
      throw DbException.convert(localJdbcSQLException);
    }
    if (i == 2) {
      this.transferList = null;
    } else if (i == 3) {
      this.sessionStateChanged = true;
    } else if (i != 1) {
      throw DbException.get(90067, "unexpected status " + i);
    }
  }
  
  public boolean isClustered()
  {
    return this.cluster;
  }
  
  public boolean isClosed()
  {
    return (this.transferList == null) || (this.transferList.size() == 0);
  }
  
  public void traceOperation(String paramString, int paramInt)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("{0} {1}", new Object[] { paramString, Integer.valueOf(paramInt) });
    }
  }
  
  public void checkPowerOff() {}
  
  public void checkWritingAllowed() {}
  
  public String getDatabasePath()
  {
    return "";
  }
  
  public String getLobCompressionAlgorithm(int paramInt)
  {
    return null;
  }
  
  public int getMaxLengthInplaceLob()
  {
    return SysProperties.LOB_CLIENT_MAX_SIZE_MEMORY;
  }
  
  public FileStore openFile(String paramString1, String paramString2, boolean paramBoolean)
  {
    if ((paramBoolean) && (!FileUtils.exists(paramString1))) {
      throw DbException.get(90124, paramString1);
    }
    FileStore localFileStore;
    if (this.cipher == null) {
      localFileStore = FileStore.open(this, paramString1, paramString2);
    } else {
      localFileStore = FileStore.open(this, paramString1, paramString2, this.cipher, this.fileEncryptionKey, 0);
    }
    localFileStore.setCheckedWriting(false);
    try
    {
      localFileStore.init();
    }
    catch (DbException localDbException)
    {
      localFileStore.closeSilently();
      throw localDbException;
    }
    return localFileStore;
  }
  
  public DataHandler getDataHandler()
  {
    return this;
  }
  
  public Object getLobSyncObject()
  {
    return this.lobSyncObject;
  }
  
  public SmallLRUCache<String, String[]> getLobFileListCache()
  {
    return null;
  }
  
  public int getLastReconnect()
  {
    return this.lastReconnect;
  }
  
  public TempFileDeleter getTempFileDeleter()
  {
    if (this.tempFileDeleter == null) {
      this.tempFileDeleter = TempFileDeleter.getInstance();
    }
    return this.tempFileDeleter;
  }
  
  public boolean isReconnectNeeded(boolean paramBoolean)
  {
    return false;
  }
  
  public SessionInterface reconnect(boolean paramBoolean)
  {
    return this;
  }
  
  public void afterWriting() {}
  
  public LobStorageInterface getLobStorage()
  {
    if (this.lobStorage == null) {
      this.lobStorage = new LobStorageFrontend(this);
    }
    return this.lobStorage;
  }
  
  public synchronized int readLob(long paramLong1, byte[] paramArrayOfByte1, long paramLong2, byte[] paramArrayOfByte2, int paramInt1, int paramInt2)
  {
    checkClosed();
    int i = 0;
    for (int j = 0; i < this.transferList.size(); i++)
    {
      Transfer localTransfer = (Transfer)this.transferList.get(i);
      try
      {
        traceOperation("LOB_READ", (int)paramLong1);
        localTransfer.writeInt(17);
        localTransfer.writeLong(paramLong1);
        if (this.clientVersion >= 12) {
          localTransfer.writeBytes(paramArrayOfByte1);
        }
        localTransfer.writeLong(paramLong2);
        localTransfer.writeInt(paramInt2);
        done(localTransfer);
        paramInt2 = localTransfer.readInt();
        if (paramInt2 <= 0) {
          return paramInt2;
        }
        localTransfer.readBytes(paramArrayOfByte2, paramInt1, paramInt2);
        return paramInt2;
      }
      catch (IOException localIOException)
      {
        removeServer(localIOException, i--, ++j);
      }
    }
    return 1;
  }
  
  public JavaObjectSerializer getJavaObjectSerializer()
  {
    initJavaObjectSerializer();
    return this.javaObjectSerializer;
  }
  
  private void initJavaObjectSerializer()
  {
    if (this.javaObjectSerializerInitialized) {
      return;
    }
    synchronized (this)
    {
      if (this.javaObjectSerializerInitialized) {
        return;
      }
      String str = readSerializationSettings();
      if (str != null)
      {
        str = str.trim();
        if ((!str.isEmpty()) && (!str.equals("null"))) {
          try
          {
            this.javaObjectSerializer = ((JavaObjectSerializer)JdbcUtils.loadUserClass(str).newInstance());
          }
          catch (Exception localException)
          {
            throw DbException.convert(localException);
          }
        }
      }
      this.javaObjectSerializerInitialized = true;
    }
  }
  
  private String readSerializationSettings()
  {
    String str = null;
    CommandInterface localCommandInterface = prepareCommand("SELECT VALUE FROM INFORMATION_SCHEMA.SETTINGS  WHERE NAME='JAVA_OBJECT_SERIALIZER'", Integer.MAX_VALUE);
    try
    {
      ResultInterface localResultInterface = localCommandInterface.executeQuery(0, false);
      if (localResultInterface.next())
      {
        Value[] arrayOfValue = localResultInterface.currentRow();
        str = arrayOfValue[0].getString();
      }
    }
    finally
    {
      localCommandInterface.close();
    }
    return str;
  }
  
  public void addTemporaryLob(Value paramValue) {}
}
