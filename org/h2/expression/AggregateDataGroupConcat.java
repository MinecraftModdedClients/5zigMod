package org.h2.expression;

import java.util.ArrayList;
import org.h2.engine.Database;
import org.h2.util.New;
import org.h2.util.ValueHashMap;
import org.h2.value.Value;
import org.h2.value.ValueNull;

class AggregateDataGroupConcat
  extends AggregateData
{
  private ArrayList<Value> list;
  private ValueHashMap<AggregateDataGroupConcat> distinctValues;
  
  void add(Database paramDatabase, int paramInt, boolean paramBoolean, Value paramValue)
  {
    if (paramValue == ValueNull.INSTANCE) {
      return;
    }
    if (paramBoolean)
    {
      if (this.distinctValues == null) {
        this.distinctValues = ValueHashMap.newInstance();
      }
      this.distinctValues.put(paramValue, this);
      return;
    }
    if (this.list == null) {
      this.list = New.arrayList();
    }
    this.list.add(paramValue);
  }
  
  Value getValue(Database paramDatabase, int paramInt, boolean paramBoolean)
  {
    if (paramBoolean) {
      groupDistinct(paramDatabase, paramInt);
    }
    return null;
  }
  
  ArrayList<Value> getList()
  {
    return this.list;
  }
  
  private void groupDistinct(Database paramDatabase, int paramInt)
  {
    if (this.distinctValues == null) {
      return;
    }
    for (Value localValue : this.distinctValues.keys()) {
      add(paramDatabase, paramInt, false, localValue);
    }
  }
}
