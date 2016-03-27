package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.index.Index;
import org.h2.message.DbException;
import org.h2.schema.Schema;

public class AlterIndexRename
  extends DefineCommand
{
  private Index oldIndex;
  private String newIndexName;
  
  public AlterIndexRename(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setOldIndex(Index paramIndex)
  {
    this.oldIndex = paramIndex;
  }
  
  public void setNewName(String paramString)
  {
    this.newIndexName = paramString;
  }
  
  public int update()
  {
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    Schema localSchema = this.oldIndex.getSchema();
    if ((localSchema.findIndex(this.session, this.newIndexName) != null) || (this.newIndexName.equals(this.oldIndex.getName()))) {
      throw DbException.get(42111, this.newIndexName);
    }
    this.session.getUser().checkRight(this.oldIndex.getTable(), 15);
    localDatabase.renameSchemaObject(this.session, this.oldIndex, this.newIndexName);
    return 0;
  }
  
  public int getType()
  {
    return 1;
  }
}
