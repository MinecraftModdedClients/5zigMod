package org.h2.engine;

import org.h2.message.DbException;
import org.h2.table.Table;

public class Setting
  extends DbObjectBase
{
  private int intValue;
  private String stringValue;
  
  public Setting(Database paramDatabase, int paramInt, String paramString)
  {
    initDbObjectBase(paramDatabase, paramInt, paramString, "setting");
  }
  
  public void setIntValue(int paramInt)
  {
    this.intValue = paramInt;
  }
  
  public int getIntValue()
  {
    return this.intValue;
  }
  
  public void setStringValue(String paramString)
  {
    this.stringValue = paramString;
  }
  
  public String getStringValue()
  {
    return this.stringValue;
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  public String getCreateSQL()
  {
    StringBuilder localStringBuilder = new StringBuilder("SET ");
    localStringBuilder.append(getSQL()).append(' ');
    if (this.stringValue != null) {
      localStringBuilder.append(this.stringValue);
    } else {
      localStringBuilder.append(this.intValue);
    }
    return localStringBuilder.toString();
  }
  
  public int getType()
  {
    return 6;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    this.database.removeMeta(paramSession, getId());
    invalidate();
  }
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("RENAME");
  }
}
