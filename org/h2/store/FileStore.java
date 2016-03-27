package org.h2.store;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import org.h2.engine.Constants;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.security.SecureFileStore;
import org.h2.store.fs.FileUtils;
import org.h2.util.TempFileDeleter;

public class FileStore
{
  public static final int HEADER_LENGTH = 48;
  private static final String HEADER = "-- H2 0.5/B --      ".substring(0, 15) + "\n";
  protected String name;
  private final DataHandler handler;
  private FileChannel file;
  private long filePos;
  private long fileLength;
  private Reference<?> autoDeleteReference;
  private boolean checkedWriting = true;
  private final String mode;
  private FileLock lock;
  
  protected FileStore(DataHandler paramDataHandler, String paramString1, String paramString2)
  {
    this.handler = paramDataHandler;
    this.name = paramString1;
    try
    {
      boolean bool = FileUtils.exists(paramString1);
      if ((bool) && (!FileUtils.canWrite(paramString1))) {
        paramString2 = "r";
      } else {
        FileUtils.createDirectories(FileUtils.getParent(paramString1));
      }
      this.file = FileUtils.open(paramString1, paramString2);
      if (bool) {
        this.fileLength = this.file.size();
      }
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, "name: " + paramString1 + " mode: " + paramString2);
    }
    this.mode = paramString2;
  }
  
  public static FileStore open(DataHandler paramDataHandler, String paramString1, String paramString2)
  {
    return open(paramDataHandler, paramString1, paramString2, null, null, 0);
  }
  
  public static FileStore open(DataHandler paramDataHandler, String paramString1, String paramString2, String paramString3, byte[] paramArrayOfByte)
  {
    return open(paramDataHandler, paramString1, paramString2, paramString3, paramArrayOfByte, 1024);
  }
  
  public static FileStore open(DataHandler paramDataHandler, String paramString1, String paramString2, String paramString3, byte[] paramArrayOfByte, int paramInt)
  {
    Object localObject;
    if (paramString3 == null) {
      localObject = new FileStore(paramDataHandler, paramString1, paramString2);
    } else {
      localObject = new SecureFileStore(paramDataHandler, paramString1, paramString2, paramString3, paramArrayOfByte, paramInt);
    }
    return (FileStore)localObject;
  }
  
  protected byte[] generateSalt()
  {
    return HEADER.getBytes(Constants.UTF8);
  }
  
  protected void initKey(byte[] paramArrayOfByte) {}
  
  public void setCheckedWriting(boolean paramBoolean)
  {
    this.checkedWriting = paramBoolean;
  }
  
  private void checkWritingAllowed()
  {
    if ((this.handler != null) && (this.checkedWriting)) {
      this.handler.checkWritingAllowed();
    }
  }
  
  private void checkPowerOff()
  {
    if (this.handler != null) {
      this.handler.checkPowerOff();
    }
  }
  
  public void init()
  {
    int i = 16;
    
    byte[] arrayOfByte2 = HEADER.getBytes(Constants.UTF8);
    byte[] arrayOfByte1;
    if (length() < 48L)
    {
      this.checkedWriting = false;
      writeDirect(arrayOfByte2, 0, i);
      arrayOfByte1 = generateSalt();
      writeDirect(arrayOfByte1, 0, i);
      initKey(arrayOfByte1);
      
      write(arrayOfByte2, 0, i);
      this.checkedWriting = true;
    }
    else
    {
      seek(0L);
      byte[] arrayOfByte3 = new byte[i];
      readFullyDirect(arrayOfByte3, 0, i);
      if (!Arrays.equals(arrayOfByte3, arrayOfByte2)) {
        throw DbException.get(90048, this.name);
      }
      arrayOfByte1 = new byte[i];
      readFullyDirect(arrayOfByte1, 0, i);
      initKey(arrayOfByte1);
      
      readFully(arrayOfByte3, 0, 16);
      if (!Arrays.equals(arrayOfByte3, arrayOfByte2)) {
        throw DbException.get(90049, this.name);
      }
    }
  }
  
  public void close()
  {
    if (this.file != null) {
      try
      {
        trace("close", this.name, this.file);
        this.file.close();
      }
      catch (IOException localIOException)
      {
        throw DbException.convertIOException(localIOException, this.name);
      }
      finally
      {
        this.file = null;
      }
    }
  }
  
  public void closeSilently()
  {
    try
    {
      close();
    }
    catch (Exception localException) {}
  }
  
  public void closeAndDeleteSilently()
  {
    if (this.file != null)
    {
      closeSilently();
      this.handler.getTempFileDeleter().deleteFile(this.autoDeleteReference, this.name);
      this.name = null;
    }
  }
  
  protected void readFullyDirect(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    readFully(paramArrayOfByte, paramInt1, paramInt2);
  }
  
  public void readFully(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    if ((SysProperties.CHECK) && ((paramInt2 < 0) || (paramInt2 % 16 != 0))) {
      DbException.throwInternalError("unaligned read " + this.name + " len " + paramInt2);
    }
    checkPowerOff();
    try
    {
      FileUtils.readFully(this.file, ByteBuffer.wrap(paramArrayOfByte, paramInt1, paramInt2));
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, this.name);
    }
    this.filePos += paramInt2;
  }
  
  public void seek(long paramLong)
  {
    if ((SysProperties.CHECK) && (paramLong % 16L != 0L)) {
      DbException.throwInternalError("unaligned seek " + this.name + " pos " + paramLong);
    }
    try
    {
      if (paramLong != this.filePos)
      {
        this.file.position(paramLong);
        this.filePos = paramLong;
      }
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, this.name);
    }
  }
  
  protected void writeDirect(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    write(paramArrayOfByte, paramInt1, paramInt2);
  }
  
  public void write(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    if ((SysProperties.CHECK) && ((paramInt2 < 0) || (paramInt2 % 16 != 0))) {
      DbException.throwInternalError("unaligned write " + this.name + " len " + paramInt2);
    }
    checkWritingAllowed();
    checkPowerOff();
    try
    {
      FileUtils.writeFully(this.file, ByteBuffer.wrap(paramArrayOfByte, paramInt1, paramInt2));
    }
    catch (IOException localIOException)
    {
      closeFileSilently();
      throw DbException.convertIOException(localIOException, this.name);
    }
    this.filePos += paramInt2;
    this.fileLength = Math.max(this.filePos, this.fileLength);
  }
  
  public void setLength(long paramLong)
  {
    if ((SysProperties.CHECK) && (paramLong % 16L != 0L)) {
      DbException.throwInternalError("unaligned setLength " + this.name + " pos " + paramLong);
    }
    checkPowerOff();
    checkWritingAllowed();
    try
    {
      if (paramLong > this.fileLength)
      {
        long l = this.filePos;
        this.file.position(paramLong - 1L);
        FileUtils.writeFully(this.file, ByteBuffer.wrap(new byte[1]));
        this.file.position(l);
      }
      else
      {
        this.file.truncate(paramLong);
      }
      this.fileLength = paramLong;
    }
    catch (IOException localIOException)
    {
      closeFileSilently();
      throw DbException.convertIOException(localIOException, this.name);
    }
  }
  
  public long length()
  {
    try
    {
      long l1 = this.fileLength;
      if (SysProperties.CHECK2)
      {
        l1 = this.file.size();
        if (l1 != this.fileLength) {
          DbException.throwInternalError("file " + this.name + " length " + l1 + " expected " + this.fileLength);
        }
      }
      if ((SysProperties.CHECK2) && (l1 % 16L != 0L))
      {
        long l2 = l1 + 16L - l1 % 16L;
        
        this.file.truncate(l2);
        this.fileLength = l2;
        DbException.throwInternalError("unaligned file length " + this.name + " len " + l1);
      }
      return l1;
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, this.name);
    }
  }
  
  public long getFilePointer()
  {
    if (SysProperties.CHECK2) {
      try
      {
        if (this.file.position() != this.filePos) {
          DbException.throwInternalError();
        }
      }
      catch (IOException localIOException)
      {
        throw DbException.convertIOException(localIOException, this.name);
      }
    }
    return this.filePos;
  }
  
  public void sync()
  {
    try
    {
      this.file.force(true);
    }
    catch (IOException localIOException)
    {
      closeFileSilently();
      throw DbException.convertIOException(localIOException, this.name);
    }
  }
  
  public void autoDelete()
  {
    if (this.autoDeleteReference == null) {
      this.autoDeleteReference = this.handler.getTempFileDeleter().addFile(this.name, this);
    }
  }
  
  public void stopAutoDelete()
  {
    this.handler.getTempFileDeleter().stopAutoDelete(this.autoDeleteReference, this.name);
    this.autoDeleteReference = null;
  }
  
  public void closeFile()
    throws IOException
  {
    this.file.close();
    this.file = null;
  }
  
  private void closeFileSilently()
  {
    try
    {
      this.file.close();
    }
    catch (IOException localIOException) {}
  }
  
  public void openFile()
    throws IOException
  {
    if (this.file == null)
    {
      this.file = FileUtils.open(this.name, this.mode);
      this.file.position(this.filePos);
    }
  }
  
  private static void trace(String paramString1, String paramString2, Object paramObject)
  {
    if (SysProperties.TRACE_IO) {
      System.out.println("FileStore." + paramString1 + " " + paramString2 + " " + paramObject);
    }
  }
  
  public synchronized boolean tryLock()
  {
    try
    {
      this.lock = this.file.tryLock();
      return this.lock != null;
    }
    catch (Exception localException) {}
    return false;
  }
  
  public synchronized void releaseLock()
  {
    if ((this.file != null) && (this.lock != null))
    {
      try
      {
        this.lock.release();
      }
      catch (Exception localException) {}
      this.lock = null;
    }
  }
}
