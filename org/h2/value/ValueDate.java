package org.h2.value;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.h2.message.DbException;
import org.h2.util.DateTimeUtils;
import org.h2.util.MathUtils;
import org.h2.util.StringUtils;

public class ValueDate
  extends Value
{
  public static final int PRECISION = 8;
  public static final int DISPLAY_SIZE = 10;
  private final long dateValue;
  
  private ValueDate(long paramLong)
  {
    this.dateValue = paramLong;
  }
  
  public static ValueDate fromDateValue(long paramLong)
  {
    return (ValueDate)Value.cache(new ValueDate(paramLong));
  }
  
  public static ValueDate get(Date paramDate)
  {
    return fromDateValue(DateTimeUtils.dateValueFromDate(paramDate.getTime()));
  }
  
  public static ValueDate fromMillis(long paramLong)
  {
    return fromDateValue(DateTimeUtils.dateValueFromDate(paramLong));
  }
  
  public static ValueDate parse(String paramString)
  {
    try
    {
      return fromDateValue(DateTimeUtils.parseDateValue(paramString, 0, paramString.length()));
    }
    catch (Exception localException)
    {
      throw DbException.get(22007, localException, new String[] { "DATE", paramString });
    }
  }
  
  public long getDateValue()
  {
    return this.dateValue;
  }
  
  public Date getDate()
  {
    return DateTimeUtils.convertDateValueToDate(this.dateValue);
  }
  
  public int getType()
  {
    return 10;
  }
  
  public String getString()
  {
    StringBuilder localStringBuilder = new StringBuilder(10);
    appendDate(localStringBuilder, this.dateValue);
    return localStringBuilder.toString();
  }
  
  public String getSQL()
  {
    return "DATE '" + getString() + "'";
  }
  
  public long getPrecision()
  {
    return 8L;
  }
  
  public int getDisplaySize()
  {
    return 10;
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    return MathUtils.compareLong(this.dateValue, ((ValueDate)paramValue).dateValue);
  }
  
  public boolean equals(Object paramObject)
  {
    if (this == paramObject) {
      return true;
    }
    return ((paramObject instanceof ValueDate)) && (this.dateValue == ((ValueDate)paramObject).dateValue);
  }
  
  public int hashCode()
  {
    return (int)(this.dateValue ^ this.dateValue >>> 32);
  }
  
  public Object getObject()
  {
    return getDate();
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setDate(paramInt, getDate());
  }
  
  static void appendDate(StringBuilder paramStringBuilder, long paramLong)
  {
    int i = DateTimeUtils.yearFromDateValue(paramLong);
    int j = DateTimeUtils.monthFromDateValue(paramLong);
    int k = DateTimeUtils.dayFromDateValue(paramLong);
    if ((i > 0) && (i < 10000)) {
      StringUtils.appendZeroPadded(paramStringBuilder, 4, i);
    } else {
      paramStringBuilder.append(i);
    }
    paramStringBuilder.append('-');
    StringUtils.appendZeroPadded(paramStringBuilder, 2, j);
    paramStringBuilder.append('-');
    StringUtils.appendZeroPadded(paramStringBuilder, 2, k);
  }
}
