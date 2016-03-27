package org.h2.index;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.store.PageStore;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.RegularTable;
import org.h2.table.TableFilter;

public class PageDelegateIndex
  extends PageIndex
{
  private final PageDataIndex mainIndex;
  
  public PageDelegateIndex(RegularTable paramRegularTable, int paramInt, String paramString, IndexType paramIndexType, PageDataIndex paramPageDataIndex, boolean paramBoolean, Session paramSession)
  {
    IndexColumn[] arrayOfIndexColumn = IndexColumn.wrap(new Column[] { paramRegularTable.getColumn(paramPageDataIndex.getMainIndexColumn()) });
    
    initBaseIndex(paramRegularTable, paramInt, paramString, arrayOfIndexColumn, paramIndexType);
    this.mainIndex = paramPageDataIndex;
    if ((!this.database.isPersistent()) || (paramInt < 0)) {
      throw DbException.throwInternalError("" + paramString);
    }
    PageStore localPageStore = this.database.getPageStore();
    localPageStore.addIndex(this);
    if (paramBoolean) {
      localPageStore.addMeta(this, paramSession);
    }
  }
  
  public void add(Session paramSession, Row paramRow) {}
  
  public boolean canFindNext()
  {
    return false;
  }
  
  public boolean canGetFirstOrLast()
  {
    return true;
  }
  
  public void close(Session paramSession) {}
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    long l1 = this.mainIndex.getKey(paramSearchRow1, Long.MIN_VALUE, Long.MIN_VALUE);
    
    long l2 = this.mainIndex.getKey(paramSearchRow2, Long.MAX_VALUE, Long.MIN_VALUE);
    return this.mainIndex.find(paramSession, l1, l2, false);
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    Cursor localCursor;
    if (paramBoolean)
    {
      localCursor = this.mainIndex.find(paramSession, Long.MIN_VALUE, Long.MAX_VALUE, false);
    }
    else
    {
      long l = this.mainIndex.getLastKey();
      localCursor = this.mainIndex.find(paramSession, l, l, false);
    }
    localCursor.next();
    return localCursor;
  }
  
  public Cursor findNext(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    throw DbException.throwInternalError();
  }
  
  public int getColumnIndex(Column paramColumn)
  {
    if (paramColumn.getColumnId() == this.mainIndex.getMainIndexColumn()) {
      return 0;
    }
    return -1;
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    return 10L * getCostRangeIndex(paramArrayOfInt, this.mainIndex.getRowCount(paramSession), paramTableFilter, paramSortOrder);
  }
  
  public boolean needRebuild()
  {
    return false;
  }
  
  public void remove(Session paramSession, Row paramRow) {}
  
  public void remove(Session paramSession)
  {
    this.mainIndex.setMainIndexColumn(-1);
    paramSession.getDatabase().getPageStore().removeMeta(this, paramSession);
  }
  
  public void truncate(Session paramSession) {}
  
  public void checkRename() {}
  
  public long getRowCount(Session paramSession)
  {
    return this.mainIndex.getRowCount(paramSession);
  }
  
  public long getRowCountApproximation()
  {
    return this.mainIndex.getRowCountApproximation();
  }
  
  public long getDiskSpaceUsed()
  {
    return this.mainIndex.getDiskSpaceUsed();
  }
  
  public void writeRowCount() {}
}
