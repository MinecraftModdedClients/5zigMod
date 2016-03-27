package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.h2.message.DbException;

public class ValueFloat
  extends Value
{
  public static final int ZERO_BITS = Float.floatToIntBits(0.0F);
  static final int PRECISION = 7;
  static final int DISPLAY_SIZE = 15;
  private static final ValueFloat ZERO = new ValueFloat(0.0F);
  private static final ValueFloat ONE = new ValueFloat(1.0F);
  private final float value;
  
  private ValueFloat(float paramFloat)
  {
    this.value = paramFloat;
  }
  
  public Value add(Value paramValue)
  {
    ValueFloat localValueFloat = (ValueFloat)paramValue;
    return get(this.value + localValueFloat.value);
  }
  
  public Value subtract(Value paramValue)
  {
    ValueFloat localValueFloat = (ValueFloat)paramValue;
    return get(this.value - localValueFloat.value);
  }
  
  public Value negate()
  {
    return get(-this.value);
  }
  
  public Value multiply(Value paramValue)
  {
    ValueFloat localValueFloat = (ValueFloat)paramValue;
    return get(this.value * localValueFloat.value);
  }
  
  public Value divide(Value paramValue)
  {
    ValueFloat localValueFloat = (ValueFloat)paramValue;
    if (localValueFloat.value == 0.0D) {
      throw DbException.get(22012, getSQL());
    }
    return get(this.value / localValueFloat.value);
  }
  
  public Value modulus(Value paramValue)
  {
    ValueFloat localValueFloat = (ValueFloat)paramValue;
    if (localValueFloat.value == 0.0F) {
      throw DbException.get(22012, getSQL());
    }
    return get(this.value % localValueFloat.value);
  }
  
  public String getSQL()
  {
    if (this.value == Float.POSITIVE_INFINITY) {
      return "POWER(0, -1)";
    }
    if (this.value == Float.NEGATIVE_INFINITY) {
      return "(-POWER(0, -1))";
    }
    if (Double.isNaN(this.value)) {
      return "SQRT(-1)";
    }
    String str = getString();
    if (str.equals("-0.0")) {
      return "-CAST(0 AS REAL)";
    }
    return str;
  }
  
  public int getType()
  {
    return 8;
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    ValueFloat localValueFloat = (ValueFloat)paramValue;
    return Float.compare(this.value, localValueFloat.value);
  }
  
  public int getSignum()
  {
    return this.value < 0.0F ? -1 : this.value == 0.0F ? 0 : 1;
  }
  
  public float getFloat()
  {
    return this.value;
  }
  
  public String getString()
  {
    return String.valueOf(this.value);
  }
  
  public long getPrecision()
  {
    return 7L;
  }
  
  public int getScale()
  {
    return 0;
  }
  
  public int hashCode()
  {
    long l = Float.floatToIntBits(this.value);
    return (int)(l ^ l >> 32);
  }
  
  public Object getObject()
  {
    return Float.valueOf(this.value);
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setFloat(paramInt, this.value);
  }
  
  public static ValueFloat get(float paramFloat)
  {
    if (paramFloat == 1.0F) {
      return ONE;
    }
    if (paramFloat == 0.0F) {
      if (Float.floatToIntBits(paramFloat) == ZERO_BITS) {
        return ZERO;
      }
    }
    return (ValueFloat)Value.cache(new ValueFloat(paramFloat));
  }
  
  public int getDisplaySize()
  {
    return 15;
  }
  
  public boolean equals(Object paramObject)
  {
    if (!(paramObject instanceof ValueFloat)) {
      return false;
    }
    return compareSecure((ValueFloat)paramObject, null) == 0;
  }
}
