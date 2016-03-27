package org.h2.value;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Locale;
import org.h2.message.DbException;
import org.h2.util.JdbcUtils;
import org.h2.util.StringUtils;

public class CompareModeIcu4J
  extends CompareMode
{
  private final Comparator<String> collator;
  
  protected CompareModeIcu4J(String paramString, int paramInt, boolean paramBoolean)
  {
    super(paramString, paramInt, paramBoolean);
    this.collator = getIcu4jCollator(paramString, paramInt);
  }
  
  public int compareString(String paramString1, String paramString2, boolean paramBoolean)
  {
    if (paramBoolean)
    {
      paramString1 = paramString1.toUpperCase();
      paramString2 = paramString2.toUpperCase();
    }
    return this.collator.compare(paramString1, paramString2);
  }
  
  public boolean equalsChars(String paramString1, int paramInt1, String paramString2, int paramInt2, boolean paramBoolean)
  {
    return compareString(paramString1.substring(paramInt1, paramInt1 + 1), paramString2.substring(paramInt2, paramInt2 + 1), paramBoolean) == 0;
  }
  
  private static Comparator<String> getIcu4jCollator(String paramString, int paramInt)
  {
    try
    {
      Comparator localComparator = null;
      Class localClass = JdbcUtils.loadUserClass("com.ibm.icu.text.Collator");
      
      Method localMethod = localClass.getMethod("getInstance", new Class[] { Locale.class });
      Locale localLocale2;
      if (paramString.length() == 2)
      {
        Locale localLocale1 = new Locale(StringUtils.toLowerEnglish(paramString), "");
        if (compareLocaleNames(localLocale1, paramString)) {
          localComparator = (Comparator)localMethod.invoke(null, new Object[] { localLocale1 });
        }
      }
      else if (paramString.length() == 5)
      {
        int i = paramString.indexOf('_');
        if (i >= 0)
        {
          String str1 = StringUtils.toLowerEnglish(paramString.substring(0, i));
          String str2 = paramString.substring(i + 1);
          localLocale2 = new Locale(str1, str2);
          if (compareLocaleNames(localLocale2, paramString)) {
            localComparator = (Comparator)localMethod.invoke(null, new Object[] { localLocale2 });
          }
        }
      }
      if (localComparator == null) {
        for (localLocale2 : (Locale[])localClass.getMethod("getAvailableLocales", new Class[0]).invoke(null, new Object[0])) {
          if (compareLocaleNames(localLocale2, paramString))
          {
            localComparator = (Comparator)localMethod.invoke(null, new Object[] { localLocale2 });
            break;
          }
        }
      }
      if (localComparator == null) {
        throw DbException.getInvalidValueException("collator", paramString);
      }
      localClass.getMethod("setStrength", new Class[] { Integer.TYPE }).invoke(localComparator, new Object[] { Integer.valueOf(paramInt) });
      return localComparator;
    }
    catch (Exception localException)
    {
      throw DbException.convert(localException);
    }
  }
}
