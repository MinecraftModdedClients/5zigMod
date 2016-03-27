package org.h2.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import org.h2.message.DbException;

public class SmallMap
{
  private final HashMap<Integer, Object> map = New.hashMap();
  private Object cache;
  private int cacheId;
  private int lastId;
  private final int maxElements;
  
  public SmallMap(int paramInt)
  {
    this.maxElements = paramInt;
  }
  
  public int addObject(int paramInt, Object paramObject)
  {
    if (this.map.size() > this.maxElements * 2)
    {
      Iterator localIterator = this.map.keySet().iterator();
      while (localIterator.hasNext())
      {
        Integer localInteger = (Integer)localIterator.next();
        if (localInteger.intValue() + this.maxElements < this.lastId) {
          localIterator.remove();
        }
      }
    }
    if (paramInt > this.lastId) {
      this.lastId = paramInt;
    }
    this.map.put(Integer.valueOf(paramInt), paramObject);
    this.cacheId = paramInt;
    this.cache = paramObject;
    return paramInt;
  }
  
  public void freeObject(int paramInt)
  {
    if (this.cacheId == paramInt)
    {
      this.cacheId = -1;
      this.cache = null;
    }
    this.map.remove(Integer.valueOf(paramInt));
  }
  
  public Object getObject(int paramInt, boolean paramBoolean)
  {
    if (paramInt == this.cacheId) {
      return this.cache;
    }
    Object localObject = this.map.get(Integer.valueOf(paramInt));
    if ((localObject == null) && (!paramBoolean)) {
      throw DbException.get(90007);
    }
    return localObject;
  }
}
