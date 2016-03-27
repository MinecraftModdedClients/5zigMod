package org.objectweb.asm.xml;

import org.objectweb.asm.ClassVisitor;
import org.xml.sax.Attributes;

final class ASMContentHandler$OuterClassRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$OuterClassRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void begin(String paramString, Attributes paramAttributes)
  {
    String str1 = paramAttributes.getValue("owner");
    String str2 = paramAttributes.getValue("name");
    String str3 = paramAttributes.getValue("desc");
    this.this$0.cv.visitOuterClass(str1, str2, str3);
  }
}
