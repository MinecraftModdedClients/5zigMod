package org.h2.util;

import java.io.Closeable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CloseWatcher
  extends PhantomReference<Object>
{
  private static ReferenceQueue<Object> queue = new ReferenceQueue();
  private static Set<CloseWatcher> refs = createSet();
  private String openStackTrace;
  private Closeable closeable;
  
  public CloseWatcher(Object paramObject, ReferenceQueue<Object> paramReferenceQueue, Closeable paramCloseable)
  {
    super(paramObject, paramReferenceQueue);
    this.closeable = paramCloseable;
  }
  
  private static Set<CloseWatcher> createSet()
  {
    return Collections.synchronizedSet(new HashSet());
  }
  
  public static CloseWatcher pollUnclosed()
  {
    ReferenceQueue localReferenceQueue = queue;
    if (localReferenceQueue == null) {
      return null;
    }
    for (;;)
    {
      CloseWatcher localCloseWatcher = (CloseWatcher)localReferenceQueue.poll();
      if (localCloseWatcher == null) {
        return null;
      }
      if (refs != null) {
        refs.remove(localCloseWatcher);
      }
      if (localCloseWatcher.closeable != null) {
        return localCloseWatcher;
      }
    }
  }
  
  public static CloseWatcher register(Object paramObject, Closeable paramCloseable, boolean paramBoolean)
  {
    ReferenceQueue localReferenceQueue = queue;
    if (localReferenceQueue == null)
    {
      localReferenceQueue = new ReferenceQueue();
      queue = localReferenceQueue;
    }
    CloseWatcher localCloseWatcher = new CloseWatcher(paramObject, localReferenceQueue, paramCloseable);
    if (paramBoolean)
    {
      Exception localException = new Exception("Open Stack Trace");
      StringWriter localStringWriter = new StringWriter();
      localException.printStackTrace(new PrintWriter(localStringWriter));
      localCloseWatcher.openStackTrace = localStringWriter.toString();
    }
    if (refs == null) {
      refs = createSet();
    }
    refs.add(localCloseWatcher);
    return localCloseWatcher;
  }
  
  public static void unregister(CloseWatcher paramCloseWatcher)
  {
    paramCloseWatcher.closeable = null;
    refs.remove(paramCloseWatcher);
  }
  
  public String getOpenStackTrace()
  {
    return this.openStackTrace;
  }
  
  public Closeable getCloseable()
  {
    return this.closeable;
  }
}
