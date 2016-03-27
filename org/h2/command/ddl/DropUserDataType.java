package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.engine.UserDataType;
import org.h2.message.DbException;

public class DropUserDataType
  extends DefineCommand
{
  private String typeName;
  private boolean ifExists;
  
  public DropUserDataType(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setIfExists(boolean paramBoolean)
  {
    this.ifExists = paramBoolean;
  }
  
  public int update()
  {
    this.session.getUser().checkAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    UserDataType localUserDataType = localDatabase.findUserDataType(this.typeName);
    if (localUserDataType == null)
    {
      if (!this.ifExists) {
        throw DbException.get(90120, this.typeName);
      }
    }
    else {
      localDatabase.removeDatabaseObject(this.session, localUserDataType);
    }
    return 0;
  }
  
  public void setTypeName(String paramString)
  {
    this.typeName = paramString;
  }
  
  public int getType()
  {
    return 47;
  }
}
