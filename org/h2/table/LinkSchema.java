package org.h2.table;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.message.DbException;
import org.h2.tools.SimpleResultSet;
import org.h2.util.JdbcUtils;
import org.h2.util.StringUtils;

public class LinkSchema
{
  public static ResultSet linkSchema(Connection paramConnection, String paramString1, String paramString2, String paramString3, String paramString4, String paramString5, String paramString6)
  {
    Connection localConnection = null;
    Statement localStatement = null;
    ResultSet localResultSet = null;
    SimpleResultSet localSimpleResultSet = new SimpleResultSet();
    localSimpleResultSet.setAutoClose(false);
    localSimpleResultSet.addColumn("TABLE_NAME", 12, Integer.MAX_VALUE, 0);
    try
    {
      localConnection = JdbcUtils.getConnection(paramString2, paramString3, paramString4, paramString5);
      localStatement = paramConnection.createStatement();
      localStatement.execute("CREATE SCHEMA IF NOT EXISTS " + StringUtils.quoteIdentifier(paramString1));
      
      localResultSet = localConnection.getMetaData().getTables(null, paramString6, null, null);
      while (localResultSet.next())
      {
        String str = localResultSet.getString("TABLE_NAME");
        StringBuilder localStringBuilder = new StringBuilder();
        localStringBuilder.append("DROP TABLE IF EXISTS ").append(StringUtils.quoteIdentifier(paramString1)).append('.').append(StringUtils.quoteIdentifier(str));
        
        localStatement.execute(localStringBuilder.toString());
        localStringBuilder = new StringBuilder();
        localStringBuilder.append("CREATE LINKED TABLE ").append(StringUtils.quoteIdentifier(paramString1)).append('.').append(StringUtils.quoteIdentifier(str)).append('(').append(StringUtils.quoteStringSQL(paramString2)).append(", ").append(StringUtils.quoteStringSQL(paramString3)).append(", ").append(StringUtils.quoteStringSQL(paramString4)).append(", ").append(StringUtils.quoteStringSQL(paramString5)).append(", ").append(StringUtils.quoteStringSQL(str)).append(')');
        
        localStatement.execute(localStringBuilder.toString());
        localSimpleResultSet.addRow(new Object[] { str });
      }
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
    finally
    {
      JdbcUtils.closeSilently(localResultSet);
      JdbcUtils.closeSilently(localConnection);
      JdbcUtils.closeSilently(localStatement);
    }
    return localSimpleResultSet;
  }
}
