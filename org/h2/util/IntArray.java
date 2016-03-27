package org.h2.util;

import org.h2.engine.SysProperties;

public class IntArray
{
  private int[] data;
  private int size;
  private int hash;
  
  public IntArray()
  {
    this(10);
  }
  
  public IntArray(int paramInt)
  {
    this.data = new int[paramInt];
  }
  
  public IntArray(int[] paramArrayOfInt)
  {
    this.data = paramArrayOfInt;
    this.size = paramArrayOfInt.length;
  }
  
  public void add(int paramInt)
  {
    if (this.size >= this.data.length) {
      ensureCapacity(this.size + this.size);
    }
    this.data[(this.size++)] = paramInt;
  }
  
  public int get(int paramInt)
  {
    if ((SysProperties.CHECK) && 
      (paramInt >= this.size)) {
      throw new ArrayIndexOutOfBoundsException("i=" + paramInt + " size=" + this.size);
    }
    return this.data[paramInt];
  }
  
  public void remove(int paramInt)
  {
    if ((SysProperties.CHECK) && 
      (paramInt >= this.size)) {
      throw new ArrayIndexOutOfBoundsException("i=" + paramInt + " size=" + this.size);
    }
    System.arraycopy(this.data, paramInt + 1, this.data, paramInt, this.size - paramInt - 1);
    this.size -= 1;
  }
  
  public void ensureCapacity(int paramInt)
  {
    paramInt = Math.max(4, paramInt);
    if (paramInt >= this.data.length)
    {
      int[] arrayOfInt = new int[paramInt];
      System.arraycopy(this.data, 0, arrayOfInt, 0, this.data.length);
      this.data = arrayOfInt;
    }
  }
  
  public boolean equals(Object paramObject)
  {
    if (!(paramObject instanceof IntArray)) {
      return false;
    }
    IntArray localIntArray = (IntArray)paramObject;
    if ((hashCode() != localIntArray.hashCode()) || (this.size != localIntArray.size)) {
      return false;
    }
    for (int i = 0; i < this.size; i++) {
      if (this.data[i] != localIntArray.data[i]) {
        return false;
      }
    }
    return true;
  }
  
  public int hashCode()
  {
    if (this.hash != 0) {
      return this.hash;
    }
    int i = this.size + 1;
    for (int j = 0; j < this.size; j++) {
      i = i * 31 + this.data[j];
    }
    this.hash = i;
    return i;
  }
  
  public int size()
  {
    return this.size;
  }
  
  public void toArray(int[] paramArrayOfInt)
  {
    System.arraycopy(this.data, 0, paramArrayOfInt, 0, this.size);
  }
  
  public String toString()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("{");
    for (int i = 0; i < this.size; i++)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(this.data[i]);
    }
    return localStatementBuilder.append('}').toString();
  }
  
  public void removeRange(int paramInt1, int paramInt2)
  {
    if ((SysProperties.CHECK) && (
      (paramInt1 > paramInt2) || (paramInt2 > this.size))) {
      throw new ArrayIndexOutOfBoundsException("from=" + paramInt1 + " to=" + paramInt2 + " size=" + this.size);
    }
    System.arraycopy(this.data, paramInt2, this.data, paramInt1, this.size - paramInt2);
    this.size -= paramInt2 - paramInt1;
  }
}
