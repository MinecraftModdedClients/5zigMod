package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.h2.message.DbException;
import org.h2.tools.SimpleResultSet;
import org.h2.util.StatementBuilder;

public class ValueResultSet
  extends Value
{
  private final ResultSet result;
  
  private ValueResultSet(ResultSet paramResultSet)
  {
    this.result = paramResultSet;
  }
  
  public static ValueResultSet get(ResultSet paramResultSet)
  {
    ValueResultSet localValueResultSet = new ValueResultSet(paramResultSet);
    return localValueResultSet;
  }
  
  public static ValueResultSet getCopy(ResultSet paramResultSet, int paramInt)
  {
    try
    {
      ResultSetMetaData localResultSetMetaData = paramResultSet.getMetaData();
      int i = localResultSetMetaData.getColumnCount();
      SimpleResultSet localSimpleResultSet = new SimpleResultSet();
      localSimpleResultSet.setAutoClose(false);
      ValueResultSet localValueResultSet = new ValueResultSet(localSimpleResultSet);
      Object localObject;
      int k;
      for (int j = 0; j < i; j++)
      {
        localObject = localResultSetMetaData.getColumnLabel(j + 1);
        k = localResultSetMetaData.getColumnType(j + 1);
        int m = localResultSetMetaData.getPrecision(j + 1);
        int n = localResultSetMetaData.getScale(j + 1);
        localSimpleResultSet.addColumn((String)localObject, k, m, n);
      }
      for (j = 0; (j < paramInt) && (paramResultSet.next()); j++)
      {
        localObject = new Object[i];
        for (k = 0; k < i; k++) {
          localObject[k] = paramResultSet.getObject(k + 1);
        }
        localSimpleResultSet.addRow((Object[])localObject);
      }
      return localValueResultSet;
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
  }
  
  public int getType()
  {
    return 18;
  }
  
  public long getPrecision()
  {
    return 2147483647L;
  }
  
  public int getDisplaySize()
  {
    return Integer.MAX_VALUE;
  }
  
  public String getString()
  {
    try
    {
      StatementBuilder localStatementBuilder = new StatementBuilder("(");
      this.result.beforeFirst();
      ResultSetMetaData localResultSetMetaData = this.result.getMetaData();
      int i = localResultSetMetaData.getColumnCount();
      for (int j = 0; this.result.next(); j++)
      {
        if (j > 0) {
          localStatementBuilder.append(", ");
        }
        localStatementBuilder.append('(');
        localStatementBuilder.resetCount();
        for (int k = 0; k < i; k++)
        {
          localStatementBuilder.appendExceptFirst(", ");
          int m = DataType.getValueTypeFromResultSet(localResultSetMetaData, k + 1);
          Value localValue = DataType.readValue(null, this.result, k + 1, m);
          localStatementBuilder.append(localValue.getString());
        }
        localStatementBuilder.append(')');
      }
      this.result.beforeFirst();
      return localStatementBuilder.append(')').toString();
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    return this == paramValue ? 0 : super.toString().compareTo(paramValue.toString());
  }
  
  public boolean equals(Object paramObject)
  {
    return paramObject == this;
  }
  
  public int hashCode()
  {
    return 0;
  }
  
  public Object getObject()
  {
    return this.result;
  }
  
  public ResultSet getResultSet()
  {
    return this.result;
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
  {
    throw throwUnsupportedExceptionForType("PreparedStatement.set");
  }
  
  public String getSQL()
  {
    return "";
  }
  
  public Value convertPrecision(long paramLong, boolean paramBoolean)
  {
    if (!paramBoolean) {
      return this;
    }
    SimpleResultSet localSimpleResultSet = new SimpleResultSet();
    localSimpleResultSet.setAutoClose(false);
    return get(localSimpleResultSet);
  }
}
