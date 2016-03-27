package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.DbObject;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.table.Column;
import org.h2.table.Table;

public class AlterTableRenameColumn
  extends DefineCommand
{
  private Table table;
  private Column column;
  private String newName;
  
  public AlterTableRenameColumn(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setTable(Table paramTable)
  {
    this.table = paramTable;
  }
  
  public void setColumn(Column paramColumn)
  {
    this.column = paramColumn;
  }
  
  public void setNewColumnName(String paramString)
  {
    this.newName = paramString;
  }
  
  public int update()
  {
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    this.session.getUser().checkRight(this.table, 15);
    this.table.checkSupportAlter();
    
    Expression localExpression = this.column.getCheckConstraint(this.session, this.newName);
    this.table.renameColumn(this.column, this.newName);
    this.column.removeCheckConstraint();
    this.column.addCheckConstraint(this.session, localExpression);
    this.table.setModified();
    localDatabase.updateMeta(this.session, this.table);
    for (DbObject localDbObject : this.table.getChildren()) {
      if (localDbObject.getCreateSQL() != null) {
        localDatabase.updateMeta(this.session, localDbObject);
      }
    }
    return 0;
  }
  
  public int getType()
  {
    return 16;
  }
}
