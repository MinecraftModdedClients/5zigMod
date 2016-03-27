package org.h2.expression;

import org.h2.engine.Database;
import org.h2.value.Value;

abstract class AggregateData
{
  static AggregateData create(int paramInt)
  {
    if (paramInt == 13) {
      return new AggregateDataSelectivity();
    }
    if (paramInt == 2) {
      return new AggregateDataGroupConcat();
    }
    if (paramInt == 0) {
      return new AggregateDataCountAll();
    }
    if (paramInt == 1) {
      return new AggregateDataCount();
    }
    if (paramInt == 14) {
      return new AggregateDataHistogram();
    }
    return new AggregateDataDefault(paramInt);
  }
  
  abstract void add(Database paramDatabase, int paramInt, boolean paramBoolean, Value paramValue);
  
  abstract Value getValue(Database paramDatabase, int paramInt, boolean paramBoolean);
}
