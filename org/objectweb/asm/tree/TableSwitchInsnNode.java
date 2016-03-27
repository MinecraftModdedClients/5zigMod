package org.objectweb.asm.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class TableSwitchInsnNode
  extends AbstractInsnNode
{
  public int min;
  public int max;
  public LabelNode dflt;
  public List labels;
  
  public TableSwitchInsnNode(int paramInt1, int paramInt2, LabelNode paramLabelNode, LabelNode... paramVarArgs)
  {
    super(170);
    this.min = paramInt1;
    this.max = paramInt2;
    this.dflt = paramLabelNode;
    this.labels = new ArrayList();
    if (paramVarArgs != null) {
      this.labels.addAll(Arrays.asList(paramVarArgs));
    }
  }
  
  public int getType()
  {
    return 11;
  }
  
  public void accept(MethodVisitor paramMethodVisitor)
  {
    Label[] arrayOfLabel = new Label[this.labels.size()];
    for (int i = 0; i < arrayOfLabel.length; i++) {
      arrayOfLabel[i] = ((LabelNode)this.labels.get(i)).getLabel();
    }
    paramMethodVisitor.visitTableSwitchInsn(this.min, this.max, this.dflt.getLabel(), arrayOfLabel);
    acceptAnnotations(paramMethodVisitor);
  }
  
  public AbstractInsnNode clone(Map paramMap)
  {
    return new TableSwitchInsnNode(this.min, this.max, clone(this.dflt, paramMap), clone(this.labels, paramMap)).cloneAnnotations(this);
  }
}
