package org.objectweb.asm.util;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;

public class CheckFieldAdapter
  extends FieldVisitor
{
  private boolean end;
  static Class class$org$objectweb$asm$util$CheckFieldAdapter = class$("org.objectweb.asm.util.CheckFieldAdapter");
  
  public CheckFieldAdapter(FieldVisitor paramFieldVisitor)
  {
    this(327680, paramFieldVisitor);
    if (getClass() != class$org$objectweb$asm$util$CheckFieldAdapter) {
      throw new IllegalStateException();
    }
  }
  
  protected CheckFieldAdapter(int paramInt, FieldVisitor paramFieldVisitor)
  {
    super(paramInt, paramFieldVisitor);
  }
  
  public AnnotationVisitor visitAnnotation(String paramString, boolean paramBoolean)
  {
    checkEnd();
    CheckMethodAdapter.checkDesc(paramString, false);
    return new CheckAnnotationAdapter(super.visitAnnotation(paramString, paramBoolean));
  }
  
  public AnnotationVisitor visitTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    checkEnd();
    int i = paramInt >>> 24;
    if (i != 19) {
      throw new IllegalArgumentException("Invalid type reference sort 0x" + Integer.toHexString(i));
    }
    CheckClassAdapter.checkTypeRefAndPath(paramInt, paramTypePath);
    CheckMethodAdapter.checkDesc(paramString, false);
    return new CheckAnnotationAdapter(super.visitTypeAnnotation(paramInt, paramTypePath, paramString, paramBoolean));
  }
  
  public void visitAttribute(Attribute paramAttribute)
  {
    checkEnd();
    if (paramAttribute == null) {
      throw new IllegalArgumentException("Invalid attribute (must not be null)");
    }
    super.visitAttribute(paramAttribute);
  }
  
  public void visitEnd()
  {
    checkEnd();
    this.end = true;
    super.visitEnd();
  }
  
  private void checkEnd()
  {
    if (this.end) {
      throw new IllegalStateException("Cannot call a visit method after visitEnd has been called");
    }
  }
  
  static Class class$(String paramString)
  {
    try
    {
      return Class.forName(paramString);
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      String str = localClassNotFoundException.getMessage();
      throw new NoClassDefFoundError(str);
    }
  }
}
