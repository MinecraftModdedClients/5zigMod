package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.schema.TriggerObject;
import org.h2.table.Table;

public class CreateTrigger
  extends SchemaCommand
{
  private String triggerName;
  private boolean ifNotExists;
  private boolean insteadOf;
  private boolean before;
  private int typeMask;
  private boolean rowBased;
  private int queueSize = 1024;
  private boolean noWait;
  private String tableName;
  private String triggerClassName;
  private boolean force;
  private boolean onRollback;
  
  public CreateTrigger(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setInsteadOf(boolean paramBoolean)
  {
    this.insteadOf = paramBoolean;
  }
  
  public void setBefore(boolean paramBoolean)
  {
    this.before = paramBoolean;
  }
  
  public void setTriggerClassName(String paramString)
  {
    this.triggerClassName = paramString;
  }
  
  public void setTypeMask(int paramInt)
  {
    this.typeMask = paramInt;
  }
  
  public void setRowBased(boolean paramBoolean)
  {
    this.rowBased = paramBoolean;
  }
  
  public void setQueueSize(int paramInt)
  {
    this.queueSize = paramInt;
  }
  
  public void setNoWait(boolean paramBoolean)
  {
    this.noWait = paramBoolean;
  }
  
  public void setTableName(String paramString)
  {
    this.tableName = paramString;
  }
  
  public void setTriggerName(String paramString)
  {
    this.triggerName = paramString;
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public int update()
  {
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    if (getSchema().findTrigger(this.triggerName) != null)
    {
      if (this.ifNotExists) {
        return 0;
      }
      throw DbException.get(90041, this.triggerName);
    }
    if (((this.typeMask & 0x8) == 8) && (this.rowBased)) {
      throw DbException.get(90005, this.triggerName);
    }
    int i = getObjectId();
    Table localTable = getSchema().getTableOrView(this.session, this.tableName);
    TriggerObject localTriggerObject = new TriggerObject(getSchema(), i, this.triggerName, localTable);
    localTriggerObject.setInsteadOf(this.insteadOf);
    localTriggerObject.setBefore(this.before);
    localTriggerObject.setNoWait(this.noWait);
    localTriggerObject.setQueueSize(this.queueSize);
    localTriggerObject.setRowBased(this.rowBased);
    localTriggerObject.setTypeMask(this.typeMask);
    localTriggerObject.setOnRollback(this.onRollback);
    localTriggerObject.setTriggerClassName(this.triggerClassName, this.force);
    localDatabase.addSchemaObject(this.session, localTriggerObject);
    localTable.addTrigger(localTriggerObject);
    return 0;
  }
  
  public void setForce(boolean paramBoolean)
  {
    this.force = paramBoolean;
  }
  
  public void setOnRollback(boolean paramBoolean)
  {
    this.onRollback = paramBoolean;
  }
  
  public int getType()
  {
    return 31;
  }
}
