package org.h2.util;

import java.util.ArrayList;
import java.util.Collections;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.message.Trace;

public class CacheLRU
  implements Cache
{
  static final String TYPE_NAME = "LRU";
  private final CacheWriter writer;
  private final boolean fifo;
  private final CacheObject head = new CacheHead();
  private final int mask;
  private CacheObject[] values;
  private int recordCount;
  private final int len;
  private int maxMemory;
  private int memory;
  
  CacheLRU(CacheWriter paramCacheWriter, int paramInt, boolean paramBoolean)
  {
    this.writer = paramCacheWriter;
    this.fifo = paramBoolean;
    setMaxMemory(paramInt);
    this.len = MathUtils.nextPowerOf2(this.maxMemory / 64);
    this.mask = (this.len - 1);
    clear();
  }
  
  public static Cache getCache(CacheWriter paramCacheWriter, String paramString, int paramInt)
  {
    SoftHashMap localSoftHashMap = null;
    if (paramString.startsWith("SOFT_"))
    {
      localSoftHashMap = new SoftHashMap();
      paramString = paramString.substring("SOFT_".length());
    }
    Object localObject;
    if ("LRU".equals(paramString)) {
      localObject = new CacheLRU(paramCacheWriter, paramInt, false);
    } else if ("TQ".equals(paramString)) {
      localObject = new CacheTQ(paramCacheWriter, paramInt);
    } else {
      throw DbException.getInvalidValueException("CACHE_TYPE", paramString);
    }
    if (localSoftHashMap != null) {
      localObject = new CacheSecondLevel((Cache)localObject, localSoftHashMap);
    }
    return (Cache)localObject;
  }
  
  public void clear()
  {
    this.head.cacheNext = (this.head.cachePrevious = this.head);
    
    this.values = null;
    this.values = new CacheObject[this.len];
    this.recordCount = 0;
    this.memory = (this.len * 8);
  }
  
  public void put(CacheObject paramCacheObject)
  {
    if (SysProperties.CHECK)
    {
      i = paramCacheObject.getPos();
      CacheObject localCacheObject = find(i);
      if (localCacheObject != null) {
        DbException.throwInternalError("try to add a record twice at pos " + i);
      }
    }
    int i = paramCacheObject.getPos() & this.mask;
    paramCacheObject.cacheChained = this.values[i];
    this.values[i] = paramCacheObject;
    this.recordCount += 1;
    this.memory += paramCacheObject.getMemory();
    addToFront(paramCacheObject);
    removeOldIfRequired();
  }
  
  public CacheObject update(int paramInt, CacheObject paramCacheObject)
  {
    CacheObject localCacheObject = find(paramInt);
    if (localCacheObject == null)
    {
      put(paramCacheObject);
    }
    else
    {
      if ((SysProperties.CHECK) && 
        (localCacheObject != paramCacheObject)) {
        DbException.throwInternalError("old!=record pos:" + paramInt + " old:" + localCacheObject + " new:" + paramCacheObject);
      }
      if (!this.fifo)
      {
        removeFromLinkedList(paramCacheObject);
        addToFront(paramCacheObject);
      }
    }
    return localCacheObject;
  }
  
  private void removeOldIfRequired()
  {
    if (this.memory >= this.maxMemory) {
      removeOld();
    }
  }
  
  private void removeOld()
  {
    int i = 0;
    ArrayList localArrayList = New.arrayList();
    int j = this.memory;
    int k = this.recordCount;
    int m = 0;
    CacheObject localCacheObject1 = this.head.cacheNext;
    while (k > 16)
    {
      if (localArrayList.size() == 0 ? 
        j <= this.maxMemory : 
        
        j * 4 <= this.maxMemory * 3) {
        break;
      }
      CacheObject localCacheObject2 = localCacheObject1;
      localCacheObject1 = localCacheObject2.cacheNext;
      i++;
      if (i >= this.recordCount) {
        if (m == 0)
        {
          this.writer.flushLog();
          m = 1;
          i = 0;
        }
        else
        {
          this.writer.getTrace().info("cannot remove records, cache size too small? records:" + this.recordCount + " memory:" + this.memory);
          
          break;
        }
      }
      if ((SysProperties.CHECK) && (localCacheObject2 == this.head)) {
        DbException.throwInternalError("try to remove head");
      }
      if (!localCacheObject2.canRemove())
      {
        removeFromLinkedList(localCacheObject2);
        addToFront(localCacheObject2);
      }
      else
      {
        k--;
        j -= localCacheObject2.getMemory();
        if (localCacheObject2.isChanged()) {
          localArrayList.add(localCacheObject2);
        } else {
          remove(localCacheObject2.getPos());
        }
      }
    }
    if (localArrayList.size() > 0)
    {
      if (m == 0) {
        this.writer.flushLog();
      }
      Collections.sort(localArrayList);
      int n = this.maxMemory;
      int i1 = localArrayList.size();
      CacheObject localCacheObject3;
      try
      {
        this.maxMemory = Integer.MAX_VALUE;
        for (i = 0; i < i1; i++)
        {
          localCacheObject3 = (CacheObject)localArrayList.get(i);
          this.writer.writeBack(localCacheObject3);
        }
      }
      finally
      {
        this.maxMemory = n;
      }
      for (i = 0; i < i1; i++)
      {
        localCacheObject3 = (CacheObject)localArrayList.get(i);
        remove(localCacheObject3.getPos());
        if ((SysProperties.CHECK) && 
          (localCacheObject3.cacheNext != null)) {
          throw DbException.throwInternalError();
        }
      }
    }
  }
  
  private void addToFront(CacheObject paramCacheObject)
  {
    if ((SysProperties.CHECK) && (paramCacheObject == this.head)) {
      DbException.throwInternalError("try to move head");
    }
    paramCacheObject.cacheNext = this.head;
    paramCacheObject.cachePrevious = this.head.cachePrevious;
    paramCacheObject.cachePrevious.cacheNext = paramCacheObject;
    this.head.cachePrevious = paramCacheObject;
  }
  
  private void removeFromLinkedList(CacheObject paramCacheObject)
  {
    if ((SysProperties.CHECK) && (paramCacheObject == this.head)) {
      DbException.throwInternalError("try to remove head");
    }
    paramCacheObject.cachePrevious.cacheNext = paramCacheObject.cacheNext;
    paramCacheObject.cacheNext.cachePrevious = paramCacheObject.cachePrevious;
    
    paramCacheObject.cacheNext = null;
    paramCacheObject.cachePrevious = null;
  }
  
  public boolean remove(int paramInt)
  {
    int i = paramInt & this.mask;
    CacheObject localCacheObject1 = this.values[i];
    if (localCacheObject1 == null) {
      return false;
    }
    CacheObject localCacheObject2;
    if (localCacheObject1.getPos() == paramInt)
    {
      this.values[i] = localCacheObject1.cacheChained;
    }
    else
    {
      do
      {
        localCacheObject2 = localCacheObject1;
        localCacheObject1 = localCacheObject1.cacheChained;
        if (localCacheObject1 == null) {
          return false;
        }
      } while (localCacheObject1.getPos() != paramInt);
      localCacheObject2.cacheChained = localCacheObject1.cacheChained;
    }
    this.recordCount -= 1;
    this.memory -= localCacheObject1.getMemory();
    removeFromLinkedList(localCacheObject1);
    if (SysProperties.CHECK)
    {
      localCacheObject1.cacheChained = null;
      localCacheObject2 = find(paramInt);
      if (localCacheObject2 != null) {
        DbException.throwInternalError("not removed: " + localCacheObject2);
      }
    }
    return true;
  }
  
  public CacheObject find(int paramInt)
  {
    CacheObject localCacheObject = this.values[(paramInt & this.mask)];
    while ((localCacheObject != null) && (localCacheObject.getPos() != paramInt)) {
      localCacheObject = localCacheObject.cacheChained;
    }
    return localCacheObject;
  }
  
  public CacheObject get(int paramInt)
  {
    CacheObject localCacheObject = find(paramInt);
    if ((localCacheObject != null) && 
      (!this.fifo))
    {
      removeFromLinkedList(localCacheObject);
      addToFront(localCacheObject);
    }
    return localCacheObject;
  }
  
  public ArrayList<CacheObject> getAllChanged()
  {
    ArrayList localArrayList = New.arrayList();
    CacheObject localCacheObject = this.head.cacheNext;
    while (localCacheObject != this.head)
    {
      if (localCacheObject.isChanged()) {
        localArrayList.add(localCacheObject);
      }
      localCacheObject = localCacheObject.cacheNext;
    }
    return localArrayList;
  }
  
  public void setMaxMemory(int paramInt)
  {
    int i = MathUtils.convertLongToInt(paramInt * 1024L / 4L);
    this.maxMemory = (i < 0 ? 0 : i);
    
    removeOldIfRequired();
  }
  
  public int getMaxMemory()
  {
    return (int)(this.maxMemory * 4L / 1024L);
  }
  
  public int getMemory()
  {
    return (int)(this.memory * 4L / 1024L);
  }
}
