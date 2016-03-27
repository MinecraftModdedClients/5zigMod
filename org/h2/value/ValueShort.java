package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.h2.message.DbException;
import org.h2.util.MathUtils;

public class ValueShort
  extends Value
{
  static final int PRECISION = 5;
  static final int DISPLAY_SIZE = 6;
  private final short value;
  
  private ValueShort(short paramShort)
  {
    this.value = paramShort;
  }
  
  public Value add(Value paramValue)
  {
    ValueShort localValueShort = (ValueShort)paramValue;
    return checkRange(this.value + localValueShort.value);
  }
  
  private static ValueShort checkRange(int paramInt)
  {
    if ((paramInt < 32768) || (paramInt > 32767)) {
      throw DbException.get(22003, Integer.toString(paramInt));
    }
    return get((short)paramInt);
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
    ValueShort localValueShort = (ValueShort)paramValue;
    return checkRange(this.value - localValueShort.value);
  }
  
  public Value multiply(Value paramValue)
  {
    ValueShort localValueShort = (ValueShort)paramValue;
    return checkRange(this.value * localValueShort.value);
  }
  
  public Value divide(Value paramValue)
  {
    ValueShort localValueShort = (ValueShort)paramValue;
    if (localValueShort.value == 0) {
      throw DbException.get(22012, getSQL());
    }
    return get((short)(this.value / localValueShort.value));
  }
  
  public Value modulus(Value paramValue)
  {
    ValueShort localValueShort = (ValueShort)paramValue;
    if (localValueShort.value == 0) {
      throw DbException.get(22012, getSQL());
    }
    return get((short)(this.value % localValueShort.value));
  }
  
  public String getSQL()
  {
    return getString();
  }
  
  public int getType()
  {
    return 3;
  }
  
  public short getShort()
  {
    return this.value;
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    ValueShort localValueShort = (ValueShort)paramValue;
    return MathUtils.compareInt(this.value, localValueShort.value);
  }
  
  public String getString()
  {
    return String.valueOf(this.value);
  }
  
  public long getPrecision()
  {
    return 5L;
  }
  
  public int hashCode()
  {
    return this.value;
  }
  
  public Object getObject()
  {
    return Short.valueOf(this.value);
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setShort(paramInt, this.value);
  }
  
  public static ValueShort get(short paramShort)
  {
    return (ValueShort)Value.cache(new ValueShort(paramShort));
  }
  
  public int getDisplaySize()
  {
    return 6;
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof ValueShort)) && (this.value == ((ValueShort)paramObject).value);
  }
}
