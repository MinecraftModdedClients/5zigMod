package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.DbObject;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.Table;
import org.h2.table.TableView;

public class DropView
  extends SchemaCommand
{
  private String viewName;
  private boolean ifExists;
  private int dropAction;
  
  public DropView(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
    this.dropAction = (paramSession.getDatabase().getSettings().dropRestrict ? 0 : 1);
  }
  
  public void setIfExists(boolean paramBoolean)
  {
    this.ifExists = paramBoolean;
  }
  
  public void setDropAction(int paramInt)
  {
    this.dropAction = paramInt;
  }
  
  public void setViewName(String paramString)
  {
    this.viewName = paramString;
  }
  
  public int update()
  {
    this.session.commit(true);
    Table localTable = getSchema().findTableOrView(this.session, this.viewName);
    if (localTable == null)
    {
      if (!this.ifExists) {
        throw DbException.get(90037, this.viewName);
      }
    }
    else
    {
      if (!"VIEW".equals(localTable.getTableType())) {
        throw DbException.get(90037, this.viewName);
      }
      this.session.getUser().checkRight(localTable, 15);
      if (this.dropAction == 0) {
        for (DbObject localDbObject : localTable.getChildren()) {
          if ((localDbObject instanceof TableView)) {
            throw DbException.get(90107, new String[] { this.viewName, localDbObject.getName() });
          }
        }
      }
      localTable.lock(this.session, true, true);
      this.session.getDatabase().removeSchemaObject(this.session, localTable);
    }
    return 0;
  }
  
  public int getType()
  {
    return 48;
  }
}
