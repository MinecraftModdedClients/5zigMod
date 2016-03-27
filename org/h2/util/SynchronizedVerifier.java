package org.h2.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SynchronizedVerifier
{
  private static volatile boolean enabled;
  private static final Map<Class<?>, AtomicBoolean> DETECT = Collections.synchronizedMap(new HashMap());
  private static final Map<Object, Object> CURRENT = Collections.synchronizedMap(new IdentityHashMap());
  
  public static void setDetect(Class<?> paramClass, boolean paramBoolean)
  {
    if (paramBoolean)
    {
      DETECT.put(paramClass, new AtomicBoolean());
    }
    else
    {
      AtomicBoolean localAtomicBoolean = (AtomicBoolean)DETECT.remove(paramClass);
      if (localAtomicBoolean == null) {
        throw new AssertionError("Detection was not enabled");
      }
      if (!localAtomicBoolean.get()) {
        throw new AssertionError("No object of this class was tested");
      }
    }
    enabled = DETECT.size() > 0;
  }
  
  public static void check(Object paramObject)
  {
    if (enabled) {
      detectConcurrentAccess(paramObject);
    }
  }
  
  private static void detectConcurrentAccess(Object paramObject)
  {
    AtomicBoolean localAtomicBoolean = (AtomicBoolean)DETECT.get(paramObject.getClass());
    if (localAtomicBoolean != null)
    {
      localAtomicBoolean.set(true);
      if (CURRENT.remove(paramObject) != null) {
        throw new AssertionError("Concurrent access");
      }
      CURRENT.put(paramObject, paramObject);
      try
      {
        Thread.sleep(1L);
      }
      catch (InterruptedException localInterruptedException) {}
      Object localObject = CURRENT.remove(paramObject);
      if (localObject == null) {
        throw new AssertionError("Concurrent access");
      }
    }
  }
}
