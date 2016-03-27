package org.h2.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import org.h2.engine.Constants;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.store.fs.FileUtils;

public class IOUtils
{
  public static void closeSilently(Closeable paramCloseable)
  {
    if (paramCloseable != null) {
      try
      {
        trace("closeSilently", null, paramCloseable);
        paramCloseable.close();
      }
      catch (Exception localException) {}
    }
  }
  
  public static void skipFully(InputStream paramInputStream, long paramLong)
    throws IOException
  {
    try
    {
      while (paramLong > 0L)
      {
        long l = paramInputStream.skip(paramLong);
        if (l <= 0L) {
          throw new EOFException();
        }
        paramLong -= l;
      }
    }
    catch (Exception localException)
    {
      throw DbException.convertToIOException(localException);
    }
  }
  
  public static void skipFully(Reader paramReader, long paramLong)
    throws IOException
  {
    try
    {
      while (paramLong > 0L)
      {
        long l = paramReader.skip(paramLong);
        if (l <= 0L) {
          throw new EOFException();
        }
        paramLong -= l;
      }
    }
    catch (Exception localException)
    {
      throw DbException.convertToIOException(localException);
    }
  }
  
  public static long copyAndClose(InputStream paramInputStream, OutputStream paramOutputStream)
    throws IOException
  {
    try
    {
      long l1 = copyAndCloseInput(paramInputStream, paramOutputStream);
      paramOutputStream.close();
      return l1;
    }
    catch (Exception localException)
    {
      throw DbException.convertToIOException(localException);
    }
    finally
    {
      closeSilently(paramOutputStream);
    }
  }
  
  public static long copyAndCloseInput(InputStream paramInputStream, OutputStream paramOutputStream)
    throws IOException
  {
    try
    {
      return copy(paramInputStream, paramOutputStream);
    }
    catch (Exception localException)
    {
      throw DbException.convertToIOException(localException);
    }
    finally
    {
      closeSilently(paramInputStream);
    }
  }
  
  public static long copy(InputStream paramInputStream, OutputStream paramOutputStream)
    throws IOException
  {
    return copy(paramInputStream, paramOutputStream, Long.MAX_VALUE);
  }
  
  public static long copy(InputStream paramInputStream, OutputStream paramOutputStream, long paramLong)
    throws IOException
  {
    try
    {
      long l = 0L;
      int i = (int)Math.min(paramLong, 4096L);
      byte[] arrayOfByte = new byte[i];
      while (paramLong > 0L)
      {
        i = paramInputStream.read(arrayOfByte, 0, i);
        if (i < 0) {
          break;
        }
        if (paramOutputStream != null) {
          paramOutputStream.write(arrayOfByte, 0, i);
        }
        l += i;
        paramLong -= i;
        i = (int)Math.min(paramLong, 4096L);
      }
      return l;
    }
    catch (Exception localException)
    {
      throw DbException.convertToIOException(localException);
    }
  }
  
  public static long copyAndCloseInput(Reader paramReader, Writer paramWriter, long paramLong)
    throws IOException
  {
    try
    {
      long l1 = 0L;
      int i = (int)Math.min(paramLong, 4096L);
      char[] arrayOfChar = new char[i];
      while (paramLong > 0L)
      {
        i = paramReader.read(arrayOfChar, 0, i);
        if (i < 0) {
          break;
        }
        if (paramWriter != null) {
          paramWriter.write(arrayOfChar, 0, i);
        }
        paramLong -= i;
        i = (int)Math.min(paramLong, 4096L);
        l1 += i;
      }
      return l1;
    }
    catch (Exception localException)
    {
      throw DbException.convertToIOException(localException);
    }
    finally
    {
      paramReader.close();
    }
  }
  
  public static void closeSilently(InputStream paramInputStream)
  {
    if (paramInputStream != null) {
      try
      {
        trace("closeSilently", null, paramInputStream);
        paramInputStream.close();
      }
      catch (Exception localException) {}
    }
  }
  
  public static void closeSilently(Reader paramReader)
  {
    if (paramReader != null) {
      try
      {
        paramReader.close();
      }
      catch (Exception localException) {}
    }
  }
  
