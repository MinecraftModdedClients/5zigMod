package org.h2.table;

import java.util.ArrayList;
import java.util.HashMap;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionVisitor;
import org.h2.util.New;

public class Plan
{
  private final TableFilter[] filters;
  private final HashMap<TableFilter, PlanItem> planItems = New.hashMap();
  private final Expression[] allConditions;
  private final TableFilter[] allFilters;
  
  public Plan(TableFilter[] paramArrayOfTableFilter, int paramInt, Expression paramExpression)
  {
    this.filters = new TableFilter[paramInt];
    System.arraycopy(paramArrayOfTableFilter, 0, this.filters, 0, paramInt);
    final ArrayList localArrayList1 = New.arrayList();
    final ArrayList localArrayList2 = New.arrayList();
    if (paramExpression != null) {
      localArrayList1.add(paramExpression);
    }
    for (int i = 0; i < paramInt; i++)
    {
      TableFilter localTableFilter = paramArrayOfTableFilter[i];
      localTableFilter.visit(new TableFilter.TableFilterVisitor()
      {
        public void accept(TableFilter paramAnonymousTableFilter)
        {
          localArrayList2.add(paramAnonymousTableFilter);
          if (paramAnonymousTableFilter.getJoinCondition() != null) {
            localArrayList1.add(paramAnonymousTableFilter.getJoinCondition());
          }
        }
      });
    }
    this.allConditions = new Expression[localArrayList1.size()];
    localArrayList1.toArray(this.allConditions);
    this.allFilters = new TableFilter[localArrayList2.size()];
    localArrayList2.toArray(this.allFilters);
  }
  
  public PlanItem getItem(TableFilter paramTableFilter)
  {
    return (PlanItem)this.planItems.get(paramTableFilter);
  }
  
  public TableFilter[] getFilters()
  {
    return this.filters;
  }
  
  public void removeUnusableIndexConditions()
  {
    for (int i = 0; i < this.allFilters.length; i++)
    {
      TableFilter localTableFilter1 = this.allFilters[i];
      setEvaluatable(localTableFilter1, true);
      if ((i < this.allFilters.length - 1) || (localTableFilter1.getSession().getDatabase().getSettings().earlyFilter)) {
        localTableFilter1.optimizeFullCondition(false);
      }
      localTableFilter1.removeUnusableIndexConditions();
    }
    for (TableFilter localTableFilter2 : this.allFilters) {
      setEvaluatable(localTableFilter2, false);
    }
  }
  
  public double calculateCost(Session paramSession)
  {
    double d = 1.0D;
    int i = 0;
    int j = 1;
    TableFilter localTableFilter;
    for (localTableFilter : this.allFilters)
    {
      PlanItem localPlanItem = localTableFilter.getBestPlanItem(paramSession, j++);
      this.planItems.put(localTableFilter, localPlanItem);
      d += d * localPlanItem.cost;
      setEvaluatable(localTableFilter, true);
      Expression localExpression = localTableFilter.getJoinCondition();
      if ((localExpression != null) && 
        (!localExpression.isEverything(ExpressionVisitor.EVALUATABLE_VISITOR)))
      {
        i = 1;
        break;
      }
    }
    if (i != 0) {
      d = Double.POSITIVE_INFINITY;
    }
    for (localTableFilter : this.allFilters) {
      setEvaluatable(localTableFilter, false);
    }
    return d;
  }
  
  private void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    paramTableFilter.setEvaluatable(paramTableFilter, paramBoolean);
    for (Expression localExpression : this.allConditions) {
      localExpression.setEvaluatable(paramTableFilter, paramBoolean);
    }
  }
}
