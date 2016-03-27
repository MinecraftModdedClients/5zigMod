package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.schema.TriggerObject;
import org.h2.table.Table;

public class DropTrigger
  extends SchemaCommand
{
  private String triggerName;
  private boolean ifExists;
  
  public DropTrigger(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setIfExists(boolean paramBoolean)
  {
    this.ifExists = paramBoolean;
  }
  
  public void setTriggerName(String paramString)
  {
    this.triggerName = paramString;
  }
  
  public int update()
  {
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    TriggerObject localTriggerObject = getSchema().findTrigger(this.triggerName);
    if (localTriggerObject == null)
    {
      if (!this.ifExists) {
        throw DbException.get(90042, this.triggerName);
      }
    }
    else
    {
      Table localTable = localTriggerObject.getTable();
      this.session.getUser().checkRight(localTable, 15);
      localDatabase.removeSchemaObject(this.session, localTriggerObject);
    }
    return 0;
  }
  
  public int getType()
  {
    return 45;
  }
}
