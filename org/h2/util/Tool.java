package org.h2.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Properties;
import org.h2.message.DbException;
import org.h2.store.FileLister;
import org.h2.store.fs.FileUtils;

public abstract class Tool
{
  protected PrintStream out = System.out;
  private Properties resources;
  
  public void setOut(PrintStream paramPrintStream)
  {
    this.out = paramPrintStream;
  }
  
  public abstract void runTool(String... paramVarArgs)
    throws SQLException;
  
  protected SQLException showUsageAndThrowUnsupportedOption(String paramString)
    throws SQLException
  {
    showUsage();
    throw throwUnsupportedOption(paramString);
  }
  
  protected SQLException throwUnsupportedOption(String paramString)
    throws SQLException
  {
    throw DbException.get(50100, paramString).getSQLException();
  }
  
  protected void printNoDatabaseFilesFound(String paramString1, String paramString2)
  {
    paramString1 = FileLister.getDir(paramString1);
    StringBuilder localStringBuilder;
    if (!FileUtils.isDirectory(paramString1))
    {
      localStringBuilder = new StringBuilder("Directory not found: ");
      localStringBuilder.append(paramString1);
    }
    else
    {
      localStringBuilder = new StringBuilder("No database files have been found");
      localStringBuilder.append(" in directory ").append(paramString1);
      if (paramString2 != null) {
        localStringBuilder.append(" for the database ").append(paramString2);
      }
    }
    this.out.println(localStringBuilder.toString());
  }
  
  protected void showUsage()
  {
    if (this.resources == null)
    {
      this.resources = new Properties();
      str = "/org/h2/res/javadoc.properties";
      try
      {
        byte[] arrayOfByte = Utils.getResource(str);
        if (arrayOfByte != null) {
          this.resources.load(new ByteArrayInputStream(arrayOfByte));
        }
      }
      catch (IOException localIOException)
      {
        this.out.println("Cannot load " + str);
      }
    }
    String str = getClass().getName();
    this.out.println(this.resources.get(str));
    this.out.println("Usage: java " + getClass().getName() + " <options>");
    this.out.println(this.resources.get(str + ".main"));
    this.out.println("See also http://h2database.com/javadoc/" + str.replace('.', '/') + ".html");
  }
  
  public static boolean isOption(String paramString1, String paramString2)
  {
    if (paramString1.equals(paramString2)) {
      return true;
    }
    if (paramString1.startsWith(paramString2)) {
      throw DbException.getUnsupportedException("expected: " + paramString2 + " got: " + paramString1);
    }
    return false;
  }
}
