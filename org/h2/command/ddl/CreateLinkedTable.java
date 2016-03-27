package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.TableLink;

public class CreateLinkedTable
  extends SchemaCommand
{
  private String tableName;
  private String driver;
  private String url;
  private String user;
  private String password;
  private String originalSchema;
  private String originalTable;
  private boolean ifNotExists;
  private String comment;
  private boolean emitUpdates;
  private boolean force;
  private boolean temporary;
  private boolean globalTemporary;
  private boolean readOnly;
  
  public CreateLinkedTable(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setTableName(String paramString)
  {
    this.tableName = paramString;
  }
  
  public void setDriver(String paramString)
  {
    this.driver = paramString;
  }
  
  public void setOriginalTable(String paramString)
  {
    this.originalTable = paramString;
  }
  
  public void setPassword(String paramString)
  {
    this.password = paramString;
  }
  
  public void setUrl(String paramString)
  {
    this.url = paramString;
  }
  
  public void setUser(String paramString)
  {
    this.user = paramString;
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public int update()
  {
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    this.session.getUser().checkAdmin();
    if (getSchema().findTableOrView(this.session, this.tableName) != null)
    {
      if (this.ifNotExists) {
        return 0;
      }
      throw DbException.get(42101, this.tableName);
    }
    int i = getObjectId();
    TableLink localTableLink = getSchema().createTableLink(i, this.tableName, this.driver, this.url, this.user, this.password, this.originalSchema, this.originalTable, this.emitUpdates, this.force);
    
    localTableLink.setTemporary(this.temporary);
    localTableLink.setGlobalTemporary(this.globalTemporary);
    localTableLink.setComment(this.comment);
    localTableLink.setReadOnly(this.readOnly);
    if ((this.temporary) && (!this.globalTemporary)) {
      this.session.addLocalTempTable(localTableLink);
    } else {
      localDatabase.addSchemaObject(this.session, localTableLink);
    }
    return 0;
  }
  
  public void setEmitUpdates(boolean paramBoolean)
  {
    this.emitUpdates = paramBoolean;
  }
  
  public void setComment(String paramString)
  {
    this.comment = paramString;
  }
  
  public void setForce(boolean paramBoolean)
  {
    this.force = paramBoolean;
  }
  
  public void setTemporary(boolean paramBoolean)
  {
    this.temporary = paramBoolean;
  }
  
  public void setGlobalTemporary(boolean paramBoolean)
  {
    this.globalTemporary = paramBoolean;
  }
  
  public void setReadOnly(boolean paramBoolean)
  {
    this.readOnly = paramBoolean;
  }
  
  public void setOriginalSchema(String paramString)
  {
    this.originalSchema = paramString;
  }
  
  public int getType()
  {
    return 26;
  }
}
