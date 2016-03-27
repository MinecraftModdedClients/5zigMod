package org.h2.store;

import java.lang.reflect.Array;
import org.h2.engine.Session;
import org.h2.util.CacheObject;

public abstract class Page
  extends CacheObject
{
  public static final int FLAG_LAST = 16;
  public static final int TYPE_EMPTY = 0;
  public static final int TYPE_DATA_LEAF = 1;
  public static final int TYPE_DATA_NODE = 2;
  public static final int TYPE_DATA_OVERFLOW = 3;
  public static final int TYPE_BTREE_LEAF = 4;
  public static final int TYPE_BTREE_NODE = 5;
  public static final int TYPE_FREE_LIST = 6;
  public static final int TYPE_STREAM_TRUNK = 7;
  public static final int TYPE_STREAM_DATA = 8;
  private static final int COPY_THRESHOLD = 4;
  protected long changeCount;
  
  public abstract void moveTo(Session paramSession, int paramInt);
  
  public abstract void write();
  
  public static <T> T[] insert(T[] paramArrayOfT, int paramInt1, int paramInt2, T paramT)
  {
    Object localObject;
    if (paramArrayOfT.length > paramInt1)
    {
      localObject = paramArrayOfT;
    }
    else
    {
      localObject = (Object[])Array.newInstance(paramArrayOfT.getClass().getComponentType(), paramInt1 + 1 + 4);
      if (paramInt2 > 0) {
        System.arraycopy(paramArrayOfT, 0, localObject, 0, paramInt2);
      }
    }
    if (paramInt1 - paramInt2 > 0) {
      System.arraycopy(paramArrayOfT, paramInt2, localObject, paramInt2 + 1, paramInt1 - paramInt2);
    }
    localObject[paramInt2] = paramT;
    return (T[])localObject;
  }
  
  public static <T> T[] remove(T[] paramArrayOfT, int paramInt1, int paramInt2)
  {
    Object localObject;
    if (paramArrayOfT.length - paramInt1 < 4)
    {
      localObject = paramArrayOfT;
    }
    else
    {
      localObject = (Object[])Array.newInstance(paramArrayOfT.getClass().getComponentType(), paramInt1 - 1);
      
      System.arraycopy(paramArrayOfT, 0, localObject, 0, Math.min(paramInt1 - 1, paramInt2));
    }
    if (paramInt2 < paramInt1) {
      System.arraycopy(paramArrayOfT, paramInt2 + 1, localObject, paramInt2, paramInt1 - paramInt2 - 1);
    }
    return (T[])localObject;
  }
  
  protected static long[] insert(long[] paramArrayOfLong, int paramInt1, int paramInt2, long paramLong)
  {
    long[] arrayOfLong;
    if ((paramArrayOfLong != null) && (paramArrayOfLong.length > paramInt1))
    {
      arrayOfLong = paramArrayOfLong;
    }
    else
    {
      arrayOfLong = new long[paramInt1 + 1 + 4];
      if (paramInt2 > 0) {
        System.arraycopy(paramArrayOfLong, 0, arrayOfLong, 0, paramInt2);
      }
    }
    if ((paramArrayOfLong != null) && (paramInt1 - paramInt2 > 0)) {
      System.arraycopy(paramArrayOfLong, paramInt2, arrayOfLong, paramInt2 + 1, paramInt1 - paramInt2);
    }
    arrayOfLong[paramInt2] = paramLong;
    return arrayOfLong;
  }
  
  protected static long[] remove(long[] paramArrayOfLong, int paramInt1, int paramInt2)
  {
    long[] arrayOfLong;
    if (paramArrayOfLong.length - paramInt1 < 4)
    {
      arrayOfLong = paramArrayOfLong;
    }
    else
    {
      arrayOfLong = new long[paramInt1 - 1];
      System.arraycopy(paramArrayOfLong, 0, arrayOfLong, 0, paramInt2);
    }
    System.arraycopy(paramArrayOfLong, paramInt2 + 1, arrayOfLong, paramInt2, paramInt1 - paramInt2 - 1);
    return arrayOfLong;
  }
  
  protected static int[] insert(int[] paramArrayOfInt, int paramInt1, int paramInt2, int paramInt3)
  {
    int[] arrayOfInt;
    if ((paramArrayOfInt != null) && (paramArrayOfInt.length > paramInt1))
    {
      arrayOfInt = paramArrayOfInt;
    }
    else
    {
      arrayOfInt = new int[paramInt1 + 1 + 4];
      if ((paramInt2 > 0) && (paramArrayOfInt != null)) {
        System.arraycopy(paramArrayOfInt, 0, arrayOfInt, 0, paramInt2);
      }
    }
    if ((paramArrayOfInt != null) && (paramInt1 - paramInt2 > 0)) {
      System.arraycopy(paramArrayOfInt, paramInt2, arrayOfInt, paramInt2 + 1, paramInt1 - paramInt2);
    }
    arrayOfInt[paramInt2] = paramInt3;
    return arrayOfInt;
  }
  
  protected static int[] remove(int[] paramArrayOfInt, int paramInt1, int paramInt2)
  {
    int[] arrayOfInt;
    if (paramArrayOfInt.length - paramInt1 < 4)
    {
      arrayOfInt = paramArrayOfInt;
    }
    else
    {
      arrayOfInt = new int[paramInt1 - 1];
      System.arraycopy(paramArrayOfInt, 0, arrayOfInt, 0, Math.min(paramInt1 - 1, paramInt2));
    }
    if (paramInt2 < paramInt1) {
      System.arraycopy(paramArrayOfInt, paramInt2 + 1, arrayOfInt, paramInt2, paramInt1 - paramInt2 - 1);
    }
    return arrayOfInt;
  }
  
  protected static void add(int[] paramArrayOfInt, int paramInt1, int paramInt2, int paramInt3)
  {
    for (int i = paramInt1; i < paramInt2; i++) {
      paramArrayOfInt[i] += paramInt3;
    }
  }
  
  public boolean canMove()
  {
    return true;
  }
}
