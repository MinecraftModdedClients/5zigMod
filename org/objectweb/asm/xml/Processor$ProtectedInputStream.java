package org.objectweb.asm.xml;

import java.io.IOException;
import java.io.InputStream;

final class Processor$ProtectedInputStream
  extends InputStream
{
  private final InputStream is;
  
  Processor$ProtectedInputStream(InputStream paramInputStream)
  {
    this.is = paramInputStream;
  }
  
  public final void close()
    throws IOException
  {}
  
  public final int read()
    throws IOException
  {
    return this.is.read();
  }
  
  public final int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    return this.is.read(paramArrayOfByte, paramInt1, paramInt2);
  }
  
  public final int available()
    throws IOException
  {
    return this.is.available();
  }
}
