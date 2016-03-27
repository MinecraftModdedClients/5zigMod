package org.objectweb.asm.commons;

import org.objectweb.asm.AnnotationVisitor;

public class RemappingAnnotationAdapter
  extends AnnotationVisitor
{
  protected final Remapper remapper;
  
  public RemappingAnnotationAdapter(AnnotationVisitor paramAnnotationVisitor, Remapper paramRemapper)
  {
    this(327680, paramAnnotationVisitor, paramRemapper);
  }
  
  protected RemappingAnnotationAdapter(int paramInt, AnnotationVisitor paramAnnotationVisitor, Remapper paramRemapper)
  {
    super(paramInt, paramAnnotationVisitor);
    this.remapper = paramRemapper;
  }
  
  public void visit(String paramString, Object paramObject)
  {
    this.av.visit(paramString, this.remapper.mapValue(paramObject));
  }
  
  public void visitEnum(String paramString1, String paramString2, String paramString3)
  {
    this.av.visitEnum(paramString1, this.remapper.mapDesc(paramString2), paramString3);
  }
  
  public AnnotationVisitor visitAnnotation(String paramString1, String paramString2)
  {
    AnnotationVisitor localAnnotationVisitor = this.av.visitAnnotation(paramString1, this.remapper.mapDesc(paramString2));
    return localAnnotationVisitor == this.av ? this : localAnnotationVisitor == null ? null : new RemappingAnnotationAdapter(localAnnotationVisitor, this.remapper);
  }
  
  public AnnotationVisitor visitArray(String paramString)
  {
    AnnotationVisitor localAnnotationVisitor = this.av.visitArray(paramString);
    return localAnnotationVisitor == this.av ? this : localAnnotationVisitor == null ? null : new RemappingAnnotationAdapter(localAnnotationVisitor, this.remapper);
  }
}
