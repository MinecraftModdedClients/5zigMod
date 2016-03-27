package org.h2.expression;

import org.h2.command.dml.Query;
import org.h2.engine.Session;
import org.h2.result.LocalResult;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.StringUtils;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;

public class ConditionExists
  extends Condition
{
  private final Query query;
  
  public ConditionExists(Query paramQuery)
  {
    this.query = paramQuery;
  }
  
  public Value getValue(Session paramSession)
  {
    this.query.setSession(paramSession);
    LocalResult localLocalResult = this.query.query(1);
    paramSession.addTemporaryResult(localLocalResult);
    boolean bool = localLocalResult.getRowCount() > 0;
    return ValueBoolean.get(bool);
  }
  
  public Expression optimize(Session paramSession)
  {
    this.query.prepare();
    return this;
  }
  
  public String getSQL()
  {
    return "EXISTS(\n" + StringUtils.indent(this.query.getPlanSQL(), 4, false) + ")";
  }
  
  public void updateAggregate(Session paramSession) {}
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    this.query.mapColumns(paramColumnResolver, paramInt + 1);
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.query.setEvaluatable(paramTableFilter, paramBoolean);
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    return this.query.isEverything(paramExpressionVisitor);
  }
  
  public int getCost()
  {
    return this.query.getCostAsExpression();
  }
}
