package org.h2.store.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

class FileRec
  extends FileBase
{
  private final FilePathRec rec;
  private final FileChannel channel;
  private final String name;
  
  FileRec(FilePathRec paramFilePathRec, FileChannel paramFileChannel, String paramString)
  {
    this.rec = paramFilePathRec;
    this.channel = paramFileChannel;
    this.name = paramString;
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
  
  public int read(ByteBuffer paramByteBuffer, long paramLong)
    throws IOException
  {
    return this.channel.read(paramByteBuffer, paramLong);
  }
  
  public FileChannel position(long paramLong)
    throws IOException
  {
    this.channel.position(paramLong);
    return this;
  }
  
  public FileChannel truncate(long paramLong)
    throws IOException
  {
    this.rec.log(7, this.name, null, paramLong);
    this.channel.truncate(paramLong);
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
    Object localObject = paramByteBuffer.array();
    int i = paramByteBuffer.remaining();
    if ((paramByteBuffer.position() != 0) || (i != localObject.length))
    {
      byte[] arrayOfByte = new byte[i];
      System.arraycopy(localObject, paramByteBuffer.arrayOffset() + paramByteBuffer.position(), arrayOfByte, 0, i);
      localObject = arrayOfByte;
    }
    int j = this.channel.write(paramByteBuffer);
    this.rec.log(8, this.name, (byte[])localObject, this.channel.position());
    return j;
  }
  
  public int write(ByteBuffer paramByteBuffer, long paramLong)
    throws IOException
  {
    Object localObject = paramByteBuffer.array();
    int i = paramByteBuffer.remaining();
    if ((paramByteBuffer.position() != 0) || (i != localObject.length))
    {
      byte[] arrayOfByte = new byte[i];
      System.arraycopy(localObject, paramByteBuffer.arrayOffset() + paramByteBuffer.position(), arrayOfByte, 0, i);
      localObject = arrayOfByte;
    }
    int j = this.channel.write(paramByteBuffer, paramLong);
    this.rec.log(8, this.name, (byte[])localObject, paramLong);
    return j;
  }
  
  public synchronized FileLock tryLock(long paramLong1, long paramLong2, boolean paramBoolean)
    throws IOException
  {
    return this.channel.tryLock(paramLong1, paramLong2, paramBoolean);
  }
  
  public String toString()
  {
    return this.name;
  }
}
