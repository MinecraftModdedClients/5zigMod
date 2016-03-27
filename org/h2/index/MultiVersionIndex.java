package org.h2.index;

import java.util.ArrayList;
import org.h2.engine.Database;
import org.h2.engine.DbObject;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.RegularTable;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class MultiVersionIndex
  implements Index
{
  private final Index base;
  private final TreeIndex delta;
  private final RegularTable table;
  private final Object sync;
  private final Column firstColumn;
  
  public MultiVersionIndex(Index paramIndex, RegularTable paramRegularTable)
  {
    this.base = paramIndex;
    this.table = paramRegularTable;
    IndexType localIndexType = IndexType.createNonUnique(false);
    if ((paramIndex instanceof SpatialIndex)) {
      throw DbException.get(50100, "MVCC & spatial index");
    }
    this.delta = new TreeIndex(paramRegularTable, -1, "DELTA", paramIndex.getIndexColumns(), localIndexType);
    
    this.delta.setMultiVersion(true);
    this.sync = paramIndex.getDatabase();
    this.firstColumn = paramIndex.getColumns()[0];
  }
  
  public void add(Session paramSession, Row paramRow)
  {
    synchronized (this.sync)
    {
      this.base.add(paramSession, paramRow);
      if (!removeIfExists(paramSession, paramRow)) {
        if (paramRow.getSessionId() != 0) {
          this.delta.add(paramSession, paramRow);
        }
      }
    }
  }
  
  public void close(Session paramSession)
  {
    synchronized (this.sync)
    {
      this.base.close(paramSession);
    }
  }
  
  public Cursor find(TableFilter paramTableFilter, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    synchronized (this.sync)
    {
      Cursor localCursor1 = this.base.find(paramTableFilter, paramSearchRow1, paramSearchRow2);
      Cursor localCursor2 = this.delta.find(paramTableFilter, paramSearchRow1, paramSearchRow2);
      return new MultiVersionCursor(paramTableFilter.getSession(), this, localCursor1, localCursor2, this.sync);
    }
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    synchronized (this.sync)
    {
      Cursor localCursor1 = this.base.find(paramSession, paramSearchRow1, paramSearchRow2);
      Cursor localCursor2 = this.delta.find(paramSession, paramSearchRow1, paramSearchRow2);
      return new MultiVersionCursor(paramSession, this, localCursor1, localCursor2, this.sync);
    }
  }
  
  public Cursor findNext(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    throw DbException.throwInternalError();
  }
  
  public boolean canFindNext()
  {
    return false;
  }
  
  public boolean canGetFirstOrLast()
  {
    return (this.base.canGetFirstOrLast()) && (this.delta.canGetFirstOrLast());
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    if (paramBoolean)
    {
      localCursor = find(paramSession, null, null);
      while (localCursor.next())
      {
        localObject1 = localCursor.getSearchRow();
        localObject2 = ((SearchRow)localObject1).getValue(this.firstColumn.getColumnId());
        if (localObject2 != ValueNull.INSTANCE) {
          return localCursor;
        }
      }
      return localCursor;
    }
    Cursor localCursor = this.base.findFirstOrLast(paramSession, false);
    Object localObject1 = this.delta.findFirstOrLast(paramSession, false);
    Object localObject2 = new MultiVersionCursor(paramSession, this, localCursor, (Cursor)localObject1, this.sync);
    
    ((MultiVersionCursor)localObject2).loadCurrent();
    while (((MultiVersionCursor)localObject2).previous())
    {
      SearchRow localSearchRow = ((MultiVersionCursor)localObject2).getSearchRow();
      if (localSearchRow == null) {
        break;
      }
      Value localValue = localSearchRow.getValue(this.firstColumn.getColumnId());
      if (localValue != ValueNull.INSTANCE) {
        return (Cursor)localObject2;
      }
    }
    return (Cursor)localObject2;
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    return this.base.getCost(paramSession, paramArrayOfInt, paramTableFilter, paramSortOrder);
  }
  
  public boolean needRebuild()
  {
    return this.base.needRebuild();
  }
  
  public boolean isUncommittedFromOtherSession(Session paramSession, Row paramRow)
  {
    Cursor localCursor = this.delta.find(paramSession, paramRow, paramRow);
    if (localCursor.next())
    {
      Row localRow = localCursor.get();
      return localRow.getSessionId() != paramSession.getId();
    }
    return false;
  }
  
  private boolean removeIfExists(Session paramSession, Row paramRow)
  {
    Cursor localCursor = this.delta.find(paramSession, paramRow, paramRow);
    while (localCursor.next())
    {
      Row localRow = localCursor.get();
      if ((localRow.getKey() == paramRow.getKey()) && (localRow.getVersion() == paramRow.getVersion())) {
        if ((localRow != paramRow) && (this.table.getScanIndex(paramSession).compareRows(localRow, paramRow) != 0))
        {
          paramRow.setVersion(localRow.getVersion() + 1);
        }
        else
        {
          this.delta.remove(paramSession, localRow);
          return true;
        }
      }
    }
    return false;
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    synchronized (this.sync)
    {
      this.base.remove(paramSession, paramRow);
      if (!removeIfExists(paramSession, paramRow)) {
        this.delta.add(paramSession, paramRow);
      }
    }
  }
  
  public void remove(Session paramSession)
  {
    synchronized (this.sync)
    {
      this.base.remove(paramSession);
    }
  }
  
  public void truncate(Session paramSession)
  {
    synchronized (this.sync)
    {
      this.delta.truncate(paramSession);
      this.base.truncate(paramSession);
    }
  }
  
  public void commit(int paramInt, Row paramRow)
  {
    synchronized (this.sync)
    {
      removeIfExists(null, paramRow);
    }
  }
  
  public int compareRows(SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    return this.base.compareRows(paramSearchRow1, paramSearchRow2);
  }
  
  public int getColumnIndex(Column paramColumn)
  {
    return this.base.getColumnIndex(paramColumn);
  }
  
  public Column[] getColumns()
  {
    return this.base.getColumns();
  }
  
  public IndexColumn[] getIndexColumns()
  {
    return this.base.getIndexColumns();
  }
  
  public String getCreateSQL()
  {
    return this.base.getCreateSQL();
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    return this.base.getCreateSQLForCopy(paramTable, paramString);
  }
  
  public String getDropSQL()
  {
    return this.base.getDropSQL();
  }
  
  public IndexType getIndexType()
  {
    return this.base.getIndexType();
  }
  
  public String getPlanSQL()
  {
    return this.base.getPlanSQL();
  }
  
  public long getRowCount(Session paramSession)
  {
    return this.base.getRowCount(paramSession);
  }
  
  public Table getTable()
  {
    return this.base.getTable();
  }
  
  public int getType()
  {
    return this.base.getType();
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    synchronized (this.sync)
    {
      this.table.removeIndex(this);
      remove(paramSession);
    }
  }
  
  public String getSQL()
  {
    return this.base.getSQL();
  }
  
  public Schema getSchema()
  {
    return this.base.getSchema();
  }
  
  public void checkRename()
  {
    this.base.checkRename();
  }
  
  public ArrayList<DbObject> getChildren()
  {
    return this.base.getChildren();
  }
  
  public String getComment()
  {
    return this.base.getComment();
  }
  
  public Database getDatabase()
  {
    return this.base.getDatabase();
  }
  
  public int getId()
  {
    return this.base.getId();
  }
  
  public String getName()
  {
    return this.base.getName();
  }
  
  public boolean isTemporary()
  {
    return this.base.isTemporary();
  }
  
  public void rename(String paramString)
  {
    this.base.rename(paramString);
  }
  
  public void setComment(String paramString)
  {
    this.base.setComment(paramString);
  }
  
  public void setTemporary(boolean paramBoolean)
  {
    this.base.setTemporary(paramBoolean);
  }
  
  public long getRowCountApproximation()
  {
    return this.base.getRowCountApproximation();
  }
  
  public long getDiskSpaceUsed()
  {
    return this.base.getDiskSpaceUsed();
  }
  
  public Index getBaseIndex()
  {
    return this.base;
  }
  
  public Row getRow(Session paramSession, long paramLong)
  {
    return this.base.getRow(paramSession, paramLong);
  }
  
  public boolean isHidden()
  {
    return this.base.isHidden();
  }
  
  public boolean isRowIdIndex()
  {
    return (this.base.isRowIdIndex()) && (this.delta.isRowIdIndex());
  }
  
  public boolean canScan()
  {
    return this.base.canScan();
  }
  
  public void setSortedInsertMode(boolean paramBoolean)
  {
    this.base.setSortedInsertMode(paramBoolean);
    this.delta.setSortedInsertMode(paramBoolean);
  }
}
