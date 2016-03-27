package org.h2.util;

import org.h2.message.DbException;

public class Permutations<T>
{
  private final T[] in;
  private final T[] out;
  private final int n;
  private final int m;
  private final int[] index;
  private boolean hasNext = true;
  
  private Permutations(T[] paramArrayOfT1, T[] paramArrayOfT2, int paramInt)
  {
    this.n = paramArrayOfT1.length;
    this.m = paramInt;
    if ((this.n < paramInt) || (paramInt < 0)) {
      DbException.throwInternalError("n < m or m < 0");
    }
    this.in = paramArrayOfT1;
    this.out = paramArrayOfT2;
    this.index = new int[this.n];
    for (int i = 0; i < this.n; i++) {
      this.index[i] = i;
    }
    reverseAfter(paramInt - 1);
  }
  
  public static <T> Permutations<T> create(T[] paramArrayOfT1, T[] paramArrayOfT2)
  {
    return new Permutations(paramArrayOfT1, paramArrayOfT2, paramArrayOfT1.length);
  }
  
  public static <T> Permutations<T> create(T[] paramArrayOfT1, T[] paramArrayOfT2, int paramInt)
  {
    return new Permutations(paramArrayOfT1, paramArrayOfT2, paramInt);
  }
  
  private void moveIndex()
  {
    int i = rightmostDip();
    if (i < 0)
    {
      this.hasNext = false;
      return;
    }
    int j = i + 1;
    for (int k = i + 2; k < this.n; k++) {
      if ((this.index[k] < this.index[j]) && (this.index[k] > this.index[i])) {
        j = k;
      }
    }
    k = this.index[i];
    this.index[i] = this.index[j];
    this.index[j] = k;
    if (this.m - 1 > i)
    {
      reverseAfter(i);
      
      reverseAfter(this.m - 1);
    }
  }
  
  private int rightmostDip()
  {
    for (int i = this.n - 2; i >= 0; i--) {
      if (this.index[i] < this.index[(i + 1)]) {
        return i;
      }
    }
    return -1;
  }
  
  private void reverseAfter(int paramInt)
  {
    int i = paramInt + 1;
    int j = this.n - 1;
    while (i < j)
    {
      int k = this.index[i];
      this.index[i] = this.index[j];
      this.index[j] = k;
      i++;
      j--;
    }
  }
  
  public boolean next()
  {
    if (!this.hasNext) {
      return false;
    }
    for (int i = 0; i < this.m; i++) {
      this.out[i] = this.in[this.index[i]];
    }
    moveIndex();
    return true;
  }
}
