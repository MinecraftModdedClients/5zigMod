package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.schema.Sequence;

public class DropSequence
  extends SchemaCommand
{
  private String sequenceName;
  private boolean ifExists;
  
  public DropSequence(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setIfExists(boolean paramBoolean)
  {
    this.ifExists = paramBoolean;
  }
  
  public void setSequenceName(String paramString)
  {
    this.sequenceName = paramString;
  }
  
  public int update()
  {
    this.session.getUser().checkAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    Sequence localSequence = getSchema().findSequence(this.sequenceName);
    if (localSequence == null)
    {
      if (!this.ifExists) {
        throw DbException.get(90036, this.sequenceName);
      }
    }
    else
    {
      if (localSequence.getBelongsToTable()) {
        throw DbException.get(90082, this.sequenceName);
      }
      localDatabase.removeSchemaObject(this.session, localSequence);
    }
    return 0;
  }
  
  public int getType()
  {
    return 43;
  }
}