  public static void closeSilently(Writer paramWriter)
  {
    if (paramWriter != null) {
      try
      {
        paramWriter.flush();
        paramWriter.close();
      }
      catch (Exception localException) {}
    }
  }
  
  public static byte[] readBytesAndClose(InputStream paramInputStream, int paramInt)
    throws IOException
  {
    try
    {
      if (paramInt <= 0) {
        paramInt = Integer.MAX_VALUE;
      }
      int i = Math.min(4096, paramInt);
      ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream(i);
      copy(paramInputStream, localByteArrayOutputStream, paramInt);
      return localByteArrayOutputStream.toByteArray();
    }
    catch (Exception localException)
    {
      throw DbException.convertToIOException(localException);
    }
    finally
    {
      paramInputStream.close();
    }
  }
  
  public static String readStringAndClose(Reader paramReader, int paramInt)
    throws IOException
  {
    try
    {
      if (paramInt <= 0) {
        paramInt = Integer.MAX_VALUE;
      }
      int i = Math.min(4096, paramInt);
      StringWriter localStringWriter = new StringWriter(i);
      copyAndCloseInput(paramReader, localStringWriter, paramInt);
      return localStringWriter.toString();
    }
    finally
    {
      paramReader.close();
    }
  }
  
  public static int readFully(InputStream paramInputStream, byte[] paramArrayOfByte, int paramInt)
    throws IOException
  {
    try
    {
      int i = 0;int j = Math.min(paramInt, paramArrayOfByte.length);
      while (j > 0)
      {
        int k = paramInputStream.read(paramArrayOfByte, i, j);
        if (k < 0) {
          break;
        }
        i += k;
        j -= k;
      }
      return i;
    }
    catch (Exception localException)
    {
      throw DbException.convertToIOException(localException);
    }
  }
  
  public static int readFully(Reader paramReader, char[] paramArrayOfChar, int paramInt)
    throws IOException
  {
    try
    {
      int i = 0;int j = Math.min(paramInt, paramArrayOfChar.length);
      while (j > 0)
      {
        int k = paramReader.read(paramArrayOfChar, i, j);
        if (k < 0) {
          break;
        }
        i += k;
        j -= k;
      }
      return i;
    }
    catch (Exception localException)
    {
      throw DbException.convertToIOException(localException);
    }
  }
  
  public static Reader getBufferedReader(InputStream paramInputStream)
  {
    return paramInputStream == null ? null : new BufferedReader(new InputStreamReader(paramInputStream, Constants.UTF8));
  }
  
  public static Reader getReader(InputStream paramInputStream)
  {
    return paramInputStream == null ? null : new BufferedReader(new InputStreamReader(paramInputStream, Constants.UTF8));
  }
  
  public static Writer getBufferedWriter(OutputStream paramOutputStream)
  {
    return paramOutputStream == null ? null : new BufferedWriter(new OutputStreamWriter(paramOutputStream, Constants.UTF8));
  }
  
  public static Reader getAsciiReader(InputStream paramInputStream)
  {
    try
    {
      return paramInputStream == null ? null : new InputStreamReader(paramInputStream, "US-ASCII");
    }
    catch (Exception localException)
    {
      throw DbException.convert(localException);
    }
  }
  
  public static void trace(String paramString1, String paramString2, Object paramObject)
  {
    if (SysProperties.TRACE_IO) {
      System.out.println("IOUtils." + paramString1 + " " + paramString2 + " " + paramObject);
    }
  }
  
  public static InputStream getInputStreamFromString(String paramString)
  {
    if (paramString == null) {
      return null;
    }
    return new ByteArrayInputStream(paramString.getBytes(Constants.UTF8));
  }
  
  public static void copyFiles(String paramString1, String paramString2)
    throws IOException
  {
    InputStream localInputStream = FileUtils.newInputStream(paramString1);
    OutputStream localOutputStream = FileUtils.newOutputStream(paramString2, false);
    copyAndClose(localInputStream, localOutputStream);
  }
}
