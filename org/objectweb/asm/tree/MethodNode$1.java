package org.objectweb.asm.tree;

import java.util.ArrayList;

class MethodNode$1
  extends ArrayList
{
  final MethodNode this$0;
  
  MethodNode$1(MethodNode paramMethodNode, int paramInt)
  {
    super(paramInt);
  }
  
  public boolean add(Object paramObject)
  {
    this.this$0.annotationDefault = paramObject;
    return super.add(paramObject);
  }
}
