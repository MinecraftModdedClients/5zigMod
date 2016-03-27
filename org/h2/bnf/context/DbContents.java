package org.h2.bnf.context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.h2.command.Parser;
import org.h2.util.New;
import org.h2.util.StringUtils;

public class DbContents
{
  private DbSchema[] schemas;
  private DbSchema defaultSchema;
  private boolean isOracle;
  private boolean isH2;
  private boolean isPostgreSQL;
  private boolean isDerby;
  private boolean isSQLite;
  private boolean isH2ModeMySQL;
  private boolean isMySQL;
  private boolean isFirebird;
  private boolean isMSSQLServer;
  
  public DbSchema getDefaultSchema()
  {
    return this.defaultSchema;
  }
  
  public boolean isDerby()
  {
    return this.isDerby;
  }
  
  public boolean isFirebird()
  {
    return this.isFirebird;
  }
  
  public boolean isH2()
  {
    return this.isH2;
  }
  
  public boolean isH2ModeMySQL()
  {
    return this.isH2ModeMySQL;
  }
  
  public boolean isMSSQLServer()
  {
    return this.isMSSQLServer;
  }
  
  public boolean isMySQL()
  {
    return this.isMySQL;
  }
  
  public boolean isOracle()
  {
    return this.isOracle;
  }
  
  public boolean isPostgreSQL()
  {
    return this.isPostgreSQL;
  }
  
  public boolean isSQLite()
  {
    return this.isSQLite;
  }
  
  public DbSchema[] getSchemas()
  {
    return this.schemas;
  }
  
  public synchronized void readContents(String paramString, Connection paramConnection)
    throws SQLException
  {
    this.isH2 = paramString.startsWith("jdbc:h2:");
    if (this.isH2)
    {
      localObject1 = paramConnection.prepareStatement("SELECT UPPER(VALUE) FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME=?");
      
      ((PreparedStatement)localObject1).setString(1, "MODE");
      localObject2 = ((PreparedStatement)localObject1).executeQuery();
      ((ResultSet)localObject2).next();
      if ("MYSQL".equals(((ResultSet)localObject2).getString(1))) {
        this.isH2ModeMySQL = true;
      }
      ((ResultSet)localObject2).close();
      ((PreparedStatement)localObject1).close();
    }
    this.isSQLite = paramString.startsWith("jdbc:sqlite:");
    this.isOracle = paramString.startsWith("jdbc:oracle:");
    
    this.isPostgreSQL = ((paramString.startsWith("jdbc:postgresql:")) || (paramString.startsWith("jdbc:vertica:")));
    
    this.isMySQL = paramString.startsWith("jdbc:mysql:");
    this.isDerby = paramString.startsWith("jdbc:derby:");
    this.isFirebird = paramString.startsWith("jdbc:firebirdsql:");
    this.isMSSQLServer = paramString.startsWith("jdbc:sqlserver:");
    Object localObject1 = paramConnection.getMetaData();
    Object localObject2 = getDefaultSchemaName((DatabaseMetaData)localObject1);
    String[] arrayOfString1 = getSchemaNames((DatabaseMetaData)localObject1);
    this.schemas = new DbSchema[arrayOfString1.length];
    Object localObject3;
    String[] arrayOfString2;
    for (int i = 0; i < arrayOfString1.length; i++)
    {
      localObject3 = arrayOfString1[i];
      boolean bool = (localObject2 == null) || (((String)localObject2).equals(localObject3));
      
      DbSchema localDbSchema = new DbSchema(this, (String)localObject3, bool);
      if (bool) {
        this.defaultSchema = localDbSchema;
      }
      this.schemas[i] = localDbSchema;
      arrayOfString2 = new String[] { "TABLE", "SYSTEM TABLE", "VIEW", "SYSTEM VIEW", "TABLE LINK", "SYNONYM", "EXTERNAL" };
      
      localDbSchema.readTables((DatabaseMetaData)localObject1, arrayOfString2);
      if (!this.isPostgreSQL) {
        localDbSchema.readProcedures((DatabaseMetaData)localObject1);
      }
    }
    if (this.defaultSchema == null)
    {
      String str = null;
      for (arrayOfString2 : this.schemas)
      {
        if ("dbo".equals(arrayOfString2.name))
        {
          this.defaultSchema = arrayOfString2;
          break;
        }
        if ((this.defaultSchema == null) || (str == null) || (arrayOfString2.name.length() < str.length()))
        {
          str = arrayOfString2.name;
          this.defaultSchema = arrayOfString2;
        }
      }
    }
  }
  
  private String[] getSchemaNames(DatabaseMetaData paramDatabaseMetaData)
    throws SQLException
  {
    if ((this.isMySQL) || (this.isSQLite)) {
      return new String[] { "" };
    }
    if (this.isFirebird) {
      return new String[] { null };
    }
    ResultSet localResultSet = paramDatabaseMetaData.getSchemas();
    ArrayList localArrayList = New.arrayList();
    while (localResultSet.next())
    {
      localObject = localResultSet.getString("TABLE_SCHEM");
      String[] arrayOfString1 = null;
      if (this.isOracle) {
        arrayOfString1 = new String[] { "CTXSYS", "DIP", "DBSNMP", "DMSYS", "EXFSYS", "FLOWS_020100", "FLOWS_FILES", "MDDATA", "MDSYS", "MGMT_VIEW", "OLAPSYS", "ORDSYS", "ORDPLUGINS", "OUTLN", "SI_INFORMTN_SCHEMA", "SYS", "SYSMAN", "SYSTEM", "TSMSYS", "WMSYS", "XDB" };
      } else if (this.isMSSQLServer) {
        arrayOfString1 = new String[] { "sys", "db_accessadmin", "db_backupoperator", "db_datareader", "db_datawriter", "db_ddladmin", "db_denydatareader", "db_denydatawriter", "db_owner", "db_securityadmin" };
      }
      if (arrayOfString1 != null) {
        for (String str : arrayOfString1) {
          if (str.equals(localObject))
          {
            localObject = null;
            break;
          }
        }
      }
      if (localObject != null) {
        localArrayList.add(localObject);
      }
    }
    localResultSet.close();
    Object localObject = new String[localArrayList.size()];
    localArrayList.toArray((Object[])localObject);
    return (String[])localObject;
  }
  
  private String getDefaultSchemaName(DatabaseMetaData paramDatabaseMetaData)
  {
    String str = "";
    try
    {
      if (this.isOracle) {
        return paramDatabaseMetaData.getUserName();
      }
      if (this.isPostgreSQL) {
        return "public";
      }
      if (this.isMySQL) {
        return "";
      }
      if (this.isDerby) {
        return StringUtils.toUpperEnglish(paramDatabaseMetaData.getUserName());
      }
      if (this.isFirebird) {
        return null;
      }
      ResultSet localResultSet = paramDatabaseMetaData.getSchemas();
      int i = localResultSet.findColumn("IS_DEFAULT");
      while (localResultSet.next()) {
        if (localResultSet.getBoolean(i)) {
          str = localResultSet.getString("TABLE_SCHEM");
        }
      }
    }
    catch (SQLException localSQLException) {}
    return str;
  }
  
  public String quoteIdentifier(String paramString)
  {
    if (paramString == null) {
      return null;
    }
    if ((this.isH2) && (!this.isH2ModeMySQL)) {
      return Parser.quoteIdentifier(paramString);
    }
    return StringUtils.toUpperEnglish(paramString);
  }
}
