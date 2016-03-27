package org.h2.value;

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.util.StatementBuilder;

public class ValueArray
  extends Value
{
  private final Class<?> componentType;
  private final Value[] values;
  private int hash;
  
  private ValueArray(Class<?> paramClass, Value[] paramArrayOfValue)
  {
    this.componentType = paramClass;
    this.values = paramArrayOfValue;
  }
  
  private ValueArray(Value[] paramArrayOfValue)
  {
    this(Object.class, paramArrayOfValue);
  }
  
  public static ValueArray get(Value[] paramArrayOfValue)
  {
    return new ValueArray(paramArrayOfValue);
  }
  
  public static ValueArray get(Class<?> paramClass, Value[] paramArrayOfValue)
  {
    return new ValueArray(paramClass, paramArrayOfValue);
  }
  
  public int hashCode()
  {
    if (this.hash != 0) {
      return this.hash;
    }
    int i = 1;
    for (Value localValue : this.values) {
      i = i * 31 + localValue.hashCode();
    }
    this.hash = i;
    return i;
  }
  
  public Value[] getList()
  {
    return this.values;
  }
  
  public int getType()
  {
    return 17;
  }
  
  public Class<?> getComponentType()
  {
    return this.componentType;
  }
  
  public long getPrecision()
  {
    long l = 0L;
    for (Value localValue : this.values) {
      l += localValue.getPrecision();
    }
    return l;
  }
  
  public String getString()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("(");
    for (Value localValue : this.values)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(localValue.getString());
    }
    return localStatementBuilder.append(')').toString();
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    ValueArray localValueArray = (ValueArray)paramValue;
    if (this.values == localValueArray.values) {
      return 0;
    }
    int i = this.values.length;
    int j = localValueArray.values.length;
    int k = Math.min(i, j);
    for (int m = 0; m < k; m++)
    {
      Value localValue1 = this.values[m];
      Value localValue2 = localValueArray.values[m];
      int n = localValue1.compareTo(localValue2, paramCompareMode);
      if (n != 0) {
        return n;
      }
    }
    return i == j ? 0 : i > j ? 1 : -1;
  }
  
  public Object getObject()
  {
    int i = this.values.length;
    Object[] arrayOfObject = (Object[])Array.newInstance(this.componentType, i);
    for (int j = 0; j < i; j++) {
      arrayOfObject[j] = this.values[j].getObject();
    }
    return arrayOfObject;
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
  {
    throw throwUnsupportedExceptionForType("PreparedStatement.set");
  }
  
  public String getSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("(");
    for (Value localValue : this.values)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(localValue.getSQL());
    }
    if (this.values.length == 1) {
      localStatementBuilder.append(',');
    }
    return localStatementBuilder.append(')').toString();
  }
  
  public String getTraceSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("(");
    for (Value localValue : this.values)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(localValue == null ? "null" : localValue.getTraceSQL());
    }
    return localStatementBuilder.append(')').toString();
  }
  
  public int getDisplaySize()
  {
    long l = 0L;
    for (Value localValue : this.values) {
      l += localValue.getDisplaySize();
    }
    return MathUtils.convertLongToInt(l);
  }
  
  public boolean equals(Object paramObject)
  {
    if (!(paramObject instanceof ValueArray)) {
      return false;
    }
    ValueArray localValueArray = (ValueArray)paramObject;
    if (this.values == localValueArray.values) {
      return true;
    }
    int i = this.values.length;
    if (i != localValueArray.values.length) {
      return false;
    }
    for (int j = 0; j < i; j++) {
      if (!this.values[j].equals(localValueArray.values[j])) {
        return false;
      }
    }
    return true;
  }
  
  public int getMemory()
  {
    int i = 32;
    for (Value localValue : this.values) {
      i += localValue.getMemory() + 8;
    }
    return i;
  }
  
  public Value convertPrecision(long paramLong, boolean paramBoolean)
  {
    if (!paramBoolean) {
      return this;
    }
    ArrayList localArrayList = New.arrayList();
    for (Value localValue : this.values)
    {
      localValue = localValue.convertPrecision(paramLong, true);
      
      paramLong -= Math.max(1L, localValue.getPrecision());
      if (paramLong < 0L) {
        break;
      }
      localArrayList.add(localValue);
    }
    ??? = new Value[localArrayList.size()];
    localArrayList.toArray(???);
    return get(???);
  }
}
