package org.h2.util;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class AbbaLockingDetector
  implements Runnable
{
  private int tickIntervalMs = 2;
  private volatile boolean stop;
  private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  private Thread thread;
  private final Map<String, Map<String, String>> lockOrdering = new WeakHashMap();
  private final Set<String> knownDeadlocks = new HashSet();
  
  public AbbaLockingDetector startCollecting()
  {
    this.thread = new Thread(this, "AbbaLockingDetector");
    this.thread.setDaemon(true);
    this.thread.start();
    return this;
  }
  
  public synchronized void reset()
  {
    this.lockOrdering.clear();
    this.knownDeadlocks.clear();
  }
  
  public AbbaLockingDetector stopCollecting()
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
  }
  
  private void tick()
  {
    if (this.tickIntervalMs > 0) {
      try
      {
        Thread.sleep(this.tickIntervalMs);
      }
      catch (InterruptedException localInterruptedException) {}
    }
    ThreadInfo[] arrayOfThreadInfo = this.threadMXBean.dumpAllThreads(true, false);
    
    processThreadList(arrayOfThreadInfo);
  }
  
  private void processThreadList(ThreadInfo[] paramArrayOfThreadInfo)
  {
    ArrayList localArrayList = new ArrayList();
    for (ThreadInfo localThreadInfo : paramArrayOfThreadInfo)
    {
      localArrayList.clear();
      generateOrdering(localArrayList, localThreadInfo);
      if (localArrayList.size() > 1) {
        markHigher(localArrayList, localThreadInfo);
      }
    }
  }
  
  private static void generateOrdering(List<String> paramList, ThreadInfo paramThreadInfo)
  {
    MonitorInfo[] arrayOfMonitorInfo1 = paramThreadInfo.getLockedMonitors();
    Arrays.sort(arrayOfMonitorInfo1, new Comparator()
    {
      public int compare(MonitorInfo paramAnonymousMonitorInfo1, MonitorInfo paramAnonymousMonitorInfo2)
      {
        return paramAnonymousMonitorInfo2.getLockedStackDepth() - paramAnonymousMonitorInfo1.getLockedStackDepth();
      }
    });
    for (MonitorInfo localMonitorInfo : arrayOfMonitorInfo1)
    {
      String str = getObjectName(localMonitorInfo);
      if (!str.equals("sun.misc.Launcher$AppClassLoader")) {
        if (!paramList.contains(str)) {
          paramList.add(str);
        }
      }
    }
  }
  
  private synchronized void markHigher(List<String> paramList, ThreadInfo paramThreadInfo)
  {
    String str1 = (String)paramList.get(paramList.size() - 1);
    Object localObject = (Map)this.lockOrdering.get(str1);
    if (localObject == null)
    {
      localObject = new WeakHashMap();
      this.lockOrdering.put(str1, localObject);
    }
    String str2 = null;
    for (int i = 0; i < paramList.size() - 1; i++)
    {
      String str3 = (String)paramList.get(i);
      Map localMap = (Map)this.lockOrdering.get(str3);
      int j = 0;
      if (localMap != null)
      {
        String str4 = (String)localMap.get(str1);
        if (str4 != null)
        {
          j = 1;
          String str5 = str1 + " " + str3;
          if (!this.knownDeadlocks.contains(str5))
          {
            System.out.println(str1 + " synchronized after \n " + str3 + ", but in the past before\n" + "AFTER\n" + getStackTraceForThread(paramThreadInfo) + "BEFORE\n" + str4);
            
            this.knownDeadlocks.add(str5);
          }
        }
      }
      if ((j == 0) && (!((Map)localObject).containsKey(str3)))
      {
        if (str2 == null) {
          str2 = getStackTraceForThread(paramThreadInfo);
        }
        ((Map)localObject).put(str3, str2);
      }
    }
  }
  
  private static String getStackTraceForThread(ThreadInfo paramThreadInfo)
  {
    StringBuilder localStringBuilder = new StringBuilder("\"" + paramThreadInfo.getThreadName() + "\"" + " Id=" + paramThreadInfo.getThreadId() + " " + paramThreadInfo.getThreadState());
    if (paramThreadInfo.getLockName() != null) {
      localStringBuilder.append(" on " + paramThreadInfo.getLockName());
    }
    if (paramThreadInfo.getLockOwnerName() != null) {
      localStringBuilder.append(" owned by \"" + paramThreadInfo.getLockOwnerName() + "\" Id=" + paramThreadInfo.getLockOwnerId());
    }
    if (paramThreadInfo.isSuspended()) {
      localStringBuilder.append(" (suspended)");
    }
    if (paramThreadInfo.isInNative()) {
      localStringBuilder.append(" (in native)");
    }
    localStringBuilder.append('\n');
    StackTraceElement[] arrayOfStackTraceElement = paramThreadInfo.getStackTrace();
    MonitorInfo[] arrayOfMonitorInfo1 = paramThreadInfo.getLockedMonitors();
    int i = 0;
    for (int j = 0; j < arrayOfStackTraceElement.length; j++)
    {
      StackTraceElement localStackTraceElement = arrayOfStackTraceElement[j];
      if (i != 0) {
        dumpStackTraceElement(paramThreadInfo, localStringBuilder, j, localStackTraceElement);
      }
      for (MonitorInfo localMonitorInfo : arrayOfMonitorInfo1) {
        if (localMonitorInfo.getLockedStackDepth() == j)
        {
          if (i == 0)
          {
            dumpStackTraceElement(paramThreadInfo, localStringBuilder, j, localStackTraceElement);
            i = 1;
          }
          localStringBuilder.append("\t-  locked " + localMonitorInfo);
          localStringBuilder.append('\n');
        }
      }
    }
    return localStringBuilder.toString();
  }
  
  private static void dumpStackTraceElement(ThreadInfo paramThreadInfo, StringBuilder paramStringBuilder, int paramInt, StackTraceElement paramStackTraceElement)
  {
    paramStringBuilder.append('\t').append("at ").append(paramStackTraceElement.toString());
    paramStringBuilder.append('\n');
    if ((paramInt == 0) && (paramThreadInfo.getLockInfo() != null))
    {
      Thread.State localState = paramThreadInfo.getThreadState();
      switch (localState)
      {
      case BLOCKED: 
        paramStringBuilder.append("\t-  blocked on " + paramThreadInfo.getLockInfo());
        paramStringBuilder.append('\n');
        break;
      case WAITING: 
        paramStringBuilder.append("\t-  waiting on " + paramThreadInfo.getLockInfo());
        paramStringBuilder.append('\n');
        break;
      case TIMED_WAITING: 
        paramStringBuilder.append("\t-  waiting on " + paramThreadInfo.getLockInfo());
        paramStringBuilder.append('\n');
        break;
      }
    }
  }
  
  private static String getObjectName(MonitorInfo paramMonitorInfo)
  {
    return paramMonitorInfo.getClassName() + "@" + Integer.toHexString(paramMonitorInfo.getIdentityHashCode());
  }
}
