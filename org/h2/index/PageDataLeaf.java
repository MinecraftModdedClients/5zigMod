package org.h2.index;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.store.Data;
import org.h2.store.Page;
import org.h2.store.PageStore;
import org.h2.table.RegularTable;
import org.h2.table.Table;
import org.h2.value.Value;

public class PageDataLeaf
  extends PageData
{
  private final boolean optimizeUpdate;
  private int[] offsets;
  private Row[] rows;
  private SoftReference<Row> rowRef;
  private int firstOverflowPageId;
  private int start;
  private int overflowRowSize;
  private int columnCount;
  private int memoryData;
  private boolean writtenData;
  
  private PageDataLeaf(PageDataIndex paramPageDataIndex, int paramInt, Data paramData)
  {
    super(paramPageDataIndex, paramInt, paramData);
    this.optimizeUpdate = paramPageDataIndex.getDatabase().getSettings().optimizeUpdate;
  }
  
  static PageDataLeaf create(PageDataIndex paramPageDataIndex, int paramInt1, int paramInt2)
  {
    PageDataLeaf localPageDataLeaf = new PageDataLeaf(paramPageDataIndex, paramInt1, paramPageDataIndex.getPageStore().createData());
    
    paramPageDataIndex.getPageStore().logUndo(localPageDataLeaf, null);
    localPageDataLeaf.rows = Row.EMPTY_ARRAY;
    localPageDataLeaf.parentPageId = paramInt2;
    localPageDataLeaf.columnCount = paramPageDataIndex.getTable().getColumns().length;
    localPageDataLeaf.writeHead();
    localPageDataLeaf.start = localPageDataLeaf.data.length();
    return localPageDataLeaf;
  }
  
  public static Page read(PageDataIndex paramPageDataIndex, Data paramData, int paramInt)
  {
    PageDataLeaf localPageDataLeaf = new PageDataLeaf(paramPageDataIndex, paramInt, paramData);
    localPageDataLeaf.read();
    return localPageDataLeaf;
  }
  
  private void read()
  {
    this.data.reset();
    int i = this.data.readByte();
    this.data.readShortInt();
    this.parentPageId = this.data.readInt();
    int j = this.data.readVarInt();
    if (j != this.index.getId()) {
      throw DbException.get(90030, "page:" + getPos() + " expected table:" + this.index.getId() + " got:" + j + " type:" + i);
    }
    this.columnCount = this.data.readVarInt();
    this.entryCount = this.data.readShortInt();
    this.offsets = new int[this.entryCount];
    this.keys = new long[this.entryCount];
    this.rows = new Row[this.entryCount];
    if (i == 1)
    {
      if (this.entryCount != 1) {
        DbException.throwInternalError("entries: " + this.entryCount);
      }
      this.firstOverflowPageId = this.data.readInt();
    }
    for (int k = 0; k < this.entryCount; k++)
    {
      this.keys[k] = this.data.readVarLong();
      this.offsets[k] = this.data.readShortInt();
    }
    this.start = this.data.length();
    this.written = true;
    this.writtenData = true;
  }
  
  private int getRowLength(Row paramRow)
  {
    int i = 0;
    for (int j = 0; j < this.columnCount; j++) {
      i += this.data.getValueLen(paramRow.getValue(j));
    }
    return i;
  }
  
  private int findInsertionPoint(long paramLong)
  {
    int i = find(paramLong);
    if ((i < this.entryCount) && (this.keys[i] == paramLong)) {
      throw this.index.getDuplicateKeyException("" + paramLong);
    }
    return i;
  }
  
  int addRowTry(Row paramRow)
  {
    this.index.getPageStore().logUndo(this, this.data);
    int i = getRowLength(paramRow);
    int j = this.index.getPageStore().getPageSize();
    int k = this.entryCount == 0 ? j : this.offsets[(this.entryCount - 1)];
    int m = 2 + Data.getVarLongLen(paramRow.getKey());
    int n;
    if ((this.entryCount > 0) && (k - i < this.start + m))
    {
      n = findInsertionPoint(paramRow.getKey());
      if (this.entryCount > 1)
      {
        if (this.entryCount < 5) {
          return this.entryCount / 2;
        }
        if (this.index.isSortedInsertMode()) {
          return n > this.entryCount - 1 ? this.entryCount - 1 : n < 2 ? 1 : n;
        }
        i1 = this.entryCount / 3;
        return n >= 2 * i1 ? 2 * i1 : n < i1 ? i1 : n;
      }
      return n;
    }
    this.index.getPageStore().logUndo(this, this.data);
    if (this.entryCount == 0)
    {
      n = 0;
    }
    else
    {
      if (!this.optimizeUpdate) {
        readAllRows();
      }
      n = findInsertionPoint(paramRow.getKey());
    }
    this.written = false;
    this.changeCount = this.index.getPageStore().getChangeCount();
    k = n == 0 ? j : this.offsets[(n - 1)];
    int i1 = k - i;
    this.start += m;
    this.offsets = insert(this.offsets, this.entryCount, n, i1);
    add(this.offsets, n + 1, this.entryCount + 1, -i);
    this.keys = insert(this.keys, this.entryCount, n, paramRow.getKey());
    this.rows = ((Row[])insert(this.rows, this.entryCount, n, paramRow));
    this.entryCount += 1;
    this.index.getPageStore().update(this);
    int i3;
    int i4;
    int i5;
    if ((this.optimizeUpdate) && 
      (this.writtenData) && (i1 >= this.start))
    {
      byte[] arrayOfByte = this.data.getBytes();
      i3 = this.offsets[(this.entryCount - 1)] + i;
      i4 = this.offsets[n];
      System.arraycopy(arrayOfByte, i3, arrayOfByte, i3 - i, i4 - i3 + i);
      
      this.data.setPos(i4);
      for (i5 = 0; i5 < this.columnCount; i5++) {
        this.data.writeValue(paramRow.getValue(i5));
      }
    }
    if (i1 < this.start)
    {
      this.writtenData = false;
      if (this.entryCount > 1) {
        DbException.throwInternalError();
      }
      this.start += 4;
      int i2 = i - (j - this.start);
      
      i1 = this.start;
      this.offsets[n] = i1;
      i3 = getPos();
      i4 = j;
      i5 = this.index.getPageStore().allocatePage();
      this.firstOverflowPageId = i5;
      this.overflowRowSize = (j + i);
      writeData();
      
      Row localRow = this.rows[0];
      this.rowRef = new SoftReference(localRow);
      this.rows[0] = null;
      Data localData = this.index.getPageStore().createData();
      localData.checkCapacity(this.data.length());
      localData.write(this.data.getBytes(), 0, this.data.length());
      this.data.truncate(this.index.getPageStore().getPageSize());
      do
      {
        int i6;
        int i7;
        int i8;
        if (i2 <= j - 9)
        {
          i6 = 19;
          i7 = i2;
          i8 = 0;
        }
        else
        {
          i6 = 3;
          i7 = j - 11;
          i8 = this.index.getPageStore().allocatePage();
        }
        PageDataOverflow localPageDataOverflow = PageDataOverflow.create(this.index.getPageStore(), i5, i6, i3, i8, localData, i4, i7);
        
        this.index.getPageStore().update(localPageDataOverflow);
        i4 += i7;
        i2 -= i7;
        i3 = i5;
        i5 = i8;
      } while (i2 > 0);
    }
    if (this.rowRef == null) {
      memoryChange(true, paramRow);
    } else {
      memoryChange(true, null);
    }
    return -1;
  }
  
  private void removeRow(int paramInt)
  {
    this.index.getPageStore().logUndo(this, this.data);
    this.written = false;
    this.changeCount = this.index.getPageStore().getChangeCount();
    if (!this.optimizeUpdate) {
      readAllRows();
    }
    Row localRow = getRowAt(paramInt);
    if (localRow != null) {
      memoryChange(false, localRow);
    }
    this.entryCount -= 1;
    if (this.entryCount < 0) {
      DbException.throwInternalError();
    }
    if (this.firstOverflowPageId != 0)
    {
      this.start -= 4;
      freeOverflow();
      this.firstOverflowPageId = 0;
      this.overflowRowSize = 0;
      this.rowRef = null;
    }
    int i = 2 + Data.getVarLongLen(this.keys[paramInt]);
    int j = paramInt > 0 ? this.offsets[(paramInt - 1)] : this.index.getPageStore().getPageSize();
    int k = j - this.offsets[paramInt];
    if (this.optimizeUpdate)
    {
      if (this.writtenData)
      {
        byte[] arrayOfByte = this.data.getBytes();
        int n = this.offsets[this.entryCount];
        System.arraycopy(arrayOfByte, n, arrayOfByte, n + k, this.offsets[paramInt] - n);
        
        Arrays.fill(arrayOfByte, n, n + k, (byte)0);
      }
    }
    else
    {
      int m = this.offsets[this.entryCount];
      Arrays.fill(this.data.getBytes(), m, m + k, (byte)0);
    }
    this.start -= i;
    this.offsets = remove(this.offsets, this.entryCount + 1, paramInt);
    add(this.offsets, paramInt, this.entryCount, k);
    this.keys = remove(this.keys, this.entryCount + 1, paramInt);
    this.rows = ((Row[])remove(this.rows, this.entryCount + 1, paramInt));
  }
  
  Cursor find(Session paramSession, long paramLong1, long paramLong2, boolean paramBoolean)
  {
    int i = find(paramLong1);
    return new PageDataCursor(paramSession, this, i, paramLong2, paramBoolean);
  }
  
  Row getRowAt(int paramInt)
  {
    Row localRow = this.rows[paramInt];
    if (localRow == null)
    {
      if (this.firstOverflowPageId == 0)
      {
        localRow = readRow(this.data, this.offsets[paramInt], this.columnCount);
      }
      else
      {
        if (this.rowRef != null)
        {
          localRow = (Row)this.rowRef.get();
          if (localRow != null) {
            return localRow;
          }
        }
        PageStore localPageStore = this.index.getPageStore();
        Data localData = localPageStore.createData();
        int i = localPageStore.getPageSize();
        int j = this.offsets[paramInt];
        localData.write(this.data.getBytes(), j, i - j);
        int k = this.firstOverflowPageId;
        do
        {
          PageDataOverflow localPageDataOverflow = this.index.getPageOverflow(k);
          k = localPageDataOverflow.readInto(localData);
        } while (k != 0);
        this.overflowRowSize = (i + localData.length());
        localRow = readRow(localData, 0, this.columnCount);
      }
      localRow.setKey(this.keys[paramInt]);
      if (this.firstOverflowPageId != 0)
      {
        this.rowRef = new SoftReference(localRow);
      }
      else
      {
        this.rows[paramInt] = localRow;
        memoryChange(true, localRow);
      }
    }
    return localRow;
  }
  
  int getEntryCount()
  {
    return this.entryCount;
  }
  
  PageData split(int paramInt)
  {
    int i = this.index.getPageStore().allocatePage();
    PageDataLeaf localPageDataLeaf = create(this.index, i, this.parentPageId);
    for (int j = paramInt; j < this.entryCount;)
    {
      int k = localPageDataLeaf.addRowTry(getRowAt(paramInt));
      if (k != -1) {
        DbException.throwInternalError("split " + k);
      }
      removeRow(paramInt);
    }
    return localPageDataLeaf;
  }
  
  long getLastKey()
  {
    if (this.entryCount == 0) {
      return 0L;
    }
    return getRowAt(this.entryCount - 1).getKey();
  }
  
  PageDataLeaf getNextPage()
  {
    if (this.parentPageId == 0) {
      return null;
    }
    PageDataNode localPageDataNode = (PageDataNode)this.index.getPage(this.parentPageId, -1);
    return localPageDataNode.getNextPage(this.keys[(this.entryCount - 1)]);
  }
  
  PageDataLeaf getFirstLeaf()
  {
    return this;
  }
  
  protected void remapChildren(int paramInt)
  {
    if (this.firstOverflowPageId == 0) {
      return;
    }
    PageDataOverflow localPageDataOverflow = this.index.getPageOverflow(this.firstOverflowPageId);
    localPageDataOverflow.setParentPageId(getPos());
    this.index.getPageStore().update(localPageDataOverflow);
  }
  
  boolean remove(long paramLong)
  {
    int i = find(paramLong);
    if ((this.keys == null) || (this.keys[i] != paramLong)) {
      throw DbException.get(90112, this.index.getSQL() + ": " + paramLong + " " + (this.keys == null ? -1L : this.keys[i]));
    }
    this.index.getPageStore().logUndo(this, this.data);
    if (this.entryCount == 1)
    {
      freeRecursive();
      return true;
    }
    removeRow(i);
    this.index.getPageStore().update(this);
    return false;
  }
  
  void freeRecursive()
  {
    this.index.getPageStore().logUndo(this, this.data);
    this.index.getPageStore().free(getPos());
    freeOverflow();
  }
  
  private void freeOverflow()
  {
    if (this.firstOverflowPageId != 0)
    {
      int i = this.firstOverflowPageId;
      do
      {
        PageDataOverflow localPageDataOverflow = this.index.getPageOverflow(i);
        localPageDataOverflow.free();
        i = localPageDataOverflow.getNextOverflow();
      } while (i != 0);
    }
  }
  
  Row getRowWithKey(long paramLong)
  {
    int i = find(paramLong);
    return getRowAt(i);
  }
  
  int getRowCount()
  {
    return this.entryCount;
  }
  
  void setRowCountStored(int paramInt) {}
  
  long getDiskSpaceUsed()
  {
    return this.index.getPageStore().getPageSize();
  }
  
  public void write()
  {
    writeData();
    this.index.getPageStore().writePage(getPos(), this.data);
    this.data.truncate(this.index.getPageStore().getPageSize());
  }
  
  private void readAllRows()
  {
    for (int i = 0; i < this.entryCount; i++) {
      getRowAt(i);
    }
  }
  
  private void writeHead()
  {
    this.data.reset();
    int i;
    if (this.firstOverflowPageId == 0) {
      i = 17;
    } else {
      i = 1;
    }
    this.data.writeByte((byte)i);
    this.data.writeShortInt(0);
    if ((SysProperties.CHECK2) && 
      (this.data.length() != 3)) {
      DbException.throwInternalError();
    }
    this.data.writeInt(this.parentPageId);
    this.data.writeVarInt(this.index.getId());
    this.data.writeVarInt(this.columnCount);
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
    if (this.firstOverflowPageId != 0)
    {
      this.data.writeInt(this.firstOverflowPageId);
      this.data.checkCapacity(this.overflowRowSize);
    }
    for (int i = 0; i < this.entryCount; i++)
    {
      this.data.writeVarLong(this.keys[i]);
      this.data.writeShortInt(this.offsets[i]);
    }
    if ((!this.writtenData) || (!this.optimizeUpdate))
    {
      for (i = 0; i < this.entryCount; i++)
      {
        this.data.setPos(this.offsets[i]);
        Row localRow = getRowAt(i);
        for (int j = 0; j < this.columnCount; j++) {
          this.data.writeValue(localRow.getValue(j));
        }
      }
      this.writtenData = true;
    }
    this.written = true;
  }
  
  public String toString()
  {
    return "page[" + getPos() + "] data leaf table:" + this.index.getId() + " " + this.index.getTable().getName() + " entries:" + this.entryCount + " parent:" + this.parentPageId + (this.firstOverflowPageId == 0 ? "" : new StringBuilder().append(" overflow:").append(this.firstOverflowPageId).toString()) + " keys:" + Arrays.toString(this.keys) + " offsets:" + Arrays.toString(this.offsets);
  }
  
  public void moveTo(Session paramSession, int paramInt)
  {
    PageStore localPageStore = this.index.getPageStore();
    if (this.parentPageId != 0) {
      localPageStore.getPage(this.parentPageId);
    }
    localPageStore.logUndo(this, this.data);
    PageDataLeaf localPageDataLeaf = create(this.index, paramInt, this.parentPageId);
    readAllRows();
    localPageDataLeaf.keys = this.keys;
    localPageDataLeaf.overflowRowSize = this.overflowRowSize;
    localPageDataLeaf.firstOverflowPageId = this.firstOverflowPageId;
    localPageDataLeaf.rowRef = this.rowRef;
    localPageDataLeaf.rows = this.rows;
    if (this.firstOverflowPageId != 0) {
      localPageDataLeaf.rows[0] = getRowAt(0);
    }
    localPageDataLeaf.entryCount = this.entryCount;
    localPageDataLeaf.offsets = this.offsets;
    localPageDataLeaf.start = this.start;
    localPageDataLeaf.remapChildren(getPos());
    localPageDataLeaf.writeData();
    localPageDataLeaf.data.truncate(this.index.getPageStore().getPageSize());
    localPageStore.update(localPageDataLeaf);
    if (this.parentPageId == 0)
    {
      this.index.setRootPageId(paramSession, paramInt);
    }
    else
    {
      PageDataNode localPageDataNode = (PageDataNode)localPageStore.getPage(this.parentPageId);
      localPageDataNode.moveChild(getPos(), paramInt);
    }
    localPageStore.free(getPos());
  }
  
  void setOverflow(int paramInt1, int paramInt2)
  {
    if ((SysProperties.CHECK) && (paramInt1 != this.firstOverflowPageId)) {
      DbException.throwInternalError("move " + this + " " + this.firstOverflowPageId);
    }
    this.index.getPageStore().logUndo(this, this.data);
    this.firstOverflowPageId = paramInt2;
    if (this.written)
    {
      this.changeCount = this.index.getPageStore().getChangeCount();
      writeHead();
      this.data.writeInt(this.firstOverflowPageId);
    }
    this.index.getPageStore().update(this);
  }
  
  private void memoryChange(boolean paramBoolean, Row paramRow)
  {
    int i = paramRow == null ? 0 : 20 + paramRow.getMemory();
    this.memoryData += (paramBoolean ? i : -i);
    this.index.memoryChange(240 + this.memoryData + this.index.getPageStore().getPageSize() >> 2);
  }
  
  public boolean isStream()
  {
    return this.firstOverflowPageId > 0;
  }
  
  private static Row readRow(Data paramData, int paramInt1, int paramInt2)
  {
    Value[] arrayOfValue = new Value[paramInt2];
    synchronized (paramData)
    {
      paramData.setPos(paramInt1);
      for (int i = 0; i < paramInt2; i++) {
        arrayOfValue[i] = paramData.readValue();
      }
    }
    return RegularTable.createRow(arrayOfValue);
  }
}
