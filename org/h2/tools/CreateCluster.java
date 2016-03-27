package org.h2.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.Driver;
import org.h2.message.DbException;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.JdbcUtils;
import org.h2.util.Tool;

public class CreateCluster
  extends Tool
{
  public static void main(String... paramVarArgs)
    throws SQLException
  {
    new CreateCluster().runTool(paramVarArgs);
  }
  
  public void runTool(String... paramVarArgs)
    throws SQLException
  {
    String str1 = null;
    String str2 = null;
    String str3 = "";
    String str4 = "";
    String str5 = null;
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++)
    {
      String str6 = paramVarArgs[i];
      if (str6.equals("-urlSource"))
      {
        str1 = paramVarArgs[(++i)];
      }
      else if (str6.equals("-urlTarget"))
      {
        str2 = paramVarArgs[(++i)];
      }
      else if (str6.equals("-user"))
      {
        str3 = paramVarArgs[(++i)];
      }
      else if (str6.equals("-password"))
      {
        str4 = paramVarArgs[(++i)];
      }
      else if (str6.equals("-serverList"))
      {
        str5 = paramVarArgs[(++i)];
      }
      else
      {
        if ((str6.equals("-help")) || (str6.equals("-?")))
        {
          showUsage();
          return;
        }
        showUsageAndThrowUnsupportedOption(str6);
      }
    }
    if ((str1 == null) || (str2 == null) || (str5 == null))
    {
      showUsage();
      throw new SQLException("Source URL, target URL, or server list not set");
    }
    process(str1, str2, str3, str4, str5);
  }
  
  public void execute(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5)
    throws SQLException
  {
    process(paramString1, paramString2, paramString3, paramString4, paramString5);
  }
  
  private void process(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5)
    throws SQLException
  {
    Connection localConnection1 = null;Connection localConnection2 = null;
    Statement localStatement1 = null;Statement localStatement2 = null;
    String str = "backup.sql";
    try
    {
      Driver.load();
      
      int i = 1;
      try
      {
        localConnection2 = DriverManager.getConnection(paramString2 + ";IFEXISTS=TRUE;CLUSTER=" + "TRUE", paramString3, paramString4);
        
        Statement localStatement3 = localConnection2.createStatement();
        localStatement3.execute("DROP ALL OBJECTS DELETE FILES");
        localStatement3.close();
        i = 0;
        localConnection2.close();
      }
      catch (SQLException localSQLException)
      {
        if (localSQLException.getErrorCode() == 90013) {
          i = 0;
        } else {
          throw localSQLException;
        }
      }
      if (i != 0) {
        throw new SQLException("Target database must not yet exist. Please delete it first: " + paramString2);
      }
      localConnection1 = DriverManager.getConnection(paramString1 + ";CLUSTER=''", paramString3, paramString4);
      
      localStatement1 = localConnection1.createStatement();
      
      localStatement1.execute("SET EXCLUSIVE 2");
      try
      {
        Script localScript = new Script();
        localScript.setOut(this.out);
        OutputStream localOutputStream = null;
        try
        {
          localOutputStream = FileUtils.newOutputStream(str, false);
          Script.process(localConnection1, localOutputStream);
        }
        catch (IOException localIOException)
        {
          throw DbException.convertIOException(localIOException, null);
        }
        finally
        {
          IOUtils.closeSilently(localOutputStream);
        }
        localConnection2 = DriverManager.getConnection(paramString2 + ";CLUSTER=''", paramString3, paramString4);
        
        localStatement2 = localConnection2.createStatement();
        localStatement2.execute("DROP ALL OBJECTS DELETE FILES");
        localConnection2.close();
        
        RunScript localRunScript = new RunScript();
        localRunScript.setOut(this.out);
        localRunScript.process(paramString2, paramString3, paramString4, str, null, false);
        
        localConnection2 = DriverManager.getConnection(paramString2, paramString3, paramString4);
        localStatement2 = localConnection2.createStatement();
        
        localStatement1.executeUpdate("SET CLUSTER '" + paramString5 + "'");
        localStatement2.executeUpdate("SET CLUSTER '" + paramString5 + "'");
      }
      finally
      {
        localStatement1.execute("SET EXCLUSIVE FALSE");
      }
    }
    finally
    {
      FileUtils.delete(str);
      JdbcUtils.closeSilently(localStatement1);
      JdbcUtils.closeSilently(localStatement2);
      JdbcUtils.closeSilently(localConnection1);
      JdbcUtils.closeSilently(localConnection2);
    }
  }
}
