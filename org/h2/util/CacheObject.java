package org.h2.util;

import org.h2.engine.SysProperties;
import org.h2.message.DbException;

public abstract class CacheObject
  implements Comparable<CacheObject>
{
  public CacheObject cachePrevious;
  public CacheObject cacheNext;
  public CacheObject cacheChained;
  private int pos;
  private boolean changed;
  
  public abstract boolean canRemove();
  
  public abstract int getMemory();
  
  public void setPos(int paramInt)
  {
    if ((SysProperties.CHECK) && (
      (this.cachePrevious != null) || (this.cacheNext != null) || (this.cacheChained != null))) {
      DbException.throwInternalError("setPos too late");
    }
    this.pos = paramInt;
  }
  
  public int getPos()
  {
    return this.pos;
  }
  
  public boolean isChanged()
  {
    return this.changed;
  }
  
  public void setChanged(boolean paramBoolean)
  {
    this.changed = paramBoolean;
  }
  
  public int compareTo(CacheObject paramCacheObject)
  {
    return MathUtils.compareInt(getPos(), paramCacheObject.getPos());
  }
  
  public boolean isStream()
  {
    return false;
  }
}
