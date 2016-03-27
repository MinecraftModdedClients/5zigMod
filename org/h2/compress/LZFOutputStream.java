package org.h2.compress;

import java.io.IOException;
import java.io.OutputStream;

public class LZFOutputStream
  extends OutputStream
{
  static final int MAGIC = 1211255123;
  private final OutputStream out;
  private final CompressLZF compress = new CompressLZF();
  private final byte[] buffer;
  private int pos;
  private byte[] outBuffer;
  
  public LZFOutputStream(OutputStream paramOutputStream)
    throws IOException
  {
    this.out = paramOutputStream;
    int i = 131072;
    this.buffer = new byte[i];
    ensureOutput(i);
    writeInt(1211255123);
  }
  
  private void ensureOutput(int paramInt)
  {
    int i = (paramInt < 100 ? paramInt + 100 : paramInt) * 2;
    if ((this.outBuffer == null) || (this.outBuffer.length < i)) {
      this.outBuffer = new byte[i];
    }
  }
  
  public void write(int paramInt)
    throws IOException
  {
    if (this.pos >= this.buffer.length) {
      flush();
    }
    this.buffer[(this.pos++)] = ((byte)paramInt);
  }
  
  private void compressAndWrite(byte[] paramArrayOfByte, int paramInt)
    throws IOException
  {
    if (paramInt > 0)
    {
      ensureOutput(paramInt);
      int i = this.compress.compress(paramArrayOfByte, paramInt, this.outBuffer, 0);
      if (i > paramInt)
      {
        writeInt(-paramInt);
        this.out.write(paramArrayOfByte, 0, paramInt);
      }
      else
      {
        writeInt(i);
        writeInt(paramInt);
        this.out.write(this.outBuffer, 0, i);
      }
    }
  }
  
  private void writeInt(int paramInt)
    throws IOException
  {
    this.out.write((byte)(paramInt >> 24));
    this.out.write((byte)(paramInt >> 16));
    this.out.write((byte)(paramInt >> 8));
    this.out.write((byte)paramInt);
  }
  
  public void write(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    while (paramInt2 > 0)
    {
      int i = Math.min(this.buffer.length - this.pos, paramInt2);
      System.arraycopy(paramArrayOfByte, paramInt1, this.buffer, this.pos, i);
      this.pos += i;
      if (this.pos >= this.buffer.length) {
        flush();
      }
      paramInt1 += i;
      paramInt2 -= i;
    }
  }
  
  public void flush()
    throws IOException
  {
    compressAndWrite(this.buffer, this.pos);
    this.pos = 0;
  }
  
  public void close()
    throws IOException
  {
    flush();
    this.out.close();
  }
}
