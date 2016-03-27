package org.h2.index;

import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.IndexColumn;
import org.h2.table.RangeTable;
import org.h2.table.TableFilter;
import org.h2.value.Value;

public class RangeIndex
  extends BaseIndex
{
  private final RangeTable rangeTable;
  
  public RangeIndex(RangeTable paramRangeTable, IndexColumn[] paramArrayOfIndexColumn)
  {
    initBaseIndex(paramRangeTable, 0, "RANGE_INDEX", paramArrayOfIndexColumn, IndexType.createNonUnique(true));
    
    this.rangeTable = paramRangeTable;
  }
  
  public void close(Session paramSession) {}
  
  public void add(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("SYSTEM_RANGE");
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("SYSTEM_RANGE");
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    long l1 = this.rangeTable.getMin(paramSession);long l2 = l1;
    long l3 = this.rangeTable.getMax(paramSession);long l4 = l3;
    try
    {
      l2 = Math.max(l1, paramSearchRow1 == null ? l1 : paramSearchRow1.getValue(0).getLong());
    }
    catch (Exception localException1) {}
    try
    {
      l4 = Math.min(l3, paramSearchRow2 == null ? l3 : paramSearchRow2.getValue(0).getLong());
    }
    catch (Exception localException2) {}
    return new RangeCursor(l2, l4);
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    return 1.0D;
  }
  
  public String getCreateSQL()
  {
    return null;
  }
  
  public void remove(Session paramSession)
  {
    throw DbException.getUnsupportedException("SYSTEM_RANGE");
  }
  
  public void truncate(Session paramSession)
  {
    throw DbException.getUnsupportedException("SYSTEM_RANGE");
  }
  
  public boolean needRebuild()
  {
    return false;
  }
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("SYSTEM_RANGE");
  }
  
  public boolean canGetFirstOrLast()
  {
    return true;
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    long l = paramBoolean ? this.rangeTable.getMin(paramSession) : this.rangeTable.getMax(paramSession);
    return new RangeCursor(l, l);
  }
  
  public long getRowCount(Session paramSession)
  {
    return this.rangeTable.getRowCountApproximation();
  }
  
  public long getRowCountApproximation()
  {
    return this.rangeTable.getRowCountApproximation();
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
}
