package org.h2.store.fs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelOutputStream
  extends OutputStream
{
  private final FileChannel channel;
  private final byte[] buffer = { 0 };
  
  public FileChannelOutputStream(FileChannel paramFileChannel, boolean paramBoolean)
    throws IOException
  {
    this.channel = paramFileChannel;
    if (paramBoolean)
    {
      paramFileChannel.position(paramFileChannel.size());
    }
    else
    {
      paramFileChannel.position(0L);
      paramFileChannel.truncate(0L);
    }
  }
  
  public void write(int paramInt)
    throws IOException
  {
    this.buffer[0] = ((byte)paramInt);
    FileUtils.writeFully(this.channel, ByteBuffer.wrap(this.buffer));
  }
  
  public void write(byte[] paramArrayOfByte)
    throws IOException
  {
    FileUtils.writeFully(this.channel, ByteBuffer.wrap(paramArrayOfByte));
  }
  
  public void write(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    FileUtils.writeFully(this.channel, ByteBuffer.wrap(paramArrayOfByte, paramInt1, paramInt2));
  }
  
  public void close()
    throws IOException
  {
    this.channel.close();
  }
}
