package org.objectweb.asm.commons;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

class JSRInlinerAdapter$Instantiation
  extends AbstractMap
{
  final Instantiation previous;
  public final BitSet subroutine;
  public final Map rangeTable = new HashMap();
  public final LabelNode returnLabel;
  final JSRInlinerAdapter this$0;
  
  JSRInlinerAdapter$Instantiation(JSRInlinerAdapter paramJSRInlinerAdapter, Instantiation paramInstantiation, BitSet paramBitSet)
  {
    this.previous = paramInstantiation;
    this.subroutine = paramBitSet;
    for (Object localObject = paramInstantiation; localObject != null; localObject = ((Instantiation)localObject).previous) {
      if (((Instantiation)localObject).subroutine == paramBitSet) {
        throw new RuntimeException("Recursive invocation of " + paramBitSet);
      }
    }
    if (paramInstantiation != null) {
      this.returnLabel = new LabelNode();
    } else {
      this.returnLabel = null;
    }
    localObject = null;
    int i = 0;
    int j = paramJSRInlinerAdapter.instructions.size();
    while (i < j)
    {
      AbstractInsnNode localAbstractInsnNode = paramJSRInlinerAdapter.instructions.get(i);
      if (localAbstractInsnNode.getType() == 8)
      {
        LabelNode localLabelNode = (LabelNode)localAbstractInsnNode;
        if (localObject == null) {
          localObject = new LabelNode();
        }
        this.rangeTable.put(localLabelNode, localObject);
      }
      else if (findOwner(i) == this)
      {
        localObject = null;
      }
      i++;
    }
  }
  
  public Instantiation findOwner(int paramInt)
  {
    if (!this.subroutine.get(paramInt)) {
      return null;
    }
    if (!this.this$0.dualCitizens.get(paramInt)) {
      return this;
    }
    Object localObject = this;
    for (Instantiation localInstantiation = this.previous; localInstantiation != null; localInstantiation = localInstantiation.previous) {
      if (localInstantiation.subroutine.get(paramInt)) {
        localObject = localInstantiation;
      }
    }
    return (Instantiation)localObject;
  }
  
  public LabelNode gotoLabel(LabelNode paramLabelNode)
  {
    Instantiation localInstantiation = findOwner(this.this$0.instructions.indexOf(paramLabelNode));
    return (LabelNode)localInstantiation.rangeTable.get(paramLabelNode);
  }
  
  public LabelNode rangeLabel(LabelNode paramLabelNode)
  {
    return (LabelNode)this.rangeTable.get(paramLabelNode);
  }
  
  public Set entrySet()
  {
    return null;
  }
  
  public LabelNode get(Object paramObject)
  {
    return gotoLabel((LabelNode)paramObject);
  }
}
