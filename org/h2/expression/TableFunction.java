package org.h2.expression;

import java.util.ArrayList;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.table.Column;
import org.h2.tools.SimpleResultSet;
import org.h2.util.MathUtils;
import org.h2.util.StatementBuilder;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueNull;
import org.h2.value.ValueResultSet;

public class TableFunction
  extends Function
{
  private final boolean distinct;
  private final long rowCount;
  private Column[] columnList;
  
  TableFunction(Database paramDatabase, FunctionInfo paramFunctionInfo, long paramLong)
  {
    super(paramDatabase, paramFunctionInfo);
    this.distinct = (paramFunctionInfo.type == 224);
    this.rowCount = paramLong;
  }
  
  public Value getValue(Session paramSession)
  {
    return getTable(paramSession, this.args, false, this.distinct);
  }
  
  protected void checkParameterCount(int paramInt)
  {
    if (paramInt < 1) {
      throw DbException.get(7001, new String[] { getName(), ">0" });
    }
  }
  
  public String getSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder(getName());
    localStatementBuilder.append('(');
    int i = 0;
    for (Expression localExpression : this.args)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(this.columnList[(i++)].getCreateSQL()).append('=').append(localExpression.getSQL());
    }
    return localStatementBuilder.append(')').toString();
  }
  
  public String getName()
  {
    return this.distinct ? "TABLE_DISTINCT" : "TABLE";
  }
  
  public ValueResultSet getValueForColumnList(Session paramSession, Expression[] paramArrayOfExpression)
  {
    return getTable(paramSession, this.args, true, false);
  }
  
  public void setColumns(ArrayList<Column> paramArrayList)
  {
    this.columnList = new Column[paramArrayList.size()];
    paramArrayList.toArray(this.columnList);
  }
  
  private ValueResultSet getTable(Session paramSession, Expression[] paramArrayOfExpression, boolean paramBoolean1, boolean paramBoolean2)
  {
    int i = this.columnList.length;
    Expression[] arrayOfExpression = new Expression[i];
    Database localDatabase = paramSession.getDatabase();
    for (int j = 0; j < i; j++)
    {
      localObject1 = this.columnList[j];
      ExpressionColumn localExpressionColumn = new ExpressionColumn(localDatabase, (Column)localObject1);
      arrayOfExpression[j] = localExpressionColumn;
    }
    LocalResult localLocalResult = new LocalResult(paramSession, arrayOfExpression, i);
    if (paramBoolean2) {
      localLocalResult.setDistinct();
    }
    if (!paramBoolean1)
    {
      localObject1 = new Value[i][];
      int k = 0;
      Object localObject2;
      Value[] arrayOfValue;
      for (int m = 0; m < i; m++)
      {
        localObject2 = paramArrayOfExpression[m].getValue(paramSession);
        if (localObject2 == ValueNull.INSTANCE)
        {
          localObject1[m] = new Value[0];
        }
        else
        {
          ValueArray localValueArray = (ValueArray)((Value)localObject2).convertTo(17);
          arrayOfValue = localValueArray.getList();
          localObject1[m] = arrayOfValue;
          k = Math.max(k, arrayOfValue.length);
        }
      }
      for (m = 0; m < k; m++)
      {
        localObject2 = new Value[i];
        for (int n = 0; n < i; n++)
        {
          arrayOfValue = localObject1[n];
          Object localObject3;
          if (arrayOfValue.length <= m)
          {
            localObject3 = ValueNull.INSTANCE;
          }
          else
          {
            Column localColumn = this.columnList[n];
            localObject3 = arrayOfValue[m];
            localObject3 = localColumn.convert((Value)localObject3);
            localObject3 = ((Value)localObject3).convertPrecision(localColumn.getPrecision(), false);
            localObject3 = ((Value)localObject3).convertScale(true, localColumn.getScale());
          }
          localObject2[n] = localObject3;
        }
        localLocalResult.addRow((Value[])localObject2);
      }
    }
    localLocalResult.done();
    Object localObject1 = ValueResultSet.get(getSimpleResultSet(localLocalResult, Integer.MAX_VALUE));
    
    return (ValueResultSet)localObject1;
  }
  
  private static SimpleResultSet getSimpleResultSet(ResultInterface paramResultInterface, int paramInt)
  {
    int i = paramResultInterface.getVisibleColumnCount();
    SimpleResultSet localSimpleResultSet = new SimpleResultSet();
    localSimpleResultSet.setAutoClose(false);
    Object localObject;
    int k;
    for (int j = 0; j < i; j++)
    {
      localObject = paramResultInterface.getColumnName(j);
      k = DataType.convertTypeToSQLType(paramResultInterface.getColumnType(j));
      int m = MathUtils.convertLongToInt(paramResultInterface.getColumnPrecision(j));
      int n = paramResultInterface.getColumnScale(j);
      localSimpleResultSet.addColumn((String)localObject, k, m, n);
    }
    paramResultInterface.reset();
    for (j = 0; (j < paramInt) && (paramResultInterface.next()); j++)
    {
      localObject = new Object[i];
      for (k = 0; k < i; k++) {
        localObject[k] = paramResultInterface.currentRow()[k].getObject();
      }
      localSimpleResultSet.addRow((Object[])localObject);
    }
    return localSimpleResultSet;
  }
  
  public long getRowCount()
  {
    return this.rowCount;
  }
  
  public Expression[] getExpressionColumns(Session paramSession)
  {
    return getExpressionColumns(paramSession, getTable(paramSession, getArgs(), true, false).getResultSet());
  }
}
