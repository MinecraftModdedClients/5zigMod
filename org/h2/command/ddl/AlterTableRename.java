package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.Table;

public class AlterTableRename
  extends SchemaCommand
{
  private Table oldTable;
  private String newTableName;
  private boolean hidden;
  
  public AlterTableRename(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setOldTable(Table paramTable)
  {
    this.oldTable = paramTable;
  }
  
  public void setNewTableName(String paramString)
  {
    this.newTableName = paramString;
  }
  
  public int update()
  {
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    this.session.getUser().checkRight(this.oldTable, 15);
    Table localTable = getSchema().findTableOrView(this.session, this.newTableName);
    if ((localTable != null) && (this.hidden) && (this.newTableName.equals(this.oldTable.getName())))
    {
      if (!localTable.isHidden())
      {
        localTable.setHidden(this.hidden);
        this.oldTable.setHidden(true);
        localDatabase.updateMeta(this.session, this.oldTable);
      }
      return 0;
    }
    if ((localTable != null) || (this.newTableName.equals(this.oldTable.getName()))) {
      throw DbException.get(42101, this.newTableName);
    }
    if (this.oldTable.isTemporary()) {
      throw DbException.getUnsupportedException("temp table");
    }
    localDatabase.renameSchemaObject(this.session, this.oldTable, this.newTableName);
    return 0;
  }
  
  public int getType()
  {
    return 15;
  }
  
  public void setHidden(boolean paramBoolean)
  {
    this.hidden = paramBoolean;
  }
}
