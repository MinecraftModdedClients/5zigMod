package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;

public class DropSchema
  extends DefineCommand
{
  private String schemaName;
  private boolean ifExists;
  
  public DropSchema(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setSchemaName(String paramString)
  {
    this.schemaName = paramString;
  }
  
  public int update()
  {
    this.session.getUser().checkSchemaAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    Schema localSchema = localDatabase.findSchema(this.schemaName);
    if (localSchema == null)
    {
      if (!this.ifExists) {
        throw DbException.get(90079, this.schemaName);
      }
    }
    else
    {
      if (!localSchema.canDrop()) {
        throw DbException.get(90090, this.schemaName);
      }
      localDatabase.removeDatabaseObject(this.session, localSchema);
    }
    return 0;
  }
  
  public void setIfExists(boolean paramBoolean)
  {
    this.ifExists = paramBoolean;
  }
  
  public int getType()
  {
    return 42;
  }
}
