package org.h2.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SoftHashMap<K, V>
  extends AbstractMap<K, V>
{
  private final Map<K, SoftValue<V>> map;
  private final ReferenceQueue<V> queue = new ReferenceQueue();
  
  public SoftHashMap()
  {
    this.map = New.hashMap();
  }
  
  private void processQueue()
  {
    for (;;)
    {
      Reference localReference = this.queue.poll();
      if (localReference == null) {
        return;
      }
      SoftValue localSoftValue = (SoftValue)localReference;
      Object localObject = localSoftValue.key;
      this.map.remove(localObject);
    }
  }
  
  public V get(Object paramObject)
  {
    processQueue();
    SoftReference localSoftReference = (SoftReference)this.map.get(paramObject);
    if (localSoftReference == null) {
      return null;
    }
    return (V)localSoftReference.get();
  }
  
  public V put(K paramK, V paramV)
  {
    processQueue();
    SoftValue localSoftValue = (SoftValue)this.map.put(paramK, new SoftValue(paramV, this.queue, paramK));
    return localSoftValue == null ? null : localSoftValue.get();
  }
  
  public V remove(Object paramObject)
  {
    processQueue();
    SoftReference localSoftReference = (SoftReference)this.map.remove(paramObject);
    return localSoftReference == null ? null : localSoftReference.get();
  }
  
  public void clear()
  {
    processQueue();
    this.map.clear();
  }
  
  public Set<Map.Entry<K, V>> entrySet()
  {
    throw new UnsupportedOperationException();
  }
  
  private static class SoftValue<T>
    extends SoftReference<T>
  {
    final Object key;
    
    public SoftValue(T paramT, ReferenceQueue<T> paramReferenceQueue, Object paramObject)
    {
      super(paramReferenceQueue);
      this.key = paramObject;
    }
  }
}
