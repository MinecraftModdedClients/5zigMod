package org.objectweb.asm.commons;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class JSRInlinerAdapter
  extends MethodNode
  implements Opcodes
{
  private final Map subroutineHeads = new HashMap();
  private final BitSet mainSubroutine = new BitSet();
  final BitSet dualCitizens = new BitSet();
  static Class class$org$objectweb$asm$commons$JSRInlinerAdapter = class$("org.objectweb.asm.commons.JSRInlinerAdapter");
  
  public JSRInlinerAdapter(MethodVisitor paramMethodVisitor, int paramInt, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    this(327680, paramMethodVisitor, paramInt, paramString1, paramString2, paramString3, paramArrayOfString);
    if (getClass() != class$org$objectweb$asm$commons$JSRInlinerAdapter) {
      throw new IllegalStateException();
    }
  }
  
  protected JSRInlinerAdapter(int paramInt1, MethodVisitor paramMethodVisitor, int paramInt2, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    super(paramInt1, paramInt2, paramString1, paramString2, paramString3, paramArrayOfString);
    this.mv = paramMethodVisitor;
  }
  
  public void visitJumpInsn(int paramInt, Label paramLabel)
  {
    super.visitJumpInsn(paramInt, paramLabel);
    LabelNode localLabelNode = ((JumpInsnNode)this.instructions.getLast()).label;
    if ((paramInt == 168) && (!this.subroutineHeads.containsKey(localLabelNode))) {
      this.subroutineHeads.put(localLabelNode, new BitSet());
    }
  }
  
  public void visitEnd()
  {
    if (!this.subroutineHeads.isEmpty())
    {
      markSubroutines();
      emitCode();
    }
    if (this.mv != null) {
      accept(this.mv);
    }
  }
  
  private void markSubroutines()
  {
    BitSet localBitSet1 = new BitSet();
    markSubroutineWalk(this.mainSubroutine, 0, localBitSet1);
    Iterator localIterator = this.subroutineHeads.entrySet().iterator();
    while (localIterator.hasNext())
    {
      Map.Entry localEntry = (Map.Entry)localIterator.next();
      LabelNode localLabelNode = (LabelNode)localEntry.getKey();
      BitSet localBitSet2 = (BitSet)localEntry.getValue();
      int i = this.instructions.indexOf(localLabelNode);
      markSubroutineWalk(localBitSet2, i, localBitSet1);
    }
  }
  
  private void markSubroutineWalk(BitSet paramBitSet1, int paramInt, BitSet paramBitSet2)
  {
    markSubroutineWalkDFS(paramBitSet1, paramInt, paramBitSet2);
    int i = 1;
    while (i != 0)
    {
      i = 0;
      Iterator localIterator = this.tryCatchBlocks.iterator();
      while (localIterator.hasNext())
      {
        TryCatchBlockNode localTryCatchBlockNode = (TryCatchBlockNode)localIterator.next();
        int j = this.instructions.indexOf(localTryCatchBlockNode.handler);
        if (!paramBitSet1.get(j))
        {
          int k = this.instructions.indexOf(localTryCatchBlockNode.start);
          int m = this.instructions.indexOf(localTryCatchBlockNode.end);
          int n = paramBitSet1.nextSetBit(k);
          if ((n != -1) && (n < m))
          {
            markSubroutineWalkDFS(paramBitSet1, j, paramBitSet2);
            i = 1;
          }
        }
      }
    }
  }
  
  private void markSubroutineWalkDFS(BitSet paramBitSet1, int paramInt, BitSet paramBitSet2)
  {
    for (;;)
    {
      AbstractInsnNode localAbstractInsnNode = this.instructions.get(paramInt);
      if (paramBitSet1.get(paramInt)) {
        return;
      }
      paramBitSet1.set(paramInt);
      if (paramBitSet2.get(paramInt)) {
        this.dualCitizens.set(paramInt);
      }
      paramBitSet2.set(paramInt);
      Object localObject;
      int i;
      if ((localAbstractInsnNode.getType() == 7) && (localAbstractInsnNode.getOpcode() != 168))
      {
        localObject = (JumpInsnNode)localAbstractInsnNode;
        i = this.instructions.indexOf(((JumpInsnNode)localObject).label);
        markSubroutineWalkDFS(paramBitSet1, i, paramBitSet2);
      }
      int j;
      LabelNode localLabelNode;
      if (localAbstractInsnNode.getType() == 11)
      {
        localObject = (TableSwitchInsnNode)localAbstractInsnNode;
        i = this.instructions.indexOf(((TableSwitchInsnNode)localObject).dflt);
        markSubroutineWalkDFS(paramBitSet1, i, paramBitSet2);
        for (j = ((TableSwitchInsnNode)localObject).labels.size() - 1; j >= 0; j--)
        {
          localLabelNode = (LabelNode)((TableSwitchInsnNode)localObject).labels.get(j);
          i = this.instructions.indexOf(localLabelNode);
          markSubroutineWalkDFS(paramBitSet1, i, paramBitSet2);
        }
      }
      if (localAbstractInsnNode.getType() == 12)
      {
        localObject = (LookupSwitchInsnNode)localAbstractInsnNode;
        i = this.instructions.indexOf(((LookupSwitchInsnNode)localObject).dflt);
        markSubroutineWalkDFS(paramBitSet1, i, paramBitSet2);
        for (j = ((LookupSwitchInsnNode)localObject).labels.size() - 1; j >= 0; j--)
        {
          localLabelNode = (LabelNode)((LookupSwitchInsnNode)localObject).labels.get(j);
          i = this.instructions.indexOf(localLabelNode);
          markSubroutineWalkDFS(paramBitSet1, i, paramBitSet2);
        }
      }
      switch (this.instructions.get(paramInt).getOpcode())
      {
      case 167: 
      case 169: 
      case 170: 
      case 171: 
      case 172: 
      case 173: 
      case 174: 
      case 175: 
      case 176: 
      case 177: 
      case 191: 
        return;
      }
      paramInt++;
      if (paramInt >= this.instructions.size()) {
        return;
      }
    }
  }
  
  private void emitCode()
  {
    LinkedList localLinkedList = new LinkedList();
    localLinkedList.add(new JSRInlinerAdapter.Instantiation(this, null, this.mainSubroutine));
    InsnList localInsnList = new InsnList();
    ArrayList localArrayList1 = new ArrayList();
    ArrayList localArrayList2 = new ArrayList();
    while (!localLinkedList.isEmpty())
    {
      JSRInlinerAdapter.Instantiation localInstantiation = (JSRInlinerAdapter.Instantiation)localLinkedList.removeFirst();
      emitSubroutine(localInstantiation, localLinkedList, localInsnList, localArrayList1, localArrayList2);
    }
    this.instructions = localInsnList;
    this.tryCatchBlocks = localArrayList1;
    this.localVariables = localArrayList2;
  }
  
  private void emitSubroutine(JSRInlinerAdapter.Instantiation paramInstantiation, List paramList1, InsnList paramInsnList, List paramList2, List paramList3)
  {
    Object localObject1 = null;
    int i = 0;
    int j = this.instructions.size();
    Object localObject3;
    Object localObject4;
    LabelNode localLabelNode1;
    while (i < j)
    {
      localObject3 = this.instructions.get(i);
      localObject4 = paramInstantiation.findOwner(i);
      Object localObject5;
      if (((AbstractInsnNode)localObject3).getType() == 8)
      {
        localLabelNode1 = (LabelNode)localObject3;
        localObject5 = paramInstantiation.rangeLabel(localLabelNode1);
        if (localObject5 != localObject1)
        {
          paramInsnList.add((AbstractInsnNode)localObject5);
          localObject1 = localObject5;
        }
      }
      else if (localObject4 == paramInstantiation)
      {
        if (((AbstractInsnNode)localObject3).getOpcode() == 169)
        {
          localLabelNode1 = null;
          for (localObject5 = paramInstantiation; localObject5 != null; localObject5 = ((JSRInlinerAdapter.Instantiation)localObject5).previous) {
            if (((JSRInlinerAdapter.Instantiation)localObject5).subroutine.get(i)) {
              localLabelNode1 = ((JSRInlinerAdapter.Instantiation)localObject5).returnLabel;
            }
          }
          if (localLabelNode1 == null) {
            throw new RuntimeException("Instruction #" + i + " is a RET not owned by any subroutine");
          }
          paramInsnList.add(new JumpInsnNode(167, localLabelNode1));
        }
        else if (((AbstractInsnNode)localObject3).getOpcode() == 168)
        {
          localLabelNode1 = ((JumpInsnNode)localObject3).label;
          localObject5 = (BitSet)this.subroutineHeads.get(localLabelNode1);
          JSRInlinerAdapter.Instantiation localInstantiation = new JSRInlinerAdapter.Instantiation(this, paramInstantiation, (BitSet)localObject5);
          LabelNode localLabelNode2 = localInstantiation.gotoLabel(localLabelNode1);
          paramInsnList.add(new InsnNode(1));
          paramInsnList.add(new JumpInsnNode(167, localLabelNode2));
          paramInsnList.add(localInstantiation.returnLabel);
          paramList1.add(localInstantiation);
        }
        else
        {
          paramInsnList.add(((AbstractInsnNode)localObject3).clone(paramInstantiation));
        }
      }
      i++;
    }
    Iterator localIterator = this.tryCatchBlocks.iterator();
    Object localObject2;
    while (localIterator.hasNext())
    {
      localObject2 = (TryCatchBlockNode)localIterator.next();
      localObject3 = paramInstantiation.rangeLabel(((TryCatchBlockNode)localObject2).start);
      localObject4 = paramInstantiation.rangeLabel(((TryCatchBlockNode)localObject2).end);
      if (localObject3 != localObject4)
      {
        localLabelNode1 = paramInstantiation.gotoLabel(((TryCatchBlockNode)localObject2).handler);
        if ((localObject3 == null) || (localObject4 == null) || (localLabelNode1 == null)) {
          throw new RuntimeException("Internal error!");
        }
        paramList2.add(new TryCatchBlockNode((LabelNode)localObject3, (LabelNode)localObject4, localLabelNode1, ((TryCatchBlockNode)localObject2).type));
      }
    }
    localIterator = this.localVariables.iterator();
    while (localIterator.hasNext())
    {
      localObject2 = (LocalVariableNode)localIterator.next();
      localObject3 = paramInstantiation.rangeLabel(((LocalVariableNode)localObject2).start);
      localObject4 = paramInstantiation.rangeLabel(((LocalVariableNode)localObject2).end);
      if (localObject3 != localObject4) {
        paramList3.add(new LocalVariableNode(((LocalVariableNode)localObject2).name, ((LocalVariableNode)localObject2).desc, ((LocalVariableNode)localObject2).signature, (LabelNode)localObject3, (LabelNode)localObject4, ((LocalVariableNode)localObject2).index));
      }
    }
  }
  
  private static void log(String paramString)
  {
    System.err.println(paramString);
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
