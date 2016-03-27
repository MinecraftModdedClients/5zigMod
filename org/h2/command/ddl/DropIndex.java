package org.h2.command.ddl;

import java.util.ArrayList;
import org.h2.constraint.Constraint;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.index.Index;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.table.Table;

public class DropIndex
  extends SchemaCommand
{
  private String indexName;
  private boolean ifExists;
  
  public DropIndex(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setIfExists(boolean paramBoolean)
  {
    this.ifExists = paramBoolean;
  }
  
  public void setIndexName(String paramString)
  {
    this.indexName = paramString;
  }
  
  public int update()
  {
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    Index localIndex = getSchema().findIndex(this.session, this.indexName);
    if (localIndex == null)
    {
      if (!this.ifExists) {
        throw DbException.get(42112, this.indexName);
      }
    }
    else
    {
      Table localTable = localIndex.getTable();
      this.session.getUser().checkRight(localIndex.getTable(), 15);
      Object localObject = null;
      ArrayList localArrayList = localTable.getConstraints();
      for (int i = 0; (localArrayList != null) && (i < localArrayList.size()); i++)
      {
        Constraint localConstraint = (Constraint)localArrayList.get(i);
        if (localConstraint.usesIndex(localIndex)) {
          if ("PRIMARY KEY".equals(localConstraint.getConstraintType())) {
            localObject = localConstraint;
          } else {
            throw DbException.get(90085, new String[] { this.indexName, localConstraint.getName() });
          }
        }
      }
      localIndex.getTable().setModified();
      if (localObject != null) {
        localDatabase.removeSchemaObject(this.session, (SchemaObject)localObject);
      } else {
        localDatabase.removeSchemaObject(this.session, localIndex);
      }
    }
    return 0;
  }
  
  public int getType()
  {
    return 40;
  }
}
