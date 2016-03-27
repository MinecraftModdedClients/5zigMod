package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Role;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;

public class CreateRole
  extends DefineCommand
{
  private String roleName;
  private boolean ifNotExists;
  
  public CreateRole(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
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
    if (localDatabase.findUser(this.roleName) != null) {
      throw DbException.get(90033, this.roleName);
    }
    if (localDatabase.findRole(this.roleName) != null)
    {
      if (this.ifNotExists) {
        return 0;
      }
      throw DbException.get(90069, this.roleName);
    }
    int i = getObjectId();
    Role localRole = new Role(localDatabase, i, this.roleName, false);
    localDatabase.addDatabaseObject(this.session, localRole);
    return 0;
  }
  
  public int getType()
  {
    return 27;
  }
}
