package org.h2.value;

import java.util.HashMap;
import org.h2.util.StringUtils;

public class CaseInsensitiveMap<V>
  extends HashMap<String, V>
{
  private static final long serialVersionUID = 1L;
  
  public V get(Object paramObject)
  {
    return (V)super.get(toUpper(paramObject));
  }
  
  public V put(String paramString, V paramV)
  {
    return (V)super.put(toUpper(paramString), paramV);
  }
  
  public boolean containsKey(Object paramObject)
  {
    return super.containsKey(toUpper(paramObject));
  }
  
  public V remove(Object paramObject)
  {
    return (V)super.remove(toUpper(paramObject));
  }
  
  private static String toUpper(Object paramObject)
  {
    return paramObject == null ? null : StringUtils.toUpperEnglish(paramObject.toString());
  }
}
