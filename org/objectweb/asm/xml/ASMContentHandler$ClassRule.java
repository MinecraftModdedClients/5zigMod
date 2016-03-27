package org.objectweb.asm.xml;

import java.util.ArrayList;
import java.util.HashMap;
import org.xml.sax.Attributes;

final class ASMContentHandler$ClassRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$ClassRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void begin(String paramString, Attributes paramAttributes)
  {
    int i = Integer.parseInt(paramAttributes.getValue("major"));
    int j = Integer.parseInt(paramAttributes.getValue("minor"));
    HashMap localHashMap = new HashMap();
    localHashMap.put("version", new Integer(j << 16 | i));
    localHashMap.put("access", paramAttributes.getValue("access"));
    localHashMap.put("name", paramAttributes.getValue("name"));
    localHashMap.put("parent", paramAttributes.getValue("parent"));
    localHashMap.put("source", paramAttributes.getValue("source"));
    localHashMap.put("signature", paramAttributes.getValue("signature"));
    localHashMap.put("interfaces", new ArrayList());
    this.this$0.push(localHashMap);
  }
}
