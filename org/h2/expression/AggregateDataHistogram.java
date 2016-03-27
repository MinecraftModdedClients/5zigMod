package org.h2.expression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import org.h2.engine.Database;
import org.h2.util.ValueHashMap;
import org.h2.value.CompareMode;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueLong;

class AggregateDataHistogram
  extends AggregateData
{
  private long count;
  private ValueHashMap<AggregateDataHistogram> distinctValues;
  
  void add(Database paramDatabase, int paramInt, boolean paramBoolean, Value paramValue)
  {
    if (this.distinctValues == null) {
      this.distinctValues = ValueHashMap.newInstance();
    }
    AggregateDataHistogram localAggregateDataHistogram = (AggregateDataHistogram)this.distinctValues.get(paramValue);
    if ((localAggregateDataHistogram == null) && 
      (this.distinctValues.size() < 10000))
    {
      localAggregateDataHistogram = new AggregateDataHistogram();
      this.distinctValues.put(paramValue, localAggregateDataHistogram);
    }
    if (localAggregateDataHistogram != null) {
      localAggregateDataHistogram.count += 1L;
    }
  }
  
  Value getValue(Database paramDatabase, int paramInt, boolean paramBoolean)
  {
    if (paramBoolean)
    {
      this.count = 0L;
      groupDistinct(paramDatabase, paramInt);
    }
    ValueArray[] arrayOfValueArray = new ValueArray[this.distinctValues.size()];
    int i = 0;
    for (final Object localObject1 = this.distinctValues.keys().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Value)((Iterator)localObject1).next();
      AggregateDataHistogram localAggregateDataHistogram = (AggregateDataHistogram)this.distinctValues.get((Value)localObject2);
      arrayOfValueArray[i] = ValueArray.get(new Value[] { localObject2, ValueLong.get(localAggregateDataHistogram.count) });
      i++;
    }
    localObject1 = paramDatabase.getCompareMode();
    Arrays.sort(arrayOfValueArray, new Comparator()
    {
      public int compare(ValueArray paramAnonymousValueArray1, ValueArray paramAnonymousValueArray2)
      {
        Value localValue1 = paramAnonymousValueArray1.getList()[0];
        Value localValue2 = paramAnonymousValueArray2.getList()[0];
        return localValue1.compareTo(localValue2, localObject1);
      }
    });
    Object localObject2 = ValueArray.get(arrayOfValueArray);
    return ((Value)localObject2).convertTo(paramInt);
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
