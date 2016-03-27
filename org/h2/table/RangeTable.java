package org.h2.table;

import java.util.ArrayList;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.index.RangeIndex;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.value.Value;

public class RangeTable
  extends Table
{
  public static final String NAME = "SYSTEM_RANGE";
  private Expression min;
  private Expression max;
  private boolean optimized;
  
  public RangeTable(Schema paramSchema, Expression paramExpression1, Expression paramExpression2, boolean paramBoolean)
  {
    super(paramSchema, 0, "SYSTEM_RANGE", true, true);
    Column[] arrayOfColumn = { paramBoolean ? new Column[0] : new Column("X", 5) };
    
    this.min = paramExpression1;
    this.max = paramExpression2;
    setColumns(arrayOfColumn);
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  public String getCreateSQL()
  {
    return null;
  }
  
  public String getSQL()
  {
    return "SYSTEM_RANGE(" + this.min.getSQL() + ", " + this.max.getSQL() + ")";
  }
  
  public boolean lock(Session paramSession, boolean paramBoolean1, boolean paramBoolean2)
  {
    return false;
  }
  
  public void close(Session paramSession) {}
  
  public void unlock(Session paramSession) {}
  
  public boolean isLockedExclusively()
  {
    return false;
  }
  
  public Index addIndex(Session paramSession, String paramString1, int paramInt, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType, boolean paramBoolean, String paramString2)
  {
    throw DbException.getUnsupportedException("SYSTEM_RANGE");
  }
  
  public void removeRow(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("SYSTEM_RANGE");
  }
  
  public void addRow(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("SYSTEM_RANGE");
  }
  
  public void checkSupportAlter()
  {
    throw DbException.getUnsupportedException("SYSTEM_RANGE");
  }
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("SYSTEM_RANGE");
  }
  
  public boolean canGetRowCount()
  {
    return true;
  }
  
  public boolean canDrop()
  {
    return false;
  }
  
  public long getRowCount(Session paramSession)
  {
    return Math.max(0L, getMax(paramSession) - getMin(paramSession) + 1L);
  }
  
  public String getTableType()
  {
    throw DbException.throwInternalError();
  }
  
  public Index getScanIndex(Session paramSession)
  {
    return new RangeIndex(this, IndexColumn.wrap(this.columns));
  }
  
  public long getMin(Session paramSession)
  {
    optimize(paramSession);
    return this.min.getValue(paramSession).getLong();
  }
  
  public long getMax(Session paramSession)
  {
    optimize(paramSession);
    return this.max.getValue(paramSession).getLong();
  }
  
  private void optimize(Session paramSession)
  {
    if (!this.optimized)
    {
      this.min = this.min.optimize(paramSession);
      this.max = this.max.optimize(paramSession);
      this.optimized = true;
    }
  }
  
  public ArrayList<Index> getIndexes()
  {
    return null;
  }
  
  public void truncate(Session paramSession)
  {
    throw DbException.getUnsupportedException("SYSTEM_RANGE");
  }
  
  public long getMaxDataModificationId()
  {
    return 0L;
  }
  
  public Index getUniqueIndex()
  {
    return null;
  }
  
  public long getRowCountApproximation()
  {
    return 100L;
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
  
  public boolean isDeterministic()
  {
    return true;
  }
  
  public boolean canReference()
  {
    return false;
  }
}
