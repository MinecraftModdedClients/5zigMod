package org.h2.store;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import org.h2.util.IOUtils;

public class DataReader
  extends Reader
{
  private final InputStream in;
  
  public DataReader(InputStream paramInputStream)
  {
    this.in = paramInputStream;
  }
  
  public byte readByte()
    throws IOException
  {
    int i = this.in.read();
    if (i < 0) {
      throw new FastEOFException();
    }
    return (byte)i;
  }
  
  public int readVarInt()
    throws IOException
  {
    int i = readByte();
    if (i >= 0) {
      return i;
    }
    int j = i & 0x7F;
    i = readByte();
    if (i >= 0) {
      return j | i << 7;
    }
    j |= (i & 0x7F) << 7;
    i = readByte();
    if (i >= 0) {
      return j | i << 14;
    }
    j |= (i & 0x7F) << 14;
    i = readByte();
    if (i >= 0) {
      return j | i << 21;
    }
    return j | (i & 0x7F) << 21 | readByte() << 28;
  }
  
  public long readVarLong()
    throws IOException
  {
    long l1 = readByte();
    if (l1 >= 0L) {
      return l1;
    }
    l1 &= 0x7F;
    for (int i = 7;; i += 7)
    {
      long l2 = readByte();
      l1 |= (l2 & 0x7F) << i;
      if (l2 >= 0L) {
        return l1;
      }
    }
  }
  
  public void readFully(byte[] paramArrayOfByte, int paramInt)
    throws IOException
  {
    int i = IOUtils.readFully(this.in, paramArrayOfByte, paramInt);
    if (i < paramInt) {
      throw new FastEOFException();
    }
  }
  
  public String readString()
    throws IOException
  {
    int i = readVarInt();
    return readString(i);
  }
  
  private String readString(int paramInt)
    throws IOException
  {
    char[] arrayOfChar = new char[paramInt];
    for (int i = 0; i < paramInt; i++) {
      arrayOfChar[i] = readChar();
    }
    return new String(arrayOfChar);
  }
  
  private char readChar()
    throws IOException
  {
    int i = readByte() & 0xFF;
    if (i < 128) {
      return (char)i;
    }
    if (i >= 224) {
      return (char)(((i & 0xF) << 12) + ((readByte() & 0x3F) << 6) + (readByte() & 0x3F));
    }
    return (char)(((i & 0x1F) << 6) + (readByte() & 0x3F));
  }
  
  public void close()
    throws IOException
  {}
  
  public int read(char[] paramArrayOfChar, int paramInt1, int paramInt2)
    throws IOException
  {
    int i = 0;
    try
    {
      for (; i < paramInt2; i++) {
        paramArrayOfChar[i] = readChar();
      }
      return paramInt2;
    }
    catch (EOFException localEOFException) {}
    return i;
  }
  
  static class FastEOFException
    extends EOFException
  {
    private static final long serialVersionUID = 1L;
    
    public synchronized Throwable fillInStackTrace()
    {
      return null;
    }
  }
}
