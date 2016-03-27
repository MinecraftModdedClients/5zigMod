package org.h2.bnf.context;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.h2.util.New;

public class DbTableOrView
{
  private final DbSchema schema;
  private final String name;
  private final String quotedName;
  private final boolean isView;
  private DbColumn[] columns;
  
  public DbTableOrView(DbSchema paramDbSchema, ResultSet paramResultSet)
    throws SQLException
  {
    this.schema = paramDbSchema;
    this.name = paramResultSet.getString("TABLE_NAME");
    String str = paramResultSet.getString("TABLE_TYPE");
    this.isView = "VIEW".equals(str);
    this.quotedName = paramDbSchema.getContents().quoteIdentifier(this.name);
  }
  
  public DbSchema getSchema()
  {
    return this.schema;
  }
  
  public DbColumn[] getColumns()
  {
    return this.columns;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public boolean isView()
  {
    return this.isView;
  }
  
  public String getQuotedName()
  {
    return this.quotedName;
  }
  
  public void readColumns(DatabaseMetaData paramDatabaseMetaData)
    throws SQLException
  {
    ResultSet localResultSet = paramDatabaseMetaData.getColumns(null, this.schema.name, this.name, null);
    ArrayList localArrayList = New.arrayList();
    while (localResultSet.next())
    {
      DbColumn localDbColumn = DbColumn.getColumn(this.schema.getContents(), localResultSet);
      localArrayList.add(localDbColumn);
    }
    localResultSet.close();
    this.columns = new DbColumn[localArrayList.size()];
    localArrayList.toArray(this.columns);
  }
}
