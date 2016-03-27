package org.h2.expression;

import org.h2.engine.Database;
import org.h2.message.DbException;
import org.h2.util.ValueHashMap;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueDouble;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;

class AggregateDataDefault
  extends AggregateData
{
  private final int aggregateType;
  private long count;
  private ValueHashMap<AggregateDataDefault> distinctValues;
  private Value value;
  private double m2;
  private double mean;
  
  AggregateDataDefault(int paramInt)
  {
    this.aggregateType = paramInt;
  }
  
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
    switch (this.aggregateType)
    {
    case 3: 
      if (this.value == null)
      {
        this.value = paramValue.convertTo(paramInt);
      }
      else
      {
        paramValue = paramValue.convertTo(this.value.getType());
        this.value = this.value.add(paramValue);
      }
      break;
    case 6: 
      if (this.value == null)
      {
        this.value = paramValue.convertTo(DataType.getAddProofType(paramInt));
      }
      else
      {
        paramValue = paramValue.convertTo(this.value.getType());
        this.value = this.value.add(paramValue);
      }
      break;
    case 4: 
      if ((this.value == null) || (paramDatabase.compare(paramValue, this.value) < 0)) {
        this.value = paramValue;
      }
      break;
    case 5: 
      if ((this.value == null) || (paramDatabase.compare(paramValue, this.value) > 0)) {
        this.value = paramValue;
      }
      break;
    case 7: 
    case 8: 
    case 9: 
    case 10: 
      double d1 = paramValue.getDouble();
      if (this.count == 1L)
      {
        this.mean = d1;
        this.m2 = 0.0D;
      }
      else
      {
        double d2 = d1 - this.mean;
        this.mean += d2 / this.count;
        this.m2 += d2 * (d1 - this.mean);
      }
      break;
    case 12: 
      paramValue = paramValue.convertTo(1);
      if (this.value == null) {
        this.value = paramValue;
      } else {
        this.value = ValueBoolean.get((this.value.getBoolean().booleanValue()) && (paramValue.getBoolean().booleanValue()));
      }
      break;
    case 11: 
      paramValue = paramValue.convertTo(1);
      if (this.value == null) {
        this.value = paramValue;
      } else {
        this.value = ValueBoolean.get((this.value.getBoolean().booleanValue()) || (paramValue.getBoolean().booleanValue()));
      }
      break;
    default: 
      DbException.throwInternalError("type=" + this.aggregateType);
    }
  }
  
  Value getValue(Database paramDatabase, int paramInt, boolean paramBoolean)
  {
    if (paramBoolean)
    {
      this.count = 0L;
      groupDistinct(paramDatabase, paramInt);
    }
    Object localObject = null;
    switch (this.aggregateType)
    {
    case 3: 
    case 4: 
    case 5: 
    case 11: 
    case 12: 
      localObject = this.value;
      break;
    case 6: 
      if (this.value != null) {
        localObject = divide(this.value, this.count);
      }
      break;
    case 7: 
      if (this.count < 1L) {
        return ValueNull.INSTANCE;
      }
      localObject = ValueDouble.get(Math.sqrt(this.m2 / this.count));
      break;
    case 8: 
      if (this.count < 2L) {
        return ValueNull.INSTANCE;
      }
      localObject = ValueDouble.get(Math.sqrt(this.m2 / (this.count - 1L)));
      break;
    case 9: 
      if (this.count < 1L) {
        return ValueNull.INSTANCE;
      }
      localObject = ValueDouble.get(this.m2 / this.count);
      break;
    case 10: 
      if (this.count < 2L) {
        return ValueNull.INSTANCE;
      }
      localObject = ValueDouble.get(this.m2 / (this.count - 1L));
      break;
    default: 
      DbException.throwInternalError("type=" + this.aggregateType);
    }
    return localObject == null ? ValueNull.INSTANCE : ((Value)localObject).convertTo(paramInt);
  }
  
  private static Value divide(Value paramValue, long paramLong)
  {
    if (paramLong == 0L) {
      return ValueNull.INSTANCE;
    }
    int i = Value.getHigherOrder(paramValue.getType(), 5);
    Value localValue = ValueLong.get(paramLong).convertTo(i);
    paramValue = paramValue.convertTo(i).divide(localValue);
    return paramValue;
  }
  
  private void groupDistinct(Database paramDatabase, int paramInt)
  {
    if (this.distinctValues == null) {
      return;
    }
    this.count = 0L;
    for (Value localValue : this.distinctValues.keys()) {
      add(paramDatabase, paramInt, false, localValue);
    }
  }
}
