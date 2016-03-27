package org.h2.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class New
{
  public static <T> ArrayList<T> arrayList()
  {
    return new ArrayList(4);
  }
  
  public static <K, V> HashMap<K, V> hashMap()
  {
    return new HashMap();
  }
  
  public static <K, V> HashMap<K, V> hashMap(int paramInt)
  {
    return new HashMap(paramInt);
  }
  
  public static <T> HashSet<T> hashSet()
  {
    return new HashSet();
  }
  
  public static <T> ArrayList<T> arrayList(Collection<T> paramCollection)
  {
    return new ArrayList(paramCollection);
  }
  
  public static <T> ArrayList<T> arrayList(int paramInt)
  {
    return new ArrayList(paramInt);
  }
}
