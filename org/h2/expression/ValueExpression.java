package org.h2.expression;

import org.h2.engine.Session;
import org.h2.index.IndexCondition;
import org.h2.message.DbException;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueNull;

public class ValueExpression
  extends Expression
{
  private static final Object NULL = new ValueExpression(ValueNull.INSTANCE);
  private static final Object DEFAULT = new ValueExpression(ValueNull.INSTANCE);
  private final Value value;
  
  private ValueExpression(Value paramValue)
  {
    this.value = paramValue;
  }
  
  public static ValueExpression getNull()
  {
    return (ValueExpression)NULL;
  }
  
  public static ValueExpression getDefault()
  {
    return (ValueExpression)DEFAULT;
  }
  
  public static ValueExpression get(Value paramValue)
  {
    if (paramValue == ValueNull.INSTANCE) {
      return getNull();
    }
    return new ValueExpression(paramValue);
  }
  
  public Value getValue(Session paramSession)
  {
    return this.value;
  }
  
  public int getType()
  {
    return this.value.getType();
  }
  
  public void createIndexConditions(Session paramSession, TableFilter paramTableFilter)
  {
    if (this.value.getType() == 1)
    {
      boolean bool = ((ValueBoolean)this.value).getBoolean().booleanValue();
      if (!bool) {
        paramTableFilter.addIndexCondition(IndexCondition.get(8, null, this));
      }
    }
  }
  
  public Expression getNotIfPossible(Session paramSession)
  {
    return new Comparison(paramSession, 0, this, get(ValueBoolean.get(false)));
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt) {}
  
  public Expression optimize(Session paramSession)
  {
    return this;
  }
  
  public boolean isConstant()
  {
    return true;
  }
  
  public boolean isValueSet()
  {
    return true;
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean) {}
  
  public int getScale()
  {
    return this.value.getScale();
  }
  
  public long getPrecision()
  {
    return this.value.getPrecision();
  }
  
  public int getDisplaySize()
  {
    return this.value.getDisplaySize();
  }
  
  public String getSQL()
  {
    if (this == DEFAULT) {
      return "DEFAULT";
    }
    return this.value.getSQL();
  }
  
  public void updateAggregate(Session paramSession) {}
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    switch (paramExpressionVisitor.getType())
    {
    case 0: 
    case 1: 
    case 2: 
    case 3: 
    case 4: 
    case 5: 
    case 6: 
    case 7: 
    case 8: 
    case 9: 
      return true;
    }
    throw DbException.throwInternalError("type=" + paramExpressionVisitor.getType());
  }
  
  public int getCost()
  {
    return 0;
  }
  
  public Expression[] getExpressionColumns(Session paramSession)
  {
    if (getType() == 17) {
      return getExpressionColumns(paramSession, (ValueArray)getValue(paramSession));
    }
    return super.getExpressionColumns(paramSession);
  }
}
