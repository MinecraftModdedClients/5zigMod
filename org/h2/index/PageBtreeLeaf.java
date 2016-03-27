package org.h2.index;

import java.util.Arrays;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.result.SearchRow;
import org.h2.store.Data;
import org.h2.store.Page;
import org.h2.store.PageStore;

public class PageBtreeLeaf
  extends PageBtree
{
  private static final int OFFSET_LENGTH = 2;
  private final boolean optimizeUpdate;
  private boolean writtenData;
  
  private PageBtreeLeaf(PageBtreeIndex paramPageBtreeIndex, int paramInt, Data paramData)
  {
    super(paramPageBtreeIndex, paramInt, paramData);
    this.optimizeUpdate = paramPageBtreeIndex.getDatabase().getSettings().optimizeUpdate;
  }
  
  public static Page read(PageBtreeIndex paramPageBtreeIndex, Data paramData, int paramInt)
  {
    PageBtreeLeaf localPageBtreeLeaf = new PageBtreeLeaf(paramPageBtreeIndex, paramInt, paramData);
    localPageBtreeLeaf.read();
    return localPageBtreeLeaf;
  }
  
  static PageBtreeLeaf create(PageBtreeIndex paramPageBtreeIndex, int paramInt1, int paramInt2)
  {
    PageBtreeLeaf localPageBtreeLeaf = new PageBtreeLeaf(paramPageBtreeIndex, paramInt1, paramPageBtreeIndex.getPageStore().createData());
    
    paramPageBtreeIndex.getPageStore().logUndo(localPageBtreeLeaf, null);
    localPageBtreeLeaf.rows = SearchRow.EMPTY_ARRAY;
    localPageBtreeLeaf.parentPageId = paramInt2;
    localPageBtreeLeaf.writeHead();
    localPageBtreeLeaf.start = localPageBtreeLeaf.data.length();
    return localPageBtreeLeaf;
  }
  
  private void read()
  {
    this.data.reset();
    int i = this.data.readByte();
    this.data.readShortInt();
    this.parentPageId = this.data.readInt();
    this.onlyPosition = ((i & 0x10) == 0);
    int j = this.data.readVarInt();
    if (j != this.index.getId()) {
      throw DbException.get(90030, "page:" + getPos() + " expected index:" + this.index.getId() + "got:" + j);
    }
    this.entryCount = this.data.readShortInt();
    this.offsets = new int[this.entryCount];
    this.rows = new SearchRow[this.entryCount];
    for (int k = 0; k < this.entryCount; k++) {
      this.offsets[k] = this.data.readShortInt();
    }
    this.start = this.data.length();
    this.written = true;
    this.writtenData = true;
  }
  
  int addRowTry(SearchRow paramSearchRow)
  {
    int i = addRow(paramSearchRow, true);
    memoryChange();
    return i;
  }
  
  private int addRow(SearchRow paramSearchRow, boolean paramBoolean)
  {
    int i = this.index.getRowSize(this.data, paramSearchRow, this.onlyPosition);
    int j = this.index.getPageStore().getPageSize();
    int k = this.entryCount == 0 ? j : this.offsets[(this.entryCount - 1)];
    int m;
    if (k - i < this.start + 2)
    {
      if ((paramBoolean) && (this.entryCount > 1))
      {
        m = find(paramSearchRow, false, true, true);
        if (this.entryCount < 5) {
          return this.entryCount / 2;
        }
        n = this.entryCount / 3;
        return m >= 2 * n ? 2 * n : m < n ? n : m;
      }
      readAllRows();
      this.writtenData = false;
      this.onlyPosition = true;
      
      m = j;
      for (n = 0; n < this.entryCount; n++)
      {
        m -= this.index.getRowSize(this.data, getRow(n), true);
        this.offsets[n] = m;
      }
      k = this.entryCount == 0 ? j : this.offsets[(this.entryCount - 1)];
      i = this.index.getRowSize(this.data, paramSearchRow, true);
      if ((SysProperties.CHECK) && (k - i < this.start + 2)) {
        throw DbException.throwInternalError();
      }
    }
    this.index.getPageStore().logUndo(this, this.data);
    if (!this.optimizeUpdate) {
      readAllRows();
    }
    this.changeCount = this.index.getPageStore().getChangeCount();
    this.written = false;
    if (this.entryCount == 0) {
      m = 0;
    } else {
      m = find(paramSearchRow, false, true, true);
    }
    this.start += 2;
    int n = (m == 0 ? j : this.offsets[(m - 1)]) - i;
    if ((this.optimizeUpdate) && (this.writtenData))
    {
      if (this.entryCount > 0)
      {
        byte[] arrayOfByte = this.data.getBytes();
        int i1 = this.offsets[(this.entryCount - 1)];
        int i2 = n;
        System.arraycopy(arrayOfByte, i1, arrayOfByte, i1 - i, i2 - i1 + i);
      }
      this.index.writeRow(this.data, n, paramSearchRow, this.onlyPosition);
    }
    this.offsets = insert(this.offsets, this.entryCount, m, n);
    add(this.offsets, m + 1, this.entryCount + 1, -i);
    this.rows = ((SearchRow[])insert(this.rows, this.entryCount, m, paramSearchRow));
    this.entryCount += 1;
    this.index.getPageStore().update(this);
    return -1;
  }
  
  private void removeRow(int paramInt)
  {
    if (!this.optimizeUpdate) {
      readAllRows();
    }
    this.index.getPageStore().logUndo(this, this.data);
    this.entryCount -= 1;
    this.written = false;
    this.changeCount = this.index.getPageStore().getChangeCount();
    if (this.entryCount <= 0) {
      DbException.throwInternalError();
    }
    int i = paramInt > 0 ? this.offsets[(paramInt - 1)] : this.index.getPageStore().getPageSize();
    int j = i - this.offsets[paramInt];
    this.start -= 2;
    if ((this.optimizeUpdate) && 
      (this.writtenData))
    {
      byte[] arrayOfByte = this.data.getBytes();
      int k = this.offsets[this.entryCount];
      System.arraycopy(arrayOfByte, k, arrayOfByte, k + j, this.offsets[paramInt] - k);
      
      Arrays.fill(arrayOfByte, k, k + j, (byte)0);
    }
    this.offsets = remove(this.offsets, this.entryCount + 1, paramInt);
    add(this.offsets, paramInt, this.entryCount, j);
    this.rows = ((SearchRow[])remove(this.rows, this.entryCount + 1, paramInt));
  }
  
  int getEntryCount()
  {
    return this.entryCount;
  }
  
  PageBtree split(int paramInt)
  {
    int i = this.index.getPageStore().allocatePage();
    PageBtreeLeaf localPageBtreeLeaf = create(this.index, i, this.parentPageId);
    for (int j = paramInt; j < this.entryCount;)
    {
      localPageBtreeLeaf.addRow(getRow(paramInt), false);
      removeRow(paramInt);
    }
    memoryChange();
    localPageBtreeLeaf.memoryChange();
    return localPageBtreeLeaf;
  }
  
  PageBtreeLeaf getFirstLeaf()
  {
    return this;
  }
  
  PageBtreeLeaf getLastLeaf()
  {
    return this;
  }
  
  SearchRow remove(SearchRow paramSearchRow)
  {
    int i = find(paramSearchRow, false, false, true);
    SearchRow localSearchRow = getRow(i);
    if ((this.index.compareRows(paramSearchRow, localSearchRow) != 0) || (localSearchRow.getKey() != paramSearchRow.getKey())) {
      throw DbException.get(90112, this.index.getSQL() + ": " + paramSearchRow);
    }
    this.index.getPageStore().logUndo(this, this.data);
    if (this.entryCount == 1) {
      return paramSearchRow;
    }
    removeRow(i);
    memoryChange();
    this.index.getPageStore().update(this);
    if (i == this.entryCount) {
      return getRow(i - 1);
    }
    return null;
  }
  
  void freeRecursive()
  {
    this.index.getPageStore().logUndo(this, this.data);
    this.index.getPageStore().free(getPos());
  }
  
  int getRowCount()
  {
    return this.entryCount;
  }
  
  void setRowCountStored(int paramInt) {}
  
  public void write()
  {
    writeData();
    this.index.getPageStore().writePage(getPos(), this.data);
  }
  
  private void writeHead()
  {
    this.data.reset();
    this.data.writeByte((byte)(0x4 | (this.onlyPosition ? 0 : 16)));
    
    this.data.writeShortInt(0);
    this.data.writeInt(this.parentPageId);
    this.data.writeVarInt(this.index.getId());
    this.data.writeShortInt(this.entryCount);
  }
  
  private void writeData()
  {
    if (this.written) {
      return;
    }
    if (!this.optimizeUpdate) {
      readAllRows();
    }
    writeHead();
    for (int i = 0; i < this.entryCount; i++) {
      this.data.writeShortInt(this.offsets[i]);
    }
    if ((!this.writtenData) || (!this.optimizeUpdate))
    {
      for (i = 0; i < this.entryCount; i++) {
        this.index.writeRow(this.data, this.offsets[i], this.rows[i], this.onlyPosition);
      }
      this.writtenData = true;
    }
    this.written = true;
    memoryChange();
  }
  
  void find(PageBtreeCursor paramPageBtreeCursor, SearchRow paramSearchRow, boolean paramBoolean)
  {
    int i = find(paramSearchRow, paramBoolean, false, false);
    if (i > this.entryCount)
    {
      if (this.parentPageId == 0) {
        return;
      }
      PageBtreeNode localPageBtreeNode = (PageBtreeNode)this.index.getPage(this.parentPageId);
      localPageBtreeNode.find(paramPageBtreeCursor, paramSearchRow, paramBoolean);
      return;
    }
    paramPageBtreeCursor.setCurrent(this, i);
  }
  
  void last(PageBtreeCursor paramPageBtreeCursor)
  {
    paramPageBtreeCursor.setCurrent(this, this.entryCount - 1);
  }
  
  void remapChildren() {}
  
  void nextPage(PageBtreeCursor paramPageBtreeCursor)
  {
    if (this.parentPageId == 0)
    {
      paramPageBtreeCursor.setCurrent(null, 0);
      return;
    }
    PageBtreeNode localPageBtreeNode = (PageBtreeNode)this.index.getPage(this.parentPageId);
    localPageBtreeNode.nextPage(paramPageBtreeCursor, getPos());
  }
  
  void previousPage(PageBtreeCursor paramPageBtreeCursor)
  {
    if (this.parentPageId == 0)
    {
      paramPageBtreeCursor.setCurrent(null, 0);
      return;
    }
    PageBtreeNode localPageBtreeNode = (PageBtreeNode)this.index.getPage(this.parentPageId);
    localPageBtreeNode.previousPage(paramPageBtreeCursor, getPos());
  }
  
  public String toString()
  {
    return "page[" + getPos() + "] b-tree leaf table:" + this.index.getId() + " entries:" + this.entryCount;
  }
  
  public void moveTo(Session paramSession, int paramInt)
  {
    PageStore localPageStore = this.index.getPageStore();
    readAllRows();
    PageBtreeLeaf localPageBtreeLeaf = create(this.index, paramInt, this.parentPageId);
    localPageStore.logUndo(this, this.data);
    localPageStore.logUndo(localPageBtreeLeaf, null);
    localPageBtreeLeaf.rows = this.rows;
    localPageBtreeLeaf.entryCount = this.entryCount;
    localPageBtreeLeaf.offsets = this.offsets;
    localPageBtreeLeaf.onlyPosition = this.onlyPosition;
    localPageBtreeLeaf.parentPageId = this.parentPageId;
    localPageBtreeLeaf.start = this.start;
    localPageStore.update(localPageBtreeLeaf);
    if (this.parentPageId == 0)
    {
      this.index.setRootPageId(paramSession, paramInt);
    }
    else
    {
      PageBtreeNode localPageBtreeNode = (PageBtreeNode)localPageStore.getPage(this.parentPageId);
      localPageBtreeNode.moveChild(getPos(), paramInt);
    }
    localPageStore.free(getPos());
  }
  
  protected void memoryChange()
  {
    if (!PageBtreeIndex.isMemoryChangeRequired()) {
      return;
    }
    int i = 184 + this.index.getPageStore().getPageSize();
    if (this.rows != null)
    {
      i += getEntryCount() * 12;
      for (int j = 0; j < this.entryCount; j++)
      {
        SearchRow localSearchRow = this.rows[j];
        if (localSearchRow != null) {
          i += localSearchRow.getMemory();
        }
      }
    }
    this.index.memoryChange(i >> 2);
  }
}
