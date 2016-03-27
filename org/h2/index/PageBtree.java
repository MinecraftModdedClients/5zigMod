package org.h2.index;

import org.h2.result.SearchRow;
import org.h2.store.Data;
import org.h2.store.Page;
import org.h2.store.PageStore;

public abstract class PageBtree
  extends Page
{
  static final int ROOT = 0;
  static final int UNKNOWN_ROWCOUNT = -1;
  protected final PageBtreeIndex index;
  protected int parentPageId;
  protected final Data data;
  protected int[] offsets;
  protected int entryCount;
  protected SearchRow[] rows;
  protected int start;
  protected boolean onlyPosition;
  protected boolean written;
  private final int memoryEstimated;
  
  PageBtree(PageBtreeIndex paramPageBtreeIndex, int paramInt, Data paramData)
  {
    this.index = paramPageBtreeIndex;
    this.data = paramData;
    setPos(paramInt);
    this.memoryEstimated = paramPageBtreeIndex.getMemoryPerPage();
  }
  
  abstract int getRowCount();
  
  abstract void setRowCountStored(int paramInt);
  
  int find(SearchRow paramSearchRow, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3)
  {
    if (paramSearchRow == null) {
      return 0;
    }
    int i = 0;int j = this.entryCount;
    int k = 1;
    while (i < j)
    {
      int m = i + j >>> 1;
      SearchRow localSearchRow = getRow(m);
      k = this.index.compareRows(localSearchRow, paramSearchRow);
      if (k == 0)
      {
        if ((paramBoolean2) && (this.index.indexType.isUnique()) && 
          (!this.index.containsNullAndAllowMultipleNull(paramSearchRow))) {
          throw this.index.getDuplicateKeyException(paramSearchRow.toString());
        }
        if (paramBoolean3)
        {
          k = this.index.compareKeys(localSearchRow, paramSearchRow);
          if (k == 0) {
            return m;
          }
        }
      }
      if ((k > 0) || ((!paramBoolean1) && (k == 0))) {
        j = m;
      } else {
        i = m + 1;
      }
    }
    return i;
  }
  
  abstract int addRowTry(SearchRow paramSearchRow);
  
  abstract void find(PageBtreeCursor paramPageBtreeCursor, SearchRow paramSearchRow, boolean paramBoolean);
  
  abstract void last(PageBtreeCursor paramPageBtreeCursor);
  
  SearchRow getRow(int paramInt)
  {
    SearchRow localSearchRow = this.rows[paramInt];
    if (localSearchRow == null)
    {
      localSearchRow = this.index.readRow(this.data, this.offsets[paramInt], this.onlyPosition, true);
      memoryChange();
      this.rows[paramInt] = localSearchRow;
    }
    else if (!this.index.hasData(localSearchRow))
    {
      localSearchRow = this.index.readRow(localSearchRow.getKey());
      memoryChange();
      this.rows[paramInt] = localSearchRow;
    }
    return localSearchRow;
  }
  
  protected void memoryChange() {}
  
  abstract PageBtree split(int paramInt);
  
  void setPageId(int paramInt)
  {
    this.changeCount = this.index.getPageStore().getChangeCount();
    this.written = false;
    this.index.getPageStore().removeFromCache(getPos());
    setPos(paramInt);
    this.index.getPageStore().logUndo(this, null);
    remapChildren();
  }
  
  abstract PageBtreeLeaf getFirstLeaf();
  
  abstract PageBtreeLeaf getLastLeaf();
  
  void setParentPageId(int paramInt)
  {
    this.index.getPageStore().logUndo(this, this.data);
    this.changeCount = this.index.getPageStore().getChangeCount();
    this.written = false;
    this.parentPageId = paramInt;
  }
  
  abstract void remapChildren();
  
  abstract SearchRow remove(SearchRow paramSearchRow);
  
  abstract void freeRecursive();
  
  protected void readAllRows()
  {
    for (int i = 0; i < this.entryCount; i++)
    {
      SearchRow localSearchRow = this.rows[i];
      if (localSearchRow == null)
      {
        localSearchRow = this.index.readRow(this.data, this.offsets[i], this.onlyPosition, false);
        this.rows[i] = localSearchRow;
      }
    }
  }
  
  public int getMemory()
  {
    return this.memoryEstimated;
  }
  
  public boolean canRemove()
  {
    if (this.changeCount >= this.index.getPageStore().getChangeCount()) {
      return false;
    }
    return true;
  }
}
