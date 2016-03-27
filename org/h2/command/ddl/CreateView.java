package org.h2.command.ddl;

import java.util.ArrayList;
import org.h2.command.dml.Query;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.Table;
import org.h2.table.TableView;

public class CreateView
  extends SchemaCommand
{
  private Query select;
  private String viewName;
  private boolean ifNotExists;
  private String selectSQL;
  private String[] columnNames;
  private String comment;
  private boolean orReplace;
  private boolean force;
  
  public CreateView(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setViewName(String paramString)
  {
    this.viewName = paramString;
  }
  
  public void setSelect(Query paramQuery)
  {
    this.select = paramQuery;
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public void setSelectSQL(String paramString)
  {
    this.selectSQL = paramString;
  }
  
  public void setColumnNames(String[] paramArrayOfString)
  {
    this.columnNames = paramArrayOfString;
  }
  
  public void setComment(String paramString)
  {
    this.comment = paramString;
  }
  
  public void setOrReplace(boolean paramBoolean)
  {
    this.orReplace = paramBoolean;
  }
  
  public void setForce(boolean paramBoolean)
  {
    this.force = paramBoolean;
  }
  
  public int update()
  {
    this.session.commit(true);
    this.session.getUser().checkAdmin();
    Database localDatabase = this.session.getDatabase();
    TableView localTableView = null;
    Table localTable = getSchema().findTableOrView(this.session, this.viewName);
    if (localTable != null)
    {
      if (this.ifNotExists) {
        return 0;
      }
      if ((!this.orReplace) || (!"VIEW".equals(localTable.getTableType()))) {
        throw DbException.get(90038, this.viewName);
      }
      localTableView = (TableView)localTable;
    }
    int i = getObjectId();
    String str;
    if (this.select == null)
    {
      str = this.selectSQL;
    }
    else
    {
      localObject1 = this.select.getParameters();
      if ((localObject1 != null) && (((ArrayList)localObject1).size() > 0)) {
        throw DbException.getUnsupportedException("parameters in views");
      }
      str = this.select.getPlanSQL();
    }
    Object localObject1 = localDatabase.getSystemSession();
    try
    {
      if (localTableView == null)
      {
        Schema localSchema = this.session.getDatabase().getSchema(this.session.getCurrentSchemaName());
        ((Session)localObject1).setCurrentSchema(localSchema);
        localTableView = new TableView(getSchema(), i, this.viewName, str, null, this.columnNames, (Session)localObject1, false);
      }
      else
      {
        localTableView.replace(str, this.columnNames, (Session)localObject1, false, this.force);
        localTableView.setModified();
      }
    }
    finally
    {
      ((Session)localObject1).setCurrentSchema(localDatabase.getSchema("PUBLIC"));
    }
    if (this.comment != null) {
      localTableView.setComment(this.comment);
    }
    if (localTable == null) {
      localDatabase.addSchemaObject(this.session, localTableView);
    } else {
      localDatabase.updateMeta(this.session, localTableView);
    }
    return 0;
  }
  
  public int getType()
  {
    return 34;
  }
}
