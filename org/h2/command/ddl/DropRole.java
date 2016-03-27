package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Role;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;

public class DropRole
  extends DefineCommand
{
  private String roleName;
  private boolean ifExists;
  
  public DropRole(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setRoleName(String paramString)
  {
    this.roleName = paramString;
  }
  
  public int update()
  {
    this.session.getUser().checkAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    if (this.roleName.equals("PUBLIC")) {
      throw DbException.get(90091, this.roleName);
    }
    Role localRole = localDatabase.findRole(this.roleName);
    if (localRole == null)
    {
      if (!this.ifExists) {
        throw DbException.get(90070, this.roleName);
      }
    }
    else {
      localDatabase.removeDatabaseObject(this.session, localRole);
    }
    return 0;
  }
  
  public void setIfExists(boolean paramBoolean)
  {
    this.ifExists = paramBoolean;
  }
  
  public int getType()
  {
    return 41;
  }
}
