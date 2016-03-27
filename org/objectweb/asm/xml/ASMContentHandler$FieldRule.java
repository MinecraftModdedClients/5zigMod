package org.objectweb.asm.xml;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

final class ASMContentHandler$FieldRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$FieldRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void begin(String paramString, Attributes paramAttributes)
    throws SAXException
  {
    int i = getAccess(paramAttributes.getValue("access"));
    String str1 = paramAttributes.getValue("name");
    String str2 = paramAttributes.getValue("signature");
    String str3 = paramAttributes.getValue("desc");
    Object localObject = getValue(str3, paramAttributes.getValue("value"));
    this.this$0.push(this.this$0.cv.visitField(i, str1, str3, str2, localObject));
  }
  
  public void end(String paramString)
  {
    ((FieldVisitor)this.this$0.pop()).visitEnd();
  }
}
