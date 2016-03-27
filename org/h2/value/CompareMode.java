package org.h2.value;

import java.text.Collator;
import java.util.Locale;
import org.h2.engine.SysProperties;
import org.h2.util.StringUtils;

public class CompareMode
{
  public static final String OFF = "OFF";
  public static final String DEFAULT = "DEFAULT_";
  public static final String ICU4J = "ICU4J_";
  public static final String SIGNED = "SIGNED";
  public static final String UNSIGNED = "UNSIGNED";
  private static CompareMode lastUsed;
  private static final boolean CAN_USE_ICU4J;
  private final String name;
  private final int strength;
  private final boolean binaryUnsigned;
  
  static
  {
    boolean bool = false;
    try
    {
      Class.forName("com.ibm.icu.text.Collator");
      bool = true;
    }
    catch (Exception localException) {}
    CAN_USE_ICU4J = bool;
  }
  
  protected CompareMode(String paramString, int paramInt, boolean paramBoolean)
  {
    this.name = paramString;
    this.strength = paramInt;
    this.binaryUnsigned = paramBoolean;
  }
  
  public static synchronized CompareMode getInstance(String paramString, int paramInt)
  {
    return getInstance(paramString, paramInt, SysProperties.SORT_BINARY_UNSIGNED);
  }
  
  public static synchronized CompareMode getInstance(String paramString, int paramInt, boolean paramBoolean)
  {
    if ((lastUsed != null) && 
      (StringUtils.equals(lastUsed.name, paramString)) && (lastUsed.strength == paramInt) && (lastUsed.binaryUnsigned == paramBoolean)) {
      return lastUsed;
    }
    if ((paramString == null) || (paramString.equals("OFF")))
    {
      lastUsed = new CompareMode(paramString, paramInt, paramBoolean);
    }
    else
    {
      boolean bool;
      if (paramString.startsWith("ICU4J_"))
      {
        bool = true;
        paramString = paramString.substring("ICU4J_".length());
      }
      else if (paramString.startsWith("DEFAULT_"))
      {
        bool = false;
        paramString = paramString.substring("DEFAULT_".length());
      }
      else
      {
        bool = CAN_USE_ICU4J;
      }
      if (bool) {
        lastUsed = new CompareModeIcu4J(paramString, paramInt, paramBoolean);
      } else {
        lastUsed = new CompareModeDefault(paramString, paramInt, paramBoolean);
      }
    }
    return lastUsed;
  }
  
  public boolean equalsChars(String paramString1, int paramInt1, String paramString2, int paramInt2, boolean paramBoolean)
  {
    char c1 = paramString1.charAt(paramInt1);
    char c2 = paramString2.charAt(paramInt2);
    if (paramBoolean)
    {
      c1 = Character.toUpperCase(c1);
      c2 = Character.toUpperCase(c2);
    }
    return c1 == c2;
  }
  
  public int compareString(String paramString1, String paramString2, boolean paramBoolean)
  {
    if (paramBoolean) {
      return paramString1.compareToIgnoreCase(paramString2);
    }
    return paramString1.compareTo(paramString2);
  }
  
  public static String getName(Locale paramLocale)
  {
    Locale localLocale = Locale.ENGLISH;
    String str = paramLocale.getDisplayLanguage(localLocale) + ' ' + paramLocale.getDisplayCountry(localLocale) + ' ' + paramLocale.getVariant();
    
    str = StringUtils.toUpperEnglish(str.trim().replace(' ', '_'));
    return str;
  }
  
  static boolean compareLocaleNames(Locale paramLocale, String paramString)
  {
    return (paramString.equalsIgnoreCase(paramLocale.toString())) || (paramString.equalsIgnoreCase(getName(paramLocale)));
  }
  
  public static Collator getCollator(String paramString)
  {
    Collator localCollator = null;
    if (paramString.startsWith("ICU4J_")) {
      paramString = paramString.substring("ICU4J_".length());
    } else if (paramString.startsWith("DEFAULT_")) {
      paramString = paramString.substring("DEFAULT_".length());
    }
    Locale localLocale2;
    if (paramString.length() == 2)
    {
      Locale localLocale1 = new Locale(StringUtils.toLowerEnglish(paramString), "");
      if (compareLocaleNames(localLocale1, paramString)) {
        localCollator = Collator.getInstance(localLocale1);
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
          localCollator = Collator.getInstance(localLocale2);
        }
      }
    }
    if (localCollator == null) {
      for (localLocale2 : Collator.getAvailableLocales()) {
        if (compareLocaleNames(localLocale2, paramString))
        {
          localCollator = Collator.getInstance(localLocale2);
          break;
        }
      }
    }
    return localCollator;
  }
  
  public String getName()
  {
    return this.name == null ? "OFF" : this.name;
  }
  
  public int getStrength()
  {
    return this.strength;
  }
  
  public boolean isBinaryUnsigned()
  {
    return this.binaryUnsigned;
  }
  
  public boolean equals(Object paramObject)
  {
    if (paramObject == this) {
      return true;
    }
    if (!(paramObject instanceof CompareMode)) {
      return false;
    }
    CompareMode localCompareMode = (CompareMode)paramObject;
    if (!getName().equals(localCompareMode.getName())) {
      return false;
    }
    if (this.strength != localCompareMode.strength) {
      return false;
    }
    if (this.binaryUnsigned != localCompareMode.binaryUnsigned) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    return getName().hashCode() ^ this.strength ^ (this.binaryUnsigned ? -1 : 0);
  }
}
