package org.h2.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import org.h2.message.DbException;

public class ToChar
{
  private static final long JULIAN_EPOCH;
  
  static
  {
    GregorianCalendar localGregorianCalendar = new GregorianCalendar(Locale.ENGLISH);
    localGregorianCalendar.setGregorianChange(new java.sql.Date(Long.MAX_VALUE));
    localGregorianCalendar.clear();
    localGregorianCalendar.set(4713, 0, 1, 0, 0, 0);
    localGregorianCalendar.set(0, 0);
    JULIAN_EPOCH = localGregorianCalendar.getTimeInMillis();
  }
  
  public static String toChar(BigDecimal paramBigDecimal, String paramString1, String paramString2)
  {
    Object localObject1 = paramString1 != null ? paramString1.toUpperCase() : null;
    if ((localObject1 == null) || (((String)localObject1).equals("TM")) || (((String)localObject1).equals("TM9")))
    {
      String str1 = paramBigDecimal.toPlainString();
      return str1.startsWith("0.") ? str1.substring(1) : str1;
    }
    if (((String)localObject1).equals("TME"))
    {
      int i = paramBigDecimal.precision() - paramBigDecimal.scale() - 1;
      paramBigDecimal = paramBigDecimal.movePointLeft(i);
      return paramBigDecimal.toPlainString() + "E" + (i < 0 ? '-' : '+') + (Math.abs(i) < 10 ? "0" : "") + Math.abs(i);
    }
    boolean bool1;
    if (((String)localObject1).equals("RN"))
    {
      bool1 = paramString1.startsWith("r");
      localObject2 = StringUtils.pad(toRomanNumeral(paramBigDecimal.intValue()), 15, " ", false);
      return bool1 ? ((String)localObject2).toLowerCase() : localObject2;
    }
    if (((String)localObject1).equals("FMRN"))
    {
      bool1 = paramString1.charAt(2) == 'r';
      localObject2 = toRomanNumeral(paramBigDecimal.intValue());
      return bool1 ? ((String)localObject2).toLowerCase() : localObject2;
    }
    if (((String)localObject1).endsWith("X")) {
      return toHex(paramBigDecimal, paramString1);
    }
    String str2 = paramString1;
    Object localObject2 = DecimalFormatSymbols.getInstance();
    char c1 = ((DecimalFormatSymbols)localObject2).getGroupingSeparator();
    char c2 = ((DecimalFormatSymbols)localObject2).getDecimalSeparator();
    
    boolean bool2 = ((String)localObject1).startsWith("S");
    if (bool2) {
      paramString1 = paramString1.substring(1);
    }
    boolean bool3 = ((String)localObject1).endsWith("S");
    if (bool3) {
      paramString1 = paramString1.substring(0, paramString1.length() - 1);
    }
    boolean bool4 = ((String)localObject1).endsWith("MI");
    if (bool4) {
      paramString1 = paramString1.substring(0, paramString1.length() - 2);
    }
    boolean bool5 = ((String)localObject1).endsWith("PR");
    if (bool5) {
      paramString1 = paramString1.substring(0, paramString1.length() - 2);
    }
    int j = ((String)localObject1).indexOf("V");
    if (j >= 0)
    {
      int k = 0;
      for (m = j + 1; m < paramString1.length(); m++)
      {
        n = paramString1.charAt(m);
        if ((n == 48) || (n == 57)) {
          k++;
        }
      }
      paramBigDecimal = paramBigDecimal.movePointRight(k);
      paramString1 = paramString1.substring(0, j) + paramString1.substring(j + 1);
    }
    Integer localInteger;
    if (paramString1.endsWith("EEEE"))
    {
      localInteger = Integer.valueOf(paramBigDecimal.precision() - paramBigDecimal.scale() - 1);
      paramBigDecimal = paramBigDecimal.movePointLeft(localInteger.intValue());
      paramString1 = paramString1.substring(0, paramString1.length() - 4);
    }
    else
    {
      localInteger = null;
    }
    int m = 1;
    int n = !((String)localObject1).startsWith("FM") ? 1 : 0;
    if (n == 0) {
      paramString1 = paramString1.substring(2);
    }
    paramString1 = paramString1.replaceAll("[Bb]", "");
    
    int i1 = findDecimalSeparator(paramString1);
    int i2 = calculateScale(paramString1, i1);
    if (i2 < paramBigDecimal.scale()) {
      paramBigDecimal = paramBigDecimal.setScale(i2, 4);
    }
    for (int i3 = paramString1.indexOf('0'); (i3 >= 0) && (i3 < i1); i3++) {
      if (paramString1.charAt(i3) == '9') {
        paramString1 = paramString1.substring(0, i3) + "0" + paramString1.substring(i3 + 1);
      }
    }
    StringBuilder localStringBuilder = new StringBuilder();
    String str3 = paramBigDecimal.unscaledValue().abs().toString();
    
    int i4 = i1 - 1;
    int i5 = str3.length() - paramBigDecimal.scale() - 1;
    char c3;
    for (; i4 >= 0; i4--)
    {
      c3 = paramString1.charAt(i4);
      m++;
      if ((c3 == '9') || (c3 == '0'))
      {
        if (i5 >= 0)
        {
          char c4 = str3.charAt(i5);
          localStringBuilder.insert(0, c4);
          i5--;
        }
        else if ((c3 == '0') && (localInteger == null))
        {
          localStringBuilder.insert(0, '0');
        }
      }
      else if (c3 == ',')
      {
        if ((i5 >= 0) || ((i4 > 0) && (paramString1.charAt(i4 - 1) == '0'))) {
          localStringBuilder.insert(0, c3);
        }
      }
      else if ((c3 == 'G') || (c3 == 'g'))
      {
        if ((i5 >= 0) || ((i4 > 0) && (paramString1.charAt(i4 - 1) == '0'))) {
          localStringBuilder.insert(0, c1);
        }
      }
      else
      {
        Currency localCurrency;
        if ((c3 == 'C') || (c3 == 'c'))
        {
          localCurrency = Currency.getInstance(Locale.getDefault());
          localStringBuilder.insert(0, localCurrency.getCurrencyCode());
          m += 6;
        }
        else if ((c3 == 'L') || (c3 == 'l') || (c3 == 'U') || (c3 == 'u'))
        {
          localCurrency = Currency.getInstance(Locale.getDefault());
          localStringBuilder.insert(0, localCurrency.getSymbol());
          m += 9;
        }
        else if (c3 == '$')
        {
          localCurrency = Currency.getInstance(Locale.getDefault());
          String str4 = localCurrency.getSymbol();
          localStringBuilder.insert(0, str4);
        }
        else
        {
          throw DbException.get(90010, str2);
        }
      }
    }
    if (i5 >= 0) {
      return StringUtils.pad("", paramString1.length() + 1, "#", true);
    }
    if (i1 < paramString1.length())
    {
      m++;
      c3 = paramString1.charAt(i1);
      if ((c3 == 'd') || (c3 == 'D')) {
        localStringBuilder.append(c2);
      } else {
        localStringBuilder.append(c3);
      }
      i4 = i1 + 1;
      i5 = str3.length() - paramBigDecimal.scale();
      for (; i4 < paramString1.length(); i4++)
      {
        int i6 = paramString1.charAt(i4);
        m++;
        if ((i6 == 57) || (i6 == 48))
        {
          if (i5 < str3.length())
          {
            char c5 = str3.charAt(i5);
            localStringBuilder.append(c5);
            i5++;
          }
          else if ((i6 == 48) || (n != 0))
          {
            localStringBuilder.append('0');
          }
        }
        else {
          throw DbException.get(90010, str2);
        }
      }
    }
    addSign(localStringBuilder, paramBigDecimal.signum(), bool2, bool3, bool4, bool5, n);
    if (localInteger != null)
    {
      localStringBuilder.append('E');
      localStringBuilder.append(localInteger.intValue() < 0 ? '-' : '+');
      localStringBuilder.append(Math.abs(localInteger.intValue()) < 10 ? "0" : "");
      localStringBuilder.append(Math.abs(localInteger.intValue()));
    }
    if (n != 0) {
      if (localInteger != null) {
        localStringBuilder.insert(0, ' ');
      } else {
        while (localStringBuilder.length() < m) {
          localStringBuilder.insert(0, ' ');
        }
      }
    }
    return localStringBuilder.toString();
  }
  
