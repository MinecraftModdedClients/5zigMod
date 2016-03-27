package org.h2.value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.h2.message.DbException;
import org.h2.util.MathUtils;

public class ValueLong
  extends Value
{
  public static final BigInteger MAX = BigInteger.valueOf(Long.MAX_VALUE);
  public static final BigDecimal MIN_BD = BigDecimal.valueOf(Long.MIN_VALUE);
  public static final int PRECISION = 19;
  public static final int DISPLAY_SIZE = 20;
  private static final BigInteger MIN = BigInteger.valueOf(Long.MIN_VALUE);
  private static final int STATIC_SIZE = 100;
  private static final ValueLong[] STATIC_CACHE = new ValueLong[100];
  private final long value;
  
  static
  {
    for (int i = 0; i < 100; i++) {
      STATIC_CACHE[i] = new ValueLong(i);
    }
  }
  
  private ValueLong(long paramLong)
  {
    this.value = paramLong;
  }
  
  public Value add(Value paramValue)
  {
    ValueLong localValueLong = (ValueLong)paramValue;
    long l = this.value + localValueLong.value;
    int i = Long.signum(this.value);
    int j = Long.signum(localValueLong.value);
    int k = Long.signum(l);
    if ((i != j) || (k == j) || (i == 0) || (j == 0)) {
      return get(l);
    }
    throw getOverflow();
  }
  
  public int getSignum()
  {
    return Long.signum(this.value);
  }
  
  public Value negate()
  {
    if (this.value == Long.MIN_VALUE) {
      throw getOverflow();
    }
    return get(-this.value);
  }
  
  private DbException getOverflow()
  {
    return DbException.get(22003, Long.toString(this.value));
  }
  
  public Value subtract(Value paramValue)
  {
    ValueLong localValueLong = (ValueLong)paramValue;
    int i = Long.signum(this.value);
    int j = Long.signum(localValueLong.value);
    if ((i == j) || (j == 0)) {
      return get(this.value - localValueLong.value);
    }
    return add(localValueLong.negate());
  }
  
  private static boolean isInteger(long paramLong)
  {
    return (paramLong >= -2147483648L) && (paramLong <= 2147483647L);
  }
  
  public Value multiply(Value paramValue)
  {
    ValueLong localValueLong = (ValueLong)paramValue;
    long l = this.value * localValueLong.value;
    if ((this.value == 0L) || (this.value == 1L) || (localValueLong.value == 0L) || (localValueLong.value == 1L)) {
      return get(l);
    }
    if ((isInteger(this.value)) && (isInteger(localValueLong.value))) {
      return get(l);
    }
    BigInteger localBigInteger1 = BigInteger.valueOf(this.value);
    BigInteger localBigInteger2 = BigInteger.valueOf(localValueLong.value);
    BigInteger localBigInteger3 = localBigInteger1.multiply(localBigInteger2);
    if ((localBigInteger3.compareTo(MIN) < 0) || (localBigInteger3.compareTo(MAX) > 0)) {
      throw getOverflow();
    }
    return get(localBigInteger3.longValue());
  }
  
  public Value divide(Value paramValue)
  {
    ValueLong localValueLong = (ValueLong)paramValue;
    if (localValueLong.value == 0L) {
      throw DbException.get(22012, getSQL());
    }
    return get(this.value / localValueLong.value);
  }
  
  public Value modulus(Value paramValue)
  {
    ValueLong localValueLong = (ValueLong)paramValue;
    if (localValueLong.value == 0L) {
      throw DbException.get(22012, getSQL());
    }
    return get(this.value % localValueLong.value);
  }
  
  public String getSQL()
  {
    return getString();
  }
  
  public int getType()
  {
    return 5;
  }
  
  public long getLong()
  {
    return this.value;
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    ValueLong localValueLong = (ValueLong)paramValue;
    return MathUtils.compareLong(this.value, localValueLong.value);
  }
  
  public String getString()
  {
    return String.valueOf(this.value);
  }
  
  public long getPrecision()
  {
    return 19L;
  }
  
  public int hashCode()
  {
    return (int)(this.value ^ this.value >> 32);
  }
  
  public Object getObject()
  {
    return Long.valueOf(this.value);
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setLong(paramInt, this.value);
  }
  
  public static ValueLong get(long paramLong)
  {
    if ((paramLong >= 0L) && (paramLong < 100L)) {
      return STATIC_CACHE[((int)paramLong)];
    }
    return (ValueLong)Value.cache(new ValueLong(paramLong));
  }
  
  public int getDisplaySize()
  {
    return 20;
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof ValueLong)) && (this.value == ((ValueLong)paramObject).value);
  }
}
