package org.objectweb.asm.xml;

import org.objectweb.asm.ClassVisitor;
import org.xml.sax.Attributes;

final class ASMContentHandler$InnerClassRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$InnerClassRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void begin(String paramString, Attributes paramAttributes)
  {
    int i = getAccess(paramAttributes.getValue("access"));
    String str1 = paramAttributes.getValue("name");
    String str2 = paramAttributes.getValue("outerName");
    String str3 = paramAttributes.getValue("innerName");
    this.this$0.cv.visitInnerClass(str1, str2, str3, i);
  }
}
