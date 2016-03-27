package org.h2.engine;

import org.h2.message.DbException;
import org.h2.table.Table;

public class Right
  extends DbObjectBase
{
  public static final int SELECT = 1;
  public static final int DELETE = 2;
  public static final int INSERT = 4;
  public static final int UPDATE = 8;
  public static final int ALTER_ANY_SCHEMA = 16;
  public static final int ALL = 15;
  private Role grantedRole;
  private int grantedRight;
  private Table grantedTable;
  private RightOwner grantee;
  
  public Right(Database paramDatabase, int paramInt, RightOwner paramRightOwner, Role paramRole)
  {
    initDbObjectBase(paramDatabase, paramInt, "RIGHT_" + paramInt, "user");
    this.grantee = paramRightOwner;
    this.grantedRole = paramRole;
  }
  
  public Right(Database paramDatabase, int paramInt1, RightOwner paramRightOwner, int paramInt2, Table paramTable)
  {
    initDbObjectBase(paramDatabase, paramInt1, "" + paramInt1, "user");
    this.grantee = paramRightOwner;
    this.grantedRight = paramInt2;
    this.grantedTable = paramTable;
  }
  
  private static boolean appendRight(StringBuilder paramStringBuilder, int paramInt1, int paramInt2, String paramString, boolean paramBoolean)
  {
    if ((paramInt1 & paramInt2) != 0)
    {
      if (paramBoolean) {
        paramStringBuilder.append(", ");
      }
      paramStringBuilder.append(paramString);
      return true;
    }
    return paramBoolean;
  }
  
  public String getRights()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    if (this.grantedRight == 15)
    {
      localStringBuilder.append("ALL");
    }
    else
    {
      boolean bool = false;
      bool = appendRight(localStringBuilder, this.grantedRight, 1, "SELECT", bool);
      bool = appendRight(localStringBuilder, this.grantedRight, 2, "DELETE", bool);
      bool = appendRight(localStringBuilder, this.grantedRight, 4, "INSERT", bool);
      bool = appendRight(localStringBuilder, this.grantedRight, 16, "ALTER ANY SCHEMA", bool);
      
      appendRight(localStringBuilder, this.grantedRight, 8, "UPDATE", bool);
    }
    return localStringBuilder.toString();
  }
  
  public Role getGrantedRole()
  {
    return this.grantedRole;
  }
  
  public Table getGrantedTable()
  {
    return this.grantedTable;
  }
  
  public DbObject getGrantee()
  {
    return this.grantee;
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append("GRANT ");
    if (this.grantedRole != null)
    {
      localStringBuilder.append(this.grantedRole.getSQL());
    }
    else
    {
      localStringBuilder.append(getRights());
      if (paramTable != null) {
        localStringBuilder.append(" ON ").append(paramTable.getSQL());
      }
    }
    localStringBuilder.append(" TO ").append(this.grantee.getSQL());
    return localStringBuilder.toString();
  }
  
  public String getCreateSQL()
  {
    return getCreateSQLForCopy(this.grantedTable, null);
  }
  
  public int getType()
  {
    return 8;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    if (this.grantedTable != null) {
      this.grantee.revokeRight(this.grantedTable);
    } else {
      this.grantee.revokeRole(this.grantedRole);
    }
    this.database.removeMeta(paramSession, getId());
    this.grantedRole = null;
    this.grantedTable = null;
    this.grantee = null;
    invalidate();
  }
  
  public void checkRename()
  {
    DbException.throwInternalError();
  }
  
  public void setRightMask(int paramInt)
  {
    this.grantedRight = paramInt;
  }
  
  public int getRightMask()
  {
    return this.grantedRight;
  }
}
