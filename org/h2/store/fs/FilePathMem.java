package org.h2.store.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.h2.message.DbException;
import org.h2.util.New;

public class FilePathMem
  extends FilePath
{
  private static final TreeMap<String, FileMemData> MEMORY_FILES = new TreeMap();
  private static final FileMemData DIRECTORY = new FileMemData("", false);
  
  public FilePathMem getPath(String paramString)
  {
    FilePathMem localFilePathMem = new FilePathMem();
    localFilePathMem.name = getCanonicalPath(paramString);
    return localFilePathMem;
  }
  
  public long size()
  {
    return getMemoryFile().length();
  }
  
  public void moveTo(FilePath paramFilePath, boolean paramBoolean)
  {
    synchronized (MEMORY_FILES)
    {
      if ((!paramBoolean) && (!paramFilePath.name.equals(this.name)) && (MEMORY_FILES.containsKey(paramFilePath.name))) {
        throw DbException.get(90024, new String[] { this.name, paramFilePath + " (exists)" });
      }
      FileMemData localFileMemData = getMemoryFile();
      localFileMemData.setName(paramFilePath.name);
      MEMORY_FILES.remove(this.name);
      MEMORY_FILES.put(paramFilePath.name, localFileMemData);
    }
  }
  
  public boolean createFile()
  {
    synchronized (MEMORY_FILES)
    {
      if (exists()) {
        return false;
      }
      getMemoryFile();
    }
    return true;
  }
  
  public boolean exists()
  {
    if (isRoot()) {
      return true;
    }
    synchronized (MEMORY_FILES)
    {
      return MEMORY_FILES.get(this.name) != null;
    }
  }
  
  public void delete()
  {
    if (isRoot()) {
      return;
    }
    synchronized (MEMORY_FILES)
    {
      MEMORY_FILES.remove(this.name);
    }
  }
  
  public List<FilePath> newDirectoryStream()
  {
    ArrayList localArrayList = New.arrayList();
    synchronized (MEMORY_FILES)
    {
      for (String str : MEMORY_FILES.tailMap(this.name).keySet())
      {
        if (!str.startsWith(this.name)) {
          break;
        }
        if ((!str.equals(this.name)) && (str.indexOf('/', this.name.length() + 1) < 0)) {
          localArrayList.add(getPath(str));
        }
      }
      return localArrayList;
    }
  }
  
  public boolean setReadOnly()
  {
    return getMemoryFile().setReadOnly();
  }
  
  public boolean canWrite()
  {
    return getMemoryFile().canWrite();
  }
  
  public FilePathMem getParent()
  {
    int i = this.name.lastIndexOf('/');
    return i < 0 ? null : getPath(this.name.substring(0, i));
  }
  
  public boolean isDirectory()
  {
    if (isRoot()) {
      return true;
    }
    synchronized (MEMORY_FILES)
    {
      FileMemData localFileMemData = (FileMemData)MEMORY_FILES.get(this.name);
      return localFileMemData == DIRECTORY;
    }
  }
  
  public boolean isAbsolute()
  {
    return true;
  }
  
  public FilePathMem toRealPath()
  {
    return this;
  }
  
  public long lastModified()
  {
    return getMemoryFile().getLastModified();
  }
  
  public void createDirectory()
  {
    if (exists()) {
      throw DbException.get(90062, this.name + " (a file with this name already exists)");
    }
    synchronized (MEMORY_FILES)
    {
      MEMORY_FILES.put(this.name, DIRECTORY);
    }
  }
  
  public OutputStream newOutputStream(boolean paramBoolean)
    throws IOException
  {
    FileMemData localFileMemData = getMemoryFile();
    FileMem localFileMem = new FileMem(localFileMemData, false);
    return new FileChannelOutputStream(localFileMem, paramBoolean);
  }
  
  public InputStream newInputStream()
  {
    FileMemData localFileMemData = getMemoryFile();
    FileMem localFileMem = new FileMem(localFileMemData, true);
    return new FileChannelInputStream(localFileMem, true);
  }
  
  public FileChannel open(String paramString)
  {
    FileMemData localFileMemData = getMemoryFile();
    return new FileMem(localFileMemData, "r".equals(paramString));
  }
  
  private FileMemData getMemoryFile()
  {
    synchronized (MEMORY_FILES)
    {
      FileMemData localFileMemData = (FileMemData)MEMORY_FILES.get(this.name);
      if (localFileMemData == DIRECTORY) {
        throw DbException.get(90062, this.name + " (a directory with this name already exists)");
      }
      if (localFileMemData == null)
      {
        localFileMemData = new FileMemData(this.name, compressed());
        MEMORY_FILES.put(this.name, localFileMemData);
      }
      return localFileMemData;
    }
  }
  
  private boolean isRoot()
  {
    return this.name.equals(getScheme() + ":");
  }
  
  private static String getCanonicalPath(String paramString)
  {
    paramString = paramString.replace('\\', '/');
    int i = paramString.indexOf(':') + 1;
    if ((paramString.length() > i) && (paramString.charAt(i) != '/')) {
      paramString = paramString.substring(0, i) + "/" + paramString.substring(i);
    }
    return paramString;
  }
  
  public String getScheme()
  {
    return "memFS";
  }
  
  boolean compressed()
  {
    return false;
  }
}
