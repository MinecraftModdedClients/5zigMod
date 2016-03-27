package org.h2.table;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.expression.FunctionCall;
import org.h2.expression.TableFunction;
import org.h2.index.FunctionIndex;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueNull;
import org.h2.value.ValueResultSet;

public class FunctionTable
  extends Table
{
  private final FunctionCall function;
  private final long rowCount;
  private Expression functionExpr;
  private LocalResult cachedResult;
  private Value cachedValue;
  
  public FunctionTable(Schema paramSchema, Session paramSession, Expression paramExpression, FunctionCall paramFunctionCall)
  {
    super(paramSchema, 0, paramFunctionCall.getName(), false, true);
    this.functionExpr = paramExpression;
    this.function = paramFunctionCall;
    if ((paramFunctionCall instanceof TableFunction)) {
      this.rowCount = ((TableFunction)paramFunctionCall).getRowCount();
    } else {
      this.rowCount = Long.MAX_VALUE;
    }
    paramFunctionCall.optimize(paramSession);
    int i = paramFunctionCall.getType();
    if (i != 18) {
      throw DbException.get(90000, paramFunctionCall.getName());
    }
    Expression[] arrayOfExpression1 = paramFunctionCall.getArgs();
    int j = arrayOfExpression1.length;
    Expression[] arrayOfExpression2 = new Expression[j];
    for (int k = 0; k < j; k++)
    {
      arrayOfExpression1[k] = arrayOfExpression1[k].optimize(paramSession);
      arrayOfExpression2[k] = arrayOfExpression1[k];
    }
    ValueResultSet localValueResultSet = paramFunctionCall.getValueForColumnList(paramSession, arrayOfExpression2);
    if (localValueResultSet == null) {
      throw DbException.get(90000, paramFunctionCall.getName());
    }
    ResultSet localResultSet = localValueResultSet.getResultSet();
    try
    {
      ResultSetMetaData localResultSetMetaData = localResultSet.getMetaData();
      int m = localResultSetMetaData.getColumnCount();
      Column[] arrayOfColumn = new Column[m];
      for (int n = 0; n < m; n++) {
        arrayOfColumn[n] = new Column(localResultSetMetaData.getColumnName(n + 1), DataType.getValueTypeFromResultSet(localResultSetMetaData, n + 1), localResultSetMetaData.getPrecision(n + 1), localResultSetMetaData.getScale(n + 1), localResultSetMetaData.getColumnDisplaySize(n + 1));
      }
      setColumns(arrayOfColumn);
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
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
    throw DbException.getUnsupportedException("ALIAS");
  }
  
  public void removeRow(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("ALIAS");
  }
  
  public void truncate(Session paramSession)
  {
    throw DbException.getUnsupportedException("ALIAS");
  }
  
  public boolean canDrop()
  {
    throw DbException.throwInternalError();
  }
  
  public void addRow(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("ALIAS");
  }
  
  public void checkSupportAlter()
  {
    throw DbException.getUnsupportedException("ALIAS");
  }
  
  public String getTableType()
  {
    return null;
  }
  
  public Index getScanIndex(Session paramSession)
  {
    return new FunctionIndex(this, IndexColumn.wrap(this.columns));
  }
  
  public ArrayList<Index> getIndexes()
  {
    return null;
  }
  
  public boolean canGetRowCount()
  {
    return this.rowCount != Long.MAX_VALUE;
  }
  
  public long getRowCount(Session paramSession)
  {
    return this.rowCount;
  }
  
  public String getCreateSQL()
  {
    return null;
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("ALIAS");
  }
  
  public ResultInterface getResult(Session paramSession)
  {
    ValueResultSet localValueResultSet = getValueResultSet(paramSession);
    if (localValueResultSet == null) {
      return null;
    }
    if ((this.cachedResult != null) && (this.cachedValue == localValueResultSet))
    {
      this.cachedResult.reset();
      return this.cachedResult;
    }
    LocalResult localLocalResult = LocalResult.read(paramSession, localValueResultSet.getResultSet(), 0);
    if (this.function.isDeterministic())
    {
      this.cachedResult = localLocalResult;
      this.cachedValue = localValueResultSet;
    }
    return localLocalResult;
  }
  
  public ResultSet getResultSet(Session paramSession)
  {
    ValueResultSet localValueResultSet = getValueResultSet(paramSession);
    return localValueResultSet == null ? null : localValueResultSet.getResultSet();
  }
  
  private ValueResultSet getValueResultSet(Session paramSession)
  {
    this.functionExpr = this.functionExpr.optimize(paramSession);
    Value localValue = this.functionExpr.getValue(paramSession);
    if (localValue == ValueNull.INSTANCE) {
      return null;
    }
    return (ValueResultSet)localValue;
  }
  
  public boolean isBufferResultSetToLocalTemp()
  {
    return this.function.isBufferResultSetToLocalTemp();
  }
  
  public long getMaxDataModificationId()
  {
    return Long.MAX_VALUE;
  }
  
  public Index getUniqueIndex()
  {
    return null;
  }
  
  public String getSQL()
  {
    return this.function.getSQL();
  }
  
  public long getRowCountApproximation()
  {
    return this.rowCount;
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
  
  public boolean isDeterministic()
  {
    return this.function.isDeterministic();
  }
  
  public boolean canReference()
  {
    return false;
  }
}
