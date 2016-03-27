package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.h2.engine.SysProperties;
import org.h2.util.MathUtils;
import org.h2.util.StringUtils;

public class ValueString
  extends Value
{
  private static final ValueString EMPTY = new ValueString("");
  protected final String value;
  
  protected ValueString(String paramString)
  {
    this.value = paramString;
  }
  
  public String getSQL()
  {
    return StringUtils.quoteStringSQL(this.value);
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof ValueString)) && (this.value.equals(((ValueString)paramObject).value));
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    ValueString localValueString = (ValueString)paramValue;
    return paramCompareMode.compareString(this.value, localValueString.value, false);
  }
  
  public String getString()
  {
    return this.value;
  }
  
  public long getPrecision()
  {
    return this.value.length();
  }
  
  public Object getObject()
  {
    return this.value;
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setString(paramInt, this.value);
  }
  
  public int getDisplaySize()
  {
    return this.value.length();
  }
  
  public int getMemory()
  {
    return this.value.length() * 2 + 48;
  }
  
  public Value convertPrecision(long paramLong, boolean paramBoolean)
  {
    if ((paramLong == 0L) || (this.value.length() <= paramLong)) {
      return this;
    }
    int i = MathUtils.convertLongToInt(paramLong);
    return getNew(this.value.substring(0, i));
  }
  
  public int hashCode()
  {
    return this.value.hashCode();
  }
  
  public int getType()
  {
    return 13;
  }
  
  public static Value get(String paramString)
  {
    return get(paramString, false);
  }
  
  public static Value get(String paramString, boolean paramBoolean)
  {
    if (paramString.isEmpty()) {
      return paramBoolean ? ValueNull.INSTANCE : EMPTY;
    }
    ValueString localValueString = new ValueString(StringUtils.cache(paramString));
    if (paramString.length() > SysProperties.OBJECT_CACHE_MAX_PER_ELEMENT_SIZE) {
      return localValueString;
    }
    return Value.cache(localValueString);
  }
  
  protected Value getNew(String paramString)
  {
    return get(paramString);
  }
}
