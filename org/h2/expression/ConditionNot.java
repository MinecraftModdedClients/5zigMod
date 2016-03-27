package org.h2.expression;

import org.h2.engine.Session;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class ConditionNot
  extends Condition
{
  private Expression condition;
  
  public ConditionNot(Expression paramExpression)
  {
    this.condition = paramExpression;
  }
  
  public Expression getNotIfPossible(Session paramSession)
  {
    return this.condition;
  }
  
  public Value getValue(Session paramSession)
  {
    Value localValue = this.condition.getValue(paramSession);
    if (localValue == ValueNull.INSTANCE) {
      return localValue;
    }
    return localValue.convertTo(1).negate();
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    this.condition.mapColumns(paramColumnResolver, paramInt);
  }
  
  public Expression optimize(Session paramSession)
  {
    Expression localExpression1 = this.condition.getNotIfPossible(paramSession);
    if (localExpression1 != null) {
      return localExpression1.optimize(paramSession);
    }
    Expression localExpression2 = this.condition.optimize(paramSession);
    if (localExpression2.isConstant())
    {
      Value localValue = localExpression2.getValue(paramSession);
      if (localValue == ValueNull.INSTANCE) {
        return ValueExpression.getNull();
      }
      return ValueExpression.get(localValue.convertTo(1).negate());
    }
    this.condition = localExpression2;
    return this;
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.condition.setEvaluatable(paramTableFilter, paramBoolean);
  }
  
  public String getSQL()
  {
    return "(NOT " + this.condition.getSQL() + ")";
  }
  
  public void updateAggregate(Session paramSession)
  {
    this.condition.updateAggregate(paramSession);
  }
  
  public void addFilterConditions(TableFilter paramTableFilter, boolean paramBoolean)
  {
    if (paramBoolean) {
      return;
    }
    super.addFilterConditions(paramTableFilter, paramBoolean);
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    return this.condition.isEverything(paramExpressionVisitor);
  }
  
  public int getCost()
  {
    return this.condition.getCost();
  }
}
