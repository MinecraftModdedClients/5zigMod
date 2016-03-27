package org.objectweb.asm.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.MethodVisitor;

public abstract class AbstractInsnNode
{
  public static final int INSN = 0;
  public static final int INT_INSN = 1;
  public static final int VAR_INSN = 2;
  public static final int TYPE_INSN = 3;
  public static final int FIELD_INSN = 4;
  public static final int METHOD_INSN = 5;
  public static final int INVOKE_DYNAMIC_INSN = 6;
  public static final int JUMP_INSN = 7;
  public static final int LABEL = 8;
  public static final int LDC_INSN = 9;
  public static final int IINC_INSN = 10;
  public static final int TABLESWITCH_INSN = 11;
  public static final int LOOKUPSWITCH_INSN = 12;
  public static final int MULTIANEWARRAY_INSN = 13;
  public static final int FRAME = 14;
  public static final int LINE = 15;
  protected int opcode;
  public List visibleTypeAnnotations;
  public List invisibleTypeAnnotations;
  AbstractInsnNode prev;
  AbstractInsnNode next;
  int index;
  
  protected AbstractInsnNode(int paramInt)
  {
    this.opcode = paramInt;
    this.index = -1;
  }
  
  public int getOpcode()
  {
    return this.opcode;
  }
  
  public abstract int getType();
  
  public AbstractInsnNode getPrevious()
  {
    return this.prev;
  }
  
  public AbstractInsnNode getNext()
  {
    return this.next;
  }
  
  public abstract void accept(MethodVisitor paramMethodVisitor);
  
  protected final void acceptAnnotations(MethodVisitor paramMethodVisitor)
  {
    int i = this.visibleTypeAnnotations == null ? 0 : this.visibleTypeAnnotations.size();
    TypeAnnotationNode localTypeAnnotationNode;
    for (int j = 0; j < i; j++)
    {
      localTypeAnnotationNode = (TypeAnnotationNode)this.visibleTypeAnnotations.get(j);
      localTypeAnnotationNode.accept(paramMethodVisitor.visitInsnAnnotation(localTypeAnnotationNode.typeRef, localTypeAnnotationNode.typePath, localTypeAnnotationNode.desc, true));
    }
    i = this.invisibleTypeAnnotations == null ? 0 : this.invisibleTypeAnnotations.size();
    for (j = 0; j < i; j++)
    {
      localTypeAnnotationNode = (TypeAnnotationNode)this.invisibleTypeAnnotations.get(j);
      localTypeAnnotationNode.accept(paramMethodVisitor.visitInsnAnnotation(localTypeAnnotationNode.typeRef, localTypeAnnotationNode.typePath, localTypeAnnotationNode.desc, false));
    }
  }
  
  public abstract AbstractInsnNode clone(Map paramMap);
  
  static LabelNode clone(LabelNode paramLabelNode, Map paramMap)
  {
    return (LabelNode)paramMap.get(paramLabelNode);
  }
  
  static LabelNode[] clone(List paramList, Map paramMap)
  {
    LabelNode[] arrayOfLabelNode = new LabelNode[paramList.size()];
    for (int i = 0; i < arrayOfLabelNode.length; i++) {
      arrayOfLabelNode[i] = ((LabelNode)paramMap.get(paramList.get(i)));
    }
    return arrayOfLabelNode;
  }
  
  protected final AbstractInsnNode cloneAnnotations(AbstractInsnNode paramAbstractInsnNode)
  {
    int i;
    TypeAnnotationNode localTypeAnnotationNode1;
    TypeAnnotationNode localTypeAnnotationNode2;
    if (paramAbstractInsnNode.visibleTypeAnnotations != null)
    {
      this.visibleTypeAnnotations = new ArrayList();
      for (i = 0; i < paramAbstractInsnNode.visibleTypeAnnotations.size(); i++)
      {
        localTypeAnnotationNode1 = (TypeAnnotationNode)paramAbstractInsnNode.visibleTypeAnnotations.get(i);
        localTypeAnnotationNode2 = new TypeAnnotationNode(localTypeAnnotationNode1.typeRef, localTypeAnnotationNode1.typePath, localTypeAnnotationNode1.desc);
        localTypeAnnotationNode1.accept(localTypeAnnotationNode2);
        this.visibleTypeAnnotations.add(localTypeAnnotationNode2);
      }
    }
    if (paramAbstractInsnNode.invisibleTypeAnnotations != null)
    {
      this.invisibleTypeAnnotations = new ArrayList();
      for (i = 0; i < paramAbstractInsnNode.invisibleTypeAnnotations.size(); i++)
      {
        localTypeAnnotationNode1 = (TypeAnnotationNode)paramAbstractInsnNode.invisibleTypeAnnotations.get(i);
        localTypeAnnotationNode2 = new TypeAnnotationNode(localTypeAnnotationNode1.typeRef, localTypeAnnotationNode1.typePath, localTypeAnnotationNode1.desc);
        localTypeAnnotationNode1.accept(localTypeAnnotationNode2);
        this.invisibleTypeAnnotations.add(localTypeAnnotationNode2);
      }
    }
    return this;
  }
}
