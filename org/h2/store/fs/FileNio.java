package org.h2.store.fs;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;

class FileNio
  extends FileBase
{
  private final String name;
  private final FileChannel channel;
  
  FileNio(String paramString1, String paramString2)
    throws IOException
  {
    this.name = paramString1;
    this.channel = new RandomAccessFile(paramString1, paramString2).getChannel();
  }
  
  public void implCloseChannel()
    throws IOException
  {
    this.channel.close();
  }
  
  public long position()
    throws IOException
  {
    return this.channel.position();
  }
  
  public long size()
    throws IOException
  {
    return this.channel.size();
  }
  
  public int read(ByteBuffer paramByteBuffer)
    throws IOException
  {
    return this.channel.read(paramByteBuffer);
  }
  
  public FileChannel position(long paramLong)
    throws IOException
  {
    this.channel.position(paramLong);
    return this;
  }
  
  public int read(ByteBuffer paramByteBuffer, long paramLong)
    throws IOException
  {
    return this.channel.read(paramByteBuffer, paramLong);
  }
  
  public int write(ByteBuffer paramByteBuffer, long paramLong)
    throws IOException
  {
    return this.channel.write(paramByteBuffer, paramLong);
  }
  
  public FileChannel truncate(long paramLong)
    throws IOException
  {
    long l1 = this.channel.size();
    if (paramLong < l1)
    {
      long l2 = this.channel.position();
      this.channel.truncate(paramLong);
      long l3 = this.channel.position();
      if (l2 < paramLong)
      {
        if (l3 != l2) {
          this.channel.position(l2);
        }
      }
      else if (l3 > paramLong) {
        this.channel.position(paramLong);
      }
    }
    return this;
  }
  
  public void force(boolean paramBoolean)
    throws IOException
  {
    this.channel.force(paramBoolean);
  }
  
  public int write(ByteBuffer paramByteBuffer)
    throws IOException
  {
    try
    {
      return this.channel.write(paramByteBuffer);
    }
    catch (NonWritableChannelException localNonWritableChannelException)
    {
      throw new IOException("read only");
    }
  }
  
  public synchronized FileLock tryLock(long paramLong1, long paramLong2, boolean paramBoolean)
    throws IOException
  {
    return this.channel.tryLock(paramLong1, paramLong2, paramBoolean);
  }
  
  public String toString()
  {
    return "nio:" + this.name;
  }
}
