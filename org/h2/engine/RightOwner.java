package org.h2.engine;

import java.util.HashMap;
import org.h2.table.Table;
import org.h2.util.New;

public abstract class RightOwner
  extends DbObjectBase
{
  private HashMap<Role, Right> grantedRoles;
  private HashMap<Table, Right> grantedRights;
  
  protected RightOwner(Database paramDatabase, int paramInt, String paramString1, String paramString2)
  {
    initDbObjectBase(paramDatabase, paramInt, paramString1, paramString2);
  }
  
  public boolean isRoleGranted(Role paramRole)
  {
    if (paramRole == this) {
      return true;
    }
    if (this.grantedRoles != null) {
      for (Role localRole : this.grantedRoles.keySet())
      {
        if (localRole == paramRole) {
          return true;
        }
        if (localRole.isRoleGranted(paramRole)) {
          return true;
        }
      }
    }
    return false;
  }
  
  boolean isRightGrantedRecursive(Table paramTable, int paramInt)
  {
    if (this.grantedRights != null)
    {
      Right localRight = (Right)this.grantedRights.get(paramTable);
      if ((localRight != null) && 
        ((localRight.getRightMask() & paramInt) == paramInt)) {
        return true;
      }
    }
    if (this.grantedRoles != null) {
      for (Role localRole : this.grantedRoles.keySet()) {
        if (localRole.isRightGrantedRecursive(paramTable, paramInt)) {
          return true;
        }
      }
    }
    return false;
  }
  
  public void grantRight(Table paramTable, Right paramRight)
  {
    if (this.grantedRights == null) {
      this.grantedRights = New.hashMap();
    }
    this.grantedRights.put(paramTable, paramRight);
  }
  
  void revokeRight(Table paramTable)
  {
    if (this.grantedRights == null) {
      return;
    }
    this.grantedRights.remove(paramTable);
    if (this.grantedRights.size() == 0) {
      this.grantedRights = null;
    }
  }
  
  public void grantRole(Role paramRole, Right paramRight)
  {
    if (this.grantedRoles == null) {
      this.grantedRoles = New.hashMap();
    }
    this.grantedRoles.put(paramRole, paramRight);
  }
  
  void revokeRole(Role paramRole)
  {
    if (this.grantedRoles == null) {
      return;
    }
    Right localRight = (Right)this.grantedRoles.get(paramRole);
    if (localRight == null) {
      return;
    }
    this.grantedRoles.remove(paramRole);
    if (this.grantedRoles.size() == 0) {
      this.grantedRoles = null;
    }
  }
  
  public Right getRightForTable(Table paramTable)
  {
    if (this.grantedRights == null) {
      return null;
    }
    return (Right)this.grantedRights.get(paramTable);
  }
  
  public Right getRightForRole(Role paramRole)
  {
    if (this.grantedRoles == null) {
      return null;
    }
    return (Right)this.grantedRoles.get(paramRole);
  }
}
