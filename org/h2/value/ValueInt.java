package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.h2.message.DbException;
import org.h2.util.MathUtils;

public class ValueInt
  extends Value
{
  public static final int PRECISION = 10;
  public static final int DISPLAY_SIZE = 11;
  private static final int STATIC_SIZE = 128;
  private static final int DYNAMIC_SIZE = 256;
  private static final ValueInt[] STATIC_CACHE = new ValueInt[''];
  private static final ValueInt[] DYNAMIC_CACHE = new ValueInt['Ā'];
  private final int value;
  
  static
  {
    for (int i = 0; i < 128; i++) {
      STATIC_CACHE[i] = new ValueInt(i);
    }
  }
  
  private ValueInt(int paramInt)
  {
    this.value = paramInt;
  }
  
  public static ValueInt get(int paramInt)
  {
    if ((paramInt >= 0) && (paramInt < 128)) {
      return STATIC_CACHE[paramInt];
    }
    ValueInt localValueInt = DYNAMIC_CACHE[(paramInt & 0xFF)];
    if ((localValueInt == null) || (localValueInt.value != paramInt))
    {
      localValueInt = new ValueInt(paramInt);
      DYNAMIC_CACHE[(paramInt & 0xFF)] = localValueInt;
    }
    return localValueInt;
  }
  
  public Value add(Value paramValue)
  {
    ValueInt localValueInt = (ValueInt)paramValue;
    return checkRange(this.value + localValueInt.value);
  }
  
  private static ValueInt checkRange(long paramLong)
  {
    if ((paramLong < -2147483648L) || (paramLong > 2147483647L)) {
      throw DbException.get(22003, Long.toString(paramLong));
    }
    return get((int)paramLong);
  }
  
  public int getSignum()
  {
    return Integer.signum(this.value);
  }
  
  public Value negate()
  {
    return checkRange(-this.value);
  }
  
  public Value subtract(Value paramValue)
  {
    ValueInt localValueInt = (ValueInt)paramValue;
    return checkRange(this.value - localValueInt.value);
  }
  
  public Value multiply(Value paramValue)
  {
    ValueInt localValueInt = (ValueInt)paramValue;
    return checkRange(this.value * localValueInt.value);
  }
  
  public Value divide(Value paramValue)
  {
    ValueInt localValueInt = (ValueInt)paramValue;
    if (localValueInt.value == 0) {
      throw DbException.get(22012, getSQL());
    }
    return get(this.value / localValueInt.value);
  }
  
  public Value modulus(Value paramValue)
  {
    ValueInt localValueInt = (ValueInt)paramValue;
    if (localValueInt.value == 0) {
      throw DbException.get(22012, getSQL());
    }
    return get(this.value % localValueInt.value);
  }
  
  public String getSQL()
  {
    return getString();
  }
  
  public int getType()
  {
    return 4;
  }
  
  public int getInt()
  {
    return this.value;
  }
  
  public long getLong()
  {
    return this.value;
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    ValueInt localValueInt = (ValueInt)paramValue;
    return MathUtils.compareInt(this.value, localValueInt.value);
  }
  
  public String getString()
  {
    return String.valueOf(this.value);
  }
  
  public long getPrecision()
  {
    return 10L;
  }
  
  public int hashCode()
  {
    return this.value;
  }
  
  public Object getObject()
  {
    return Integer.valueOf(this.value);
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setInt(paramInt, this.value);
  }
  
  public int getDisplaySize()
  {
    return 11;
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof ValueInt)) && (this.value == ((ValueInt)paramObject).value);
  }
}
