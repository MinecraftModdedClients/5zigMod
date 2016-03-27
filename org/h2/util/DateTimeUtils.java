package org.h2.util;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import org.h2.message.DbException;
import org.h2.value.Value;
import org.h2.value.ValueDate;
import org.h2.value.ValueNull;
import org.h2.value.ValueTime;
import org.h2.value.ValueTimestamp;

public class DateTimeUtils
{
  public static final long MILLIS_PER_DAY = 86400000L;
  private static final long NANOS_PER_DAY = 86400000000000L;
  private static final int SHIFT_YEAR = 9;
  private static final int SHIFT_MONTH = 5;
  private static final int[] NORMAL_DAYS_PER_MONTH = { 0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
  private static final int[] DAYS_OFFSET = { 0, 31, 61, 92, 122, 153, 184, 214, 245, 275, 306, 337, 366 };
  private static final ThreadLocal<Calendar> CACHED_CALENDAR = new ThreadLocal()
  {
    protected Calendar initialValue()
    {
      return Calendar.getInstance();
    }
  };
  
  public static void resetCalendar()
  {
    CACHED_CALENDAR.remove();
  }
  
  public static java.sql.Date convertDate(Value paramValue, Calendar paramCalendar)
  {
    if (paramValue == ValueNull.INSTANCE) {
      return null;
    }
    ValueDate localValueDate = (ValueDate)paramValue.convertTo(10);
    Calendar localCalendar = (Calendar)paramCalendar.clone();
    localCalendar.clear();
    localCalendar.setLenient(true);
    long l1 = localValueDate.getDateValue();
    setCalendarFields(localCalendar, yearFromDateValue(l1), monthFromDateValue(l1), dayFromDateValue(l1), 0, 0, 0, 0);
    
    long l2 = localCalendar.getTimeInMillis();
    return new java.sql.Date(l2);
  }
  
  public static Time convertTime(Value paramValue, Calendar paramCalendar)
  {
    if (paramValue == ValueNull.INSTANCE) {
      return null;
    }
    ValueTime localValueTime = (ValueTime)paramValue.convertTo(9);
    Calendar localCalendar = (Calendar)paramCalendar.clone();
    localCalendar.clear();
    localCalendar.setLenient(true);
    long l1 = localValueTime.getNanos();
    long l2 = l1 / 1000000L;
    l1 -= l2 * 1000000L;
    long l3 = l2 / 1000L;
    l2 -= l3 * 1000L;
    long l4 = l3 / 60L;
    l3 -= l4 * 60L;
    long l5 = l4 / 60L;
    l4 -= l5 * 60L;
    setCalendarFields(localCalendar, 1970, 1, 1, (int)l5, (int)l4, (int)l3, (int)l2);
    
    long l6 = localCalendar.getTimeInMillis();
    return new Time(l6);
  }
  
  public static Timestamp convertTimestamp(Value paramValue, Calendar paramCalendar)
  {
    if (paramValue == ValueNull.INSTANCE) {
      return null;
    }
    ValueTimestamp localValueTimestamp = (ValueTimestamp)paramValue.convertTo(11);
    Calendar localCalendar = (Calendar)paramCalendar.clone();
    localCalendar.clear();
    localCalendar.setLenient(true);
    long l1 = localValueTimestamp.getDateValue();
    long l2 = localValueTimestamp.getTimeNanos();
    long l3 = l2 / 1000000L;
    l2 -= l3 * 1000000L;
    long l4 = l3 / 1000L;
    l3 -= l4 * 1000L;
    long l5 = l4 / 60L;
    l4 -= l5 * 60L;
    long l6 = l5 / 60L;
    l5 -= l6 * 60L;
    setCalendarFields(localCalendar, yearFromDateValue(l1), monthFromDateValue(l1), dayFromDateValue(l1), (int)l6, (int)l5, (int)l4, (int)l3);
    
    long l7 = localCalendar.getTimeInMillis();
    Timestamp localTimestamp = new Timestamp(l7);
    localTimestamp.setNanos((int)(l2 + l3 * 1000000L));
    return localTimestamp;
  }
  
  public static ValueDate convertDate(java.sql.Date paramDate, Calendar paramCalendar)
  {
    if (paramCalendar == null) {
      throw DbException.getInvalidValueException("calendar", null);
    }
    Calendar localCalendar = (Calendar)paramCalendar.clone();
    localCalendar.setTimeInMillis(paramDate.getTime());
    long l = dateValueFromCalendar(localCalendar);
    return ValueDate.fromDateValue(l);
  }
  
  public static ValueTime convertTime(Time paramTime, Calendar paramCalendar)
  {
    if (paramCalendar == null) {
      throw DbException.getInvalidValueException("calendar", null);
    }
    Calendar localCalendar = (Calendar)paramCalendar.clone();
    localCalendar.setTimeInMillis(paramTime.getTime());
    long l = nanosFromCalendar(localCalendar);
    return ValueTime.fromNanos(l);
  }
  
  public static long convertToLocal(java.util.Date paramDate, Calendar paramCalendar)
  {
    if (paramCalendar == null) {
      throw DbException.getInvalidValueException("calendar", null);
    }
    paramCalendar = (Calendar)paramCalendar.clone();
    Calendar localCalendar = Calendar.getInstance();
    synchronized (localCalendar)
    {
      localCalendar.setTime(paramDate);
      convertTime(localCalendar, paramCalendar);
    }
    return paramCalendar.getTime().getTime();
  }
  
  private static void convertTime(Calendar paramCalendar1, Calendar paramCalendar2)
  {
    paramCalendar2.set(0, paramCalendar1.get(0));
    paramCalendar2.set(1, paramCalendar1.get(1));
    paramCalendar2.set(2, paramCalendar1.get(2));
    paramCalendar2.set(5, paramCalendar1.get(5));
    paramCalendar2.set(11, paramCalendar1.get(11));
    paramCalendar2.set(12, paramCalendar1.get(12));
    paramCalendar2.set(13, paramCalendar1.get(13));
    paramCalendar2.set(14, paramCalendar1.get(14));
  }
  
  public static ValueTimestamp convertTimestamp(Timestamp paramTimestamp, Calendar paramCalendar)
  {
    if (paramCalendar == null) {
      throw DbException.getInvalidValueException("calendar", null);
    }
    Calendar localCalendar = (Calendar)paramCalendar.clone();
    localCalendar.setTimeInMillis(paramTimestamp.getTime());
    long l1 = dateValueFromCalendar(localCalendar);
    long l2 = nanosFromCalendar(localCalendar);
    l2 += paramTimestamp.getNanos() % 1000000;
    return ValueTimestamp.fromDateValueAndNanos(l1, l2);
  }
  
  public static long parseDateValue(String paramString, int paramInt1, int paramInt2)
  {
    if (paramString.charAt(paramInt1) == '+') {
      paramInt1++;
    }
    int i = paramString.indexOf('-', paramInt1 + 1);
    int j = paramString.indexOf('-', i + 1);
    if ((i <= 0) || (j <= i)) {
      throw new IllegalArgumentException(paramString);
    }
    int k = Integer.parseInt(paramString.substring(paramInt1, i));
    int m = Integer.parseInt(paramString.substring(i + 1, j));
    int n = Integer.parseInt(paramString.substring(j + 1, paramInt2));
    if (!isValidDate(k, m, n)) {
      throw new IllegalArgumentException(k + "-" + m + "-" + n);
    }
    return dateValue(k, m, n);
  }
  
  public static long parseTimeNanos(String paramString, int paramInt1, int paramInt2, boolean paramBoolean)
  {
    int i = 0;int j = 0;int k = 0;
    long l = 0L;
    int m = paramString.indexOf(':', paramInt1);
    int n = paramString.indexOf(':', m + 1);
    int i1 = paramString.indexOf('.', n + 1);
    if ((m <= 0) || (n <= m)) {
      throw new IllegalArgumentException(paramString);
    }
    i = Integer.parseInt(paramString.substring(paramInt1, m));
    int i2;
    if (i < 0)
    {
      if (paramBoolean) {
        throw new IllegalArgumentException(paramString);
      }
      i2 = 1;
      i = -i;
    }
    else
    {
      i2 = 0;
    }
    j = Integer.parseInt(paramString.substring(m + 1, n));
    if (i1 < 0)
    {
      k = Integer.parseInt(paramString.substring(n + 1, paramInt2));
    }
    else
    {
      k = Integer.parseInt(paramString.substring(n + 1, i1));
      String str = (paramString.substring(i1 + 1, paramInt2) + "000000000").substring(0, 9);
      l = Integer.parseInt(str);
    }
    if ((i >= 2000000) || (j < 0) || (j >= 60) || (k < 0) || (k >= 60)) {
      throw new IllegalArgumentException(paramString);
    }
    if ((paramBoolean) && (i >= 24)) {
      throw new IllegalArgumentException(paramString);
    }
    l += ((i * 60L + j) * 60L + k) * 1000000000L;
    return i2 != 0 ? -l : l;
  }
  
  public static long getMillis(TimeZone paramTimeZone, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, int paramInt7)
  {
    try
    {
      return getTimeTry(false, paramTimeZone, paramInt1, paramInt2, paramInt3, paramInt4, paramInt5, paramInt6, paramInt7);
    }
    catch (IllegalArgumentException localIllegalArgumentException)
    {
      String str = localIllegalArgumentException.toString();
      if (str.indexOf("HOUR_OF_DAY") > 0)
      {
        if ((paramInt4 < 0) || (paramInt4 > 23)) {
          throw localIllegalArgumentException;
        }
        return getTimeTry(true, paramTimeZone, paramInt1, paramInt2, paramInt3, paramInt4, paramInt5, paramInt6, paramInt7);
      }
      if (str.indexOf("DAY_OF_MONTH") > 0)
      {
        int i;
        if (paramInt2 == 2) {
          i = new GregorianCalendar().isLeapYear(paramInt1) ? 29 : 28;
        } else {
          i = 30 + (paramInt2 + (paramInt2 > 7 ? 1 : 0) & 0x1);
        }
        if ((paramInt3 < 1) || (paramInt3 > i)) {
          throw localIllegalArgumentException;
        }
        paramInt4 += 6;
        return getTimeTry(true, paramTimeZone, paramInt1, paramInt2, paramInt3, paramInt4, paramInt5, paramInt6, paramInt7);
      }
    }
    return getTimeTry(true, paramTimeZone, paramInt1, paramInt2, paramInt3, paramInt4, paramInt5, paramInt6, paramInt7);
  }
  
  private static long getTimeTry(boolean paramBoolean, TimeZone paramTimeZone, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, int paramInt7)
  {
    Calendar localCalendar;
    if (paramTimeZone == null) {
      localCalendar = (Calendar)CACHED_CALENDAR.get();
    } else {
      localCalendar = Calendar.getInstance(paramTimeZone);
    }
    localCalendar.clear();
    localCalendar.setLenient(paramBoolean);
    setCalendarFields(localCalendar, paramInt1, paramInt2, paramInt3, paramInt4, paramInt5, paramInt6, paramInt7);
    return localCalendar.getTime().getTime();
  }
  
  private static void setCalendarFields(Calendar paramCalendar, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, int paramInt7)
  {
    if (paramInt1 <= 0)
    {
      paramCalendar.set(0, 0);
      paramCalendar.set(1, 1 - paramInt1);
    }
    else
    {
      paramCalendar.set(0, 1);
      paramCalendar.set(1, paramInt1);
    }
    paramCalendar.set(2, paramInt2 - 1);
    paramCalendar.set(5, paramInt3);
    paramCalendar.set(11, paramInt4);
    paramCalendar.set(12, paramInt5);
    paramCalendar.set(13, paramInt6);
    paramCalendar.set(14, paramInt7);
  }
  
  public static int getDatePart(java.util.Date paramDate, int paramInt)
  {
    Calendar localCalendar = (Calendar)CACHED_CALENDAR.get();
    localCalendar.setTime(paramDate);
    if (paramInt == 1) {
      return getYear(localCalendar);
    }
    int i = localCalendar.get(paramInt);
    if (paramInt == 2) {
      return i + 1;
    }
    return i;
  }
  
  private static int getYear(Calendar paramCalendar)
  {
    int i = paramCalendar.get(1);
    if (paramCalendar.get(0) == 0) {
      i = 1 - i;
    }
    return i;
  }
  
  public static long getTimeLocalWithoutDst(java.util.Date paramDate)
  {
    return paramDate.getTime() + ((Calendar)CACHED_CALENDAR.get()).get(15);
  }
  
  public static long getTimeUTCWithoutDst(long paramLong)
  {
    return paramLong - ((Calendar)CACHED_CALENDAR.get()).get(15);
  }
  
  public static int getIsoDayOfWeek(java.util.Date paramDate)
  {
    Calendar localCalendar = Calendar.getInstance();
    localCalendar.setTimeInMillis(paramDate.getTime());
    int i = localCalendar.get(7) - 1;
    return i == 0 ? 7 : i;
  }
  
  public static int getIsoWeek(java.util.Date paramDate)
  {
    Calendar localCalendar = Calendar.getInstance();
    localCalendar.setTimeInMillis(paramDate.getTime());
    localCalendar.setFirstDayOfWeek(2);
    localCalendar.setMinimalDaysInFirstWeek(4);
    return localCalendar.get(3);
  }
  
  public static int getIsoYear(java.util.Date paramDate)
  {
    Calendar localCalendar = Calendar.getInstance();
    localCalendar.setTimeInMillis(paramDate.getTime());
    localCalendar.setFirstDayOfWeek(2);
    localCalendar.setMinimalDaysInFirstWeek(4);
    int i = getYear(localCalendar);
    int j = localCalendar.get(2);
    int k = localCalendar.get(3);
    if ((j == 0) && (k > 51)) {
      i--;
    } else if ((j == 11) && (k == 1)) {
      i++;
    }
    return i;
  }
  
  /* Error */
  public static String formatDateTime(java.util.Date paramDate, String paramString1, String paramString2, String paramString3)
  {
    // Byte code:
    //   0: aload_1
    //   1: aload_2
    //   2: aload_3
    //   3: invokestatic 86	org/h2/util/DateTimeUtils:getDateFormat	(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/text/SimpleDateFormat;
    //   6: astore 4
    //   8: aload 4
    //   10: dup
    //   11: astore 5
    //   13: monitorenter
    //   14: aload 4
    //   16: aload_0
    //   17: invokevirtual 87	java/text/SimpleDateFormat:format	(Ljava/util/Date;)Ljava/lang/String;
    //   20: aload 5
    //   22: monitorexit
    //   23: areturn
    //   24: astore 6
    //   26: aload 5
    //   28: monitorexit
    //   29: aload 6
    //   31: athrow
    // Line number table:
    //   Java source line #541	-> byte code offset #0
    //   Java source line #542	-> byte code offset #8
    //   Java source line #543	-> byte code offset #14
    //   Java source line #544	-> byte code offset #24
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	32	0	paramDate	java.util.Date
    //   0	32	1	paramString1	String
    //   0	32	2	paramString2	String
    //   0	32	3	paramString3	String
    //   6	9	4	localSimpleDateFormat	SimpleDateFormat
    //   11	16	5	Ljava/lang/Object;	Object
    //   24	6	6	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   14	23	24	finally
    //   24	29	24	finally
  }
  
  /* Error */
  public static java.util.Date parseDateTime(String paramString1, String paramString2, String paramString3, String paramString4)
  {
    // Byte code:
    //   0: aload_1
    //   1: aload_2
    //   2: aload_3
    //   3: invokestatic 86	org/h2/util/DateTimeUtils:getDateFormat	(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/text/SimpleDateFormat;
    //   6: astore 4
    //   8: aload 4
    //   10: dup
    //   11: astore 5
    //   13: monitorenter
    //   14: aload 4
    //   16: aload_0
    //   17: invokevirtual 88	java/text/SimpleDateFormat:parse	(Ljava/lang/String;)Ljava/util/Date;
    //   20: aload 5
    //   22: monitorexit
    //   23: areturn
    //   24: astore 6
    //   26: aload 5
    //   28: monitorexit
    //   29: aload 6
    //   31: athrow
    //   32: astore 5
    //   34: ldc 90
    //   36: aload 5
    //   38: iconst_1
    //   39: anewarray 91	java/lang/String
    //   42: dup
    //   43: iconst_0
    //   44: aload_0
    //   45: aastore
    //   46: invokestatic 92	org/h2/message/DbException:get	(ILjava/lang/Throwable;[Ljava/lang/String;)Lorg/h2/message/DbException;
    //   49: athrow
    // Line number table:
    //   Java source line #558	-> byte code offset #0
    //   Java source line #560	-> byte code offset #8
    //   Java source line #561	-> byte code offset #14
    //   Java source line #562	-> byte code offset #24
    //   Java source line #563	-> byte code offset #32
    //   Java source line #565	-> byte code offset #34
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	50	0	paramString1	String
    //   0	50	1	paramString2	String
    //   0	50	2	paramString3	String
    //   0	50	3	paramString4	String
    //   6	9	4	localSimpleDateFormat	SimpleDateFormat
    //   11	16	5	Ljava/lang/Object;	Object
    //   32	5	5	localException	Exception
    //   24	6	6	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   14	23	24	finally
    //   24	29	24	finally
    //   8	23	32	java/lang/Exception
    //   24	32	32	java/lang/Exception
  }
  
  private static SimpleDateFormat getDateFormat(String paramString1, String paramString2, String paramString3)
  {
    try
    {
      SimpleDateFormat localSimpleDateFormat;
      if (paramString2 == null)
      {
        localSimpleDateFormat = new SimpleDateFormat(paramString1);
      }
      else
      {
        Locale localLocale = new Locale(paramString2);
        localSimpleDateFormat = new SimpleDateFormat(paramString1, localLocale);
      }
      if (paramString3 != null) {
        localSimpleDateFormat.setTimeZone(TimeZone.getTimeZone(paramString3));
      }
      return localSimpleDateFormat;
    }
    catch (Exception localException)
    {
      throw DbException.get(90014, localException, new String[] { paramString1 + "/" + paramString2 + "/" + paramString3 });
    }
  }
  
  public static boolean isValidDate(int paramInt1, int paramInt2, int paramInt3)
  {
    if ((paramInt2 < 1) || (paramInt2 > 12) || (paramInt3 < 1)) {
      return false;
    }
    if (paramInt1 > 1582)
    {
      if (paramInt2 != 2) {
        return paramInt3 <= NORMAL_DAYS_PER_MONTH[paramInt2];
      }
      if ((paramInt1 & 0x3) != 0) {
        return paramInt3 <= 28;
      }
      return paramInt3 <= ((paramInt1 % 100 != 0) || (paramInt1 % 400 == 0) ? 29 : 28);
    }
    if ((paramInt1 == 1582) && (paramInt2 == 10)) {
      return (paramInt3 <= 31) && ((paramInt3 < 5) || (paramInt3 > 14));
    }
    if ((paramInt2 != 2) && (paramInt3 <= NORMAL_DAYS_PER_MONTH[paramInt2])) {
      return true;
    }
    return paramInt3 <= ((paramInt1 & 0x3) != 0 ? 28 : 29);
  }
  
  public static java.sql.Date convertDateValueToDate(long paramLong)
  {
    long l = getMillis(null, yearFromDateValue(paramLong), monthFromDateValue(paramLong), dayFromDateValue(paramLong), 0, 0, 0, 0);
    
    return new java.sql.Date(l);
  }
  
  public static Timestamp convertDateValueToTimestamp(long paramLong1, long paramLong2)
  {
    long l1 = paramLong2 / 1000000L;
    paramLong2 -= l1 * 1000000L;
    long l2 = l1 / 1000L;
    l1 -= l2 * 1000L;
    long l3 = l2 / 60L;
    l2 -= l3 * 60L;
    long l4 = l3 / 60L;
    l3 -= l4 * 60L;
    long l5 = getMillis(null, yearFromDateValue(paramLong1), monthFromDateValue(paramLong1), dayFromDateValue(paramLong1), (int)l4, (int)l3, (int)l2, 0);
    
    Timestamp localTimestamp = new Timestamp(l5);
    localTimestamp.setNanos((int)(paramLong2 + l1 * 1000000L));
    return localTimestamp;
  }
  
  public static Time convertNanoToTime(long paramLong)
  {
    long l1 = paramLong / 1000000L;
    long l2 = l1 / 1000L;
    l1 -= l2 * 1000L;
    long l3 = l2 / 60L;
    l2 -= l3 * 60L;
    long l4 = l3 / 60L;
    l3 -= l4 * 60L;
    long l5 = getMillis(null, 1970, 1, 1, (int)(l4 % 24L), (int)l3, (int)l2, (int)l1);
    
    return new Time(l5);
  }
  
  public static int yearFromDateValue(long paramLong)
  {
    return (int)(paramLong >>> 9);
  }
  
  public static int monthFromDateValue(long paramLong)
  {
    return (int)(paramLong >>> 5) & 0xF;
  }
  
  public static int dayFromDateValue(long paramLong)
  {
    return (int)(paramLong & 0x1F);
  }
  
  public static long dateValue(long paramLong, int paramInt1, int paramInt2)
  {
    return paramLong << 9 | paramInt1 << 5 | paramInt2;
  }
  
  public static long dateValueFromDate(long paramLong)
  {
    Calendar localCalendar = (Calendar)CACHED_CALENDAR.get();
    localCalendar.clear();
    localCalendar.setTimeInMillis(paramLong);
    return dateValueFromCalendar(localCalendar);
  }
  
  private static long dateValueFromCalendar(Calendar paramCalendar)
  {
    int i = getYear(paramCalendar);
    int j = paramCalendar.get(2) + 1;
    int k = paramCalendar.get(5);
    return i << 9 | j << 5 | k;
  }
  
  public static long nanosFromDate(long paramLong)
  {
    Calendar localCalendar = (Calendar)CACHED_CALENDAR.get();
    localCalendar.clear();
    localCalendar.setTimeInMillis(paramLong);
    return nanosFromCalendar(localCalendar);
  }
  
  private static long nanosFromCalendar(Calendar paramCalendar)
  {
    int i = paramCalendar.get(11);
    int j = paramCalendar.get(12);
    int k = paramCalendar.get(13);
    int m = paramCalendar.get(14);
    return (((i * 60L + j) * 60L + k) * 1000L + m) * 1000000L;
  }
  
  public static ValueTimestamp normalizeTimestamp(long paramLong1, long paramLong2)
  {
    if ((paramLong2 > 86400000000000L) || (paramLong2 < 0L))
    {
      long l;
      if (paramLong2 > 86400000000000L) {
        l = paramLong2 / 86400000000000L;
      } else {
        l = (paramLong2 - 86400000000000L + 1L) / 86400000000000L;
      }
      paramLong2 -= l * 86400000000000L;
      paramLong1 += l;
    }
    return ValueTimestamp.fromDateValueAndNanos(dateValueFromAbsoluteDay(paramLong1), paramLong2);
  }
  
  public static long absoluteDayFromDateValue(long paramLong)
  {
    long l1 = yearFromDateValue(paramLong);
    int i = monthFromDateValue(paramLong);
    int j = dayFromDateValue(paramLong);
    if (i <= 2)
    {
      l1 -= 1L;
      i += 12;
    }
    long l2 = (l1 * 2922L >> 3) + DAYS_OFFSET[(i - 3)] + j - 719484L;
    if ((l1 <= 1582L) && ((l1 < 1582L) || (i * 100 + j < 1005))) {
      l2 += 13L;
    } else if ((l1 < 1901L) || (l1 > 2099L)) {
      l2 += l1 / 400L - l1 / 100L + 15L;
    }
    return l2;
  }
  
  public static long dateValueFromAbsoluteDay(long paramLong)
  {
    long l1 = paramLong + 719468L;
    long l2 = 0L;
    long l3;
    if (l1 > 578040L)
    {
      l4 = l1 / 146097L;
      l1 -= l4 * 146097L;
      l2 = l1 / 36524L;
      l1 -= l2 * 36524L;
      l3 = l4 * 400L + l2 * 100L;
    }
    else
    {
      l1 += 292200000002L;
      l3 = -800000000L;
    }
    long l4 = l1 / 1461L;
    l1 -= l4 * 1461L;
    long l5 = l1 / 365L;
    l1 -= l5 * 365L;
    if ((l1 == 0L) && ((l5 == 4L) || (l2 == 4L)))
    {
      l5 -= 1L;
      l1 += 365L;
    }
    l5 += l3 + l4 * 4L;
    
    int i = ((int)l1 * 2 + 1) * 5 / 306;
    l1 -= DAYS_OFFSET[i] - 1;
    if (i >= 10)
    {
      l5 += 1L;
      i -= 12;
    }
    return dateValue(l5, i + 3, (int)l1);
  }
}
