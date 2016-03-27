package org.objectweb.asm.xml;

import org.objectweb.asm.AnnotationVisitor;
import org.xml.sax.Attributes;

final class ASMContentHandler$AnnotationValueAnnotationRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$AnnotationValueAnnotationRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public void begin(String paramString, Attributes paramAttributes)
  {
    AnnotationVisitor localAnnotationVisitor = (AnnotationVisitor)this.this$0.peek();
    this.this$0.push(localAnnotationVisitor == null ? null : localAnnotationVisitor.visitAnnotation(paramAttributes.getValue("name"), paramAttributes.getValue("desc")));
  }
  
  public void end(String paramString)
  {
    AnnotationVisitor localAnnotationVisitor = (AnnotationVisitor)this.this$0.pop();
    if (localAnnotationVisitor != null) {
      localAnnotationVisitor.visitEnd();
    }
  }
}
