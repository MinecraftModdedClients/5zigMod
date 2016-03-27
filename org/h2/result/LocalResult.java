package org.h2.result;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.util.New;
import org.h2.util.ValueHashMap;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueArray;

public class LocalResult
  implements ResultInterface, ResultTarget
{
  private int maxMemoryRows;
  private Session session;
  private int visibleColumnCount;
  private Expression[] expressions;
  private int rowId;
  private int rowCount;
  private ArrayList<Value[]> rows;
  private SortOrder sort;
  private ValueHashMap<Value[]> distinctRows;
  private Value[] currentRow;
  private int offset;
  private int limit = -1;
  private ResultExternal external;
  private int diskOffset;
  private boolean distinct;
  private boolean randomAccess;
  private boolean closed;
  
  public LocalResult() {}
  
  public LocalResult(Session paramSession, Expression[] paramArrayOfExpression, int paramInt)
  {
    this.session = paramSession;
    if (paramSession == null)
    {
      this.maxMemoryRows = Integer.MAX_VALUE;
    }
    else
    {
      Database localDatabase = paramSession.getDatabase();
      if ((localDatabase.isPersistent()) && (!localDatabase.isReadOnly())) {
        this.maxMemoryRows = paramSession.getDatabase().getMaxMemoryRows();
      } else {
        this.maxMemoryRows = Integer.MAX_VALUE;
      }
    }
    this.rows = New.arrayList();
    this.visibleColumnCount = paramInt;
    this.rowId = -1;
    this.expressions = paramArrayOfExpression;
  }
  
  public void setMaxMemoryRows(int paramInt)
  {
    this.maxMemoryRows = paramInt;
  }
  
  public static LocalResult read(Session paramSession, ResultSet paramResultSet, int paramInt)
  {
    Expression[] arrayOfExpression = Expression.getExpressionColumns(paramSession, paramResultSet);
    int i = arrayOfExpression.length;
    LocalResult localLocalResult = new LocalResult(paramSession, arrayOfExpression, i);
    try
    {
      for (int j = 0; ((paramInt == 0) || (j < paramInt)) && (paramResultSet.next()); j++)
      {
        Value[] arrayOfValue = new Value[i];
        for (int k = 0; k < i; k++)
        {
          int m = localLocalResult.getColumnType(k);
          arrayOfValue[k] = DataType.readValue(paramSession, paramResultSet, k + 1, m);
        }
        localLocalResult.addRow(arrayOfValue);
      }
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
    localLocalResult.done();
    return localLocalResult;
  }
  
  public LocalResult createShallowCopy(Session paramSession)
  {
    if ((this.external == null) && ((this.rows == null) || (this.rows.size() < this.rowCount))) {
      return null;
    }
    ResultExternal localResultExternal = null;
    if (this.external != null)
    {
      localResultExternal = this.external.createShallowCopy();
      if (localResultExternal == null) {
        return null;
      }
    }
    LocalResult localLocalResult = new LocalResult();
    localLocalResult.maxMemoryRows = this.maxMemoryRows;
    localLocalResult.session = paramSession;
    localLocalResult.visibleColumnCount = this.visibleColumnCount;
    localLocalResult.expressions = this.expressions;
    localLocalResult.rowId = -1;
    localLocalResult.rowCount = this.rowCount;
    localLocalResult.rows = this.rows;
    localLocalResult.sort = this.sort;
    localLocalResult.distinctRows = this.distinctRows;
    localLocalResult.distinct = this.distinct;
    localLocalResult.randomAccess = this.randomAccess;
    localLocalResult.currentRow = null;
    localLocalResult.offset = 0;
    localLocalResult.limit = -1;
    localLocalResult.external = localResultExternal;
    localLocalResult.diskOffset = this.diskOffset;
    return localLocalResult;
  }
  
  public void setSortOrder(SortOrder paramSortOrder)
  {
    this.sort = paramSortOrder;
  }
  
  public void setDistinct()
  {
    this.distinct = true;
    this.distinctRows = ValueHashMap.newInstance();
  }
  
  public void setRandomAccess()
  {
    this.randomAccess = true;
  }
  
  public void removeDistinct(Value[] paramArrayOfValue)
  {
    if (!this.distinct) {
      DbException.throwInternalError();
    }
    if (this.distinctRows != null)
    {
      ValueArray localValueArray = ValueArray.get(paramArrayOfValue);
      this.distinctRows.remove(localValueArray);
      this.rowCount = this.distinctRows.size();
    }
    else
    {
      this.rowCount = this.external.removeRow(paramArrayOfValue);
    }
  }
  
  public boolean containsDistinct(Value[] paramArrayOfValue)
  {
    if (this.external != null) {
      return this.external.contains(paramArrayOfValue);
    }
    if (this.distinctRows == null)
    {
      this.distinctRows = ValueHashMap.newInstance();
      for (localObject1 = this.rows.iterator(); ((Iterator)localObject1).hasNext();)
      {
        Object localObject2 = (Value[])((Iterator)localObject1).next();
        if (localObject2.length > this.visibleColumnCount)
        {
          localObject3 = new Value[this.visibleColumnCount];
          System.arraycopy(localObject2, 0, localObject3, 0, this.visibleColumnCount);
          localObject2 = localObject3;
        }
        Object localObject3 = ValueArray.get((Value[])localObject2);
        this.distinctRows.put((Value)localObject3, localObject2);
      }
    }
    Object localObject1 = ValueArray.get(paramArrayOfValue);
    return this.distinctRows.get((Value)localObject1) != null;
  }
  
  public void reset()
  {
    this.rowId = -1;
    if (this.external != null)
    {
      this.external.reset();
      if (this.diskOffset > 0) {
        for (int i = 0; i < this.diskOffset; i++) {
          this.external.next();
        }
      }
    }
  }
  
  public Value[] currentRow()
  {
    return this.currentRow;
  }
  
  public boolean next()
  {
    if ((!this.closed) && (this.rowId < this.rowCount))
    {
      this.rowId += 1;
      if (this.rowId < this.rowCount)
      {
        if (this.external != null) {
          this.currentRow = this.external.next();
        } else {
          this.currentRow = ((Value[])this.rows.get(this.rowId));
        }
        return true;
      }
      this.currentRow = null;
    }
    return false;
  }
  
  public int getRowId()
  {
    return this.rowId;
  }
  
  private void cloneLobs(Value[] paramArrayOfValue)
  {
    for (int i = 0; i < paramArrayOfValue.length; i++)
    {
      Value localValue1 = paramArrayOfValue[i];
      Value localValue2 = localValue1.copyToResult();
      if (localValue2 != localValue1)
      {
        this.session.addTemporaryLob(localValue2);
        paramArrayOfValue[i] = localValue2;
      }
    }
  }
  
  public void addRow(Value[] paramArrayOfValue)
  {
    cloneLobs(paramArrayOfValue);
    if (this.distinct)
    {
      if (this.distinctRows != null)
      {
        ValueArray localValueArray = ValueArray.get(paramArrayOfValue);
        this.distinctRows.put(localValueArray, paramArrayOfValue);
        this.rowCount = this.distinctRows.size();
        if (this.rowCount > this.maxMemoryRows)
        {
          this.external = new ResultTempTable(this.session, this.expressions, true, this.sort);
          this.rowCount = this.external.addRows(this.distinctRows.values());
          this.distinctRows = null;
        }
      }
      else
      {
        this.rowCount = this.external.addRow(paramArrayOfValue);
      }
      return;
    }
    this.rows.add(paramArrayOfValue);
    this.rowCount += 1;
    if (this.rows.size() > this.maxMemoryRows)
    {
      if (this.external == null) {
        this.external = new ResultTempTable(this.session, this.expressions, false, this.sort);
      }
      addRowsToDisk();
    }
  }
  
  private void addRowsToDisk()
  {
    this.rowCount = this.external.addRows(this.rows);
    this.rows.clear();
  }
  
  public int getVisibleColumnCount()
  {
    return this.visibleColumnCount;
  }
  
  public void done()
  {
    if (this.distinct) {
      if (this.distinctRows != null)
      {
        this.rows = this.distinctRows.values();
      }
      else if ((this.external != null) && (this.sort != null))
      {
        ResultExternal localResultExternal = this.external;
        this.external = null;
        localResultExternal.reset();
        this.rows = New.arrayList();
        for (;;)
        {
          Value[] arrayOfValue = localResultExternal.next();
          if (arrayOfValue == null) {
            break;
          }
          if (this.external == null) {
            this.external = new ResultTempTable(this.session, this.expressions, true, this.sort);
          }
          this.rows.add(arrayOfValue);
          if (this.rows.size() > this.maxMemoryRows)
          {
            this.rowCount = this.external.addRows(this.rows);
            this.rows.clear();
          }
        }
        localResultExternal.close();
      }
    }
    if (this.external != null)
    {
      addRowsToDisk();
      this.external.done();
    }
    else if (this.sort != null)
    {
      if ((this.offset > 0) || (this.limit > 0)) {
        this.sort.sort(this.rows, this.offset, this.limit < 0 ? this.rows.size() : this.limit);
      } else {
        this.sort.sort(this.rows);
      }
    }
    applyOffset();
    applyLimit();
    reset();
  }
  
  public int getRowCount()
  {
    return this.rowCount;
  }
  
  public void setLimit(int paramInt)
  {
    this.limit = paramInt;
  }
  
  private void applyLimit()
  {
    if (this.limit < 0) {
      return;
    }
    if (this.external == null)
    {
      if (this.rows.size() > this.limit)
      {
        this.rows = New.arrayList(this.rows.subList(0, this.limit));
        this.rowCount = this.limit;
      }
    }
    else if (this.limit < this.rowCount) {
      this.rowCount = this.limit;
    }
  }
  
  public boolean needToClose()
  {
    return this.external != null;
  }
  
  public void close()
  {
    if (this.external != null)
    {
      this.external.close();
      this.external = null;
      this.closed = true;
    }
  }
  
  public String getAlias(int paramInt)
  {
    return this.expressions[paramInt].getAlias();
  }
  
  public String getTableName(int paramInt)
  {
    return this.expressions[paramInt].getTableName();
  }
  
  public String getSchemaName(int paramInt)
  {
    return this.expressions[paramInt].getSchemaName();
  }
  
  public int getDisplaySize(int paramInt)
  {
    return this.expressions[paramInt].getDisplaySize();
  }
  
  public String getColumnName(int paramInt)
  {
    return this.expressions[paramInt].getColumnName();
  }
  
  public int getColumnType(int paramInt)
  {
    return this.expressions[paramInt].getType();
  }
  
  public long getColumnPrecision(int paramInt)
  {
    return this.expressions[paramInt].getPrecision();
  }
  
  public int getNullable(int paramInt)
  {
    return this.expressions[paramInt].getNullable();
  }
  
  public boolean isAutoIncrement(int paramInt)
  {
    return this.expressions[paramInt].isAutoIncrement();
  }
  
  public int getColumnScale(int paramInt)
  {
    return this.expressions[paramInt].getScale();
  }
  
  public void setOffset(int paramInt)
  {
    this.offset = paramInt;
  }
  
  private void applyOffset()
  {
    if (this.offset <= 0) {
      return;
    }
    if (this.external == null)
    {
      if (this.offset >= this.rows.size())
      {
        this.rows.clear();
        this.rowCount = 0;
      }
      else
      {
        int i = Math.min(this.offset, this.rows.size());
        this.rows = New.arrayList(this.rows.subList(i, this.rows.size()));
        this.rowCount -= i;
      }
    }
    else if (this.offset >= this.rowCount)
    {
      this.rowCount = 0;
    }
    else
    {
      this.diskOffset = this.offset;
      this.rowCount -= this.offset;
    }
  }
  
  public String toString()
  {
    return super.toString() + " columns: " + this.visibleColumnCount + " rows: " + this.rowCount + " pos: " + this.rowId;
  }
  
  public boolean isClosed()
  {
    return this.closed;
  }
  
  public int getFetchSize()
  {
    return 0;
  }
  
  public void setFetchSize(int paramInt) {}
}
