package org.h2.store.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public abstract class FileBase
  extends FileChannel
{
  public abstract long size()
    throws IOException;
  
  public abstract long position()
    throws IOException;
  
  public abstract FileChannel position(long paramLong)
    throws IOException;
  
  public abstract int read(ByteBuffer paramByteBuffer)
    throws IOException;
  
  public abstract int write(ByteBuffer paramByteBuffer)
    throws IOException;
  
  public synchronized int read(ByteBuffer paramByteBuffer, long paramLong)
    throws IOException
  {
    long l = position();
    position(paramLong);
    int i = read(paramByteBuffer);
    position(l);
    return i;
  }
  
  public synchronized int write(ByteBuffer paramByteBuffer, long paramLong)
    throws IOException
  {
    long l = position();
    position(paramLong);
    int i = write(paramByteBuffer);
    position(l);
    return i;
  }
  
  public abstract FileChannel truncate(long paramLong)
    throws IOException;
  
  public void force(boolean paramBoolean)
    throws IOException
  {}
  
  protected void implCloseChannel()
    throws IOException
  {}
  
  public FileLock lock(long paramLong1, long paramLong2, boolean paramBoolean)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }
  
  public MappedByteBuffer map(FileChannel.MapMode paramMapMode, long paramLong1, long paramLong2)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }
  
  public long read(ByteBuffer[] paramArrayOfByteBuffer, int paramInt1, int paramInt2)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }
  
  public long transferFrom(ReadableByteChannel paramReadableByteChannel, long paramLong1, long paramLong2)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }
  
  public long transferTo(long paramLong1, long paramLong2, WritableByteChannel paramWritableByteChannel)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }
  
  public FileLock tryLock(long paramLong1, long paramLong2, boolean paramBoolean)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }
  
  public long write(ByteBuffer[] paramArrayOfByteBuffer, int paramInt1, int paramInt2)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }
}
