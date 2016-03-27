package org.h2.tools;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.Driver;
import org.h2.engine.Constants;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.JdbcUtils;
import org.h2.util.ScriptReader;
import org.h2.util.StringUtils;
import org.h2.util.Tool;

public class RunScript
  extends Tool
{
  private boolean showResults;
  private boolean checkResults;
  
  public static void main(String... paramVarArgs)
    throws SQLException
  {
    new RunScript().runTool(paramVarArgs);
  }
  
  public void runTool(String... paramVarArgs)
    throws SQLException
  {
    String str1 = null;
    String str2 = "";
    String str3 = "";
    String str4 = "backup.sql";
    String str5 = null;
    boolean bool = false;
    int i = 0;
    for (int j = 0; (paramVarArgs != null) && (j < paramVarArgs.length); j++)
    {
      String str6 = paramVarArgs[j];
      if (str6.equals("-url"))
      {
        str1 = paramVarArgs[(++j)];
      }
      else if (str6.equals("-user"))
      {
        str2 = paramVarArgs[(++j)];
      }
      else if (str6.equals("-password"))
      {
        str3 = paramVarArgs[(++j)];
      }
      else if (str6.equals("-continueOnError"))
      {
        bool = true;
      }
      else if (str6.equals("-checkResults"))
      {
        this.checkResults = true;
      }
      else if (str6.equals("-showResults"))
      {
        this.showResults = true;
      }
      else if (str6.equals("-script"))
      {
        str4 = paramVarArgs[(++j)];
      }
      else if (str6.equals("-time"))
      {
        i = 1;
      }
      else
      {
        Object localObject;
        if (str6.equals("-driver"))
        {
          localObject = paramVarArgs[(++j)];
          JdbcUtils.loadUserClass((String)localObject);
        }
        else if (str6.equals("-options"))
        {
          localObject = new StringBuilder();
          j++;
          for (; j < paramVarArgs.length; j++) {
            ((StringBuilder)localObject).append(' ').append(paramVarArgs[j]);
          }
          str5 = ((StringBuilder)localObject).toString();
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
    }
    if (str1 == null)
    {
      showUsage();
      throw new SQLException("URL not set");
    }
    long l = System.currentTimeMillis();
    if (str5 != null) {
      processRunscript(str1, str2, str3, str4, str5);
    } else {
      process(str1, str2, str3, str4, null, bool);
    }
    if (i != 0)
    {
      l = System.currentTimeMillis() - l;
      this.out.println("Done in " + l + " ms");
    }
  }
  
  public static ResultSet execute(Connection paramConnection, Reader paramReader)
    throws SQLException
  {
    Statement localStatement = paramConnection.createStatement();
    ResultSet localResultSet = null;
    ScriptReader localScriptReader = new ScriptReader(paramReader);
    for (;;)
    {
      String str = localScriptReader.readStatement();
      if (str == null) {
        break;
      }
      if (str.trim().length() != 0)
      {
        boolean bool = localStatement.execute(str);
        if (bool)
        {
          if (localResultSet != null)
          {
            localResultSet.close();
            localResultSet = null;
          }
          localResultSet = localStatement.getResultSet();
        }
      }
    }
    return localResultSet;
  }
  
  private void process(Connection paramConnection, String paramString, boolean paramBoolean, Charset paramCharset)
    throws SQLException, IOException
  {
    Object localObject1 = FileUtils.newInputStream(paramString);
    String str = FileUtils.getParent(paramString);
    try
    {
      localObject1 = new BufferedInputStream((InputStream)localObject1, 4096);
      InputStreamReader localInputStreamReader = new InputStreamReader((InputStream)localObject1, paramCharset);
      process(paramConnection, paramBoolean, str, localInputStreamReader, paramCharset);
    }
    finally
    {
      IOUtils.closeSilently((InputStream)localObject1);
    }
  }
  
  private void process(Connection paramConnection, boolean paramBoolean, String paramString, Reader paramReader, Charset paramCharset)
    throws SQLException, IOException
  {
    Statement localStatement = paramConnection.createStatement();
    ScriptReader localScriptReader = new ScriptReader(paramReader);
    for (;;)
    {
      Object localObject = localScriptReader.readStatement();
      if (localObject == null) {
        break;
      }
      String str1 = ((String)localObject).trim();
      if (str1.length() != 0) {
        if ((str1.startsWith("@")) && (StringUtils.toUpperEnglish(str1).startsWith("@INCLUDE")))
        {
          localObject = str1;
          localObject = ((String)localObject).substring("@INCLUDE".length()).trim();
          if (!FileUtils.isAbsolute((String)localObject)) {
            localObject = paramString + SysProperties.FILE_SEPARATOR + (String)localObject;
          }
          process(paramConnection, (String)localObject, paramBoolean, paramCharset);
        }
        else
        {
          try
          {
            if ((this.showResults) && (!str1.startsWith("-->"))) {
              this.out.print((String)localObject + ";");
            }
            if ((this.showResults) || (this.checkResults))
            {
              boolean bool = localStatement.execute((String)localObject);
              if (bool)
              {
                ResultSet localResultSet = localStatement.getResultSet();
                int i = localResultSet.getMetaData().getColumnCount();
                StringBuilder localStringBuilder = new StringBuilder();
                String str3;
                while (localResultSet.next())
                {
                  localStringBuilder.append("\n-->");
                  for (int j = 0; j < i; j++)
                  {
                    str3 = localResultSet.getString(j + 1);
                    if (str3 != null)
                    {
                      str3 = StringUtils.replaceAll(str3, "\r\n", "\n");
                      str3 = StringUtils.replaceAll(str3, "\n", "\n-->    ");
                      str3 = StringUtils.replaceAll(str3, "\r", "\r-->    ");
                    }
                    localStringBuilder.append(' ').append(str3);
                  }
                }
                localStringBuilder.append("\n;");
                String str2 = localStringBuilder.toString();
                if (this.showResults) {
                  this.out.print(str2);
                }
                if (this.checkResults)
                {
                  str3 = localScriptReader.readStatement() + ";";
                  str3 = StringUtils.replaceAll(str3, "\r\n", "\n");
                  str3 = StringUtils.replaceAll(str3, "\r", "\n");
                  if (!str3.equals(str2))
                  {
                    str3 = StringUtils.replaceAll(str3, " ", "+");
                    str2 = StringUtils.replaceAll(str2, " ", "+");
                    throw new SQLException("Unexpected output for:\n" + ((String)localObject).trim() + "\nGot:\n" + str2 + "\nExpected:\n" + str3);
                  }
                }
              }
            }
            else
            {
              localStatement.execute((String)localObject);
            }
          }
          catch (Exception localException)
          {
            if (paramBoolean) {
              localException.printStackTrace(this.out);
            } else {
              throw DbException.toSQLException(localException);
            }
          }
        }
      }
    }
  }
  
  private static void processRunscript(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5)
    throws SQLException
  {
    Connection localConnection = null;
    Statement localStatement = null;
    try
    {
      Driver.load();
      localConnection = DriverManager.getConnection(paramString1, paramString2, paramString3);
      localStatement = localConnection.createStatement();
      String str = "RUNSCRIPT FROM '" + paramString4 + "' " + paramString5;
      localStatement.execute(str);
    }
    finally
    {
      JdbcUtils.closeSilently(localStatement);
      JdbcUtils.closeSilently(localConnection);
    }
  }
  
  public static void execute(String paramString1, String paramString2, String paramString3, String paramString4, Charset paramCharset, boolean paramBoolean)
    throws SQLException
  {
    new RunScript().process(paramString1, paramString2, paramString3, paramString4, paramCharset, paramBoolean);
  }
  
  void process(String paramString1, String paramString2, String paramString3, String paramString4, Charset paramCharset, boolean paramBoolean)
    throws SQLException
  {
    try
    {
      Driver.load();
      Connection localConnection = DriverManager.getConnection(paramString1, paramString2, paramString3);
      if (paramCharset == null) {
        paramCharset = Constants.UTF8;
      }
      try
      {
        process(localConnection, paramString4, paramBoolean, paramCharset);
      }
      finally
      {
        localConnection.close();
      }
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, paramString4);
    }
  }
}
