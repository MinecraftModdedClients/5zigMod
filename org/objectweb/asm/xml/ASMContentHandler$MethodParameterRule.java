package org.objectweb.asm.xml;

import org.objectweb.asm.MethodVisitor;
import org.xml.sax.Attributes;

final class ASMContentHandler$MethodParameterRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$MethodParameterRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public void begin(String paramString, Attributes paramAttributes)
  {
    String str = paramAttributes.getValue("name");
    int i = getAccess(paramAttributes.getValue("access"));
    getCodeVisitor().visitParameter(str, i);
  }
}
