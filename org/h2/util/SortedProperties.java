package org.h2.util;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;
import org.h2.store.fs.FileUtils;

public class SortedProperties
  extends Properties
{
  private static final long serialVersionUID = 1L;
  
  public synchronized Enumeration<Object> keys()
  {
    Vector localVector = new Vector();
    for (Object localObject : keySet()) {
      localVector.add(localObject.toString());
    }
    Collections.sort(localVector);
    return new Vector(localVector).elements();
  }
  
  public static boolean getBooleanProperty(Properties paramProperties, String paramString, boolean paramBoolean)
  {
    String str = paramProperties.getProperty(paramString, "" + paramBoolean);
    try
    {
      return Boolean.parseBoolean(str);
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
    return paramBoolean;
  }
  
  public static int getIntProperty(Properties paramProperties, String paramString, int paramInt)
  {
    String str = paramProperties.getProperty(paramString, "" + paramInt);
    try
    {
      return Integer.decode(str).intValue();
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
    return paramInt;
  }
  
  public static synchronized SortedProperties loadProperties(String paramString)
    throws IOException
  {
    SortedProperties localSortedProperties = new SortedProperties();
    if (FileUtils.exists(paramString))
    {
      InputStream localInputStream = null;
      try
      {
        localInputStream = FileUtils.newInputStream(paramString);
        localSortedProperties.load(localInputStream);
      }
      finally
      {
        if (localInputStream != null) {
          localInputStream.close();
        }
      }
    }
    return localSortedProperties;
  }
  
  public synchronized void store(String paramString)
    throws IOException
  {
    ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
    store(localByteArrayOutputStream, null);
    ByteArrayInputStream localByteArrayInputStream = new ByteArrayInputStream(localByteArrayOutputStream.toByteArray());
    InputStreamReader localInputStreamReader = new InputStreamReader(localByteArrayInputStream, "ISO8859-1");
    LineNumberReader localLineNumberReader = new LineNumberReader(localInputStreamReader);
    OutputStreamWriter localOutputStreamWriter;
    try
    {
      localOutputStreamWriter = new OutputStreamWriter(FileUtils.newOutputStream(paramString, false));
    }
    catch (Exception localException)
    {
      throw new IOException(localException.toString(), localException);
    }
    PrintWriter localPrintWriter = new PrintWriter(new BufferedWriter(localOutputStreamWriter));
    for (;;)
    {
      String str = localLineNumberReader.readLine();
      if (str == null) {
        break;
      }
      if (!str.startsWith("#")) {
        localPrintWriter.print(str + "\n");
      }
    }
    localPrintWriter.close();
  }
  
  public synchronized String toLines()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    for (Map.Entry localEntry : new TreeMap(this).entrySet()) {
      localStringBuilder.append(localEntry.getKey()).append('=').append(localEntry.getValue()).append('\n');
    }
    return localStringBuilder.toString();
  }
  
  public static SortedProperties fromLines(String paramString)
  {
    SortedProperties localSortedProperties = new SortedProperties();
    for (String str : StringUtils.arraySplit(paramString, '\n', true))
    {
      int k = str.indexOf('=');
      if (k > 0) {
        localSortedProperties.put(str.substring(0, k), str.substring(k + 1));
      }
    }
    return localSortedProperties;
  }
}
