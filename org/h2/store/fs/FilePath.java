package org.h2.store.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.h2.util.MathUtils;
import org.h2.util.New;

public abstract class FilePath
{
  private static FilePath defaultProvider;
  private static Map<String, FilePath> providers;
  private static String tempRandom;
  private static long tempSequence;
  protected String name;
  
  public static FilePath get(String paramString)
  {
    paramString = paramString.replace('\\', '/');
    int i = paramString.indexOf(':');
    registerDefaultProviders();
    if (i < 2) {
      return defaultProvider.getPath(paramString);
    }
    String str = paramString.substring(0, i);
    FilePath localFilePath = (FilePath)providers.get(str);
    if (localFilePath == null) {
      localFilePath = defaultProvider;
    }
    return localFilePath.getPath(paramString);
  }
  
  private static void registerDefaultProviders()
  {
    if ((providers == null) || (defaultProvider == null))
    {
      Map localMap = Collections.synchronizedMap(New.hashMap());
      for (String str : new String[] { "org.h2.store.fs.FilePathDisk", "org.h2.store.fs.FilePathMem", "org.h2.store.fs.FilePathMemLZF", "org.h2.store.fs.FilePathNioMem", "org.h2.store.fs.FilePathNioMemLZF", "org.h2.store.fs.FilePathSplit", "org.h2.store.fs.FilePathNio", "org.h2.store.fs.FilePathNioMapped", "org.h2.store.fs.FilePathZip" }) {
        try
        {
          FilePath localFilePath = (FilePath)Class.forName(str).newInstance();
          localMap.put(localFilePath.getScheme(), localFilePath);
          if (defaultProvider == null) {
            defaultProvider = localFilePath;
          }
        }
        catch (Exception localException) {}
      }
      providers = localMap;
    }
  }
  
  public static void register(FilePath paramFilePath)
  {
    registerDefaultProviders();
    providers.put(paramFilePath.getScheme(), paramFilePath);
  }
  
  public static void unregister(FilePath paramFilePath)
  {
    registerDefaultProviders();
    providers.remove(paramFilePath.getScheme());
  }
  
  public abstract long size();
  
  public abstract void moveTo(FilePath paramFilePath, boolean paramBoolean);
  
  public abstract boolean createFile();
  
  public abstract boolean exists();
  
  public abstract void delete();
  
  public abstract List<FilePath> newDirectoryStream();
  
  public abstract FilePath toRealPath();
  
  public abstract FilePath getParent();
  
  public abstract boolean isDirectory();
  
  public abstract boolean isAbsolute();
  
  public abstract long lastModified();
  
  public abstract boolean canWrite();
  
  public abstract void createDirectory();
  
  public String getName()
  {
    int i = Math.max(this.name.indexOf(':'), this.name.lastIndexOf('/'));
    return i < 0 ? this.name : this.name.substring(i + 1);
  }
  
  public abstract OutputStream newOutputStream(boolean paramBoolean)
    throws IOException;
  
  public abstract FileChannel open(String paramString)
    throws IOException;
  
  public abstract InputStream newInputStream()
    throws IOException;
  
  public abstract boolean setReadOnly();
  
  public FilePath createTempFile(String paramString, boolean paramBoolean1, boolean paramBoolean2)
    throws IOException
  {
    FilePath localFilePath;
    for (;;)
    {
      localFilePath = getPath(this.name + getNextTempFileNamePart(false) + paramString);
      if ((!localFilePath.exists()) && (localFilePath.createFile())) {
        break;
      }
      getNextTempFileNamePart(true);
    }
    localFilePath.open("rw").close();
    return localFilePath;
  }
  
  protected static synchronized String getNextTempFileNamePart(boolean paramBoolean)
  {
    if ((paramBoolean) || (tempRandom == null)) {
      tempRandom = MathUtils.randomInt(Integer.MAX_VALUE) + ".";
    }
    return tempRandom + tempSequence++;
  }
  
  public String toString()
  {
    return this.name;
  }
  
  public abstract String getScheme();
  
  public abstract FilePath getPath(String paramString);
  
  public FilePath unwrap()
  {
    return this;
  }
}
