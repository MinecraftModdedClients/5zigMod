package org.h2.value;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import org.h2.message.DbException;
import org.h2.util.DateTimeUtils;
import org.h2.util.MathUtils;

public class ValueTimestamp
  extends Value
{
  public static final int PRECISION = 23;
  static final int DISPLAY_SIZE = 23;
  static final int DEFAULT_SCALE = 10;
  private final long dateValue;
  private final long timeNanos;
  
  private ValueTimestamp(long paramLong1, long paramLong2)
  {
    this.dateValue = paramLong1;
    this.timeNanos = paramLong2;
  }
  
  public static ValueTimestamp fromDateValueAndNanos(long paramLong1, long paramLong2)
  {
    return (ValueTimestamp)Value.cache(new ValueTimestamp(paramLong1, paramLong2));
  }
  
  public static ValueTimestamp get(Timestamp paramTimestamp)
  {
    long l1 = paramTimestamp.getTime();
    long l2 = paramTimestamp.getNanos() % 1000000;
    long l3 = DateTimeUtils.dateValueFromDate(l1);
    l2 += DateTimeUtils.nanosFromDate(l1);
    return fromDateValueAndNanos(l3, l2);
  }
  
  public static ValueTimestamp fromMillisNanos(long paramLong, int paramInt)
  {
    long l1 = DateTimeUtils.dateValueFromDate(paramLong);
    long l2 = paramInt + DateTimeUtils.nanosFromDate(paramLong);
    return fromDateValueAndNanos(l1, l2);
  }
  
  public static ValueTimestamp fromMillis(long paramLong)
  {
    long l1 = DateTimeUtils.dateValueFromDate(paramLong);
    long l2 = DateTimeUtils.nanosFromDate(paramLong);
    return fromDateValueAndNanos(l1, l2);
  }
  
  public static ValueTimestamp parse(String paramString)
  {
    try
    {
      return parseTry(paramString);
    }
    catch (Exception localException)
    {
      throw DbException.get(22007, localException, new String[] { "TIMESTAMP", paramString });
    }
  }
  
  private static ValueTimestamp parseTry(String paramString)
  {
    int i = paramString.indexOf(' ');
    if (i < 0) {
      i = paramString.indexOf('T');
    }
    int j;
    if (i < 0)
    {
      i = paramString.length();
      j = -1;
    }
    else
    {
      j = i + 1;
    }
    long l1 = DateTimeUtils.parseDateValue(paramString, 0, i);
    long l2;
    if (j < 0)
    {
      l2 = 0L;
    }
    else
    {
      int k = paramString.length();
      TimeZone localTimeZone = null;
      int m;
      if (paramString.endsWith("Z"))
      {
        localTimeZone = TimeZone.getTimeZone("UTC");
        k--;
      }
      else
      {
        m = paramString.indexOf('+', i);
        if (m < 0) {
          m = paramString.indexOf('-', i);
        }
        String str;
        if (m >= 0)
        {
          str = "GMT" + paramString.substring(m);
          localTimeZone = TimeZone.getTimeZone(str);
          if (!localTimeZone.getID().startsWith(str)) {
            throw new IllegalArgumentException(str + " (" + localTimeZone.getID() + "?)");
          }
          k = m;
        }
        else
        {
          m = paramString.indexOf(' ', i + 1);
          if (m > 0)
          {
            str = paramString.substring(m + 1);
            localTimeZone = TimeZone.getTimeZone(str);
            if (!localTimeZone.getID().startsWith(str)) {
              throw new IllegalArgumentException(str);
            }
            k = m;
          }
        }
      }
      l2 = DateTimeUtils.parseTimeNanos(paramString, i + 1, k, true);
      if (localTimeZone != null)
      {
        m = DateTimeUtils.yearFromDateValue(l1);
        int n = DateTimeUtils.monthFromDateValue(l1);
        int i1 = DateTimeUtils.dayFromDateValue(l1);
        long l3 = l2 / 1000000L;
        l2 -= l3 * 1000000L;
        long l4 = l3 / 1000L;
        l3 -= l4 * 1000L;
        int i2 = (int)(l4 / 60L);
        l4 -= i2 * 60;
        int i3 = i2 / 60;
        i2 -= i3 * 60;
        long l5 = DateTimeUtils.getMillis(localTimeZone, m, n, i1, i3, i2, (int)l4, (int)l3);
        
        l3 = DateTimeUtils.convertToLocal(new Date(l5), Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        
        long l6 = 86400000L;
        long l7 = (l3 >= 0L ? l3 : l3 - l6 + 1L) / l6;
        l1 = DateTimeUtils.dateValueFromAbsoluteDay(l7);
        l3 -= l7 * l6;
        l2 += l3 * 1000000L;
      }
    }
    return fromDateValueAndNanos(l1, l2);
  }
  
  public long getDateValue()
  {
    return this.dateValue;
  }
  
  public long getTimeNanos()
  {
    return this.timeNanos;
  }
  
  public Timestamp getTimestamp()
  {
    return DateTimeUtils.convertDateValueToTimestamp(this.dateValue, this.timeNanos);
  }
  
  public int getType()
  {
    return 11;
  }
  
  public String getString()
  {
    StringBuilder localStringBuilder = new StringBuilder(23);
    ValueDate.appendDate(localStringBuilder, this.dateValue);
    localStringBuilder.append(' ');
    ValueTime.appendTime(localStringBuilder, this.timeNanos, true);
    return localStringBuilder.toString();
  }
  
  public String getSQL()
  {
    return "TIMESTAMP '" + getString() + "'";
  }
  
  public long getPrecision()
  {
    return 23L;
  }
  
  public int getScale()
  {
    return 10;
  }
  
  public int getDisplaySize()
  {
    return 23;
  }
  
  public Value convertScale(boolean paramBoolean, int paramInt)
  {
    if (paramInt >= 10) {
      return this;
    }
    if (paramInt < 0) {
      throw DbException.getInvalidValueException("scale", Integer.valueOf(paramInt));
    }
    long l1 = this.timeNanos;
    BigDecimal localBigDecimal = BigDecimal.valueOf(l1);
    localBigDecimal = localBigDecimal.movePointLeft(9);
    localBigDecimal = ValueDecimal.setScale(localBigDecimal, paramInt);
    localBigDecimal = localBigDecimal.movePointRight(9);
    long l2 = localBigDecimal.longValue();
    if (l2 == l1) {
      return this;
    }
    return fromDateValueAndNanos(this.dateValue, l2);
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    ValueTimestamp localValueTimestamp = (ValueTimestamp)paramValue;
    int i = MathUtils.compareLong(this.dateValue, localValueTimestamp.dateValue);
    if (i != 0) {
      return i;
    }
    return MathUtils.compareLong(this.timeNanos, localValueTimestamp.timeNanos);
  }
  
  public boolean equals(Object paramObject)
  {
    if (this == paramObject) {
      return true;
    }
    if (!(paramObject instanceof ValueTimestamp)) {
      return false;
    }
    ValueTimestamp localValueTimestamp = (ValueTimestamp)paramObject;
    return (this.dateValue == localValueTimestamp.dateValue) && (this.timeNanos == localValueTimestamp.timeNanos);
  }
  
  public int hashCode()
  {
    return (int)(this.dateValue ^ this.dateValue >>> 32 ^ this.timeNanos ^ this.timeNanos >>> 32);
  }
  
  public Object getObject()
  {
    return getTimestamp();
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setTimestamp(paramInt, getTimestamp());
  }
  
  public Value add(Value paramValue)
  {
    ValueTimestamp localValueTimestamp = (ValueTimestamp)paramValue.convertTo(11);
    long l1 = DateTimeUtils.absoluteDayFromDateValue(this.dateValue);
    long l2 = DateTimeUtils.absoluteDayFromDateValue(localValueTimestamp.dateValue);
    return DateTimeUtils.normalizeTimestamp(l1 + l2, this.timeNanos + localValueTimestamp.timeNanos);
  }
  
  public Value subtract(Value paramValue)
  {
    ValueTimestamp localValueTimestamp = (ValueTimestamp)paramValue.convertTo(11);
    long l1 = DateTimeUtils.absoluteDayFromDateValue(this.dateValue);
    long l2 = DateTimeUtils.absoluteDayFromDateValue(localValueTimestamp.dateValue);
    return DateTimeUtils.normalizeTimestamp(l1 - l2, this.timeNanos - localValueTimestamp.timeNanos);
  }
}
