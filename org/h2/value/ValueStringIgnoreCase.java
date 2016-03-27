package org.h2.value;

import org.h2.engine.SysProperties;
import org.h2.util.StringUtils;

public class ValueStringIgnoreCase
  extends ValueString
{
  private static final ValueStringIgnoreCase EMPTY = new ValueStringIgnoreCase("");
  private int hash;
  
  protected ValueStringIgnoreCase(String paramString)
  {
    super(paramString);
  }
  
  public int getType()
  {
    return 14;
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    ValueStringIgnoreCase localValueStringIgnoreCase = (ValueStringIgnoreCase)paramValue;
    return paramCompareMode.compareString(this.value, localValueStringIgnoreCase.value, true);
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof ValueString)) && (this.value.equalsIgnoreCase(((ValueString)paramObject).value));
  }
  
  public int hashCode()
  {
    if (this.hash == 0) {
      this.hash = this.value.toUpperCase().hashCode();
    }
    return this.hash;
  }
  
  public String getSQL()
  {
    return "CAST(" + StringUtils.quoteStringSQL(this.value) + " AS VARCHAR_IGNORECASE)";
  }
  
  public static ValueStringIgnoreCase get(String paramString)
  {
    if (paramString.length() == 0) {
      return EMPTY;
    }
    ValueStringIgnoreCase localValueStringIgnoreCase1 = new ValueStringIgnoreCase(StringUtils.cache(paramString));
    if (paramString.length() > SysProperties.OBJECT_CACHE_MAX_PER_ELEMENT_SIZE) {
      return localValueStringIgnoreCase1;
    }
    ValueStringIgnoreCase localValueStringIgnoreCase2 = (ValueStringIgnoreCase)Value.cache(localValueStringIgnoreCase1);
    if (localValueStringIgnoreCase2.value.equals(paramString)) {
      return localValueStringIgnoreCase2;
    }
    return localValueStringIgnoreCase1;
  }
  
  protected ValueString getNew(String paramString)
  {
    return get(paramString);
  }
}
