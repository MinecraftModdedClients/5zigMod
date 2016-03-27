package org.h2.index;

import java.util.Arrays;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.store.Data;
import org.h2.store.Page;
import org.h2.store.PageStore;
import org.h2.util.Utils;

public class PageDataNode
  extends PageData
{
  private int[] childPageIds;
  private int rowCountStored = -1;
  private int rowCount = -1;
  private int length;
  
  private PageDataNode(PageDataIndex paramPageDataIndex, int paramInt, Data paramData)
  {
    super(paramPageDataIndex, paramInt, paramData);
  }
  
  static PageDataNode create(PageDataIndex paramPageDataIndex, int paramInt1, int paramInt2)
  {
    PageDataNode localPageDataNode = new PageDataNode(paramPageDataIndex, paramInt1, paramPageDataIndex.getPageStore().createData());
    
    paramPageDataIndex.getPageStore().logUndo(localPageDataNode, null);
    localPageDataNode.parentPageId = paramInt2;
    localPageDataNode.writeHead();
    
    localPageDataNode.length = (localPageDataNode.data.length() + 4);
    return localPageDataNode;
  }
  
  public static Page read(PageDataIndex paramPageDataIndex, Data paramData, int paramInt)
  {
    PageDataNode localPageDataNode = new PageDataNode(paramPageDataIndex, paramInt, paramData);
    localPageDataNode.read();
    return localPageDataNode;
  }
  
  private void read()
  {
    this.data.reset();
    this.data.readByte();
    this.data.readShortInt();
    this.parentPageId = this.data.readInt();
    int i = this.data.readVarInt();
    if (i != this.index.getId()) {
      throw DbException.get(90030, "page:" + getPos() + " expected index:" + this.index.getId() + "got:" + i);
    }
    this.rowCount = (this.rowCountStored = this.data.readInt());
    this.entryCount = this.data.readShortInt();
    this.childPageIds = new int[this.entryCount + 1];
    this.childPageIds[this.entryCount] = this.data.readInt();
    this.keys = Utils.newLongArray(this.entryCount);
    for (int j = 0; j < this.entryCount; j++)
    {
      this.childPageIds[j] = this.data.readInt();
      this.keys[j] = this.data.readVarLong();
    }
    this.length = this.data.length();
    check();
    this.written = true;
  }
  
  private void addChild(int paramInt1, int paramInt2, long paramLong)
  {
    this.index.getPageStore().logUndo(this, this.data);
    this.written = false;
    this.changeCount = this.index.getPageStore().getChangeCount();
    this.childPageIds = insert(this.childPageIds, this.entryCount + 1, paramInt1 + 1, paramInt2);
    this.keys = insert(this.keys, this.entryCount, paramInt1, paramLong);
    this.entryCount += 1;
    this.length += 4 + Data.getVarLongLen(paramLong);
  }
  
  int addRowTry(Row paramRow)
  {
    this.index.getPageStore().logUndo(this, this.data);
    int i = 4 + Data.getVarLongLen(paramRow.getKey());
    for (;;)
    {
      int j = find(paramRow.getKey());
      PageData localPageData1 = this.index.getPage(this.childPageIds[j], getPos());
      int k = localPageData1.addRowTry(paramRow);
      if (k == -1) {
        break;
      }
      if (this.length + i > this.index.getPageStore().getPageSize()) {
        return this.entryCount / 2;
      }
      long l = k == 0 ? paramRow.getKey() : localPageData1.getKey(k - 1);
      PageData localPageData2 = localPageData1.split(k);
      this.index.getPageStore().update(localPageData1);
      this.index.getPageStore().update(localPageData2);
      addChild(j, localPageData2.getPos(), l);
      this.index.getPageStore().update(this);
    }
    updateRowCount(1);
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
  
  Cursor find(Session paramSession, long paramLong1, long paramLong2, boolean paramBoolean)
  {
    int i = find(paramLong1);
    int j = this.childPageIds[i];
    return this.index.getPage(j, getPos()).find(paramSession, paramLong1, paramLong2, paramBoolean);
  }
  
  PageData split(int paramInt)
  {
    int i = this.index.getPageStore().allocatePage();
    PageDataNode localPageDataNode = create(this.index, i, this.parentPageId);
    int j = this.childPageIds[paramInt];
    for (int k = paramInt; k < this.entryCount;)
    {
      localPageDataNode.addChild(localPageDataNode.entryCount, this.childPageIds[(paramInt + 1)], this.keys[paramInt]);
      removeChild(paramInt);
    }
    k = this.childPageIds[(paramInt - 1)];
    removeChild(paramInt - 1);
    this.childPageIds[(paramInt - 1)] = k;
    localPageDataNode.childPageIds[0] = j;
    localPageDataNode.remapChildren(getPos());
    return localPageDataNode;
  }
  
  protected void remapChildren(int paramInt)
  {
    for (int i = 0; i < this.entryCount + 1; i++)
    {
      int j = this.childPageIds[i];
      PageData localPageData = this.index.getPage(j, paramInt);
      localPageData.setParentPageId(getPos());
      this.index.getPageStore().update(localPageData);
    }
  }
  
  void init(PageData paramPageData1, long paramLong, PageData paramPageData2)
  {
    this.entryCount = 1;
    this.childPageIds = new int[] { paramPageData1.getPos(), paramPageData2.getPos() };
    this.keys = new long[] { paramLong };
    this.length += 4 + Data.getVarLongLen(paramLong);
    check();
  }
  
  long getLastKey()
  {
    return this.index.getPage(this.childPageIds[this.entryCount], getPos()).getLastKey();
  }
  
  PageDataLeaf getNextPage(long paramLong)
  {
    int i = find(paramLong) + 1;
    if (i > this.entryCount)
    {
      if (this.parentPageId == 0) {
        return null;
      }
      localObject = (PageDataNode)this.index.getPage(this.parentPageId, -1);
      return ((PageDataNode)localObject).getNextPage(paramLong);
    }
    Object localObject = this.index.getPage(this.childPageIds[i], getPos());
    return ((PageData)localObject).getFirstLeaf();
  }
  
  PageDataLeaf getFirstLeaf()
  {
    int i = this.childPageIds[0];
    return this.index.getPage(i, getPos()).getFirstLeaf();
  }
  
  boolean remove(long paramLong)
  {
    int i = find(paramLong);
    
    PageData localPageData = this.index.getPage(this.childPageIds[i], getPos());
    boolean bool = localPageData.remove(paramLong);
    this.index.getPageStore().logUndo(this, this.data);
    updateRowCount(-1);
    if (!bool) {
      return false;
    }
    this.index.getPageStore().free(localPageData.getPos());
    if (this.entryCount < 1) {
      return true;
    }
    removeChild(i);
    this.index.getPageStore().update(this);
    return false;
  }
  
  void freeRecursive()
  {
    this.index.getPageStore().logUndo(this, this.data);
    this.index.getPageStore().free(getPos());
    for (int i = 0; i < this.entryCount + 1; i++)
    {
      int j = this.childPageIds[i];
      this.index.getPage(j, getPos()).freeRecursive();
    }
  }
  
  Row getRowWithKey(long paramLong)
  {
    int i = find(paramLong);
    PageData localPageData = this.index.getPage(this.childPageIds[i], getPos());
    return localPageData.getRowWithKey(paramLong);
  }
  
  int getRowCount()
  {
    if (this.rowCount == -1)
    {
      int i = 0;
      for (int j = 0; j < this.entryCount + 1; j++)
      {
        int k = this.childPageIds[j];
        PageData localPageData = this.index.getPage(k, getPos());
        if (getPos() == localPageData.getPos()) {
          throw DbException.throwInternalError("Page is its own child: " + getPos());
        }
        i += localPageData.getRowCount();
        this.index.getDatabase().setProgress(0, this.index.getTable() + "." + this.index.getName(), i, Integer.MAX_VALUE);
      }
      this.rowCount = i;
    }
    return this.rowCount;
  }
  
  long getDiskSpaceUsed()
  {
    long l = 0L;
    for (int i = 0; i < this.entryCount + 1; i++)
    {
      int j = this.childPageIds[i];
      PageData localPageData = this.index.getPage(j, getPos());
      if (getPos() == localPageData.getPos()) {
        throw DbException.throwInternalError("Page is its own child: " + getPos());
      }
      l += localPageData.getDiskSpaceUsed();
      this.index.getDatabase().setProgress(0, this.index.getTable() + "." + this.index.getName(), (int)(l >> 16), Integer.MAX_VALUE);
    }
    return l;
  }
  
  void setRowCountStored(int paramInt)
  {
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
    writeData();
    this.index.getPageStore().writePage(getPos(), this.data);
  }
  
  private void writeHead()
  {
    this.data.reset();
    this.data.writeByte((byte)2);
    this.data.writeShortInt(0);
    if ((SysProperties.CHECK2) && 
      (this.data.length() != 3)) {
      DbException.throwInternalError();
    }
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
    check();
    writeHead();
    this.data.writeInt(this.childPageIds[this.entryCount]);
    for (int i = 0; i < this.entryCount; i++)
    {
      this.data.writeInt(this.childPageIds[i]);
      this.data.writeVarLong(this.keys[i]);
    }
    if (this.length != this.data.length()) {
      DbException.throwInternalError("expected pos: " + this.length + " got: " + this.data.length());
    }
    this.written = true;
  }
  
  private void removeChild(int paramInt)
  {
    this.index.getPageStore().logUndo(this, this.data);
    this.written = false;
    this.changeCount = this.index.getPageStore().getChangeCount();
    int i = paramInt < this.entryCount ? paramInt : paramInt - 1;
    this.entryCount -= 1;
    this.length -= 4 + Data.getVarLongLen(this.keys[i]);
    if (this.entryCount < 0) {
      DbException.throwInternalError();
    }
    this.keys = remove(this.keys, this.entryCount + 1, i);
    this.childPageIds = remove(this.childPageIds, this.entryCount + 2, paramInt);
  }
  
  public String toString()
  {
    return "page[" + getPos() + "] data node table:" + this.index.getId() + " entries:" + this.entryCount + " " + Arrays.toString(this.childPageIds);
  }
  
  public void moveTo(Session paramSession, int paramInt)
  {
    PageStore localPageStore = this.index.getPageStore();
    for (int i = 0; i < this.entryCount + 1; i++)
    {
      int j = this.childPageIds[i];
      localPageStore.getPage(j);
    }
    if (this.parentPageId != 0) {
      localPageStore.getPage(this.parentPageId);
    }
    localPageStore.logUndo(this, this.data);
    PageDataNode localPageDataNode1 = create(this.index, paramInt, this.parentPageId);
    localPageDataNode1.rowCountStored = this.rowCountStored;
    localPageDataNode1.rowCount = this.rowCount;
    localPageDataNode1.childPageIds = this.childPageIds;
    localPageDataNode1.keys = this.keys;
    localPageDataNode1.entryCount = this.entryCount;
    localPageDataNode1.length = this.length;
    localPageStore.update(localPageDataNode1);
    if (this.parentPageId == 0)
    {
      this.index.setRootPageId(paramSession, paramInt);
    }
    else
    {
      PageDataNode localPageDataNode2 = (PageDataNode)localPageStore.getPage(this.parentPageId);
      localPageDataNode2.moveChild(getPos(), paramInt);
    }
    for (int k = 0; k < this.entryCount + 1; k++)
    {
      int m = this.childPageIds[k];
      PageData localPageData = (PageData)localPageStore.getPage(m);
      localPageData.setParentPageId(paramInt);
      localPageStore.update(localPageData);
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
