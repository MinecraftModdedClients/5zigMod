package org.h2.store;

import org.h2.engine.Session;
import org.h2.message.DbException;

public class PageStreamTrunk
  extends Page
{
  private static final int DATA_START = 17;
  int parent;
  int nextTrunk;
  private final PageStore store;
  private int logKey;
  private int[] pageIds;
  private int pageCount;
  private Data data;
  
  private PageStreamTrunk(PageStore paramPageStore, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int[] paramArrayOfInt)
  {
    setPos(paramInt2);
    this.parent = paramInt1;
    this.store = paramPageStore;
    this.nextTrunk = paramInt3;
    this.logKey = paramInt4;
    this.pageCount = paramArrayOfInt.length;
    this.pageIds = paramArrayOfInt;
  }
  
  private PageStreamTrunk(PageStore paramPageStore, Data paramData, int paramInt)
  {
    setPos(paramInt);
    this.data = paramData;
    this.store = paramPageStore;
  }
  
  static PageStreamTrunk read(PageStore paramPageStore, Data paramData, int paramInt)
  {
    PageStreamTrunk localPageStreamTrunk = new PageStreamTrunk(paramPageStore, paramData, paramInt);
    localPageStreamTrunk.read();
    return localPageStreamTrunk;
  }
  
  static PageStreamTrunk create(PageStore paramPageStore, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int[] paramArrayOfInt)
  {
    return new PageStreamTrunk(paramPageStore, paramInt1, paramInt2, paramInt3, paramInt4, paramArrayOfInt);
  }
  
  private void read()
  {
    this.data.reset();
    this.data.readByte();
    this.data.readShortInt();
    this.parent = this.data.readInt();
    this.logKey = this.data.readInt();
    this.nextTrunk = this.data.readInt();
    this.pageCount = this.data.readShortInt();
    this.pageIds = new int[this.pageCount];
    for (int i = 0; i < this.pageCount; i++) {
      this.pageIds[i] = this.data.readInt();
    }
  }
  
  int getPageData(int paramInt)
  {
    if (paramInt >= this.pageIds.length) {
      return -1;
    }
    return this.pageIds[paramInt];
  }
  
  public void write()
  {
    this.data = this.store.createData();
    this.data.writeByte((byte)7);
    this.data.writeShortInt(0);
    this.data.writeInt(this.parent);
    this.data.writeInt(this.logKey);
    this.data.writeInt(this.nextTrunk);
    this.data.writeShortInt(this.pageCount);
    for (int i = 0; i < this.pageCount; i++) {
      this.data.writeInt(this.pageIds[i]);
    }
    this.store.writePage(getPos(), this.data);
  }
  
  static int getPagesAddressed(int paramInt)
  {
    return (paramInt - 17) / 4;
  }
  
  boolean contains(int paramInt)
  {
    for (int i = 0; i < this.pageCount; i++) {
      if (this.pageIds[i] == paramInt) {
        return true;
      }
    }
    return false;
  }
  
  int free(int paramInt)
  {
    this.store.free(getPos(), false);
    int i = 1;
    int j = 0;
    for (int k = 0; k < this.pageCount; k++)
    {
      int m = this.pageIds[k];
      if (j != 0) {
        this.store.freeUnused(m);
      } else {
        this.store.free(m, false);
      }
      i++;
      if (m == paramInt) {
        j = 1;
      }
    }
    return i;
  }
  
  public int getMemory()
  {
    return this.store.getPageSize() >> 2;
  }
  
  public void moveTo(Session paramSession, int paramInt) {}
  
  int getLogKey()
  {
    return this.logKey;
  }
  
  public int getNextTrunk()
  {
    return this.nextTrunk;
  }
  
  static class Iterator
  {
    private final PageStore store;
    private int first;
    private int next;
    private int previous;
    private boolean canDelete;
    private int current;
    
    Iterator(PageStore paramPageStore, int paramInt)
    {
      this.store = paramPageStore;
      this.next = paramInt;
    }
    
    int getCurrentPageId()
    {
      return this.current;
    }
    
    PageStreamTrunk next()
    {
      this.canDelete = false;
      if (this.first == 0) {
        this.first = this.next;
      } else if (this.first == this.next) {
        return null;
      }
      if ((this.next == 0) || (this.next >= this.store.getPageCount())) {
        return null;
      }
      this.current = this.next;
      Page localPage;
      try
      {
        localPage = this.store.getPage(this.next);
      }
      catch (DbException localDbException)
      {
        if (localDbException.getErrorCode() == 90030) {
          return null;
        }
        throw localDbException;
      }
      if ((localPage == null) || ((localPage instanceof PageStreamTrunk)) || ((localPage instanceof PageStreamData))) {
        this.canDelete = true;
      }
      if (!(localPage instanceof PageStreamTrunk)) {
        return null;
      }
      PageStreamTrunk localPageStreamTrunk = (PageStreamTrunk)localPage;
      if ((this.previous > 0) && (localPageStreamTrunk.parent != this.previous)) {
        return null;
      }
      this.previous = this.next;
      this.next = localPageStreamTrunk.nextTrunk;
      return localPageStreamTrunk;
    }
    
    boolean canDelete()
    {
      return this.canDelete;
    }
  }
  
  public boolean canRemove()
  {
    return true;
  }
  
  public String toString()
  {
    return "page[" + getPos() + "] stream trunk key:" + this.logKey + " next:" + this.nextTrunk;
  }
  
  public boolean canMove()
  {
    return false;
  }
}
