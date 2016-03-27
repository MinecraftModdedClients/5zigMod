package org.h2.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import org.h2.engine.Constants;

public class CountingReaderInputStream
  extends InputStream
{
  private final Reader reader;
  private final CharBuffer charBuffer = CharBuffer.allocate(4096);
  private final CharsetEncoder encoder = Constants.UTF8.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
  private ByteBuffer byteBuffer = ByteBuffer.allocate(0);
  private long length;
  private long remaining;
  
  CountingReaderInputStream(Reader paramReader, long paramLong)
  {
    this.reader = paramReader;
    this.remaining = paramLong;
  }
  
  public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    if (!fetch()) {
      return -1;
    }
    paramInt2 = Math.min(paramInt2, this.byteBuffer.remaining());
    this.byteBuffer.get(paramArrayOfByte, paramInt1, paramInt2);
    return paramInt2;
  }
  
  public int read()
    throws IOException
  {
    if (!fetch()) {
      return -1;
    }
    return this.byteBuffer.get() & 0xFF;
  }
  
  private boolean fetch()
    throws IOException
  {
    if ((this.byteBuffer != null) && (this.byteBuffer.remaining() == 0)) {
      fillBuffer();
    }
    return this.byteBuffer != null;
  }
  
  private void fillBuffer()
    throws IOException
  {
    int i = (int)Math.min(this.charBuffer.capacity() - this.charBuffer.position(), this.remaining);
    if (i > 0) {
      i = this.reader.read(this.charBuffer.array(), this.charBuffer.position(), i);
    }
    if (i > 0)
    {
      this.remaining -= i;
    }
    else
    {
      i = 0;
      this.remaining = 0L;
    }
    this.length += i;
    this.charBuffer.limit(this.charBuffer.position() + i);
    this.charBuffer.rewind();
    this.byteBuffer = ByteBuffer.allocate(4096);
    boolean bool = this.remaining == 0L;
    this.encoder.encode(this.charBuffer, this.byteBuffer, bool);
    if ((bool) && (this.byteBuffer.position() == 0))
    {
      this.byteBuffer = null;
      return;
    }
    this.byteBuffer.flip();
    this.charBuffer.compact();
    this.charBuffer.flip();
    this.charBuffer.position(this.charBuffer.limit());
  }
  
  public long getLength()
  {
    return this.length;
  }
  
  public void close()
    throws IOException
  {
    this.reader.close();
  }
}
