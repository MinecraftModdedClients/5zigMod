package org.h2.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.Driver;
import org.h2.message.DbException;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.JdbcUtils;
import org.h2.util.StringUtils;
import org.h2.util.Tool;

public class Script
  extends Tool
{
  public static void main(String... paramVarArgs)
    throws SQLException
  {
    new Script().runTool(paramVarArgs);
  }
  
  public void runTool(String... paramVarArgs)
    throws SQLException
  {
    String str1 = null;
    String str2 = "";
    String str3 = "";
    String str4 = "backup.sql";
    String str5 = null;String str6 = null;
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++)
    {
      String str7 = paramVarArgs[i];
      if (str7.equals("-url"))
      {
        str1 = paramVarArgs[(++i)];
      }
      else if (str7.equals("-user"))
      {
        str2 = paramVarArgs[(++i)];
      }
      else if (str7.equals("-password"))
      {
        str3 = paramVarArgs[(++i)];
      }
      else if (str7.equals("-script"))
      {
        str4 = paramVarArgs[(++i)];
      }
      else if (str7.equals("-options"))
      {
        StringBuilder localStringBuilder1 = new StringBuilder();
        StringBuilder localStringBuilder2 = new StringBuilder();
        i++;
        for (; i < paramVarArgs.length; i++)
        {
          String str8 = paramVarArgs[i];
          String str9 = StringUtils.toUpperEnglish(str8);
          if (("SIMPLE".equals(str9)) || (str9.startsWith("NO")) || ("DROP".equals(str9)))
          {
            localStringBuilder1.append(' ');
            localStringBuilder1.append(paramVarArgs[i]);
          }
          else
          {
            localStringBuilder2.append(' ');
            localStringBuilder2.append(paramVarArgs[i]);
          }
        }
        str5 = localStringBuilder1.toString();
        str6 = localStringBuilder2.toString();
      }
      else
      {
        if ((str7.equals("-help")) || (str7.equals("-?")))
        {
          showUsage();
          return;
        }
        showUsageAndThrowUnsupportedOption(str7);
      }
    }
    if (str1 == null)
    {
      showUsage();
      throw new SQLException("URL not set");
    }
    if (str5 != null) {
      processScript(str1, str2, str3, str4, str5, str6);
    } else {
      execute(str1, str2, str3, str4);
    }
  }
  
  private static void processScript(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5, String paramString6)
    throws SQLException
  {
    Connection localConnection = null;
    Statement localStatement = null;
    try
    {
      Driver.load();
      localConnection = DriverManager.getConnection(paramString1, paramString2, paramString3);
      localStatement = localConnection.createStatement();
      String str = "SCRIPT " + paramString5 + " TO '" + paramString4 + "' " + paramString6;
      localStatement.execute(str);
    }
    finally
    {
      JdbcUtils.closeSilently(localStatement);
      JdbcUtils.closeSilently(localConnection);
    }
  }
  
  public static void execute(String paramString1, String paramString2, String paramString3, String paramString4)
    throws SQLException
  {
    OutputStream localOutputStream = null;
    try
    {
      localOutputStream = FileUtils.newOutputStream(paramString4, false);
      execute(paramString1, paramString2, paramString3, localOutputStream);
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
    finally
    {
      IOUtils.closeSilently(localOutputStream);
    }
  }
  
  public static void execute(String paramString1, String paramString2, String paramString3, OutputStream paramOutputStream)
    throws SQLException
  {
    Connection localConnection = null;
    try
    {
      Driver.load();
      localConnection = DriverManager.getConnection(paramString1, paramString2, paramString3);
      process(localConnection, paramOutputStream);
    }
    finally
    {
      JdbcUtils.closeSilently(localConnection);
    }
  }
  
  static void process(Connection paramConnection, OutputStream paramOutputStream)
    throws SQLException
  {
    Statement localStatement = null;
    try
    {
      localStatement = paramConnection.createStatement();
      PrintWriter localPrintWriter = new PrintWriter(IOUtils.getBufferedWriter(paramOutputStream));
      ResultSet localResultSet = localStatement.executeQuery("SCRIPT");
      while (localResultSet.next())
      {
        String str = localResultSet.getString(1);
        localPrintWriter.println(str);
      }
      localPrintWriter.flush();
    }
    finally
    {
      JdbcUtils.closeSilently(localStatement);
    }
  }
}
