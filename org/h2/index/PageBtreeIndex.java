package org.h2.index;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.store.Data;
import org.h2.store.LobStorageInterface;
import org.h2.store.Page;
import org.h2.store.PageStore;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.RegularTable;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.MathUtils;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class PageBtreeIndex
  extends PageIndex
{
  private static int memoryChangeRequired;
  private final PageStore store;
  private final RegularTable tableData;
  private final boolean needRebuild;
  private long rowCount;
  private int memoryPerPage;
  private int memoryCount;
  
  public PageBtreeIndex(RegularTable paramRegularTable, int paramInt, String paramString, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType, boolean paramBoolean, Session paramSession)
  {
    initBaseIndex(paramRegularTable, paramInt, paramString, paramArrayOfIndexColumn, paramIndexType);
    if ((!this.database.isStarting()) && (paramBoolean)) {
      checkIndexColumnTypes(paramArrayOfIndexColumn);
    }
    this.tableData = paramRegularTable;
    if ((!this.database.isPersistent()) || (paramInt < 0)) {
      throw DbException.throwInternalError("" + paramString);
    }
    this.store = this.database.getPageStore();
    this.store.addIndex(this);
    Object localObject;
    if (paramBoolean)
    {
      this.rootPageId = this.store.allocatePage();
      
      this.store.addMeta(this, paramSession);
      localObject = PageBtreeLeaf.create(this, this.rootPageId, 0);
      this.store.logUndo((Page)localObject, null);
      this.store.update((Page)localObject);
    }
    else
    {
      this.rootPageId = this.store.getRootPageId(paramInt);
      localObject = getPage(this.rootPageId);
      this.rowCount = ((PageBtree)localObject).getRowCount();
    }
    this.needRebuild = ((paramBoolean) || ((this.rowCount == 0L) && (this.store.isRecoveryRunning())));
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("opened {0} rows: {1}", new Object[] { getName(), Long.valueOf(this.rowCount) });
    }
    this.memoryPerPage = (184 + this.store.getPageSize() >> 2);
  }
  
  public void add(Session paramSession, Row paramRow)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("{0} add {1}", new Object[] { getName(), paramRow });
    }
    SearchRow localSearchRow = getSearchRow(paramRow);
    try
    {
      addRow(localSearchRow);
    }
    finally
    {
      this.store.incrementChangeCount();
    }
  }
  
  private void addRow(SearchRow paramSearchRow)
  {
    for (;;)
    {
      Object localObject1 = getPage(this.rootPageId);
      int i = ((PageBtree)localObject1).addRowTry(paramSearchRow);
      if (i == -1) {
        break;
      }
      if (this.trace.isDebugEnabled()) {
        this.trace.debug("split {0}", new Object[] { Integer.valueOf(i) });
      }
      SearchRow localSearchRow = ((PageBtree)localObject1).getRow(i - 1);
      this.store.logUndo((Page)localObject1, ((PageBtree)localObject1).data);
      Object localObject2 = localObject1;
      PageBtree localPageBtree = ((PageBtree)localObject1).split(i);
      this.store.logUndo(localPageBtree, null);
      int j = this.store.allocatePage();
      ((PageBtree)localObject2).setPageId(j);
      ((PageBtree)localObject2).setParentPageId(this.rootPageId);
      localPageBtree.setParentPageId(this.rootPageId);
      PageBtreeNode localPageBtreeNode = PageBtreeNode.create(this, this.rootPageId, 0);
      
      this.store.logUndo(localPageBtreeNode, null);
      localPageBtreeNode.init((PageBtree)localObject2, localSearchRow, localPageBtree);
      this.store.update((Page)localObject2);
      this.store.update(localPageBtree);
      this.store.update(localPageBtreeNode);
      localObject1 = localPageBtreeNode;
    }
    invalidateRowCount();
    this.rowCount += 1L;
  }
  
  private SearchRow getSearchRow(Row paramRow)
  {
    SearchRow localSearchRow = this.table.getTemplateSimpleRow(this.columns.length == 1);
    localSearchRow.setKeyAndVersion(paramRow);
    for (Column localColumn : this.columns)
    {
      int k = localColumn.getColumnId();
      localSearchRow.setValue(k, paramRow.getValue(k));
    }
    return localSearchRow;
  }
  
  PageBtree getPage(int paramInt)
  {
    Page localPage = this.store.getPage(paramInt);
    if (localPage == null)
    {
      PageBtreeLeaf localPageBtreeLeaf = PageBtreeLeaf.create(this, paramInt, 0);
      
      this.store.logUndo(localPageBtreeLeaf, null);
      this.store.update(localPageBtreeLeaf);
      return localPageBtreeLeaf;
    }
    if (!(localPage instanceof PageBtree)) {
      throw DbException.get(90030, "" + localPage);
    }
    return (PageBtree)localPage;
  }
  
  public boolean canGetFirstOrLast()
  {
    return true;
  }
  
  public Cursor findNext(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    return find(paramSession, paramSearchRow1, true, paramSearchRow2);
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    return find(paramSession, paramSearchRow1, false, paramSearchRow2);
  }
  
  private Cursor find(Session paramSession, SearchRow paramSearchRow1, boolean paramBoolean, SearchRow paramSearchRow2)
  {
    if ((SysProperties.CHECK) && (this.store == null)) {
      throw DbException.get(90007);
    }
    PageBtree localPageBtree = getPage(this.rootPageId);
    PageBtreeCursor localPageBtreeCursor = new PageBtreeCursor(paramSession, this, paramSearchRow2);
    localPageBtree.find(localPageBtreeCursor, paramSearchRow1, paramBoolean);
    return localPageBtreeCursor;
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    Object localObject3;
    if (paramBoolean)
    {
      localObject1 = find(paramSession, null, false, null);
      while (((Cursor)localObject1).next())
      {
        localObject2 = ((Cursor)localObject1).getSearchRow();
        localObject3 = ((SearchRow)localObject2).getValue(this.columnIds[0]);
        if (localObject3 != ValueNull.INSTANCE) {
          return (Cursor)localObject1;
        }
      }
      return (Cursor)localObject1;
    }
    Object localObject1 = getPage(this.rootPageId);
    Object localObject2 = new PageBtreeCursor(paramSession, this, null);
    ((PageBtree)localObject1).last((PageBtreeCursor)localObject2);
    ((PageBtreeCursor)localObject2).previous();
    do
    {
      localObject3 = ((PageBtreeCursor)localObject2).getSearchRow();
      if (localObject3 == null) {
        break;
      }
      Value localValue = ((SearchRow)localObject3).getValue(this.columnIds[0]);
      if (localValue != ValueNull.INSTANCE) {
        return (Cursor)localObject2;
      }
    } while (((PageBtreeCursor)localObject2).previous());
    return (Cursor)localObject2;
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    return 10L * getCostRangeIndex(paramArrayOfInt, this.tableData.getRowCount(paramSession), paramTableFilter, paramSortOrder);
  }
  
  public boolean needRebuild()
  {
    return this.needRebuild;
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("{0} remove {1}", new Object[] { getName(), paramRow });
    }
    if (this.rowCount == 1L) {
      removeAllRows();
    } else {
      try
      {
        PageBtree localPageBtree = getPage(this.rootPageId);
        localPageBtree.remove(paramRow);
        invalidateRowCount();
        this.rowCount -= 1L;
      }
      finally
      {
        this.store.incrementChangeCount();
      }
    }
  }
  
  public void remove(Session paramSession)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("remove");
    }
    removeAllRows();
    this.store.free(this.rootPageId);
    this.store.removeMeta(this, paramSession);
  }
  
  public void truncate(Session paramSession)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("truncate");
    }
    removeAllRows();
    if (this.tableData.getContainsLargeObject()) {
      this.database.getLobStorage().removeAllForTable(this.table.getId());
    }
    this.tableData.setRowCount(0L);
  }
  
  private void removeAllRows()
  {
    try
    {
      Object localObject1 = getPage(this.rootPageId);
      ((PageBtree)localObject1).freeRecursive();
      localObject1 = PageBtreeLeaf.create(this, this.rootPageId, 0);
      this.store.removeFromCache(this.rootPageId);
      this.store.update((Page)localObject1);
      this.rowCount = 0L;
    }
    finally
    {
      this.store.incrementChangeCount();
    }
  }
  
  public void checkRename() {}
  
  public Row getRow(Session paramSession, long paramLong)
  {
    return this.tableData.getRow(paramSession, paramLong);
  }
  
  PageStore getPageStore()
  {
    return this.store;
  }
  
  public long getRowCountApproximation()
  {
    return this.tableData.getRowCountApproximation();
  }
  
  public long getDiskSpaceUsed()
  {
    return this.tableData.getDiskSpaceUsed();
  }
  
  public long getRowCount(Session paramSession)
  {
    return this.rowCount;
  }
  
  public void close(Session paramSession)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("close");
    }
    try
    {
      writeRowCount();
    }
    finally
    {
      this.store.incrementChangeCount();
    }
  }
  
  SearchRow readRow(Data paramData, int paramInt, boolean paramBoolean1, boolean paramBoolean2)
  {
    synchronized (paramData)
    {
      paramData.setPos(paramInt);
      long l = paramData.readVarLong();
      if (paramBoolean1)
      {
        if (paramBoolean2) {
          return this.tableData.getRow(null, l);
        }
        localSearchRow = this.table.getTemplateSimpleRow(true);
        localSearchRow.setKey(l);
        return localSearchRow;
      }
      SearchRow localSearchRow = this.table.getTemplateSimpleRow(this.columns.length == 1);
      localSearchRow.setKey(l);
      for (Column localColumn : this.columns)
      {
        int k = localColumn.getColumnId();
        localSearchRow.setValue(k, paramData.readValue());
      }
      return localSearchRow;
    }
  }
  
  SearchRow readRow(long paramLong)
  {
    return this.tableData.getRow(null, paramLong);
  }
  
  void writeRow(Data paramData, int paramInt, SearchRow paramSearchRow, boolean paramBoolean)
  {
    paramData.setPos(paramInt);
    paramData.writeVarLong(paramSearchRow.getKey());
    if (!paramBoolean) {
      for (Column localColumn : this.columns)
      {
        int k = localColumn.getColumnId();
        paramData.writeValue(paramSearchRow.getValue(k));
      }
    }
  }
  
  int getRowSize(Data paramData, SearchRow paramSearchRow, boolean paramBoolean)
  {
    int i = Data.getVarLongLen(paramSearchRow.getKey());
    if (!paramBoolean) {
      for (Column localColumn : this.columns)
      {
        Value localValue = paramSearchRow.getValue(localColumn.getColumnId());
        i += paramData.getValueLen(localValue);
      }
    }
    return i;
  }
  
  public boolean canFindNext()
  {
    return true;
  }
  
  void setRootPageId(Session paramSession, int paramInt)
  {
    this.store.removeMeta(this, paramSession);
    this.rootPageId = paramInt;
    this.store.addMeta(this, paramSession);
    this.store.addIndex(this);
  }
  
  private void invalidateRowCount()
  {
    PageBtree localPageBtree = getPage(this.rootPageId);
    localPageBtree.setRowCountStored(-1);
  }
  
  public void writeRowCount()
  {
    if ((SysProperties.MODIFY_ON_WRITE) && (this.rootPageId == 0)) {
      return;
    }
    PageBtree localPageBtree = getPage(this.rootPageId);
    localPageBtree.setRowCountStored(MathUtils.convertLongToInt(this.rowCount));
  }
  
  boolean hasData(SearchRow paramSearchRow)
  {
    return paramSearchRow.getValue(this.columns[0].getColumnId()) != null;
  }
  
  int getMemoryPerPage()
  {
    return this.memoryPerPage;
  }
  
  void memoryChange(int paramInt)
  {
    if (this.memoryCount < 64) {
      this.memoryPerPage += (paramInt - this.memoryPerPage) / ++this.memoryCount;
    } else {
      this.memoryPerPage += (paramInt > this.memoryPerPage ? 1 : -1) + (paramInt - this.memoryPerPage) / 64;
    }
  }
  
  static boolean isMemoryChangeRequired()
  {
    if (memoryChangeRequired-- <= 0)
    {
      memoryChangeRequired = 10;
      return true;
    }
    return false;
  }
}
