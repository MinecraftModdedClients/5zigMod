package org.h2.command.dml;

import org.h2.command.ddl.SchemaCommand;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.Table;

public class AlterTableSet
  extends SchemaCommand
{
  private String tableName;
  private final int type;
  private final boolean value;
  private boolean checkExisting;
  
  public AlterTableSet(Session paramSession, Schema paramSchema, int paramInt, boolean paramBoolean)
  {
    super(paramSession, paramSchema);
    this.type = paramInt;
    this.value = paramBoolean;
  }
  
  public void setCheckExisting(boolean paramBoolean)
  {
    this.checkExisting = paramBoolean;
  }
  
  public boolean isTransactional()
  {
    return true;
  }
  
  public void setTableName(String paramString)
  {
    this.tableName = paramString;
  }
  
  public int update()
  {
    Table localTable = getSchema().getTableOrView(this.session, this.tableName);
    this.session.getUser().checkRight(localTable, 15);
    localTable.lock(this.session, true, true);
    switch (this.type)
    {
    case 55: 
      localTable.setCheckForeignKeyConstraints(this.session, this.value, this.value ? this.checkExisting : false);
      
      break;
    default: 
      DbException.throwInternalError("type=" + this.type);
    }
    return 0;
  }
  
  public int getType()
  {
    return this.type;
  }
}
