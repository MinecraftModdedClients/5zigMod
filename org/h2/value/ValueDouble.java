package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.h2.message.DbException;

public class ValueDouble
  extends Value
{
  public static final int PRECISION = 17;
  public static final int DISPLAY_SIZE = 24;
  public static final long ZERO_BITS = Double.doubleToLongBits(0.0D);
  private static final ValueDouble ZERO = new ValueDouble(0.0D);
  private static final ValueDouble ONE = new ValueDouble(1.0D);
  private static final ValueDouble NAN = new ValueDouble(NaN.0D);
  private final double value;
  
  private ValueDouble(double paramDouble)
  {
    this.value = paramDouble;
  }
  
  public Value add(Value paramValue)
  {
    ValueDouble localValueDouble = (ValueDouble)paramValue;
    return get(this.value + localValueDouble.value);
  }
  
  public Value subtract(Value paramValue)
  {
    ValueDouble localValueDouble = (ValueDouble)paramValue;
    return get(this.value - localValueDouble.value);
  }
  
  public Value negate()
  {
    return get(-this.value);
  }
  
  public Value multiply(Value paramValue)
  {
    ValueDouble localValueDouble = (ValueDouble)paramValue;
    return get(this.value * localValueDouble.value);
  }
  
  public Value divide(Value paramValue)
  {
    ValueDouble localValueDouble = (ValueDouble)paramValue;
    if (localValueDouble.value == 0.0D) {
      throw DbException.get(22012, getSQL());
    }
    return get(this.value / localValueDouble.value);
  }
  
  public ValueDouble modulus(Value paramValue)
  {
    ValueDouble localValueDouble = (ValueDouble)paramValue;
    if (localValueDouble.value == 0.0D) {
      throw DbException.get(22012, getSQL());
    }
    return get(this.value % localValueDouble.value);
  }
  
  public String getSQL()
  {
    if (this.value == Double.POSITIVE_INFINITY) {
      return "POWER(0, -1)";
    }
    if (this.value == Double.NEGATIVE_INFINITY) {
      return "(-POWER(0, -1))";
    }
    if (Double.isNaN(this.value)) {
      return "SQRT(-1)";
    }
    String str = getString();
    if (str.equals("-0.0")) {
      return "-CAST(0 AS DOUBLE)";
    }
    return str;
  }
  
  public int getType()
  {
    return 7;
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    ValueDouble localValueDouble = (ValueDouble)paramValue;
    return Double.compare(this.value, localValueDouble.value);
  }
  
  public int getSignum()
  {
    return this.value < 0.0D ? -1 : this.value == 0.0D ? 0 : 1;
  }
  
  public double getDouble()
  {
    return this.value;
  }
  
  public String getString()
  {
    return String.valueOf(this.value);
  }
  
  public long getPrecision()
  {
    return 17L;
  }
  
  public int getScale()
  {
    return 0;
  }
  
  public int hashCode()
  {
    long l = Double.doubleToLongBits(this.value);
    return (int)(l ^ l >> 32);
  }
  
  public Object getObject()
  {
    return Double.valueOf(this.value);
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setDouble(paramInt, this.value);
  }
  
  public static ValueDouble get(double paramDouble)
  {
    if (paramDouble == 1.0D) {
      return ONE;
    }
    if (paramDouble == 0.0D)
    {
      if (Double.doubleToLongBits(paramDouble) == ZERO_BITS) {
        return ZERO;
      }
    }
    else if (Double.isNaN(paramDouble)) {
      return NAN;
    }
    return (ValueDouble)Value.cache(new ValueDouble(paramDouble));
  }
  
  public int getDisplaySize()
  {
    return 24;
  }
  
  public boolean equals(Object paramObject)
  {
    if (!(paramObject instanceof ValueDouble)) {
      return false;
    }
    return compareSecure((ValueDouble)paramObject, null) == 0;
  }
}
