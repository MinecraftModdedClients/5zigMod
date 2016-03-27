package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.FunctionAlias;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;

public class DropFunctionAlias
  extends SchemaCommand
{
  private String aliasName;
  private boolean ifExists;
  
  public DropFunctionAlias(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public int update()
  {
    this.session.getUser().checkAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    FunctionAlias localFunctionAlias = getSchema().findFunction(this.aliasName);
    if (localFunctionAlias == null)
    {
      if (!this.ifExists) {
        throw DbException.get(90077, this.aliasName);
      }
    }
    else {
      localDatabase.removeSchemaObject(this.session, localFunctionAlias);
    }
    return 0;
  }
  
  public void setAliasName(String paramString)
  {
    this.aliasName = paramString;
  }
  
  public void setIfExists(boolean paramBoolean)
  {
    this.ifExists = paramBoolean;
  }
  
  public int getType()
  {
    return 39;
  }
}
