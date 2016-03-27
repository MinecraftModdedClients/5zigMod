package org.h2.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.h2.command.dml.BackupCommand;
import org.h2.message.DbException;
import org.h2.store.FileLister;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.Tool;

public class Backup
  extends Tool
{
  public static void main(String... paramVarArgs)
    throws SQLException
  {
    new Backup().runTool(paramVarArgs);
  }
  
  public void runTool(String... paramVarArgs)
    throws SQLException
  {
    String str1 = "backup.zip";
    String str2 = ".";
    String str3 = null;
    boolean bool = false;
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++)
    {
      String str4 = paramVarArgs[i];
      if (str4.equals("-dir"))
      {
        str2 = paramVarArgs[(++i)];
      }
      else if (str4.equals("-db"))
      {
        str3 = paramVarArgs[(++i)];
      }
      else if (str4.equals("-quiet"))
      {
        bool = true;
      }
      else if (str4.equals("-file"))
      {
        str1 = paramVarArgs[(++i)];
      }
      else
      {
        if ((str4.equals("-help")) || (str4.equals("-?")))
        {
          showUsage();
          return;
        }
        showUsageAndThrowUnsupportedOption(str4);
      }
    }
    try
    {
      process(str1, str2, str3, bool);
    }
    catch (Exception localException)
    {
      throw DbException.toSQLException(localException);
    }
  }
  
  public static void execute(String paramString1, String paramString2, String paramString3, boolean paramBoolean)
    throws SQLException
  {
    try
    {
      new Backup().process(paramString1, paramString2, paramString3, paramBoolean);
    }
    catch (Exception localException)
    {
      throw DbException.toSQLException(localException);
    }
  }
  
  private void process(String paramString1, String paramString2, String paramString3, boolean paramBoolean)
    throws SQLException
  {
    int i = (paramString3 != null) && (paramString3.length() == 0) ? 1 : 0;
    Object localObject1;
    if (i != 0) {
      localObject1 = FileUtils.newDirectoryStream(paramString2);
    } else {
      localObject1 = FileLister.getDatabaseFiles(paramString2, paramString3, true);
    }
    if (((List)localObject1).size() == 0)
    {
      if (!paramBoolean) {
        printNoDatabaseFilesFound(paramString2, paramString3);
      }
      return;
    }
    if (!paramBoolean) {
      FileLister.tryUnlockDatabase((List)localObject1, "backup");
    }
    paramString1 = FileUtils.toRealPath(paramString1);
    FileUtils.delete(paramString1);
    OutputStream localOutputStream = null;
    try
    {
      localOutputStream = FileUtils.newOutputStream(paramString1, false);
      ZipOutputStream localZipOutputStream = new ZipOutputStream(localOutputStream);
      String str1 = "";
      for (Iterator localIterator = ((List)localObject1).iterator(); localIterator.hasNext();)
      {
        str2 = (String)localIterator.next();
        if ((i != 0) || (str2.endsWith(".h2.db")) || (str2.endsWith(".mv.db")))
        {
          str1 = FileUtils.getParent(str2);
          break;
        }
      }
      String str2;
      for (localIterator = ((List)localObject1).iterator(); localIterator.hasNext();)
      {
        str2 = (String)localIterator.next();
        String str3 = FileUtils.toRealPath(str2);
        if (!str3.startsWith(str1)) {
          DbException.throwInternalError(str3 + " does not start with " + str1);
        }
        if ((!str3.endsWith(paramString1)) && 
        
          (!FileUtils.isDirectory(str2)))
        {
          str3 = str3.substring(str1.length());
          str3 = BackupCommand.correctFileName(str3);
          ZipEntry localZipEntry = new ZipEntry(str3);
          localZipOutputStream.putNextEntry(localZipEntry);
          InputStream localInputStream = null;
          try
          {
            localInputStream = FileUtils.newInputStream(str2);
            IOUtils.copyAndCloseInput(localInputStream, localZipOutputStream);
          }
          catch (FileNotFoundException localFileNotFoundException) {}finally {}
          localZipOutputStream.closeEntry();
          if (!paramBoolean) {
            this.out.println("Processed: " + str2);
          }
        }
      }
      localZipOutputStream.close();
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, paramString1);
    }
    finally
    {
      IOUtils.closeSilently(localOutputStream);
    }
  }
}
