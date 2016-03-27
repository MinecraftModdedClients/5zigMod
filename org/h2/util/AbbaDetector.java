package org.h2.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class AbbaDetector
{
  private static final boolean TRACE = false;
  private static final ThreadLocal<Deque<Object>> STACK = new ThreadLocal()
  {
    protected Deque<Object> initialValue()
    {
      return new ArrayDeque();
    }
  };
  private static final Map<Object, Map<Object, Exception>> LOCK_ORDERING = new WeakHashMap();
  private static final Set<String> KNOWN_DEADLOCKS = new HashSet();
  
  public static Object begin(Object paramObject)
  {
    if (paramObject == null) {
      paramObject = new SecurityManager().
      {
        Class<?> clazz = getClassContext()[2];
      };
    }
    Deque localDeque = (Deque)STACK.get();
    if (!localDeque.isEmpty())
    {
      if (localDeque.contains(paramObject)) {
        return paramObject;
      }
      while (!localDeque.isEmpty())
      {
        Object localObject = localDeque.peek();
        if (Thread.holdsLock(localObject)) {
          break;
        }
        localDeque.pop();
      }
    }
    if (localDeque.size() > 0) {
      markHigher(paramObject, localDeque);
    }
    localDeque.push(paramObject);
    return paramObject;
  }
  
  private static Object getTest(Object paramObject)
  {
    return paramObject;
  }
  
  private static String getObjectName(Object paramObject)
  {
    return paramObject.getClass().getSimpleName() + "@" + System.identityHashCode(paramObject);
  }
  
  private static synchronized void markHigher(Object paramObject, Deque<Object> paramDeque)
  {
    Object localObject1 = getTest(paramObject);
    Object localObject2 = (Map)LOCK_ORDERING.get(localObject1);
    if (localObject2 == null)
    {
      localObject2 = new WeakHashMap();
      LOCK_ORDERING.put(localObject1, localObject2);
    }
    Exception localException1 = null;
    for (Object localObject3 : paramDeque)
    {
      Object localObject4 = getTest(localObject3);
      if (localObject4 != localObject1)
      {
        Map localMap = (Map)LOCK_ORDERING.get(localObject4);
        if (localMap != null)
        {
          Exception localException2 = (Exception)localMap.get(localObject1);
          if (localException2 != null)
          {
            String str1 = localObject1.getClass() + " " + localObject4.getClass();
            if (!KNOWN_DEADLOCKS.contains(str1))
            {
              String str2 = getObjectName(localObject1) + " synchronized after \n " + getObjectName(localObject4) + ", but in the past before";
              
              RuntimeException localRuntimeException = new RuntimeException(str2);
              localRuntimeException.initCause(localException2);
              localRuntimeException.printStackTrace(System.out);
              
              KNOWN_DEADLOCKS.add(str1);
            }
          }
        }
        if (!((Map)localObject2).containsKey(localObject4))
        {
          if (localException1 == null) {
            localException1 = new Exception("Before");
          }
          ((Map)localObject2).put(localObject4, localException1);
        }
      }
    }
  }
}
