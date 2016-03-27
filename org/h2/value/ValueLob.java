package org.h2.value;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.h2.engine.Constants;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils;
import org.h2.store.DataHandler;
import org.h2.store.FileStore;
import org.h2.store.FileStoreInputStream;
import org.h2.store.FileStoreOutputStream;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.MathUtils;
import org.h2.util.SmallLRUCache;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class ValueLob
  extends Value
{
  private static int dirCounter;
  private final int type;
  private long precision;
  private DataHandler handler;
  private int tableId;
  private int objectId;
  private String fileName;
  private boolean linked;
  private byte[] small;
  private int hash;
  private boolean compressed;
  private FileStore tempFile;
  
  private ValueLob(int paramInt1, DataHandler paramDataHandler, String paramString, int paramInt2, int paramInt3, boolean paramBoolean1, long paramLong, boolean paramBoolean2)
  {
    this.type = paramInt1;
    this.handler = paramDataHandler;
    this.fileName = paramString;
    this.tableId = paramInt2;
    this.objectId = paramInt3;
    this.linked = paramBoolean1;
    this.precision = paramLong;
    this.compressed = paramBoolean2;
  }
  
  private ValueLob(int paramInt, byte[] paramArrayOfByte)
  {
    this.type = paramInt;
    this.small = paramArrayOfByte;
    if (paramArrayOfByte != null) {
      if (paramInt == 15) {
        this.precision = paramArrayOfByte.length;
      } else {
        this.precision = getString().length();
      }
    }
  }
  
  private static ValueLob copy(ValueLob paramValueLob)
  {
    ValueLob localValueLob = new ValueLob(paramValueLob.type, paramValueLob.handler, paramValueLob.fileName, paramValueLob.tableId, paramValueLob.objectId, paramValueLob.linked, paramValueLob.precision, paramValueLob.compressed);
    
    localValueLob.small = paramValueLob.small;
    localValueLob.hash = paramValueLob.hash;
    return localValueLob;
  }
  
  private static ValueLob createSmallLob(int paramInt, byte[] paramArrayOfByte)
  {
    return new ValueLob(paramInt, paramArrayOfByte);
  }
  
  private static String getFileName(DataHandler paramDataHandler, int paramInt1, int paramInt2)
  {
    if ((SysProperties.CHECK) && (paramInt1 == 0) && (paramInt2 == 0)) {
      DbException.throwInternalError("0 LOB");
    }
    String str = ".t" + paramInt1;
    return getFileNamePrefix(paramDataHandler.getDatabasePath(), paramInt2) + str + ".lob.db";
  }
  
  public static ValueLob openLinked(int paramInt1, DataHandler paramDataHandler, int paramInt2, int paramInt3, long paramLong, boolean paramBoolean)
  {
    String str = getFileName(paramDataHandler, paramInt2, paramInt3);
    return new ValueLob(paramInt1, paramDataHandler, str, paramInt2, paramInt3, true, paramLong, paramBoolean);
  }
  
  public static ValueLob openUnlinked(int paramInt1, DataHandler paramDataHandler, int paramInt2, int paramInt3, long paramLong, boolean paramBoolean, String paramString)
  {
    return new ValueLob(paramInt1, paramDataHandler, paramString, paramInt2, paramInt3, false, paramLong, paramBoolean);
  }
  
  private static ValueLob createClob(Reader paramReader, long paramLong, DataHandler paramDataHandler)
  {
    try
    {
      if (paramDataHandler == null)
      {
        String str = IOUtils.readStringAndClose(paramReader, (int)paramLong);
        return createSmallLob(16, str.getBytes(Constants.UTF8));
      }
      boolean bool = paramDataHandler.getLobCompressionAlgorithm(16) != null;
      long l = Long.MAX_VALUE;
      if ((paramLong >= 0L) && (paramLong < l)) {
        l = paramLong;
      }
      int i = getBufferSize(paramDataHandler, bool, l);
      char[] arrayOfChar;
      if (i >= Integer.MAX_VALUE)
      {
        localObject = IOUtils.readStringAndClose(paramReader, -1);
        arrayOfChar = ((String)localObject).toCharArray();
        i = arrayOfChar.length;
      }
      else
      {
        arrayOfChar = new char[i];
        i = IOUtils.readFully(paramReader, arrayOfChar, i);
      }
      if (i <= paramDataHandler.getMaxLengthInplaceLob())
      {
        localObject = new String(arrayOfChar, 0, i).getBytes(Constants.UTF8);
        return createSmallLob(16, (byte[])localObject);
      }
      Object localObject = new ValueLob(16, null);
      ((ValueLob)localObject).createFromReader(arrayOfChar, i, paramReader, l, paramDataHandler);
      return (ValueLob)localObject;
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
  
  private void createFromReader(char[] paramArrayOfChar, int paramInt, Reader paramReader, long paramLong, DataHandler paramDataHandler)
    throws IOException
  {
    FileStoreOutputStream localFileStoreOutputStream = initLarge(paramDataHandler);
    boolean bool = paramDataHandler.getLobCompressionAlgorithm(16) != null;
    try
    {
      for (;;)
      {
        this.precision += paramInt;
        byte[] arrayOfByte = new String(paramArrayOfChar, 0, paramInt).getBytes(Constants.UTF8);
        localFileStoreOutputStream.write(arrayOfByte, 0, arrayOfByte.length);
        paramLong -= paramInt;
        if (paramLong <= 0L) {
          break;
        }
        paramInt = getBufferSize(paramDataHandler, bool, paramLong);
        paramInt = IOUtils.readFully(paramReader, paramArrayOfChar, paramInt);
        if (paramInt == 0) {
          break;
        }
      }
    }
    finally
    {
      localFileStoreOutputStream.close();
    }
  }
  
  private static String getFileNamePrefix(String paramString, int paramInt)
  {
    int i = paramInt % SysProperties.LOB_FILES_PER_DIRECTORY;
    if (i > 0) {
      str = SysProperties.FILE_SEPARATOR + paramInt;
    } else {
      str = "";
    }
    paramInt /= SysProperties.LOB_FILES_PER_DIRECTORY;
    while (paramInt > 0)
    {
      i = paramInt % SysProperties.LOB_FILES_PER_DIRECTORY;
      str = SysProperties.FILE_SEPARATOR + i + ".lobs.db" + str;
      
      paramInt /= SysProperties.LOB_FILES_PER_DIRECTORY;
    }
    String str = FileUtils.toRealPath(paramString + ".lobs.db" + str);
    
    return str;
  }
  
  private static int getNewObjectId(DataHandler paramDataHandler)
  {
    String str1 = paramDataHandler.getDatabasePath();
    if ((str1 != null) && (str1.length() == 0)) {
      str1 = new File(Utils.getProperty("java.io.tmpdir", "."), SysProperties.PREFIX_TEMP_FILE).getAbsolutePath();
    }
    int i = 0;
    int j = SysProperties.LOB_FILES_PER_DIRECTORY;
    for (;;)
    {
      String str2 = getFileNamePrefix(str1, i);
      String[] arrayOfString1 = getFileList(paramDataHandler, str2);
      int k = 0;
      boolean[] arrayOfBoolean = new boolean[j];
      for (String str3 : arrayOfString1) {
        if (str3.endsWith(".db"))
        {
          str3 = FileUtils.getName(str3);
          String str4 = str3.substring(0, str3.indexOf('.'));
          int i2;
          try
          {
            i2 = Integer.parseInt(str4);
          }
          catch (NumberFormatException localNumberFormatException)
          {
            i2 = -1;
          }
          if (i2 > 0)
          {
            k++;
            arrayOfBoolean[(i2 % j)] = true;
          }
        }
      }
      int m = -1;
      if (k < j) {
        for (??? = 1; ??? < j; ???++) {
          if (arrayOfBoolean[???] == 0)
          {
            m = ???;
            break;
          }
        }
      }
      if (m > 0)
      {
        i += m;
        invalidateFileList(paramDataHandler, str2);
        break;
      }
      if (i > Integer.MAX_VALUE / j)
      {
        i = 0;
        dirCounter = MathUtils.randomInt(j - 1) * j;
      }
      else
      {
        ??? = dirCounter++ / (j - 1) + 1;
        i *= j;
        i += ??? * j;
      }
    }
    return i;
  }
  
  private static void invalidateFileList(DataHandler paramDataHandler, String paramString)
  {
    SmallLRUCache localSmallLRUCache = paramDataHandler.getLobFileListCache();
    if (localSmallLRUCache != null) {
      synchronized (localSmallLRUCache)
      {
        localSmallLRUCache.remove(paramString);
      }
    }
  }
  
  private static String[] getFileList(DataHandler paramDataHandler, String paramString)
  {
    SmallLRUCache localSmallLRUCache = paramDataHandler.getLobFileListCache();
    String[] arrayOfString;
    if (localSmallLRUCache == null) {
      arrayOfString = (String[])FileUtils.newDirectoryStream(paramString).toArray(new String[0]);
    } else {
      synchronized (localSmallLRUCache)
      {
        arrayOfString = (String[])localSmallLRUCache.get(paramString);
        if (arrayOfString == null)
        {
          arrayOfString = (String[])FileUtils.newDirectoryStream(paramString).toArray(new String[0]);
          localSmallLRUCache.put(paramString, arrayOfString);
        }
      }
    }
    return arrayOfString;
  }
  
  private static ValueLob createBlob(InputStream paramInputStream, long paramLong, DataHandler paramDataHandler)
  {
    try
    {
      if (paramDataHandler == null)
      {
        byte[] arrayOfByte1 = IOUtils.readBytesAndClose(paramInputStream, (int)paramLong);
        return createSmallLob(15, arrayOfByte1);
      }
      long l = Long.MAX_VALUE;
      boolean bool = paramDataHandler.getLobCompressionAlgorithm(15) != null;
      if ((paramLong >= 0L) && (paramLong < l)) {
        l = paramLong;
      }
      int i = getBufferSize(paramDataHandler, bool, l);
      byte[] arrayOfByte2;
      if (i >= Integer.MAX_VALUE)
      {
        arrayOfByte2 = IOUtils.readBytesAndClose(paramInputStream, -1);
        i = arrayOfByte2.length;
      }
      else
      {
        arrayOfByte2 = DataUtils.newBytes(i);
        i = IOUtils.readFully(paramInputStream, arrayOfByte2, i);
      }
      if (i <= paramDataHandler.getMaxLengthInplaceLob())
      {
        localObject = DataUtils.newBytes(i);
        System.arraycopy(arrayOfByte2, 0, localObject, 0, i);
        return createSmallLob(15, (byte[])localObject);
      }
      Object localObject = new ValueLob(15, null);
      ((ValueLob)localObject).createFromStream(arrayOfByte2, i, paramInputStream, l, paramDataHandler);
      return (ValueLob)localObject;
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
  }
  
  private FileStoreOutputStream initLarge(DataHandler paramDataHandler)
  {
    this.handler = paramDataHandler;
    this.tableId = 0;
    this.linked = false;
    this.precision = 0L;
    this.small = null;
    this.hash = 0;
    String str1 = paramDataHandler.getLobCompressionAlgorithm(this.type);
    this.compressed = (str1 != null);
    synchronized (paramDataHandler)
    {
      String str2 = paramDataHandler.getDatabasePath();
      if ((str2 != null) && (str2.length() == 0)) {
        str2 = new File(Utils.getProperty("java.io.tmpdir", "."), SysProperties.PREFIX_TEMP_FILE).getAbsolutePath();
      }
      this.objectId = getNewObjectId(paramDataHandler);
      this.fileName = (getFileNamePrefix(str2, this.objectId) + ".temp.db");
      this.tempFile = paramDataHandler.openFile(this.fileName, "rw", false);
      this.tempFile.autoDelete();
    }
    ??? = new FileStoreOutputStream(this.tempFile, paramDataHandler, str1);
    
    return (FileStoreOutputStream)???;
  }
  
  private void createFromStream(byte[] paramArrayOfByte, int paramInt, InputStream paramInputStream, long paramLong, DataHandler paramDataHandler)
    throws IOException
  {
    FileStoreOutputStream localFileStoreOutputStream = initLarge(paramDataHandler);
    boolean bool = paramDataHandler.getLobCompressionAlgorithm(15) != null;
    try
    {
      for (;;)
      {
        this.precision += paramInt;
        localFileStoreOutputStream.write(paramArrayOfByte, 0, paramInt);
        paramLong -= paramInt;
        if (paramLong > 0L)
        {
          paramInt = getBufferSize(paramDataHandler, bool, paramLong);
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
  }
  
  public Value convertTo(int paramInt)
  {
    if (paramInt == this.type) {
      return this;
    }
    ValueLob localValueLob;
    if (paramInt == 16)
    {
      localValueLob = createClob(getReader(), -1L, this.handler);
      return localValueLob;
    }
    if (paramInt == 15)
    {
      localValueLob = createBlob(getInputStream(), -1L, this.handler);
      return localValueLob;
    }
    return super.convertTo(paramInt);
  }
  
  public boolean isLinked()
  {
    return this.linked;
  }
  
  public String getFileName()
  {
    return this.fileName;
  }
  
  public void close()
  {
    if (this.fileName != null)
    {
      if (this.tempFile != null)
      {
        this.tempFile.stopAutoDelete();
        this.tempFile = null;
      }
      deleteFile(this.handler, this.fileName);
    }
  }
  
  public void unlink(DataHandler paramDataHandler)
  {
    if ((this.linked) && (this.fileName != null)) {
      synchronized (paramDataHandler)
      {
        String str = getFileName(paramDataHandler, -1, this.objectId);
        deleteFile(paramDataHandler, str);
        renameFile(paramDataHandler, this.fileName, str);
        this.tempFile = FileStore.open(paramDataHandler, str, "rw");
        this.tempFile.autoDelete();
        this.tempFile.closeSilently();
        this.fileName = str;
        this.linked = false;
      }
    }
  }
  
  public Value link(DataHandler paramDataHandler, int paramInt)
  {
    if (this.fileName == null)
    {
      this.tableId = paramInt;
      return this;
    }
    Object localObject;
    if (this.linked)
    {
      localObject = copy(this);
      ((ValueLob)localObject).objectId = getNewObjectId(paramDataHandler);
      ((ValueLob)localObject).tableId = paramInt;
      String str = getFileName(paramDataHandler, ((ValueLob)localObject).tableId, ((ValueLob)localObject).objectId);
      copyFileTo(paramDataHandler, this.fileName, str);
      ((ValueLob)localObject).fileName = str;
      ((ValueLob)localObject).linked = true;
      return (Value)localObject;
    }
    if (!this.linked)
    {
      this.tableId = paramInt;
      localObject = getFileName(paramDataHandler, this.tableId, this.objectId);
      if (this.tempFile != null)
      {
        this.tempFile.stopAutoDelete();
        this.tempFile = null;
      }
      renameFile(paramDataHandler, this.fileName, (String)localObject);
      this.fileName = ((String)localObject);
      this.linked = true;
    }
    return this;
  }
  
  public int getTableId()
  {
    return this.tableId;
  }
  
  public int getObjectId()
  {
    return this.objectId;
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
      throw DbException.convertIOException(localIOException, this.fileName);
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
      throw DbException.convertIOException(localIOException, this.fileName);
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
    if (this.type == 16) {
      return Integer.signum(getString().compareTo(paramValue.getString()));
    }
    byte[] arrayOfByte = paramValue.getBytesNoCopy();
    return Utils.compareNotNullSigned(getBytes(), arrayOfByte);
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
    if (this.fileName == null) {
      return new ByteArrayInputStream(this.small);
    }
    FileStore localFileStore = this.handler.openFile(this.fileName, "r", true);
    boolean bool = SysProperties.lobCloseBetweenReads;
    return new BufferedInputStream(new FileStoreInputStream(localFileStore, this.handler, this.compressed, bool), 4096);
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
    localStringBuilder.append(" /* ").append(this.fileName).append(" */)");
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
    return ((paramObject instanceof ValueLob)) && (compareSecure((Value)paramObject, null) == 0);
  }
  
  public void convertToFileIfRequired(DataHandler paramDataHandler)
  {
    try
    {
      if ((this.small != null) && (this.small.length > paramDataHandler.getMaxLengthInplaceLob()))
      {
        boolean bool = paramDataHandler.getLobCompressionAlgorithm(this.type) != null;
        int i = getBufferSize(paramDataHandler, bool, Long.MAX_VALUE);
        int j = this.tableId;
        if (this.type == 15) {
          createFromStream(DataUtils.newBytes(i), 0, getInputStream(), Long.MAX_VALUE, paramDataHandler);
        } else {
          createFromReader(new char[i], 0, getReader(), Long.MAX_VALUE, paramDataHandler);
        }
        Value localValue = link(paramDataHandler, j);
        if ((SysProperties.CHECK) && (localValue != this)) {
          DbException.throwInternalError();
        }
      }
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
  }
  
  public boolean isCompressed()
  {
    return this.compressed;
  }
  
  private static synchronized void deleteFile(DataHandler paramDataHandler, String paramString)
  {
    synchronized (paramDataHandler.getLobSyncObject())
    {
      FileUtils.delete(paramString);
    }
  }
  
  private static synchronized void renameFile(DataHandler paramDataHandler, String paramString1, String paramString2)
  {
    synchronized (paramDataHandler.getLobSyncObject())
    {
      FileUtils.move(paramString1, paramString2);
    }
  }
  
  private static void copyFileTo(DataHandler paramDataHandler, String paramString1, String paramString2)
  {
    synchronized (paramDataHandler.getLobSyncObject())
    {
      try
      {
        IOUtils.copyFiles(paramString1, paramString2);
      }
      catch (IOException localIOException)
      {
        throw DbException.convertIOException(localIOException, null);
      }
    }
  }
  
  public int getMemory()
  {
    if (this.small != null) {
      return this.small.length + 104;
    }
    return 140;
  }
  
  public ValueLob copyToTemp()
  {
    ValueLob localValueLob;
    if (this.type == 16) {
      localValueLob = createClob(getReader(), this.precision, this.handler);
    } else {
      localValueLob = createBlob(getInputStream(), this.precision, this.handler);
    }
    return localValueLob;
  }
  
  public Value convertPrecision(long paramLong, boolean paramBoolean)
  {
    if (this.precision <= paramLong) {
      return this;
    }
    ValueLob localValueLob;
    if (this.type == 16) {
      localValueLob = createClob(getReader(), paramLong, this.handler);
    } else {
      localValueLob = createBlob(getInputStream(), paramLong, this.handler);
    }
    return localValueLob;
  }
}
