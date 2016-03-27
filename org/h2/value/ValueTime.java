package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import org.h2.message.DbException;
import org.h2.util.DateTimeUtils;
import org.h2.util.MathUtils;
import org.h2.util.StringUtils;

public class ValueTime
  extends Value
{
  public static final int PRECISION = 6;
  static final int DISPLAY_SIZE = 8;
  private final long nanos;
  
  private ValueTime(long paramLong)
  {
    this.nanos = paramLong;
  }
  
  public static ValueTime fromNanos(long paramLong)
  {
    return (ValueTime)Value.cache(new ValueTime(paramLong));
  }
  
  public static ValueTime get(Time paramTime)
  {
    return fromNanos(DateTimeUtils.nanosFromDate(paramTime.getTime()));
  }
  
  public static ValueTime fromMillis(long paramLong)
  {
    return fromNanos(DateTimeUtils.nanosFromDate(paramLong));
  }
  
  public static ValueTime parse(String paramString)
  {
    try
    {
      return fromNanos(DateTimeUtils.parseTimeNanos(paramString, 0, paramString.length(), false));
    }
    catch (Exception localException)
    {
      throw DbException.get(22007, localException, new String[] { "TIME", paramString });
    }
  }
  
  public long getNanos()
  {
    return this.nanos;
  }
  
  public Time getTime()
  {
    return DateTimeUtils.convertNanoToTime(this.nanos);
  }
  
  public int getType()
  {
    return 9;
  }
  
  public String getString()
  {
    StringBuilder localStringBuilder = new StringBuilder(8);
    appendTime(localStringBuilder, this.nanos, false);
    return localStringBuilder.toString();
  }
  
  public String getSQL()
  {
    return "TIME '" + getString() + "'";
  }
  
  public long getPrecision()
  {
    return 6L;
  }
  
  public int getDisplaySize()
  {
    return 8;
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    return MathUtils.compareLong(this.nanos, ((ValueTime)paramValue).nanos);
  }
  
  public boolean equals(Object paramObject)
  {
    if (this == paramObject) {
      return true;
    }
    return ((paramObject instanceof ValueTime)) && (this.nanos == ((ValueTime)paramObject).nanos);
  }
  
  public int hashCode()
  {
    return (int)(this.nanos ^ this.nanos >>> 32);
  }
  
  public Object getObject()
  {
    return getTime();
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setTime(paramInt, getTime());
  }
  
  public Value add(Value paramValue)
  {
    ValueTime localValueTime = (ValueTime)paramValue.convertTo(9);
    return fromNanos(this.nanos + localValueTime.getNanos());
  }
  
  public Value subtract(Value paramValue)
  {
    ValueTime localValueTime = (ValueTime)paramValue.convertTo(9);
    return fromNanos(this.nanos - localValueTime.getNanos());
  }
  
  public Value multiply(Value paramValue)
  {
    return fromNanos((this.nanos * paramValue.getDouble()));
  }
  
  public Value divide(Value paramValue)
  {
    return fromNanos((this.nanos / paramValue.getDouble()));
  }
  
  public int getSignum()
  {
    return Long.signum(this.nanos);
  }
  
  public Value negate()
  {
    return fromNanos(-this.nanos);
  }
  
  static void appendTime(StringBuilder paramStringBuilder, long paramLong, boolean paramBoolean)
  {
    if (paramLong < 0L)
    {
      paramStringBuilder.append('-');
      paramLong = -paramLong;
    }
    long l1 = paramLong / 1000000L;
    paramLong -= l1 * 1000000L;
    long l2 = l1 / 1000L;
    l1 -= l2 * 1000L;
    long l3 = l2 / 60L;
    l2 -= l3 * 60L;
    long l4 = l3 / 60L;
    l3 -= l4 * 60L;
    StringUtils.appendZeroPadded(paramStringBuilder, 2, l4);
    paramStringBuilder.append(':');
    StringUtils.appendZeroPadded(paramStringBuilder, 2, l3);
    paramStringBuilder.append(':');
    StringUtils.appendZeroPadded(paramStringBuilder, 2, l2);
    if ((paramBoolean) || (l1 > 0L) || (paramLong > 0L))
    {
      paramStringBuilder.append('.');
      int i = paramStringBuilder.length();
      StringUtils.appendZeroPadded(paramStringBuilder, 3, l1);
      if (paramLong > 0L) {
        StringUtils.appendZeroPadded(paramStringBuilder, 6, paramLong);
      }
      for (int j = paramStringBuilder.length() - 1; j > i; j--)
      {
        if (paramStringBuilder.charAt(j) != '0') {
          break;
        }
        paramStringBuilder.deleteCharAt(j);
      }
    }
  }
}
