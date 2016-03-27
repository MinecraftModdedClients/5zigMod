package org.h2.util;

public final class BitField
{
  private static final int ADDRESS_BITS = 6;
  private static final int BITS = 64;
  private static final int ADDRESS_MASK = 63;
  private long[] data;
  private int maxLength;
  
  public BitField()
  {
    this(64);
  }
  
  public BitField(int paramInt)
  {
    this.data = new long[paramInt >>> 3];
  }
  
  public int nextClearBit(int paramInt)
  {
    int i = paramInt >> 6;
    int j = this.data.length;
    for (; i < j; i++) {
      if (this.data[i] != -1L)
      {
        int k = Math.max(paramInt, i << 6);
        for (int m = k + 64; k < m; k++) {
          if (!get(k)) {
            return k;
          }
        }
      }
    }
    return j << 6;
  }
  
  public boolean get(int paramInt)
  {
    int i = paramInt >> 6;
    if (i >= this.data.length) {
      return false;
    }
    return (this.data[i] & getBitMask(paramInt)) != 0L;
  }
  
  public int getByte(int paramInt)
  {
    int i = paramInt >> 6;
    if (i >= this.data.length) {
      return 0;
    }
    return (int)(this.data[i] >>> (paramInt & 0x38) & 0xFF);
  }
  
  public void setByte(int paramInt1, int paramInt2)
  {
    int i = paramInt1 >> 6;
    checkCapacity(i);
    this.data[i] |= paramInt2 << (paramInt1 & 0x38);
    if ((this.maxLength < paramInt1) && (paramInt2 != 0)) {
      this.maxLength = (paramInt1 + 7);
    }
  }
  
  public void set(int paramInt)
  {
    int i = paramInt >> 6;
    checkCapacity(i);
    this.data[i] |= getBitMask(paramInt);
    if (this.maxLength < paramInt) {
      this.maxLength = paramInt;
    }
  }
  
  public void clear(int paramInt)
  {
    int i = paramInt >> 6;
    if (i >= this.data.length) {
      return;
    }
    this.data[i] &= (getBitMask(paramInt) ^ 0xFFFFFFFFFFFFFFFF);
  }
  
  private static long getBitMask(int paramInt)
  {
    return 1L << (paramInt & 0x3F);
  }
  
  private void checkCapacity(int paramInt)
  {
    if (paramInt >= this.data.length) {
      expandCapacity(paramInt);
    }
  }
  
  private void expandCapacity(int paramInt)
  {
    while (paramInt >= this.data.length)
    {
      int i = this.data.length == 0 ? 1 : this.data.length * 2;
      long[] arrayOfLong = new long[i];
      System.arraycopy(this.data, 0, arrayOfLong, 0, this.data.length);
      this.data = arrayOfLong;
    }
  }
  
  public void set(int paramInt1, int paramInt2, boolean paramBoolean)
  {
    for (int i = paramInt2 - 1; i >= paramInt1; i--) {
      set(i, paramBoolean);
    }
    if (paramBoolean)
    {
      if (paramInt2 > this.maxLength) {
        this.maxLength = paramInt2;
      }
    }
    else if (paramInt2 >= this.maxLength) {
      this.maxLength = paramInt1;
    }
  }
  
  private void set(int paramInt, boolean paramBoolean)
  {
    if (paramBoolean) {
      set(paramInt);
    } else {
      clear(paramInt);
    }
  }
  
  public int length()
  {
    int i = this.maxLength >> 6;
    while ((i > 0) && (this.data[i] == 0L)) {
      i--;
    }
    this.maxLength = ((i << 6) + (64 - Long.numberOfLeadingZeros(this.data[i])));
    
    return this.maxLength;
  }
}
