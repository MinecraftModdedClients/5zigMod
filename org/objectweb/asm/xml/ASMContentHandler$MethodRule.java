package org.objectweb.asm.xml;

import java.util.ArrayList;
import java.util.HashMap;
import org.objectweb.asm.MethodVisitor;
import org.xml.sax.Attributes;

final class ASMContentHandler$MethodRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$MethodRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void begin(String paramString, Attributes paramAttributes)
  {
    this.this$0.labels = new HashMap();
    HashMap localHashMap = new HashMap();
    localHashMap.put("access", paramAttributes.getValue("access"));
    localHashMap.put("name", paramAttributes.getValue("name"));
    localHashMap.put("desc", paramAttributes.getValue("desc"));
    localHashMap.put("signature", paramAttributes.getValue("signature"));
    localHashMap.put("exceptions", new ArrayList());
    this.this$0.push(localHashMap);
  }
  
  public final void end(String paramString)
  {
    ((MethodVisitor)this.this$0.pop()).visitEnd();
    this.this$0.labels = null;
  }
}
