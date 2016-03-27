package org.h2.store;

import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.util.BitField;
import org.h2.util.IntArray;

public class PageOutputStream
{
  private PageStore store;
  private final Trace trace;
  private final BitField exclude;
  private final boolean atEnd;
  private final int minPageId;
  private int trunkPageId;
  private int trunkNext;
  private IntArray reservedPages = new IntArray();
  private PageStreamTrunk trunk;
  private int trunkIndex;
  private PageStreamData data;
  private int reserved;
  private boolean needFlush;
  private boolean writing;
  private int pageCount;
  private int logKey;
  
  public PageOutputStream(PageStore paramPageStore, int paramInt1, BitField paramBitField, int paramInt2, boolean paramBoolean)
  {
    this.trace = paramPageStore.getTrace();
    this.store = paramPageStore;
    this.trunkPageId = paramInt1;
    this.exclude = paramBitField;
    
    this.logKey = (paramInt2 - 1);
    this.atEnd = paramBoolean;
    this.minPageId = (paramBoolean ? paramInt1 : 0);
  }
  
  void reserve(int paramInt)
  {
    if (this.reserved < paramInt)
    {
      int i = this.store.getPageSize();
      int j = PageStreamData.getCapacity(i);
      int k = PageStreamTrunk.getPagesAddressed(i);
      int m = 0;int n = 0;
      do
      {
        m += k + 1;
        n += k * j;
      } while (n < paramInt);
      int i1 = this.atEnd ? this.trunkPageId : 0;
      this.store.allocatePages(this.reservedPages, m, this.exclude, i1);
      this.reserved += n;
      if (this.data == null) {
        initNextData();
      }
    }
  }
  
  private void initNextData()
  {
    int i = this.trunk == null ? -1 : this.trunk.getPageData(this.trunkIndex++);
    if (i == -1)
    {
      int j = this.trunkPageId;
      if (this.trunkNext != 0) {
        this.trunkPageId = this.trunkNext;
      }
      int k = PageStreamTrunk.getPagesAddressed(this.store.getPageSize());
      int[] arrayOfInt = new int[k];
      for (int m = 0; m < k; m++) {
        arrayOfInt[m] = this.reservedPages.get(m);
      }
      this.trunkNext = this.reservedPages.get(k);
      this.logKey += 1;
      this.trunk = PageStreamTrunk.create(this.store, j, this.trunkPageId, this.trunkNext, this.logKey, arrayOfInt);
      
      this.trunkIndex = 0;
      this.pageCount += 1;
      this.trunk.write();
      this.reservedPages.removeRange(0, k + 1);
      i = this.trunk.getPageData(this.trunkIndex++);
    }
    this.data = PageStreamData.create(this.store, i, this.trunk.getPos(), this.logKey);
    this.pageCount += 1;
    this.data.initWrite();
  }
  
  public void write(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    if (paramInt2 <= 0) {
      return;
    }
    if (this.writing) {
      DbException.throwInternalError("writing while still writing");
    }
    try
    {
      reserve(paramInt2);
      this.writing = true;
      while (paramInt2 > 0)
      {
        int i = this.data.write(paramArrayOfByte, paramInt1, paramInt2);
        if (i < paramInt2)
        {
          storePage();
          initNextData();
        }
        this.reserved -= i;
        paramInt1 += i;
        paramInt2 -= i;
      }
      this.needFlush = true;
    }
    finally
    {
      this.writing = false;
    }
  }
  
  private void storePage()
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("pageOut.storePage " + this.data);
    }
    this.data.write();
  }
  
  public void flush()
  {
    if (this.needFlush)
    {
      storePage();
      this.needFlush = false;
    }
  }
  
  public void close()
  {
    this.store = null;
  }
  
  int getCurrentDataPageId()
  {
    return this.data.getPos();
  }
  
  void fillPage()
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("pageOut.storePage fill " + this.data.getPos());
    }
    reserve(this.data.getRemaining() + 1);
    this.reserved -= this.data.getRemaining();
    this.data.write();
    initNextData();
  }
  
  long getSize()
  {
    return this.pageCount * this.store.getPageSize();
  }
  
  void free(PageStreamTrunk paramPageStreamTrunk)
  {
    this.pageCount -= paramPageStreamTrunk.free(0);
  }
  
  void freeReserved()
  {
    if (this.reservedPages.size() > 0)
    {
      int[] arrayOfInt1 = new int[this.reservedPages.size()];
      this.reservedPages.toArray(arrayOfInt1);
      this.reservedPages = new IntArray();
      this.reserved = 0;
      for (int k : arrayOfInt1) {
        this.store.free(k, false);
      }
    }
  }
  
  int getMinPageId()
  {
    return this.minPageId;
  }
}
