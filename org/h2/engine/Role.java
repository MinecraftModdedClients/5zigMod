package org.h2.engine;

import java.util.ArrayList;
import java.util.Iterator;
import org.h2.message.DbException;
import org.h2.table.Table;

public class Role
  extends RightOwner
{
  private final boolean system;
  
  public Role(Database paramDatabase, int paramInt, String paramString, boolean paramBoolean)
  {
    super(paramDatabase, paramInt, paramString, "user");
    this.system = paramBoolean;
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  public String getCreateSQL(boolean paramBoolean)
  {
    if (this.system) {
      return null;
    }
    StringBuilder localStringBuilder = new StringBuilder("CREATE ROLE ");
    if (paramBoolean) {
      localStringBuilder.append("IF NOT EXISTS ");
    }
    localStringBuilder.append(getSQL());
    return localStringBuilder.toString();
  }
  
  public String getCreateSQL()
  {
    return getCreateSQL(false);
  }
  
  public int getType()
  {
    return 7;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    for (Iterator localIterator = this.database.getAllUsers().iterator(); localIterator.hasNext();)
    {
      localObject = (User)localIterator.next();
      localRight = ((User)localObject).getRightForRole(this);
      if (localRight != null) {
        this.database.removeDatabaseObject(paramSession, localRight);
      }
    }
    Object localObject;
    Right localRight;
    for (localIterator = this.database.getAllRoles().iterator(); localIterator.hasNext();)
    {
      localObject = (Role)localIterator.next();
      localRight = ((Role)localObject).getRightForRole(this);
      if (localRight != null) {
        this.database.removeDatabaseObject(paramSession, localRight);
      }
    }
    for (localIterator = this.database.getAllRights().iterator(); localIterator.hasNext();)
    {
      localObject = (Right)localIterator.next();
      if (((Right)localObject).getGrantee() == this) {
        this.database.removeDatabaseObject(paramSession, (DbObject)localObject);
      }
    }
    this.database.removeMeta(paramSession, getId());
    invalidate();
  }
  
  public void checkRename() {}
}