  private static void addSign(StringBuilder paramStringBuilder, int paramInt, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3, boolean paramBoolean4, boolean paramBoolean5)
  {
    if (paramBoolean4)
    {
      if (paramInt < 0)
      {
        paramStringBuilder.insert(0, '<');
        paramStringBuilder.append('>');
      }
      else if (paramBoolean5)
      {
        paramStringBuilder.insert(0, ' ');
        paramStringBuilder.append(' ');
      }
    }
    else
    {
      String str;
      if (paramInt == 0) {
        str = "";
      } else if (paramInt < 0) {
        str = "-";
      } else if ((paramBoolean1) || (paramBoolean2)) {
        str = "+";
      } else if (paramBoolean5) {
        str = " ";
      } else {
        str = "";
      }
      if ((paramBoolean3) || (paramBoolean2)) {
        paramStringBuilder.append(str);
      } else {
        paramStringBuilder.insert(0, str);
      }
    }
  }
  
  private static int findDecimalSeparator(String paramString)
  {
    int i = paramString.indexOf('.');
    if (i == -1)
    {
      i = paramString.indexOf('D');
      if (i == -1)
      {
        i = paramString.indexOf('d');
        if (i == -1) {
          i = paramString.length();
        }
      }
    }
    return i;
  }
  
  private static int calculateScale(String paramString, int paramInt)
  {
    int i = 0;
    for (int j = paramInt; j < paramString.length(); j++)
    {
      int k = paramString.charAt(j);
      if ((k == 48) || (k == 57)) {
        i++;
      }
    }
    return i;
  }
  
