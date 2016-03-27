package org.h2.index;

import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.store.Data;
import org.h2.store.Page;
import org.h2.store.PageStore;

public class PageDataOverflow
  extends Page
{
  static final int START_LAST = 9;
  static final int START_MORE = 11;
  private static final int START_NEXT_OVERFLOW = 7;
  private final PageStore store;
  private int type;
  private int parentPageId;
  private int nextPage;
  private final Data data;
  private int start;
  private int size;
  
  private PageDataOverflow(PageStore paramPageStore, int paramInt, Data paramData)
  {
    this.store = paramPageStore;
    setPos(paramInt);
    this.data = paramData;
  }
  
  public static Page read(PageStore paramPageStore, Data paramData, int paramInt)
  {
    PageDataOverflow localPageDataOverflow = new PageDataOverflow(paramPageStore, paramInt, paramData);
    localPageDataOverflow.read();
    return localPageDataOverflow;
  }
  
  static PageDataOverflow create(PageStore paramPageStore, int paramInt1, int paramInt2, int paramInt3, int paramInt4, Data paramData, int paramInt5, int paramInt6)
  {
    Data localData = paramPageStore.createData();
    PageDataOverflow localPageDataOverflow = new PageDataOverflow(paramPageStore, paramInt1, localData);
    paramPageStore.logUndo(localPageDataOverflow, null);
    localData.writeByte((byte)paramInt2);
    localData.writeShortInt(0);
    localData.writeInt(paramInt3);
    if (paramInt2 == 3) {
      localData.writeInt(paramInt4);
    } else {
      localData.writeShortInt(paramInt6);
    }
    localPageDataOverflow.start = localData.length();
    localData.write(paramData.getBytes(), paramInt5, paramInt6);
    localPageDataOverflow.type = paramInt2;
    localPageDataOverflow.parentPageId = paramInt3;
    localPageDataOverflow.nextPage = paramInt4;
    localPageDataOverflow.size = paramInt6;
    return localPageDataOverflow;
  }
  
  private void read()
  {
    this.data.reset();
    this.type = this.data.readByte();
    this.data.readShortInt();
    this.parentPageId = this.data.readInt();
    if (this.type == 19)
    {
      this.size = this.data.readShortInt();
      this.nextPage = 0;
    }
    else if (this.type == 3)
    {
      this.nextPage = this.data.readInt();
      this.size = (this.store.getPageSize() - this.data.length());
    }
    else
    {
      throw DbException.get(90030, "page:" + getPos() + " type:" + this.type);
    }
    this.start = this.data.length();
  }
  
  int readInto(Data paramData)
  {
    paramData.checkCapacity(this.size);
    if (this.type == 19)
    {
      paramData.write(this.data.getBytes(), 9, this.size);
      return 0;
    }
    paramData.write(this.data.getBytes(), 11, this.size);
    return this.nextPage;
  }
  
  int getNextOverflow()
  {
    return this.nextPage;
  }
  
  private void writeHead()
  {
    this.data.writeByte((byte)this.type);
    this.data.writeShortInt(0);
    this.data.writeInt(this.parentPageId);
  }
  
  public void write()
  {
    writeData();
    this.store.writePage(getPos(), this.data);
  }
  
  private void writeData()
  {
    this.data.reset();
    writeHead();
    if (this.type == 3) {
      this.data.writeInt(this.nextPage);
    } else {
      this.data.writeShortInt(this.size);
    }
  }
  
  public String toString()
  {
    return "page[" + getPos() + "] data leaf overflow parent:" + this.parentPageId + " next:" + this.nextPage;
  }
  
  public int getMemory()
  {
    return 120 + this.store.getPageSize() >> 2;
  }
  
  void setParentPageId(int paramInt)
  {
    this.store.logUndo(this, this.data);
    this.parentPageId = paramInt;
  }
  
  public void moveTo(Session paramSession, int paramInt)
  {
    Page localPage = this.store.getPage(this.parentPageId);
    if (localPage == null) {
      throw DbException.throwInternalError();
    }
    PageDataOverflow localPageDataOverflow1 = null;
    if (this.nextPage != 0) {
      localPageDataOverflow1 = (PageDataOverflow)this.store.getPage(this.nextPage);
    }
    this.store.logUndo(this, this.data);
    PageDataOverflow localPageDataOverflow2 = create(this.store, paramInt, this.type, this.parentPageId, this.nextPage, this.data, this.start, this.size);
    
    this.store.update(localPageDataOverflow2);
    if (localPageDataOverflow1 != null)
    {
      localPageDataOverflow1.setParentPageId(paramInt);
      this.store.update(localPageDataOverflow1);
    }
    Object localObject;
    if ((localPage instanceof PageDataOverflow))
    {
      localObject = (PageDataOverflow)localPage;
      ((PageDataOverflow)localObject).setNext(getPos(), paramInt);
    }
    else
    {
      localObject = (PageDataLeaf)localPage;
      ((PageDataLeaf)localObject).setOverflow(getPos(), paramInt);
    }
    this.store.update(localPage);
    this.store.free(getPos());
  }
  
  private void setNext(int paramInt1, int paramInt2)
  {
    if ((SysProperties.CHECK) && (paramInt1 != this.nextPage)) {
      DbException.throwInternalError("move " + this + " " + paramInt2);
    }
    this.store.logUndo(this, this.data);
    this.nextPage = paramInt2;
    this.data.setInt(7, paramInt2);
  }
  
  void free()
  {
    this.store.logUndo(this, this.data);
    this.store.free(getPos());
  }
  
  public boolean canRemove()
  {
    return true;
  }
  
  public boolean isStream()
  {
    return true;
  }
}
