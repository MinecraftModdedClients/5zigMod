package org.h2.tools;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.h2.message.DbException;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.util.StringUtils;
import org.h2.util.Tool;

public class ConvertTraceFile
  extends Tool
{
  private final HashMap<String, Stat> stats;
  private long timeTotal;
  
  public ConvertTraceFile()
  {
    this.stats = New.hashMap();
  }
  
  static class Stat
    implements Comparable<Stat>
  {
    String sql;
    int executeCount;
    long time;
    long resultCount;
    
    public int compareTo(Stat paramStat)
    {
      if (paramStat == this) {
        return 0;
      }
      int i = MathUtils.compareLong(paramStat.time, this.time);
      if (i == 0)
      {
        i = MathUtils.compareInt(paramStat.executeCount, this.executeCount);
        if (i == 0) {
          i = this.sql.compareTo(paramStat.sql);
        }
      }
      return i;
    }
  }
  
  public static void main(String... paramVarArgs)
    throws SQLException
  {
    new ConvertTraceFile().runTool(paramVarArgs);
  }
  
  public void runTool(String... paramVarArgs)
    throws SQLException
  {
    String str1 = "test.trace.db";
    String str2 = "Test";
    String str3 = "test.sql";
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++)
    {
      String str4 = paramVarArgs[i];
      if (str4.equals("-traceFile"))
      {
        str1 = paramVarArgs[(++i)];
      }
      else if (str4.equals("-javaClass"))
      {
        str2 = paramVarArgs[(++i)];
      }
      else if (str4.equals("-script"))
      {
        str3 = paramVarArgs[(++i)];
      }
      else
      {
        if ((str4.equals("-help")) || (str4.equals("-?")))
        {
          showUsage();
          return;
        }
        showUsageAndThrowUnsupportedOption(str4);
      }
    }
    try
    {
      convertFile(str1, str2, str3);
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, str1);
    }
  }
  
  private void convertFile(String paramString1, String paramString2, String paramString3)
    throws IOException
  {
    LineNumberReader localLineNumberReader = new LineNumberReader(IOUtils.getBufferedReader(FileUtils.newInputStream(paramString1)));
    
    PrintWriter localPrintWriter1 = new PrintWriter(IOUtils.getBufferedWriter(FileUtils.newOutputStream(paramString2 + ".java", false)));
    
    PrintWriter localPrintWriter2 = new PrintWriter(IOUtils.getBufferedWriter(FileUtils.newOutputStream(paramString3, false)));
    
    localPrintWriter1.println("import java.io.*;");
    localPrintWriter1.println("import java.sql.*;");
    localPrintWriter1.println("import java.math.*;");
    localPrintWriter1.println("import java.util.Calendar;");
    String str1 = paramString2.replace('\\', '/');
    int i = str1.lastIndexOf('/');
    if (i > 0) {
      str1 = str1.substring(i + 1);
    }
    localPrintWriter1.println("public class " + str1 + " {");
    localPrintWriter1.println("    public static void main(String... args) throws Exception {");
    
    localPrintWriter1.println("        Class.forName(\"org.h2.Driver\");");
    Object localObject1;
    Object localObject2;
    for (;;)
    {
      String str2 = localLineNumberReader.readLine();
      if (str2 == null) {
        break;
      }
      if (str2.startsWith("/**/"))
      {
        str2 = "        " + str2.substring(4);
        localPrintWriter1.println(str2);
      }
      else if (str2.startsWith("/*SQL"))
      {
        int k = str2.indexOf("*/");
        localObject1 = str2.substring(k + "*/".length());
        localObject1 = StringUtils.javaDecode((String)localObject1);
        str2 = str2.substring("/*SQL".length(), k);
        if (str2.length() > 0)
        {
          localObject2 = localObject1;
          int m = 0;
          long l = 0L;
          str2 = str2.trim();
          if (str2.length() > 0)
          {
            StringTokenizer localStringTokenizer = new StringTokenizer(str2, " :");
            while (localStringTokenizer.hasMoreElements())
            {
              String str3 = localStringTokenizer.nextToken();
              if ("l".equals(str3))
              {
                int n = Integer.parseInt(localStringTokenizer.nextToken());
                localObject2 = ((String)localObject1).substring(0, n) + ";";
              }
              else if ("#".equals(str3))
              {
                m = Integer.parseInt(localStringTokenizer.nextToken());
              }
              else if ("t".equals(str3))
              {
                l = Long.parseLong(localStringTokenizer.nextToken());
              }
            }
          }
          addToStats((String)localObject2, m, l);
        }
        localPrintWriter2.println((String)localObject1);
      }
    }
    localPrintWriter1.println("    }");
    localPrintWriter1.println('}');
    localLineNumberReader.close();
    localPrintWriter1.close();
    int j;
    if (this.stats.size() > 0)
    {
      localPrintWriter2.println("-----------------------------------------");
      localPrintWriter2.println("-- SQL Statement Statistics");
      localPrintWriter2.println("-- time: total time in milliseconds (accumulated)");
      localPrintWriter2.println("-- count: how many times the statement ran");
      localPrintWriter2.println("-- result: total update count or row count");
      localPrintWriter2.println("-----------------------------------------");
      localPrintWriter2.println("-- self accu    time   count  result sql");
      j = 0;
      ArrayList localArrayList = New.arrayList(this.stats.values());
      Collections.sort(localArrayList);
      if (this.timeTotal == 0L) {
        this.timeTotal = 1L;
      }
      for (localObject1 = localArrayList.iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (Stat)((Iterator)localObject1).next();
        j = (int)(j + ((Stat)localObject2).time);
        StringBuilder localStringBuilder = new StringBuilder(100);
        localStringBuilder.append("-- ").append(padNumberLeft(100L * ((Stat)localObject2).time / this.timeTotal, 3)).append("% ").append(padNumberLeft(100 * j / this.timeTotal, 3)).append('%').append(padNumberLeft(((Stat)localObject2).time, 8)).append(padNumberLeft(((Stat)localObject2).executeCount, 8)).append(padNumberLeft(((Stat)localObject2).resultCount, 8)).append(' ').append(removeNewlines(((Stat)localObject2).sql));
        
        localPrintWriter2.println(localStringBuilder.toString());
      }
    }
    localPrintWriter2.close();
  }
  
  private static String removeNewlines(String paramString)
  {
    return paramString == null ? paramString : paramString.replace('\r', ' ').replace('\n', ' ');
  }
  
  private static String padNumberLeft(long paramLong, int paramInt)
  {
    return StringUtils.pad(String.valueOf(paramLong), paramInt, " ", false);
  }
  
  private void addToStats(String paramString, int paramInt, long paramLong)
  {
    Stat localStat = (Stat)this.stats.get(paramString);
    if (localStat == null)
    {
      localStat = new Stat();
      localStat.sql = paramString;
      this.stats.put(paramString, localStat);
    }
    localStat.executeCount += 1;
    localStat.resultCount += paramInt;
    localStat.time += paramLong;
    this.timeTotal += paramLong;
  }
}
