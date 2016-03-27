package org.h2.store.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.util.New;

public class FilePathSplit
  extends FilePathWrapper
{
  private static final String PART_SUFFIX = ".part";
  
  protected String getPrefix()
  {
    return getScheme() + ":" + parse(this.name)[0] + ":";
  }
  
  public FilePath unwrap(String paramString)
  {
    return FilePath.get(parse(paramString)[1]);
  }
  
  public boolean setReadOnly()
  {
    boolean bool = false;
    for (int i = 0;; i++)
    {
      FilePath localFilePath = getBase(i);
      if (!localFilePath.exists()) {
        break;
      }
      bool = localFilePath.setReadOnly();
    }
    return bool;
  }
  
  public void delete()
  {
    for (int i = 0;; i++)
    {
      FilePath localFilePath = getBase(i);
      if (!localFilePath.exists()) {
        break;
      }
      localFilePath.delete();
    }
  }
  
  public long lastModified()
  {
    long l1 = 0L;
    for (int i = 0;; i++)
    {
      FilePath localFilePath = getBase(i);
      if (!localFilePath.exists()) {
        break;
      }
      long l2 = localFilePath.lastModified();
      l1 = Math.max(l1, l2);
    }
    return l1;
  }
  
  public long size()
  {
    long l = 0L;
    for (int i = 0;; i++)
    {
      FilePath localFilePath = getBase(i);
      if (!localFilePath.exists()) {
        break;
      }
      l += localFilePath.size();
    }
    return l;
  }
  
  public ArrayList<FilePath> newDirectoryStream()
  {
    List localList = getBase().newDirectoryStream();
    ArrayList localArrayList = New.arrayList();
    int i = 0;
    for (int j = localList.size(); i < j; i++)
    {
      FilePath localFilePath = (FilePath)localList.get(i);
      if (!localFilePath.getName().endsWith(".part")) {
        localArrayList.add(wrap(localFilePath));
      }
    }
    return localArrayList;
  }
  
  public InputStream newInputStream()
    throws IOException
  {
    Object localObject = getBase().newInputStream();
    for (int i = 1;; i++)
    {
      FilePath localFilePath = getBase(i);
      if (!localFilePath.exists()) {
        break;
      }
      InputStream localInputStream = localFilePath.newInputStream();
      localObject = new SequenceInputStream((InputStream)localObject, localInputStream);
    }
    return (InputStream)localObject;
  }
  
  public FileChannel open(String paramString)
    throws IOException
  {
    ArrayList localArrayList = New.arrayList();
    localArrayList.add(getBase().open(paramString));
    for (int i = 1;; i++)
    {
      FilePath localFilePath = getBase(i);
      if (!localFilePath.exists()) {
        break;
      }
      localArrayList.add(localFilePath.open(paramString));
    }
    FileChannel[] arrayOfFileChannel = new FileChannel[localArrayList.size()];
    localArrayList.toArray(arrayOfFileChannel);
    long l1 = arrayOfFileChannel[0].size();
    long l2 = l1;
    if (arrayOfFileChannel.length == 1)
    {
      long l3 = getDefaultMaxLength();
      if (l1 < l3) {
        l1 = l3;
      }
    }
    else
    {
      if (l1 == 0L) {
        closeAndThrow(0, arrayOfFileChannel, arrayOfFileChannel[0], l1);
      }
      for (int j = 1; j < arrayOfFileChannel.length - 1; j++)
      {
        FileChannel localFileChannel2 = arrayOfFileChannel[j];
        long l5 = localFileChannel2.size();
        l2 += l5;
        if (l5 != l1) {
          closeAndThrow(j, arrayOfFileChannel, localFileChannel2, l1);
        }
      }
      FileChannel localFileChannel1 = arrayOfFileChannel[(arrayOfFileChannel.length - 1)];
      long l4 = localFileChannel1.size();
      l2 += l4;
      if (l4 > l1) {
        closeAndThrow(arrayOfFileChannel.length - 1, arrayOfFileChannel, localFileChannel1, l1);
      }
    }
    return new FileSplit(this, paramString, arrayOfFileChannel, l2, l1);
  }
  
  private long getDefaultMaxLength()
  {
    return 1L << Integer.decode(parse(this.name)[0]).intValue();
  }
  
  private void closeAndThrow(int paramInt, FileChannel[] paramArrayOfFileChannel, FileChannel paramFileChannel, long paramLong)
    throws IOException
  {
    String str = "Expected file length: " + paramLong + " got: " + paramFileChannel.size() + " for " + getName(paramInt);
    for (FileChannel localFileChannel : paramArrayOfFileChannel) {
      localFileChannel.close();
    }
    throw new IOException(str);
  }
  
  public OutputStream newOutputStream(boolean paramBoolean)
    throws IOException
  {
    return new FileChannelOutputStream(open("rw"), paramBoolean);
  }
  
  public void moveTo(FilePath paramFilePath, boolean paramBoolean)
  {
    FilePathSplit localFilePathSplit = (FilePathSplit)paramFilePath;
    for (int i = 0;; i++)
    {
      FilePath localFilePath = getBase(i);
      if (!localFilePath.exists()) {
        break;
      }
      localFilePath.moveTo(localFilePathSplit.getBase(i), paramBoolean);
    }
  }
  
  private String[] parse(String paramString)
  {
    if (!paramString.startsWith(getScheme())) {
      DbException.throwInternalError(paramString + " doesn't start with " + getScheme());
    }
    paramString = paramString.substring(getScheme().length() + 1);
    String str;
    if ((paramString.length() > 0) && (Character.isDigit(paramString.charAt(0))))
    {
      int i = paramString.indexOf(':');
      str = paramString.substring(0, i);
      try
      {
        paramString = paramString.substring(i + 1);
      }
      catch (NumberFormatException localNumberFormatException) {}
    }
    else
    {
      str = Long.toString(SysProperties.SPLIT_FILE_SIZE_SHIFT);
    }
    return new String[] { str, paramString };
  }
  
  FilePath getBase(int paramInt)
  {
    return FilePath.get(getName(paramInt));
  }
  
  private String getName(int paramInt)
  {
    return paramInt > 0 ? getBase().name + "." + paramInt + ".part" : getBase().name;
  }
  
  public String getScheme()
  {
    return "split";
  }
}
