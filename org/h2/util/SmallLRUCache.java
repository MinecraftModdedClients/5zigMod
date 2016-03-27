package org.h2.util;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class SmallLRUCache<K, V>
  extends LinkedHashMap<K, V>
{
  private static final long serialVersionUID = 1L;
  private int size;
  
  private SmallLRUCache(int paramInt)
  {
    super(paramInt, 0.75F, true);
    this.size = paramInt;
  }
  
  public static <K, V> SmallLRUCache<K, V> newInstance(int paramInt)
  {
    return new SmallLRUCache(paramInt);
  }
  
  public void setMaxSize(int paramInt)
  {
    this.size = paramInt;
  }
  
  protected boolean removeEldestEntry(Map.Entry<K, V> paramEntry)
  {
    return size() > this.size;
  }
}
