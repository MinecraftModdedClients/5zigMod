package org.h2.expression;

import org.h2.command.dml.Query;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.index.IndexCondition;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.StringUtils;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueNull;

public class ConditionInSelect
  extends Condition
{
  private final Database database;
  private Expression left;
  private final Query query;
  private final boolean all;
  private final int compareType;
  private int queryLevel;
  
  public ConditionInSelect(Database paramDatabase, Expression paramExpression, Query paramQuery, boolean paramBoolean, int paramInt)
  {
    this.database = paramDatabase;
    this.left = paramExpression;
    this.query = paramQuery;
    this.all = paramBoolean;
    this.compareType = paramInt;
  }
  
  public Value getValue(Session paramSession)
  {
    this.query.setSession(paramSession);
    this.query.setDistinct(true);
    LocalResult localLocalResult = this.query.query(0);
    try
    {
      Value localValue = this.left.getValue(paramSession);
      Object localObject1;
      if (localLocalResult.getRowCount() == 0) {
        return ValueBoolean.get(this.all);
      }
      if (localValue == ValueNull.INSTANCE) {
        return localValue;
      }
      if (!paramSession.getDatabase().getSettings().optimizeInSelect) {
        return getValueSlow(localLocalResult, localValue);
      }
      if ((this.all) || ((this.compareType != 0) && (this.compareType != 16))) {
        return getValueSlow(localLocalResult, localValue);
      }
      int i = localLocalResult.getColumnType(0);
      Object localObject2;
      if (i == 0) {
        return ValueBoolean.get(false);
      }
      localValue = localValue.convertTo(i);
      if (localLocalResult.containsDistinct(new Value[] { localValue })) {
        return ValueBoolean.get(true);
      }
      if (localLocalResult.containsDistinct(new Value[] { ValueNull.INSTANCE })) {
        return ValueNull.INSTANCE;
      }
      return ValueBoolean.get(false);
    }
    finally
    {
      localLocalResult.close();
    }
  }
  
  private Value getValueSlow(LocalResult paramLocalResult, Value paramValue)
  {
    int i = 0;
    boolean bool1 = this.all;
    while (paramLocalResult.next())
    {
      Value localValue = paramLocalResult.currentRow()[0];
      boolean bool2;
      if (localValue == ValueNull.INSTANCE)
      {
        bool2 = false;
        i = 1;
      }
      else
      {
        bool2 = Comparison.compareNotNull(this.database, paramValue, localValue, this.compareType);
      }
      if ((!bool2) && (this.all))
      {
        bool1 = false;
        break;
      }
      if ((bool2) && (!this.all))
      {
        bool1 = true;
        break;
      }
    }
    if ((!bool1) && (i != 0)) {
      return ValueNull.INSTANCE;
    }
    return ValueBoolean.get(bool1);
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    this.left.mapColumns(paramColumnResolver, paramInt);
    this.query.mapColumns(paramColumnResolver, paramInt + 1);
    this.queryLevel = Math.max(paramInt, this.queryLevel);
  }
  
  public Expression optimize(Session paramSession)
  {
    this.left = this.left.optimize(paramSession);
    this.query.setRandomAccessResult(true);
    this.query.prepare();
    if (this.query.getColumnCount() != 1) {
      throw DbException.get(90052);
    }
    return this;
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.left.setEvaluatable(paramTableFilter, paramBoolean);
    this.query.setEvaluatable(paramTableFilter, paramBoolean);
  }
  
  public String getSQL()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append('(').append(this.left.getSQL()).append(' ');
    if (this.all) {
      localStringBuilder.append(Comparison.getCompareOperator(this.compareType)).append(" ALL");
    } else if (this.compareType == 0) {
      localStringBuilder.append("IN");
    } else {
      localStringBuilder.append(Comparison.getCompareOperator(this.compareType)).append(" ANY");
    }
    localStringBuilder.append("(\n").append(StringUtils.indent(this.query.getPlanSQL(), 4, false)).append("))");
    
    return localStringBuilder.toString();
  }
  
  public void updateAggregate(Session paramSession)
  {
    this.left.updateAggregate(paramSession);
    this.query.updateAggregate(paramSession);
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    return (this.left.isEverything(paramExpressionVisitor)) && (this.query.isEverything(paramExpressionVisitor));
  }
  
  public int getCost()
  {
    return this.left.getCost() + this.query.getCostAsExpression();
  }
  
  public void createIndexConditions(Session paramSession, TableFilter paramTableFilter)
  {
    if (!paramSession.getDatabase().getSettings().optimizeInList) {
      return;
    }
    if (!(this.left instanceof ExpressionColumn)) {
      return;
    }
    ExpressionColumn localExpressionColumn = (ExpressionColumn)this.left;
    if (paramTableFilter != localExpressionColumn.getTableFilter()) {
      return;
    }
    ExpressionVisitor localExpressionVisitor = ExpressionVisitor.getNotFromResolverVisitor(paramTableFilter);
    if (!this.query.isEverything(localExpressionVisitor)) {
      return;
    }
    paramTableFilter.addIndexCondition(IndexCondition.getInQuery(localExpressionColumn, this.query));
  }
}
