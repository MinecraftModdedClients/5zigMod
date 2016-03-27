package org.h2.index;

import java.util.ArrayList;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.MetaTable;
import org.h2.table.TableFilter;

public class MetaIndex
  extends BaseIndex
{
  private final MetaTable meta;
  private final boolean scan;
  
  public MetaIndex(MetaTable paramMetaTable, IndexColumn[] paramArrayOfIndexColumn, boolean paramBoolean)
  {
    initBaseIndex(paramMetaTable, 0, null, paramArrayOfIndexColumn, IndexType.createNonUnique(true));
    this.meta = paramMetaTable;
    this.scan = paramBoolean;
  }
  
  public void close(Session paramSession) {}
  
  public void add(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    ArrayList localArrayList = this.meta.generateRows(paramSession, paramSearchRow1, paramSearchRow2);
    return new MetaCursor(localArrayList);
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    if (this.scan) {
      return 10000.0D;
    }
    return getCostRangeIndex(paramArrayOfInt, 1000L, paramTableFilter, paramSortOrder);
  }
  
  public void truncate(Session paramSession)
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public void remove(Session paramSession)
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public int getColumnIndex(Column paramColumn)
  {
    if (this.scan) {
      return -1;
    }
    return super.getColumnIndex(paramColumn);
  }
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public boolean needRebuild()
  {
    return false;
  }
  
  public String getCreateSQL()
  {
    return null;
  }
  
  public boolean canGetFirstOrLast()
  {
    return false;
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public long getRowCount(Session paramSession)
  {
    return 1000L;
  }
  
  public long getRowCountApproximation()
  {
    return 1000L;
  }
  
  public long getDiskSpaceUsed()
  {
    return this.meta.getDiskSpaceUsed();
  }
  
  public String getPlanSQL()
  {
    return "meta";
  }
}
