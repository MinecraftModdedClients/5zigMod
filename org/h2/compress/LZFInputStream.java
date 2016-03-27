package org.h2.compress;

import java.io.IOException;
import java.io.InputStream;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils;

public class LZFInputStream
  extends InputStream
{
  private final InputStream in;
  private CompressLZF decompress = new CompressLZF();
  private int pos;
  private int bufferLength;
  private byte[] inBuffer;
  private byte[] buffer;
  
  public LZFInputStream(InputStream paramInputStream)
    throws IOException
  {
    this.in = paramInputStream;
    if (readInt() != 1211255123) {
      throw new IOException("Not an LZFInputStream");
    }
  }
  
  private static byte[] ensureSize(byte[] paramArrayOfByte, int paramInt)
  {
    return (paramArrayOfByte == null) || (paramArrayOfByte.length < paramInt) ? DataUtils.newBytes(paramInt) : paramArrayOfByte;
  }
  
  private void fillBuffer()
    throws IOException
  {
    if ((this.buffer != null) && (this.pos < this.bufferLength)) {
      return;
    }
    int i = readInt();
    if (this.decompress == null)
    {
      this.bufferLength = 0;
    }
    else if (i < 0)
    {
      i = -i;
      this.buffer = ensureSize(this.buffer, i);
      readFully(this.buffer, i);
      this.bufferLength = i;
    }
    else
    {
      this.inBuffer = ensureSize(this.inBuffer, i);
      int j = readInt();
      readFully(this.inBuffer, i);
      this.buffer = ensureSize(this.buffer, j);
      try
      {
        this.decompress.expand(this.inBuffer, 0, i, this.buffer, 0, j);
      }
      catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException)
      {
        DbException.convertToIOException(localArrayIndexOutOfBoundsException);
      }
      this.bufferLength = j;
    }
    this.pos = 0;
  }
  
  private void readFully(byte[] paramArrayOfByte, int paramInt)
    throws IOException
  {
    int i = 0;
    while (paramInt > 0)
    {
      int j = this.in.read(paramArrayOfByte, i, paramInt);
      paramInt -= j;
      i += j;
    }
  }
  
  private int readInt()
    throws IOException
  {
    int i = this.in.read();
    if (i < 0)
    {
      this.decompress = null;
      return 0;
    }
    i = (i << 24) + (this.in.read() << 16) + (this.in.read() << 8) + this.in.read();
    return i;
  }
  
  public int read()
    throws IOException
  {
    fillBuffer();
    if (this.pos >= this.bufferLength) {
      return -1;
    }
    return this.buffer[(this.pos++)] & 0xFF;
  }
  
  public int read(byte[] paramArrayOfByte)
    throws IOException
  {
    return read(paramArrayOfByte, 0, paramArrayOfByte.length);
  }
  
  public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    if (paramInt2 == 0) {
      return 0;
    }
    int i = 0;
    while (paramInt2 > 0)
    {
      int j = readBlock(paramArrayOfByte, paramInt1, paramInt2);
      if (j < 0) {
        break;
      }
      i += j;
      paramInt1 += j;
      paramInt2 -= j;
    }
    return i == 0 ? -1 : i;
  }
  
  private int readBlock(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    fillBuffer();
    if (this.pos >= this.bufferLength) {
      return -1;
    }
    int i = Math.min(paramInt2, this.bufferLength - this.pos);
    i = Math.min(i, paramArrayOfByte.length - paramInt1);
    System.arraycopy(this.buffer, this.pos, paramArrayOfByte, paramInt1, i);
    this.pos += i;
    return i;
  }
  
  public void close()
    throws IOException
  {
    this.in.close();
  }
}
