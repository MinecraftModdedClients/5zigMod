package org.h2.util;

import java.util.ArrayList;

public class CacheTQ
  implements Cache
{
  static final String TYPE_NAME = "TQ";
  private final Cache lru;
  private final Cache fifo;
  private final SmallLRUCache<Integer, Object> recentlyUsed = SmallLRUCache.newInstance(1024);
  private int lastUsed = -1;
  private int maxMemory;
  
  CacheTQ(CacheWriter paramCacheWriter, int paramInt)
  {
    this.maxMemory = paramInt;
    this.lru = new CacheLRU(paramCacheWriter, (int)(paramInt * 0.8D), false);
    this.fifo = new CacheLRU(paramCacheWriter, (int)(paramInt * 0.2D), true);
    setMaxMemory(4 * paramInt);
  }
  
  public void clear()
  {
    this.lru.clear();
    this.fifo.clear();
    this.recentlyUsed.clear();
    this.lastUsed = -1;
  }
  
  public CacheObject find(int paramInt)
  {
    CacheObject localCacheObject = this.lru.find(paramInt);
    if (localCacheObject == null) {
      localCacheObject = this.fifo.find(paramInt);
    }
    return localCacheObject;
  }
  
  public CacheObject get(int paramInt)
  {
    CacheObject localCacheObject = this.lru.find(paramInt);
    if (localCacheObject != null) {
      return localCacheObject;
    }
    localCacheObject = this.fifo.find(paramInt);
    if ((localCacheObject != null) && (!localCacheObject.isStream()))
    {
      if (this.recentlyUsed.get(Integer.valueOf(paramInt)) != null)
      {
        if (this.lastUsed != paramInt)
        {
          this.fifo.remove(paramInt);
          this.lru.put(localCacheObject);
        }
      }
      else {
        this.recentlyUsed.put(Integer.valueOf(paramInt), this);
      }
      this.lastUsed = paramInt;
    }
    return localCacheObject;
  }
  
  public ArrayList<CacheObject> getAllChanged()
  {
    ArrayList localArrayList = New.arrayList();
    localArrayList.addAll(this.lru.getAllChanged());
    localArrayList.addAll(this.fifo.getAllChanged());
    return localArrayList;
  }
  
  public int getMaxMemory()
  {
    return this.maxMemory;
  }
  
  public int getMemory()
  {
    return this.lru.getMemory() + this.fifo.getMemory();
  }
  
  public void put(CacheObject paramCacheObject)
  {
    if (paramCacheObject.isStream())
    {
      this.fifo.put(paramCacheObject);
    }
    else if (this.recentlyUsed.get(Integer.valueOf(paramCacheObject.getPos())) != null)
    {
      this.lru.put(paramCacheObject);
    }
    else
    {
      this.fifo.put(paramCacheObject);
      this.lastUsed = paramCacheObject.getPos();
    }
  }
  
  public boolean remove(int paramInt)
  {
    boolean bool = this.lru.remove(paramInt);
    if (!bool) {
      bool = this.fifo.remove(paramInt);
    }
    this.recentlyUsed.remove(Integer.valueOf(paramInt));
    return bool;
  }
  
  public void setMaxMemory(int paramInt)
  {
    this.maxMemory = paramInt;
    this.lru.setMaxMemory((int)(paramInt * 0.8D));
    this.fifo.setMaxMemory((int)(paramInt * 0.2D));
    this.recentlyUsed.setMaxSize(4 * paramInt);
  }
  
  public CacheObject update(int paramInt, CacheObject paramCacheObject)
  {
    if (this.lru.find(paramInt) != null) {
      return this.lru.update(paramInt, paramCacheObject);
    }
    return this.fifo.update(paramInt, paramCacheObject);
  }
}
