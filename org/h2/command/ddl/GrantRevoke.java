package org.h2.command.ddl;

import java.util.ArrayList;
import org.h2.engine.Database;
import org.h2.engine.Right;
import org.h2.engine.RightOwner;
import org.h2.engine.Role;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.table.Table;
import org.h2.util.New;

public class GrantRevoke
  extends DefineCommand
{
  private ArrayList<String> roleNames;
  private int operationType;
  private int rightMask;
  private final ArrayList<Table> tables = New.arrayList();
  private RightOwner grantee;
  
  public GrantRevoke(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setOperationType(int paramInt)
  {
    this.operationType = paramInt;
  }
  
  public void addRight(int paramInt)
  {
    this.rightMask |= paramInt;
  }
  
  public void addRoleName(String paramString)
  {
    if (this.roleNames == null) {
      this.roleNames = New.arrayList();
    }
    this.roleNames.add(paramString);
  }
  
  public void setGranteeName(String paramString)
  {
    Database localDatabase = this.session.getDatabase();
    this.grantee = localDatabase.findUser(paramString);
    if (this.grantee == null)
    {
      this.grantee = localDatabase.findRole(paramString);
      if (this.grantee == null) {
        throw DbException.get(90071, paramString);
      }
    }
  }
  
  public int update()
  {
    this.session.getUser().checkAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    if (this.roleNames != null) {
      for (String str : this.roleNames)
      {
        Role localRole = localDatabase.findRole(str);
        if (localRole == null) {
          throw DbException.get(90070, str);
        }
        if (this.operationType == 49) {
          grantRole(localRole);
        } else if (this.operationType == 50) {
          revokeRole(localRole);
        } else {
          DbException.throwInternalError("type=" + this.operationType);
        }
      }
    } else if (this.operationType == 49) {
      grantRight();
    } else if (this.operationType == 50) {
      revokeRight();
    } else {
      DbException.throwInternalError("type=" + this.operationType);
    }
    return 0;
  }
  
  private void grantRight()
  {
    Database localDatabase = this.session.getDatabase();
    for (Table localTable : this.tables)
    {
      Right localRight = this.grantee.getRightForTable(localTable);
      if (localRight == null)
      {
        int i = getObjectId();
        localRight = new Right(localDatabase, i, this.grantee, this.rightMask, localTable);
        this.grantee.grantRight(localTable, localRight);
        localDatabase.addDatabaseObject(this.session, localRight);
      }
      else
      {
        localRight.setRightMask(localRight.getRightMask() | this.rightMask);
        localDatabase.updateMeta(this.session, localRight);
      }
    }
  }
  
  private void grantRole(Role paramRole)
  {
    if ((paramRole != this.grantee) && (this.grantee.isRoleGranted(paramRole))) {
      return;
    }
    if ((this.grantee instanceof Role))
    {
      localObject = (Role)this.grantee;
      if (paramRole.isRoleGranted((Role)localObject)) {
        throw DbException.get(90074, paramRole.getSQL());
      }
    }
    Object localObject = this.session.getDatabase();
    int i = getObjectId();
    Right localRight = new Right((Database)localObject, i, this.grantee, paramRole);
    ((Database)localObject).addDatabaseObject(this.session, localRight);
    this.grantee.grantRole(paramRole, localRight);
  }
  
  private void revokeRight()
  {
    for (Table localTable : this.tables)
    {
      Right localRight = this.grantee.getRightForTable(localTable);
      if (localRight != null)
      {
        int i = localRight.getRightMask();
        int j = i & (this.rightMask ^ 0xFFFFFFFF);
        Database localDatabase = this.session.getDatabase();
        if (j == 0)
        {
          localDatabase.removeDatabaseObject(this.session, localRight);
        }
        else
        {
          localRight.setRightMask(j);
          localDatabase.updateMeta(this.session, localRight);
        }
      }
    }
  }
  
  private void revokeRole(Role paramRole)
  {
    Right localRight = this.grantee.getRightForRole(paramRole);
    if (localRight == null) {
      return;
    }
    Database localDatabase = this.session.getDatabase();
    localDatabase.removeDatabaseObject(this.session, localRight);
  }
  
  public boolean isTransactional()
  {
    return false;
  }
  
  public void addTable(Table paramTable)
  {
    this.tables.add(paramTable);
  }
  
  public int getType()
  {
    return this.operationType;
  }
  
  public boolean isRoleMode()
  {
    return this.roleNames != null;
  }
  
  public boolean isRightMode()
  {
    return this.rightMask != 0;
  }
}
