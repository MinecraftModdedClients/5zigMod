package org.objectweb.asm.tree;

import java.util.Map;
import org.objectweb.asm.MethodVisitor;

public class IntInsnNode
  extends AbstractInsnNode
{
  public int operand;
  
  public IntInsnNode(int paramInt1, int paramInt2)
  {
    super(paramInt1);
    this.operand = paramInt2;
  }
  
  public void setOpcode(int paramInt)
  {
    this.opcode = paramInt;
  }
  
  public int getType()
  {
    return 1;
  }
  
  public void accept(MethodVisitor paramMethodVisitor)
  {
    paramMethodVisitor.visitIntInsn(this.opcode, this.operand);
    acceptAnnotations(paramMethodVisitor);
  }
  
  public AbstractInsnNode clone(Map paramMap)
  {
    return new IntInsnNode(this.opcode, this.operand).cloneAnnotations(this);
  }
}
