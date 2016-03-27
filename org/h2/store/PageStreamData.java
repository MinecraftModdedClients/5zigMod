package org.h2.store;

import org.h2.engine.Session;

public class PageStreamData
  extends Page
{
  private static final int DATA_START = 11;
  private final PageStore store;
  private int trunk;
  private int logKey;
  private Data data;
  private int remaining;
  
  private PageStreamData(PageStore paramPageStore, int paramInt1, int paramInt2, int paramInt3)
  {
    setPos(paramInt1);
    this.store = paramPageStore;
    this.trunk = paramInt2;
    this.logKey = paramInt3;
  }
  
  static PageStreamData read(PageStore paramPageStore, Data paramData, int paramInt)
  {
    PageStreamData localPageStreamData = new PageStreamData(paramPageStore, paramInt, 0, 0);
    localPageStreamData.data = paramData;
    localPageStreamData.read();
    return localPageStreamData;
  }
  
  static PageStreamData create(PageStore paramPageStore, int paramInt1, int paramInt2, int paramInt3)
  {
    return new PageStreamData(paramPageStore, paramInt1, paramInt2, paramInt3);
  }
  
  private void read()
  {
    this.data.reset();
    this.data.readByte();
    this.data.readShortInt();
    this.trunk = this.data.readInt();
    this.logKey = this.data.readInt();
  }
  
  void initWrite()
  {
    this.data = this.store.createData();
    this.data.writeByte((byte)8);
    this.data.writeShortInt(0);
    this.data.writeInt(this.trunk);
    this.data.writeInt(this.logKey);
    this.remaining = (this.store.getPageSize() - this.data.length());
  }
  
  int write(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    int i = Math.min(this.remaining, paramInt2);
    this.data.write(paramArrayOfByte, paramInt1, i);
    this.remaining -= i;
    return i;
  }
  
  public void write()
  {
    this.store.writePage(getPos(), this.data);
  }
  
  static int getCapacity(int paramInt)
  {
    return paramInt - 11;
  }
  
  void read(int paramInt1, byte[] paramArrayOfByte, int paramInt2, int paramInt3)
  {
    System.arraycopy(this.data.getBytes(), paramInt1, paramArrayOfByte, paramInt2, paramInt3);
  }
  
  int getRemaining()
  {
    return this.remaining;
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
  
  public String toString()
  {
    return "[" + getPos() + "] stream data key:" + this.logKey + " pos:" + this.data.length() + " remaining:" + this.remaining;
  }
  
  public boolean canRemove()
  {
    return true;
  }
  
  public static int getReadStart()
  {
    return 11;
  }
  
  public boolean canMove()
  {
    return false;
  }
}
