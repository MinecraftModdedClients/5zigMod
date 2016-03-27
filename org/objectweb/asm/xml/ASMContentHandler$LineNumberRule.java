package org.objectweb.asm.xml;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.xml.sax.Attributes;

final class ASMContentHandler$LineNumberRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$LineNumberRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void begin(String paramString, Attributes paramAttributes)
  {
    int i = Integer.parseInt(paramAttributes.getValue("line"));
    Label localLabel = getLabel(paramAttributes.getValue("start"));
    getCodeVisitor().visitLineNumber(i, localLabel);
  }
}
