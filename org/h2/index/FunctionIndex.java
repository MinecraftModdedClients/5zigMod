package org.h2.index;

import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.FunctionTable;
import org.h2.table.IndexColumn;
import org.h2.table.TableFilter;

public class FunctionIndex
  extends BaseIndex
{
  private final FunctionTable functionTable;
  
  public FunctionIndex(FunctionTable paramFunctionTable, IndexColumn[] paramArrayOfIndexColumn)
  {
    initBaseIndex(paramFunctionTable, 0, null, paramArrayOfIndexColumn, IndexType.createNonUnique(true));
    this.functionTable = paramFunctionTable;
  }
  
  public void close(Session paramSession) {}
  
  public void add(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("ALIAS");
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("ALIAS");
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    if (this.functionTable.isBufferResultSetToLocalTemp()) {
      return new FunctionCursor(this.functionTable.getResult(paramSession));
    }
    return new FunctionCursorResultSet(paramSession, this.functionTable.getResultSet(paramSession));
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    if (paramArrayOfInt != null) {
      throw DbException.getUnsupportedException("ALIAS");
    }
    long l;
    if (this.functionTable.canGetRowCount()) {
      l = this.functionTable.getRowCountApproximation();
    } else {
      l = this.database.getSettings().estimatedFunctionTableRows;
    }
    return l * 10L;
  }
  
  public void remove(Session paramSession)
  {
    throw DbException.getUnsupportedException("ALIAS");
  }
  
  public void truncate(Session paramSession)
  {
    throw DbException.getUnsupportedException("ALIAS");
  }
  
  public boolean needRebuild()
  {
    return false;
  }
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("ALIAS");
  }
  
  public boolean canGetFirstOrLast()
  {
    return false;
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    throw DbException.getUnsupportedException("ALIAS");
  }
  
  public long getRowCount(Session paramSession)
  {
    return this.functionTable.getRowCount(paramSession);
  }
  
  public long getRowCountApproximation()
  {
    return this.functionTable.getRowCountApproximation();
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
  
  public String getPlanSQL()
  {
    return "function";
  }
  
  public boolean canScan()
  {
    return false;
  }
}
