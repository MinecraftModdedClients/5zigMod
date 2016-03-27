package org.h2.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.h2.command.ddl.CreateTableData;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.mvstore.db.MVTableEngine;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;

public abstract class TableBase
  extends Table
{
  private final String tableEngine;
  private List<String> tableEngineParams = new ArrayList();
  private final boolean globalTemporary;
  
  public TableBase(CreateTableData paramCreateTableData)
  {
    super(paramCreateTableData.schema, paramCreateTableData.id, paramCreateTableData.tableName, paramCreateTableData.persistIndexes, paramCreateTableData.persistData);
    
    this.tableEngine = paramCreateTableData.tableEngine;
    this.globalTemporary = paramCreateTableData.globalTemporary;
    if (paramCreateTableData.tableEngineParams != null) {
      this.tableEngineParams = paramCreateTableData.tableEngineParams;
    }
    setTemporary(paramCreateTableData.temporary);
    Column[] arrayOfColumn = new Column[paramCreateTableData.columns.size()];
    paramCreateTableData.columns.toArray(arrayOfColumn);
    setColumns(arrayOfColumn);
  }
  
  public String getDropSQL()
  {
    return "DROP TABLE IF EXISTS " + getSQL() + " CASCADE";
  }
  
  public String getCreateSQL()
  {
    Database localDatabase = getDatabase();
    if (localDatabase == null) {
      return null;
    }
    StatementBuilder localStatementBuilder = new StatementBuilder("CREATE ");
    if (isTemporary())
    {
      if (isGlobalTemporary()) {
        localStatementBuilder.append("GLOBAL ");
      } else {
        localStatementBuilder.append("LOCAL ");
      }
      localStatementBuilder.append("TEMPORARY ");
    }
    else if (isPersistIndexes())
    {
      localStatementBuilder.append("CACHED ");
    }
    else
    {
      localStatementBuilder.append("MEMORY ");
    }
    localStatementBuilder.append("TABLE ");
    if (this.isHidden) {
      localStatementBuilder.append("IF NOT EXISTS ");
    }
    localStatementBuilder.append(getSQL());
    if (this.comment != null) {
      localStatementBuilder.append(" COMMENT ").append(StringUtils.quoteStringSQL(this.comment));
    }
    localStatementBuilder.append("(\n    ");
    for (Object localObject2 : this.columns)
    {
      localStatementBuilder.appendExceptFirst(",\n    ");
      localStatementBuilder.append(((Column)localObject2).getCreateSQL());
    }
    localStatementBuilder.append("\n)");
    String str;
    if (this.tableEngine != null)
    {
      ??? = localDatabase.getSettings();
      str = ((DbSettings)???).defaultTableEngine;
      if ((str == null) && (((DbSettings)???).mvStore)) {
        str = MVTableEngine.class.getName();
      }
      if ((str == null) || (!this.tableEngine.endsWith(str)))
      {
        localStatementBuilder.append("\nENGINE ");
        localStatementBuilder.append(StringUtils.quoteIdentifier(this.tableEngine));
      }
    }
    if (!this.tableEngineParams.isEmpty())
    {
      localStatementBuilder.append("\nWITH ");
      localStatementBuilder.resetCount();
      for (??? = this.tableEngineParams.iterator(); ((Iterator)???).hasNext();)
      {
        str = (String)((Iterator)???).next();
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append(StringUtils.quoteIdentifier(str));
      }
    }
    if ((!isPersistIndexes()) && (!isPersistData())) {
      localStatementBuilder.append("\nNOT PERSISTENT");
    }
    if (this.isHidden) {
      localStatementBuilder.append("\nHIDDEN");
    }
    return localStatementBuilder.toString();
  }
  
  public boolean isGlobalTemporary()
  {
    return this.globalTemporary;
  }
}
