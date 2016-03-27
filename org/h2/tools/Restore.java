package org.h2.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.Tool;

public class Restore
  extends Tool
{
  public static void main(String... paramVarArgs)
    throws SQLException
  {
    new Restore().runTool(paramVarArgs);
  }
  
  public void runTool(String... paramVarArgs)
    throws SQLException
  {
    String str1 = "backup.zip";
    String str2 = ".";
    String str3 = null;
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++)
    {
      String str4 = paramVarArgs[i];
      if (str4.equals("-dir"))
      {
        str2 = paramVarArgs[(++i)];
      }
      else if (str4.equals("-file"))
      {
        str1 = paramVarArgs[(++i)];
      }
      else if (str4.equals("-db"))
      {
        str3 = paramVarArgs[(++i)];
      }
      else if (!str4.equals("-quiet"))
      {
        if ((str4.equals("-help")) || (str4.equals("-?")))
        {
          showUsage();
          return;
        }
        showUsageAndThrowUnsupportedOption(str4);
      }
    }
    execute(str1, str2, str3);
  }
  
  private static String getOriginalDbName(String paramString1, String paramString2)
    throws IOException
  {
    InputStream localInputStream = null;
    try
    {
      localInputStream = FileUtils.newInputStream(paramString1);
      ZipInputStream localZipInputStream = new ZipInputStream(localInputStream);
      Object localObject1 = null;
      int i = 0;
      Object localObject2;
      for (;;)
      {
        localObject2 = localZipInputStream.getNextEntry();
        if (localObject2 == null) {
          break;
        }
        String str1 = ((ZipEntry)localObject2).getName();
        localZipInputStream.closeEntry();
        String str2 = getDatabaseNameFromFileName(str1);
        if (str2 != null)
        {
          if (paramString2.equals(str2))
          {
            localObject1 = str2;
            
            break;
          }
          if (localObject1 == null) {
            localObject1 = str2;
          } else {
            i = 1;
          }
        }
      }
      localZipInputStream.close();
      if ((i != 0) && (!paramString2.equals(localObject1))) {
        throw new IOException("Multiple databases found, but not " + paramString2);
      }
      return (String)localObject1;
    }
    finally
    {
      IOUtils.closeSilently(localInputStream);
    }
  }
  
  private static String getDatabaseNameFromFileName(String paramString)
  {
    if (paramString.endsWith(".h2.db")) {
      return paramString.substring(0, paramString.length() - ".h2.db".length());
    }
    if (paramString.endsWith(".mv.db")) {
      return paramString.substring(0, paramString.length() - ".mv.db".length());
    }
    return null;
  }
  
  public static void execute(String paramString1, String paramString2, String paramString3)
  {
    InputStream localInputStream = null;
    try
    {
      if (!FileUtils.exists(paramString1)) {
        throw new IOException("File not found: " + paramString1);
      }
      String str1 = null;
      int i = 0;
      if (paramString3 != null)
      {
        str1 = getOriginalDbName(paramString1, paramString3);
        if (str1 == null) {
          throw new IOException("No database named " + paramString3 + " found");
        }
        if (str1.startsWith(SysProperties.FILE_SEPARATOR)) {
          str1 = str1.substring(1);
        }
        i = str1.length();
      }
      localInputStream = FileUtils.newInputStream(paramString1);
      ZipInputStream localZipInputStream = new ZipInputStream(localInputStream);
      for (;;)
      {
        ZipEntry localZipEntry = localZipInputStream.getNextEntry();
        if (localZipEntry == null) {
          break;
        }
        String str2 = localZipEntry.getName();
        
        str2 = str2.replace('\\', SysProperties.FILE_SEPARATOR.charAt(0));
        str2 = str2.replace('/', SysProperties.FILE_SEPARATOR.charAt(0));
        if (str2.startsWith(SysProperties.FILE_SEPARATOR)) {
          str2 = str2.substring(1);
        }
        int j = 0;
        if (paramString3 == null)
        {
          j = 1;
        }
        else if (str2.startsWith(str1 + "."))
        {
          str2 = paramString3 + str2.substring(i);
          j = 1;
        }
        if (j != 0)
        {
          OutputStream localOutputStream = null;
          try
          {
            localOutputStream = FileUtils.newOutputStream(paramString2 + SysProperties.FILE_SEPARATOR + str2, false);
            
            IOUtils.copy(localZipInputStream, localOutputStream);
            localOutputStream.close();
          }
          finally {}
        }
        localZipInputStream.closeEntry();
      }
      localZipInputStream.closeEntry();
      localZipInputStream.close();
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, paramString1);
    }
    finally
    {
      IOUtils.closeSilently(localInputStream);
    }
  }
}
