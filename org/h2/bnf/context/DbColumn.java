package org.h2.bnf.context;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DbColumn
{
  private final String name;
  private final String quotedName;
  private final String dataType;
  private int position;
  
  private DbColumn(DbContents paramDbContents, ResultSet paramResultSet, boolean paramBoolean)
    throws SQLException
  {
    this.name = paramResultSet.getString("COLUMN_NAME");
    this.quotedName = paramDbContents.quoteIdentifier(this.name);
    String str1 = paramResultSet.getString("TYPE_NAME");
    String str2;
    if (paramBoolean) {
      str2 = "PRECISION";
    } else {
      str2 = "COLUMN_SIZE";
    }
    int i = paramResultSet.getInt(str2);
    this.position = paramResultSet.getInt("ORDINAL_POSITION");
    boolean bool = paramDbContents.isSQLite();
    if ((i > 0) && (!bool))
    {
      str1 = str1 + "(" + i;
      String str3;
      if (paramBoolean) {
        str3 = "SCALE";
      } else {
        str3 = "DECIMAL_DIGITS";
      }
      int j = paramResultSet.getInt(str3);
      if (j > 0) {
        str1 = str1 + ", " + j;
      }
      str1 = str1 + ")";
    }
    if (paramResultSet.getInt("NULLABLE") == 0) {
      str1 = str1 + " NOT NULL";
    }
    this.dataType = str1;
  }
  
  public static DbColumn getProcedureColumn(DbContents paramDbContents, ResultSet paramResultSet)
    throws SQLException
  {
    return new DbColumn(paramDbContents, paramResultSet, true);
  }
  
  public static DbColumn getColumn(DbContents paramDbContents, ResultSet paramResultSet)
    throws SQLException
  {
    return new DbColumn(paramDbContents, paramResultSet, false);
  }
  
  public String getDataType()
  {
    return this.dataType;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public String getQuotedName()
  {
    return this.quotedName;
  }
  
  public int getPosition()
  {
    return this.position;
  }
}