  private static String toRomanNumeral(int paramInt)
  {
    int[] arrayOfInt = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
    
    String[] arrayOfString = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
    
    StringBuilder localStringBuilder = new StringBuilder();
    for (int i = 0; i < arrayOfInt.length; i++)
    {
      int j = arrayOfInt[i];
      String str = arrayOfString[i];
      while (paramInt >= j)
      {
        localStringBuilder.append(str);
        paramInt -= j;
      }
    }
    return localStringBuilder.toString();
  }
  
  private static String toHex(BigDecimal paramBigDecimal, String paramString)
  {
    int i = !paramString.toUpperCase().startsWith("FM") ? 1 : 0;
    int j = !paramString.contains("x") ? 1 : 0;
    boolean bool = paramString.startsWith("0");
    int k = 0;
    for (int m = 0; m < paramString.length(); m++)
    {
      int n = paramString.charAt(m);
      if ((n == 48) || (n == 88) || (n == 120)) {
        k++;
      }
    }
    m = paramBigDecimal.setScale(0, 4).intValue();
    String str = Integer.toHexString(m);
    if (k < str.length())
    {
      str = StringUtils.pad("", k + 1, "#", true);
    }
    else
    {
      if (j != 0) {
        str = str.toUpperCase();
      }
      if (bool) {
        str = StringUtils.pad(str, k, "0", false);
      }
      if (i != 0) {
        str = StringUtils.pad(str, paramString.length() + 1, " ", false);
      }
    }
    return str;
  }
  
