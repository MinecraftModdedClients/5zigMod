package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;

public class DropUser
  extends DefineCommand
{
  private boolean ifExists;
  private String userName;
  
  public DropUser(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setIfExists(boolean paramBoolean)
  {
    this.ifExists = paramBoolean;
  }
  
  public void setUserName(String paramString)
  {
    this.userName = paramString;
  }
  
  public int update()
  {
    this.session.getUser().checkAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    User localUser1 = localDatabase.findUser(this.userName);
    if (localUser1 == null)
    {
      if (!this.ifExists) {
        throw DbException.get(90032, this.userName);
      }
    }
    else
    {
      if (localUser1 == this.session.getUser())
      {
        int i = 0;
        for (User localUser2 : localDatabase.getAllUsers()) {
          if (localUser2.isAdmin()) {
            i++;
          }
        }
        if (i == 1) {
          throw DbException.get(90019);
        }
      }
      localUser1.checkOwnsNoSchemas();
      localDatabase.removeDatabaseObject(this.session, localUser1);
    }
    return 0;
  }
  
  public boolean isTransactional()
  {
    return false;
  }
  
  public int getType()
  {
    return 46;
  }
}
