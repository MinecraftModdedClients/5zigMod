package org.h2.store.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import org.h2.message.DbException;

class FileSplit
  extends FileBase
{
  private final FilePathSplit file;
  private final String mode;
  private final long maxLength;
  private FileChannel[] list;
  private long filePointer;
  private long length;
  
  FileSplit(FilePathSplit paramFilePathSplit, String paramString, FileChannel[] paramArrayOfFileChannel, long paramLong1, long paramLong2)
  {
    this.file = paramFilePathSplit;
    this.mode = paramString;
    this.list = paramArrayOfFileChannel;
    this.length = paramLong1;
    this.maxLength = paramLong2;
  }
  
  public void implCloseChannel()
    throws IOException
  {
    for (FileChannel localFileChannel : this.list) {
      localFileChannel.close();
    }
  }
  
  public long position()
  {
    return this.filePointer;
  }
  
  public long size()
  {
    return this.length;
  }
  
  public int read(ByteBuffer paramByteBuffer)
    throws IOException
  {
    int i = paramByteBuffer.remaining();
    if (i == 0) {
      return 0;
    }
    i = (int)Math.min(i, this.length - this.filePointer);
    if (i <= 0) {
      return -1;
    }
    long l = this.filePointer % this.maxLength;
    i = (int)Math.min(i, this.maxLength - l);
    FileChannel localFileChannel = getFileChannel();
    localFileChannel.position(l);
    i = localFileChannel.read(paramByteBuffer);
    this.filePointer += i;
    return i;
  }
  
  public FileChannel position(long paramLong)
  {
    this.filePointer = paramLong;
    return this;
  }
  
  private FileChannel getFileChannel()
    throws IOException
  {
    int i = (int)(this.filePointer / this.maxLength);
    while (i >= this.list.length)
    {
      int j = this.list.length;
      FileChannel[] arrayOfFileChannel = new FileChannel[j + 1];
      System.arraycopy(this.list, 0, arrayOfFileChannel, 0, j);
      FilePath localFilePath = this.file.getBase(j);
      arrayOfFileChannel[j] = localFilePath.open(this.mode);
      this.list = arrayOfFileChannel;
    }
    return this.list[i];
  }
  
  public FileChannel truncate(long paramLong)
    throws IOException
  {
    if (paramLong >= this.length) {
      return this;
    }
    this.filePointer = Math.min(this.filePointer, paramLong);
    int i = 1 + (int)(paramLong / this.maxLength);
    if (i < this.list.length)
    {
      FileChannel[] arrayOfFileChannel = new FileChannel[i];
      for (int j = this.list.length - 1; j >= i; j--)
      {
        this.list[j].truncate(0L);
        this.list[j].close();
        try
        {
          this.file.getBase(j).delete();
        }
        catch (DbException localDbException)
        {
          throw DbException.convertToIOException(localDbException);
        }
      }
      System.arraycopy(this.list, 0, arrayOfFileChannel, 0, arrayOfFileChannel.length);
      this.list = arrayOfFileChannel;
    }
    long l = paramLong - this.maxLength * (i - 1);
    this.list[(this.list.length - 1)].truncate(l);
    this.length = paramLong;
    return this;
  }
  
  public void force(boolean paramBoolean)
    throws IOException
  {
    for (FileChannel localFileChannel : this.list) {
      localFileChannel.force(paramBoolean);
    }
  }
  
  public int write(ByteBuffer paramByteBuffer)
    throws IOException
  {
    if ((this.filePointer >= this.length) && (this.filePointer > this.maxLength))
    {
      l1 = this.filePointer;
      for (long l2 = this.length - this.length % this.maxLength + this.maxLength; l2 < this.filePointer; l2 += this.maxLength)
      {
        if (l2 > this.length)
        {
          position(l2 - 1L);
          write(ByteBuffer.wrap(new byte[1]));
        }
        this.filePointer = l1;
      }
    }
    long l1 = this.filePointer % this.maxLength;
    int i = paramByteBuffer.remaining();
    FileChannel localFileChannel = getFileChannel();
    localFileChannel.position(l1);
    int j = (int)Math.min(i, this.maxLength - l1);
    if (j == i)
    {
      j = localFileChannel.write(paramByteBuffer);
    }
    else
    {
      int k = paramByteBuffer.limit();
      paramByteBuffer.limit(paramByteBuffer.position() + j);
      j = localFileChannel.write(paramByteBuffer);
      paramByteBuffer.limit(k);
    }
    this.filePointer += j;
    this.length = Math.max(this.length, this.filePointer);
    return j;
  }
  
  public synchronized FileLock tryLock(long paramLong1, long paramLong2, boolean paramBoolean)
    throws IOException
  {
    return this.list[0].tryLock(paramLong1, paramLong2, paramBoolean);
  }
  
  public String toString()
  {
    return this.file.toString();
  }
}
