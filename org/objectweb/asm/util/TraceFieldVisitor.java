package org.objectweb.asm.util;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;

public final class TraceFieldVisitor
  extends FieldVisitor
{
  public final Printer p;
  
  public TraceFieldVisitor(Printer paramPrinter)
  {
    this(null, paramPrinter);
  }
  
  public TraceFieldVisitor(FieldVisitor paramFieldVisitor, Printer paramPrinter)
  {
    super(327680, paramFieldVisitor);
    this.p = paramPrinter;
  }
  
  public AnnotationVisitor visitAnnotation(String paramString, boolean paramBoolean)
  {
    Printer localPrinter = this.p.visitFieldAnnotation(paramString, paramBoolean);
    AnnotationVisitor localAnnotationVisitor = this.fv == null ? null : this.fv.visitAnnotation(paramString, paramBoolean);
    return new TraceAnnotationVisitor(localAnnotationVisitor, localPrinter);
  }
  
  public AnnotationVisitor visitTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    Printer localPrinter = this.p.visitFieldTypeAnnotation(paramInt, paramTypePath, paramString, paramBoolean);
    AnnotationVisitor localAnnotationVisitor = this.fv == null ? null : this.fv.visitTypeAnnotation(paramInt, paramTypePath, paramString, paramBoolean);
    return new TraceAnnotationVisitor(localAnnotationVisitor, localPrinter);
  }
  
  public void visitAttribute(Attribute paramAttribute)
  {
    this.p.visitFieldAttribute(paramAttribute);
    super.visitAttribute(paramAttribute);
  }
  
  public void visitEnd()
  {
    this.p.visitFieldEnd();
    super.visitEnd();
  }
}
