package org.h2.mvstore;

import java.util.BitSet;
import org.h2.util.MathUtils;

public class FreeSpaceBitSet
{
  private static final boolean DETAILED_INFO = false;
  private final int firstFreeBlock;
  private final int blockSize;
  private final BitSet set = new BitSet();
  
  public FreeSpaceBitSet(int paramInt1, int paramInt2)
  {
    this.firstFreeBlock = paramInt1;
    this.blockSize = paramInt2;
    clear();
  }
  
  public void clear()
  {
    this.set.clear();
    this.set.set(0, this.firstFreeBlock);
  }
  
  public boolean isUsed(long paramLong, int paramInt)
  {
    int i = getBlock(paramLong);
    int j = getBlockCount(paramInt);
    for (int k = i; k < i + j; k++) {
      if (!this.set.get(k)) {
        return false;
      }
    }
    return true;
  }
  
  public boolean isFree(long paramLong, int paramInt)
  {
    int i = getBlock(paramLong);
    int j = getBlockCount(paramInt);
    for (int k = i; k < i + j; k++) {
      if (this.set.get(k)) {
        return false;
      }
    }
    return true;
  }
  
  public long allocate(int paramInt)
  {
    int i = getBlockCount(paramInt);
    int j = 0;
    for (;;)
    {
      int k = this.set.nextClearBit(j);
      int m = this.set.nextSetBit(k + 1);
      if ((m < 0) || (m - k >= i))
      {
        this.set.set(k, k + i);
        return getPos(k);
      }
      j = m;
    }
  }
  
  public void markUsed(long paramLong, int paramInt)
  {
    int i = getBlock(paramLong);
    int j = getBlockCount(paramInt);
    this.set.set(i, i + j);
  }
  
  public void free(long paramLong, int paramInt)
  {
    int i = getBlock(paramLong);
    int j = getBlockCount(paramInt);
    this.set.clear(i, i + j);
  }
  
  private long getPos(int paramInt)
  {
    return paramInt * this.blockSize;
  }
  
  private int getBlock(long paramLong)
  {
    return (int)(paramLong / this.blockSize);
  }
  
  private int getBlockCount(int paramInt)
  {
    return MathUtils.roundUpInt(paramInt, this.blockSize) / this.blockSize;
  }
  
  public int getFillRate()
  {
    int i = this.set.length();int j = 0;
    for (int k = 0; k < i; k++) {
      if (this.set.get(k)) {
        j++;
      }
    }
    if (j == 0) {
      return 0;
    }
    return Math.max(1, (int)(100L * j / i));
  }
  
  public long getFirstFree()
  {
    return getPos(this.set.nextClearBit(0));
  }
  
  public String toString()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    
    localStringBuilder.append('[');
    int i = 0;
    for (;;)
    {
      if (i > 0) {
        localStringBuilder.append(", ");
      }
      int j = this.set.nextClearBit(i);
      localStringBuilder.append(Integer.toHexString(j)).append('-');
      int k = this.set.nextSetBit(j + 1);
      if (k < 0) {
        break;
      }
      localStringBuilder.append(Integer.toHexString(k - 1));
      i = k + 1;
    }
    localStringBuilder.append(']');
    return localStringBuilder.toString();
  }
}
