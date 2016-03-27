package org.h2.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils
{
  public static final byte[] EMPTY_BYTES = new byte[0];
  public static final int[] EMPTY_INT_ARRAY = new int[0];
  private static final long[] EMPTY_LONG_ARRAY = new long[0];
  private static final int GC_DELAY = 50;
  private static final int MAX_GC = 8;
  private static long lastGC;
  private static final HashMap<String, byte[]> RESOURCES = New.hashMap();
  
  private static int readInt(byte[] paramArrayOfByte, int paramInt)
  {
    return (paramArrayOfByte[(paramInt++)] << 24) + ((paramArrayOfByte[(paramInt++)] & 0xFF) << 16) + ((paramArrayOfByte[(paramInt++)] & 0xFF) << 8) + (paramArrayOfByte[paramInt] & 0xFF);
  }
  
  public static void writeLong(byte[] paramArrayOfByte, int paramInt, long paramLong)
  {
    writeInt(paramArrayOfByte, paramInt, (int)(paramLong >> 32));
    writeInt(paramArrayOfByte, paramInt + 4, (int)paramLong);
  }
  
  private static void writeInt(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    paramArrayOfByte[(paramInt1++)] = ((byte)(paramInt2 >> 24));
    paramArrayOfByte[(paramInt1++)] = ((byte)(paramInt2 >> 16));
    paramArrayOfByte[(paramInt1++)] = ((byte)(paramInt2 >> 8));
    paramArrayOfByte[(paramInt1++)] = ((byte)paramInt2);
  }
  
  public static long readLong(byte[] paramArrayOfByte, int paramInt)
  {
    return (readInt(paramArrayOfByte, paramInt) << 32) + (readInt(paramArrayOfByte, paramInt + 4) & 0xFFFFFFFF);
  }
  
  public static int indexOf(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt)
  {
    if (paramArrayOfByte2.length == 0) {
      return paramInt;
    }
    if (paramInt > paramArrayOfByte1.length) {
      return -1;
    }
    int i = paramArrayOfByte1.length - paramArrayOfByte2.length + 1;
    int j = paramArrayOfByte2.length;
    label66:
    for (; paramInt < i; paramInt++)
    {
      for (int k = 0; k < j; k++) {
        if (paramArrayOfByte1[(paramInt + k)] != paramArrayOfByte2[k]) {
          break label66;
        }
      }
      return paramInt;
    }
    return -1;
  }
  
  public static int getByteArrayHash(byte[] paramArrayOfByte)
  {
    int i = paramArrayOfByte.length;
    int j = i;
    int k;
    if (i < 50)
    {
      for (k = 0; k < i; k++) {
        j = 31 * j + paramArrayOfByte[k];
      }
    }
    else
    {
      k = i / 16;
      for (int m = 0; m < 4; m++)
      {
        j = 31 * j + paramArrayOfByte[m];
        j = 31 * j + paramArrayOfByte[(--i)];
      }
      for (m = 4 + k; m < i; m += k) {
        j = 31 * j + paramArrayOfByte[m];
      }
    }
    return j;
  }
  
  public static boolean compareSecure(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
  {
    if ((paramArrayOfByte1 == null) || (paramArrayOfByte2 == null)) {
      return (paramArrayOfByte1 == null) && (paramArrayOfByte2 == null);
    }
    int i = paramArrayOfByte1.length;
    if (i != paramArrayOfByte2.length) {
      return false;
    }
    if (i == 0) {
      return true;
    }
    int j = 0;
    for (int k = 0; k < i; k++) {
      j |= paramArrayOfByte1[k] ^ paramArrayOfByte2[k];
    }
    return j == 0;
  }
  
  public static int compareNotNullSigned(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
  {
    if (paramArrayOfByte1 == paramArrayOfByte2) {
      return 0;
    }
    int i = Math.min(paramArrayOfByte1.length, paramArrayOfByte2.length);
    for (int j = 0; j < i; j++)
    {
      int k = paramArrayOfByte1[j];
      int m = paramArrayOfByte2[j];
      if (k != m) {
        return k > m ? 1 : -1;
      }
    }
    return Integer.signum(paramArrayOfByte1.length - paramArrayOfByte2.length);
  }
  
  public static int compareNotNullUnsigned(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
  {
    if (paramArrayOfByte1 == paramArrayOfByte2) {
      return 0;
    }
    int i = Math.min(paramArrayOfByte1.length, paramArrayOfByte2.length);
    for (int j = 0; j < i; j++)
    {
      int k = paramArrayOfByte1[j] & 0xFF;
      int m = paramArrayOfByte2[j] & 0xFF;
      if (k != m) {
        return k > m ? 1 : -1;
      }
    }
    return Integer.signum(paramArrayOfByte1.length - paramArrayOfByte2.length);
  }
  
  public static byte[] copy(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
  {
    int i = paramArrayOfByte1.length;
    if (i > paramArrayOfByte2.length) {
      paramArrayOfByte2 = new byte[i];
    }
    System.arraycopy(paramArrayOfByte1, 0, paramArrayOfByte2, 0, i);
    return paramArrayOfByte2;
  }
  
  public static byte[] cloneByteArray(byte[] paramArrayOfByte)
  {
    if (paramArrayOfByte == null) {
      return null;
    }
    int i = paramArrayOfByte.length;
    if (i == 0) {
      return EMPTY_BYTES;
    }
    byte[] arrayOfByte = new byte[i];
    System.arraycopy(paramArrayOfByte, 0, arrayOfByte, 0, i);
    return arrayOfByte;
  }
  
  public static int hashCode(Object paramObject)
  {
    return paramObject == null ? 0 : paramObject.hashCode();
  }
  
  public static int getMemoryUsed()
  {
    collectGarbage();
    Runtime localRuntime = Runtime.getRuntime();
    long l = localRuntime.totalMemory() - localRuntime.freeMemory();
    return (int)(l >> 10);
  }
  
  public static int getMemoryFree()
  {
    collectGarbage();
    Runtime localRuntime = Runtime.getRuntime();
    long l = localRuntime.freeMemory();
    return (int)(l >> 10);
  }
  
  public static long getMemoryMax()
  {
    long l = Runtime.getRuntime().maxMemory();
    return l / 1024L;
  }
  
  private static synchronized void collectGarbage()
  {
    Runtime localRuntime = Runtime.getRuntime();
    long l1 = localRuntime.totalMemory();
    long l2 = System.currentTimeMillis();
    if (lastGC + 50L < l2) {
      for (int i = 0; i < 8; i++)
      {
        localRuntime.gc();
        long l3 = localRuntime.totalMemory();
        if (l3 == l1)
        {
          lastGC = System.currentTimeMillis();
          break;
        }
        l1 = l3;
      }
    }
  }
  
  public static int[] newIntArray(int paramInt)
  {
    if (paramInt == 0) {
      return EMPTY_INT_ARRAY;
    }
    return new int[paramInt];
  }
  
  public static long[] newLongArray(int paramInt)
  {
    if (paramInt == 0) {
      return EMPTY_LONG_ARRAY;
    }
    return new long[paramInt];
  }
  
  public static <X> void sortTopN(X[] paramArrayOfX, int paramInt1, int paramInt2, Comparator<? super X> paramComparator)
  {
    partitionTopN(paramArrayOfX, paramInt1, paramInt2, paramComparator);
    Arrays.sort(paramArrayOfX, paramInt1, (int)Math.min(paramInt1 + paramInt2, paramArrayOfX.length), paramComparator);
  }
  
  private static <X> void partitionTopN(X[] paramArrayOfX, int paramInt1, int paramInt2, Comparator<? super X> paramComparator)
  {
    partialQuickSort(paramArrayOfX, 0, paramArrayOfX.length - 1, paramComparator, paramInt1, paramInt1 + paramInt2 - 1);
  }
  
  private static <X> void partialQuickSort(X[] paramArrayOfX, int paramInt1, int paramInt2, Comparator<? super X> paramComparator, int paramInt3, int paramInt4)
  {
    if ((paramInt1 > paramInt4) || (paramInt2 < paramInt3) || ((paramInt1 > paramInt3) && (paramInt2 < paramInt4))) {
      return;
    }
    if (paramInt1 == paramInt2) {
      return;
    }
    int i = paramInt1;int j = paramInt2;
    
    int k = paramInt1 + MathUtils.randomInt(paramInt2 - paramInt1);
    X ? = paramArrayOfX[k];
    int m = paramInt1 + paramInt2 >>> 1;
    X ? = paramArrayOfX[m];
    paramArrayOfX[m] = ?;
    paramArrayOfX[k] = ?;
    while (i <= j)
    {
      while (paramComparator.compare(paramArrayOfX[i], ?) < 0) {
        i++;
      }
      while (paramComparator.compare(paramArrayOfX[j], ?) > 0) {
        j--;
      }
      if (i <= j)
      {
        ? = paramArrayOfX[i];
        paramArrayOfX[(i++)] = paramArrayOfX[j];
        paramArrayOfX[(j--)] = ?;
      }
    }
    if (paramInt1 < j) {
      partialQuickSort(paramArrayOfX, paramInt1, j, paramComparator, paramInt3, paramInt4);
    }
    if (i < paramInt2) {
      partialQuickSort(paramArrayOfX, i, paramInt2, paramComparator, paramInt3, paramInt4);
    }
  }
  
  public static boolean haveCommonComparableSuperclass(Class<?> paramClass1, Class<?> paramClass2)
  {
    if ((paramClass1 == paramClass2) || (paramClass1.isAssignableFrom(paramClass2)) || (paramClass2.isAssignableFrom(paramClass1))) {
      return true;
    }
    Class<?> localClass1;
    do
    {
      localClass1 = paramClass1;
      paramClass1 = paramClass1.getSuperclass();
    } while (Comparable.class.isAssignableFrom(paramClass1));
    Class<?> localClass2;
    do
    {
      localClass2 = paramClass2;
      paramClass2 = paramClass2.getSuperclass();
    } while (Comparable.class.isAssignableFrom(paramClass2));
    return localClass1 == localClass2;
  }
  
  public static byte[] getResource(String paramString)
    throws IOException
  {
    byte[] arrayOfByte = (byte[])RESOURCES.get(paramString);
    if (arrayOfByte == null)
    {
      arrayOfByte = loadResource(paramString);
      if (arrayOfByte != null) {
        RESOURCES.put(paramString, arrayOfByte);
      }
    }
    return arrayOfByte;
  }
  
  private static byte[] loadResource(String paramString)
    throws IOException
  {
    InputStream localInputStream = Utils.class.getResourceAsStream("data.zip");
    if (localInputStream == null)
    {
      localInputStream = Utils.class.getResourceAsStream(paramString);
      if (localInputStream == null) {
        return null;
      }
      return IOUtils.readBytesAndClose(localInputStream, 0);
    }
    ZipInputStream localZipInputStream = new ZipInputStream(localInputStream);
    try
    {
      for (;;)
      {
        ZipEntry localZipEntry = localZipInputStream.getNextEntry();
        if (localZipEntry == null) {
          break;
        }
        String str = localZipEntry.getName();
        if (!str.startsWith("/")) {
          str = "/" + str;
        }
        if (str.equals(paramString))
        {
          ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
          IOUtils.copy(localZipInputStream, localByteArrayOutputStream);
          localZipInputStream.closeEntry();
          return localByteArrayOutputStream.toByteArray();
        }
        localZipInputStream.closeEntry();
      }
    }
    catch (IOException localIOException)
    {
      localIOException.printStackTrace();
    }
    finally
    {
      localZipInputStream.close();
    }
    return null;
  }
  
  public static Object callStaticMethod(String paramString, Object... paramVarArgs)
    throws Exception
  {
    int i = paramString.lastIndexOf('.');
    String str1 = paramString.substring(0, i);
    String str2 = paramString.substring(i + 1);
    return callMethod(null, Class.forName(str1), str2, paramVarArgs);
  }
  
  public static Object callMethod(Object paramObject, String paramString, Object... paramVarArgs)
    throws Exception
  {
    return callMethod(paramObject, paramObject.getClass(), paramString, paramVarArgs);
  }
  
  private static Object callMethod(Object paramObject, Class<?> paramClass, String paramString, Object... paramVarArgs)
    throws Exception
  {
    Object localObject = null;
    int i = 0;
    int j = paramObject == null ? 1 : 0;
    for (Method localMethod : paramClass.getMethods()) {
      if ((Modifier.isStatic(localMethod.getModifiers()) == j) && (localMethod.getName().equals(paramString)))
      {
        int n = match(localMethod.getParameterTypes(), paramVarArgs);
        if (n > i)
        {
          i = n;
          localObject = localMethod;
        }
      }
    }
    if (localObject == null) {
      throw new NoSuchMethodException(paramString);
    }
    return ((Method)localObject).invoke(paramObject, paramVarArgs);
  }
  
  public static Object newInstance(String paramString, Object... paramVarArgs)
    throws Exception
  {
    Object localObject = null;
    int i = 0;
    for (Constructor localConstructor : Class.forName(paramString).getConstructors())
    {
      int m = match(localConstructor.getParameterTypes(), paramVarArgs);
      if (m > i)
      {
        i = m;
        localObject = localConstructor;
      }
    }
    if (localObject == null) {
      throw new NoSuchMethodException(paramString);
    }
    return ((Constructor)localObject).newInstance(paramVarArgs);
  }
  
  private static int match(Class<?>[] paramArrayOfClass, Object[] paramArrayOfObject)
  {
    int i = paramArrayOfClass.length;
    if (i == paramArrayOfObject.length)
    {
      int j = 1;
      for (int k = 0; k < i; k++)
      {
        Class localClass1 = getNonPrimitiveClass(paramArrayOfClass[k]);
        Object localObject = paramArrayOfObject[k];
        Class localClass2 = localObject == null ? null : localObject.getClass();
        if (localClass1 == localClass2) {
          j++;
        } else if (localClass2 != null) {
          if (!localClass1.isAssignableFrom(localClass2)) {
            return 0;
          }
        }
      }
      return j;
    }
    return 0;
  }
  
  public static Object getStaticField(String paramString)
    throws Exception
  {
    int i = paramString.lastIndexOf('.');
    String str1 = paramString.substring(0, i);
    String str2 = paramString.substring(i + 1);
    return Class.forName(str1).getField(str2).get(null);
  }
  
  public static Object getField(Object paramObject, String paramString)
    throws Exception
  {
    return paramObject.getClass().getField(paramString).get(paramObject);
  }
  
  public static boolean isClassPresent(String paramString)
  {
    try
    {
      Class.forName(paramString);
      return true;
    }
    catch (ClassNotFoundException localClassNotFoundException) {}
    return false;
  }
  
  public static Class<?> getNonPrimitiveClass(Class<?> paramClass)
  {
    if (!paramClass.isPrimitive()) {
      return paramClass;
    }
    if (paramClass == Boolean.TYPE) {
      return Boolean.class;
    }
    if (paramClass == Byte.TYPE) {
      return Byte.class;
    }
    if (paramClass == Character.TYPE) {
      return Character.class;
    }
    if (paramClass == Double.TYPE) {
      return Double.class;
    }
    if (paramClass == Float.TYPE) {
      return Float.class;
    }
    if (paramClass == Integer.TYPE) {
      return Integer.class;
    }
    if (paramClass == Long.TYPE) {
      return Long.class;
    }
    if (paramClass == Short.TYPE) {
      return Short.class;
    }
    if (paramClass == Void.TYPE) {
      return Void.class;
    }
    return paramClass;
  }
  
  public static String getProperty(String paramString1, String paramString2)
  {
    try
    {
      return System.getProperty(paramString1, paramString2);
    }
    catch (SecurityException localSecurityException) {}
    return paramString2;
  }
  
  public static int getProperty(String paramString, int paramInt)
  {
    String str = getProperty(paramString, null);
    if (str != null) {
      try
      {
        return Integer.decode(str).intValue();
      }
      catch (NumberFormatException localNumberFormatException) {}
    }
    return paramInt;
  }
  
  public static boolean getProperty(String paramString, boolean paramBoolean)
  {
    String str = getProperty(paramString, null);
    if (str != null) {
      try
      {
        return Boolean.parseBoolean(str);
      }
      catch (NumberFormatException localNumberFormatException) {}
    }
    return paramBoolean;
  }
  
  public static int scaleForAvailableMemory(int paramInt)
  {
    long l1 = Runtime.getRuntime().maxMemory();
    if (l1 != Long.MAX_VALUE) {
      return (int)(paramInt * l1 / 1073741824L);
    }
    try
    {
      OperatingSystemMXBean localOperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
      
      Method localMethod = Class.forName("com.sun.management.OperatingSystemMXBean").getMethod("getTotalPhysicalMemorySize", new Class[0]);
      
      long l2 = ((Number)localMethod.invoke(localOperatingSystemMXBean, new Object[0])).longValue();
      return (int)(paramInt * l2 / 1073741824L);
    }
    catch (Exception localException) {}
    return paramInt;
  }
  
  public static abstract interface ClassFactory
  {
    public abstract boolean match(String paramString);
    
    public abstract Class<?> loadClass(String paramString)
      throws ClassNotFoundException;
  }
}
