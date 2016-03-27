package org.h2.expression;

import java.util.ArrayList;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.index.IndexCondition;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.StatementBuilder;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueNull;

public class ConditionIn
  extends Condition
{
  private final Database database;
  private Expression left;
  private final ArrayList<Expression> valueList;
  private int queryLevel;
  
  public ConditionIn(Database paramDatabase, Expression paramExpression, ArrayList<Expression> paramArrayList)
  {
    this.database = paramDatabase;
    this.left = paramExpression;
    this.valueList = paramArrayList;
  }
  
  public Value getValue(Session paramSession)
  {
    Value localValue1 = this.left.getValue(paramSession);
    if (localValue1 == ValueNull.INSTANCE) {
      return localValue1;
    }
    boolean bool = false;
    int i = 0;
    for (Expression localExpression : this.valueList)
    {
      Value localValue2 = localExpression.getValue(paramSession);
      if (localValue2 == ValueNull.INSTANCE)
      {
        i = 1;
      }
      else
      {
        localValue2 = localValue2.convertTo(localValue1.getType());
        bool = Comparison.compareNotNull(this.database, localValue1, localValue2, 0);
        if (bool) {
          break;
        }
      }
    }
    if ((!bool) && (i != 0)) {
      return ValueNull.INSTANCE;
    }
    return ValueBoolean.get(bool);
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    this.left.mapColumns(paramColumnResolver, paramInt);
    for (Expression localExpression : this.valueList) {
      localExpression.mapColumns(paramColumnResolver, paramInt);
    }
    this.queryLevel = Math.max(paramInt, this.queryLevel);
  }
  
  public Expression optimize(Session paramSession)
  {
    this.left = this.left.optimize(paramSession);
    boolean bool = this.left.isConstant();
    if ((bool) && (this.left == ValueExpression.getNull())) {
      return this.left;
    }
    int i = 1;
    int j = 1;
    int k = this.valueList.size();
    Object localObject;
    for (int m = 0; m < k; m++)
    {
      localObject = (Expression)this.valueList.get(m);
      localObject = ((Expression)localObject).optimize(paramSession);
      if ((((Expression)localObject).isConstant()) && (((Expression)localObject).getValue(paramSession) != ValueNull.INSTANCE)) {
        j = 0;
      }
      if ((i != 0) && (!((Expression)localObject).isConstant())) {
        i = 0;
      }
      this.valueList.set(m, localObject);
    }
    if ((bool) && (i != 0)) {
      return ValueExpression.get(getValue(paramSession));
    }
    if (k == 1)
    {
      Expression localExpression = (Expression)this.valueList.get(0);
      localObject = new Comparison(paramSession, 0, this.left, localExpression);
      localObject = ((Expression)localObject).optimize(paramSession);
      return (Expression)localObject;
    }
    if ((i != 0) && (j == 0))
    {
      int n = this.left.getType();
      if (n == -1) {
        return this;
      }
      localObject = new ConditionInConstantSet(paramSession, this.left, this.valueList);
      localObject = ((Expression)localObject).optimize(paramSession);
      return (Expression)localObject;
    }
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
      ExpressionVisitor localExpressionVisitor = ExpressionVisitor.getNotFromResolverVisitor(paramTableFilter);
      for (Expression localExpression : this.valueList) {
        if (!localExpression.isEverything(localExpressionVisitor)) {
          return;
        }
      }
      paramTableFilter.addIndexCondition(IndexCondition.getInList(localExpressionColumn, this.valueList));
      return;
    }
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.left.setEvaluatable(paramTableFilter, paramBoolean);
    for (Expression localExpression : this.valueList) {
      localExpression.setEvaluatable(paramTableFilter, paramBoolean);
    }
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
  
  public void updateAggregate(Session paramSession)
  {
    this.left.updateAggregate(paramSession);
    for (Expression localExpression : this.valueList) {
      localExpression.updateAggregate(paramSession);
    }
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    if (!this.left.isEverything(paramExpressionVisitor)) {
      return false;
    }
    return areAllValues(paramExpressionVisitor);
  }
  
  private boolean areAllValues(ExpressionVisitor paramExpressionVisitor)
  {
    for (Expression localExpression : this.valueList) {
      if (!localExpression.isEverything(paramExpressionVisitor)) {
        return false;
      }
    }
    return true;
  }
  
  public int getCost()
  {
    int i = this.left.getCost();
    for (Expression localExpression : this.valueList) {
      i += localExpression.getCost();
    }
    return i;
  }
  
  Expression getAdditional(Comparison paramComparison)
  {
    Expression localExpression = paramComparison.getIfEquals(this.left);
    if (localExpression != null)
    {
      this.valueList.add(localExpression);
      return this;
    }
    return null;
  }
}
