package org.h2.tools;

import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import org.h2.store.FileLister;
import org.h2.store.fs.FileUtils;
import org.h2.util.Tool;

public class DeleteDbFiles
  extends Tool
{
  public static void main(String... paramVarArgs)
    throws SQLException
  {
    new DeleteDbFiles().runTool(paramVarArgs);
  }
  
  public void runTool(String... paramVarArgs)
    throws SQLException
  {
    String str1 = ".";
    String str2 = null;
    boolean bool = false;
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++)
    {
      String str3 = paramVarArgs[i];
      if (str3.equals("-dir"))
      {
        str1 = paramVarArgs[(++i)];
      }
      else if (str3.equals("-db"))
      {
        str2 = paramVarArgs[(++i)];
      }
      else if (str3.equals("-quiet"))
      {
        bool = true;
      }
      else
      {
        if ((str3.equals("-help")) || (str3.equals("-?")))
        {
          showUsage();
          return;
        }
        showUsageAndThrowUnsupportedOption(str3);
      }
    }
    process(str1, str2, bool);
  }
  
  public static void execute(String paramString1, String paramString2, boolean paramBoolean)
  {
    new DeleteDbFiles().process(paramString1, paramString2, paramBoolean);
  }
  
  private void process(String paramString1, String paramString2, boolean paramBoolean)
  {
    ArrayList localArrayList = FileLister.getDatabaseFiles(paramString1, paramString2, true);
    if ((localArrayList.size() == 0) && (!paramBoolean)) {
      printNoDatabaseFilesFound(paramString1, paramString2);
    }
    for (String str : localArrayList)
    {
      process(str, paramBoolean);
      if (!paramBoolean) {
        this.out.println("Processed: " + str);
      }
    }
  }
  
  private static void process(String paramString, boolean paramBoolean)
  {
    if (FileUtils.isDirectory(paramString)) {
      FileUtils.tryDelete(paramString);
    } else if ((paramBoolean) || (paramString.endsWith(".temp.db")) || (paramString.endsWith(".trace.db"))) {
      FileUtils.tryDelete(paramString);
    } else {
      FileUtils.delete(paramString);
    }
  }
}
