package org.h2.util;

import java.io.IOException;
import java.io.InputStream;

public class AutoCloseInputStream
  extends InputStream
{
  private final InputStream in;
  private boolean closed;
  
  public AutoCloseInputStream(InputStream paramInputStream)
  {
    this.in = paramInputStream;
  }
  
  private int autoClose(int paramInt)
    throws IOException
  {
    if (paramInt < 0) {
      close();
    }
    return paramInt;
  }
  
  public void close()
    throws IOException
  {
    if (!this.closed)
    {
      this.in.close();
      this.closed = true;
    }
  }
  
  public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    return this.closed ? -1 : autoClose(this.in.read(paramArrayOfByte, paramInt1, paramInt2));
  }
  
  public int read(byte[] paramArrayOfByte)
    throws IOException
  {
    return this.closed ? -1 : autoClose(this.in.read(paramArrayOfByte));
  }
  
  public int read()
    throws IOException
  {
    return this.closed ? -1 : autoClose(this.in.read());
  }
}
