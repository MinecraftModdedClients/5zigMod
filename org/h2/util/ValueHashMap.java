package org.h2.util;

import java.util.ArrayList;
import org.h2.message.DbException;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class ValueHashMap<V>
  extends HashBase
{
  private Value[] keys;
  private V[] values;
  
  public static <T> ValueHashMap<T> newInstance()
  {
    return new ValueHashMap();
  }
  
  protected void reset(int paramInt)
  {
    super.reset(paramInt);
    this.keys = new Value[this.len];
    this.values = ((Object[])new Object[this.len]);
  }
  
  protected void rehash(int paramInt)
  {
    Value[] arrayOfValue = this.keys;
    Object[] arrayOfObject = this.values;
    reset(paramInt);
    int i = arrayOfValue.length;
    for (int j = 0; j < i; j++)
    {
      Value localValue = arrayOfValue[j];
      if ((localValue != null) && (localValue != ValueNull.DELETED)) {
        internalPut(localValue, arrayOfObject[j]);
      }
    }
  }
  
  private int getIndex(Value paramValue)
  {
    return paramValue.hashCode() & this.mask;
  }
  
  public void put(Value paramValue, V paramV)
  {
    checkSizePut();
    internalPut(paramValue, paramV);
  }
  
  private void internalPut(Value paramValue, V paramV)
  {
    int i = getIndex(paramValue);
    int j = 1;
    int k = -1;
    do
    {
      Value localValue = this.keys[i];
      if (localValue == null)
      {
        if (k >= 0)
        {
          i = k;
          this.deletedCount -= 1;
        }
        this.size += 1;
        this.keys[i] = paramValue;
        this.values[i] = paramV;
        return;
      }
      if (localValue == ValueNull.DELETED)
      {
        if (k < 0) {
          k = i;
        }
      }
      else if (localValue.equals(paramValue))
      {
        this.values[i] = paramV;
        return;
      }
      i = i + j++ & this.mask;
    } while (j <= this.len);
    DbException.throwInternalError("hashmap is full");
  }
  
  public void remove(Value paramValue)
  {
    checkSizeRemove();
    int i = getIndex(paramValue);
    int j = 1;
    do
    {
      Value localValue = this.keys[i];
      if (localValue == null) {
        return;
      }
      if (localValue != ValueNull.DELETED) {
        if (localValue.equals(paramValue))
        {
          this.keys[i] = ValueNull.DELETED;
          this.values[i] = null;
          this.deletedCount += 1;
          this.size -= 1;
          return;
        }
      }
      i = i + j++ & this.mask;
    } while (j <= this.len);
  }
  
  public V get(Value paramValue)
  {
    int i = getIndex(paramValue);
    int j = 1;
    do
    {
      Value localValue = this.keys[i];
      if (localValue == null) {
        return null;
      }
      if (localValue != ValueNull.DELETED) {
        if (localValue.equals(paramValue)) {
          return (V)this.values[i];
        }
      }
      i = i + j++ & this.mask;
    } while (j <= this.len);
    return null;
  }
  
  public ArrayList<Value> keys()
  {
    ArrayList localArrayList = New.arrayList(this.size);
    for (Value localValue : this.keys) {
      if ((localValue != null) && (localValue != ValueNull.DELETED)) {
        localArrayList.add(localValue);
      }
    }
    return localArrayList;
  }
  
  public ArrayList<V> values()
  {
    ArrayList localArrayList = New.arrayList(this.size);
    int i = this.keys.length;
    for (int j = 0; j < i; j++)
    {
      Value localValue = this.keys[j];
      if ((localValue != null) && (localValue != ValueNull.DELETED)) {
        localArrayList.add(this.values[j]);
      }
    }
    return localArrayList;
  }
}
