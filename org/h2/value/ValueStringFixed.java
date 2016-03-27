package org.h2.value;

import org.h2.engine.SysProperties;
import org.h2.util.StringUtils;

public class ValueStringFixed
  extends ValueString
{
  private static final ValueStringFixed EMPTY = new ValueStringFixed("");
  
  protected ValueStringFixed(String paramString)
  {
    super(paramString);
  }
  
  private static String trimRight(String paramString)
  {
    int i = paramString.length() - 1;
    int j = i;
    while ((j >= 0) && (paramString.charAt(j) == ' ')) {
      j--;
    }
    paramString = j == i ? paramString : paramString.substring(0, j + 1);
    return paramString;
  }
  
  public int getType()
  {
    return 21;
  }
  
  public static ValueStringFixed get(String paramString)
  {
    paramString = trimRight(paramString);
    if (paramString.length() == 0) {
      return EMPTY;
    }
    ValueStringFixed localValueStringFixed = new ValueStringFixed(StringUtils.cache(paramString));
    if (paramString.length() > SysProperties.OBJECT_CACHE_MAX_PER_ELEMENT_SIZE) {
      return localValueStringFixed;
    }
    return (ValueStringFixed)Value.cache(localValueStringFixed);
  }
  
  protected ValueString getNew(String paramString)
  {
    return get(paramString);
  }
}
