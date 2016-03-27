package org.h2.store.fs;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import org.h2.util.New;

public class FileUtils
{
  public static boolean exists(String paramString)
  {
    return FilePath.get(paramString).exists();
  }
  
  public static void createDirectory(String paramString)
  {
    FilePath.get(paramString).createDirectory();
  }
  
  public static boolean createFile(String paramString)
  {
    return FilePath.get(paramString).createFile();
  }
  
  public static void delete(String paramString)
  {
    FilePath.get(paramString).delete();
  }
  
  public static String toRealPath(String paramString)
  {
    return FilePath.get(paramString).toRealPath().toString();
  }
  
  public static String getParent(String paramString)
  {
    FilePath localFilePath = FilePath.get(paramString).getParent();
    return localFilePath == null ? null : localFilePath.toString();
  }
  
  public static boolean isAbsolute(String paramString)
  {
    return FilePath.get(paramString).isAbsolute();
  }
  
  public static void move(String paramString1, String paramString2)
  {
    FilePath.get(paramString1).moveTo(FilePath.get(paramString2), false);
  }
  
  public static void moveAtomicReplace(String paramString1, String paramString2)
  {
    FilePath.get(paramString1).moveTo(FilePath.get(paramString2), true);
  }
  
  public static String getName(String paramString)
  {
    return FilePath.get(paramString).getName();
  }
  
  public static List<String> newDirectoryStream(String paramString)
  {
    List localList = FilePath.get(paramString).newDirectoryStream();
    int i = localList.size();
    ArrayList localArrayList = New.arrayList(i);
    for (int j = 0; j < i; j++) {
      localArrayList.add(((FilePath)localList.get(j)).toString());
    }
    return localArrayList;
  }
  
  public static long lastModified(String paramString)
  {
    return FilePath.get(paramString).lastModified();
  }
  
  public static long size(String paramString)
  {
    return FilePath.get(paramString).size();
  }
  
  public static boolean isDirectory(String paramString)
  {
    return FilePath.get(paramString).isDirectory();
  }
  
  public static FileChannel open(String paramString1, String paramString2)
    throws IOException
  {
    return FilePath.get(paramString1).open(paramString2);
  }
  
  public static InputStream newInputStream(String paramString)
    throws IOException
  {
    return FilePath.get(paramString).newInputStream();
  }
  
  public static OutputStream newOutputStream(String paramString, boolean paramBoolean)
    throws IOException
  {
    return FilePath.get(paramString).newOutputStream(paramBoolean);
  }
  
  public static boolean canWrite(String paramString)
  {
    return FilePath.get(paramString).canWrite();
  }
  
  public static boolean setReadOnly(String paramString)
  {
    return FilePath.get(paramString).setReadOnly();
  }
  
  public static String unwrap(String paramString)
  {
    return FilePath.get(paramString).unwrap().toString();
  }
  
  public static void deleteRecursive(String paramString, boolean paramBoolean)
  {
    if (exists(paramString))
    {
      if (isDirectory(paramString)) {
        for (String str : newDirectoryStream(paramString)) {
          deleteRecursive(str, paramBoolean);
        }
      }
      if (paramBoolean) {
        tryDelete(paramString);
      } else {
        delete(paramString);
      }
    }
  }
  
  public static void createDirectories(String paramString)
  {
    if (paramString != null) {
      if (exists(paramString))
      {
        if (!isDirectory(paramString)) {
          createDirectory(paramString);
        }
      }
      else
      {
        String str = getParent(paramString);
        createDirectories(str);
        createDirectory(paramString);
      }
    }
  }
  
  public static boolean tryDelete(String paramString)
  {
    try
    {
      FilePath.get(paramString).delete();
      return true;
    }
    catch (Exception localException) {}
    return false;
  }
  
  public static String createTempFile(String paramString1, String paramString2, boolean paramBoolean1, boolean paramBoolean2)
    throws IOException
  {
    return FilePath.get(paramString1).createTempFile(paramString2, paramBoolean1, paramBoolean2).toString();
  }
  
  public static void readFully(FileChannel paramFileChannel, ByteBuffer paramByteBuffer)
    throws IOException
  {
    do
    {
      int i = paramFileChannel.read(paramByteBuffer);
      if (i < 0) {
        throw new EOFException();
      }
    } while (paramByteBuffer.remaining() > 0);
  }
  
  public static void writeFully(FileChannel paramFileChannel, ByteBuffer paramByteBuffer)
    throws IOException
  {
    do
    {
      paramFileChannel.write(paramByteBuffer);
    } while (paramByteBuffer.remaining() > 0);
  }
}
