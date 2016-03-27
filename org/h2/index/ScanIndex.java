package org.h2.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.store.LobStorageInterface;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.RegularTable;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.New;

public class ScanIndex
  extends BaseIndex
{
  private long firstFree = -1L;
  private ArrayList<Row> rows = New.arrayList();
  private final RegularTable tableData;
  private int rowCountDiff;
  private final HashMap<Integer, Integer> sessionRowCount;
  private HashSet<Row> delta;
  private long rowCount;
  
  public ScanIndex(RegularTable paramRegularTable, int paramInt, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType)
  {
    initBaseIndex(paramRegularTable, paramInt, paramRegularTable.getName() + "_DATA", paramArrayOfIndexColumn, paramIndexType);
    if (this.database.isMultiVersion()) {
      this.sessionRowCount = New.hashMap();
    } else {
      this.sessionRowCount = null;
    }
    this.tableData = paramRegularTable;
  }
  
  public void remove(Session paramSession)
  {
    truncate(paramSession);
  }
  
  public void truncate(Session paramSession)
  {
    this.rows = New.arrayList();
    this.firstFree = -1L;
    if ((this.tableData.getContainsLargeObject()) && (this.tableData.isPersistData())) {
      this.database.getLobStorage().removeAllForTable(this.table.getId());
    }
    this.tableData.setRowCount(0L);
    this.rowCount = 0L;
    this.rowCountDiff = 0;
    if (this.database.isMultiVersion()) {
      this.sessionRowCount.clear();
    }
  }
  
  public String getCreateSQL()
  {
    return null;
  }
  
  public void close(Session paramSession) {}
  
  public Row getRow(Session paramSession, long paramLong)
  {
    return (Row)this.rows.get((int)paramLong);
  }
  
  public void add(Session paramSession, Row paramRow)
  {
    if (this.firstFree == -1L)
    {
      int i = this.rows.size();
      paramRow.setKey(i);
      this.rows.add(paramRow);
    }
    else
    {
      long l = this.firstFree;
      Row localRow = (Row)this.rows.get((int)l);
      this.firstFree = localRow.getKey();
      paramRow.setKey(l);
      this.rows.set((int)l, paramRow);
    }
    paramRow.setDeleted(false);
    if (this.database.isMultiVersion())
    {
      if (this.delta == null) {
        this.delta = New.hashSet();
      }
      boolean bool = this.delta.remove(paramRow);
      if (!bool) {
        this.delta.add(paramRow);
      }
      incrementRowCount(paramSession.getId(), 1);
    }
    this.rowCount += 1L;
  }
  
  public void commit(int paramInt, Row paramRow)
  {
    if (this.database.isMultiVersion())
    {
      if (this.delta != null) {
        this.delta.remove(paramRow);
      }
      incrementRowCount(paramRow.getSessionId(), paramInt == 1 ? 1 : -1);
    }
  }
  
  private void incrementRowCount(int paramInt1, int paramInt2)
  {
    if (this.database.isMultiVersion())
    {
      Integer localInteger1 = Integer.valueOf(paramInt1);
      Integer localInteger2 = (Integer)this.sessionRowCount.get(localInteger1);
      int i = localInteger2 == null ? 0 : localInteger2.intValue();
      this.sessionRowCount.put(localInteger1, Integer.valueOf(i + paramInt2));
      this.rowCountDiff += paramInt2;
    }
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    if ((!this.database.isMultiVersion()) && (this.rowCount == 1L))
    {
      this.rows = New.arrayList();
      this.firstFree = -1L;
    }
    else
    {
      Row localRow = new Row(null, 1);
      localRow.setKey(this.firstFree);
      long l = paramRow.getKey();
      if (this.rows.size() <= l) {
        throw DbException.get(90112, this.rows.size() + ": " + l);
      }
      this.rows.set((int)l, localRow);
      this.firstFree = l;
    }
    if (this.database.isMultiVersion())
    {
      paramRow.setDeleted(true);
      if (this.delta == null) {
        this.delta = New.hashSet();
      }
      boolean bool = this.delta.remove(paramRow);
      if (!bool) {
        this.delta.add(paramRow);
      }
      incrementRowCount(paramSession.getId(), -1);
    }
    this.rowCount -= 1L;
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    return new ScanCursor(paramSession, this, this.database.isMultiVersion());
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    return this.tableData.getRowCountApproximation() + 1000L;
  }
  
  public long getRowCount(Session paramSession)
  {
    if (this.database.isMultiVersion())
    {
      Integer localInteger = (Integer)this.sessionRowCount.get(Integer.valueOf(paramSession.getId()));
      long l = localInteger == null ? 0L : localInteger.intValue();
      l += this.rowCount;
      l -= this.rowCountDiff;
      return l;
    }
    return this.rowCount;
  }
  
  Row getNextRow(Row paramRow)
  {
    long l;
    if (paramRow == null) {
      l = -1L;
    } else {
      l = paramRow.getKey();
    }
    do
    {
      l += 1L;
      if (l >= this.rows.size()) {
        return null;
      }
      paramRow = (Row)this.rows.get((int)l);
    } while (paramRow.isEmpty());
    return paramRow;
  }
  
  public int getColumnIndex(Column paramColumn)
  {
    return -1;
  }
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("SCAN");
  }
  
  public boolean needRebuild()
  {
    return false;
  }
  
  public boolean canGetFirstOrLast()
  {
    return false;
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    throw DbException.getUnsupportedException("SCAN");
  }
  
  Iterator<Row> getDelta()
  {
    if (this.delta == null)
    {
      List localList = Collections.emptyList();
      return localList.iterator();
    }
    return this.delta.iterator();
  }
  
  public long getRowCountApproximation()
  {
    return this.rowCount;
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
  
  public String getPlanSQL()
  {
    return this.table.getSQL() + ".tableScan";
  }
}
