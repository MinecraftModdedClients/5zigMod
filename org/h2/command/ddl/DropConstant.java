package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Constant;
import org.h2.schema.Schema;

public class DropConstant
  extends SchemaCommand
{
  private String constantName;
  private boolean ifExists;
  
  public DropConstant(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setIfExists(boolean paramBoolean)
  {
    this.ifExists = paramBoolean;
  }
  
  public void setConstantName(String paramString)
  {
    this.constantName = paramString;
  }
  
  public int update()
  {
    this.session.getUser().checkAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    Constant localConstant = getSchema().findConstant(this.constantName);
    if (localConstant == null)
    {
      if (!this.ifExists) {
        throw DbException.get(90115, this.constantName);
      }
    }
    else {
      localDatabase.removeSchemaObject(this.session, localConstant);
    }
    return 0;
  }
  
  public int getType()
  {
    return 37;
  }
}
