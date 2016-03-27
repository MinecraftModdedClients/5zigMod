package org.objectweb.asm.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;

public class ClassNode
  extends ClassVisitor
{
  public int version;
  public int access;
  public String name;
  public String signature;
  public String superName;
  public List interfaces = new ArrayList();
  public String sourceFile;
  public String sourceDebug;
  public String outerClass;
  public String outerMethod;
  public String outerMethodDesc;
  public List visibleAnnotations;
  public List invisibleAnnotations;
  public List visibleTypeAnnotations;
  public List invisibleTypeAnnotations;
  public List attrs;
  public List innerClasses = new ArrayList();
  public List fields = new ArrayList();
  public List methods = new ArrayList();
  static Class class$org$objectweb$asm$tree$ClassNode = class$("org.objectweb.asm.tree.ClassNode");
  
  public ClassNode()
  {
    this(327680);
    if (getClass() != class$org$objectweb$asm$tree$ClassNode) {
      throw new IllegalStateException();
    }
  }
  
  public ClassNode(int paramInt)
  {
    super(paramInt);
  }
  
  public void visit(int paramInt1, int paramInt2, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    this.version = paramInt1;
    this.access = paramInt2;
    this.name = paramString1;
    this.signature = paramString2;
    this.superName = paramString3;
    if (paramArrayOfString != null) {
      this.interfaces.addAll(Arrays.asList(paramArrayOfString));
    }
  }
  
  public void visitSource(String paramString1, String paramString2)
  {
    this.sourceFile = paramString1;
    this.sourceDebug = paramString2;
  }
  
  public void visitOuterClass(String paramString1, String paramString2, String paramString3)
  {
    this.outerClass = paramString1;
    this.outerMethod = paramString2;
    this.outerMethodDesc = paramString3;
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
  
  public void visitInnerClass(String paramString1, String paramString2, String paramString3, int paramInt)
  {
    InnerClassNode localInnerClassNode = new InnerClassNode(paramString1, paramString2, paramString3, paramInt);
    this.innerClasses.add(localInnerClassNode);
  }
  
  public FieldVisitor visitField(int paramInt, String paramString1, String paramString2, String paramString3, Object paramObject)
  {
    FieldNode localFieldNode = new FieldNode(paramInt, paramString1, paramString2, paramString3, paramObject);
    this.fields.add(localFieldNode);
    return localFieldNode;
  }
  
  public MethodVisitor visitMethod(int paramInt, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    MethodNode localMethodNode = new MethodNode(paramInt, paramString1, paramString2, paramString3, paramArrayOfString);
    this.methods.add(localMethodNode);
    return localMethodNode;
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
      Iterator localIterator = this.fields.iterator();
      Object localObject;
      while (localIterator.hasNext())
      {
        localObject = (FieldNode)localIterator.next();
        ((FieldNode)localObject).check(paramInt);
      }
      localIterator = this.methods.iterator();
      while (localIterator.hasNext())
      {
        localObject = (MethodNode)localIterator.next();
        ((MethodNode)localObject).check(paramInt);
      }
    }
  }
  
  public void accept(ClassVisitor paramClassVisitor)
  {
    String[] arrayOfString = new String[this.interfaces.size()];
    this.interfaces.toArray(arrayOfString);
    paramClassVisitor.visit(this.version, this.access, this.name, this.signature, this.superName, arrayOfString);
    if ((this.sourceFile != null) || (this.sourceDebug != null)) {
      paramClassVisitor.visitSource(this.sourceFile, this.sourceDebug);
    }
    if (this.outerClass != null) {
      paramClassVisitor.visitOuterClass(this.outerClass, this.outerMethod, this.outerMethodDesc);
    }
    int i = this.visibleAnnotations == null ? 0 : this.visibleAnnotations.size();
    Object localObject;
    for (int j = 0; j < i; j++)
    {
      localObject = (AnnotationNode)this.visibleAnnotations.get(j);
      ((AnnotationNode)localObject).accept(paramClassVisitor.visitAnnotation(((AnnotationNode)localObject).desc, true));
    }
    i = this.invisibleAnnotations == null ? 0 : this.invisibleAnnotations.size();
    for (j = 0; j < i; j++)
    {
      localObject = (AnnotationNode)this.invisibleAnnotations.get(j);
      ((AnnotationNode)localObject).accept(paramClassVisitor.visitAnnotation(((AnnotationNode)localObject).desc, false));
    }
    i = this.visibleTypeAnnotations == null ? 0 : this.visibleTypeAnnotations.size();
    for (j = 0; j < i; j++)
    {
      localObject = (TypeAnnotationNode)this.visibleTypeAnnotations.get(j);
      ((TypeAnnotationNode)localObject).accept(paramClassVisitor.visitTypeAnnotation(((TypeAnnotationNode)localObject).typeRef, ((TypeAnnotationNode)localObject).typePath, ((TypeAnnotationNode)localObject).desc, true));
    }
    i = this.invisibleTypeAnnotations == null ? 0 : this.invisibleTypeAnnotations.size();
    for (j = 0; j < i; j++)
    {
      localObject = (TypeAnnotationNode)this.invisibleTypeAnnotations.get(j);
      ((TypeAnnotationNode)localObject).accept(paramClassVisitor.visitTypeAnnotation(((TypeAnnotationNode)localObject).typeRef, ((TypeAnnotationNode)localObject).typePath, ((TypeAnnotationNode)localObject).desc, false));
    }
    i = this.attrs == null ? 0 : this.attrs.size();
    for (j = 0; j < i; j++) {
      paramClassVisitor.visitAttribute((Attribute)this.attrs.get(j));
    }
    for (j = 0; j < this.innerClasses.size(); j++) {
      ((InnerClassNode)this.innerClasses.get(j)).accept(paramClassVisitor);
    }
    for (j = 0; j < this.fields.size(); j++) {
      ((FieldNode)this.fields.get(j)).accept(paramClassVisitor);
    }
    for (j = 0; j < this.methods.size(); j++) {
      ((MethodNode)this.methods.get(j)).accept(paramClassVisitor);
    }
    paramClassVisitor.visitEnd();
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
