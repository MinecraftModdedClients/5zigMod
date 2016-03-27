package org.h2.mvstore.db;

import java.util.List;
import org.h2.engine.Session;
import org.h2.index.BaseIndex;
import org.h2.index.Cursor;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.TableFilter;
import org.h2.value.ValueLong;

public class MVDelegateIndex
  extends BaseIndex
  implements MVIndex
{
  private final MVPrimaryIndex mainIndex;
  
  public MVDelegateIndex(MVTable paramMVTable, int paramInt, String paramString, MVPrimaryIndex paramMVPrimaryIndex, IndexType paramIndexType)
  {
    IndexColumn[] arrayOfIndexColumn = IndexColumn.wrap(new Column[] { paramMVTable.getColumn(paramMVPrimaryIndex.getMainIndexColumn()) });
    
    initBaseIndex(paramMVTable, paramInt, paramString, arrayOfIndexColumn, paramIndexType);
    this.mainIndex = paramMVPrimaryIndex;
    if (paramInt < 0) {
      throw DbException.throwInternalError("" + paramString);
    }
  }
  
  public void addRowsToBuffer(List<Row> paramList, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  public void addBufferedRows(List<String> paramList)
  {
    throw DbException.throwInternalError();
  }
  
  public void add(Session paramSession, Row paramRow) {}
  
  public boolean canGetFirstOrLast()
  {
    return true;
  }
  
  public void close(Session paramSession) {}
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    ValueLong localValueLong1 = this.mainIndex.getKey(paramSearchRow1, MVPrimaryIndex.MIN, MVPrimaryIndex.MIN);
    
    ValueLong localValueLong2 = this.mainIndex.getKey(paramSearchRow2, MVPrimaryIndex.MAX, MVPrimaryIndex.MIN);
    
    return this.mainIndex.find(paramSession, localValueLong1, localValueLong2);
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    return this.mainIndex.findFirstOrLast(paramSession, paramBoolean);
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
    return 10L * getCostRangeIndex(paramArrayOfInt, this.mainIndex.getRowCountApproximation(), paramTableFilter, paramSortOrder);
  }
  
  public boolean needRebuild()
  {
    return false;
  }
  
  public void remove(Session paramSession, Row paramRow) {}
  
  public void remove(Session paramSession)
  {
    this.mainIndex.setMainIndexColumn(-1);
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
    return 0L;
  }
}
