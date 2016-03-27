package org.h2.tools;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.h2.util.New;
import org.h2.util.StringUtils;

public class MultiDimension
  implements Comparator<long[]>
{
  private static final MultiDimension INSTANCE = new MultiDimension();
  
  public static MultiDimension getInstance()
  {
    return INSTANCE;
  }
  
  public int normalize(int paramInt, double paramDouble1, double paramDouble2, double paramDouble3)
  {
    if ((paramDouble1 < paramDouble2) || (paramDouble1 > paramDouble3)) {
      throw new IllegalArgumentException(paramDouble2 + "<" + paramDouble1 + "<" + paramDouble3);
    }
    double d = (paramDouble1 - paramDouble2) / (paramDouble3 - paramDouble2);
    return (int)(d * getMaxValue(paramInt));
  }
  
  public int getMaxValue(int paramInt)
  {
    if ((paramInt < 2) || (paramInt > 32)) {
      throw new IllegalArgumentException("" + paramInt);
    }
    int i = getBitsPerValue(paramInt);
    return (int)((1L << i) - 1L);
  }
  
  private static int getBitsPerValue(int paramInt)
  {
    return Math.min(31, 64 / paramInt);
  }
  
  public long interleave(int... paramVarArgs)
  {
    int i = paramVarArgs.length;
    long l1 = getMaxValue(i);
    int j = getBitsPerValue(i);
    long l2 = 0L;
    for (int k = 0; k < i; k++)
    {
      long l3 = paramVarArgs[k];
      if ((l3 < 0L) || (l3 > l1)) {
        throw new IllegalArgumentException("0<" + l3 + "<" + l1);
      }
      for (int m = 0; m < j; m++) {
        l2 |= (l3 & 1L << m) << k + (i - 1) * m;
      }
    }
    return l2;
  }
  
  public long interleave(int paramInt1, int paramInt2)
  {
    if (paramInt1 < 0) {
      throw new IllegalArgumentException("0<" + paramInt1);
    }
    if (paramInt2 < 0) {
      throw new IllegalArgumentException("0<" + paramInt2);
    }
    long l = 0L;
    for (int i = 0; i < 32; i++)
    {
      l |= (paramInt1 & 1L << i) << i;
      l |= (paramInt2 & 1L << i) << i + 1;
    }
    return l;
  }
  
  public int deinterleave(int paramInt1, long paramLong, int paramInt2)
  {
    int i = getBitsPerValue(paramInt1);
    int j = 0;
    for (int k = 0; k < i; k++) {
      j = (int)(j | paramLong >> paramInt2 + (paramInt1 - 1) * k & 1L << k);
    }
    return j;
  }
  
  public String generatePreparedQuery(String paramString1, String paramString2, String[] paramArrayOfString)
  {
    StringBuilder localStringBuilder = new StringBuilder("SELECT D.* FROM ");
    localStringBuilder.append(StringUtils.quoteIdentifier(paramString1)).append(" D, TABLE(_FROM_ BIGINT=?, _TO_ BIGINT=?) WHERE ").append(StringUtils.quoteIdentifier(paramString2)).append(" BETWEEN _FROM_ AND _TO_");
    for (String str : paramArrayOfString) {
      localStringBuilder.append(" AND ").append(StringUtils.quoteIdentifier(str)).append("+1 BETWEEN ?+1 AND ?+1");
    }
    return localStringBuilder.toString();
  }
  
  public ResultSet getResult(PreparedStatement paramPreparedStatement, int[] paramArrayOfInt1, int[] paramArrayOfInt2)
    throws SQLException
  {
    long[][] arrayOfLong = getMortonRanges(paramArrayOfInt1, paramArrayOfInt2);
    int i = arrayOfLong.length;
    Long[] arrayOfLong1 = new Long[i];
    Long[] arrayOfLong2 = new Long[i];
    for (int j = 0; j < i; j++)
    {
      arrayOfLong1[j] = Long.valueOf(arrayOfLong[j][0]);
      arrayOfLong2[j] = Long.valueOf(arrayOfLong[j][1]);
    }
    paramPreparedStatement.setObject(1, arrayOfLong1);
    paramPreparedStatement.setObject(2, arrayOfLong2);
    i = paramArrayOfInt1.length;
    j = 0;
    for (int k = 3; j < i; j++)
    {
      paramPreparedStatement.setInt(k++, paramArrayOfInt1[j]);
      paramPreparedStatement.setInt(k++, paramArrayOfInt2[j]);
    }
    return paramPreparedStatement.executeQuery();
  }
  
  private long[][] getMortonRanges(int[] paramArrayOfInt1, int[] paramArrayOfInt2)
  {
    int i = paramArrayOfInt1.length;
    if (paramArrayOfInt2.length != i) {
      throw new IllegalArgumentException(i + "=" + paramArrayOfInt2.length);
    }
    for (int j = 0; j < i; j++) {
      if (paramArrayOfInt1[j] > paramArrayOfInt2[j])
      {
        int k = paramArrayOfInt1[j];
        paramArrayOfInt1[j] = paramArrayOfInt2[j];
        paramArrayOfInt2[j] = k;
      }
    }
    j = getSize(paramArrayOfInt1, paramArrayOfInt2, i);
    ArrayList localArrayList = New.arrayList();
    addMortonRanges(localArrayList, paramArrayOfInt1, paramArrayOfInt2, i, 0);
    combineEntries(localArrayList, j);
    long[][] arrayOfLong = new long[localArrayList.size()][2];
    localArrayList.toArray(arrayOfLong);
    return arrayOfLong;
  }
  
  private static int getSize(int[] paramArrayOfInt1, int[] paramArrayOfInt2, int paramInt)
  {
    int i = 1;
    for (int j = 0; j < paramInt; j++)
    {
      int k = paramArrayOfInt2[j] - paramArrayOfInt1[j];
      i *= (k + 1);
    }
    return i;
  }
  
  private void combineEntries(ArrayList<long[]> paramArrayList, int paramInt)
  {
    Collections.sort(paramArrayList, this);
    for (int i = 10; i < paramInt; i += i / 2)
    {
      long[] arrayOfLong;
      for (int j = 0; j < paramArrayList.size() - 1; j++)
      {
        localObject = (long[])paramArrayList.get(j);
        arrayOfLong = (long[])paramArrayList.get(j + 1);
        if (localObject[1] + i >= arrayOfLong[0])
        {
          localObject[1] = arrayOfLong[1];
          paramArrayList.remove(j + 1);
          j--;
        }
      }
      j = 0;
      for (Object localObject = paramArrayList.iterator(); ((Iterator)localObject).hasNext();)
      {
        arrayOfLong = (long[])((Iterator)localObject).next();
        j = (int)(j + (arrayOfLong[1] - arrayOfLong[0] + 1L));
      }
      if ((j > 2 * paramInt) || (paramArrayList.size() < 100)) {
        break;
      }
    }
  }
  
  public int compare(long[] paramArrayOfLong1, long[] paramArrayOfLong2)
  {
    return paramArrayOfLong1[0] > paramArrayOfLong2[0] ? 1 : -1;
  }
  
  private void addMortonRanges(ArrayList<long[]> paramArrayList, int[] paramArrayOfInt1, int[] paramArrayOfInt2, int paramInt1, int paramInt2)
  {
    if (paramInt2 > 100) {
      throw new IllegalArgumentException("" + paramInt2);
    }
    int i = 0;int j = 0;
    long l1 = 1L;
    for (int k = 0; k < paramInt1; k++)
    {
      int m = paramArrayOfInt2[k] - paramArrayOfInt1[k];
      if (m < 0) {
        throw new IllegalArgumentException("" + m);
      }
      l1 *= (m + 1);
      if (l1 < 0L) {
        throw new IllegalArgumentException("" + l1);
      }
      if (m > j)
      {
        j = m;
        i = k;
      }
    }
    long l2 = interleave(paramArrayOfInt1);long l3 = interleave(paramArrayOfInt2);
    if (l3 < l2) {
      throw new IllegalArgumentException(l3 + "<" + l2);
    }
    long l4 = l3 - l2 + 1L;
    if (l4 == l1)
    {
      long[] arrayOfLong = { l2, l3 };
      paramArrayList.add(arrayOfLong);
    }
    else
    {
      int n = findMiddle(paramArrayOfInt1[i], paramArrayOfInt2[i]);
      int i1 = paramArrayOfInt2[i];
      paramArrayOfInt2[i] = n;
      addMortonRanges(paramArrayList, paramArrayOfInt1, paramArrayOfInt2, paramInt1, paramInt2 + 1);
      paramArrayOfInt2[i] = i1;
      i1 = paramArrayOfInt1[i];
      paramArrayOfInt1[i] = (n + 1);
      addMortonRanges(paramArrayList, paramArrayOfInt1, paramArrayOfInt2, paramInt1, paramInt2 + 1);
      paramArrayOfInt1[i] = i1;
    }
  }
  
  private static int roundUp(int paramInt1, int paramInt2)
  {
    return paramInt1 + paramInt2 - 1 & -paramInt2;
  }
  
  private static int findMiddle(int paramInt1, int paramInt2)
  {
    int i = paramInt2 - paramInt1 - 1;
    if (i == 0) {
      return paramInt1;
    }
    if (i == 1) {
      return paramInt1 + 1;
    }
    int j = 0;
    while (1 << j < i) {
      j++;
    }
    j--;
    int k = roundUp(paramInt1 + 2, 1 << j) - 1;
    if ((k <= paramInt1) || (k >= paramInt2)) {
      throw new IllegalArgumentException(paramInt1 + "<" + k + "<" + paramInt2);
    }
    return k;
  }
}
