package org.h2.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Profiler
  implements Runnable
{
  private static Instrumentation instrumentation;
  private static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");
  private static final int MAX_ELEMENTS = 1000;
  public int interval = 2;
  public int depth = 48;
  public boolean paused;
  public boolean sumClasses;
  private int pid;
  private final String[] ignoreLines = "java,sun,com.sun.,com.google.common.,com.mongodb.".split(",");
  private final String[] ignorePackages = "java,sun,com.sun.,com.google.common.,com.mongodb.".split(",");
  private final String[] ignoreThreads = "java.lang.Object.wait,java.lang.Thread.dumpThreads,java.lang.Thread.getThreads,java.lang.Thread.sleep,java.lang.UNIXProcess.waitForProcessExit,java.net.PlainDatagramSocketImpl.receive0,java.net.PlainSocketImpl.accept,java.net.PlainSocketImpl.socketAccept,java.net.SocketInputStream.socketRead,java.net.SocketOutputStream.socketWrite,sun.awt.windows.WToolkit.eventLoop,sun.misc.Unsafe.park,sun.nio.ch.EPollArrayWrapper.epollWait,sun.nio.ch.KQueueArrayWrapper.kevent0,sun.nio.ch.ServerSocketChannelImpl.accept,dalvik.system.VMStack.getThreadStackTrace,dalvik.system.NativeStart.run".split(",");
  private volatile boolean stop;
  private final HashMap<String, Integer> counts = new HashMap();
  private final HashMap<String, Integer> summary = new HashMap();
  private int minCount = 1;
  private int total;
  private Thread thread;
  private long start;
  private long time;
  private int threadDumps;
  
  public static void premain(String paramString, Instrumentation paramInstrumentation)
  {
    instrumentation = paramInstrumentation;
  }
  
  public static Instrumentation getInstrumentation()
  {
    return instrumentation;
  }
  
  public static void main(String... paramVarArgs)
  {
    new Profiler().run(paramVarArgs);
  }
  
  private void run(String... paramVarArgs)
  {
    if (paramVarArgs.length == 0)
    {
      System.out.println("Show profiling data");
      System.out.println("Usage: java " + getClass().getName() + " <pid> | <stackTraceFileNames>");
      
      System.out.println("Processes:");
      String str1 = exec(new String[] { "jps", "-l" });
      System.out.println(str1);
      return;
    }
    this.start = System.currentTimeMillis();
    if (paramVarArgs[0].matches("\\d+"))
    {
      this.pid = Integer.parseInt(paramVarArgs[0]);
      long l1 = 0L;
      for (;;)
      {
        tick();
        long l2 = System.currentTimeMillis();
        if (l2 - l1 > 5000L)
        {
          this.time = (System.currentTimeMillis() - this.start);
          System.out.println(getTopTraces(3));
          l1 = l2;
        }
      }
    }
    try
    {
      for (String str2 : paramVarArgs)
      {
        InputStreamReader localInputStreamReader = new InputStreamReader(new FileInputStream(str2), "CP1252");
        
        LineNumberReader localLineNumberReader = new LineNumberReader(localInputStreamReader);
        for (;;)
        {
          String str3 = localLineNumberReader.readLine();
          if (str3 == null) {
            break;
          }
          if (str3.startsWith("Full thread dump")) {
            this.threadDumps += 1;
          }
        }
        localInputStreamReader.close();
        localInputStreamReader = new InputStreamReader(new FileInputStream(str2), "CP1252");
        
        localLineNumberReader = new LineNumberReader(localInputStreamReader);
        processList(readStackTrace(localLineNumberReader));
        localInputStreamReader.close();
      }
      System.out.println(getTopTraces(3));
    }
    catch (IOException localIOException)
    {
      throw new RuntimeException(localIOException);
    }
  }
  
  private static List<Object[]> getRunnableStackTraces()
  {
    ArrayList localArrayList = new ArrayList();
    Map localMap = Thread.getAllStackTraces();
    for (Map.Entry localEntry : localMap.entrySet())
    {
      Thread localThread = (Thread)localEntry.getKey();
      if (localThread.getState() == Thread.State.RUNNABLE)
      {
        StackTraceElement[] arrayOfStackTraceElement = (StackTraceElement[])localEntry.getValue();
        if ((arrayOfStackTraceElement != null) && (arrayOfStackTraceElement.length != 0)) {
          localArrayList.add(arrayOfStackTraceElement);
        }
      }
    }
    return localArrayList;
  }
  
  private static List<Object[]> readRunnableStackTraces(int paramInt)
  {
    try
    {
      String str = exec(new String[] { "jstack", "" + paramInt });
      LineNumberReader localLineNumberReader = new LineNumberReader(new StringReader(str));
      
      return readStackTrace(localLineNumberReader);
    }
    catch (IOException localIOException)
    {
      throw new RuntimeException(localIOException);
    }
  }
  
  private static List<Object[]> readStackTrace(LineNumberReader paramLineNumberReader)
    throws IOException
  {
    ArrayList localArrayList1 = new ArrayList();
    for (;;)
    {
      String str = paramLineNumberReader.readLine();
      if (str == null) {
        break;
      }
      if (str.startsWith("\""))
      {
        str = paramLineNumberReader.readLine();
        if (str == null) {
          break;
        }
        str = str.trim();
        if (str.startsWith("java.lang.Thread.State: RUNNABLE"))
        {
          ArrayList localArrayList2 = new ArrayList();
          for (;;)
          {
            str = paramLineNumberReader.readLine();
            if (str == null) {
              break;
            }
            str = str.trim();
            if (!str.startsWith("- "))
            {
              if (!str.startsWith("at ")) {
                break;
              }
              str = str.substring(3).trim();
              localArrayList2.add(str);
            }
          }
          if (localArrayList2.size() > 0)
          {
            String[] arrayOfString = (String[])localArrayList2.toArray(new String[localArrayList2.size()]);
            localArrayList1.add(arrayOfString);
          }
        }
      }
    }
    return localArrayList1;
  }
  
  private static String exec(String... paramVarArgs)
  {
    ByteArrayOutputStream localByteArrayOutputStream1 = new ByteArrayOutputStream();
    ByteArrayOutputStream localByteArrayOutputStream2 = new ByteArrayOutputStream();
    try
    {
      Process localProcess = Runtime.getRuntime().exec(paramVarArgs);
      copyInThread(localProcess.getInputStream(), localByteArrayOutputStream2);
      copyInThread(localProcess.getErrorStream(), localByteArrayOutputStream1);
      localProcess.waitFor();
      String str1 = new String(localByteArrayOutputStream1.toByteArray(), "UTF-8");
      if (str1.length() > 0) {
        throw new RuntimeException(str1);
      }
      return new String(localByteArrayOutputStream2.toByteArray(), "UTF-8");
    }
    catch (Exception localException)
    {
      throw new RuntimeException(localException);
    }
  }
  
  private static void copyInThread(final InputStream paramInputStream, final OutputStream paramOutputStream)
  {
    new Thread("Profiler stream copy")
    {
      public void run()
      {
        byte[] arrayOfByte = new byte['က'];
        try
        {
          for (;;)
          {
            int i = paramInputStream.read(arrayOfByte, 0, arrayOfByte.length);
            if (i < 0) {
              break;
            }
            paramOutputStream.write(arrayOfByte, 0, i);
          }
        }
        catch (Exception localException)
        {
          throw new RuntimeException(localException);
        }
      }
    }.run();
  }
  
  public Profiler startCollecting()
  {
    this.thread = new Thread(this, "Profiler");
    this.thread.setDaemon(true);
    this.thread.start();
    return this;
  }
  
  public Profiler stopCollecting()
  {
    this.stop = true;
    if (this.thread != null)
    {
      try
      {
        this.thread.join();
      }
      catch (InterruptedException localInterruptedException) {}
      this.thread = null;
    }
    return this;
  }
  
  public void run()
  {
    this.start = System.currentTimeMillis();
    for (;;)
    {
      if (!this.stop) {
        try
        {
          tick();
        }
        catch (Throwable localThrowable) {}
      }
    }
    this.time = (System.currentTimeMillis() - this.start);
  }
  
  private void tick()
  {
    if (this.interval > 0)
    {
      if (this.paused) {
        return;
      }
      try
      {
        Thread.sleep(this.interval);
      }
      catch (Exception localException) {}
    }
    List localList;
    if (this.pid != 0) {
      localList = readRunnableStackTraces(this.pid);
    } else {
      localList = getRunnableStackTraces();
    }
    this.threadDumps += 1;
    processList(localList);
  }
  
  private void processList(List<Object[]> paramList)
  {
    for (Object[] arrayOfObject : paramList) {
      if (!startsWithAny(arrayOfObject[0].toString(), this.ignoreThreads))
      {
        StringBuilder localStringBuilder = new StringBuilder();
        
        Object localObject = null;
        int i = 0;
        int j = 0;
        for (int k = 0; (k < arrayOfObject.length) && (j < this.depth); k++)
        {
          String str1 = arrayOfObject[k].toString();
          if ((!str1.equals(localObject)) && (!startsWithAny(str1, this.ignoreLines)))
          {
            localObject = str1;
            localStringBuilder.append("at ").append(str1).append(LINE_SEPARATOR);
            if ((i == 0) && (!startsWithAny(str1, this.ignorePackages)))
            {
              i = 1;
              for (int m = 0; m < str1.length(); m++)
              {
                char c = str1.charAt(m);
                if ((c == '(') || (Character.isUpperCase(c))) {
                  break;
                }
              }
              if ((m > 0) && (str1.charAt(m - 1) == '.')) {
                m--;
              }
              if (this.sumClasses)
              {
                int n = str1.indexOf('.', m + 1);
                m = n >= 0 ? n : m;
              }
              String str2 = str1.substring(0, m);
              increment(this.summary, str2, 0);
            }
            j++;
          }
        }
        if (localStringBuilder.length() > 0)
        {
          this.minCount = increment(this.counts, localStringBuilder.toString().trim(), this.minCount);
          this.total += 1;
        }
      }
    }
  }
  
  private static boolean startsWithAny(String paramString, String[] paramArrayOfString)
  {
    for (String str : paramArrayOfString) {
      if ((str.length() > 0) && (paramString.startsWith(str))) {
        return true;
      }
    }
    return false;
  }
  
  private static int increment(HashMap<String, Integer> paramHashMap, String paramString, int paramInt)
  {
    Integer localInteger = (Integer)paramHashMap.get(paramString);
    if (localInteger == null) {
      paramHashMap.put(paramString, Integer.valueOf(1));
    } else {
      paramHashMap.put(paramString, Integer.valueOf(localInteger.intValue() + 1));
    }
    while (paramHashMap.size() > 1000)
    {
      Iterator localIterator = paramHashMap.entrySet().iterator();
      while (localIterator.hasNext())
      {
        Map.Entry localEntry = (Map.Entry)localIterator.next();
        if (((Integer)localEntry.getValue()).intValue() <= paramInt) {
          localIterator.remove();
        }
      }
      if (paramHashMap.size() > 1000) {
        paramInt++;
      }
    }
    return paramInt;
  }
  
  public String getTop(int paramInt)
  {
    stopCollecting();
    return getTopTraces(paramInt);
  }
  
  private String getTopTraces(int paramInt)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append("Profiler: top ").append(paramInt).append(" stack trace(s) of ");
    if (this.time > 0L) {
      localStringBuilder.append(" of ").append(this.time).append(" ms");
    }
    if (this.threadDumps > 0) {
      localStringBuilder.append(" of ").append(this.threadDumps).append(" thread dumps");
    }
    localStringBuilder.append(":").append(LINE_SEPARATOR);
    if (this.counts.size() == 0) {
      localStringBuilder.append("(none)").append(LINE_SEPARATOR);
    }
    HashMap localHashMap = new HashMap(this.counts);
    appendTop(localStringBuilder, localHashMap, paramInt, this.total, false);
    localStringBuilder.append("summary:").append(LINE_SEPARATOR);
    localHashMap = new HashMap(this.summary);
    appendTop(localStringBuilder, localHashMap, paramInt, this.total, true);
    localStringBuilder.append('.');
    return localStringBuilder.toString();
  }
  
  private static void appendTop(StringBuilder paramStringBuilder, HashMap<String, Integer> paramHashMap, int paramInt1, int paramInt2, boolean paramBoolean)
  {
    int i = 0;int j = 0;
    for (;;)
    {
      int k = 0;
      Object localObject = null;
      for (Map.Entry localEntry : paramHashMap.entrySet()) {
        if (((Integer)localEntry.getValue()).intValue() > k)
        {
          localObject = localEntry;
          k = ((Integer)localEntry.getValue()).intValue();
        }
      }
      if (localObject == null) {
        break;
      }
      paramHashMap.remove(((Map.Entry)localObject).getKey());
      i++;
      if (i >= paramInt1)
      {
        if (((Integer)((Map.Entry)localObject).getValue()).intValue() < j) {
          break;
        }
        j = ((Integer)((Map.Entry)localObject).getValue()).intValue();
      }
      int m = ((Integer)((Map.Entry)localObject).getValue()).intValue();
      int n = 100 * m / Math.max(paramInt2, 1);
      if (paramBoolean)
      {
        if (n > 1) {
          paramStringBuilder.append(n).append("%: ").append((String)((Map.Entry)localObject).getKey()).append(LINE_SEPARATOR);
        }
      }
      else {
        paramStringBuilder.append(m).append('/').append(paramInt2).append(" (").append(n).append("%):").append(LINE_SEPARATOR).append((String)((Map.Entry)localObject).getKey()).append(LINE_SEPARATOR);
      }
    }
  }
}
