package org.h2.store;

import org.h2.engine.Session;
import org.h2.util.BitField;

public class PageFreeList
  extends Page
{
  private static final int DATA_START = 3;
  private final PageStore store;
  private final BitField used;
  private final int pageCount;
  private boolean full;
  private Data data;
  
  private PageFreeList(PageStore paramPageStore, int paramInt)
  {
    setPos(paramInt);
    this.store = paramPageStore;
    this.pageCount = ((paramPageStore.getPageSize() - 3) * 8);
    this.used = new BitField(this.pageCount);
    this.used.set(0);
  }
  
  static PageFreeList read(PageStore paramPageStore, Data paramData, int paramInt)
  {
    PageFreeList localPageFreeList = new PageFreeList(paramPageStore, paramInt);
    localPageFreeList.data = paramData;
    localPageFreeList.read();
    return localPageFreeList;
  }
  
  static PageFreeList create(PageStore paramPageStore, int paramInt)
  {
    return new PageFreeList(paramPageStore, paramInt);
  }
  
  int allocate(BitField paramBitField, int paramInt)
  {
    if (this.full) {
      return -1;
    }
    int i = Math.max(0, paramInt - getPos());
    for (;;)
    {
      int j = this.used.nextClearBit(i);
      if (j >= this.pageCount)
      {
        if (i == 0) {
          this.full = true;
        }
        return -1;
      }
      if ((paramBitField != null) && (paramBitField.get(j + getPos())))
      {
        i = paramBitField.nextClearBit(j + getPos()) - getPos();
        if (i >= this.pageCount) {
          return -1;
        }
      }
      else
      {
        this.used.set(j);
        this.store.logUndo(this, this.data);
        this.store.update(this);
        return j + getPos();
      }
    }
  }
  
  int getFirstFree(int paramInt)
  {
    if (this.full) {
      return -1;
    }
    int i = Math.max(0, paramInt - getPos());
    int j = this.used.nextClearBit(i);
    if (j >= this.pageCount) {
      return -1;
    }
    return j + getPos();
  }
  
  int getLastUsed()
  {
    int i = this.used.length() - 1;
    return i <= 0 ? -1 : i + getPos();
  }
  
  void allocate(int paramInt)
  {
    int i = paramInt - getPos();
    if ((i >= 0) && (!this.used.get(i)))
    {
      this.used.set(i);
      this.store.logUndo(this, this.data);
      this.store.update(this);
    }
  }
  
  void free(int paramInt)
  {
    this.full = false;
    this.store.logUndo(this, this.data);
    this.used.clear(paramInt - getPos());
    this.store.update(this);
  }
  
  private void read()
  {
    this.data.reset();
    this.data.readByte();
    this.data.readShortInt();
    for (int i = 0; i < this.pageCount; i += 8)
    {
      int j = this.data.readByte() & 0xFF;
      this.used.setByte(i, j);
    }
    this.full = false;
  }
  
  public void write()
  {
    this.data = this.store.createData();
    this.data.writeByte((byte)6);
    this.data.writeShortInt(0);
    for (int i = 0; i < this.pageCount; i += 8) {
      this.data.writeByte((byte)this.used.getByte(i));
    }
    this.store.writePage(getPos(), this.data);
  }
  
  public static int getPagesAddressed(int paramInt)
  {
    return (paramInt - 3) * 8;
  }
  
  public int getMemory()
  {
    return this.store.getPageSize() >> 2;
  }
  
  boolean isUsed(int paramInt)
  {
    return this.used.get(paramInt - getPos());
  }
  
  public void moveTo(Session paramSession, int paramInt)
  {
    this.store.free(getPos(), false);
  }
  
  public String toString()
  {
    return "page [" + getPos() + "] freeList" + (this.full ? "full" : "");
  }
  
  public boolean canRemove()
  {
    return true;
  }
  
  public boolean canMove()
  {
    return false;
  }
}
