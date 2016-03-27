package org.objectweb.asm.tree;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;

public class FieldNode
  extends FieldVisitor
{
  public int access;
  public String name;
  public String desc;
  public String signature;
  public Object value;
  public List visibleAnnotations;
  public List invisibleAnnotations;
  public List visibleTypeAnnotations;
  public List invisibleTypeAnnotations;
  public List attrs;
  static Class class$org$objectweb$asm$tree$FieldNode = class$("org.objectweb.asm.tree.FieldNode");
  
  public FieldNode(int paramInt, String paramString1, String paramString2, String paramString3, Object paramObject)
  {
    this(327680, paramInt, paramString1, paramString2, paramString3, paramObject);
    if (getClass() != class$org$objectweb$asm$tree$FieldNode) {
      throw new IllegalStateException();
    }
  }
  
  public FieldNode(int paramInt1, int paramInt2, String paramString1, String paramString2, String paramString3, Object paramObject)
  {
    super(paramInt1);
    this.access = paramInt2;
    this.name = paramString1;
    this.desc = paramString2;
    this.signature = paramString3;
    this.value = paramObject;
  }
  
  public AnnotationVisitor visitAnnotation(String paramString, boolean paramBoolean)
  {
    AnnotationNode localAnnotationNode = new AnnotationNode(paramString);
    if (paramBoolean)
    {
      if (this.visibleAnnotations == null) {
        this.visibleAnnotations = new ArrayList(1);
      }
      this.visibleAnnotations.add(localAnnotationNode);
    }
    else
    {
      if (this.invisibleAnnotations == null) {
        this.invisibleAnnotations = new ArrayList(1);
      }
      this.invisibleAnnotations.add(localAnnotationNode);
    }
    return localAnnotationNode;
  }
  
  public AnnotationVisitor visitTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    TypeAnnotationNode localTypeAnnotationNode = new TypeAnnotationNode(paramInt, paramTypePath, paramString);
    if (paramBoolean)
    {
      if (this.visibleTypeAnnotations == null) {
        this.visibleTypeAnnotations = new ArrayList(1);
      }
      this.visibleTypeAnnotations.add(localTypeAnnotationNode);
    }
    else
    {
      if (this.invisibleTypeAnnotations == null) {
        this.invisibleTypeAnnotations = new ArrayList(1);
      }
      this.invisibleTypeAnnotations.add(localTypeAnnotationNode);
    }
    return localTypeAnnotationNode;
  }
  
  public void visitAttribute(Attribute paramAttribute)
  {
    if (this.attrs == null) {
      this.attrs = new ArrayList(1);
    }
    this.attrs.add(paramAttribute);
  }
  
  public void visitEnd() {}
  
  public void check(int paramInt)
  {
    if (paramInt == 262144)
    {
      if ((this.visibleTypeAnnotations != null) && (this.visibleTypeAnnotations.size() > 0)) {
        throw new RuntimeException();
      }
      if ((this.invisibleTypeAnnotations != null) && (this.invisibleTypeAnnotations.size() > 0)) {
        throw new RuntimeException();
      }
    }
  }
  
  public void accept(ClassVisitor paramClassVisitor)
  {
    FieldVisitor localFieldVisitor = paramClassVisitor.visitField(this.access, this.name, this.desc, this.signature, this.value);
    if (localFieldVisitor == null) {
      return;
    }
    int i = this.visibleAnnotations == null ? 0 : this.visibleAnnotations.size();
    Object localObject;
    for (int j = 0; j < i; j++)
    {
      localObject = (AnnotationNode)this.visibleAnnotations.get(j);
      ((AnnotationNode)localObject).accept(localFieldVisitor.visitAnnotation(((AnnotationNode)localObject).desc, true));
    }
    i = this.invisibleAnnotations == null ? 0 : this.invisibleAnnotations.size();
    for (j = 0; j < i; j++)
    {
      localObject = (AnnotationNode)this.invisibleAnnotations.get(j);
      ((AnnotationNode)localObject).accept(localFieldVisitor.visitAnnotation(((AnnotationNode)localObject).desc, false));
    }
    i = this.visibleTypeAnnotations == null ? 0 : this.visibleTypeAnnotations.size();
    for (j = 0; j < i; j++)
    {
      localObject = (TypeAnnotationNode)this.visibleTypeAnnotations.get(j);
      ((TypeAnnotationNode)localObject).accept(localFieldVisitor.visitTypeAnnotation(((TypeAnnotationNode)localObject).typeRef, ((TypeAnnotationNode)localObject).typePath, ((TypeAnnotationNode)localObject).desc, true));
    }
    i = this.invisibleTypeAnnotations == null ? 0 : this.invisibleTypeAnnotations.size();
    for (j = 0; j < i; j++)
    {
      localObject = (TypeAnnotationNode)this.invisibleTypeAnnotations.get(j);
      ((TypeAnnotationNode)localObject).accept(localFieldVisitor.visitTypeAnnotation(((TypeAnnotationNode)localObject).typeRef, ((TypeAnnotationNode)localObject).typePath, ((TypeAnnotationNode)localObject).desc, false));
    }
    i = this.attrs == null ? 0 : this.attrs.size();
    for (j = 0; j < i; j++) {
      localFieldVisitor.visitAttribute((Attribute)this.attrs.get(j));
    }
    localFieldVisitor.visitEnd();
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
