package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.engine.UserAggregate;
import org.h2.message.DbException;

public class DropAggregate
  extends DefineCommand
{
  private String name;
  private boolean ifExists;
  
  public DropAggregate(Session paramSession)
  {
    super(paramSession);
  }
  
  public int update()
  {
    this.session.getUser().checkAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    UserAggregate localUserAggregate = localDatabase.findAggregate(this.name);
    if (localUserAggregate == null)
    {
      if (!this.ifExists) {
        throw DbException.get(90132, this.name);
      }
    }
    else {
      localDatabase.removeDatabaseObject(this.session, localUserAggregate);
    }
    return 0;
  }
  
  public void setName(String paramString)
  {
    this.name = paramString;
  }
  
  public void setIfExists(boolean paramBoolean)
  {
    this.ifExists = paramBoolean;
  }
  
  public int getType()
  {
    return 36;
  }
}
