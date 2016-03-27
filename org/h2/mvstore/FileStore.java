package org.h2.mvstore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import org.h2.mvstore.cache.FilePathCache;
import org.h2.store.fs.FilePath;
import org.h2.store.fs.FilePathDisk;
import org.h2.store.fs.FilePathEncrypt;
import org.h2.store.fs.FilePathEncrypt.FileEncrypt;
import org.h2.store.fs.FilePathNio;

public class FileStore
{
  protected long readCount;
  protected long readBytes;
  protected long writeCount;
  protected long writeBytes;
  protected final FreeSpaceBitSet freeSpace = new FreeSpaceBitSet(2, 4096);
  protected String fileName;
  protected boolean readOnly;
  protected long fileSize;
  protected FileChannel file;
  protected FileChannel encryptedFile;
  protected FileLock fileLock;
  
  public String toString()
  {
    return this.fileName;
  }
  
  public ByteBuffer readFully(long paramLong, int paramInt)
  {
    ByteBuffer localByteBuffer = ByteBuffer.allocate(paramInt);
    DataUtils.readFully(this.file, paramLong, localByteBuffer);
    this.readCount += 1L;
    this.readBytes += paramInt;
    return localByteBuffer;
  }
  
  public void writeFully(long paramLong, ByteBuffer paramByteBuffer)
  {
    int i = paramByteBuffer.remaining();
    this.fileSize = Math.max(this.fileSize, paramLong + i);
    DataUtils.writeFully(this.file, paramLong, paramByteBuffer);
    this.writeCount += 1L;
    this.writeBytes += i;
  }
  
  public void open(String paramString, boolean paramBoolean, char[] paramArrayOfChar)
  {
    if (this.file != null) {
      return;
    }
    if ((paramString != null) && 
      ((FilePath.get(paramString) instanceof FilePathDisk)))
    {
      FilePathNio.class.getName();
      paramString = "nio:" + paramString;
    }
    this.fileName = paramString;
    FilePath localFilePath1 = FilePath.get(paramString);
    FilePath localFilePath2 = localFilePath1.getParent();
    if ((localFilePath2 != null) && (!localFilePath2.exists())) {
      throw DataUtils.newIllegalArgumentException("Directory does not exist: {0}", new Object[] { localFilePath2 });
    }
    if ((localFilePath1.exists()) && (!localFilePath1.canWrite())) {
      paramBoolean = true;
    }
    this.readOnly = paramBoolean;
    try
    {
      this.file = localFilePath1.open(paramBoolean ? "r" : "rw");
      if (paramArrayOfChar != null)
      {
        byte[] arrayOfByte = FilePathEncrypt.getPasswordBytes(paramArrayOfChar);
        this.encryptedFile = this.file;
        this.file = new FilePathEncrypt.FileEncrypt(paramString, arrayOfByte, this.file);
      }
      this.file = FilePathCache.wrap(this.file);
      try
      {
        if (paramBoolean) {
          this.fileLock = this.file.tryLock(0L, Long.MAX_VALUE, true);
        } else {
          this.fileLock = this.file.tryLock();
        }
      }
      catch (OverlappingFileLockException localOverlappingFileLockException)
      {
        throw DataUtils.newIllegalStateException(7, "The file is locked: {0}", new Object[] { paramString, localOverlappingFileLockException });
      }
      if (this.fileLock == null) {
        throw DataUtils.newIllegalStateException(7, "The file is locked: {0}", new Object[] { paramString });
      }
      this.fileSize = this.file.size();
    }
    catch (IOException localIOException)
    {
      throw DataUtils.newIllegalStateException(1, "Could not open file {0}", new Object[] { paramString, localIOException });
    }
  }
  
  public void close()
  {
    try
    {
      if (this.fileLock != null)
      {
        this.fileLock.release();
        this.fileLock = null;
      }
      this.file.close();
      this.freeSpace.clear();
    }
    catch (Exception localException)
    {
      throw DataUtils.newIllegalStateException(2, "Closing failed for file {0}", new Object[] { this.fileName, localException });
    }
    finally
    {
      this.file = null;
    }
  }
  
  public void sync()
  {
    try
    {
      this.file.force(true);
    }
    catch (IOException localIOException)
    {
      throw DataUtils.newIllegalStateException(2, "Could not sync file {0}", new Object[] { this.fileName, localIOException });
    }
  }
  
  public long size()
  {
    return this.fileSize;
  }
  
  public void truncate(long paramLong)
  {
    try
    {
      this.writeCount += 1L;
      this.file.truncate(paramLong);
      this.fileSize = Math.min(this.fileSize, paramLong);
    }
    catch (IOException localIOException)
    {
      throw DataUtils.newIllegalStateException(2, "Could not truncate file {0} to size {1}", new Object[] { this.fileName, Long.valueOf(paramLong), localIOException });
    }
  }
  
  public FileChannel getFile()
  {
    return this.file;
  }
  
  public FileChannel getEncryptedFile()
  {
    return this.encryptedFile;
  }
  
  public long getWriteCount()
  {
    return this.writeCount;
  }
  
  public long getWriteBytes()
  {
    return this.writeBytes;
  }
  
  public long getReadCount()
  {
    return this.readCount;
  }
  
  public long getReadBytes()
  {
    return this.readBytes;
  }
  
  public boolean isReadOnly()
  {
    return this.readOnly;
  }
  
  public int getDefaultRetentionTime()
  {
    return 45000;
  }
  
  public void markUsed(long paramLong, int paramInt)
  {
    this.freeSpace.markUsed(paramLong, paramInt);
  }
  
  public long allocate(int paramInt)
  {
    return this.freeSpace.allocate(paramInt);
  }
  
  public void free(long paramLong, int paramInt)
  {
    this.freeSpace.free(paramLong, paramInt);
  }
  
  public int getFillRate()
  {
    return this.freeSpace.getFillRate();
  }
  
  long getFirstFree()
  {
    return this.freeSpace.getFirstFree();
  }
  
  public void clear()
  {
    this.freeSpace.clear();
  }
  
  public String getFileName()
  {
    return this.fileName;
  }
}
