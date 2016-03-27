package org.h2.result;

import java.util.ArrayList;
import java.util.Arrays;
import org.h2.command.ddl.CreateTableData;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.index.Cursor;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class ResultTempTable
  implements ResultExternal
{
  private static final String COLUMN_NAME = "DATA";
  private final boolean distinct;
  private final SortOrder sort;
  private Index index;
  private Session session;
  private Table table;
  private Cursor resultCursor;
  private int rowCount;
  private int columnCount;
  private final ResultTempTable parent;
  private boolean closed;
  private int childCount;
  private boolean containsLob;
  
  ResultTempTable(Session paramSession, Expression[] paramArrayOfExpression, boolean paramBoolean, SortOrder paramSortOrder)
  {
    this.session = paramSession;
    this.distinct = paramBoolean;
    this.sort = paramSortOrder;
    this.columnCount = paramArrayOfExpression.length;
    Schema localSchema = paramSession.getDatabase().getSchema("PUBLIC");
    CreateTableData localCreateTableData = new CreateTableData();
    for (int i = 0; i < paramArrayOfExpression.length; i++)
    {
      int j = paramArrayOfExpression[i].getType();
      Column localColumn = new Column("DATA" + i, j);
      if ((j == 16) || (j == 15)) {
        this.containsLob = true;
      }
      localCreateTableData.columns.add(localColumn);
    }
    localCreateTableData.id = paramSession.getDatabase().allocateObjectId();
    localCreateTableData.tableName = ("TEMP_RESULT_SET_" + localCreateTableData.id);
    localCreateTableData.temporary = true;
    localCreateTableData.persistIndexes = false;
    localCreateTableData.persistData = true;
    localCreateTableData.create = true;
    localCreateTableData.session = paramSession;
    this.table = localSchema.createTable(localCreateTableData);
    if ((paramSortOrder != null) || (paramBoolean)) {
      createIndex();
    }
    this.parent = null;
  }
  
  private ResultTempTable(ResultTempTable paramResultTempTable)
  {
    this.parent = paramResultTempTable;
    this.columnCount = paramResultTempTable.columnCount;
    this.distinct = paramResultTempTable.distinct;
    this.session = paramResultTempTable.session;
    this.table = paramResultTempTable.table;
    this.index = paramResultTempTable.index;
    this.rowCount = paramResultTempTable.rowCount;
    this.sort = paramResultTempTable.sort;
    this.containsLob = paramResultTempTable.containsLob;
    reset();
  }
  
  private void createIndex()
  {
    IndexColumn[] arrayOfIndexColumn = null;
    if (this.sort != null)
    {
      int[] arrayOfInt = this.sort.getQueryColumnIndexes();
      arrayOfIndexColumn = new IndexColumn[arrayOfInt.length];
      for (int j = 0; j < arrayOfInt.length; j++)
      {
        localObject = new IndexColumn();
        ((IndexColumn)localObject).column = this.table.getColumn(arrayOfInt[j]);
        ((IndexColumn)localObject).sortType = this.sort.getSortTypes()[j];
        ((IndexColumn)localObject).columnName = ("DATA" + j);
        arrayOfIndexColumn[j] = localObject;
      }
    }
    else
    {
      arrayOfIndexColumn = new IndexColumn[this.columnCount];
      for (int i = 0; i < this.columnCount; i++)
      {
        IndexColumn localIndexColumn = new IndexColumn();
        localIndexColumn.column = this.table.getColumn(i);
        localIndexColumn.columnName = ("DATA" + i);
        arrayOfIndexColumn[i] = localIndexColumn;
      }
    }
    String str = this.table.getSchema().getUniqueIndexName(this.session, this.table, "INDEX_");
    
    int k = this.session.getDatabase().allocateObjectId();
    Object localObject = IndexType.createNonUnique(true);
    this.index = this.table.addIndex(this.session, str, k, arrayOfIndexColumn, (IndexType)localObject, true, null);
  }
  
  public synchronized ResultExternal createShallowCopy()
  {
    if (this.parent != null) {
      return this.parent.createShallowCopy();
    }
    if (this.closed) {
      return null;
    }
    this.childCount += 1;
    return new ResultTempTable(this);
  }
  
  public int removeRow(Value[] paramArrayOfValue)
  {
    Row localRow = convertToRow(paramArrayOfValue);
    Cursor localCursor = find(localRow);
    if (localCursor != null)
    {
      localRow = localCursor.get();
      this.table.removeRow(this.session, localRow);
      this.rowCount -= 1;
    }
    return this.rowCount;
  }
  
  public boolean contains(Value[] paramArrayOfValue)
  {
    return find(convertToRow(paramArrayOfValue)) != null;
  }
  
  public int addRow(Value[] paramArrayOfValue)
  {
    Row localRow = convertToRow(paramArrayOfValue);
    if (this.distinct)
    {
      Cursor localCursor = find(localRow);
      if (localCursor == null)
      {
        this.table.addRow(this.session, localRow);
        this.rowCount += 1;
      }
    }
    else
    {
      this.table.addRow(this.session, localRow);
      this.rowCount += 1;
    }
    return this.rowCount;
  }
  
  public int addRows(ArrayList<Value[]> paramArrayList)
  {
    if (this.sort != null) {
      this.sort.sort(paramArrayList);
    }
    for (Value[] arrayOfValue : paramArrayList) {
      addRow(arrayOfValue);
    }
    return this.rowCount;
  }
  
  private synchronized void closeChild()
  {
    if ((--this.childCount == 0) && (this.closed)) {
      dropTable();
    }
  }
  
  public synchronized void close()
  {
    if (this.closed) {
      return;
    }
    this.closed = true;
    if (this.parent != null) {
      this.parent.closeChild();
    } else if (this.childCount == 0) {
      dropTable();
    }
  }
  
  private void dropTable()
  {
    if (this.table == null) {
      return;
    }
    if (this.containsLob) {
      return;
    }
    try
    {
      Database localDatabase = this.session.getDatabase();
      synchronized (this.session)
      {
        synchronized (localDatabase)
        {
          this.table.truncate(this.session);
        }
      }
      if (!localDatabase.isSysTableLocked())
      {
        ??? = localDatabase.getSystemSession();
        this.table.removeChildrenAndResources((Session)???);
        if (this.index != null) {
          this.session.removeLocalTempTableIndex(this.index);
        }
        synchronized (this.session)
        {
          synchronized (???)
          {
            synchronized (localDatabase)
            {
              ((Session)???).commit(false);
            }
          }
        }
      }
    }
    finally
    {
      this.table = null;
    }
  }
  
  public void done() {}
  
  public Value[] next()
  {
    if (this.resultCursor == null)
    {
      if ((this.distinct) || (this.sort != null)) {
        localObject = this.index;
      } else {
        localObject = this.table.getScanIndex(this.session);
      }
      if (this.session.getDatabase().getMvStore() != null)
      {
        if ((((Index)localObject).getRowCount(this.session) == 0L) && (this.rowCount > 0)) {
          this.resultCursor = ((Index)localObject).find((Session)null, null, null);
        } else {
          this.resultCursor = ((Index)localObject).find(this.session, null, null);
        }
      }
      else {
        this.resultCursor = ((Index)localObject).find(this.session, null, null);
      }
    }
    if (!this.resultCursor.next()) {
      return null;
    }
    Object localObject = this.resultCursor.get();
    return ((Row)localObject).getValueList();
  }
  
  public void reset()
  {
    this.resultCursor = null;
  }
  
  private Row convertToRow(Value[] paramArrayOfValue)
  {
    if (paramArrayOfValue.length < this.columnCount)
    {
      Value[] arrayOfValue = (Value[])Arrays.copyOf(paramArrayOfValue, this.columnCount);
      for (int i = paramArrayOfValue.length; i < this.columnCount; i++) {
        arrayOfValue[i] = ValueNull.INSTANCE;
      }
      paramArrayOfValue = arrayOfValue;
    }
    return new Row(paramArrayOfValue, -1);
  }
  
  private Cursor find(Row paramRow)
  {
    if (this.index == null) {
      createIndex();
    }
    Cursor localCursor = this.index.find(this.session, paramRow, paramRow);
    while (localCursor.next())
    {
      SearchRow localSearchRow = localCursor.getSearchRow();
      int i = 1;
      Database localDatabase = this.session.getDatabase();
      for (int j = 0; j < paramRow.getColumnCount(); j++) {
        if (!localDatabase.areEqual(paramRow.getValue(j), localSearchRow.getValue(j)))
        {
          i = 0;
          break;
        }
      }
      if (i != 0) {
        return localCursor;
      }
    }
    return null;
  }
}
