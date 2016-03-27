package org.h2.bnf.context;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.h2.engine.SysProperties;
import org.h2.util.New;
import org.h2.util.StringUtils;

public class DbSchema
{
  public final String name;
  public final boolean isDefault;
  public final boolean isSystem;
  public final String quotedName;
  private final DbContents contents;
  private DbTableOrView[] tables;
  private DbProcedure[] procedures;
  
  DbSchema(DbContents paramDbContents, String paramString, boolean paramBoolean)
  {
    this.contents = paramDbContents;
    this.name = paramString;
    this.quotedName = paramDbContents.quoteIdentifier(paramString);
    this.isDefault = paramBoolean;
    if (paramString == null) {
      this.isSystem = true;
    } else if ("INFORMATION_SCHEMA".equals(paramString)) {
      this.isSystem = true;
    } else if ((!paramDbContents.isH2()) && (StringUtils.toUpperEnglish(paramString).startsWith("INFO"))) {
      this.isSystem = true;
    } else if ((paramDbContents.isPostgreSQL()) && (StringUtils.toUpperEnglish(paramString).startsWith("PG_"))) {
      this.isSystem = true;
    } else if ((paramDbContents.isDerby()) && (paramString.startsWith("SYS"))) {
      this.isSystem = true;
    } else {
      this.isSystem = false;
    }
  }
  
  public DbContents getContents()
  {
    return this.contents;
  }
  
  public DbTableOrView[] getTables()
  {
    return this.tables;
  }
  
  public DbProcedure[] getProcedures()
  {
    return this.procedures;
  }
  
  public void readTables(DatabaseMetaData paramDatabaseMetaData, String[] paramArrayOfString)
    throws SQLException
  {
    ResultSet localResultSet = paramDatabaseMetaData.getTables(null, this.name, null, paramArrayOfString);
    ArrayList localArrayList = New.arrayList();
    Object localObject1;
    while (localResultSet.next())
    {
      localObject1 = new DbTableOrView(this, localResultSet);
      if ((!this.contents.isOracle()) || (((DbTableOrView)localObject1).getName().indexOf('$') <= 0)) {
        localArrayList.add(localObject1);
      }
    }
    localResultSet.close();
    this.tables = new DbTableOrView[localArrayList.size()];
    localArrayList.toArray(this.tables);
    if (this.tables.length < SysProperties.CONSOLE_MAX_TABLES_LIST_COLUMNS) {
      for (Object localObject2 : this.tables) {
        try
        {
          ((DbTableOrView)localObject2).readColumns(paramDatabaseMetaData);
        }
        catch (SQLException localSQLException) {}
      }
    }
  }
  
  public void readProcedures(DatabaseMetaData paramDatabaseMetaData)
    throws SQLException
  {
    ResultSet localResultSet = paramDatabaseMetaData.getProcedures(null, this.name, null);
    ArrayList localArrayList = New.arrayList();
    while (localResultSet.next()) {
      localArrayList.add(new DbProcedure(this, localResultSet));
    }
    localResultSet.close();
    this.procedures = new DbProcedure[localArrayList.size()];
    localArrayList.toArray(this.procedures);
    if (this.procedures.length < SysProperties.CONSOLE_MAX_PROCEDURES_LIST_COLUMNS) {
      for (DbProcedure localDbProcedure : this.procedures) {
        localDbProcedure.readParameters(paramDatabaseMetaData);
      }
    }
  }
}
