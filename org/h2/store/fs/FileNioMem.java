package org.h2.store.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;

class FileNioMem
  extends FileBase
{
  final FileNioMemData data;
  private final boolean readOnly;
  private long pos;
  
  FileNioMem(FileNioMemData paramFileNioMemData, boolean paramBoolean)
  {
    this.data = paramFileNioMemData;
    this.readOnly = paramBoolean;
  }
  
  public long size()
  {
    return this.data.length();
  }
  
  public FileChannel truncate(long paramLong)
    throws IOException
  {
    if (this.readOnly) {
      throw new NonWritableChannelException();
    }
    if (paramLong < size())
    {
      this.data.touch(this.readOnly);
      this.pos = Math.min(this.pos, paramLong);
      this.data.truncate(paramLong);
    }
    return this;
  }
  
  public FileChannel position(long paramLong)
  {
    this.pos = ((int)paramLong);
    return this;
  }
  
  public int write(ByteBuffer paramByteBuffer)
    throws IOException
  {
    int i = paramByteBuffer.remaining();
    if (i == 0) {
      return 0;
    }
    this.data.touch(this.readOnly);
    
    this.pos = this.data.readWrite(this.pos, paramByteBuffer, 0, i, true);
    paramByteBuffer.position(paramByteBuffer.position() + i);
    return i;
  }
  
  public int read(ByteBuffer paramByteBuffer)
    throws IOException
  {
    int i = paramByteBuffer.remaining();
    if (i == 0) {
      return 0;
    }
    long l = this.data.readWrite(this.pos, paramByteBuffer, paramByteBuffer.position(), i, false);
    i = (int)(l - this.pos);
    if (i <= 0) {
      return -1;
    }
    paramByteBuffer.position(paramByteBuffer.position() + i);
    this.pos = l;
    return i;
  }
  
  public long position()
  {
    return this.pos;
  }
  
  public void implCloseChannel()
    throws IOException
  {
    this.pos = 0L;
  }
  
  public void force(boolean paramBoolean)
    throws IOException
  {}
  
  public synchronized FileLock tryLock(long paramLong1, long paramLong2, boolean paramBoolean)
    throws IOException
  {
    if (paramBoolean)
    {
      if (!this.data.lockShared()) {
        return null;
      }
    }
    else if (!this.data.lockExclusive()) {
      return null;
    }
    FileLock local1 = new FileLock((FileChannel)null, paramLong1, paramLong2, paramBoolean)
    {
      public boolean isValid()
      {
        return true;
      }
      
      public void release()
        throws IOException
      {
        FileNioMem.this.data.unlock();
      }
    };
    return local1;
  }
  
  public String toString()
  {
    return this.data.getName();
  }
}
