package org.h2.store.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.util.IOUtils;
import org.h2.util.New;

public class FilePathDisk
  extends FilePath
{
  private static final String CLASSPATH_PREFIX = "classpath:";
  
  public FilePathDisk getPath(String paramString)
  {
    FilePathDisk localFilePathDisk = new FilePathDisk();
    localFilePathDisk.name = translateFileName(paramString);
    return localFilePathDisk;
  }
  
  public long size()
  {
    return new File(this.name).length();
  }
  
  protected static String translateFileName(String paramString)
  {
    paramString = paramString.replace('\\', '/');
    if (paramString.startsWith("file:")) {
      paramString = paramString.substring("file:".length());
    }
    return expandUserHomeDirectory(paramString);
  }
  
  public static String expandUserHomeDirectory(String paramString)
  {
    if ((paramString.startsWith("~")) && ((paramString.length() == 1) || (paramString.startsWith("~/"))))
    {
      String str = SysProperties.USER_HOME;
      paramString = str + paramString.substring(1);
    }
    return paramString;
  }
  
  public void moveTo(FilePath paramFilePath, boolean paramBoolean)
  {
    File localFile1 = new File(this.name);
    File localFile2 = new File(paramFilePath.name);
    if (localFile1.getAbsolutePath().equals(localFile2.getAbsolutePath())) {
      return;
    }
    if (!localFile1.exists()) {
      throw DbException.get(90024, new String[] { this.name + " (not found)", paramFilePath.name });
    }
    if (paramBoolean)
    {
      i = localFile1.renameTo(localFile2);
      if (i != 0) {
        return;
      }
      throw DbException.get(90024, new String[] { this.name, paramFilePath.name });
    }
    if (localFile2.exists()) {
      throw DbException.get(90024, new String[] { this.name, paramFilePath + " (exists)" });
    }
    for (int i = 0; i < SysProperties.MAX_FILE_RETRY; i++)
    {
      IOUtils.trace("rename", this.name + " >" + paramFilePath, null);
      boolean bool = localFile1.renameTo(localFile2);
      if (bool) {
        return;
      }
      wait(i);
    }
    throw DbException.get(90024, new String[] { this.name, paramFilePath.name });
  }
  
  private static void wait(int paramInt)
  {
    if (paramInt == 8) {
      System.gc();
    }
    try
    {
      long l = Math.min(256, paramInt * paramInt);
      Thread.sleep(l);
    }
    catch (InterruptedException localInterruptedException) {}
  }
  
  public boolean createFile()
  {
    File localFile = new File(this.name);
    for (int i = 0; i < SysProperties.MAX_FILE_RETRY; i++) {
      try
      {
        return localFile.createNewFile();
      }
      catch (IOException localIOException)
      {
        wait(i);
      }
    }
    return false;
  }
  
  public boolean exists()
  {
    return new File(this.name).exists();
  }
  
  public void delete()
  {
    File localFile = new File(this.name);
    for (int i = 0; i < SysProperties.MAX_FILE_RETRY; i++)
    {
      IOUtils.trace("delete", this.name, null);
      boolean bool = localFile.delete();
      if ((bool) || (!localFile.exists())) {
        return;
      }
      wait(i);
    }
    throw DbException.get(90025, this.name);
  }
  
  public List<FilePath> newDirectoryStream()
  {
    ArrayList localArrayList = New.arrayList();
    File localFile = new File(this.name);
    try
    {
      String[] arrayOfString = localFile.list();
      if (arrayOfString != null)
      {
        String str = localFile.getCanonicalPath();
        if (!str.endsWith(SysProperties.FILE_SEPARATOR)) {
          str = str + SysProperties.FILE_SEPARATOR;
        }
        int i = 0;
        for (int j = arrayOfString.length; i < j; i++) {
          localArrayList.add(getPath(str + arrayOfString[i]));
        }
      }
      return localArrayList;
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, this.name);
    }
  }
  
  public boolean canWrite()
  {
    return canWriteInternal(new File(this.name));
  }
  
  public boolean setReadOnly()
  {
    File localFile = new File(this.name);
    return localFile.setReadOnly();
  }
  
  public FilePathDisk toRealPath()
  {
    try
    {
      String str = new File(this.name).getCanonicalPath();
      return getPath(str);
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, this.name);
    }
  }
  
  public FilePath getParent()
  {
    String str = new File(this.name).getParent();
    return str == null ? null : getPath(str);
  }
  
  public boolean isDirectory()
  {
    return new File(this.name).isDirectory();
  }
  
  public boolean isAbsolute()
  {
    return new File(this.name).isAbsolute();
  }
  
  public long lastModified()
  {
    return new File(this.name).lastModified();
  }
  
  private static boolean canWriteInternal(File paramFile)
  {
    try
    {
      if (!paramFile.canWrite()) {
        return false;
      }
    }
    catch (Exception localException)
    {
      return false;
    }
    RandomAccessFile localRandomAccessFile = null;
    try
    {
      localRandomAccessFile = new RandomAccessFile(paramFile, "rw");
      return true;
    }
    catch (FileNotFoundException localFileNotFoundException)
    {
      return false;
    }
    finally
    {
      if (localRandomAccessFile != null) {
        try
        {
          localRandomAccessFile.close();
        }
        catch (IOException localIOException3) {}
      }
    }
  }
  
  public void createDirectory()
  {
    File localFile = new File(this.name);
    for (int i = 0; i < SysProperties.MAX_FILE_RETRY; i++)
    {
      if (localFile.exists())
      {
        if (localFile.isDirectory()) {
          return;
        }
        throw DbException.get(90062, this.name + " (a file with this name already exists)");
      }
      if (localFile.mkdir()) {
        return;
      }
      wait(i);
    }
    throw DbException.get(90062, this.name);
  }
  
  public OutputStream newOutputStream(boolean paramBoolean)
    throws IOException
  {
    try
    {
      File localFile1 = new File(this.name);
      File localFile2 = localFile1.getParentFile();
      if (localFile2 != null) {
        FileUtils.createDirectories(localFile2.getAbsolutePath());
      }
      FileOutputStream localFileOutputStream = new FileOutputStream(this.name, paramBoolean);
      IOUtils.trace("openFileOutputStream", this.name, localFileOutputStream);
      return localFileOutputStream;
    }
    catch (IOException localIOException)
    {
      freeMemoryAndFinalize();
    }
    return new FileOutputStream(this.name);
  }
  
  public InputStream newInputStream()
    throws IOException
  {
    int i = this.name.indexOf(':');
    if ((i > 1) && (i < 20))
    {
      if (this.name.startsWith("classpath:"))
      {
        localObject = this.name.substring("classpath:".length());
        if (!((String)localObject).startsWith("/")) {
          localObject = "/" + (String)localObject;
        }
        localInputStream = getClass().getResourceAsStream((String)localObject);
        if (localInputStream == null) {
          localInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream((String)localObject);
        }
        if (localInputStream == null) {
          throw new FileNotFoundException("resource " + (String)localObject);
        }
        return localInputStream;
      }
      localObject = new URL(this.name);
      InputStream localInputStream = ((URL)localObject).openStream();
      return localInputStream;
    }
    Object localObject = new FileInputStream(this.name);
    IOUtils.trace("openFileInputStream", this.name, localObject);
    return (InputStream)localObject;
  }
  
  static void freeMemoryAndFinalize()
  {
    IOUtils.trace("freeMemoryAndFinalize", null, null);
    Runtime localRuntime = Runtime.getRuntime();
    long l1 = localRuntime.freeMemory();
    for (int i = 0; i < 16; i++)
    {
      localRuntime.gc();
      long l2 = localRuntime.freeMemory();
      localRuntime.runFinalization();
      if (l2 == l1) {
        break;
      }
      l1 = l2;
    }
  }
  
  public FileChannel open(String paramString)
    throws IOException
  {
    FileDisk localFileDisk;
    try
    {
      localFileDisk = new FileDisk(this.name, paramString);
      IOUtils.trace("open", this.name, localFileDisk);
    }
    catch (IOException localIOException1)
    {
      freeMemoryAndFinalize();
      try
      {
        localFileDisk = new FileDisk(this.name, paramString);
      }
      catch (IOException localIOException2)
      {
        throw localIOException1;
      }
    }
    return localFileDisk;
  }
  
  public String getScheme()
  {
    return "file";
  }
  
  public FilePath createTempFile(String paramString, boolean paramBoolean1, boolean paramBoolean2)
    throws IOException
  {
    String str1 = this.name + ".";
    String str2 = new File(str1).getName();
    File localFile1;
    if (paramBoolean2) {
      localFile1 = new File(System.getProperty("java.io.tmpdir", "."));
    } else {
      localFile1 = new File(str1).getAbsoluteFile().getParentFile();
    }
    FileUtils.createDirectories(localFile1.getAbsolutePath());
    File localFile2;
    for (;;)
    {
      localFile2 = new File(localFile1, str2 + getNextTempFileNamePart(false) + paramString);
      if ((!localFile2.exists()) && (localFile2.createNewFile())) {
        break;
      }
      getNextTempFileNamePart(true);
    }
    if (paramBoolean1) {
      try
      {
        localFile2.deleteOnExit();
      }
      catch (Throwable localThrowable) {}
    }
    return get(localFile2.getCanonicalPath());
  }
}
