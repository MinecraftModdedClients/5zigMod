package org.h2.index;

import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.RegularTable;
import org.h2.table.TableFilter;
import org.h2.util.ValueHashMap;
import org.h2.value.Value;

public class HashIndex
  extends BaseIndex
{
  private final int indexColumn;
  private final RegularTable tableData;
  private ValueHashMap<Long> rows;
  
  public HashIndex(RegularTable paramRegularTable, int paramInt, String paramString, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType)
  {
    initBaseIndex(paramRegularTable, paramInt, paramString, paramArrayOfIndexColumn, paramIndexType);
    this.indexColumn = paramArrayOfIndexColumn[0].column.getColumnId();
    this.tableData = paramRegularTable;
    reset();
  }
  
  private void reset()
  {
    this.rows = ValueHashMap.newInstance();
  }
  
  public void truncate(Session paramSession)
  {
    reset();
  }
  
  public void add(Session paramSession, Row paramRow)
  {
    Value localValue = paramRow.getValue(this.indexColumn);
    Object localObject = this.rows.get(localValue);
    if (localObject != null) {
      throw getDuplicateKeyException(localValue.toString());
    }
    this.rows.put(localValue, Long.valueOf(paramRow.getKey()));
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    this.rows.remove(paramRow.getValue(this.indexColumn));
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    if ((paramSearchRow1 == null) || (paramSearchRow2 == null)) {
      throw DbException.throwInternalError();
    }
    Value localValue = paramSearchRow1.getValue(this.indexColumn);
    
    localValue = localValue.convertTo(this.tableData.getColumn(this.indexColumn).getType());
    
    Long localLong = (Long)this.rows.get(localValue);
    Row localRow;
    if (localLong == null) {
      localRow = null;
    } else {
      localRow = this.tableData.getRow(paramSession, localLong.intValue());
    }
    return new SingleRowCursor(localRow);
  }
  
  public long getRowCount(Session paramSession)
  {
    return getRowCountApproximation();
  }
  
  public long getRowCountApproximation()
  {
    return this.rows.size();
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
  
  public void close(Session paramSession) {}
  
  public void remove(Session paramSession) {}
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    for (Column localColumn : this.columns)
    {
      int k = localColumn.getColumnId();
      int m = paramArrayOfInt[k];
      if ((m & 0x1) != 1) {
        return 9.223372036854776E18D;
      }
    }
    return 2.0D;
  }
  
  public void checkRename() {}
  
  public boolean needRebuild()
  {
    return true;
  }
  
  public boolean canGetFirstOrLast()
  {
    return false;
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    throw DbException.getUnsupportedException("HASH");
  }
  
  public boolean canScan()
  {
    return false;
  }
}
