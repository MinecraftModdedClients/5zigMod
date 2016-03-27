package org.h2.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import org.h2.Driver;
import org.h2.engine.Constants;
import org.h2.server.web.ConnectionInfo;
import org.h2.util.JdbcUtils;
import org.h2.util.New;
import org.h2.util.ScriptReader;
import org.h2.util.SortedProperties;
import org.h2.util.StringUtils;
import org.h2.util.Tool;
import org.h2.util.Utils;

public class Shell
  extends Tool
  implements Runnable
{
  private static final int MAX_ROW_BUFFER = 5000;
  private static final int HISTORY_COUNT = 20;
  private static final char BOX_VERTICAL = '|';
  private PrintStream err = System.err;
  private InputStream in = System.in;
  private BufferedReader reader;
  private Connection conn;
  private Statement stat;
  private boolean listMode;
  private int maxColumnSize = 100;
  private final ArrayList<String> history = New.arrayList();
  private boolean stopHide;
  private String serverPropertiesDir = "~";
  
  public static void main(String... paramVarArgs)
    throws SQLException
  {
    new Shell().runTool(paramVarArgs);
  }
  
  public void setErr(PrintStream paramPrintStream)
  {
    this.err = paramPrintStream;
  }
  
  public void setIn(InputStream paramInputStream)
  {
    this.in = paramInputStream;
  }
  
  public void setInReader(BufferedReader paramBufferedReader)
  {
    this.reader = paramBufferedReader;
  }
  
  public void runTool(String... paramVarArgs)
    throws SQLException
  {
    String str1 = null;
    String str2 = "";
    String str3 = "";
    String str4 = null;
    String str5;
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++)
    {
      str5 = paramVarArgs[i];
      if (str5.equals("-url"))
      {
        str1 = paramVarArgs[(++i)];
      }
      else if (str5.equals("-user"))
      {
        str2 = paramVarArgs[(++i)];
      }
      else if (str5.equals("-password"))
      {
        str3 = paramVarArgs[(++i)];
      }
      else if (str5.equals("-driver"))
      {
        String str6 = paramVarArgs[(++i)];
        JdbcUtils.loadUserClass(str6);
      }
      else if (str5.equals("-sql"))
      {
        str4 = paramVarArgs[(++i)];
      }
      else if (str5.equals("-properties"))
      {
        this.serverPropertiesDir = paramVarArgs[(++i)];
      }
      else
      {
        if ((str5.equals("-help")) || (str5.equals("-?")))
        {
          showUsage();
          return;
        }
        if (str5.equals("-list")) {
          this.listMode = true;
        } else {
          showUsageAndThrowUnsupportedOption(str5);
        }
      }
    }
    if (str1 != null)
    {
      Driver.load();
      this.conn = DriverManager.getConnection(str1, str2, str3);
      this.stat = this.conn.createStatement();
    }
    if (str4 == null)
    {
      promptLoop();
    }
    else
    {
      ScriptReader localScriptReader = new ScriptReader(new StringReader(str4));
      for (;;)
      {
        str5 = localScriptReader.readStatement();
        if (str5 == null) {
          break;
        }
        execute(str5);
      }
      if (this.conn != null) {
        this.conn.close();
      }
    }
  }
  
  public void runTool(Connection paramConnection, String... paramVarArgs)
    throws SQLException
  {
    this.conn = paramConnection;
    this.stat = paramConnection.createStatement();
    runTool(paramVarArgs);
  }
  
  private void showHelp()
  {
    println("Commands are case insensitive; SQL statements end with ';'");
    println("help or ?      Display this help");
    println("list           Toggle result list / stack trace mode");
    println("maxwidth       Set maximum column width (default is 100)");
    println("autocommit     Enable or disable autocommit");
    println("history        Show the last 20 statements");
    println("quit or exit   Close the connection and exit");
    println("");
  }
  
  private void promptLoop()
  {
    println("");
    println("Welcome to H2 Shell " + Constants.getFullVersion());
    println("Exit with Ctrl+C");
    if (this.conn != null) {
      showHelp();
    }
    Object localObject = null;
    if (this.reader == null) {
      this.reader = new BufferedReader(new InputStreamReader(this.in));
    }
    try
    {
      for (;;)
      {
        if (this.conn == null)
        {
          connect();
          showHelp();
        }
        if (localObject == null) {
          print("sql> ");
        } else {
          print("...> ");
        }
        String str1 = readLine();
        if (str1 == null) {
          break;
        }
        String str2 = str1.trim();
        if (str2.length() != 0)
        {
          boolean bool = str2.endsWith(";");
          if (bool)
          {
            str1 = str1.substring(0, str1.lastIndexOf(';'));
            str2 = str2.substring(0, str2.length() - 1);
          }
          String str3 = StringUtils.toLowerEnglish(str2);
          if ((!"exit".equals(str3)) && ("quit".equals(str3))) {
            break;
          }
          if (("help".equals(str3)) || ("?".equals(str3)))
          {
            showHelp();
          }
          else if ("list".equals(str3))
          {
            this.listMode = (!this.listMode);
            println("Result list mode is now " + (this.listMode ? "on" : "off"));
          }
          else
          {
            int k;
            if ("history".equals(str3))
            {
              int i = 0;
              for (k = this.history.size(); i < k; i++)
              {
                String str4 = (String)this.history.get(i);
                str4 = str4.replace('\n', ' ').replace('\r', ' ');
                println("#" + (1 + i) + ": " + str4);
              }
              if (this.history.size() > 0) {
                println("To re-run a statement, type the number and press and enter");
              } else {
                println("No history");
              }
            }
            else if (str3.startsWith("autocommit"))
            {
              str3 = str3.substring("autocommit".length()).trim();
              if ("true".equals(str3)) {
                this.conn.setAutoCommit(true);
              } else if ("false".equals(str3)) {
                this.conn.setAutoCommit(false);
              } else {
                println("Usage: autocommit [true|false]");
              }
              println("Autocommit is now " + this.conn.getAutoCommit());
            }
            else if (str3.startsWith("maxwidth"))
            {
              str3 = str3.substring("maxwidth".length()).trim();
              try
              {
                this.maxColumnSize = Integer.parseInt(str3);
              }
              catch (NumberFormatException localNumberFormatException)
              {
                println("Usage: maxwidth <integer value>");
              }
              println("Maximum column width is now " + this.maxColumnSize);
            }
            else
            {
              int j = 1;
              if (localObject == null)
              {
                if (StringUtils.isNumber(str1))
                {
                  k = Integer.parseInt(str1);
                  if ((k == 0) || (k > this.history.size()))
                  {
                    println("Not found");
                  }
                  else
                  {
                    localObject = (String)this.history.get(k - 1);
                    j = 0;
                    println((String)localObject);
                    bool = true;
                  }
                }
                else
                {
                  localObject = str1;
                }
              }
              else {
                localObject = (String)localObject + "\n" + str1;
              }
              if (bool)
              {
                if (j != 0)
                {
                  this.history.add(0, localObject);
                  if (this.history.size() > 20) {
                    this.history.remove(20);
                  }
                }
                execute((String)localObject);
                localObject = null;
              }
            }
          }
        }
      }
    }
    catch (SQLException localSQLException1)
    {
      for (;;)
      {
        println("SQL Exception: " + localSQLException1.getMessage());
        localObject = null;
      }
    }
    catch (IOException localIOException)
    {
      println(localIOException.getMessage());
    }
    catch (Exception localException)
    {
      println("Exception: " + localException.toString());
      localException.printStackTrace(this.err);
    }
    if (this.conn != null) {
      try
      {
        this.conn.close();
        println("Connection closed");
      }
      catch (SQLException localSQLException2)
      {
        println("SQL Exception: " + localSQLException2.getMessage());
        localSQLException2.printStackTrace(this.err);
      }
    }
  }
  
  private void connect()
    throws IOException, SQLException
  {
    String str1 = "jdbc:h2:~/test";
    String str2 = "";
    String str3 = null;
    try
    {
      Object localObject1;
      if ("null".equals(this.serverPropertiesDir)) {
        localObject1 = new Properties();
      } else {
        localObject1 = SortedProperties.loadProperties(this.serverPropertiesDir + "/" + ".h2.server.properties");
      }
      Object localObject2 = null;
      int i = 0;
      for (int j = 0;; j++)
      {
        String str5 = ((Properties)localObject1).getProperty(String.valueOf(j));
        if (str5 == null) {
          break;
        }
        i = 1;
        localObject2 = str5;
      }
      if (i != 0)
      {
        ConnectionInfo localConnectionInfo = new ConnectionInfo((String)localObject2);
        str1 = localConnectionInfo.url;
        str2 = localConnectionInfo.user;
        str3 = localConnectionInfo.driver;
      }
    }
    catch (IOException localIOException) {}
    println("[Enter]   " + str1);
    print("URL       ");
    str1 = readLine(str1).trim();
    if (str3 == null) {
      str3 = JdbcUtils.getDriver(str1);
    }
    if (str3 != null) {
      println("[Enter]   " + str3);
    }
    print("Driver    ");
    str3 = readLine(str3).trim();
    println("[Enter]   " + str2);
    print("User      ");
    str2 = readLine(str2);
    println("[Enter]   Hide");
    print("Password  ");
    String str4 = readLine();
    if (str4.length() == 0) {
      str4 = readPassword();
    }
    this.conn = JdbcUtils.getConnection(str3, str1, str2, str4);
    this.stat = this.conn.createStatement();
    println("Connected");
  }
  
  protected void print(String paramString)
  {
    this.out.print(paramString);
    this.out.flush();
  }
  
  private void println(String paramString)
  {
    this.out.println(paramString);
    this.out.flush();
  }
  
  private String readPassword()
    throws IOException
  {
    Object localObject2;
    try
    {
      Object localObject1 = Utils.callStaticMethod("java.lang.System.console", new Object[0]);
      print("Password  ");
      localObject2 = (char[])Utils.callMethod(localObject1, "readPassword", new Object[0]);
      return localObject2 == null ? null : new String((char[])localObject2);
    }
    catch (Exception localException)
    {
      Thread localThread = new Thread(this, "Password hider");
      this.stopHide = false;
      localThread.start();
      print("Password  > ");
      localObject2 = readLine();
      this.stopHide = true;
      try
      {
        localThread.join();
      }
      catch (InterruptedException localInterruptedException) {}
      print("\b\b");
    }
    return (String)localObject2;
  }
  
  public void run()
  {
    while (!this.stopHide)
    {
      print("\b\b><");
      try
      {
        Thread.sleep(10L);
      }
      catch (InterruptedException localInterruptedException) {}
    }
  }
  
  private String readLine(String paramString)
    throws IOException
  {
    String str = readLine();
    return str.length() == 0 ? paramString : str;
  }
  
  private String readLine()
    throws IOException
  {
    String str = this.reader.readLine();
    if (str == null) {
      throw new IOException("Aborted");
    }
    return str;
  }
  
  private void execute(String paramString)
  {
    if (paramString.trim().length() == 0) {
      return;
    }
    long l = System.currentTimeMillis();
    try
    {
      ResultSet localResultSet = null;
      try
      {
        int i;
        if (this.stat.execute(paramString))
        {
          localResultSet = this.stat.getResultSet();
          i = printResult(localResultSet, this.listMode);
          l = System.currentTimeMillis() - l;
          println("(" + i + (i == 1 ? " row, " : " rows, ") + l + " ms)");
        }
        else
        {
          i = this.stat.getUpdateCount();
          l = System.currentTimeMillis() - l;
          println("(Update count: " + i + ", " + l + " ms)");
        }
      }
      finally
      {
        JdbcUtils.closeSilently(localResultSet);
      }
    }
    catch (SQLException localSQLException)
    {
      println("Error: " + localSQLException.toString());
      if (this.listMode) {
        localSQLException.printStackTrace(this.err);
      }
      return;
    }
  }
  
  private int printResult(ResultSet paramResultSet, boolean paramBoolean)
    throws SQLException
  {
    if (paramBoolean) {
      return printResultAsList(paramResultSet);
    }
    return printResultAsTable(paramResultSet);
  }
  
  private int printResultAsTable(ResultSet paramResultSet)
    throws SQLException
  {
    ResultSetMetaData localResultSetMetaData = paramResultSet.getMetaData();
    int i = localResultSetMetaData.getColumnCount();
    boolean bool = false;
    ArrayList localArrayList = New.arrayList();
    
    String[] arrayOfString = new String[i];
    for (int j = 0; j < i; j++)
    {
      String str = localResultSetMetaData.getColumnLabel(j + 1);
      arrayOfString[j] = (str == null ? "" : str);
    }
    localArrayList.add(arrayOfString);
    j = 0;
    while (paramResultSet.next())
    {
      j++;
      bool |= loadRow(paramResultSet, i, localArrayList);
      if (j > 5000)
      {
        printRows(localArrayList, i);
        localArrayList.clear();
      }
    }
    printRows(localArrayList, i);
    localArrayList.clear();
    if (bool) {
      println("(data is partially truncated)");
    }
    return j;
  }
  
  private boolean loadRow(ResultSet paramResultSet, int paramInt, ArrayList<String[]> paramArrayList)
    throws SQLException
  {
    boolean bool = false;
    String[] arrayOfString = new String[paramInt];
    for (int i = 0; i < paramInt; i++)
    {
      String str = paramResultSet.getString(i + 1);
      if (str == null) {
        str = "null";
      }
      if ((paramInt > 1) && (str.length() > this.maxColumnSize))
      {
        str = str.substring(0, this.maxColumnSize);
        bool = true;
      }
      arrayOfString[i] = str;
    }
    paramArrayList.add(arrayOfString);
    return bool;
  }
  
  private int[] printRows(ArrayList<String[]> paramArrayList, int paramInt)
  {
    int[] arrayOfInt = new int[paramInt];
    Object localObject;
    for (int i = 0; i < paramInt; i++)
    {
      int j = 0;
      for (localObject = paramArrayList.iterator(); ((Iterator)localObject).hasNext();)
      {
        String[] arrayOfString2 = (String[])((Iterator)localObject).next();
        j = Math.max(j, arrayOfString2[i].length());
      }
      if (paramInt > 1) {
        Math.min(this.maxColumnSize, j);
      }
      arrayOfInt[i] = j;
    }
    for (String[] arrayOfString1 : paramArrayList)
    {
      localObject = new StringBuilder();
      for (int k = 0; k < paramInt; k++)
      {
        if (k > 0) {
          ((StringBuilder)localObject).append(' ').append('|').append(' ');
        }
        String str = arrayOfString1[k];
        ((StringBuilder)localObject).append(str);
        if (k < paramInt - 1) {
          for (int m = str.length(); m < arrayOfInt[k]; m++) {
            ((StringBuilder)localObject).append(' ');
          }
        }
      }
      println(((StringBuilder)localObject).toString());
    }
    return arrayOfInt;
  }
  
  private int printResultAsList(ResultSet paramResultSet)
    throws SQLException
  {
    ResultSetMetaData localResultSetMetaData = paramResultSet.getMetaData();
    int i = 0;
    int j = localResultSetMetaData.getColumnCount();
    String[] arrayOfString = new String[j];
    for (int k = 0; k < j; k++)
    {
      String str1 = localResultSetMetaData.getColumnLabel(k + 1);
      arrayOfString[k] = str1;
      i = Math.max(i, str1.length());
    }
    StringBuilder localStringBuilder = new StringBuilder();
    int m = 0;
    int n;
    String str2;
    while (paramResultSet.next())
    {
      m++;
      localStringBuilder.setLength(0);
      if (m > 1) {
        println("");
      }
      for (n = 0; n < j; n++)
      {
        if (n > 0) {
          localStringBuilder.append('\n');
        }
        str2 = arrayOfString[n];
        localStringBuilder.append(str2);
        for (int i1 = str2.length(); i1 < i; i1++) {
          localStringBuilder.append(' ');
        }
        localStringBuilder.append(": ").append(paramResultSet.getString(n + 1));
      }
      println(localStringBuilder.toString());
    }
    if (m == 0)
    {
      for (n = 0; n < j; n++)
      {
        if (n > 0) {
          localStringBuilder.append('\n');
        }
        str2 = arrayOfString[n];
        localStringBuilder.append(str2);
      }
      println(localStringBuilder.toString());
    }
    return m;
  }
}
