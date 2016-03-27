package org.h2.command.ddl;

import java.util.ArrayList;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;

public class AlterSchemaRename
  extends DefineCommand
{
  private Schema oldSchema;
  private String newSchemaName;
  
  public AlterSchemaRename(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setOldSchema(Schema paramSchema)
  {
    this.oldSchema = paramSchema;
  }
  
  public void setNewName(String paramString)
  {
    this.newSchemaName = paramString;
  }
  
  public int update()
  {
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    if (!this.oldSchema.canDrop()) {
      throw DbException.get(90090, this.oldSchema.getName());
    }
    if ((localDatabase.findSchema(this.newSchemaName) != null) || (this.newSchemaName.equals(this.oldSchema.getName()))) {
      throw DbException.get(90078, this.newSchemaName);
    }
    this.session.getUser().checkSchemaAdmin();
    localDatabase.renameDatabaseObject(this.session, this.oldSchema, this.newSchemaName);
    ArrayList localArrayList = localDatabase.getAllSchemaObjects();
    for (SchemaObject localSchemaObject : localArrayList) {
      localDatabase.updateMeta(this.session, localSchemaObject);
    }
    return 0;
  }
  
  public int getType()
  {
    return 2;
  }
}
