package org.h2.index;

import org.h2.engine.Session;
import org.h2.result.Row;
import org.h2.store.Data;
import org.h2.store.Page;
import org.h2.store.PageStore;

abstract class PageData
  extends Page
{
  static final int START_PARENT = 3;
  static final int ROOT = 0;
  static final int UNKNOWN_ROWCOUNT = -1;
  protected final PageDataIndex index;
  protected int parentPageId;
  protected final Data data;
  protected int entryCount;
  protected long[] keys;
  protected boolean written;
  private final int memoryEstimated;
  
  PageData(PageDataIndex paramPageDataIndex, int paramInt, Data paramData)
  {
    this.index = paramPageDataIndex;
    this.data = paramData;
    setPos(paramInt);
    this.memoryEstimated = paramPageDataIndex.getMemoryPerPage();
  }
  
  abstract int getRowCount();
  
  abstract void setRowCountStored(int paramInt);
  
  abstract long getDiskSpaceUsed();
  
  int find(long paramLong)
  {
    int i = 0;int j = this.entryCount;
    while (i < j)
    {
      int k = i + j >>> 1;
      long l = this.keys[k];
      if (l == paramLong) {
        return k;
      }
      if (l > paramLong) {
        j = k;
      } else {
        i = k + 1;
      }
    }
    return i;
  }
  
  abstract int addRowTry(Row paramRow);
  
  abstract Cursor find(Session paramSession, long paramLong1, long paramLong2, boolean paramBoolean);
  
  long getKey(int paramInt)
  {
    return this.keys[paramInt];
  }
  
  abstract PageData split(int paramInt);
  
  void setPageId(int paramInt)
  {
    int i = getPos();
    this.index.getPageStore().removeFromCache(getPos());
    setPos(paramInt);
    this.index.getPageStore().logUndo(this, null);
    remapChildren(i);
  }
  
  abstract long getLastKey();
  
  abstract PageDataLeaf getFirstLeaf();
  
  void setParentPageId(int paramInt)
  {
    this.index.getPageStore().logUndo(this, this.data);
    this.parentPageId = paramInt;
    if (this.written)
    {
      this.changeCount = this.index.getPageStore().getChangeCount();
      this.data.setInt(3, this.parentPageId);
    }
  }
  
  abstract void remapChildren(int paramInt);
  
  abstract boolean remove(long paramLong);
  
  abstract void freeRecursive();
  
  abstract Row getRowWithKey(long paramLong);
  
  public int getMemory()
  {
    return this.memoryEstimated;
  }
  
  int getParentPageId()
  {
    return this.parentPageId;
  }
  
  public boolean canRemove()
  {
    if (this.changeCount >= this.index.getPageStore().getChangeCount()) {
      return false;
    }
    return true;
  }
}
