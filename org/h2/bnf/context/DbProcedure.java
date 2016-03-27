package org.h2.bnf.context;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.h2.util.New;

public class DbProcedure
{
  private final DbSchema schema;
  private final String name;
  private final String quotedName;
  private boolean returnsResult;
  private DbColumn[] parameters;
  
  public DbProcedure(DbSchema paramDbSchema, ResultSet paramResultSet)
    throws SQLException
  {
    this.schema = paramDbSchema;
    this.name = paramResultSet.getString("PROCEDURE_NAME");
    this.returnsResult = (paramResultSet.getShort("PROCEDURE_TYPE") == 2);
    
    this.quotedName = paramDbSchema.getContents().quoteIdentifier(this.name);
  }
  
  public DbSchema getSchema()
  {
    return this.schema;
  }
  
  public DbColumn[] getParameters()
  {
    return this.parameters;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public String getQuotedName()
  {
    return this.quotedName;
  }
  
  public boolean isReturnsResult()
  {
    return this.returnsResult;
  }
  
  void readParameters(DatabaseMetaData paramDatabaseMetaData)
    throws SQLException
  {
    ResultSet localResultSet = paramDatabaseMetaData.getProcedureColumns(null, this.schema.name, this.name, null);
    ArrayList localArrayList = New.arrayList();
    while (localResultSet.next())
    {
      DbColumn localDbColumn1 = DbColumn.getProcedureColumn(this.schema.getContents(), localResultSet);
      if (localDbColumn1.getPosition() > 0) {
        localArrayList.add(localDbColumn1);
      }
    }
    localResultSet.close();
    this.parameters = new DbColumn[localArrayList.size()];
    for (int i = 0; i < this.parameters.length; i++)
    {
      DbColumn localDbColumn2 = (DbColumn)localArrayList.get(i);
      if ((localDbColumn2.getPosition() > 0) && (localDbColumn2.getPosition() <= this.parameters.length)) {
        this.parameters[(localDbColumn2.getPosition() - 1)] = localDbColumn2;
      }
    }
  }
}
