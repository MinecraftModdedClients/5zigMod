package org.h2.util;

import java.util.ArrayList;
import java.util.Map;

class CacheSecondLevel
  implements Cache
{
  private final Cache baseCache;
  private final Map<Integer, CacheObject> map;
  
  CacheSecondLevel(Cache paramCache, Map<Integer, CacheObject> paramMap)
  {
    this.baseCache = paramCache;
    this.map = paramMap;
  }
  
  public void clear()
  {
    this.map.clear();
    this.baseCache.clear();
  }
  
  public CacheObject find(int paramInt)
  {
    CacheObject localCacheObject = this.baseCache.find(paramInt);
    if (localCacheObject == null) {
      localCacheObject = (CacheObject)this.map.get(Integer.valueOf(paramInt));
    }
    return localCacheObject;
  }
  
  public CacheObject get(int paramInt)
  {
    CacheObject localCacheObject = this.baseCache.get(paramInt);
    if (localCacheObject == null) {
      localCacheObject = (CacheObject)this.map.get(Integer.valueOf(paramInt));
    }
    return localCacheObject;
  }
  
  public ArrayList<CacheObject> getAllChanged()
  {
    return this.baseCache.getAllChanged();
  }
  
  public int getMaxMemory()
  {
    return this.baseCache.getMaxMemory();
  }
  
  public int getMemory()
  {
    return this.baseCache.getMemory();
  }
  
  public void put(CacheObject paramCacheObject)
  {
    this.baseCache.put(paramCacheObject);
    this.map.put(Integer.valueOf(paramCacheObject.getPos()), paramCacheObject);
  }
  
  public boolean remove(int paramInt)
  {
    boolean bool = this.baseCache.remove(paramInt);
    bool |= this.map.remove(Integer.valueOf(paramInt)) != null;
    return bool;
  }
  
  public void setMaxMemory(int paramInt)
  {
    this.baseCache.setMaxMemory(paramInt);
  }
  
  public CacheObject update(int paramInt, CacheObject paramCacheObject)
  {
    CacheObject localCacheObject = this.baseCache.update(paramInt, paramCacheObject);
    this.map.put(Integer.valueOf(paramInt), paramCacheObject);
    return localCacheObject;
  }
}
