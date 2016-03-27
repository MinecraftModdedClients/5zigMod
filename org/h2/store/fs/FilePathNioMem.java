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

public class FilePathNioMem
  extends FilePath
{
  private static final TreeMap<String, FileNioMemData> MEMORY_FILES = new TreeMap();
  
  public FilePathNioMem getPath(String paramString)
  {
    FilePathNioMem localFilePathNioMem = new FilePathNioMem();
    localFilePathNioMem.name = getCanonicalPath(paramString);
    return localFilePathNioMem;
  }
  
  public long size()
  {
    return getMemoryFile().length();
  }
  
  public void moveTo(FilePath paramFilePath, boolean paramBoolean)
  {
    synchronized (MEMORY_FILES)
    {
      if ((!paramBoolean) && (!this.name.equals(paramFilePath.name)) && (MEMORY_FILES.containsKey(paramFilePath.name))) {
        throw DbException.get(90024, new String[] { this.name, paramFilePath + " (exists)" });
      }
      FileNioMemData localFileNioMemData = getMemoryFile();
      localFileNioMemData.setName(paramFilePath.name);
      MEMORY_FILES.remove(this.name);
      MEMORY_FILES.put(paramFilePath.name, localFileNioMemData);
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
        localArrayList.add(getPath(str));
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
  
  public FilePathNioMem getParent()
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
      return MEMORY_FILES.get(this.name) == null;
    }
  }
  
  public boolean isAbsolute()
  {
    return true;
  }
  
  public FilePathNioMem toRealPath()
  {
    return this;
  }
  
  public long lastModified()
  {
    return getMemoryFile().getLastModified();
  }
  
  public void createDirectory()
  {
    if ((exists()) && (isDirectory())) {
      throw DbException.get(90062, this.name + " (a file with this name already exists)");
    }
  }
  
  public OutputStream newOutputStream(boolean paramBoolean)
    throws IOException
  {
    FileNioMemData localFileNioMemData = getMemoryFile();
    FileNioMem localFileNioMem = new FileNioMem(localFileNioMemData, false);
    return new FileChannelOutputStream(localFileNioMem, paramBoolean);
  }
  
  public InputStream newInputStream()
  {
    FileNioMemData localFileNioMemData = getMemoryFile();
    FileNioMem localFileNioMem = new FileNioMem(localFileNioMemData, true);
    return new FileChannelInputStream(localFileNioMem, true);
  }
  
  public FileChannel open(String paramString)
  {
    FileNioMemData localFileNioMemData = getMemoryFile();
    return new FileNioMem(localFileNioMemData, "r".equals(paramString));
  }
  
  private FileNioMemData getMemoryFile()
  {
    synchronized (MEMORY_FILES)
    {
      FileNioMemData localFileNioMemData = (FileNioMemData)MEMORY_FILES.get(this.name);
      if (localFileNioMemData == null)
      {
        localFileNioMemData = new FileNioMemData(this.name, compressed());
        MEMORY_FILES.put(this.name, localFileNioMemData);
      }
      return localFileNioMemData;
    }
  }
  
  private boolean isRoot()
  {
    return this.name.equals(getScheme());
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
    return "nioMemFS";
  }
  
  boolean compressed()
  {
    return false;
  }
}
