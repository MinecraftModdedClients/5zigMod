package org.h2.store.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelInputStream
  extends InputStream
{
  private final FileChannel channel;
  private final boolean closeChannel;
  private ByteBuffer buffer;
  private long pos;
  
  public FileChannelInputStream(FileChannel paramFileChannel, boolean paramBoolean)
  {
    this.channel = paramFileChannel;
    this.closeChannel = paramBoolean;
  }
  
  public int read()
    throws IOException
  {
    if (this.buffer == null) {
      this.buffer = ByteBuffer.allocate(1);
    }
    this.buffer.rewind();
    int i = this.channel.read(this.buffer, this.pos++);
    if (i < 0) {
      return -1;
    }
    return this.buffer.get(0) & 0xFF;
  }
  
  public int read(byte[] paramArrayOfByte)
    throws IOException
  {
    return read(paramArrayOfByte, 0, paramArrayOfByte.length);
  }
  
  public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    ByteBuffer localByteBuffer = ByteBuffer.wrap(paramArrayOfByte, paramInt1, paramInt2);
    int i = this.channel.read(localByteBuffer, this.pos);
    if (i == -1) {
      return -1;
    }
    this.pos += i;
    return i;
  }
  
  public void close()
    throws IOException
  {
    if (this.closeChannel) {
      this.channel.close();
    }
  }
}
