package org.h2.command.ddl;

import java.util.ArrayList;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.Table;
import org.h2.table.TableView;
import org.h2.util.StatementBuilder;

public class DropTable
  extends SchemaCommand
{
  private boolean ifExists;
  private String tableName;
  private Table table;
  private DropTable next;
  private int dropAction;
  
  public DropTable(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
    this.dropAction = (paramSession.getDatabase().getSettings().dropRestrict ? 0 : 1);
  }
  
  public void addNextDropTable(DropTable paramDropTable)
  {
    if (this.next == null) {
      this.next = paramDropTable;
    } else {
      this.next.addNextDropTable(paramDropTable);
    }
  }
  
  public void setIfExists(boolean paramBoolean)
  {
    this.ifExists = paramBoolean;
    if (this.next != null) {
      this.next.setIfExists(paramBoolean);
    }
  }
  
  public void setTableName(String paramString)
  {
    this.tableName = paramString;
  }
  
  private void prepareDrop()
  {
    this.table = getSchema().findTableOrView(this.session, this.tableName);
    if (this.table == null)
    {
      if (!this.ifExists) {
        throw DbException.get(42102, this.tableName);
      }
    }
    else
    {
      this.session.getUser().checkRight(this.table, 15);
      if (!this.table.canDrop()) {
        throw DbException.get(90118, this.tableName);
      }
      if (this.dropAction == 0)
      {
        ArrayList localArrayList = this.table.getViews();
        if ((localArrayList != null) && (localArrayList.size() > 0))
        {
          StatementBuilder localStatementBuilder = new StatementBuilder();
          for (TableView localTableView : localArrayList)
          {
            localStatementBuilder.appendExceptFirst(", ");
            localStatementBuilder.append(localTableView.getName());
          }
          throw DbException.get(90107, new String[] { this.tableName, localStatementBuilder.toString() });
        }
      }
      this.table.lock(this.session, true, true);
    }
    if (this.next != null) {
      this.next.prepareDrop();
    }
  }
  
  private void executeDrop()
  {
    this.table = getSchema().findTableOrView(this.session, this.tableName);
    if (this.table != null)
    {
      this.table.setModified();
      Database localDatabase = this.session.getDatabase();
      localDatabase.lockMeta(this.session);
      localDatabase.removeSchemaObject(this.session, this.table);
    }
    if (this.next != null) {
      this.next.executeDrop();
    }
  }
  
  public int update()
  {
    this.session.commit(true);
    prepareDrop();
    executeDrop();
    return 0;
  }
  
  public void setDropAction(int paramInt)
  {
    this.dropAction = paramInt;
    if (this.next != null) {
      this.next.setDropAction(paramInt);
    }
  }
  
  public int getType()
  {
    return 44;
  }
}
