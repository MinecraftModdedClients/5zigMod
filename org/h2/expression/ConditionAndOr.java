package org.h2.expression;

import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueNull;

public class ConditionAndOr
  extends Condition
{
  public static final int AND = 0;
  public static final int OR = 1;
  private final int andOrType;
  private Expression left;
  private Expression right;
  
  public ConditionAndOr(int paramInt, Expression paramExpression1, Expression paramExpression2)
  {
    this.andOrType = paramInt;
    this.left = paramExpression1;
    this.right = paramExpression2;
    if ((SysProperties.CHECK) && ((paramExpression1 == null) || (paramExpression2 == null))) {
      DbException.throwInternalError();
    }
  }
  
  public String getSQL()
  {
    String str;
    switch (this.andOrType)
    {
    case 0: 
      str = this.left.getSQL() + "\n    AND " + this.right.getSQL();
      break;
    case 1: 
      str = this.left.getSQL() + "\n    OR " + this.right.getSQL();
      break;
    default: 
      throw DbException.throwInternalError("andOrType=" + this.andOrType);
    }
    return "(" + str + ")";
  }
  
  public void createIndexConditions(Session paramSession, TableFilter paramTableFilter)
  {
    if (this.andOrType == 0)
    {
      this.left.createIndexConditions(paramSession, paramTableFilter);
      this.right.createIndexConditions(paramSession, paramTableFilter);
    }
  }
  
  public Expression getNotIfPossible(Session paramSession)
  {
    Object localObject1 = this.left.getNotIfPossible(paramSession);
    if (localObject1 == null) {
      localObject1 = new ConditionNot(this.left);
    }
    Object localObject2 = this.right.getNotIfPossible(paramSession);
    if (localObject2 == null) {
      localObject2 = new ConditionNot(this.right);
    }
    int i = this.andOrType == 0 ? 1 : 0;
    return new ConditionAndOr(i, (Expression)localObject1, (Expression)localObject2);
  }
  
  public Value getValue(Session paramSession)
  {
    Value localValue1 = this.left.getValue(paramSession);
    Value localValue2;
    switch (this.andOrType)
    {
    case 0: 
      if (Boolean.FALSE.equals(localValue1.getBoolean())) {
        return localValue1;
      }
      localValue2 = this.right.getValue(paramSession);
      if (Boolean.FALSE.equals(localValue2.getBoolean())) {
        return localValue2;
      }
      if (localValue1 == ValueNull.INSTANCE) {
        return localValue1;
      }
      if (localValue2 == ValueNull.INSTANCE) {
        return localValue2;
      }
      return ValueBoolean.get(true);
    case 1: 
      if (Boolean.TRUE.equals(localValue1.getBoolean())) {
        return localValue1;
      }
      localValue2 = this.right.getValue(paramSession);
      if (Boolean.TRUE.equals(localValue2.getBoolean())) {
        return localValue2;
      }
      if (localValue1 == ValueNull.INSTANCE) {
        return localValue1;
      }
      if (localValue2 == ValueNull.INSTANCE) {
        return localValue2;
      }
      return ValueBoolean.get(false);
    }
    throw DbException.throwInternalError("type=" + this.andOrType);
  }
  
  public Expression optimize(Session paramSession)
  {
    this.left = this.left.optimize(paramSession);
    this.right = this.right.optimize(paramSession);
    int i = this.left.getCost();int j = this.right.getCost();
    if (j < i)
    {
      localObject = this.left;
      this.left = this.right;
      this.right = ((Expression)localObject);
    }
    Expression localExpression;
    if ((paramSession.getDatabase().getSettings().optimizeTwoEquals) && (this.andOrType == 0)) {
      if (((this.left instanceof Comparison)) && ((this.right instanceof Comparison)))
      {
        localObject = (Comparison)this.left;
        localComparison = (Comparison)this.right;
        localExpression = ((Comparison)localObject).getAdditional(paramSession, localComparison, true);
        if (localExpression != null)
        {
          localExpression = localExpression.optimize(paramSession);
          ConditionAndOr localConditionAndOr = new ConditionAndOr(0, this, localExpression);
          return localConditionAndOr;
        }
      }
    }
    if ((this.andOrType == 1) && (paramSession.getDatabase().getSettings().optimizeOr)) {
      if (((this.left instanceof Comparison)) && ((this.right instanceof Comparison)))
      {
        localObject = (Comparison)this.left;
        localComparison = (Comparison)this.right;
        localExpression = ((Comparison)localObject).getAdditional(paramSession, localComparison, false);
        if (localExpression != null) {
          return localExpression.optimize(paramSession);
        }
      }
      else if (((this.left instanceof ConditionIn)) && ((this.right instanceof Comparison)))
      {
        localObject = ((ConditionIn)this.left).getAdditional((Comparison)this.right);
        if (localObject != null) {
          return ((Expression)localObject).optimize(paramSession);
        }
      }
      else if (((this.right instanceof ConditionIn)) && ((this.left instanceof Comparison)))
      {
        localObject = ((ConditionIn)this.right).getAdditional((Comparison)this.left);
        if (localObject != null) {
          return ((Expression)localObject).optimize(paramSession);
        }
      }
      else if (((this.left instanceof ConditionInConstantSet)) && ((this.right instanceof Comparison)))
      {
        localObject = ((ConditionInConstantSet)this.left).getAdditional(paramSession, (Comparison)this.right);
        if (localObject != null) {
          return ((Expression)localObject).optimize(paramSession);
        }
      }
      else if (((this.right instanceof ConditionInConstantSet)) && ((this.left instanceof Comparison)))
      {
        localObject = ((ConditionInConstantSet)this.right).getAdditional(paramSession, (Comparison)this.left);
        if (localObject != null) {
          return ((Expression)localObject).optimize(paramSession);
        }
      }
    }
    Object localObject = this.left.isConstant() ? this.left.getValue(paramSession) : null;
    Comparison localComparison = this.right.isConstant() ? this.right.getValue(paramSession) : null;
    if ((localObject == null) && (localComparison == null)) {
      return this;
    }
    if ((localObject != null) && (localComparison != null)) {
      return ValueExpression.get(getValue(paramSession));
    }
    switch (this.andOrType)
    {
    case 0: 
      if (localObject != null)
      {
        if (Boolean.FALSE.equals(((Value)localObject).getBoolean())) {
          return ValueExpression.get((Value)localObject);
        }
        if (Boolean.TRUE.equals(((Value)localObject).getBoolean())) {
          return this.right;
        }
      }
      else if (localComparison != null)
      {
        if (Boolean.FALSE.equals(localComparison.getBoolean())) {
          return ValueExpression.get(localComparison);
        }
        if (Boolean.TRUE.equals(localComparison.getBoolean())) {
          return this.left;
        }
      }
      break;
    case 1: 
      if (localObject != null)
      {
        if (Boolean.TRUE.equals(((Value)localObject).getBoolean())) {
          return ValueExpression.get((Value)localObject);
        }
        if (Boolean.FALSE.equals(((Value)localObject).getBoolean())) {
          return this.right;
        }
      }
      else if (localComparison != null)
      {
        if (Boolean.TRUE.equals(localComparison.getBoolean())) {
          return ValueExpression.get(localComparison);
        }
        if (Boolean.FALSE.equals(localComparison.getBoolean())) {
          return this.left;
        }
      }
      break;
    default: 
      DbException.throwInternalError("type=" + this.andOrType);
    }
    return this;
  }
  
  public void addFilterConditions(TableFilter paramTableFilter, boolean paramBoolean)
  {
    if (this.andOrType == 0)
    {
      this.left.addFilterConditions(paramTableFilter, paramBoolean);
      this.right.addFilterConditions(paramTableFilter, paramBoolean);
    }
    else
    {
      super.addFilterConditions(paramTableFilter, paramBoolean);
    }
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    this.left.mapColumns(paramColumnResolver, paramInt);
    this.right.mapColumns(paramColumnResolver, paramInt);
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.left.setEvaluatable(paramTableFilter, paramBoolean);
    this.right.setEvaluatable(paramTableFilter, paramBoolean);
  }
  
  public void updateAggregate(Session paramSession)
  {
    this.left.updateAggregate(paramSession);
    this.right.updateAggregate(paramSession);
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    return (this.left.isEverything(paramExpressionVisitor)) && (this.right.isEverything(paramExpressionVisitor));
  }
  
  public int getCost()
  {
    return this.left.getCost() + this.right.getCost();
  }
  
  public Expression getExpression(boolean paramBoolean)
  {
    return paramBoolean ? this.left : this.right;
  }
}
