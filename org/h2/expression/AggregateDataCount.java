package org.h2.expression;

import org.h2.engine.Database;
import org.h2.util.ValueHashMap;
import org.h2.value.Value;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;

class AggregateDataCount
  extends AggregateData
{
  private long count;
  private ValueHashMap<AggregateDataCount> distinctValues;
  
  void add(Database paramDatabase, int paramInt, boolean paramBoolean, Value paramValue)
  {
    if (paramValue == ValueNull.INSTANCE) {
      return;
    }
    this.count += 1L;
    if (paramBoolean)
    {
      if (this.distinctValues == null) {
        this.distinctValues = ValueHashMap.newInstance();
      }
      this.distinctValues.put(paramValue, this);
      return;
    }
  }
  
  Value getValue(Database paramDatabase, int paramInt, boolean paramBoolean)
  {
    if (paramBoolean) {
      if (this.distinctValues != null) {
        this.count = this.distinctValues.size();
      } else {
        this.count = 0L;
      }
    }
    ValueLong localValueLong = ValueLong.get(this.count);
    return localValueLong.convertTo(paramInt);
  }
}
