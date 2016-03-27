package eu.the5zig.util;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

public class LinkedProperties
  extends Properties
{
  private static final long serialVersionUID = 1L;
  private Map<Object, Object> linkMap = new LinkedHashMap();
  
  public synchronized Object put(Object key, Object value)
  {
    return this.linkMap.put(key, value);
  }
  
  public synchronized boolean contains(Object value)
  {
    return this.linkMap.containsValue(value);
  }
  
  public boolean containsValue(Object value)
  {
    return this.linkMap.containsValue(value);
  }
  
  public synchronized Enumeration<Object> elements()
  {
    throw new UnsupportedOperationException("Enumerations are so old-school, don't use them, use keySet() or entrySet() instead");
  }
  
  public Set<Map.Entry<Object, Object>> entrySet()
  {
    return this.linkMap.entrySet();
  }
  
  public synchronized void clear()
  {
    this.linkMap.clear();
  }
  
  public synchronized boolean containsKey(Object key)
  {
    return this.linkMap.containsKey(key);
  }
}
