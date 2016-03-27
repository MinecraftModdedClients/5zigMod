package org.h2.store.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.List;

public abstract class FilePathWrapper
  extends FilePath
{
  private FilePath base;
  
  public FilePathWrapper getPath(String paramString)
  {
    return create(paramString, unwrap(paramString));
  }
  
  public FilePathWrapper wrap(FilePath paramFilePath)
  {
    return paramFilePath == null ? null : create(getPrefix() + paramFilePath.name, paramFilePath);
  }
  
  public FilePath unwrap()
  {
    return unwrap(this.name);
  }
  
  private FilePathWrapper create(String paramString, FilePath paramFilePath)
  {
    try
    {
      FilePathWrapper localFilePathWrapper = (FilePathWrapper)getClass().newInstance();
      localFilePathWrapper.name = paramString;
      localFilePathWrapper.base = paramFilePath;
      return localFilePathWrapper;
    }
    catch (Exception localException)
    {
      throw new IllegalArgumentException("Path: " + paramString, localException);
    }
  }
  
  protected String getPrefix()
  {
    return getScheme() + ":";
  }
  
  protected FilePath unwrap(String paramString)
  {
    return FilePath.get(paramString.substring(getScheme().length() + 1));
  }
  
  protected FilePath getBase()
  {
    return this.base;
  }
  
  public boolean canWrite()
  {
    return this.base.canWrite();
  }
  
  public void createDirectory()
  {
    this.base.createDirectory();
  }
  
  public boolean createFile()
  {
    return this.base.createFile();
  }
  
  public void delete()
  {
    this.base.delete();
  }
  
  public boolean exists()
  {
    return this.base.exists();
  }
  
  public FilePath getParent()
  {
    return wrap(this.base.getParent());
  }
  
  public boolean isAbsolute()
  {
    return this.base.isAbsolute();
  }
  
  public boolean isDirectory()
  {
    return this.base.isDirectory();
  }
  
  public long lastModified()
  {
    return this.base.lastModified();
  }
  
  public FilePath toRealPath()
  {
    return wrap(this.base.toRealPath());
  }
  
  public List<FilePath> newDirectoryStream()
  {
    List localList = this.base.newDirectoryStream();
    int i = 0;
    for (int j = localList.size(); i < j; i++) {
      localList.set(i, wrap((FilePath)localList.get(i)));
    }
    return localList;
  }
  
  public void moveTo(FilePath paramFilePath, boolean paramBoolean)
  {
    this.base.moveTo(((FilePathWrapper)paramFilePath).base, paramBoolean);
  }
  
  public InputStream newInputStream()
    throws IOException
  {
    return this.base.newInputStream();
  }
  
  public OutputStream newOutputStream(boolean paramBoolean)
    throws IOException
  {
    return this.base.newOutputStream(paramBoolean);
  }
  
  public FileChannel open(String paramString)
    throws IOException
  {
    return this.base.open(paramString);
  }
  
  public boolean setReadOnly()
  {
    return this.base.setReadOnly();
  }
  
  public long size()
  {
    return this.base.size();
  }
  
  public FilePath createTempFile(String paramString, boolean paramBoolean1, boolean paramBoolean2)
    throws IOException
  {
    return wrap(this.base.createTempFile(paramString, paramBoolean1, paramBoolean2));
  }
}
