package org.h2.mvstore;

import java.nio.ByteBuffer;

public class WriteBuffer
{
  private static final int MAX_REUSE_CAPACITY = 4194304;
  private static final int MIN_GROW = 1048576;
  private ByteBuffer reuse = ByteBuffer.allocate(1048576);
  private ByteBuffer buff = this.reuse;
  
  public WriteBuffer putVarInt(int paramInt)
  {
    DataUtils.writeVarInt(ensureCapacity(5), paramInt);
    return this;
  }
  
  public WriteBuffer putVarLong(long paramLong)
  {
    DataUtils.writeVarLong(ensureCapacity(10), paramLong);
    return this;
  }
  
  public WriteBuffer putStringData(String paramString, int paramInt)
  {
    ByteBuffer localByteBuffer = ensureCapacity(3 * paramInt);
    for (int i = 0; i < paramInt; i++)
    {
      int j = paramString.charAt(i);
      if (j < 128)
      {
        localByteBuffer.put((byte)j);
      }
      else if (j >= 2048)
      {
        localByteBuffer.put((byte)(0xE0 | j >> 12));
        localByteBuffer.put((byte)(j >> 6 & 0x3F));
        localByteBuffer.put((byte)(j & 0x3F));
      }
      else
      {
        localByteBuffer.put((byte)(0xC0 | j >> 6));
        localByteBuffer.put((byte)(j & 0x3F));
      }
    }
    return this;
  }
  
  public WriteBuffer put(byte paramByte)
  {
    ensureCapacity(1).put(paramByte);
    return this;
  }
  
  public WriteBuffer putChar(char paramChar)
  {
    ensureCapacity(2).putChar(paramChar);
    return this;
  }
  
  public WriteBuffer putShort(short paramShort)
  {
    ensureCapacity(2).putShort(paramShort);
    return this;
  }
  
  public WriteBuffer putInt(int paramInt)
  {
    ensureCapacity(4).putInt(paramInt);
    return this;
  }
  
  public WriteBuffer putLong(long paramLong)
  {
    ensureCapacity(8).putLong(paramLong);
    return this;
  }
  
  public WriteBuffer putFloat(float paramFloat)
  {
    ensureCapacity(4).putFloat(paramFloat);
    return this;
  }
  
  public WriteBuffer putDouble(double paramDouble)
  {
    ensureCapacity(8).putDouble(paramDouble);
    return this;
  }
  
  public WriteBuffer put(byte[] paramArrayOfByte)
  {
    ensureCapacity(paramArrayOfByte.length).put(paramArrayOfByte);
    return this;
  }
  
  public WriteBuffer put(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    ensureCapacity(paramInt2).put(paramArrayOfByte, paramInt1, paramInt2);
    return this;
  }
  
  public WriteBuffer put(ByteBuffer paramByteBuffer)
  {
    ensureCapacity(this.buff.remaining()).put(paramByteBuffer);
    return this;
  }
  
  public WriteBuffer limit(int paramInt)
  {
    ensureCapacity(paramInt - this.buff.position()).limit(paramInt);
    return this;
  }
  
  public int capacity()
  {
    return this.buff.capacity();
  }
  
  public WriteBuffer position(int paramInt)
  {
    this.buff.position(paramInt);
    return this;
  }
  
  public int limit()
  {
    return this.buff.limit();
  }
  
  public int position()
  {
    return this.buff.position();
  }
  
  public WriteBuffer get(byte[] paramArrayOfByte)
  {
    this.buff.get(paramArrayOfByte);
    return this;
  }
  
  public WriteBuffer putInt(int paramInt1, int paramInt2)
  {
    this.buff.putInt(paramInt1, paramInt2);
    return this;
  }
  
  public WriteBuffer putShort(int paramInt, short paramShort)
  {
    this.buff.putShort(paramInt, paramShort);
    return this;
  }
  
  public WriteBuffer clear()
  {
    if (this.buff.limit() > 4194304) {
      this.buff = this.reuse;
    } else if (this.buff != this.reuse) {
      this.reuse = this.buff;
    }
    this.buff.clear();
    return this;
  }
  
  public ByteBuffer getBuffer()
  {
    return this.buff;
  }
  
  private ByteBuffer ensureCapacity(int paramInt)
  {
    if (this.buff.remaining() < paramInt) {
      grow(paramInt);
    }
    return this.buff;
  }
  
  private void grow(int paramInt)
  {
    ByteBuffer localByteBuffer = this.buff;
    int i = paramInt - localByteBuffer.remaining();
    int j = Math.max(i, 1048576);
    
    j = Math.max(localByteBuffer.capacity() / 2, j);
    int k = localByteBuffer.capacity() + j;
    try
    {
      this.buff = ByteBuffer.allocate(k);
    }
    catch (OutOfMemoryError localOutOfMemoryError)
    {
      throw new OutOfMemoryError("Capacity: " + k);
    }
    localByteBuffer.flip();
    this.buff.put(localByteBuffer);
    if (k <= 4194304) {
      this.reuse = this.buff;
    }
  }
}
