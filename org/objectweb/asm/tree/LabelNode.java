package org.objectweb.asm.tree;

import java.util.Map;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class LabelNode
  extends AbstractInsnNode
{
  private Label label;
  
  public LabelNode()
  {
    super(-1);
  }
  
  public LabelNode(Label paramLabel)
  {
    super(-1);
    this.label = paramLabel;
  }
  
  public int getType()
  {
    return 8;
  }
  
  public Label getLabel()
  {
    if (this.label == null) {
      this.label = new Label();
    }
    return this.label;
  }
  
  public void accept(MethodVisitor paramMethodVisitor)
  {
    paramMethodVisitor.visitLabel(getLabel());
  }
  
  public AbstractInsnNode clone(Map paramMap)
  {
    return (AbstractInsnNode)paramMap.get(this);
  }
  
  public void resetLabel()
  {
    this.label = null;
  }
}
