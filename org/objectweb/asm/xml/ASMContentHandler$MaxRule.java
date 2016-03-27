package org.objectweb.asm.xml;

import org.objectweb.asm.MethodVisitor;
import org.xml.sax.Attributes;

final class ASMContentHandler$MaxRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$MaxRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void begin(String paramString, Attributes paramAttributes)
  {
    int i = Integer.parseInt(paramAttributes.getValue("maxStack"));
    int j = Integer.parseInt(paramAttributes.getValue("maxLocals"));
    getCodeVisitor().visitMaxs(i, j);
  }
}
