package org.h2.index;

import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.result.SearchRow;
import org.h2.store.Data;
import org.h2.store.Page;
import org.h2.store.PageStore;
import org.h2.util.Utils;

public class PageBtreeNode
  extends PageBtree
{
  private static final int CHILD_OFFSET_PAIR_LENGTH = 6;
  private static final int MAX_KEY_LENGTH = 10;
  private final boolean pageStoreInternalCount;
  private int[] childPageIds;
  private int rowCountStored = -1;
  private int rowCount = -1;
  
  private PageBtreeNode(PageBtreeIndex paramPageBtreeIndex, int paramInt, Data paramData)
  {
    super(paramPageBtreeIndex, paramInt, paramData);
    this.pageStoreInternalCount = paramPageBtreeIndex.getDatabase().getSettings().pageStoreInternalCount;
  }
  
  public static Page read(PageBtreeIndex paramPageBtreeIndex, Data paramData, int paramInt)
  {
    PageBtreeNode localPageBtreeNode = new PageBtreeNode(paramPageBtreeIndex, paramInt, paramData);
    localPageBtreeNode.read();
    return localPageBtreeNode;
  }
  
  static PageBtreeNode create(PageBtreeIndex paramPageBtreeIndex, int paramInt1, int paramInt2)
  {
    PageBtreeNode localPageBtreeNode = new PageBtreeNode(paramPageBtreeIndex, paramInt1, paramPageBtreeIndex.getPageStore().createData());
    
    paramPageBtreeIndex.getPageStore().logUndo(localPageBtreeNode, null);
    localPageBtreeNode.parentPageId = paramInt2;
    localPageBtreeNode.writeHead();
    
    localPageBtreeNode.start = (localPageBtreeNode.data.length() + 4);
    localPageBtreeNode.rows = SearchRow.EMPTY_ARRAY;
    if (localPageBtreeNode.pageStoreInternalCount) {
      localPageBtreeNode.rowCount = 0;
    }
    return localPageBtreeNode;
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
    this.rowCount = (this.rowCountStored = this.data.readInt());
    this.entryCount = this.data.readShortInt();
    this.childPageIds = new int[this.entryCount + 1];
    this.childPageIds[this.entryCount] = this.data.readInt();
    this.rows = (this.entryCount == 0 ? SearchRow.EMPTY_ARRAY : new SearchRow[this.entryCount]);
    this.offsets = Utils.newIntArray(this.entryCount);
    for (int k = 0; k < this.entryCount; k++)
    {
      this.childPageIds[k] = this.data.readInt();
      this.offsets[k] = this.data.readShortInt();
    }
    check();
    this.start = this.data.length();
    this.written = true;
  }
  
  private int addChildTry(SearchRow paramSearchRow)
  {
    if (this.entryCount < 4) {
      return -1;
    }
    int i;
    if (this.onlyPosition)
    {
      i = this.entryCount + 10;
    }
    else
    {
      int j = this.index.getRowSize(this.data, paramSearchRow, this.onlyPosition);
      int k = this.index.getPageStore().getPageSize();
      int m = this.entryCount == 0 ? k : this.offsets[(this.entryCount - 1)];
      i = m - j;
    }
    if (i < this.start + 6) {
      return this.entryCount / 2;
    }
    return -1;
  }
  
  private void addChild(int paramInt1, int paramInt2, SearchRow paramSearchRow)
  {
    int i = this.index.getRowSize(this.data, paramSearchRow, this.onlyPosition);
    int j = this.index.getPageStore().getPageSize();
    int k = this.entryCount == 0 ? j : this.offsets[(this.entryCount - 1)];
    if (k - i < this.start + 6)
    {
      readAllRows();
      this.onlyPosition = true;
      
      m = j;
      for (int n = 0; n < this.entryCount; n++)
      {
        m -= this.index.getRowSize(this.data, getRow(n), true);
        this.offsets[n] = m;
      }
      k = this.entryCount == 0 ? j : this.offsets[(this.entryCount - 1)];
      i = this.index.getRowSize(this.data, paramSearchRow, true);
      if ((SysProperties.CHECK) && (k - i < this.start + 6)) {
        throw DbException.throwInternalError();
      }
    }
    int m = k - i;
    if ((this.entryCount > 0) && 
      (paramInt1 < this.entryCount)) {
      m = (paramInt1 == 0 ? j : this.offsets[(paramInt1 - 1)]) - i;
    }
    this.rows = ((SearchRow[])insert(this.rows, this.entryCount, paramInt1, paramSearchRow));
    this.offsets = insert(this.offsets, this.entryCount, paramInt1, m);
    add(this.offsets, paramInt1 + 1, this.entryCount + 1, -i);
    this.childPageIds = insert(this.childPageIds, this.entryCount + 1, paramInt1 + 1, paramInt2);
    this.start += 6;
    if ((this.pageStoreInternalCount) && 
      (this.rowCount != -1)) {
      this.rowCount += m;
    }
    this.entryCount += 1;
    this.written = false;
    this.changeCount = this.index.getPageStore().getChangeCount();
  }
  
  int addRowTry(SearchRow paramSearchRow)
  {
    for (;;)
    {
      int i = find(paramSearchRow, false, true, true);
      PageBtree localPageBtree1 = this.index.getPage(this.childPageIds[i]);
      int j = localPageBtree1.addRowTry(paramSearchRow);
      if (j == -1) {
        break;
      }
      SearchRow localSearchRow = localPageBtree1.getRow(j - 1);
      this.index.getPageStore().logUndo(this, this.data);
      int k = addChildTry(localSearchRow);
      if (k != -1) {
        return k;
      }
      PageBtree localPageBtree2 = localPageBtree1.split(j);
      readAllRows();
      addChild(i, localPageBtree2.getPos(), localSearchRow);
      this.index.getPageStore().update(localPageBtree1);
      this.index.getPageStore().update(localPageBtree2);
      this.index.getPageStore().update(this);
    }
    updateRowCount(1);
    this.written = false;
    this.changeCount = this.index.getPageStore().getChangeCount();
    return -1;
  }
  
  private void updateRowCount(int paramInt)
  {
    if (this.rowCount != -1) {
      this.rowCount += paramInt;
    }
    if (this.rowCountStored != -1)
    {
      this.rowCountStored = -1;
      this.index.getPageStore().logUndo(this, this.data);
      if (this.written) {
        writeHead();
      }
      this.index.getPageStore().update(this);
    }
  }
  
  PageBtree split(int paramInt)
  {
    int i = this.index.getPageStore().allocatePage();
    PageBtreeNode localPageBtreeNode = create(this.index, i, this.parentPageId);
    this.index.getPageStore().logUndo(this, this.data);
    if (this.onlyPosition) {
      localPageBtreeNode.onlyPosition = true;
    }
    int j = this.childPageIds[paramInt];
    readAllRows();
    for (int k = paramInt; k < this.entryCount;)
    {
      localPageBtreeNode.addChild(localPageBtreeNode.entryCount, this.childPageIds[(paramInt + 1)], getRow(paramInt));
      removeChild(paramInt);
    }
    k = this.childPageIds[(paramInt - 1)];
    removeChild(paramInt - 1);
    this.childPageIds[(paramInt - 1)] = k;
    if (localPageBtreeNode.childPageIds == null) {
      localPageBtreeNode.childPageIds = new int[1];
    }
    localPageBtreeNode.childPageIds[0] = j;
    localPageBtreeNode.remapChildren();
    return localPageBtreeNode;
  }
  
  protected void remapChildren()
  {
    for (int i = 0; i < this.entryCount + 1; i++)
    {
      int j = this.childPageIds[i];
      PageBtree localPageBtree = this.index.getPage(j);
      localPageBtree.setParentPageId(getPos());
      this.index.getPageStore().update(localPageBtree);
    }
  }
  
  void init(PageBtree paramPageBtree1, SearchRow paramSearchRow, PageBtree paramPageBtree2)
  {
    this.entryCount = 0;
    this.childPageIds = new int[] { paramPageBtree1.getPos() };
    this.rows = SearchRow.EMPTY_ARRAY;
    this.offsets = Utils.EMPTY_INT_ARRAY;
    addChild(0, paramPageBtree2.getPos(), paramSearchRow);
    if (this.pageStoreInternalCount) {
      this.rowCount = (paramPageBtree1.getRowCount() + paramPageBtree2.getRowCount());
    }
    check();
  }
  
  void find(PageBtreeCursor paramPageBtreeCursor, SearchRow paramSearchRow, boolean paramBoolean)
  {
    int i = find(paramSearchRow, paramBoolean, false, false);
    if (i > this.entryCount)
    {
      if (this.parentPageId == 0) {
        return;
      }
      localObject = (PageBtreeNode)this.index.getPage(this.parentPageId);
      ((PageBtreeNode)localObject).find(paramPageBtreeCursor, paramSearchRow, paramBoolean);
      return;
    }
    Object localObject = this.index.getPage(this.childPageIds[i]);
    ((PageBtree)localObject).find(paramPageBtreeCursor, paramSearchRow, paramBoolean);
  }
  
  void last(PageBtreeCursor paramPageBtreeCursor)
  {
    int i = this.childPageIds[this.entryCount];
    this.index.getPage(i).last(paramPageBtreeCursor);
  }
  
  PageBtreeLeaf getFirstLeaf()
  {
    int i = this.childPageIds[0];
    return this.index.getPage(i).getFirstLeaf();
  }
  
  PageBtreeLeaf getLastLeaf()
  {
    int i = this.childPageIds[this.entryCount];
    return this.index.getPage(i).getLastLeaf();
  }
  
  SearchRow remove(SearchRow paramSearchRow)
  {
    int i = find(paramSearchRow, false, false, true);
    
    PageBtree localPageBtree = this.index.getPage(this.childPageIds[i]);
    SearchRow localSearchRow = localPageBtree.remove(paramSearchRow);
    this.index.getPageStore().logUndo(this, this.data);
    updateRowCount(-1);
    this.written = false;
    this.changeCount = this.index.getPageStore().getChangeCount();
    if (localSearchRow == null) {
      return null;
    }
    if (localSearchRow == paramSearchRow)
    {
      this.index.getPageStore().free(localPageBtree.getPos());
      if (this.entryCount < 1) {
        return paramSearchRow;
      }
      if (i == this.entryCount) {
        localSearchRow = getRow(i - 1);
      } else {
        localSearchRow = null;
      }
      removeChild(i);
      this.index.getPageStore().update(this);
      return localSearchRow;
    }
    if (i == this.entryCount) {
      return localSearchRow;
    }
    int j = this.childPageIds[i];
    removeChild(i);
    
    addChild(i, j, localSearchRow);
    
    int k = this.childPageIds[i];
    this.childPageIds[i] = this.childPageIds[(i + 1)];
    this.childPageIds[(i + 1)] = k;
    this.index.getPageStore().update(this);
    return null;
  }
  
  int getRowCount()
  {
    if (this.rowCount == -1)
    {
      int i = 0;
      for (int j = 0; j < this.entryCount + 1; j++)
      {
        int k = this.childPageIds[j];
        PageBtree localPageBtree = this.index.getPage(k);
        i += localPageBtree.getRowCount();
        this.index.getDatabase().setProgress(0, this.index.getName(), i, Integer.MAX_VALUE);
      }
      this.rowCount = i;
    }
    return this.rowCount;
  }
  
  void setRowCountStored(int paramInt)
  {
    if ((paramInt < 0) && (this.pageStoreInternalCount)) {
      return;
    }
    this.rowCount = paramInt;
    if (this.rowCountStored != paramInt)
    {
      this.rowCountStored = paramInt;
      this.index.getPageStore().logUndo(this, this.data);
      if (this.written)
      {
        this.changeCount = this.index.getPageStore().getChangeCount();
        writeHead();
      }
      this.index.getPageStore().update(this);
    }
  }
  
  private void check()
  {
    if (SysProperties.CHECK) {
      for (int i = 0; i < this.entryCount + 1; i++)
      {
        int j = this.childPageIds[i];
        if (j == 0) {
          DbException.throwInternalError();
        }
      }
    }
  }
  
  public void write()
  {
    check();
    writeData();
    this.index.getPageStore().writePage(getPos(), this.data);
  }
  
  private void writeHead()
  {
    this.data.reset();
    this.data.writeByte((byte)(0x5 | (this.onlyPosition ? 0 : 16)));
    
    this.data.writeShortInt(0);
    this.data.writeInt(this.parentPageId);
    this.data.writeVarInt(this.index.getId());
    this.data.writeInt(this.rowCountStored);
    this.data.writeShortInt(this.entryCount);
  }
  
  private void writeData()
  {
    if (this.written) {
      return;
    }
    readAllRows();
    writeHead();
    this.data.writeInt(this.childPageIds[this.entryCount]);
    for (int i = 0; i < this.entryCount; i++)
    {
      this.data.writeInt(this.childPageIds[i]);
      this.data.writeShortInt(this.offsets[i]);
    }
    for (i = 0; i < this.entryCount; i++) {
      this.index.writeRow(this.data, this.offsets[i], this.rows[i], this.onlyPosition);
    }
    this.written = true;
  }
  
  void freeRecursive()
  {
    this.index.getPageStore().logUndo(this, this.data);
    this.index.getPageStore().free(getPos());
    for (int i = 0; i < this.entryCount + 1; i++)
    {
      int j = this.childPageIds[i];
      this.index.getPage(j).freeRecursive();
    }
  }
  
  private void removeChild(int paramInt)
  {
    readAllRows();
    this.entryCount -= 1;
    if (this.pageStoreInternalCount) {
      updateRowCount(-this.index.getPage(this.childPageIds[paramInt]).getRowCount());
    }
    this.written = false;
    this.changeCount = this.index.getPageStore().getChangeCount();
    if (this.entryCount < 0) {
      DbException.throwInternalError();
    }
    if (this.entryCount > paramInt)
    {
      int i = paramInt > 0 ? this.offsets[(paramInt - 1)] : this.index.getPageStore().getPageSize();
      int j = i - this.offsets[paramInt];
      add(this.offsets, paramInt, this.entryCount + 1, j);
    }
    this.rows = ((SearchRow[])remove(this.rows, this.entryCount + 1, paramInt));
    this.offsets = remove(this.offsets, this.entryCount + 1, paramInt);
    this.childPageIds = remove(this.childPageIds, this.entryCount + 2, paramInt);
    this.start -= 6;
  }
  
  void nextPage(PageBtreeCursor paramPageBtreeCursor, int paramInt)
  {
    for (int i = 0; i < this.entryCount + 1; i++) {
      if (this.childPageIds[i] == paramInt)
      {
        i++;
        break;
      }
    }
    if (i > this.entryCount)
    {
      if (this.parentPageId == 0)
      {
        paramPageBtreeCursor.setCurrent(null, 0);
        return;
      }
      localObject = (PageBtreeNode)this.index.getPage(this.parentPageId);
      ((PageBtreeNode)localObject).nextPage(paramPageBtreeCursor, getPos());
      return;
    }
    Object localObject = this.index.getPage(this.childPageIds[i]);
    PageBtreeLeaf localPageBtreeLeaf = ((PageBtree)localObject).getFirstLeaf();
    paramPageBtreeCursor.setCurrent(localPageBtreeLeaf, 0);
  }
  
  void previousPage(PageBtreeCursor paramPageBtreeCursor, int paramInt)
  {
    for (int i = this.entryCount; i >= 0; i--) {
      if (this.childPageIds[i] == paramInt)
      {
        i--;
        break;
      }
    }
    if (i < 0)
    {
      if (this.parentPageId == 0)
      {
        paramPageBtreeCursor.setCurrent(null, 0);
        return;
      }
      localObject = (PageBtreeNode)this.index.getPage(this.parentPageId);
      ((PageBtreeNode)localObject).previousPage(paramPageBtreeCursor, getPos());
      return;
    }
    Object localObject = this.index.getPage(this.childPageIds[i]);
    PageBtreeLeaf localPageBtreeLeaf = ((PageBtree)localObject).getLastLeaf();
    paramPageBtreeCursor.setCurrent(localPageBtreeLeaf, localPageBtreeLeaf.entryCount - 1);
  }
  
  public String toString()
  {
    return "page[" + getPos() + "] b-tree node table:" + this.index.getId() + " entries:" + this.entryCount;
  }
  
  public void moveTo(Session paramSession, int paramInt)
  {
    PageStore localPageStore = this.index.getPageStore();
    localPageStore.logUndo(this, this.data);
    PageBtreeNode localPageBtreeNode1 = create(this.index, paramInt, this.parentPageId);
    readAllRows();
    localPageBtreeNode1.rowCountStored = this.rowCountStored;
    localPageBtreeNode1.rowCount = this.rowCount;
    localPageBtreeNode1.childPageIds = this.childPageIds;
    localPageBtreeNode1.rows = this.rows;
    localPageBtreeNode1.entryCount = this.entryCount;
    localPageBtreeNode1.offsets = this.offsets;
    localPageBtreeNode1.onlyPosition = this.onlyPosition;
    localPageBtreeNode1.parentPageId = this.parentPageId;
    localPageBtreeNode1.start = this.start;
    localPageStore.update(localPageBtreeNode1);
    if (this.parentPageId == 0)
    {
      this.index.setRootPageId(paramSession, paramInt);
    }
    else
    {
      Page localPage = localPageStore.getPage(this.parentPageId);
      if (!(localPage instanceof PageBtreeNode)) {
        throw DbException.throwInternalError();
      }
      PageBtreeNode localPageBtreeNode2 = (PageBtreeNode)localPage;
      localPageBtreeNode2.moveChild(getPos(), paramInt);
    }
    for (int i = 0; i < this.entryCount + 1; i++)
    {
      int j = this.childPageIds[i];
      PageBtree localPageBtree = this.index.getPage(j);
      localPageBtree.setParentPageId(paramInt);
      localPageStore.update(localPageBtree);
    }
    localPageStore.free(getPos());
  }
  
  void moveChild(int paramInt1, int paramInt2)
  {
    for (int i = 0; i < this.entryCount + 1; i++) {
      if (this.childPageIds[i] == paramInt1)
      {
        this.index.getPageStore().logUndo(this, this.data);
        this.written = false;
        this.changeCount = this.index.getPageStore().getChangeCount();
        this.childPageIds[i] = paramInt2;
        this.index.getPageStore().update(this);
        return;
      }
    }
    throw DbException.throwInternalError();
  }
}
