package org.h2.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.h2.command.dml.Query;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.expression.ExpressionVisitor;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.table.Column;
import org.h2.table.Table;
import org.h2.util.StatementBuilder;
import org.h2.value.CompareMode;
import org.h2.value.Value;

public class IndexCondition
{
  public static final int EQUALITY = 1;
  public static final int START = 2;
  public static final int END = 4;
  public static final int RANGE = 6;
  public static final int ALWAYS_FALSE = 8;
  public static final int SPATIAL_INTERSECTS = 16;
  private final Column column;
  private final int compareType;
  private final Expression expression;
  private List<Expression> expressionList;
  private Query expressionQuery;
  
  private IndexCondition(int paramInt, ExpressionColumn paramExpressionColumn, Expression paramExpression)
  {
    this.compareType = paramInt;
    this.column = (paramExpressionColumn == null ? null : paramExpressionColumn.getColumn());
    this.expression = paramExpression;
  }
  
  public static IndexCondition get(int paramInt, ExpressionColumn paramExpressionColumn, Expression paramExpression)
  {
    return new IndexCondition(paramInt, paramExpressionColumn, paramExpression);
  }
  
  public static IndexCondition getInList(ExpressionColumn paramExpressionColumn, List<Expression> paramList)
  {
    IndexCondition localIndexCondition = new IndexCondition(9, paramExpressionColumn, null);
    localIndexCondition.expressionList = paramList;
    return localIndexCondition;
  }
  
  public static IndexCondition getInQuery(ExpressionColumn paramExpressionColumn, Query paramQuery)
  {
    IndexCondition localIndexCondition = new IndexCondition(10, paramExpressionColumn, null);
    localIndexCondition.expressionQuery = paramQuery;
    return localIndexCondition;
  }
  
  public Value getCurrentValue(Session paramSession)
  {
    return this.expression.getValue(paramSession);
  }
  
  public Value[] getCurrentValueList(Session paramSession)
  {
    HashSet localHashSet = new HashSet();
    for (Object localObject1 = this.expressionList.iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Expression)((Iterator)localObject1).next();
      Value localValue = ((Expression)localObject2).getValue(paramSession);
      localValue = this.column.convert(localValue);
      localHashSet.add(localValue);
    }
    localObject1 = new Value[localHashSet.size()];
    localHashSet.toArray((Object[])localObject1);
    final Object localObject2 = paramSession.getDatabase().getCompareMode();
    Arrays.sort((Object[])localObject1, new Comparator()
    {
      public int compare(Value paramAnonymousValue1, Value paramAnonymousValue2)
      {
        return paramAnonymousValue1.compareTo(paramAnonymousValue2, localObject2);
      }
    });
    return (Value[])localObject1;
  }
  
  public ResultInterface getCurrentResult()
  {
    return this.expressionQuery.query(0);
  }
  
  public String getSQL()
  {
    if (this.compareType == 8) {
      return "FALSE";
    }
    StatementBuilder localStatementBuilder = new StatementBuilder();
    localStatementBuilder.append(this.column.getSQL());
    switch (this.compareType)
    {
    case 0: 
      localStatementBuilder.append(" = ");
      break;
    case 16: 
      localStatementBuilder.append(" IS ");
      break;
    case 1: 
      localStatementBuilder.append(" >= ");
      break;
    case 2: 
      localStatementBuilder.append(" > ");
      break;
    case 3: 
      localStatementBuilder.append(" <= ");
      break;
    case 4: 
      localStatementBuilder.append(" < ");
      break;
    case 9: 
      localStatementBuilder.append(" IN(");
      for (Expression localExpression : this.expressionList)
      {
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append(localExpression.getSQL());
      }
      localStatementBuilder.append(')');
      break;
    case 10: 
      localStatementBuilder.append(" IN(");
      localStatementBuilder.append(this.expressionQuery.getPlanSQL());
      localStatementBuilder.append(')');
      break;
    case 11: 
      localStatementBuilder.append(" && ");
      break;
    case 5: 
    case 6: 
    case 7: 
    case 8: 
    case 12: 
    case 13: 
    case 14: 
    case 15: 
    default: 
      DbException.throwInternalError("type=" + this.compareType);
    }
    if (this.expression != null) {
      localStatementBuilder.append(this.expression.getSQL());
    }
    return localStatementBuilder.toString();
  }
  
  public int getMask(ArrayList<IndexCondition> paramArrayList)
  {
    switch (this.compareType)
    {
    case 8: 
      return 8;
    case 0: 
    case 16: 
      return 1;
    case 9: 
    case 10: 
      if ((paramArrayList.size() > 1) && 
        (!"TABLE".equals(this.column.getTable().getTableType()))) {
        return 0;
      }
      return 1;
    case 1: 
    case 2: 
      return 2;
    case 3: 
    case 4: 
      return 4;
    case 11: 
      return 16;
    }
    throw DbException.throwInternalError("type=" + this.compareType);
  }
  
  public boolean isAlwaysFalse()
  {
    return this.compareType == 8;
  }
  
  public boolean isStart()
  {
    switch (this.compareType)
    {
    case 0: 
    case 1: 
    case 2: 
    case 16: 
      return true;
    }
    return false;
  }
  
  public boolean isEnd()
  {
    switch (this.compareType)
    {
    case 0: 
    case 3: 
    case 4: 
    case 16: 
      return true;
    }
    return false;
  }
  
  public boolean isSpatialIntersects()
  {
    switch (this.compareType)
    {
    case 11: 
      return true;
    }
    return false;
  }
  
  public int getCompareType()
  {
    return this.compareType;
  }
  
  public Column getColumn()
  {
    return this.column;
  }
  
  public boolean isEvaluatable()
  {
    if (this.expression != null) {
      return this.expression.isEverything(ExpressionVisitor.EVALUATABLE_VISITOR);
    }
    if (this.expressionList != null)
    {
      for (Expression localExpression : this.expressionList) {
        if (!localExpression.isEverything(ExpressionVisitor.EVALUATABLE_VISITOR)) {
          return false;
        }
      }
      return true;
    }
    return this.expressionQuery.isEverything(ExpressionVisitor.EVALUATABLE_VISITOR);
  }
}
