package org.objectweb.asm.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.MethodVisitor;

public class FrameNode
  extends AbstractInsnNode
{
  public int type;
  public List local;
  public List stack;
  
  private FrameNode()
  {
    super(-1);
  }
  
  public FrameNode(int paramInt1, int paramInt2, Object[] paramArrayOfObject1, int paramInt3, Object[] paramArrayOfObject2)
  {
    super(-1);
    this.type = paramInt1;
    switch (paramInt1)
    {
    case -1: 
    case 0: 
      this.local = asList(paramInt2, paramArrayOfObject1);
      this.stack = asList(paramInt3, paramArrayOfObject2);
      break;
    case 1: 
      this.local = asList(paramInt2, paramArrayOfObject1);
      break;
    case 2: 
      this.local = Arrays.asList(new Object[paramInt2]);
      break;
    case 3: 
      break;
    case 4: 
      this.stack = asList(1, paramArrayOfObject2);
    }
  }
  
  public int getType()
  {
    return 14;
  }
  
  public void accept(MethodVisitor paramMethodVisitor)
  {
    switch (this.type)
    {
    case -1: 
    case 0: 
      paramMethodVisitor.visitFrame(this.type, this.local.size(), asArray(this.local), this.stack.size(), asArray(this.stack));
      break;
    case 1: 
      paramMethodVisitor.visitFrame(this.type, this.local.size(), asArray(this.local), 0, null);
      break;
    case 2: 
      paramMethodVisitor.visitFrame(this.type, this.local.size(), null, 0, null);
      break;
    case 3: 
      paramMethodVisitor.visitFrame(this.type, 0, null, 0, null);
      break;
    case 4: 
      paramMethodVisitor.visitFrame(this.type, 0, null, 1, asArray(this.stack));
    }
  }
  
  public AbstractInsnNode clone(Map paramMap)
  {
    FrameNode localFrameNode = new FrameNode();
    localFrameNode.type = this.type;
    int i;
    Object localObject;
    if (this.local != null)
    {
      localFrameNode.local = new ArrayList();
      for (i = 0; i < this.local.size(); i++)
      {
        localObject = this.local.get(i);
        if ((localObject instanceof LabelNode)) {
          localObject = paramMap.get(localObject);
        }
        localFrameNode.local.add(localObject);
      }
    }
    if (this.stack != null)
    {
      localFrameNode.stack = new ArrayList();
      for (i = 0; i < this.stack.size(); i++)
      {
        localObject = this.stack.get(i);
        if ((localObject instanceof LabelNode)) {
          localObject = paramMap.get(localObject);
        }
        localFrameNode.stack.add(localObject);
      }
    }
    return localFrameNode;
  }
  
  private static List asList(int paramInt, Object[] paramArrayOfObject)
  {
    return Arrays.asList(paramArrayOfObject).subList(0, paramInt);
  }
  
  private static Object[] asArray(List paramList)
  {
    Object[] arrayOfObject = new Object[paramList.size()];
    for (int i = 0; i < arrayOfObject.length; i++)
    {
      Object localObject = paramList.get(i);
      if ((localObject instanceof LabelNode)) {
        localObject = ((LabelNode)localObject).getLabel();
      }
      arrayOfObject[i] = localObject;
    }
    return arrayOfObject;
  }
}
