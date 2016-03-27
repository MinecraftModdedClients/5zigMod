package org.h2.command.dml;

import org.h2.command.Prepared;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.result.ResultInterface;
import org.h2.result.Row;
import org.h2.result.RowList;
import org.h2.table.PlanItem;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.StringUtils;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class Delete
  extends Prepared
{
  private Expression condition;
  private TableFilter tableFilter;
  private Expression limitExpr;
  
  public Delete(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setTableFilter(TableFilter paramTableFilter)
  {
    this.tableFilter = paramTableFilter;
  }
  
  public void setCondition(Expression paramExpression)
  {
    this.condition = paramExpression;
  }
  
  public int update()
  {
    this.tableFilter.startQuery(this.session);
    this.tableFilter.reset();
    Table localTable = this.tableFilter.getTable();
    this.session.getUser().checkRight(localTable, 2);
    localTable.fire(this.session, 4, true);
    localTable.lock(this.session, true, false);
    RowList localRowList = new RowList(this.session);
    int i = -1;
    if (this.limitExpr != null)
    {
      Value localValue = this.limitExpr.getValue(this.session);
      if (localValue != ValueNull.INSTANCE) {
        i = localValue.getInt();
      }
    }
    try
    {
      setCurrentRowNumber(0);
      Row localRow1 = 0;
      while ((i != 0) && (this.tableFilter.next()))
      {
        setCurrentRowNumber(localRowList.size() + 1);
        if ((this.condition == null) || (Boolean.TRUE.equals(this.condition.getBooleanValue(this.session))))
        {
          Row localRow2 = this.tableFilter.get();
          boolean bool = false;
          if (localTable.fireRow()) {
            bool = localTable.fireBeforeRow(this.session, localRow2, null);
          }
          if (!bool) {
            localRowList.add(localRow2);
          }
          localRow1++;
          if ((i >= 0) && (localRow1 >= i)) {
            break;
          }
        }
      }
      int j = 0;
      for (localRowList.reset(); localRowList.hasNext();)
      {
        j++;
        if ((j & 0x7F) == 0) {
          checkCanceled();
        }
        localRow3 = localRowList.next();
        localTable.removeRow(this.session, localRow3);
        this.session.log(localTable, (short)1, localRow3);
      }
      Row localRow3;
      if (localTable.fireRow()) {
        for (localRowList.reset(); localRowList.hasNext();)
        {
          localRow3 = localRowList.next();
          localTable.fireAfterRow(this.session, localRow3, null, false);
        }
      }
      localTable.fire(this.session, 4, false);
      return localRow1;
    }
    finally
    {
      localRowList.close();
    }
  }
  
  public String getPlanSQL()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append("DELETE ");
    localStringBuilder.append("FROM ").append(this.tableFilter.getPlanSQL(false));
    if (this.condition != null) {
      localStringBuilder.append("\nWHERE ").append(StringUtils.unEnclose(this.condition.getSQL()));
    }
    if (this.limitExpr != null) {
      localStringBuilder.append("\nLIMIT (").append(StringUtils.unEnclose(this.limitExpr.getSQL())).append(')');
    }
    return localStringBuilder.toString();
  }
  
  public void prepare()
  {
    if (this.condition != null)
    {
      this.condition.mapColumns(this.tableFilter, 0);
      this.condition = this.condition.optimize(this.session);
      this.condition.createIndexConditions(this.session, this.tableFilter);
    }
    PlanItem localPlanItem = this.tableFilter.getBestPlanItem(this.session, 1);
    this.tableFilter.setPlanItem(localPlanItem);
    this.tableFilter.prepare();
  }
  
  public boolean isTransactional()
  {
    return true;
  }
  
  public ResultInterface queryMeta()
  {
    return null;
  }
  
  public int getType()
  {
    return 58;
  }
  
  public void setLimit(Expression paramExpression)
  {
    this.limitExpr = paramExpression;
  }
  
  public boolean isCacheable()
  {
    return true;
  }
}
