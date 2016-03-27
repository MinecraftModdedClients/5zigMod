package org.h2.expression;

import java.util.ArrayList;
import org.h2.command.dml.Query;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueNull;

public class Subquery
  extends Expression
{
  private final Query query;
  private Expression expression;
  
  public Subquery(Query paramQuery)
  {
    this.query = paramQuery;
  }
  
  public Value getValue(Session paramSession)
  {
    this.query.setSession(paramSession);
    LocalResult localLocalResult = this.query.query(2);
    try
    {
      int i = localLocalResult.getRowCount();
      if (i > 1) {
        throw DbException.get(90053);
      }
      Object localObject1;
      Object localObject2;
      if (i <= 0)
      {
        localObject1 = ValueNull.INSTANCE;
      }
      else
      {
        localLocalResult.next();
        localObject2 = localLocalResult.currentRow();
        if (localLocalResult.getVisibleColumnCount() == 1) {
          localObject1 = localObject2[0];
        } else {
          localObject1 = ValueArray.get((Value[])localObject2);
        }
      }
      return (Value)localObject1;
    }
    finally
    {
      localLocalResult.close();
    }
  }
  
  public int getType()
  {
    return getExpression().getType();
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    this.query.mapColumns(paramColumnResolver, paramInt + 1);
  }
  
  public Expression optimize(Session paramSession)
  {
    this.query.prepare();
    return this;
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.query.setEvaluatable(paramTableFilter, paramBoolean);
  }
  
  public int getScale()
  {
    return getExpression().getScale();
  }
  
  public long getPrecision()
  {
    return getExpression().getPrecision();
  }
  
  public int getDisplaySize()
  {
    return getExpression().getDisplaySize();
  }
  
  public String getSQL()
  {
    return "(" + this.query.getPlanSQL() + ")";
  }
  
  public void updateAggregate(Session paramSession)
  {
    this.query.updateAggregate(paramSession);
  }
  
  private Expression getExpression()
  {
    if (this.expression == null)
    {
      ArrayList localArrayList = this.query.getExpressions();
      int i = this.query.getColumnCount();
      if (i == 1)
      {
        this.expression = ((Expression)localArrayList.get(0));
      }
      else
      {
        Expression[] arrayOfExpression = new Expression[i];
        for (int j = 0; j < i; j++) {
          arrayOfExpression[j] = ((Expression)localArrayList.get(j));
        }
        this.expression = new ExpressionList(arrayOfExpression);
      }
    }
    return this.expression;
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    return this.query.isEverything(paramExpressionVisitor);
  }
  
  public Query getQuery()
  {
    return this.query;
  }
  
  public int getCost()
  {
    return this.query.getCostAsExpression();
  }
  
  public Expression[] getExpressionColumns(Session paramSession)
  {
    return getExpression().getExpressionColumns(paramSession);
  }
}
