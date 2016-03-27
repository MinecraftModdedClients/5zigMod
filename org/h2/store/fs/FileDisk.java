package org.h2.store.fs;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import org.h2.engine.SysProperties;

class FileDisk
  extends FileBase
{
  private final RandomAccessFile file;
  private final String name;
  private final boolean readOnly;
  
  FileDisk(String paramString1, String paramString2)
    throws FileNotFoundException
  {
    this.file = new RandomAccessFile(paramString1, paramString2);
    this.name = paramString1;
    this.readOnly = paramString2.equals("r");
  }
  
  public void force(boolean paramBoolean)
    throws IOException
  {
    String str = SysProperties.SYNC_METHOD;
    if (!"".equals(str)) {
      if ("sync".equals(str)) {
        this.file.getFD().sync();
      } else if ("force".equals(str)) {
        this.file.getChannel().force(true);
      } else if ("forceFalse".equals(str)) {
        this.file.getChannel().force(false);
      } else {
        this.file.getFD().sync();
      }
    }
  }
  
  public FileChannel truncate(long paramLong)
    throws IOException
  {
    if (this.readOnly) {
      throw new NonWritableChannelException();
    }
    if (paramLong < this.file.length()) {
      this.file.setLength(paramLong);
    }
    return this;
  }
  
  public synchronized FileLock tryLock(long paramLong1, long paramLong2, boolean paramBoolean)
    throws IOException
  {
    return this.file.getChannel().tryLock(paramLong1, paramLong2, paramBoolean);
  }
  
  public void implCloseChannel()
    throws IOException
  {
    this.file.close();
  }
  
  public long position()
    throws IOException
  {
    return this.file.getFilePointer();
  }
  
  public long size()
    throws IOException
  {
    return this.file.length();
  }
  
  public int read(ByteBuffer paramByteBuffer)
    throws IOException
  {
    int i = this.file.read(paramByteBuffer.array(), paramByteBuffer.arrayOffset() + paramByteBuffer.position(), paramByteBuffer.remaining());
    if (i > 0) {
      paramByteBuffer.position(paramByteBuffer.position() + i);
    }
    return i;
  }
  
  public FileChannel position(long paramLong)
    throws IOException
  {
    this.file.seek(paramLong);
    return this;
  }
  
  public int write(ByteBuffer paramByteBuffer)
    throws IOException
  {
    int i = paramByteBuffer.remaining();
    this.file.write(paramByteBuffer.array(), paramByteBuffer.arrayOffset() + paramByteBuffer.position(), i);
    paramByteBuffer.position(paramByteBuffer.position() + i);
    return i;
  }
  
  public String toString()
  {
    return this.name;
  }
}
