package org.h2.expression;

import org.h2.engine.Database;
import org.h2.message.DbException;
import org.h2.value.Value;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;

class AggregateDataCountAll
  extends AggregateData
{
  private long count;
  
  void add(Database paramDatabase, int paramInt, boolean paramBoolean, Value paramValue)
  {
    if (paramBoolean) {
      throw DbException.throwInternalError();
    }
    this.count += 1L;
  }
  
  Value getValue(Database paramDatabase, int paramInt, boolean paramBoolean)
  {
    if (paramBoolean) {
      throw DbException.throwInternalError();
    }
    ValueLong localValueLong = ValueLong.get(this.count);
    return localValueLong == null ? ValueNull.INSTANCE : localValueLong.convertTo(paramInt);
  }
}
