package org.h2.store;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.h2.message.DbException;
import org.h2.message.TraceSystem;
import org.h2.store.fs.FilePath;
import org.h2.store.fs.FileUtils;
import org.h2.util.New;

public class FileLister
{
  public static void tryUnlockDatabase(List<String> paramList, String paramString)
    throws SQLException
  {
    Iterator localIterator = paramList.iterator();
    for (;;)
    {
      if (localIterator.hasNext())
      {
        String str = (String)localIterator.next();
        Object localObject1;
        if (str.endsWith(".lock.db"))
        {
          localObject1 = new FileLock(new TraceSystem(null), str, 1000);
          try
          {
            ((FileLock)localObject1).lock(1);
            ((FileLock)localObject1).unlock();
          }
          catch (DbException localDbException)
          {
            throw DbException.get(90133, paramString).getSQLException();
          }
        }
        else if (str.endsWith(".mv.db"))
        {
          localObject1 = null;
          try
          {
            localObject1 = FilePath.get(str).open("r");
            java.nio.channels.FileLock localFileLock = ((FileChannel)localObject1).tryLock(0L, Long.MAX_VALUE, true);
            localFileLock.release();
            if (localObject1 != null) {
              try
              {
                ((FileChannel)localObject1).close();
              }
              catch (IOException localIOException1) {}
            }
          }
          catch (Exception localException)
          {
            throw DbException.get(90133, localException, new String[] { paramString }).getSQLException();
          }
          finally
          {
            if (localObject1 != null) {
              try
              {
                ((FileChannel)localObject1).close();
              }
              catch (IOException localIOException2) {}
            }
          }
        }
      }
    }
  }
  
  public static String getDir(String paramString)
  {
    if ((paramString == null) || (paramString.equals(""))) {
      return ".";
    }
    return FileUtils.toRealPath(paramString);
  }
  
  public static ArrayList<String> getDatabaseFiles(String paramString1, String paramString2, boolean paramBoolean)
  {
    ArrayList localArrayList = New.arrayList();
    
    String str1 = FileUtils.toRealPath(new StringBuilder().append(paramString1).append("/").append(paramString2).toString()) + ".";
    for (String str2 : FileUtils.newDirectoryStream(paramString1))
    {
      int i = 0;
      if (str2.endsWith(".lobs.db"))
      {
        if ((str1 == null) || (str2.startsWith(str1)))
        {
          localArrayList.addAll(getDatabaseFiles(str2, null, paramBoolean));
          i = 1;
        }
      }
      else if (str2.endsWith(".lob.db")) {
        i = 1;
      } else if (str2.endsWith(".h2.db")) {
        i = 1;
      } else if (str2.endsWith(".mv.db")) {
        i = 1;
      } else if (paramBoolean) {
        if (str2.endsWith(".lock.db")) {
          i = 1;
        } else if (str2.endsWith(".temp.db")) {
          i = 1;
        } else if (str2.endsWith(".trace.db")) {
          i = 1;
        }
      }
      if ((i != 0) && (
        (paramString2 == null) || (str2.startsWith(str1))))
      {
        String str3 = str2;
        localArrayList.add(str3);
      }
    }
    return localArrayList;
  }
}
