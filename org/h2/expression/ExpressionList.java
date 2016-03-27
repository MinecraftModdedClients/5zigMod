package org.h2.expression;

import org.h2.engine.Session;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.StatementBuilder;
import org.h2.value.Value;
import org.h2.value.ValueArray;

public class ExpressionList
  extends Expression
{
  private final Expression[] list;
  
  public ExpressionList(Expression[] paramArrayOfExpression)
  {
    this.list = paramArrayOfExpression;
  }
  
  public Value getValue(Session paramSession)
  {
    Value[] arrayOfValue = new Value[this.list.length];
    for (int i = 0; i < this.list.length; i++) {
      arrayOfValue[i] = this.list[i].getValue(paramSession);
    }
    return ValueArray.get(arrayOfValue);
  }
  
  public int getType()
  {
    return 17;
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    for (Expression localExpression : this.list) {
      localExpression.mapColumns(paramColumnResolver, paramInt);
    }
  }
  
  public Expression optimize(Session paramSession)
  {
    int i = 1;
    for (int j = 0; j < this.list.length; j++)
    {
      Expression localExpression = this.list[j].optimize(paramSession);
      if (!localExpression.isConstant()) {
        i = 0;
      }
      this.list[j] = localExpression;
    }
    if (i != 0) {
      return ValueExpression.get(getValue(paramSession));
    }
    return this;
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    for (Expression localExpression : this.list) {
      localExpression.setEvaluatable(paramTableFilter, paramBoolean);
    }
  }
  
  public int getScale()
  {
    return 0;
  }
  
  public long getPrecision()
  {
    return 2147483647L;
  }
  
  public int getDisplaySize()
  {
    return Integer.MAX_VALUE;
  }
  
  public String getSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("(");
    for (Expression localExpression : this.list)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(localExpression.getSQL());
    }
    if (this.list.length == 1) {
      localStatementBuilder.append(',');
    }
    return localStatementBuilder.append(')').toString();
  }
  
  public void updateAggregate(Session paramSession)
  {
    for (Expression localExpression : this.list) {
      localExpression.updateAggregate(paramSession);
    }
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    for (Expression localExpression : this.list) {
      if (!localExpression.isEverything(paramExpressionVisitor)) {
        return false;
      }
    }
    return true;
  }
  
  public int getCost()
  {
    int i = 1;
    for (Expression localExpression : this.list) {
      i += localExpression.getCost();
    }
    return i;
  }
  
  public Expression[] getExpressionColumns(Session paramSession)
  {
    ExpressionColumn[] arrayOfExpressionColumn = new ExpressionColumn[this.list.length];
    for (int i = 0; i < this.list.length; i++)
    {
      Expression localExpression = this.list[i];
      Column localColumn = new Column("C" + (i + 1), localExpression.getType(), localExpression.getPrecision(), localExpression.getScale(), localExpression.getDisplaySize());
      
      arrayOfExpressionColumn[i] = new ExpressionColumn(paramSession.getDatabase(), localColumn);
    }
    return arrayOfExpressionColumn;
  }
}
