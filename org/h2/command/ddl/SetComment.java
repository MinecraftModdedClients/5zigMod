package org.h2.command.ddl;

import org.h2.engine.Comment;
import org.h2.engine.Database;
import org.h2.engine.DbObject;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.Table;
import org.h2.value.Value;

public class SetComment
  extends DefineCommand
{
  private String schemaName;
  private String objectName;
  private boolean column;
  private String columnName;
  private int objectType;
  private Expression expr;
  
  public SetComment(Session paramSession)
  {
    super(paramSession);
  }
  
  public int update()
  {
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    this.session.getUser().checkAdmin();
    Object localObject1 = null;
    int i = 50000;
    if (this.schemaName == null) {
      this.schemaName = this.session.getCurrentSchemaName();
    }
    switch (this.objectType)
    {
    case 11: 
      localObject1 = localDatabase.getSchema(this.schemaName).getConstant(this.objectName);
      break;
    case 5: 
      localObject1 = localDatabase.getSchema(this.schemaName).getConstraint(this.objectName);
      break;
    case 9: 
      localObject1 = localDatabase.getSchema(this.schemaName).findFunction(this.objectName);
      i = 90077;
      break;
    case 1: 
      localObject1 = localDatabase.getSchema(this.schemaName).getIndex(this.objectName);
      break;
    case 7: 
      this.schemaName = null;
      localObject1 = localDatabase.findRole(this.objectName);
      i = 90070;
      break;
    case 10: 
      this.schemaName = null;
      localObject1 = localDatabase.findSchema(this.objectName);
      i = 90079;
      break;
    case 3: 
      localObject1 = localDatabase.getSchema(this.schemaName).getSequence(this.objectName);
      break;
    case 0: 
      localObject1 = localDatabase.getSchema(this.schemaName).getTableOrView(this.session, this.objectName);
      break;
    case 4: 
      localObject1 = localDatabase.getSchema(this.schemaName).findTrigger(this.objectName);
      i = 90042;
      break;
    case 2: 
      this.schemaName = null;
      localObject1 = localDatabase.getUser(this.objectName);
      break;
    case 12: 
      this.schemaName = null;
      localObject1 = localDatabase.findUserDataType(this.objectName);
      i = 90119;
      break;
    }
    if (localObject1 == null) {
      throw DbException.get(i, this.objectName);
    }
    String str = this.expr.optimize(this.session).getValue(this.session).getString();
    Object localObject2;
    if (this.column)
    {
      localObject2 = (Table)localObject1;
      ((Table)localObject2).getColumn(this.columnName).setComment(str);
    }
    else
    {
      ((DbObject)localObject1).setComment(str);
    }
    if ((this.column) || (this.objectType == 0) || (this.objectType == 2) || (this.objectType == 1) || (this.objectType == 5))
    {
      localDatabase.updateMeta(this.session, (DbObject)localObject1);
    }
    else
    {
      localObject2 = localDatabase.findComment((DbObject)localObject1);
      if (localObject2 == null)
      {
        if (str != null)
        {
          int j = getObjectId();
          localObject2 = new Comment(localDatabase, j, (DbObject)localObject1);
          ((Comment)localObject2).setCommentText(str);
          localDatabase.addDatabaseObject(this.session, (DbObject)localObject2);
        }
      }
      else if (str == null)
      {
        localDatabase.removeDatabaseObject(this.session, (DbObject)localObject2);
      }
      else
      {
        ((Comment)localObject2).setCommentText(str);
        localDatabase.updateMeta(this.session, (DbObject)localObject2);
      }
    }
    return 0;
  }
  
  public void setCommentExpression(Expression paramExpression)
  {
    this.expr = paramExpression;
  }
  
  public void setObjectName(String paramString)
  {
    this.objectName = paramString;
  }
  
  public void setObjectType(int paramInt)
  {
    this.objectType = paramInt;
  }
  
  public void setColumnName(String paramString)
  {
    this.columnName = paramString;
  }
  
  public void setSchemaName(String paramString)
  {
    this.schemaName = paramString;
  }
  
  public void setColumn(boolean paramBoolean)
  {
    this.column = paramBoolean;
  }
  
  public int getType()
  {
    return 52;
  }
}
