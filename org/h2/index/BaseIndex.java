package org.h2.index;

import org.h2.engine.Database;
import org.h2.engine.Mode;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.schema.SchemaObjectBase;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.MathUtils;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public abstract class BaseIndex
  extends SchemaObjectBase
  implements Index
{
  protected IndexColumn[] indexColumns;
  protected Column[] columns;
  protected int[] columnIds;
  protected Table table;
  protected IndexType indexType;
  protected boolean isMultiVersion;
  
  protected void initBaseIndex(Table paramTable, int paramInt, String paramString, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType)
  {
    initSchemaObjectBase(paramTable.getSchema(), paramInt, paramString, "index");
    this.indexType = paramIndexType;
    this.table = paramTable;
    if (paramArrayOfIndexColumn != null)
    {
      this.indexColumns = paramArrayOfIndexColumn;
      this.columns = new Column[paramArrayOfIndexColumn.length];
      int i = this.columns.length;
      this.columnIds = new int[i];
      for (int j = 0; j < i; j++)
      {
        Column localColumn = paramArrayOfIndexColumn[j].column;
        this.columns[j] = localColumn;
        this.columnIds[j] = localColumn.getColumnId();
      }
    }
  }
  
  protected static void checkIndexColumnTypes(IndexColumn[] paramArrayOfIndexColumn)
  {
    for (IndexColumn localIndexColumn : paramArrayOfIndexColumn)
    {
      int k = localIndexColumn.column.getType();
      if ((k == 16) || (k == 15)) {
        throw DbException.getUnsupportedException("Index on BLOB or CLOB column: " + localIndexColumn.column.getCreateSQL());
      }
    }
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  protected DbException getDuplicateKeyException(String paramString)
  {
    String str = getName() + " ON " + this.table.getSQL() + "(" + getColumnListSQL() + ")";
    if (paramString != null) {
      str = str + " VALUES " + paramString;
    }
    DbException localDbException = DbException.get(23505, str);
    localDbException.setSource(this);
    return localDbException;
  }
  
  public String getPlanSQL()
  {
    return getSQL();
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    this.table.removeIndex(this);
    remove(paramSession);
    this.database.removeMeta(paramSession, getId());
  }
  
  public boolean canFindNext()
  {
    return false;
  }
  
  public Cursor find(TableFilter paramTableFilter, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    return find(paramTableFilter.getSession(), paramSearchRow1, paramSearchRow2);
  }
  
  public Cursor findNext(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    throw DbException.throwInternalError();
  }
  
  protected long getCostRangeIndex(int[] paramArrayOfInt, long paramLong, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    paramLong += 1000L;
    long l1 = paramLong;
    long l2 = paramLong;
    int i = 0;
    if (paramArrayOfInt == null) {
      return l1;
    }
    int j = 0;
    Object localObject;
    int m;
    int n;
    for (int k = this.columns.length; j < k; j++)
    {
      localObject = this.columns[j];
      m = ((Column)localObject).getColumnId();
      n = paramArrayOfInt[m];
      if ((n & 0x1) == 1)
      {
        if ((j == this.columns.length - 1) && (getIndexType().isUnique()))
        {
          l1 = 3L;
          break;
        }
        i = 100 - (100 - i) * (100 - ((Column)localObject).getSelectivity()) / 100;
        
        long l3 = paramLong * i / 100L;
        if (l3 <= 0L) {
          l3 = 1L;
        }
        l2 = Math.max(paramLong / l3, 1L);
        l1 = 2L + l2;
      }
      else
      {
        if ((n & 0x6) == 6)
        {
          l1 = 2L + l2 / 4L;
          break;
        }
        if ((n & 0x2) == 2)
        {
          l1 = 2L + l2 / 3L;
          break;
        }
        if ((n & 0x4) != 4) {
          break;
        }
        l1 = l2 / 3L;
        break;
      }
    }
    if (paramSortOrder != null)
    {
      j = 1;
      k = 0;
      localObject = paramSortOrder.getSortTypes();
      m = 0;
      for (n = localObject.length; m < n; m++)
      {
        if (m >= this.indexColumns.length) {
          break;
        }
        Column localColumn = paramSortOrder.getColumn(m, paramTableFilter);
        if (localColumn == null)
        {
          j = 0;
          break;
        }
        IndexColumn localIndexColumn = this.indexColumns[m];
        if (localColumn != localIndexColumn.column)
        {
          j = 0;
          break;
        }
        int i1 = localObject[m];
        if (i1 != localIndexColumn.sortType)
        {
          j = 0;
          break;
        }
        k++;
      }
      if (j != 0) {
        l1 -= k;
      }
    }
    return l1;
  }
  
  public int compareRows(SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    if (paramSearchRow1 == paramSearchRow2) {
      return 0;
    }
    int i = 0;
    for (int j = this.indexColumns.length; i < j; i++)
    {
      int k = this.columnIds[i];
      Value localValue = paramSearchRow2.getValue(k);
      if (localValue == null) {
        return 0;
      }
      int m = compareValues(paramSearchRow1.getValue(k), localValue, this.indexColumns[i].sortType);
      if (m != 0) {
        return m;
      }
    }
    return 0;
  }
  
  protected boolean containsNullAndAllowMultipleNull(SearchRow paramSearchRow)
  {
    Mode localMode = this.database.getMode();
    if (localMode.uniqueIndexSingleNull) {
      return false;
    }
    int k;
    Value localValue;
    if (localMode.uniqueIndexSingleNullExceptAllColumnsAreNull)
    {
      for (k : this.columnIds)
      {
        localValue = paramSearchRow.getValue(k);
        if (localValue != ValueNull.INSTANCE) {
          return false;
        }
      }
      return true;
    }
    for (k : this.columnIds)
    {
      localValue = paramSearchRow.getValue(k);
      if (localValue == ValueNull.INSTANCE) {
        return true;
      }
    }
    return false;
  }
  
  int compareKeys(SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    long l1 = paramSearchRow1.getKey();
    long l2 = paramSearchRow2.getKey();
    if (l1 == l2)
    {
      if (this.isMultiVersion)
      {
        int i = paramSearchRow1.getVersion();
        int j = paramSearchRow2.getVersion();
        return MathUtils.compareInt(j, i);
      }
      return 0;
    }
    return l1 > l2 ? 1 : -1;
  }
  
  private int compareValues(Value paramValue1, Value paramValue2, int paramInt)
  {
    if (paramValue1 == paramValue2) {
      return 0;
    }
    boolean bool = paramValue1 == null;int i = paramValue2 == null ? 1 : 0;
    if ((bool) || (i != 0)) {
      return SortOrder.compareNull(bool, paramInt);
    }
    int j = this.table.compareTypeSave(paramValue1, paramValue2);
    if ((paramInt & 0x1) != 0) {
      j = -j;
    }
    return j;
  }
  
  public int getColumnIndex(Column paramColumn)
  {
    int i = 0;
    for (int j = this.columns.length; i < j; i++) {
      if (this.columns[i].equals(paramColumn)) {
        return i;
      }
    }
    return -1;
  }
  
  private String getColumnListSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder();
    for (IndexColumn localIndexColumn : this.indexColumns)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(localIndexColumn.getSQL());
    }
    return localStatementBuilder.toString();
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder("CREATE ");
    localStringBuilder.append(this.indexType.getSQL());
    localStringBuilder.append(' ');
    if (this.table.isHidden()) {
      localStringBuilder.append("IF NOT EXISTS ");
    }
    localStringBuilder.append(paramString);
    localStringBuilder.append(" ON ").append(paramTable.getSQL());
    if (this.comment != null) {
      localStringBuilder.append(" COMMENT ").append(StringUtils.quoteStringSQL(this.comment));
    }
    localStringBuilder.append('(').append(getColumnListSQL()).append(')');
    return localStringBuilder.toString();
  }
  
  public String getCreateSQL()
  {
    return getCreateSQLForCopy(this.table, getSQL());
  }
  
  public IndexColumn[] getIndexColumns()
  {
    return this.indexColumns;
  }
  
  public Column[] getColumns()
  {
    return this.columns;
  }
  
  public IndexType getIndexType()
  {
    return this.indexType;
  }
  
  public int getType()
  {
    return 1;
  }
  
  public Table getTable()
  {
    return this.table;
  }
  
  public void commit(int paramInt, Row paramRow) {}
  
  void setMultiVersion(boolean paramBoolean)
  {
    this.isMultiVersion = paramBoolean;
  }
  
  public Row getRow(Session paramSession, long paramLong)
  {
    throw DbException.getUnsupportedException(toString());
  }
  
  public boolean isHidden()
  {
    return this.table.isHidden();
  }
  
  public boolean isRowIdIndex()
  {
    return false;
  }
  
  public boolean canScan()
  {
    return true;
  }
  
  public void setSortedInsertMode(boolean paramBoolean) {}
}
