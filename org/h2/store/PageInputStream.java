package org.h2.store;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.util.BitField;

public class PageInputStream
  extends InputStream
{
  private final PageStore store;
  private final Trace trace;
  private final int firstTrunkPage;
  private final PageStreamTrunk.Iterator trunkIterator;
  private int dataPage;
  private PageStreamTrunk trunk;
  private int trunkIndex;
  private PageStreamData data;
  private int dataPos;
  private boolean endOfFile;
  private int remaining;
  private final byte[] buffer = { 0 };
  private int logKey;
  
  PageInputStream(PageStore paramPageStore, int paramInt1, int paramInt2, int paramInt3)
  {
    this.store = paramPageStore;
    this.trace = paramPageStore.getTrace();
    
    this.logKey = (paramInt1 - 1);
    this.firstTrunkPage = paramInt2;
    this.trunkIterator = new PageStreamTrunk.Iterator(paramPageStore, paramInt2);
    this.dataPage = paramInt3;
  }
  
  public int read()
    throws IOException
  {
    int i = read(this.buffer);
    return i < 0 ? -1 : this.buffer[0] & 0xFF;
  }
  
  public int read(byte[] paramArrayOfByte)
    throws IOException
  {
    return read(paramArrayOfByte, 0, paramArrayOfByte.length);
  }
  
  public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    if (paramInt2 == 0) {
      return 0;
    }
    int i = 0;
    while (paramInt2 > 0)
    {
      int j = readBlock(paramArrayOfByte, paramInt1, paramInt2);
      if (j < 0) {
        break;
      }
      i += j;
      paramInt1 += j;
      paramInt2 -= j;
    }
    return i == 0 ? -1 : i;
  }
  
  private int readBlock(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    try
    {
      fillBuffer();
      if (this.endOfFile) {
        return -1;
      }
      int i = Math.min(this.remaining, paramInt2);
      this.data.read(this.dataPos, paramArrayOfByte, paramInt1, i);
      this.remaining -= i;
      this.dataPos += i;
      return i;
    }
    catch (DbException localDbException)
    {
      throw new EOFException();
    }
  }
  
  private void fillBuffer()
  {
    if ((this.remaining > 0) || (this.endOfFile)) {
      return;
    }
    int i;
    for (;;)
    {
      if (this.trunk == null)
      {
        this.trunk = this.trunkIterator.next();
        this.trunkIndex = 0;
        this.logKey += 1;
        if ((this.trunk == null) || (this.trunk.getLogKey() != this.logKey))
        {
          this.endOfFile = true;
          return;
        }
      }
      if (this.trunk != null)
      {
        i = this.trunk.getPageData(this.trunkIndex++);
        if (i == -1) {
          this.trunk = null;
        } else if (this.dataPage != -1) {
          if (this.dataPage == i) {
            break;
          }
        }
      }
    }
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("pageIn.readPage " + i);
    }
    this.dataPage = -1;
    this.data = null;
    Page localPage = this.store.getPage(i);
    if ((localPage instanceof PageStreamData)) {
      this.data = ((PageStreamData)localPage);
    }
    if ((this.data == null) || (this.data.getLogKey() != this.logKey))
    {
      this.endOfFile = true;
      return;
    }
    this.dataPos = PageStreamData.getReadStart();
    this.remaining = (this.store.getPageSize() - this.dataPos);
  }
  
  BitField allocateAllPages()
  {
    BitField localBitField = new BitField();
    int i = this.logKey;
    PageStreamTrunk.Iterator localIterator = new PageStreamTrunk.Iterator(this.store, this.firstTrunkPage);
    for (;;)
    {
      PageStreamTrunk localPageStreamTrunk = localIterator.next();
      i++;
      if (localIterator.canDelete()) {
        this.store.allocatePage(localIterator.getCurrentPageId());
      }
      if ((localPageStreamTrunk == null) || (localPageStreamTrunk.getLogKey() != i)) {
        break;
      }
      localBitField.set(localPageStreamTrunk.getPos());
      for (int j = 0;; j++)
      {
        int k = localPageStreamTrunk.getPageData(j);
        if (k == -1) {
          break;
        }
        localBitField.set(k);
        this.store.allocatePage(k);
      }
    }
    return localBitField;
  }
  
  int getDataPage()
  {
    return this.data.getPos();
  }
  
  public void close() {}
}
