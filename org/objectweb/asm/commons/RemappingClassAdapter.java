package org.objectweb.asm.commons;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;

public class RemappingClassAdapter
  extends ClassVisitor
{
  protected final Remapper remapper;
  protected String className;
  
  public RemappingClassAdapter(ClassVisitor paramClassVisitor, Remapper paramRemapper)
  {
    this(327680, paramClassVisitor, paramRemapper);
  }
  
  protected RemappingClassAdapter(int paramInt, ClassVisitor paramClassVisitor, Remapper paramRemapper)
  {
    super(paramInt, paramClassVisitor);
    this.remapper = paramRemapper;
  }
  
  public void visit(int paramInt1, int paramInt2, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    this.className = paramString1;
    super.visit(paramInt1, paramInt2, this.remapper.mapType(paramString1), this.remapper.mapSignature(paramString2, false), this.remapper.mapType(paramString3), paramArrayOfString == null ? null : this.remapper.mapTypes(paramArrayOfString));
  }
  
  public AnnotationVisitor visitAnnotation(String paramString, boolean paramBoolean)
  {
    AnnotationVisitor localAnnotationVisitor = super.visitAnnotation(this.remapper.mapDesc(paramString), paramBoolean);
    return localAnnotationVisitor == null ? null : createRemappingAnnotationAdapter(localAnnotationVisitor);
  }
  
  public AnnotationVisitor visitTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    AnnotationVisitor localAnnotationVisitor = super.visitTypeAnnotation(paramInt, paramTypePath, this.remapper.mapDesc(paramString), paramBoolean);
    return localAnnotationVisitor == null ? null : createRemappingAnnotationAdapter(localAnnotationVisitor);
  }
  
  public FieldVisitor visitField(int paramInt, String paramString1, String paramString2, String paramString3, Object paramObject)
  {
    FieldVisitor localFieldVisitor = super.visitField(paramInt, this.remapper.mapFieldName(this.className, paramString1, paramString2), this.remapper.mapDesc(paramString2), this.remapper.mapSignature(paramString3, true), this.remapper.mapValue(paramObject));
    return localFieldVisitor == null ? null : createRemappingFieldAdapter(localFieldVisitor);
  }
  
  public MethodVisitor visitMethod(int paramInt, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    String str = this.remapper.mapMethodDesc(paramString2);
    MethodVisitor localMethodVisitor = super.visitMethod(paramInt, this.remapper.mapMethodName(this.className, paramString1, paramString2), str, this.remapper.mapSignature(paramString3, false), paramArrayOfString == null ? null : this.remapper.mapTypes(paramArrayOfString));
    return localMethodVisitor == null ? null : createRemappingMethodAdapter(paramInt, str, localMethodVisitor);
  }
  
  public void visitInnerClass(String paramString1, String paramString2, String paramString3, int paramInt)
  {
    super.visitInnerClass(this.remapper.mapType(paramString1), paramString2 == null ? null : this.remapper.mapType(paramString2), paramString3, paramInt);
  }
  
  public void visitOuterClass(String paramString1, String paramString2, String paramString3)
  {
    super.visitOuterClass(this.remapper.mapType(paramString1), paramString2 == null ? null : this.remapper.mapMethodName(paramString1, paramString2, paramString3), paramString3 == null ? null : this.remapper.mapMethodDesc(paramString3));
  }
  
  protected FieldVisitor createRemappingFieldAdapter(FieldVisitor paramFieldVisitor)
  {
    return new RemappingFieldAdapter(paramFieldVisitor, this.remapper);
  }
  
  protected MethodVisitor createRemappingMethodAdapter(int paramInt, String paramString, MethodVisitor paramMethodVisitor)
  {
    return new RemappingMethodAdapter(paramInt, paramString, paramMethodVisitor, this.remapper);
  }
  
  protected AnnotationVisitor createRemappingAnnotationAdapter(AnnotationVisitor paramAnnotationVisitor)
  {
    return new RemappingAnnotationAdapter(paramAnnotationVisitor, this.remapper);
  }
}
