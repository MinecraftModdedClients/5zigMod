package org.objectweb.asm.commons;

import java.util.Collections;
import java.util.Map;

public class SimpleRemapper
  extends Remapper
{
  private final Map mapping;
  
  public SimpleRemapper(Map paramMap)
  {
    this.mapping = paramMap;
  }
  
  public SimpleRemapper(String paramString1, String paramString2)
  {
    this.mapping = Collections.singletonMap(paramString1, paramString2);
  }
  
  public String mapMethodName(String paramString1, String paramString2, String paramString3)
  {
    String str = map(paramString1 + '.' + paramString2 + paramString3);
    return str == null ? paramString2 : str;
  }
  
  public String mapFieldName(String paramString1, String paramString2, String paramString3)
  {
    String str = map(paramString1 + '.' + paramString2);
    return str == null ? paramString2 : str;
  }
  
  public String map(String paramString)
  {
    return (String)this.mapping.get(paramString);
  }
}
