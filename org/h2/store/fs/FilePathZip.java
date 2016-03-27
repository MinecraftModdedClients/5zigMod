package org.h2.store.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.h2.message.DbException;
import org.h2.util.New;

public class FilePathZip
  extends FilePath
{
  public FilePathZip getPath(String paramString)
  {
    FilePathZip localFilePathZip = new FilePathZip();
    localFilePathZip.name = paramString;
    return localFilePathZip;
  }
  
  public void createDirectory() {}
  
  public boolean createFile()
  {
    throw DbException.getUnsupportedException("write");
  }
  
  public void delete()
  {
    throw DbException.getUnsupportedException("write");
  }
  
  public boolean exists()
  {
    try
    {
      String str = getEntryName();
      if (str.length() == 0) {
        return true;
      }
      ZipFile localZipFile = openZipFile();
      try
      {
        return localZipFile.getEntry(str) != null;
      }
      finally
      {
        localZipFile.close();
      }
      return false;
    }
    catch (IOException localIOException) {}
  }
  
  public long lastModified()
  {
    return 0L;
  }
  
  public FilePath getParent()
  {
    int i = this.name.lastIndexOf('/');
    return i < 0 ? null : getPath(this.name.substring(0, i));
  }
  
  public boolean isAbsolute()
  {
    String str = translateFileName(this.name);
    return FilePath.get(str).isAbsolute();
  }
  
  public FilePath unwrap()
  {
    return FilePath.get(this.name.substring(getScheme().length() + 1));
  }
  
  public boolean isDirectory()
  {
    try
    {
      String str1 = getEntryName();
      if (str1.length() == 0) {
        return true;
      }
      ZipFile localZipFile = openZipFile();
      try
      {
        Enumeration localEnumeration = localZipFile.entries();
        while (localEnumeration.hasMoreElements())
        {
          ZipEntry localZipEntry = (ZipEntry)localEnumeration.nextElement();
          String str2 = localZipEntry.getName();
          boolean bool;
          if (str2.equals(str1)) {
            return localZipEntry.isDirectory();
          }
          if ((str2.startsWith(str1)) && 
            (str2.length() == str1.length() + 1) && 
            (str2.equals(str1 + "/"))) {
            return true;
          }
        }
      }
      finally
      {
        localZipFile.close();
      }
      return false;
    }
    catch (IOException localIOException) {}
    return false;
  }
  
  public boolean canWrite()
  {
    return false;
  }
  
  public boolean setReadOnly()
  {
    return true;
  }
  
  public long size()
  {
    try
    {
      ZipFile localZipFile = openZipFile();
      try
      {
        ZipEntry localZipEntry = localZipFile.getEntry(getEntryName());
        return localZipEntry == null ? 0L : localZipEntry.getSize();
      }
      finally
      {
        localZipFile.close();
      }
      return 0L;
    }
    catch (IOException localIOException) {}
  }
  
  public ArrayList<FilePath> newDirectoryStream()
  {
    String str1 = this.name;
    ArrayList localArrayList = New.arrayList();
    try
    {
      if (str1.indexOf('!') < 0) {
        str1 = str1 + "!";
      }
      if (!str1.endsWith("/")) {
        str1 = str1 + "/";
      }
      ZipFile localZipFile = openZipFile();
      try
      {
        String str2 = getEntryName();
        String str3 = str1.substring(0, str1.length() - str2.length());
        Enumeration localEnumeration = localZipFile.entries();
        while (localEnumeration.hasMoreElements())
        {
          ZipEntry localZipEntry = (ZipEntry)localEnumeration.nextElement();
          String str4 = localZipEntry.getName();
          if ((str4.startsWith(str2)) && 
          
            (str4.length() > str2.length()))
          {
            int i = str4.indexOf('/', str2.length());
            if ((i < 0) || (i >= str4.length() - 1)) {
              localArrayList.add(getPath(str3 + str4));
            }
          }
        }
      }
      finally
      {
        localZipFile.close();
      }
      return localArrayList;
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, "listFiles " + str1);
    }
  }
  
  public InputStream newInputStream()
    throws IOException
  {
    return new FileChannelInputStream(open("r"), true);
  }
  
  public FileChannel open(String paramString)
    throws IOException
  {
    ZipFile localZipFile = openZipFile();
    ZipEntry localZipEntry = localZipFile.getEntry(getEntryName());
    if (localZipEntry == null)
    {
      localZipFile.close();
      throw new FileNotFoundException(this.name);
    }
    return new FileZip(localZipFile, localZipEntry);
  }
  
  public OutputStream newOutputStream(boolean paramBoolean)
    throws IOException
  {
    throw new IOException("write");
  }
  
  public void moveTo(FilePath paramFilePath, boolean paramBoolean)
  {
    throw DbException.getUnsupportedException("write");
  }
  
  private static String translateFileName(String paramString)
  {
    if (paramString.startsWith("zip:")) {
      paramString = paramString.substring("zip:".length());
    }
    int i = paramString.indexOf('!');
    if (i >= 0) {
      paramString = paramString.substring(0, i);
    }
    return FilePathDisk.expandUserHomeDirectory(paramString);
  }
  
  public FilePath toRealPath()
  {
    return this;
  }
  
  private String getEntryName()
  {
    int i = this.name.indexOf('!');
    if (i <= 0) {
      str = "";
    } else {
      str = this.name.substring(i + 1);
    }
    String str = str.replace('\\', '/');
    if (str.startsWith("/")) {
      str = str.substring(1);
    }
    return str;
  }
  
  private ZipFile openZipFile()
    throws IOException
  {
    String str = translateFileName(this.name);
    return new ZipFile(str);
  }
  
  public FilePath createTempFile(String paramString, boolean paramBoolean1, boolean paramBoolean2)
    throws IOException
  {
    if (!paramBoolean2) {
      throw new IOException("File system is read-only");
    }
    return new FilePathDisk().getPath(this.name).createTempFile(paramString, paramBoolean1, true);
  }
  
  public String getScheme()
  {
    return "zip";
  }
}
