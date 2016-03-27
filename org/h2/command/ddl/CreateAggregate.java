package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.engine.UserAggregate;
import org.h2.message.DbException;
import org.h2.schema.Schema;

public class CreateAggregate
  extends DefineCommand
{
  private Schema schema;
  private String name;
  private String javaClassMethod;
  private boolean ifNotExists;
  private boolean force;
  
  public CreateAggregate(Session paramSession)
  {
    super(paramSession);
  }
  
  public int update()
  {
    this.session.commit(true);
    this.session.getUser().checkAdmin();
    Database localDatabase = this.session.getDatabase();
    if ((localDatabase.findAggregate(this.name) != null) || (this.schema.findFunction(this.name) != null))
    {
      if (!this.ifNotExists) {
        throw DbException.get(90076, this.name);
      }
    }
    else
    {
      int i = getObjectId();
      UserAggregate localUserAggregate = new UserAggregate(localDatabase, i, this.name, this.javaClassMethod, this.force);
      
      localDatabase.addDatabaseObject(this.session, localUserAggregate);
    }
    return 0;
  }
  
  public void setSchema(Schema paramSchema)
  {
    this.schema = paramSchema;
  }
  
  public void setName(String paramString)
  {
    this.name = paramString;
  }
  
  public void setJavaClassMethod(String paramString)
  {
    this.javaClassMethod = paramString;
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public void setForce(boolean paramBoolean)
  {
    this.force = paramBoolean;
  }
  
  public int getType()
  {
    return 22;
  }
}
