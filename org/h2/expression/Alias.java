package org.h2.expression;

import org.h2.command.Parser;
import org.h2.engine.Session;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.Value;

public class Alias
  extends Expression
{
  private final String alias;
  private Expression expr;
  private final boolean aliasColumnName;
  
  public Alias(Expression paramExpression, String paramString, boolean paramBoolean)
  {
    this.expr = paramExpression;
    this.alias = paramString;
    this.aliasColumnName = paramBoolean;
  }
  
  public Expression getNonAliasExpression()
  {
    return this.expr;
  }
  
  public Value getValue(Session paramSession)
  {
    return this.expr.getValue(paramSession);
  }
  
  public int getType()
  {
    return this.expr.getType();
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    this.expr.mapColumns(paramColumnResolver, paramInt);
  }
  
  public Expression optimize(Session paramSession)
  {
    this.expr = this.expr.optimize(paramSession);
    return this;
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.expr.setEvaluatable(paramTableFilter, paramBoolean);
  }
  
  public int getScale()
  {
    return this.expr.getScale();
  }
  
  public long getPrecision()
  {
    return this.expr.getPrecision();
  }
  
  public int getDisplaySize()
  {
    return this.expr.getDisplaySize();
  }
  
  public boolean isAutoIncrement()
  {
    return this.expr.isAutoIncrement();
  }
  
  public String getSQL()
  {
    return this.expr.getSQL() + " AS " + Parser.quoteIdentifier(this.alias);
  }
  
  public void updateAggregate(Session paramSession)
  {
    this.expr.updateAggregate(paramSession);
  }
  
  public String getAlias()
  {
    return this.alias;
  }
  
  public int getNullable()
  {
    return this.expr.getNullable();
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    return this.expr.isEverything(paramExpressionVisitor);
  }
  
  public int getCost()
  {
    return this.expr.getCost();
  }
  
  public String getTableName()
  {
    if (this.aliasColumnName) {
      return super.getTableName();
    }
    return this.expr.getTableName();
  }
  
  public String getColumnName()
  {
    if ((!(this.expr instanceof ExpressionColumn)) || (this.aliasColumnName)) {
      return super.getColumnName();
    }
    return this.expr.getColumnName();
  }
}