  public static String toChar(Timestamp paramTimestamp, String paramString1, String paramString2)
  {
    if (paramString1 == null) {
      paramString1 = "DD-MON-YY HH.MI.SS.FF PM";
    }
    GregorianCalendar localGregorianCalendar = new GregorianCalendar(Locale.ENGLISH);
    localGregorianCalendar.setTimeInMillis(paramTimestamp.getTime());
    StringBuilder localStringBuilder = new StringBuilder();
    int i = 1;
    for (int j = 0; j < paramString1.length();)
    {
      Capitalization localCapitalization;
      String str1;
      if ((localCapitalization = containsAt(paramString1, j, new String[] { "A.D.", "B.C." })) != null)
      {
        str1 = localGregorianCalendar.get(0) == 1 ? "A.D." : "B.C.";
        localStringBuilder.append(localCapitalization.apply(str1));
        j += 4;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "AD", "BC" })) != null)
      {
        str1 = localGregorianCalendar.get(0) == 1 ? "AD" : "BC";
        localStringBuilder.append(localCapitalization.apply(str1));
        j += 2;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "A.M.", "P.M." })) != null)
      {
        str1 = localGregorianCalendar.get(9) == 0 ? "A.M." : "P.M.";
        localStringBuilder.append(localCapitalization.apply(str1));
        j += 4;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "AM", "PM" })) != null)
      {
        str1 = localGregorianCalendar.get(9) == 0 ? "AM" : "PM";
        localStringBuilder.append(localCapitalization.apply(str1));
        j += 2;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "DL" })) != null)
      {
        localStringBuilder.append(new SimpleDateFormat("EEEE, MMMM d, yyyy").format(paramTimestamp));
        j += 2;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "DS" })) != null)
      {
        localStringBuilder.append(new SimpleDateFormat("MM/dd/yyyy").format(paramTimestamp));
        j += 2;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "TS" })) != null)
      {
        localStringBuilder.append(new SimpleDateFormat("h:mm:ss aa").format(paramTimestamp));
        j += 2;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "DDD" })) != null)
      {
        localStringBuilder.append(localGregorianCalendar.get(6));
        j += 3;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "DD" })) != null)
      {
        localStringBuilder.append(String.format("%02d", new Object[] { Integer.valueOf(localGregorianCalendar.get(5)) }));
        
        j += 2;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "DY" })) != null)
      {
        str1 = new SimpleDateFormat("EEE").format(paramTimestamp).toUpperCase();
        localStringBuilder.append(localCapitalization.apply(str1));
        j += 2;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "DAY" })) != null)
      {
        str1 = new SimpleDateFormat("EEEE").format(paramTimestamp);
        if (i != 0) {
          str1 = StringUtils.pad(str1, "Wednesday".length(), " ", true);
        }
        localStringBuilder.append(localCapitalization.apply(str1));
        j += 3;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "D" })) != null)
      {
        localStringBuilder.append(localGregorianCalendar.get(7));
        j++;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "J" })) != null)
      {
        long l1 = paramTimestamp.getTime() - JULIAN_EPOCH;
        long l2 = Math.floor(l1 / 86400000L);
        localStringBuilder.append(l2);
        j++;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "HH24" })) != null)
      {
        localStringBuilder.append(new DecimalFormat("00").format(localGregorianCalendar.get(11)));
        j += 4;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "HH12" })) != null)
      {
        localStringBuilder.append(new DecimalFormat("00").format(localGregorianCalendar.get(10)));
        j += 4;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "HH" })) != null)
      {
        localStringBuilder.append(new DecimalFormat("00").format(localGregorianCalendar.get(10)));
        j += 2;
      }
      else if ((localCapitalization = containsAt(paramString1, j, new String[] { "MI" })) != null)
      {
        localStringBuilder.append(new DecimalFormat("00").format(localGregorianCalendar.get(12)));
        j += 2;
      }
      else
      {
        int k;
        if ((localCapitalization = containsAt(paramString1, j, new String[] { "SSSSS" })) != null)
        {
          k = localGregorianCalendar.get(11) * 60 * 60;
          k += localGregorianCalendar.get(12) * 60;
          k += localGregorianCalendar.get(13);
          localStringBuilder.append(k);
          j += 5;
        }
        else if ((localCapitalization = containsAt(paramString1, j, new String[] { "SS" })) != null)
        {
          localStringBuilder.append(new DecimalFormat("00").format(localGregorianCalendar.get(13)));
          j += 2;
        }
        else if ((localCapitalization = containsAt(paramString1, j, new String[] { "FF1", "FF2", "FF3", "FF4", "FF5", "FF6", "FF7", "FF8", "FF9" })) != null)
        {
          k = Integer.parseInt(paramString1.substring(j + 2, j + 3));
          int i1 = (int)(localGregorianCalendar.get(14) * Math.pow(10.0D, k - 3));
          localStringBuilder.append(i1);
          j += 3;
        }
        else if ((localCapitalization = containsAt(paramString1, j, new String[] { "FF" })) != null)
        {
          localStringBuilder.append(localGregorianCalendar.get(14) * 1000);
          j += 2;
        }
        else
        {
          TimeZone localTimeZone;
          if ((localCapitalization = containsAt(paramString1, j, new String[] { "TZR" })) != null)
          {
            localTimeZone = TimeZone.getDefault();
            localStringBuilder.append(localTimeZone.getID());
            j += 3;
          }
          else if ((localCapitalization = containsAt(paramString1, j, new String[] { "TZD" })) != null)
          {
            localTimeZone = TimeZone.getDefault();
            boolean bool = localTimeZone.inDaylightTime(new java.util.Date());
            localStringBuilder.append(localTimeZone.getDisplayName(bool, 0));
            j += 3;
          }
          else if ((localCapitalization = containsAt(paramString1, j, new String[] { "IW", "WW" })) != null)
          {
            localStringBuilder.append(localGregorianCalendar.get(3));
            j += 2;
          }
          else if ((localCapitalization = containsAt(paramString1, j, new String[] { "W" })) != null)
          {
            int m = (int)(1.0D + Math.floor(localGregorianCalendar.get(5) / 7));
            localStringBuilder.append(m);
            j++;
          }
          else if ((localCapitalization = containsAt(paramString1, j, new String[] { "Y,YYY" })) != null)
          {
            localStringBuilder.append(new DecimalFormat("#,###").format(getYear(localGregorianCalendar)));
            j += 5;
          }
          else if ((localCapitalization = containsAt(paramString1, j, new String[] { "SYYYY" })) != null)
          {
            if (localGregorianCalendar.get(0) == 0) {
              localStringBuilder.append('-');
            }
            localStringBuilder.append(new DecimalFormat("0000").format(getYear(localGregorianCalendar)));
            j += 5;
          }
          else if ((localCapitalization = containsAt(paramString1, j, new String[] { "YYYY", "IYYY", "RRRR" })) != null)
          {
            localStringBuilder.append(new DecimalFormat("0000").format(getYear(localGregorianCalendar)));
            j += 4;
          }
          else if ((localCapitalization = containsAt(paramString1, j, new String[] { "YYY", "IYY" })) != null)
          {
            localStringBuilder.append(new DecimalFormat("000").format(getYear(localGregorianCalendar) % 1000));
            j += 3;
          }
          else if ((localCapitalization = containsAt(paramString1, j, new String[] { "YY", "IY", "RR" })) != null)
          {
            localStringBuilder.append(new DecimalFormat("00").format(getYear(localGregorianCalendar) % 100));
            j += 2;
          }
          else if ((localCapitalization = containsAt(paramString1, j, new String[] { "I", "Y" })) != null)
          {
            localStringBuilder.append(getYear(localGregorianCalendar) % 10);
            j++;
          }
          else
          {
            String str2;
            if ((localCapitalization = containsAt(paramString1, j, new String[] { "MONTH" })) != null)
            {
              str2 = new SimpleDateFormat("MMMM").format(paramTimestamp);
              if (i != 0) {
                str2 = StringUtils.pad(str2, "September".length(), " ", true);
              }
              localStringBuilder.append(localCapitalization.apply(str2));
              j += 5;
            }
            else if ((localCapitalization = containsAt(paramString1, j, new String[] { "MON" })) != null)
            {
              str2 = new SimpleDateFormat("MMM").format(paramTimestamp);
              localStringBuilder.append(localCapitalization.apply(str2));
              j += 3;
            }
            else if ((localCapitalization = containsAt(paramString1, j, new String[] { "MM" })) != null)
            {
              localStringBuilder.append(String.format("%02d", new Object[] { Integer.valueOf(localGregorianCalendar.get(2) + 1) }));
              j += 2;
            }
            else
            {
              int n;
              if ((localCapitalization = containsAt(paramString1, j, new String[] { "RM" })) != null)
              {
                n = localGregorianCalendar.get(2) + 1;
                localStringBuilder.append(localCapitalization.apply(toRomanNumeral(n)));
                j += 2;
              }
              else if ((localCapitalization = containsAt(paramString1, j, new String[] { "Q" })) != null)
              {
                n = (int)(1.0D + Math.floor(localGregorianCalendar.get(2) / 3));
                localStringBuilder.append(n);
                j++;
              }
              else if ((localCapitalization = containsAt(paramString1, j, new String[] { "X" })) != null)
              {
                n = DecimalFormatSymbols.getInstance().getDecimalSeparator();
                localStringBuilder.append(n);
                j++;
              }
              else if ((localCapitalization = containsAt(paramString1, j, new String[] { "FM" })) != null)
              {
                i = i == 0 ? 1 : 0;
                j += 2;
              }
              else if ((localCapitalization = containsAt(paramString1, j, new String[] { "FX" })) != null)
              {
                j += 2;
              }
              else
              {
                if ((localCapitalization = containsAt(paramString1, j, new String[] { "\"" })) != null) {
                  for (j += 1; j < paramString1.length(); j++)
                  {
                    char c = paramString1.charAt(j);
                    if (c != '"')
                    {
                      localStringBuilder.append(c);
                    }
                    else
                    {
                      j++;
                      break;
                    }
                  }
                }
                if ((paramString1.charAt(j) == '-') || (paramString1.charAt(j) == '/') || (paramString1.charAt(j) == ',') || (paramString1.charAt(j) == '.') || (paramString1.charAt(j) == ';') || (paramString1.charAt(j) == ':') || (paramString1.charAt(j) == ' '))
                {
                  localStringBuilder.append(paramString1.charAt(j));
                  j++;
                }
                else
                {
                  throw DbException.get(90010, paramString1);
                }
              }
            }
          }
        }
      }
    }
    return localStringBuilder.toString();
  }
  
  private static int getYear(Calendar paramCalendar)
  {
    int i = paramCalendar.get(1);
    if (paramCalendar.get(0) == 0) {
      i--;
    }
    return i;
  }
  
  private static Capitalization containsAt(String paramString, int paramInt, String... paramVarArgs)
  {
    for (String str : paramVarArgs) {
      if (paramInt + str.length() <= paramString.length())
      {
        int k = 1;
        Boolean localBoolean1 = null;
        Boolean localBoolean2 = null;
        for (int m = 0; m < str.length(); m++)
        {
          char c1 = paramString.charAt(paramInt + m);
          char c2 = str.charAt(m);
          if ((c1 != c2) && (Character.toUpperCase(c1) != Character.toUpperCase(c2)))
          {
            k = 0;
            break;
          }
          if (Character.isLetter(c1)) {
            if (localBoolean1 == null) {
              localBoolean1 = Boolean.valueOf(Character.isUpperCase(c1));
            } else if (localBoolean2 == null) {
              localBoolean2 = Boolean.valueOf(Character.isUpperCase(c1));
            }
          }
        }
        if (k != 0) {
          return Capitalization.toCapitalization(localBoolean1, localBoolean2);
        }
      }
    }
    return null;
  }
  
  private static enum Capitalization
  {
    UPPERCASE,  LOWERCASE,  CAPITALIZE;
    
    private Capitalization() {}
    
    public static Capitalization toCapitalization(Boolean paramBoolean1, Boolean paramBoolean2)
    {
      if (paramBoolean1 == null) {
        return CAPITALIZE;
      }
      if (paramBoolean2 == null) {
        return paramBoolean1.booleanValue() ? UPPERCASE : LOWERCASE;
      }
      if (paramBoolean1.booleanValue()) {
        return paramBoolean2.booleanValue() ? UPPERCASE : CAPITALIZE;
      }
      return LOWERCASE;
    }
    
    public String apply(String paramString)
    {
      if ((paramString == null) || (paramString.isEmpty())) {
        return paramString;
      }
      switch (ToChar.1.$SwitchMap$org$h2$util$ToChar$Capitalization[ordinal()])
      {
      case 1: 
        return paramString.toUpperCase();
      case 2: 
        return paramString.toLowerCase();
      case 3: 
        return Character.toUpperCase(paramString.charAt(0)) + (paramString.length() > 1 ? paramString.toLowerCase().substring(1) : "");
      }
      throw new IllegalArgumentException("Unknown capitalization strategy: " + this);
    }
  }
}
