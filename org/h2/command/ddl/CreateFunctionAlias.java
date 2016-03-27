package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.FunctionAlias;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.util.StringUtils;

public class CreateFunctionAlias
  extends SchemaCommand
{
  private String aliasName;
  private String javaClassMethod;
  private boolean deterministic;
  private boolean ifNotExists;
  private boolean force;
  private String source;
  private boolean bufferResultSetToLocalTemp = true;
  
  public CreateFunctionAlias(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public int update()
  {
    this.session.commit(true);
    this.session.getUser().checkAdmin();
    Database localDatabase = this.session.getDatabase();
    if (getSchema().findFunction(this.aliasName) != null)
    {
      if (!this.ifNotExists) {
        throw DbException.get(90076, this.aliasName);
      }
    }
    else
    {
      int i = getObjectId();
      FunctionAlias localFunctionAlias;
      if (this.javaClassMethod != null) {
        localFunctionAlias = FunctionAlias.newInstance(getSchema(), i, this.aliasName, this.javaClassMethod, this.force, this.bufferResultSetToLocalTemp);
      } else {
        localFunctionAlias = FunctionAlias.newInstanceFromSource(getSchema(), i, this.aliasName, this.source, this.force, this.bufferResultSetToLocalTemp);
      }
      localFunctionAlias.setDeterministic(this.deterministic);
      localDatabase.addSchemaObject(this.session, localFunctionAlias);
    }
    return 0;
  }
  
  public void setAliasName(String paramString)
  {
    this.aliasName = paramString;
  }
  
  public void setJavaClassMethod(String paramString)
  {
    this.javaClassMethod = StringUtils.replaceAll(paramString, " ", "");
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public void setForce(boolean paramBoolean)
  {
    this.force = paramBoolean;
  }
  
  public void setDeterministic(boolean paramBoolean)
  {
    this.deterministic = paramBoolean;
  }
  
  public void setBufferResultSetToLocalTemp(boolean paramBoolean)
  {
    this.bufferResultSetToLocalTemp = paramBoolean;
  }
  
  public void setSource(String paramString)
  {
    this.source = paramString;
  }
  
  public int getType()
  {
    return 24;
  }
}
