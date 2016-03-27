package org.h2.expression;

import org.h2.engine.Database;
import org.h2.util.IntIntHashMap;
import org.h2.value.Value;
import org.h2.value.ValueInt;

class AggregateDataSelectivity
  extends AggregateData
{
  private long count;
  private IntIntHashMap distinctHashes;
  private double m2;
  
  void add(Database paramDatabase, int paramInt, boolean paramBoolean, Value paramValue)
  {
    this.count += 1L;
    if (this.distinctHashes == null) {
      this.distinctHashes = new IntIntHashMap();
    }
    int i = this.distinctHashes.size();
    if (i > 10000)
    {
      this.distinctHashes = new IntIntHashMap();
      this.m2 += i;
    }
    int j = paramValue.hashCode();
    
    this.distinctHashes.put(j, 1);
  }
  
  Value getValue(Database paramDatabase, int paramInt, boolean paramBoolean)
  {
    if (paramBoolean) {
      this.count = 0L;
    }
    ValueInt localValueInt = null;
    int i = 0;
    if (this.count == 0L)
    {
      i = 0;
    }
    else
    {
      this.m2 += this.distinctHashes.size();
      this.m2 = (100.0D * this.m2 / this.count);
      i = (int)this.m2;
      i = i > 100 ? 100 : i <= 0 ? 1 : i;
    }
    localValueInt = ValueInt.get(i);
    return localValueInt.convertTo(paramInt);
  }
}
