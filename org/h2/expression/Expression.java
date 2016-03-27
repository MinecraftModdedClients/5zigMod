package org.h2.expression;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.StringUtils;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueArray;

public abstract class Expression
{
  private boolean addedToFilter;
  
  public abstract Value getValue(Session paramSession);
  
  public abstract int getType();
  
  public abstract void mapColumns(ColumnResolver paramColumnResolver, int paramInt);
  
  public abstract Expression optimize(Session paramSession);
  
  public abstract void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean);
  
  public abstract int getScale();
  
  public abstract long getPrecision();
  
  public abstract int getDisplaySize();
  
  public abstract String getSQL();
  
  public abstract void updateAggregate(Session paramSession);
  
  public abstract boolean isEverything(ExpressionVisitor paramExpressionVisitor);
  
  public abstract int getCost();
  
  public Expression getNotIfPossible(Session paramSession)
  {
    return null;
  }
  
  public boolean isConstant()
  {
    return false;
  }
  
  public boolean isValueSet()
  {
    return false;
  }
  
  public boolean isAutoIncrement()
  {
    return false;
  }
  
  public Boolean getBooleanValue(Session paramSession)
  {
    return getValue(paramSession).getBoolean();
  }
  
  public void createIndexConditions(Session paramSession, TableFilter paramTableFilter) {}
  
  public String getColumnName()
  {
    return getAlias();
  }
  
  public String getSchemaName()
  {
    return null;
  }
  
  public String getTableName()
  {
    return null;
  }
  
  public int getNullable()
  {
    return 2;
  }
  
  public String getTableAlias()
  {
    return null;
  }
  
  public String getAlias()
  {
    return StringUtils.unEnclose(getSQL());
  }
  
  public boolean isWildcard()
  {
    return false;
  }
  
  public Expression getNonAliasExpression()
  {
    return this;
  }
  
  public void addFilterConditions(TableFilter paramTableFilter, boolean paramBoolean)
  {
    if ((!this.addedToFilter) && (!paramBoolean) && (isEverything(ExpressionVisitor.EVALUATABLE_VISITOR)))
    {
      paramTableFilter.addFilterCondition(this, false);
      this.addedToFilter = true;
    }
  }
  
  public String toString()
  {
    return getSQL();
  }
  
  public Expression[] getExpressionColumns(Session paramSession)
  {
    return null;
  }
  
  static Expression[] getExpressionColumns(Session paramSession, ValueArray paramValueArray)
  {
    Value[] arrayOfValue = paramValueArray.getList();
    ExpressionColumn[] arrayOfExpressionColumn = new ExpressionColumn[arrayOfValue.length];
    int i = 0;
    for (int j = arrayOfValue.length; i < j; i++)
    {
      Value localValue = arrayOfValue[i];
      Column localColumn = new Column("C" + (i + 1), localValue.getType(), localValue.getPrecision(), localValue.getScale(), localValue.getDisplaySize());
      
      arrayOfExpressionColumn[i] = new ExpressionColumn(paramSession.getDatabase(), localColumn);
    }
    return arrayOfExpressionColumn;
  }
  
  public static Expression[] getExpressionColumns(Session paramSession, ResultSet paramResultSet)
  {
    try
    {
      ResultSetMetaData localResultSetMetaData = paramResultSet.getMetaData();
      int i = localResultSetMetaData.getColumnCount();
      Expression[] arrayOfExpression = new Expression[i];
      Database localDatabase = paramSession == null ? null : paramSession.getDatabase();
      for (int j = 0; j < i; j++)
      {
        String str = localResultSetMetaData.getColumnLabel(j + 1);
        int k = DataType.getValueTypeFromResultSet(localResultSetMetaData, j + 1);
        int m = localResultSetMetaData.getPrecision(j + 1);
        int n = localResultSetMetaData.getScale(j + 1);
        int i1 = localResultSetMetaData.getColumnDisplaySize(j + 1);
        Column localColumn = new Column(str, k, m, n, i1);
        ExpressionColumn localExpressionColumn = new ExpressionColumn(localDatabase, localColumn);
        arrayOfExpression[j] = localExpressionColumn;
      }
      return arrayOfExpression;
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
  }
}
