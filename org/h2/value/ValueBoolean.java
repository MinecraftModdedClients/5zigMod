package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ValueBoolean
  extends Value
{
  public static final int PRECISION = 1;
  public static final int DISPLAY_SIZE = 5;
  private static final Object TRUE = new ValueBoolean(true);
  private static final Object FALSE = new ValueBoolean(false);
  private final Boolean value;
  
  private ValueBoolean(boolean paramBoolean)
  {
    this.value = Boolean.valueOf(paramBoolean);
  }
  
  public int getType()
  {
    return 1;
  }
  
  public String getSQL()
  {
    return getString();
  }
  
  public String getString()
  {
    return this.value.booleanValue() ? "TRUE" : "FALSE";
  }
  
  public Value negate()
  {
    return (ValueBoolean)(this.value.booleanValue() ? FALSE : TRUE);
  }
  
  public Boolean getBoolean()
  {
    return this.value;
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    boolean bool1 = ((ValueBoolean)paramValue).value.booleanValue();
    boolean bool2 = this.value.booleanValue();
    return bool2 ? 1 : bool2 == bool1 ? 0 : -1;
  }
  
  public long getPrecision()
  {
    return 1L;
  }
  
  public int hashCode()
  {
    return this.value.booleanValue() ? 1 : 0;
  }
  
  public Object getObject()
  {
    return this.value;
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setBoolean(paramInt, this.value.booleanValue());
  }
  
  public static ValueBoolean get(boolean paramBoolean)
  {
    return (ValueBoolean)(paramBoolean ? TRUE : FALSE);
  }
  
  public int getDisplaySize()
  {
    return 5;
  }
  
  public boolean equals(Object paramObject)
  {
    return this == paramObject;
  }
}
