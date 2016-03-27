package org.objectweb.asm.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

public class MethodNode
  extends MethodVisitor
{
  public int access;
  public String name;
  public String desc;
  public String signature;
  public List exceptions;
  public List parameters;
  public List visibleAnnotations;
  public List invisibleAnnotations;
  public List visibleTypeAnnotations;
  public List invisibleTypeAnnotations;
  public List attrs;
  public Object annotationDefault;
  public List[] visibleParameterAnnotations;
  public List[] invisibleParameterAnnotations;
  public InsnList instructions;
  public List tryCatchBlocks;
  public int maxStack;
  public int maxLocals;
  public List localVariables;
  public List visibleLocalVariableAnnotations;
  public List invisibleLocalVariableAnnotations;
  private boolean visited;
  static Class class$org$objectweb$asm$tree$MethodNode = class$("org.objectweb.asm.tree.MethodNode");
  
  public MethodNode()
  {
    this(327680);
    if (getClass() != class$org$objectweb$asm$tree$MethodNode) {
      throw new IllegalStateException();
    }
  }
  
  public MethodNode(int paramInt)
  {
    super(paramInt);
    this.instructions = new InsnList();
  }
  
  public MethodNode(int paramInt, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    this(327680, paramInt, paramString1, paramString2, paramString3, paramArrayOfString);
    if (getClass() != class$org$objectweb$asm$tree$MethodNode) {
      throw new IllegalStateException();
    }
  }
  
  public MethodNode(int paramInt1, int paramInt2, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    super(paramInt1);
    this.access = paramInt2;
    this.name = paramString1;
    this.desc = paramString2;
    this.signature = paramString3;
    this.exceptions = new ArrayList(paramArrayOfString == null ? 0 : paramArrayOfString.length);
    int i = (paramInt2 & 0x400) != 0 ? 1 : 0;
    if (i == 0) {
      this.localVariables = new ArrayList(5);
    }
    this.tryCatchBlocks = new ArrayList();
    if (paramArrayOfString != null) {
      this.exceptions.addAll(Arrays.asList(paramArrayOfString));
    }
    this.instructions = new InsnList();
  }
  
  public void visitParameter(String paramString, int paramInt)
  {
    if (this.parameters == null) {
      this.parameters = new ArrayList(5);
    }
    this.parameters.add(new ParameterNode(paramString, paramInt));
  }
  
  public AnnotationVisitor visitAnnotationDefault()
  {
    return new AnnotationNode(new MethodNode.1(this, 0));
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
  
  public AnnotationVisitor visitParameterAnnotation(int paramInt, String paramString, boolean paramBoolean)
  {
    AnnotationNode localAnnotationNode = new AnnotationNode(paramString);
    int i;
    if (paramBoolean)
    {
      if (this.visibleParameterAnnotations == null)
      {
        i = Type.getArgumentTypes(this.desc).length;
        this.visibleParameterAnnotations = ((List[])new List[i]);
      }
      if (this.visibleParameterAnnotations[paramInt] == null) {
        this.visibleParameterAnnotations[paramInt] = new ArrayList(1);
      }
      this.visibleParameterAnnotations[paramInt].add(localAnnotationNode);
    }
    else
    {
      if (this.invisibleParameterAnnotations == null)
      {
        i = Type.getArgumentTypes(this.desc).length;
        this.invisibleParameterAnnotations = ((List[])new List[i]);
      }
      if (this.invisibleParameterAnnotations[paramInt] == null) {
        this.invisibleParameterAnnotations[paramInt] = new ArrayList(1);
      }
      this.invisibleParameterAnnotations[paramInt].add(localAnnotationNode);
    }
    return localAnnotationNode;
  }
  
  public void visitAttribute(Attribute paramAttribute)
  {
    if (this.attrs == null) {
      this.attrs = new ArrayList(1);
    }
    this.attrs.add(paramAttribute);
  }
  
  public void visitCode() {}
  
  public void visitFrame(int paramInt1, int paramInt2, Object[] paramArrayOfObject1, int paramInt3, Object[] paramArrayOfObject2)
  {
    this.instructions.add(new FrameNode(paramInt1, paramInt2, paramArrayOfObject1 == null ? null : getLabelNodes(paramArrayOfObject1), paramInt3, paramArrayOfObject2 == null ? null : getLabelNodes(paramArrayOfObject2)));
  }
  
  public void visitInsn(int paramInt)
  {
    this.instructions.add(new InsnNode(paramInt));
  }
  
  public void visitIntInsn(int paramInt1, int paramInt2)
  {
    this.instructions.add(new IntInsnNode(paramInt1, paramInt2));
  }
  
  public void visitVarInsn(int paramInt1, int paramInt2)
  {
    this.instructions.add(new VarInsnNode(paramInt1, paramInt2));
  }
  
  public void visitTypeInsn(int paramInt, String paramString)
  {
    this.instructions.add(new TypeInsnNode(paramInt, paramString));
  }
  
  public void visitFieldInsn(int paramInt, String paramString1, String paramString2, String paramString3)
  {
    this.instructions.add(new FieldInsnNode(paramInt, paramString1, paramString2, paramString3));
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
    this.instructions.add(new MethodInsnNode(paramInt, paramString1, paramString2, paramString3));
  }
  
  public void visitMethodInsn(int paramInt, String paramString1, String paramString2, String paramString3, boolean paramBoolean)
  {
    if (this.api < 327680)
    {
      super.visitMethodInsn(paramInt, paramString1, paramString2, paramString3, paramBoolean);
      return;
    }
    this.instructions.add(new MethodInsnNode(paramInt, paramString1, paramString2, paramString3, paramBoolean));
  }
  
  public void visitInvokeDynamicInsn(String paramString1, String paramString2, Handle paramHandle, Object... paramVarArgs)
  {
    this.instructions.add(new InvokeDynamicInsnNode(paramString1, paramString2, paramHandle, paramVarArgs));
  }
  
  public void visitJumpInsn(int paramInt, Label paramLabel)
  {
    this.instructions.add(new JumpInsnNode(paramInt, getLabelNode(paramLabel)));
  }
  
  public void visitLabel(Label paramLabel)
  {
    this.instructions.add(getLabelNode(paramLabel));
  }
  
  public void visitLdcInsn(Object paramObject)
  {
    this.instructions.add(new LdcInsnNode(paramObject));
  }
  
  public void visitIincInsn(int paramInt1, int paramInt2)
  {
    this.instructions.add(new IincInsnNode(paramInt1, paramInt2));
  }
  
  public void visitTableSwitchInsn(int paramInt1, int paramInt2, Label paramLabel, Label... paramVarArgs)
  {
    this.instructions.add(new TableSwitchInsnNode(paramInt1, paramInt2, getLabelNode(paramLabel), getLabelNodes(paramVarArgs)));
  }
  
  public void visitLookupSwitchInsn(Label paramLabel, int[] paramArrayOfInt, Label[] paramArrayOfLabel)
  {
    this.instructions.add(new LookupSwitchInsnNode(getLabelNode(paramLabel), paramArrayOfInt, getLabelNodes(paramArrayOfLabel)));
  }
  
  public void visitMultiANewArrayInsn(String paramString, int paramInt)
  {
    this.instructions.add(new MultiANewArrayInsnNode(paramString, paramInt));
  }
  
  public AnnotationVisitor visitInsnAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    for (AbstractInsnNode localAbstractInsnNode = this.instructions.getLast(); localAbstractInsnNode.getOpcode() == -1; localAbstractInsnNode = localAbstractInsnNode.getPrevious()) {}
    TypeAnnotationNode localTypeAnnotationNode = new TypeAnnotationNode(paramInt, paramTypePath, paramString);
    if (paramBoolean)
    {
      if (localAbstractInsnNode.visibleTypeAnnotations == null) {
        localAbstractInsnNode.visibleTypeAnnotations = new ArrayList(1);
      }
      localAbstractInsnNode.visibleTypeAnnotations.add(localTypeAnnotationNode);
    }
    else
    {
      if (localAbstractInsnNode.invisibleTypeAnnotations == null) {
        localAbstractInsnNode.invisibleTypeAnnotations = new ArrayList(1);
      }
      localAbstractInsnNode.invisibleTypeAnnotations.add(localTypeAnnotationNode);
    }
    return localTypeAnnotationNode;
  }
  
  public void visitTryCatchBlock(Label paramLabel1, Label paramLabel2, Label paramLabel3, String paramString)
  {
    this.tryCatchBlocks.add(new TryCatchBlockNode(getLabelNode(paramLabel1), getLabelNode(paramLabel2), getLabelNode(paramLabel3), paramString));
  }
  
  public AnnotationVisitor visitTryCatchAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    TryCatchBlockNode localTryCatchBlockNode = (TryCatchBlockNode)this.tryCatchBlocks.get((paramInt & 0xFFFF00) >> 8);
    TypeAnnotationNode localTypeAnnotationNode = new TypeAnnotationNode(paramInt, paramTypePath, paramString);
    if (paramBoolean)
    {
      if (localTryCatchBlockNode.visibleTypeAnnotations == null) {
        localTryCatchBlockNode.visibleTypeAnnotations = new ArrayList(1);
      }
      localTryCatchBlockNode.visibleTypeAnnotations.add(localTypeAnnotationNode);
    }
    else
    {
      if (localTryCatchBlockNode.invisibleTypeAnnotations == null) {
        localTryCatchBlockNode.invisibleTypeAnnotations = new ArrayList(1);
      }
      localTryCatchBlockNode.invisibleTypeAnnotations.add(localTypeAnnotationNode);
    }
    return localTypeAnnotationNode;
  }
  
  public void visitLocalVariable(String paramString1, String paramString2, String paramString3, Label paramLabel1, Label paramLabel2, int paramInt)
  {
    this.localVariables.add(new LocalVariableNode(paramString1, paramString2, paramString3, getLabelNode(paramLabel1), getLabelNode(paramLabel2), paramInt));
  }
  
  public AnnotationVisitor visitLocalVariableAnnotation(int paramInt, TypePath paramTypePath, Label[] paramArrayOfLabel1, Label[] paramArrayOfLabel2, int[] paramArrayOfInt, String paramString, boolean paramBoolean)
  {
    LocalVariableAnnotationNode localLocalVariableAnnotationNode = new LocalVariableAnnotationNode(paramInt, paramTypePath, getLabelNodes(paramArrayOfLabel1), getLabelNodes(paramArrayOfLabel2), paramArrayOfInt, paramString);
    if (paramBoolean)
    {
      if (this.visibleLocalVariableAnnotations == null) {
        this.visibleLocalVariableAnnotations = new ArrayList(1);
      }
      this.visibleLocalVariableAnnotations.add(localLocalVariableAnnotationNode);
    }
    else
    {
      if (this.invisibleLocalVariableAnnotations == null) {
        this.invisibleLocalVariableAnnotations = new ArrayList(1);
      }
      this.invisibleLocalVariableAnnotations.add(localLocalVariableAnnotationNode);
    }
    return localLocalVariableAnnotationNode;
  }
  
  public void visitLineNumber(int paramInt, Label paramLabel)
  {
    this.instructions.add(new LineNumberNode(paramInt, getLabelNode(paramLabel)));
  }
  
  public void visitMaxs(int paramInt1, int paramInt2)
  {
    this.maxStack = paramInt1;
    this.maxLocals = paramInt2;
  }
  
  public void visitEnd() {}
  
  protected LabelNode getLabelNode(Label paramLabel)
  {
    if (!(paramLabel.info instanceof LabelNode)) {
      paramLabel.info = new LabelNode();
    }
    return (LabelNode)paramLabel.info;
  }
  
  private LabelNode[] getLabelNodes(Label[] paramArrayOfLabel)
  {
    LabelNode[] arrayOfLabelNode = new LabelNode[paramArrayOfLabel.length];
    for (int i = 0; i < paramArrayOfLabel.length; i++) {
      arrayOfLabelNode[i] = getLabelNode(paramArrayOfLabel[i]);
    }
    return arrayOfLabelNode;
  }
  
  private Object[] getLabelNodes(Object[] paramArrayOfObject)
  {
    Object[] arrayOfObject = new Object[paramArrayOfObject.length];
    for (int i = 0; i < paramArrayOfObject.length; i++)
    {
      Object localObject = paramArrayOfObject[i];
      if ((localObject instanceof Label)) {
        localObject = getLabelNode((Label)localObject);
      }
      arrayOfObject[i] = localObject;
    }
    return arrayOfObject;
  }
  
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
      int i = this.tryCatchBlocks == null ? 0 : this.tryCatchBlocks.size();
      Object localObject;
      for (int j = 0; j < i; j++)
      {
        localObject = (TryCatchBlockNode)this.tryCatchBlocks.get(j);
        if ((((TryCatchBlockNode)localObject).visibleTypeAnnotations != null) && (((TryCatchBlockNode)localObject).visibleTypeAnnotations.size() > 0)) {
          throw new RuntimeException();
        }
        if ((((TryCatchBlockNode)localObject).invisibleTypeAnnotations != null) && (((TryCatchBlockNode)localObject).invisibleTypeAnnotations.size() > 0)) {
          throw new RuntimeException();
        }
      }
      for (j = 0; j < this.instructions.size(); j++)
      {
        localObject = this.instructions.get(j);
        if ((((AbstractInsnNode)localObject).visibleTypeAnnotations != null) && (((AbstractInsnNode)localObject).visibleTypeAnnotations.size() > 0)) {
          throw new RuntimeException();
        }
        if ((((AbstractInsnNode)localObject).invisibleTypeAnnotations != null) && (((AbstractInsnNode)localObject).invisibleTypeAnnotations.size() > 0)) {
          throw new RuntimeException();
        }
        if ((localObject instanceof MethodInsnNode))
        {
          boolean bool = ((MethodInsnNode)localObject).itf;
          if (bool != (((AbstractInsnNode)localObject).opcode == 185)) {
            throw new RuntimeException();
          }
        }
      }
      if ((this.visibleLocalVariableAnnotations != null) && (this.visibleLocalVariableAnnotations.size() > 0)) {
        throw new RuntimeException();
      }
      if ((this.invisibleLocalVariableAnnotations != null) && (this.invisibleLocalVariableAnnotations.size() > 0)) {
        throw new RuntimeException();
      }
    }
  }
  
  public void accept(ClassVisitor paramClassVisitor)
  {
    String[] arrayOfString = new String[this.exceptions.size()];
    this.exceptions.toArray(arrayOfString);
    MethodVisitor localMethodVisitor = paramClassVisitor.visitMethod(this.access, this.name, this.desc, this.signature, arrayOfString);
    if (localMethodVisitor != null) {
      accept(localMethodVisitor);
    }
  }
  
  public void accept(MethodVisitor paramMethodVisitor)
  {
    int i = this.parameters == null ? 0 : this.parameters.size();
    Object localObject;
    for (int j = 0; j < i; j++)
    {
      localObject = (ParameterNode)this.parameters.get(j);
      paramMethodVisitor.visitParameter(((ParameterNode)localObject).name, ((ParameterNode)localObject).access);
    }
    if (this.annotationDefault != null)
    {
      localObject = paramMethodVisitor.visitAnnotationDefault();
      AnnotationNode.accept((AnnotationVisitor)localObject, null, this.annotationDefault);
      if (localObject != null) {
        ((AnnotationVisitor)localObject).visitEnd();
      }
    }
    i = this.visibleAnnotations == null ? 0 : this.visibleAnnotations.size();
    for (j = 0; j < i; j++)
    {
      localObject = (AnnotationNode)this.visibleAnnotations.get(j);
      ((AnnotationNode)localObject).accept(paramMethodVisitor.visitAnnotation(((AnnotationNode)localObject).desc, true));
    }
    i = this.invisibleAnnotations == null ? 0 : this.invisibleAnnotations.size();
    for (j = 0; j < i; j++)
    {
      localObject = (AnnotationNode)this.invisibleAnnotations.get(j);
      ((AnnotationNode)localObject).accept(paramMethodVisitor.visitAnnotation(((AnnotationNode)localObject).desc, false));
    }
    i = this.visibleTypeAnnotations == null ? 0 : this.visibleTypeAnnotations.size();
    for (j = 0; j < i; j++)
    {
      localObject = (TypeAnnotationNode)this.visibleTypeAnnotations.get(j);
      ((TypeAnnotationNode)localObject).accept(paramMethodVisitor.visitTypeAnnotation(((TypeAnnotationNode)localObject).typeRef, ((TypeAnnotationNode)localObject).typePath, ((TypeAnnotationNode)localObject).desc, true));
    }
    i = this.invisibleTypeAnnotations == null ? 0 : this.invisibleTypeAnnotations.size();
    for (j = 0; j < i; j++)
    {
      localObject = (TypeAnnotationNode)this.invisibleTypeAnnotations.get(j);
      ((TypeAnnotationNode)localObject).accept(paramMethodVisitor.visitTypeAnnotation(((TypeAnnotationNode)localObject).typeRef, ((TypeAnnotationNode)localObject).typePath, ((TypeAnnotationNode)localObject).desc, false));
    }
    i = this.visibleParameterAnnotations == null ? 0 : this.visibleParameterAnnotations.length;
    int k;
    AnnotationNode localAnnotationNode;
    for (j = 0; j < i; j++)
    {
      localObject = this.visibleParameterAnnotations[j];
      if (localObject != null) {
        for (k = 0; k < ((List)localObject).size(); k++)
        {
          localAnnotationNode = (AnnotationNode)((List)localObject).get(k);
          localAnnotationNode.accept(paramMethodVisitor.visitParameterAnnotation(j, localAnnotationNode.desc, true));
        }
      }
    }
    i = this.invisibleParameterAnnotations == null ? 0 : this.invisibleParameterAnnotations.length;
    for (j = 0; j < i; j++)
    {
      localObject = this.invisibleParameterAnnotations[j];
      if (localObject != null) {
        for (k = 0; k < ((List)localObject).size(); k++)
        {
          localAnnotationNode = (AnnotationNode)((List)localObject).get(k);
          localAnnotationNode.accept(paramMethodVisitor.visitParameterAnnotation(j, localAnnotationNode.desc, false));
        }
      }
    }
    if (this.visited) {
      this.instructions.resetLabels();
    }
    i = this.attrs == null ? 0 : this.attrs.size();
    for (j = 0; j < i; j++) {
      paramMethodVisitor.visitAttribute((Attribute)this.attrs.get(j));
    }
    if (this.instructions.size() > 0)
    {
      paramMethodVisitor.visitCode();
      i = this.tryCatchBlocks == null ? 0 : this.tryCatchBlocks.size();
      for (j = 0; j < i; j++)
      {
        ((TryCatchBlockNode)this.tryCatchBlocks.get(j)).updateIndex(j);
        ((TryCatchBlockNode)this.tryCatchBlocks.get(j)).accept(paramMethodVisitor);
      }
      this.instructions.accept(paramMethodVisitor);
      i = this.localVariables == null ? 0 : this.localVariables.size();
      for (j = 0; j < i; j++) {
        ((LocalVariableNode)this.localVariables.get(j)).accept(paramMethodVisitor);
      }
      i = this.visibleLocalVariableAnnotations == null ? 0 : this.visibleLocalVariableAnnotations.size();
      for (j = 0; j < i; j++) {
        ((LocalVariableAnnotationNode)this.visibleLocalVariableAnnotations.get(j)).accept(paramMethodVisitor, true);
      }
      i = this.invisibleLocalVariableAnnotations == null ? 0 : this.invisibleLocalVariableAnnotations.size();
      for (j = 0; j < i; j++) {
        ((LocalVariableAnnotationNode)this.invisibleLocalVariableAnnotations.get(j)).accept(paramMethodVisitor, false);
      }
      paramMethodVisitor.visitMaxs(this.maxStack, this.maxLocals);
      this.visited = true;
    }
    paramMethodVisitor.visitEnd();
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
