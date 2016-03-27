package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;

public class CreateSchema
  extends DefineCommand
{
  private String schemaName;
  private String authorization;
  private boolean ifNotExists;
  
  public CreateSchema(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public int update()
  {
    this.session.getUser().checkSchemaAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    User localUser = localDatabase.getUser(this.authorization);
    if (!localDatabase.isStarting()) {
      localUser.checkSchemaAdmin();
    }
    if (localDatabase.findSchema(this.schemaName) != null)
    {
      if (this.ifNotExists) {
        return 0;
      }
      throw DbException.get(90078, this.schemaName);
    }
    int i = getObjectId();
    Schema localSchema = new Schema(localDatabase, i, this.schemaName, localUser, false);
    localDatabase.addDatabaseObject(this.session, localSchema);
    return 0;
  }
  
  public void setSchemaName(String paramString)
  {
    this.schemaName = paramString;
  }
  
  public void setAuthorization(String paramString)
  {
    this.authorization = paramString;
  }
  
  public int getType()
  {
    return 28;
  }
}
