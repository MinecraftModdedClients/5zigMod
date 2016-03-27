package org.h2.util;

import org.h2.message.DbException;

public class IntIntHashMap
  extends HashBase
{
  public static final int NOT_FOUND = -1;
  private static final int DELETED = 1;
  private int[] keys;
  private int[] values;
  private int zeroValue;
  
  protected void reset(int paramInt)
  {
    super.reset(paramInt);
    this.keys = new int[this.len];
    this.values = new int[this.len];
  }
  
  public void put(int paramInt1, int paramInt2)
  {
    if (paramInt1 == 0)
    {
      this.zeroKey = true;
      this.zeroValue = paramInt2;
      return;
    }
    checkSizePut();
    internalPut(paramInt1, paramInt2);
  }
  
  private void internalPut(int paramInt1, int paramInt2)
  {
    int i = getIndex(paramInt1);
    int j = 1;
    int k = -1;
    do
    {
      int m = this.keys[i];
      if (m == 0)
      {
        if (this.values[i] != 1)
        {
          if (k >= 0)
          {
            i = k;
            this.deletedCount -= 1;
          }
          this.size += 1;
          this.keys[i] = paramInt1;
          this.values[i] = paramInt2;
          return;
        }
        if (k < 0) {
          k = i;
        }
      }
      else if (m == paramInt1)
      {
        this.values[i] = paramInt2;
        return;
      }
      i = i + j++ & this.mask;
    } while (j <= this.len);
    DbException.throwInternalError("hashmap is full");
  }
  
  public void remove(int paramInt)
  {
    if (paramInt == 0)
    {
      this.zeroKey = false;
      return;
    }
    checkSizeRemove();
    int i = getIndex(paramInt);
    int j = 1;
    do
    {
      int k = this.keys[i];
      if (k == paramInt)
      {
        this.keys[i] = 0;
        this.values[i] = 1;
        this.deletedCount += 1;
        this.size -= 1;
        return;
      }
      if ((k == 0) && (this.values[i] == 0)) {
        return;
      }
      i = i + j++ & this.mask;
    } while (j <= this.len);
  }
  
  protected void rehash(int paramInt)
  {
    int[] arrayOfInt1 = this.keys;
    int[] arrayOfInt2 = this.values;
    reset(paramInt);
    for (int i = 0; i < arrayOfInt1.length; i++)
    {
      int j = arrayOfInt1[i];
      if (j != 0) {
        internalPut(j, arrayOfInt2[i]);
      }
    }
  }
  
  public int get(int paramInt)
  {
    if (paramInt == 0) {
      return this.zeroKey ? this.zeroValue : -1;
    }
    int i = getIndex(paramInt);
    int j = 1;
    do
    {
      int k = this.keys[i];
      if ((k == 0) && (this.values[i] == 0)) {
        return -1;
      }
      if (k == paramInt) {
        return this.values[i];
      }
      i = i + j++ & this.mask;
    } while (j <= this.len);
    return -1;
  }
}
