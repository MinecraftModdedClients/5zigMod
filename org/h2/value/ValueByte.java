package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.h2.message.DbException;
import org.h2.util.MathUtils;

public class ValueByte
  extends Value
{
  static final int PRECISION = 3;
  static final int DISPLAY_SIZE = 4;
  private final byte value;
  
  private ValueByte(byte paramByte)
  {
    this.value = paramByte;
  }
  
  public Value add(Value paramValue)
  {
    ValueByte localValueByte = (ValueByte)paramValue;
    return checkRange(this.value + localValueByte.value);
  }
  
  private static ValueByte checkRange(int paramInt)
  {
    if ((paramInt < -128) || (paramInt > 127)) {
      throw DbException.get(22003, Integer.toString(paramInt));
    }
    return get((byte)paramInt);
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
    ValueByte localValueByte = (ValueByte)paramValue;
    return checkRange(this.value - localValueByte.value);
  }
  
  public Value multiply(Value paramValue)
  {
    ValueByte localValueByte = (ValueByte)paramValue;
    return checkRange(this.value * localValueByte.value);
  }
  
  public Value divide(Value paramValue)
  {
    ValueByte localValueByte = (ValueByte)paramValue;
    if (localValueByte.value == 0) {
      throw DbException.get(22012, getSQL());
    }
    return get((byte)(this.value / localValueByte.value));
  }
  
  public Value modulus(Value paramValue)
  {
    ValueByte localValueByte = (ValueByte)paramValue;
    if (localValueByte.value == 0) {
      throw DbException.get(22012, getSQL());
    }
    return get((byte)(this.value % localValueByte.value));
  }
  
  public String getSQL()
  {
    return getString();
  }
  
  public int getType()
  {
    return 2;
  }
  
  public byte getByte()
  {
    return this.value;
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    ValueByte localValueByte = (ValueByte)paramValue;
    return MathUtils.compareInt(this.value, localValueByte.value);
  }
  
  public String getString()
  {
    return String.valueOf(this.value);
  }
  
  public long getPrecision()
  {
    return 3L;
  }
  
  public int hashCode()
  {
    return this.value;
  }
  
  public Object getObject()
  {
    return Byte.valueOf(this.value);
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setByte(paramInt, this.value);
  }
  
  public static ValueByte get(byte paramByte)
  {
    return (ValueByte)Value.cache(new ValueByte(paramByte));
  }
  
  public int getDisplaySize()
  {
    return 4;
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof ValueByte)) && (this.value == ((ValueByte)paramObject).value);
  }
}
