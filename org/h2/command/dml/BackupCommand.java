package org.h2.command.dml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.h2.command.Prepared;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.db.MVTableEngine.Store;
import org.h2.result.ResultInterface;
import org.h2.store.FileLister;
import org.h2.store.PageStore;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.value.Value;

public class BackupCommand
  extends Prepared
{
  private Expression fileNameExpr;
  
  public BackupCommand(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setFileName(Expression paramExpression)
  {
    this.fileNameExpr = paramExpression;
  }
  
  public int update()
  {
    String str = this.fileNameExpr.getValue(this.session).getString();
    this.session.getUser().checkAdmin();
    backupTo(str);
    return 0;
  }
  
  private void backupTo(String paramString)
  {
    Database localDatabase = this.session.getDatabase();
    if (!localDatabase.isPersistent()) {
      throw DbException.get(90126);
    }
    try
    {
      MVTableEngine.Store localStore = localDatabase.getMvStore();
      if (localStore != null) {
        localStore.flush();
      }
      String str1 = localDatabase.getName();
      str1 = FileUtils.getName(str1);
      OutputStream localOutputStream = FileUtils.newOutputStream(paramString, false);
      ZipOutputStream localZipOutputStream = new ZipOutputStream(localOutputStream);
      localDatabase.flush();
      if (localDatabase.getPageStore() != null)
      {
        str2 = localDatabase.getName() + ".h2.db";
        backupPageStore(localZipOutputStream, str2, localDatabase.getPageStore());
      }
      String str2 = FileUtils.getParent(localDatabase.getName());
      synchronized (localDatabase.getLobSyncObject())
      {
        String str3 = localDatabase.getDatabasePath();
        String str4 = FileUtils.getParent(str3);
        str4 = FileLister.getDir(str4);
        ArrayList localArrayList = FileLister.getDatabaseFiles(str4, str1, true);
        for (String str5 : localArrayList)
        {
          if (str5.endsWith(".lob.db")) {
            backupFile(localZipOutputStream, str2, str5);
          }
          if ((str5.endsWith(".mv.db")) && (localStore != null))
          {
            MVStore localMVStore = localStore.getStore();
            boolean bool = localMVStore.getReuseSpace();
            localMVStore.setReuseSpace(false);
            try
            {
              InputStream localInputStream = localStore.getInputStream();
              backupFile(localZipOutputStream, str2, str5, localInputStream);
            }
            finally
            {
              localMVStore.setReuseSpace(bool);
            }
          }
        }
      }
      localZipOutputStream.close();
      localOutputStream.close();
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, paramString);
    }
  }
  
  private void backupPageStore(ZipOutputStream paramZipOutputStream, String paramString, PageStore paramPageStore)
    throws IOException
  {
    Database localDatabase = this.session.getDatabase();
    paramString = FileUtils.getName(paramString);
    paramZipOutputStream.putNextEntry(new ZipEntry(paramString));
    int i = 0;
    try
    {
      paramPageStore.setBackup(true);
      for (;;)
      {
        i = paramPageStore.copyDirect(i, paramZipOutputStream);
        if (i < 0) {
          break;
        }
        int j = paramPageStore.getPageCount();
        localDatabase.setProgress(3, paramString, i, j);
      }
    }
    finally
    {
      paramPageStore.setBackup(false);
    }
    paramZipOutputStream.closeEntry();
  }
  
  private static void backupFile(ZipOutputStream paramZipOutputStream, String paramString1, String paramString2)
    throws IOException
  {
    InputStream localInputStream = FileUtils.newInputStream(paramString2);
    backupFile(paramZipOutputStream, paramString1, paramString2, localInputStream);
  }
  
  private static void backupFile(ZipOutputStream paramZipOutputStream, String paramString1, String paramString2, InputStream paramInputStream)
    throws IOException
  {
    String str = FileUtils.toRealPath(paramString2);
    paramString1 = FileUtils.toRealPath(paramString1);
    if (!str.startsWith(paramString1)) {
      DbException.throwInternalError(str + " does not start with " + paramString1);
    }
    str = str.substring(paramString1.length());
    str = correctFileName(str);
    paramZipOutputStream.putNextEntry(new ZipEntry(str));
    IOUtils.copyAndCloseInput(paramInputStream, paramZipOutputStream);
    paramZipOutputStream.closeEntry();
  }
  
  public boolean isTransactional()
  {
    return true;
  }
  
  public static String correctFileName(String paramString)
  {
    paramString = paramString.replace('\\', '/');
    if (paramString.startsWith("/")) {
      paramString = paramString.substring(1);
    }
    return paramString;
  }
  
  public boolean needRecompile()
  {
    return false;
  }
  
  public ResultInterface queryMeta()
  {
    return null;
  }
  
  public int getType()
  {
    return 56;
  }
}
