package org.objectweb.asm.tree;

import java.util.Map;
import org.objectweb.asm.MethodVisitor;

public class InsnNode
  extends AbstractInsnNode
{
  public InsnNode(int paramInt)
  {
    super(paramInt);
  }
  
  public int getType()
  {
    return 0;
  }
  
  public void accept(MethodVisitor paramMethodVisitor)
  {
    paramMethodVisitor.visitInsn(this.opcode);
    acceptAnnotations(paramMethodVisitor);
  }
  
  public AbstractInsnNode clone(Map paramMap)
  {
    return new InsnNode(this.opcode).cloneAnnotations(this);
  }
}
