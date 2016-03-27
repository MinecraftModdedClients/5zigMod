package org.objectweb.asm.commons;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;

public class RemappingMethodAdapter
  extends LocalVariablesSorter
{
  protected final Remapper remapper;
  
  public RemappingMethodAdapter(int paramInt, String paramString, MethodVisitor paramMethodVisitor, Remapper paramRemapper)
  {
    this(327680, paramInt, paramString, paramMethodVisitor, paramRemapper);
  }
  
  protected RemappingMethodAdapter(int paramInt1, int paramInt2, String paramString, MethodVisitor paramMethodVisitor, Remapper paramRemapper)
  {
    super(paramInt1, paramInt2, paramString, paramMethodVisitor);
    this.remapper = paramRemapper;
  }
  
  public AnnotationVisitor visitAnnotationDefault()
  {
    AnnotationVisitor localAnnotationVisitor = super.visitAnnotationDefault();
    return localAnnotationVisitor == null ? localAnnotationVisitor : new RemappingAnnotationAdapter(localAnnotationVisitor, this.remapper);
  }
  
  public AnnotationVisitor visitAnnotation(String paramString, boolean paramBoolean)
  {
    AnnotationVisitor localAnnotationVisitor = super.visitAnnotation(this.remapper.mapDesc(paramString), paramBoolean);
    return localAnnotationVisitor == null ? localAnnotationVisitor : new RemappingAnnotationAdapter(localAnnotationVisitor, this.remapper);
  }
  
  public AnnotationVisitor visitTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    AnnotationVisitor localAnnotationVisitor = super.visitTypeAnnotation(paramInt, paramTypePath, this.remapper.mapDesc(paramString), paramBoolean);
    return localAnnotationVisitor == null ? localAnnotationVisitor : new RemappingAnnotationAdapter(localAnnotationVisitor, this.remapper);
  }
  
  public AnnotationVisitor visitParameterAnnotation(int paramInt, String paramString, boolean paramBoolean)
  {
    AnnotationVisitor localAnnotationVisitor = super.visitParameterAnnotation(paramInt, this.remapper.mapDesc(paramString), paramBoolean);
    return localAnnotationVisitor == null ? localAnnotationVisitor : new RemappingAnnotationAdapter(localAnnotationVisitor, this.remapper);
  }
  
  public void visitFrame(int paramInt1, int paramInt2, Object[] paramArrayOfObject1, int paramInt3, Object[] paramArrayOfObject2)
  {
    super.visitFrame(paramInt1, paramInt2, remapEntries(paramInt2, paramArrayOfObject1), paramInt3, remapEntries(paramInt3, paramArrayOfObject2));
  }
  
  private Object[] remapEntries(int paramInt, Object[] paramArrayOfObject)
  {
    for (int i = 0; i < paramInt; i++) {
      if ((paramArrayOfObject[i] instanceof String))
      {
        Object[] arrayOfObject = new Object[paramInt];
        if (i > 0) {
          System.arraycopy(paramArrayOfObject, 0, arrayOfObject, 0, i);
        }
        do
        {
          Object localObject = paramArrayOfObject[i];
          arrayOfObject[(i++)] = ((localObject instanceof String) ? this.remapper.mapType((String)localObject) : localObject);
        } while (i < paramInt);
        return arrayOfObject;
      }
    }
    return paramArrayOfObject;
  }
  
  public void visitFieldInsn(int paramInt, String paramString1, String paramString2, String paramString3)
  {
    super.visitFieldInsn(paramInt, this.remapper.mapType(paramString1), this.remapper.mapFieldName(paramString1, paramString2, paramString3), this.remapper.mapDesc(paramString3));
  }
  
  /**
   * @deprecated
   */
  public void visitMethodInsn(int paramInt, String paramString1, String paramString2, String paramString3)
  {
    if (this.api >= 327680)
    {
      super.visitMethodInsn(paramInt, paramString1, paramString2, paramString3);
      return;
    }
    doVisitMethodInsn(paramInt, paramString1, paramString2, paramString3, paramInt == 185);
  }
  
  public void visitMethodInsn(int paramInt, String paramString1, String paramString2, String paramString3, boolean paramBoolean)
  {
    if (this.api < 327680)
    {
      super.visitMethodInsn(paramInt, paramString1, paramString2, paramString3, paramBoolean);
      return;
    }
    doVisitMethodInsn(paramInt, paramString1, paramString2, paramString3, paramBoolean);
  }
  
  private void doVisitMethodInsn(int paramInt, String paramString1, String paramString2, String paramString3, boolean paramBoolean)
  {
    if (this.mv != null) {
      this.mv.visitMethodInsn(paramInt, this.remapper.mapType(paramString1), this.remapper.mapMethodName(paramString1, paramString2, paramString3), this.remapper.mapMethodDesc(paramString3), paramBoolean);
    }
  }
  
  public void visitInvokeDynamicInsn(String paramString1, String paramString2, Handle paramHandle, Object... paramVarArgs)
  {
    for (int i = 0; i < paramVarArgs.length; i++) {
      paramVarArgs[i] = this.remapper.mapValue(paramVarArgs[i]);
    }
    super.visitInvokeDynamicInsn(this.remapper.mapInvokeDynamicMethodName(paramString1, paramString2), this.remapper.mapMethodDesc(paramString2), (Handle)this.remapper.mapValue(paramHandle), paramVarArgs);
  }
  
  public void visitTypeInsn(int paramInt, String paramString)
  {
    super.visitTypeInsn(paramInt, this.remapper.mapType(paramString));
  }
  
  public void visitLdcInsn(Object paramObject)
  {
    super.visitLdcInsn(this.remapper.mapValue(paramObject));
  }
  
  public void visitMultiANewArrayInsn(String paramString, int paramInt)
  {
    super.visitMultiANewArrayInsn(this.remapper.mapDesc(paramString), paramInt);
  }
  
  public AnnotationVisitor visitInsnAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    AnnotationVisitor localAnnotationVisitor = super.visitInsnAnnotation(paramInt, paramTypePath, this.remapper.mapDesc(paramString), paramBoolean);
    return localAnnotationVisitor == null ? localAnnotationVisitor : new RemappingAnnotationAdapter(localAnnotationVisitor, this.remapper);
  }
  
  public void visitTryCatchBlock(Label paramLabel1, Label paramLabel2, Label paramLabel3, String paramString)
  {
    super.visitTryCatchBlock(paramLabel1, paramLabel2, paramLabel3, paramString == null ? null : this.remapper.mapType(paramString));
  }
  
  public AnnotationVisitor visitTryCatchAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    AnnotationVisitor localAnnotationVisitor = super.visitTryCatchAnnotation(paramInt, paramTypePath, this.remapper.mapDesc(paramString), paramBoolean);
    return localAnnotationVisitor == null ? localAnnotationVisitor : new RemappingAnnotationAdapter(localAnnotationVisitor, this.remapper);
  }
  
  public void visitLocalVariable(String paramString1, String paramString2, String paramString3, Label paramLabel1, Label paramLabel2, int paramInt)
  {
    super.visitLocalVariable(paramString1, this.remapper.mapDesc(paramString2), this.remapper.mapSignature(paramString3, true), paramLabel1, paramLabel2, paramInt);
  }
  
  public AnnotationVisitor visitLocalVariableAnnotation(int paramInt, TypePath paramTypePath, Label[] paramArrayOfLabel1, Label[] paramArrayOfLabel2, int[] paramArrayOfInt, String paramString, boolean paramBoolean)
  {
    AnnotationVisitor localAnnotationVisitor = super.visitLocalVariableAnnotation(paramInt, paramTypePath, paramArrayOfLabel1, paramArrayOfLabel2, paramArrayOfInt, this.remapper.mapDesc(paramString), paramBoolean);
    return localAnnotationVisitor == null ? localAnnotationVisitor : new RemappingAnnotationAdapter(localAnnotationVisitor, this.remapper);
  }
}
