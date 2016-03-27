package org.h2.command.dml;

import java.util.Random;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.table.Plan;
import org.h2.table.PlanItem;
import org.h2.table.TableFilter;
import org.h2.util.BitField;
import org.h2.util.Permutations;

class Optimizer
{
  private static final int MAX_BRUTE_FORCE_FILTERS = 7;
  private static final int MAX_BRUTE_FORCE = 2000;
  private static final int MAX_GENETIC = 500;
  private long start;
  private BitField switched;
  private final TableFilter[] filters;
  private final Expression condition;
  private final Session session;
  private Plan bestPlan;
  private TableFilter topFilter;
  private double cost;
  private Random random;
  
  Optimizer(TableFilter[] paramArrayOfTableFilter, Expression paramExpression, Session paramSession)
  {
    this.filters = paramArrayOfTableFilter;
    this.condition = paramExpression;
    this.session = paramSession;
  }
  
  private static int getMaxBruteForceFilters(int paramInt)
  {
    int i = 0;int j = paramInt;int k = paramInt;
    while ((j > 0) && (k * (j * (j - 1) / 2) < 2000))
    {
      j--;
      k *= j;
      i++;
    }
    return i;
  }
  
  private void calculateBestPlan()
  {
    this.start = System.currentTimeMillis();
    this.cost = -1.0D;
    if (this.filters.length == 1)
    {
      testPlan(this.filters);
    }
    else if (this.filters.length <= 7)
    {
      calculateBruteForceAll();
    }
    else
    {
      calculateBruteForceSome();
      this.random = new Random(0L);
      calculateGenetic();
    }
  }
  
  private boolean canStop(int paramInt)
  {
    if ((paramInt & 0x7F) == 0)
    {
      long l = System.currentTimeMillis() - this.start;
      if ((this.cost >= 0.0D) && (10L * l > this.cost)) {
        return true;
      }
    }
    return false;
  }
  
  private void calculateBruteForceAll()
  {
    TableFilter[] arrayOfTableFilter = new TableFilter[this.filters.length];
    Permutations localPermutations = Permutations.create(this.filters, arrayOfTableFilter);
    for (int i = 0; (!canStop(i)) && (localPermutations.next()); i++) {
      testPlan(arrayOfTableFilter);
    }
  }
  
  private void calculateBruteForceSome()
  {
    int i = getMaxBruteForceFilters(this.filters.length);
    TableFilter[] arrayOfTableFilter1 = new TableFilter[this.filters.length];
    Permutations localPermutations = Permutations.create(this.filters, arrayOfTableFilter1, i);
    for (int j = 0; (!canStop(j)) && (localPermutations.next()); j++)
    {
      for (TableFilter localTableFilter : this.filters) {
        localTableFilter.setUsed(false);
      }
      for (int k = 0; k < i; k++) {
        arrayOfTableFilter1[k].setUsed(true);
      }
      for (k = i; k < this.filters.length; k++)
      {
        double d1 = -1.0D;
        int i1 = -1;
        for (int i2 = 0; i2 < this.filters.length; i2++) {
          if (!this.filters[i2].isUsed())
          {
            if (k == this.filters.length - 1)
            {
              i1 = i2;
              break;
            }
            arrayOfTableFilter1[k] = this.filters[i2];
            Plan localPlan = new Plan(arrayOfTableFilter1, k + 1, this.condition);
            double d2 = localPlan.calculateCost(this.session);
            if ((d1 < 0.0D) || (d2 < d1))
            {
              d1 = d2;
              i1 = i2;
            }
          }
        }
        this.filters[i1].setUsed(true);
        arrayOfTableFilter1[k] = this.filters[i1];
      }
      testPlan(arrayOfTableFilter1);
    }
  }
  
  private void calculateGenetic()
  {
    TableFilter[] arrayOfTableFilter1 = new TableFilter[this.filters.length];
    TableFilter[] arrayOfTableFilter2 = new TableFilter[this.filters.length];
    for (int i = 0; i < 500; i++)
    {
      if (canStop(i)) {
        break;
      }
      int j = (i & 0x7F) == 0 ? 1 : 0;
      if (j == 0)
      {
        System.arraycopy(arrayOfTableFilter1, 0, arrayOfTableFilter2, 0, this.filters.length);
        if (!shuffleTwo(arrayOfTableFilter2)) {
          j = 1;
        }
      }
      if (j != 0)
      {
        this.switched = new BitField();
        System.arraycopy(this.filters, 0, arrayOfTableFilter1, 0, this.filters.length);
        shuffleAll(arrayOfTableFilter1);
        System.arraycopy(arrayOfTableFilter1, 0, arrayOfTableFilter2, 0, this.filters.length);
      }
      if (testPlan(arrayOfTableFilter2))
      {
        this.switched = new BitField();
        System.arraycopy(arrayOfTableFilter2, 0, arrayOfTableFilter1, 0, this.filters.length);
      }
    }
  }
  
  private boolean testPlan(TableFilter[] paramArrayOfTableFilter)
  {
    Plan localPlan = new Plan(paramArrayOfTableFilter, paramArrayOfTableFilter.length, this.condition);
    double d = localPlan.calculateCost(this.session);
    if ((this.cost < 0.0D) || (d < this.cost))
    {
      this.cost = d;
      this.bestPlan = localPlan;
      return true;
    }
    return false;
  }
  
  private void shuffleAll(TableFilter[] paramArrayOfTableFilter)
  {
    for (int i = 0; i < paramArrayOfTableFilter.length - 1; i++)
    {
      int j = i + this.random.nextInt(paramArrayOfTableFilter.length - i);
      if (j != i)
      {
        TableFilter localTableFilter = paramArrayOfTableFilter[i];
        paramArrayOfTableFilter[i] = paramArrayOfTableFilter[j];
        paramArrayOfTableFilter[j] = localTableFilter;
      }
    }
  }
  
  private boolean shuffleTwo(TableFilter[] paramArrayOfTableFilter)
  {
    int i = 0;int j = 0;
    for (int k = 0; k < 20; k++)
    {
      i = this.random.nextInt(paramArrayOfTableFilter.length);
      j = this.random.nextInt(paramArrayOfTableFilter.length);
      if (i != j)
      {
        if (i < j)
        {
          m = i;
          i = j;
          j = m;
        }
        int m = i * paramArrayOfTableFilter.length + j;
        if (!this.switched.get(m))
        {
          this.switched.set(m);
          break;
        }
      }
    }
    if (k == 20) {
      return false;
    }
    TableFilter localTableFilter = paramArrayOfTableFilter[i];
    paramArrayOfTableFilter[i] = paramArrayOfTableFilter[j];
    paramArrayOfTableFilter[j] = localTableFilter;
    return true;
  }
  
  void optimize()
  {
    calculateBestPlan();
    this.bestPlan.removeUnusableIndexConditions();
    TableFilter[] arrayOfTableFilter1 = this.bestPlan.getFilters();
    this.topFilter = arrayOfTableFilter1[0];
    for (int i = 0; i < arrayOfTableFilter1.length - 1; i++) {
      arrayOfTableFilter1[i].addJoin(arrayOfTableFilter1[(i + 1)], false, false, null);
    }
    for (TableFilter localTableFilter : arrayOfTableFilter1)
    {
      PlanItem localPlanItem = this.bestPlan.getItem(localTableFilter);
      localTableFilter.setPlanItem(localPlanItem);
    }
  }
  
  public TableFilter getTopFilter()
  {
    return this.topFilter;
  }
  
  double getCost()
  {
    return this.cost;
  }
}
