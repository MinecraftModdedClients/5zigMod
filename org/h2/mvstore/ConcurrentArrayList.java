package org.h2.mvstore;

import java.util.Arrays;
import java.util.Iterator;

public class ConcurrentArrayList<K>
{
  K[] array = (Object[])new Object[0];
  
  public K peekFirst()
  {
    Object[] arrayOfObject = this.array;
    return arrayOfObject.length == 0 ? null : arrayOfObject[0];
  }
  
  public K peekLast()
  {
    Object[] arrayOfObject = this.array;
    int i = arrayOfObject.length;
    return i == 0 ? null : arrayOfObject[(i - 1)];
  }
  
  public synchronized void add(K paramK)
  {
    int i = this.array.length;
    this.array = Arrays.copyOf(this.array, i + 1);
    this.array[i] = paramK;
  }
  
  public synchronized boolean removeFirst(K paramK)
  {
    if (peekFirst() != paramK) {
      return false;
    }
    int i = this.array.length;
    
    Object[] arrayOfObject = (Object[])new Object[i - 1];
    System.arraycopy(this.array, 1, arrayOfObject, 0, i - 1);
    this.array = arrayOfObject;
    return true;
  }
  
  public synchronized boolean removeLast(K paramK)
  {
    if (peekLast() != paramK) {
      return false;
    }
    this.array = Arrays.copyOf(this.array, this.array.length - 1);
    return true;
  }
  
  public Iterator<K> iterator()
  {
    new Iterator()
    {
      K[] a = ConcurrentArrayList.this.array;
      int index;
      
      public boolean hasNext()
      {
        return this.index < this.a.length;
      }
      
      public K next()
      {
        return (K)this.a[(this.index++)];
      }
      
      public void remove()
      {
        throw DataUtils.newUnsupportedOperationException("remove");
      }
    };
  }
}
