package org.h2.table;

import org.h2.index.Index;

public class PlanItem
{
  double cost;
  private Index index;
  private PlanItem joinPlan;
  private PlanItem nestedJoinPlan;
  
  void setIndex(Index paramIndex)
  {
    this.index = paramIndex;
  }
  
  public Index getIndex()
  {
    return this.index;
  }
  
  PlanItem getJoinPlan()
  {
    return this.joinPlan;
  }
  
  PlanItem getNestedJoinPlan()
  {
    return this.nestedJoinPlan;
  }
  
  void setJoinPlan(PlanItem paramPlanItem)
  {
    this.joinPlan = paramPlanItem;
  }
  
  void setNestedJoinPlan(PlanItem paramPlanItem)
  {
    this.nestedJoinPlan = paramPlanItem;
  }
}
