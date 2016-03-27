package org.h2.index;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.table.TableLink;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class LinkedIndex
  extends BaseIndex
{
  private final TableLink link;
  private final String targetTableName;
  private long rowCount;
  
  public LinkedIndex(TableLink paramTableLink, int paramInt, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType)
  {
    initBaseIndex(paramTableLink, paramInt, null, paramArrayOfIndexColumn, paramIndexType);
    this.link = paramTableLink;
    this.targetTableName = this.link.getQualifiedTable();
  }
  
  public String getCreateSQL()
  {
    return null;
  }
  
  public void close(Session paramSession) {}
  
  private static boolean isNull(Value paramValue)
  {
    return (paramValue == null) || (paramValue == ValueNull.INSTANCE);
  }
  
  public void add(Session paramSession, Row paramRow)
  {
    ArrayList localArrayList = New.arrayList();
    StatementBuilder localStatementBuilder = new StatementBuilder("INSERT INTO ");
    localStatementBuilder.append(this.targetTableName).append(" VALUES(");
    for (int i = 0; i < paramRow.getColumnCount(); i++)
    {
      Value localValue = paramRow.getValue(i);
      localStatementBuilder.appendExceptFirst(", ");
      if (localValue == null)
      {
        localStatementBuilder.append("DEFAULT");
      }
      else if (isNull(localValue))
      {
        localStatementBuilder.append("NULL");
      }
      else
      {
        localStatementBuilder.append('?');
        localArrayList.add(localValue);
      }
    }
    localStatementBuilder.append(')');
    String str = localStatementBuilder.toString();
    try
    {
      this.link.execute(str, localArrayList, true);
      this.rowCount += 1L;
    }
    catch (Exception localException)
    {
      throw TableLink.wrapException(str, localException);
    }
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    ArrayList localArrayList = New.arrayList();
    StatementBuilder localStatementBuilder = new StatementBuilder("SELECT * FROM ");
    localStatementBuilder.append(this.targetTableName).append(" T");
    Object localObject1;
    Object localObject2;
    for (int i = 0; (paramSearchRow1 != null) && (i < paramSearchRow1.getColumnCount()); i++)
    {
      localObject1 = paramSearchRow1.getValue(i);
      if (localObject1 != null)
      {
        localStatementBuilder.appendOnlyFirst(" WHERE ");
        localStatementBuilder.appendExceptFirst(" AND ");
        localObject2 = this.table.getColumn(i);
        localStatementBuilder.append(((Column)localObject2).getSQL());
        if (localObject1 == ValueNull.INSTANCE)
        {
          localStatementBuilder.append(" IS NULL");
        }
        else
        {
          localStatementBuilder.append(">=");
          addParameter(localStatementBuilder, (Column)localObject2);
          localArrayList.add(localObject1);
        }
      }
    }
    for (i = 0; (paramSearchRow2 != null) && (i < paramSearchRow2.getColumnCount()); i++)
    {
      localObject1 = paramSearchRow2.getValue(i);
      if (localObject1 != null)
      {
        localStatementBuilder.appendOnlyFirst(" WHERE ");
        localStatementBuilder.appendExceptFirst(" AND ");
        localObject2 = this.table.getColumn(i);
        localStatementBuilder.append(((Column)localObject2).getSQL());
        if (localObject1 == ValueNull.INSTANCE)
        {
          localStatementBuilder.append(" IS NULL");
        }
        else
        {
          localStatementBuilder.append("<=");
          addParameter(localStatementBuilder, (Column)localObject2);
          localArrayList.add(localObject1);
        }
      }
    }
    String str = localStatementBuilder.toString();
    try
    {
      localObject1 = this.link.execute(str, localArrayList, false);
      localObject2 = ((PreparedStatement)localObject1).getResultSet();
      return new LinkedCursor(this.link, (ResultSet)localObject2, paramSession, str, (PreparedStatement)localObject1);
    }
    catch (Exception localException)
    {
      throw TableLink.wrapException(str, localException);
    }
  }
  
  private void addParameter(StatementBuilder paramStatementBuilder, Column paramColumn)
  {
    if ((paramColumn.getType() == 21) && (this.link.isOracle())) {
      paramStatementBuilder.append("CAST(? AS CHAR(").append(paramColumn.getPrecision()).append("))");
    } else {
      paramStatementBuilder.append('?');
    }
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    return 100L + getCostRangeIndex(paramArrayOfInt, this.rowCount + 1000L, paramTableFilter, paramSortOrder);
  }
  
  public void remove(Session paramSession) {}
  
  public void truncate(Session paramSession) {}
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("LINKED");
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
    throw DbException.getUnsupportedException("LINKED");
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    ArrayList localArrayList = New.arrayList();
    StatementBuilder localStatementBuilder = new StatementBuilder("DELETE FROM ");
    localStatementBuilder.append(this.targetTableName).append(" WHERE ");
    Object localObject;
    for (int i = 0; i < paramRow.getColumnCount(); i++)
    {
      localStatementBuilder.appendExceptFirst("AND ");
      localObject = this.table.getColumn(i);
      localStatementBuilder.append(((Column)localObject).getSQL());
      Value localValue = paramRow.getValue(i);
      if (isNull(localValue))
      {
        localStatementBuilder.append(" IS NULL ");
      }
      else
      {
        localStatementBuilder.append('=');
        addParameter(localStatementBuilder, (Column)localObject);
        localArrayList.add(localValue);
        localStatementBuilder.append(' ');
      }
    }
    String str = localStatementBuilder.toString();
    try
    {
      localObject = this.link.execute(str, localArrayList, false);
      int j = ((PreparedStatement)localObject).executeUpdate();
      this.link.reusePreparedStatement((PreparedStatement)localObject, str);
      this.rowCount -= j;
    }
    catch (Exception localException)
    {
      throw TableLink.wrapException(str, localException);
    }
  }
  
  public void update(Row paramRow1, Row paramRow2)
  {
    ArrayList localArrayList = New.arrayList();
    StatementBuilder localStatementBuilder = new StatementBuilder("UPDATE ");
    localStatementBuilder.append(this.targetTableName).append(" SET ");
    Object localObject;
    for (int i = 0; i < paramRow2.getColumnCount(); i++)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(this.table.getColumn(i).getSQL()).append('=');
      localObject = paramRow2.getValue(i);
      if (localObject == null)
      {
        localStatementBuilder.append("DEFAULT");
      }
      else
      {
        localStatementBuilder.append('?');
        localArrayList.add(localObject);
      }
    }
    localStatementBuilder.append(" WHERE ");
    localStatementBuilder.resetCount();
    for (i = 0; i < paramRow1.getColumnCount(); i++)
    {
      localObject = this.table.getColumn(i);
      localStatementBuilder.appendExceptFirst(" AND ");
      localStatementBuilder.append(((Column)localObject).getSQL());
      Value localValue = paramRow1.getValue(i);
      if (isNull(localValue))
      {
        localStatementBuilder.append(" IS NULL");
      }
      else
      {
        localStatementBuilder.append('=');
        localArrayList.add(localValue);
        addParameter(localStatementBuilder, (Column)localObject);
      }
    }
    String str = localStatementBuilder.toString();
    try
    {
      this.link.execute(str, localArrayList, true);
    }
    catch (Exception localException)
    {
      throw TableLink.wrapException(str, localException);
    }
  }
  
  public long getRowCount(Session paramSession)
  {
    return this.rowCount;
  }
  
  public long getRowCountApproximation()
  {
    return this.rowCount;
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
}
