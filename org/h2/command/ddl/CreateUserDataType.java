package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.engine.UserDataType;
import org.h2.message.DbException;
import org.h2.table.Column;
import org.h2.table.Table;
import org.h2.value.DataType;

public class CreateUserDataType
  extends DefineCommand
{
  private String typeName;
  private Column column;
  private boolean ifNotExists;
  
  public CreateUserDataType(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setTypeName(String paramString)
  {
    this.typeName = paramString;
  }
  
  public void setColumn(Column paramColumn)
  {
    this.column = paramColumn;
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public int update()
  {
    this.session.getUser().checkAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    this.session.getUser().checkAdmin();
    if (localDatabase.findUserDataType(this.typeName) != null)
    {
      if (this.ifNotExists) {
        return 0;
      }
      throw DbException.get(90119, this.typeName);
    }
    DataType localDataType = DataType.getTypeByName(this.typeName);
    if (localDataType != null)
    {
      if (!localDataType.hidden) {
        throw DbException.get(90119, this.typeName);
      }
      Table localTable = this.session.getDatabase().getFirstUserTable();
      if (localTable != null) {
        throw DbException.get(90119, this.typeName + " (" + localTable.getSQL() + ")");
      }
    }
    int i = getObjectId();
    UserDataType localUserDataType = new UserDataType(localDatabase, i, this.typeName);
    localUserDataType.setColumn(this.column);
    localDatabase.addDatabaseObject(this.session, localUserDataType);
    return 0;
  }
  
  public int getType()
  {
    return 33;
  }
}
