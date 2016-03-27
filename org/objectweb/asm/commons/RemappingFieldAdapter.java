package org.objectweb.asm.commons;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;

public class RemappingFieldAdapter
  extends FieldVisitor
{
  private final Remapper remapper;
  
  public RemappingFieldAdapter(FieldVisitor paramFieldVisitor, Remapper paramRemapper)
  {
    this(327680, paramFieldVisitor, paramRemapper);
  }
  
  protected RemappingFieldAdapter(int paramInt, FieldVisitor paramFieldVisitor, Remapper paramRemapper)
  {
    super(paramInt, paramFieldVisitor);
    this.remapper = paramRemapper;
  }
  
  public AnnotationVisitor visitAnnotation(String paramString, boolean paramBoolean)
  {
    AnnotationVisitor localAnnotationVisitor = this.fv.visitAnnotation(this.remapper.mapDesc(paramString), paramBoolean);
    return localAnnotationVisitor == null ? null : new RemappingAnnotationAdapter(localAnnotationVisitor, this.remapper);
  }
  
  public AnnotationVisitor visitTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    AnnotationVisitor localAnnotationVisitor = super.visitTypeAnnotation(paramInt, paramTypePath, this.remapper.mapDesc(paramString), paramBoolean);
    return localAnnotationVisitor == null ? null : new RemappingAnnotationAdapter(localAnnotationVisitor, this.remapper);
  }
}
