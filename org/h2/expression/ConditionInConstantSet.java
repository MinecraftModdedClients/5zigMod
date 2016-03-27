package org.h2.expression;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.index.IndexCondition;
import org.h2.message.DbException;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.StatementBuilder;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueNull;

public class ConditionInConstantSet
  extends Condition
{
  private Expression left;
  private int queryLevel;
  private final ArrayList<Expression> valueList;
  private final TreeSet<Value> valueSet;
  
  public ConditionInConstantSet(final Session paramSession, Expression paramExpression, ArrayList<Expression> paramArrayList)
  {
    this.left = paramExpression;
    this.valueList = paramArrayList;
    this.valueSet = new TreeSet(new Comparator()
    {
      public int compare(Value paramAnonymousValue1, Value paramAnonymousValue2)
      {
        return paramSession.getDatabase().compare(paramAnonymousValue1, paramAnonymousValue2);
      }
    });
    int i = paramExpression.getType();
    for (Expression localExpression : paramArrayList) {
      this.valueSet.add(localExpression.getValue(paramSession).convertTo(i));
    }
  }
  
  public Value getValue(Session paramSession)
  {
    Value localValue = this.left.getValue(paramSession);
    if (localValue == ValueNull.INSTANCE) {
      return localValue;
    }
    boolean bool1 = this.valueSet.contains(localValue);
    if (!bool1)
    {
      boolean bool2 = this.valueSet.contains(ValueNull.INSTANCE);
      if (bool2) {
        return ValueNull.INSTANCE;
      }
    }
    return ValueBoolean.get(bool1);
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    this.left.mapColumns(paramColumnResolver, paramInt);
    this.queryLevel = Math.max(paramInt, this.queryLevel);
  }
  
  public Expression optimize(Session paramSession)
  {
    this.left = this.left.optimize(paramSession);
    return this;
  }
  
  public void createIndexConditions(Session paramSession, TableFilter paramTableFilter)
  {
    if (!(this.left instanceof ExpressionColumn)) {
      return;
    }
    ExpressionColumn localExpressionColumn = (ExpressionColumn)this.left;
    if (paramTableFilter != localExpressionColumn.getTableFilter()) {
      return;
    }
    if (paramSession.getDatabase().getSettings().optimizeInList)
    {
      paramTableFilter.addIndexCondition(IndexCondition.getInList(localExpressionColumn, this.valueList));
      return;
    }
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.left.setEvaluatable(paramTableFilter, paramBoolean);
  }
  
  public String getSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("(");
    localStatementBuilder.append(this.left.getSQL()).append(" IN(");
    for (Expression localExpression : this.valueList)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(localExpression.getSQL());
    }
    return localStatementBuilder.append("))").toString();
  }
  
  public void updateAggregate(Session paramSession) {}
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    if (!this.left.isEverything(paramExpressionVisitor)) {
      return false;
    }
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
    int i = this.left.getCost();
    return i;
  }
  
  Expression getAdditional(Session paramSession, Comparison paramComparison)
  {
    Expression localExpression = paramComparison.getIfEquals(this.left);
    if ((localExpression != null) && 
      (localExpression.isConstant()))
    {
      this.valueList.add(localExpression);
      this.valueSet.add(localExpression.getValue(paramSession).convertTo(this.left.getType()));
      return this;
    }
    return null;
  }
}
