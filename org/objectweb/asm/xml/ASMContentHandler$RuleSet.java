package org.objectweb.asm.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

final class ASMContentHandler$RuleSet
{
  private final HashMap rules = new HashMap();
  private final ArrayList lpatterns = new ArrayList();
  private final ArrayList rpatterns = new ArrayList();
  
  public void add(String paramString, Object paramObject)
  {
    String str = paramString;
    if (paramString.startsWith("*/"))
    {
      str = paramString.substring(1);
      this.lpatterns.add(str);
    }
    else if (paramString.endsWith("/*"))
    {
      str = paramString.substring(0, paramString.length() - 1);
      this.rpatterns.add(str);
    }
    this.rules.put(str, paramObject);
  }
  
  public Object match(String paramString)
  {
    if (this.rules.containsKey(paramString)) {
      return this.rules.get(paramString);
    }
    int i = paramString.lastIndexOf('/');
    Iterator localIterator = this.lpatterns.iterator();
    String str;
    while (localIterator.hasNext())
    {
      str = (String)localIterator.next();
      if (paramString.substring(i).endsWith(str)) {
        return this.rules.get(str);
      }
    }
    localIterator = this.rpatterns.iterator();
    while (localIterator.hasNext())
    {
      str = (String)localIterator.next();
      if (paramString.startsWith(str)) {
        return this.rules.get(str);
      }
    }
    return null;
  }
}
