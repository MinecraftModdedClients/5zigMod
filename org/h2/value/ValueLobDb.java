package org.h2.value;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.h2.engine.Constants;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils;
import org.h2.store.DataHandler;
import org.h2.store.FileStore;
import org.h2.store.FileStoreInputStream;
import org.h2.store.FileStoreOutputStream;
import org.h2.store.LobStorageInterface;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.MathUtils;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class ValueLobDb
  extends Value
  implements Value.ValueClob, Value.ValueBlob
{
  private final int type;
  private final long lobId;
  private final byte[] hmac;
  private final byte[] small;
  private final DataHandler handler;
  private final long precision;
  private final String fileName;
  private final FileStore tempFile;
  private int tableId;
  private int hash;
  
  private ValueLobDb(int paramInt1, DataHandler paramDataHandler, int paramInt2, long paramLong1, byte[] paramArrayOfByte, long paramLong2)
  {
    this.type = paramInt1;
    this.handler = paramDataHandler;
    this.tableId = paramInt2;
    this.lobId = paramLong1;
    this.hmac = paramArrayOfByte;
    this.precision = paramLong2;
    this.small = null;
    this.fileName = null;
    this.tempFile = null;
  }
  
  private ValueLobDb(int paramInt, byte[] paramArrayOfByte, long paramLong)
  {
    this.type = paramInt;
    this.small = paramArrayOfByte;
    this.precision = paramLong;
    this.lobId = 0L;
    this.hmac = null;
    this.handler = null;
    this.fileName = null;
    this.tempFile = null;
  }
  
  private ValueLobDb(DataHandler paramDataHandler, Reader paramReader, long paramLong)
    throws IOException
  {
    this.type = 16;
    this.handler = paramDataHandler;
    this.small = null;
    this.lobId = 0L;
    this.hmac = null;
    this.fileName = createTempLobFileName(paramDataHandler);
    this.tempFile = this.handler.openFile(this.fileName, "rw", false);
    this.tempFile.autoDelete();
    FileStoreOutputStream localFileStoreOutputStream = new FileStoreOutputStream(this.tempFile, null, null);
    long l = 0L;
    try
    {
      char[] arrayOfChar = new char['က'];
      for (;;)
      {
        int i = getBufferSize(this.handler, false, paramLong);
        i = IOUtils.readFully(paramReader, arrayOfChar, i);
        if (i == 0) {
          break;
        }
      }
    }
    finally
    {
      localFileStoreOutputStream.close();
    }
    this.precision = l;
  }
  
  private ValueLobDb(DataHandler paramDataHandler, byte[] paramArrayOfByte, int paramInt, InputStream paramInputStream, long paramLong)
    throws IOException
  {
    this.type = 15;
    this.handler = paramDataHandler;
    this.small = null;
    this.lobId = 0L;
    this.hmac = null;
    this.fileName = createTempLobFileName(paramDataHandler);
    this.tempFile = this.handler.openFile(this.fileName, "rw", false);
    this.tempFile.autoDelete();
    FileStoreOutputStream localFileStoreOutputStream = new FileStoreOutputStream(this.tempFile, null, null);
    long l = 0L;
    boolean bool = this.handler.getLobCompressionAlgorithm(15) != null;
    try
    {
      for (;;)
      {
        l += paramInt;
        localFileStoreOutputStream.write(paramArrayOfByte, 0, paramInt);
        paramLong -= paramInt;
        if (paramLong > 0L)
        {
          paramInt = getBufferSize(this.handler, bool, paramLong);
          paramInt = IOUtils.readFully(paramInputStream, paramArrayOfByte, paramInt);
          if (paramInt <= 0) {
            break;
          }
        }
      }
    }
    finally
    {
      localFileStoreOutputStream.close();
    }
    this.precision = l;
  }
  
  private static String createTempLobFileName(DataHandler paramDataHandler)
    throws IOException
  {
    String str = paramDataHandler.getDatabasePath();
    if (str.length() == 0) {
      str = SysProperties.PREFIX_TEMP_FILE;
    }
    return FileUtils.createTempFile(str, ".temp.db", true, true);
  }
  
  public static ValueLobDb create(int paramInt1, DataHandler paramDataHandler, int paramInt2, long paramLong1, byte[] paramArrayOfByte, long paramLong2)
  {
    return new ValueLobDb(paramInt1, paramDataHandler, paramInt2, paramLong1, paramArrayOfByte, paramLong2);
  }
  
  public Value convertTo(int paramInt)
  {
    if (paramInt == this.type) {
      return this;
    }
    Value localValue;
    if (paramInt == 16)
    {
      if (this.handler != null)
      {
        localValue = this.handler.getLobStorage().createClob(getReader(), -1L);
        
        return localValue;
      }
      if (this.small != null) {
        return createSmallLob(paramInt, this.small);
      }
    }
    else if (paramInt == 15)
    {
      if (this.handler != null)
      {
        localValue = this.handler.getLobStorage().createBlob(getInputStream(), -1L);
        
        return localValue;
      }
      if (this.small != null) {
        return createSmallLob(paramInt, this.small);
      }
    }
    return super.convertTo(paramInt);
  }
  
  public boolean isLinked()
  {
    return (this.tableId != -1) && (this.small == null);
  }
  
  public boolean isStored()
  {
    return (this.small == null) && (this.fileName == null);
  }
  
  public void close()
  {
    if (this.fileName != null)
    {
      if (this.tempFile != null) {
        this.tempFile.stopAutoDelete();
      }
      synchronized (this.handler.getLobSyncObject())
      {
        FileUtils.delete(this.fileName);
      }
    }
    if (this.handler != null) {
      this.handler.getLobStorage().removeLob(this);
    }
  }
  
  public void unlink(DataHandler paramDataHandler)
  {
    if ((this.small == null) && (this.tableId != -1))
    {
      paramDataHandler.getLobStorage().setTable(this, -1);
      
      this.tableId = -1;
    }
  }
  
  public Value link(DataHandler paramDataHandler, int paramInt)
  {
    if (this.small == null)
    {
      if (this.tableId == -2)
      {
        paramDataHandler.getLobStorage().setTable(this, paramInt);
        this.tableId = paramInt;
      }
      else
      {
        return this.handler.getLobStorage().copyLob(this, paramInt, getPrecision());
      }
    }
    else if (this.small.length > paramDataHandler.getMaxLengthInplaceLob())
    {
      LobStorageInterface localLobStorageInterface = paramDataHandler.getLobStorage();
      Value localValue;
      if (this.type == 15) {
        localValue = localLobStorageInterface.createBlob(getInputStream(), getPrecision());
      } else {
        localValue = localLobStorageInterface.createClob(getReader(), getPrecision());
      }
      return localValue.link(paramDataHandler, paramInt);
    }
    return this;
  }
  
  public int getTableId()
  {
    return this.tableId;
  }
  
  public int getType()
  {
    return this.type;
  }
  
  public long getPrecision()
  {
    return this.precision;
  }
  
  public String getString()
  {
    int i = (this.precision > 2147483647L) || (this.precision == 0L) ? Integer.MAX_VALUE : (int)this.precision;
    try
    {
      if (this.type == 16)
      {
        if (this.small != null) {
          return new String(this.small, Constants.UTF8);
        }
        return IOUtils.readStringAndClose(getReader(), i);
      }
      byte[] arrayOfByte;
      if (this.small != null) {
        arrayOfByte = this.small;
      } else {
        arrayOfByte = IOUtils.readBytesAndClose(getInputStream(), i);
      }
      return StringUtils.convertBytesToHex(arrayOfByte);
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, toString());
    }
  }
  
  public byte[] getBytes()
  {
    if (this.type == 16) {
      return super.getBytes();
    }
    byte[] arrayOfByte = getBytesNoCopy();
    return Utils.cloneByteArray(arrayOfByte);
  }
  
  public byte[] getBytesNoCopy()
  {
    if (this.type == 16) {
      return super.getBytesNoCopy();
    }
    if (this.small != null) {
      return this.small;
    }
    try
    {
      return IOUtils.readBytesAndClose(getInputStream(), Integer.MAX_VALUE);
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, toString());
    }
  }
  
  public int hashCode()
  {
    if (this.hash == 0)
    {
      if (this.precision > 4096L) {
        return (int)(this.precision ^ this.precision >>> 32);
      }
      if (this.type == 16) {
        this.hash = getString().hashCode();
      } else {
        this.hash = Utils.getByteArrayHash(getBytes());
      }
    }
    return this.hash;
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    if ((paramValue instanceof ValueLobDb))
    {
      localObject = (ValueLobDb)paramValue;
      if (paramValue == this) {
        return 0;
      }
      if ((this.lobId == ((ValueLobDb)localObject).lobId) && (this.small == null) && (((ValueLobDb)localObject).small == null)) {
        return 0;
      }
    }
    if (this.type == 16) {
      return Integer.signum(getString().compareTo(paramValue.getString()));
    }
    Object localObject = paramValue.getBytesNoCopy();
    return Utils.compareNotNullSigned(getBytes(), (byte[])localObject);
  }
  
  public Object getObject()
  {
    if (this.type == 16) {
      return getReader();
    }
    return getInputStream();
  }
  
  public Reader getReader()
  {
    return IOUtils.getBufferedReader(getInputStream());
  }
  
  public InputStream getInputStream()
  {
    if (this.small != null) {
      return new ByteArrayInputStream(this.small);
    }
    if (this.fileName != null)
    {
      FileStore localFileStore = this.handler.openFile(this.fileName, "r", true);
      boolean bool = SysProperties.lobCloseBetweenReads;
      return new BufferedInputStream(new FileStoreInputStream(localFileStore, this.handler, false, bool), 4096);
    }
    long l = this.type == 15 ? this.precision : -1L;
    try
    {
      return this.handler.getLobStorage().getInputStream(this, this.hmac, l);
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, toString());
    }
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    long l = getPrecision();
    if ((l > 2147483647L) || (l <= 0L)) {
      l = -1L;
    }
    if (this.type == 15) {
      paramPreparedStatement.setBinaryStream(paramInt, getInputStream(), (int)l);
    } else {
      paramPreparedStatement.setCharacterStream(paramInt, getReader(), (int)l);
    }
  }
  
  public String getSQL()
  {
    if (this.type == 16)
    {
      str = getString();
      return StringUtils.quoteStringSQL(str);
    }
    byte[] arrayOfByte = getBytes();
    String str = StringUtils.convertBytesToHex(arrayOfByte);
    return "X'" + str + "'";
  }
  
  public String getTraceSQL()
  {
    if ((this.small != null) && (getPrecision() <= SysProperties.MAX_TRACE_DATA_LENGTH)) {
      return getSQL();
    }
    StringBuilder localStringBuilder = new StringBuilder();
    if (this.type == 16) {
      localStringBuilder.append("SPACE(").append(getPrecision());
    } else {
      localStringBuilder.append("CAST(REPEAT('00', ").append(getPrecision()).append(") AS BINARY");
    }
    localStringBuilder.append(" /* table: ").append(this.tableId).append(" id: ").append(this.lobId).append(" */)");
    
    return localStringBuilder.toString();
  }
  
  public byte[] getSmall()
  {
    return this.small;
  }
  
  public int getDisplaySize()
  {
    return MathUtils.convertLongToInt(getPrecision());
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof ValueLobDb)) && (compareSecure((Value)paramObject, null) == 0);
  }
  
  public int getMemory()
  {
    if (this.small != null) {
      return this.small.length + 104;
    }
    return 140;
  }
  
  public ValueLobDb copyToTemp()
  {
    return this;
  }
  
  public ValueLobDb copyToResult()
  {
    if (this.handler == null) {
      return this;
    }
    LobStorageInterface localLobStorageInterface = this.handler.getLobStorage();
    if (localLobStorageInterface.isReadOnly()) {
      return this;
    }
    return localLobStorageInterface.copyLob(this, -3, getPrecision());
  }
  
  public long getLobId()
  {
    return this.lobId;
  }
  
  public String toString()
  {
    return "lob: " + this.fileName + " table: " + this.tableId + " id: " + this.lobId;
  }
  
  public static ValueLobDb createTempClob(Reader paramReader, long paramLong, DataHandler paramDataHandler)
  {
    BufferedReader localBufferedReader;
    if ((paramReader instanceof BufferedReader)) {
      localBufferedReader = (BufferedReader)paramReader;
    } else {
      localBufferedReader = new BufferedReader(paramReader, 4096);
    }
    try
    {
      boolean bool = paramDataHandler.getLobCompressionAlgorithm(16) != null;
      long l = Long.MAX_VALUE;
      if ((paramLong >= 0L) && (paramLong < l)) {
        l = paramLong;
      }
      int i = getBufferSize(paramDataHandler, bool, l);
      Object localObject;
      char[] arrayOfChar;
      if (i >= Integer.MAX_VALUE)
      {
        localObject = IOUtils.readStringAndClose(localBufferedReader, -1);
        arrayOfChar = ((String)localObject).toCharArray();
        i = arrayOfChar.length;
      }
      else
      {
        arrayOfChar = new char[i];
        localBufferedReader.mark(i);
        i = IOUtils.readFully(localBufferedReader, arrayOfChar, i);
      }
      if (i <= paramDataHandler.getMaxLengthInplaceLob())
      {
        localObject = new String(arrayOfChar, 0, i).getBytes(Constants.UTF8);
        return createSmallLob(16, (byte[])localObject, i);
      }
      localBufferedReader.reset();
      return new ValueLobDb(paramDataHandler, localBufferedReader, l);
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
  }
  
  public static ValueLobDb createTempBlob(InputStream paramInputStream, long paramLong, DataHandler paramDataHandler)
  {
    try
    {
      long l = Long.MAX_VALUE;
      boolean bool = paramDataHandler.getLobCompressionAlgorithm(15) != null;
      if ((paramLong >= 0L) && (paramLong < l)) {
        l = paramLong;
      }
      int i = getBufferSize(paramDataHandler, bool, l);
      byte[] arrayOfByte;
      if (i >= Integer.MAX_VALUE)
      {
        arrayOfByte = IOUtils.readBytesAndClose(paramInputStream, -1);
        i = arrayOfByte.length;
      }
      else
      {
        arrayOfByte = DataUtils.newBytes(i);
        i = IOUtils.readFully(paramInputStream, arrayOfByte, i);
      }
      Object localObject;
      if (i <= paramDataHandler.getMaxLengthInplaceLob())
      {
        localObject = DataUtils.newBytes(i);
        System.arraycopy(arrayOfByte, 0, localObject, 0, i);
        return createSmallLob(15, (byte[])localObject, localObject.length);
      }
      return new ValueLobDb(paramDataHandler, arrayOfByte, i, paramInputStream, l);
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
  }
  
  private static int getBufferSize(DataHandler paramDataHandler, boolean paramBoolean, long paramLong)
  {
    if ((paramLong < 0L) || (paramLong > 2147483647L)) {
      paramLong = 2147483647L;
    }
    int i = paramDataHandler.getMaxLengthInplaceLob();
    long l = paramBoolean ? 131072L : 4096L;
    if ((l < paramLong) && (l <= i))
    {
      l = Math.min(paramLong, i + 1L);
      
      l = MathUtils.roundUpLong(l, 4096L);
    }
    l = Math.min(paramLong, l);
    l = MathUtils.convertLongToInt(l);
    if (l < 0L) {
      l = 2147483647L;
    }
    return (int)l;
  }
  
  public Value convertPrecision(long paramLong, boolean paramBoolean)
  {
    if (this.precision <= paramLong) {
      return this;
    }
    Object localObject;
    ValueLobDb localValueLobDb;
    if (this.type == 16)
    {
      if (this.handler == null) {
        try
        {
          int i = MathUtils.convertLongToInt(paramLong);
          localObject = IOUtils.readStringAndClose(getReader(), i);
          byte[] arrayOfByte = ((String)localObject).getBytes(Constants.UTF8);
          localValueLobDb = createSmallLob(this.type, arrayOfByte, ((String)localObject).length());
        }
        catch (IOException localIOException1)
        {
          throw DbException.convertIOException(localIOException1, null);
        }
      } else {
        localValueLobDb = createTempClob(getReader(), paramLong, this.handler);
      }
    }
    else if (this.handler == null) {
      try
      {
        int j = MathUtils.convertLongToInt(paramLong);
        localObject = IOUtils.readBytesAndClose(getInputStream(), j);
        localValueLobDb = createSmallLob(this.type, (byte[])localObject, localObject.length);
      }
      catch (IOException localIOException2)
      {
        throw DbException.convertIOException(localIOException2, null);
      }
    } else {
      localValueLobDb = createTempBlob(getInputStream(), paramLong, this.handler);
    }
    return localValueLobDb;
  }
  
  public static Value createSmallLob(int paramInt, byte[] paramArrayOfByte)
  {
    int i;
    if (paramInt == 16) {
      i = new String(paramArrayOfByte, Constants.UTF8).length();
    } else {
      i = paramArrayOfByte.length;
    }
    return createSmallLob(paramInt, paramArrayOfByte, i);
  }
  
  public static ValueLobDb createSmallLob(int paramInt, byte[] paramArrayOfByte, long paramLong)
  {
    return new ValueLobDb(paramInt, paramArrayOfByte, paramLong);
  }
}
