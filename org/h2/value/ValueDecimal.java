package org.h2.value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.h2.message.DbException;
import org.h2.util.MathUtils;

public class ValueDecimal
  extends Value
{
  public static final Object ZERO = new ValueDecimal(BigDecimal.ZERO);
  public static final Object ONE = new ValueDecimal(BigDecimal.ONE);
  static final int DEFAULT_PRECISION = 65535;
  static final int DEFAULT_SCALE = 32767;
  static final int DEFAULT_DISPLAY_SIZE = 65535;
  private static final int DIVIDE_SCALE_ADD = 25;
  private static final int BIG_DECIMAL_SCALE_MAX = 100000;
  private final BigDecimal value;
  private String valueString;
  private int precision;
  
  private ValueDecimal(BigDecimal paramBigDecimal)
  {
    if (paramBigDecimal == null) {
      throw new IllegalArgumentException();
    }
    if (!paramBigDecimal.getClass().equals(BigDecimal.class)) {
      throw DbException.get(90125, new String[] { BigDecimal.class.getName(), paramBigDecimal.getClass().getName() });
    }
    this.value = paramBigDecimal;
  }
  
  public Value add(Value paramValue)
  {
    ValueDecimal localValueDecimal = (ValueDecimal)paramValue;
    return get(this.value.add(localValueDecimal.value));
  }
  
  public Value subtract(Value paramValue)
  {
    ValueDecimal localValueDecimal = (ValueDecimal)paramValue;
    return get(this.value.subtract(localValueDecimal.value));
  }
  
  public Value negate()
  {
    return get(this.value.negate());
  }
  
  public Value multiply(Value paramValue)
  {
    ValueDecimal localValueDecimal = (ValueDecimal)paramValue;
    return get(this.value.multiply(localValueDecimal.value));
  }
  
  public Value divide(Value paramValue)
  {
    ValueDecimal localValueDecimal = (ValueDecimal)paramValue;
    if (localValueDecimal.value.signum() == 0) {
      throw DbException.get(22012, getSQL());
    }
    BigDecimal localBigDecimal = this.value.divide(localValueDecimal.value, this.value.scale() + 25, 5);
    if (localBigDecimal.signum() == 0) {
      localBigDecimal = BigDecimal.ZERO;
    } else if ((localBigDecimal.scale() > 0) && 
      (!localBigDecimal.unscaledValue().testBit(0))) {
      localBigDecimal = localBigDecimal.stripTrailingZeros();
    }
    return get(localBigDecimal);
  }
  
  public ValueDecimal modulus(Value paramValue)
  {
    ValueDecimal localValueDecimal = (ValueDecimal)paramValue;
    if (localValueDecimal.value.signum() == 0) {
      throw DbException.get(22012, getSQL());
    }
    BigDecimal localBigDecimal = this.value.remainder(localValueDecimal.value);
    return get(localBigDecimal);
  }
  
  public String getSQL()
  {
    return getString();
  }
  
  public int getType()
  {
    return 6;
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    ValueDecimal localValueDecimal = (ValueDecimal)paramValue;
    return this.value.compareTo(localValueDecimal.value);
  }
  
  public int getSignum()
  {
    return this.value.signum();
  }
  
  public BigDecimal getBigDecimal()
  {
    return this.value;
  }
  
  public String getString()
  {
    if (this.valueString == null)
    {
      String str = this.value.toPlainString();
      if (str.length() < 40) {
        this.valueString = str;
      } else {
        this.valueString = this.value.toString();
      }
    }
    return this.valueString;
  }
  
  public long getPrecision()
  {
    if (this.precision == 0) {
      this.precision = this.value.precision();
    }
    return this.precision;
  }
  
  public boolean checkPrecision(long paramLong)
  {
    if (paramLong == 65535L) {
      return true;
    }
    return getPrecision() <= paramLong;
  }
  
  public int getScale()
  {
    return this.value.scale();
  }
  
  public int hashCode()
  {
    return this.value.hashCode();
  }
  
  public Object getObject()
  {
    return this.value;
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setBigDecimal(paramInt, this.value);
  }
  
  public Value convertScale(boolean paramBoolean, int paramInt)
  {
    if (this.value.scale() == paramInt) {
      return this;
    }
    if (((paramBoolean) || (paramInt >= 32767)) && 
      (this.value.scale() < paramInt)) {
      return this;
    }
    BigDecimal localBigDecimal = setScale(this.value, paramInt);
    return get(localBigDecimal);
  }
  
  public Value convertPrecision(long paramLong, boolean paramBoolean)
  {
    if (getPrecision() <= paramLong) {
      return this;
    }
    if (paramBoolean) {
      return get(BigDecimal.valueOf(this.value.doubleValue()));
    }
    throw DbException.get(22003, Long.toString(paramLong));
  }
  
  public static ValueDecimal get(BigDecimal paramBigDecimal)
  {
    if (BigDecimal.ZERO.equals(paramBigDecimal)) {
      return (ValueDecimal)ZERO;
    }
    if (BigDecimal.ONE.equals(paramBigDecimal)) {
      return (ValueDecimal)ONE;
    }
    return (ValueDecimal)Value.cache(new ValueDecimal(paramBigDecimal));
  }
  
  public int getDisplaySize()
  {
    return MathUtils.convertLongToInt(getPrecision() + 2L);
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof ValueDecimal)) && (this.value.equals(((ValueDecimal)paramObject).value));
  }
  
  public int getMemory()
  {
    return this.value.precision() + 120;
  }
  
  public static BigDecimal setScale(BigDecimal paramBigDecimal, int paramInt)
  {
    if ((paramInt > 100000) || (paramInt < -100000)) {
      throw DbException.getInvalidValueException("scale", Integer.valueOf(paramInt));
    }
    return paramBigDecimal.setScale(paramInt, 4);
  }
}
