package org.h2.engine;

import org.h2.message.DbException;
import org.h2.table.Column;
import org.h2.table.Table;

public class UserDataType
  extends DbObjectBase
{
  private Column column;
  
  public UserDataType(Database paramDatabase, int paramInt, String paramString)
  {
    initDbObjectBase(paramDatabase, paramInt, paramString, "database");
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  public String getDropSQL()
  {
    return "DROP DOMAIN IF EXISTS " + getSQL();
  }
  
  public String getCreateSQL()
  {
    return "CREATE DOMAIN " + getSQL() + " AS " + this.column.getCreateSQL();
  }
  
  public Column getColumn()
  {
    return this.column;
  }
  
  public int getType()
  {
    return 12;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    this.database.removeMeta(paramSession, getId());
  }
  
  public void checkRename() {}
  
  public void setColumn(Column paramColumn)
  {
    this.column = paramColumn;
  }
}
