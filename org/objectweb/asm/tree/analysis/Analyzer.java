package org.objectweb.asm.tree.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

public class Analyzer
  implements Opcodes
{
  private final Interpreter interpreter;
  private int n;
  private InsnList insns;
  private List[] handlers;
  private Frame[] frames;
  private Subroutine[] subroutines;
  private boolean[] queued;
  private int[] queue;
  private int top;
  
  public Analyzer(Interpreter paramInterpreter)
  {
    this.interpreter = paramInterpreter;
  }
  
  public Frame[] analyze(String paramString, MethodNode paramMethodNode)
    throws AnalyzerException
  {
    if ((paramMethodNode.access & 0x500) != 0)
    {
      this.frames = ((Frame[])new Frame[0]);
      return this.frames;
    }
    this.n = paramMethodNode.instructions.size();
    this.insns = paramMethodNode.instructions;
    this.handlers = ((List[])new List[this.n]);
    this.frames = ((Frame[])new Frame[this.n]);
    this.subroutines = new Subroutine[this.n];
    this.queued = new boolean[this.n];
    this.queue = new int[this.n];
    this.top = 0;
    for (int i = 0; i < paramMethodNode.tryCatchBlocks.size(); i++)
    {
      localObject1 = (TryCatchBlockNode)paramMethodNode.tryCatchBlocks.get(i);
      int j = this.insns.indexOf(((TryCatchBlockNode)localObject1).start);
      int k = this.insns.indexOf(((TryCatchBlockNode)localObject1).end);
      for (int i1 = j; i1 < k; i1++)
      {
        localObject3 = this.handlers[i1];
        if (localObject3 == null)
        {
          localObject3 = new ArrayList();
          this.handlers[i1] = localObject3;
        }
        ((List)localObject3).add(localObject1);
      }
    }
    Subroutine localSubroutine1 = new Subroutine(null, paramMethodNode.maxLocals, null);
    Object localObject1 = new ArrayList();
    HashMap localHashMap = new HashMap();
    findSubroutine(0, localSubroutine1, (List)localObject1);
    while (!((List)localObject1).isEmpty())
    {
      JumpInsnNode localJumpInsnNode1 = (JumpInsnNode)((List)localObject1).remove(0);
      localObject2 = (Subroutine)localHashMap.get(localJumpInsnNode1.label);
      if (localObject2 == null)
      {
        localObject2 = new Subroutine(localJumpInsnNode1.label, paramMethodNode.maxLocals, localJumpInsnNode1);
        localHashMap.put(localJumpInsnNode1.label, localObject2);
        findSubroutine(this.insns.indexOf(localJumpInsnNode1.label), (Subroutine)localObject2, (List)localObject1);
      }
      else
      {
        ((Subroutine)localObject2).callers.add(localJumpInsnNode1);
      }
    }
    for (int m = 0; m < this.n; m++) {
      if ((this.subroutines[m] != null) && (this.subroutines[m].start == null)) {
        this.subroutines[m] = null;
      }
    }
    Frame localFrame1 = newFrame(paramMethodNode.maxLocals, paramMethodNode.maxStack);
    Object localObject2 = newFrame(paramMethodNode.maxLocals, paramMethodNode.maxStack);
    localFrame1.setReturn(this.interpreter.newValue(Type.getReturnType(paramMethodNode.desc)));
    Object localObject3 = Type.getArgumentTypes(paramMethodNode.desc);
    int i2 = 0;
    if ((paramMethodNode.access & 0x8) == 0)
    {
      Type localType = Type.getObjectType(paramString);
      localFrame1.setLocal(i2++, this.interpreter.newValue(localType));
    }
    for (int i3 = 0; i3 < localObject3.length; i3++)
    {
      localFrame1.setLocal(i2++, this.interpreter.newValue(localObject3[i3]));
      if (localObject3[i3].getSize() == 2) {
        localFrame1.setLocal(i2++, this.interpreter.newValue(null));
      }
    }
    while (i2 < paramMethodNode.maxLocals) {
      localFrame1.setLocal(i2++, this.interpreter.newValue(null));
    }
    merge(0, localFrame1, null);
    init(paramString, paramMethodNode);
    while (this.top > 0)
    {
      i3 = this.queue[(--this.top)];
      Frame localFrame2 = this.frames[i3];
      Subroutine localSubroutine2 = this.subroutines[i3];
      this.queued[i3] = false;
      AbstractInsnNode localAbstractInsnNode = null;
      try
      {
        localAbstractInsnNode = paramMethodNode.instructions.get(i3);
        int i4 = localAbstractInsnNode.getOpcode();
        int i5 = localAbstractInsnNode.getType();
        Object localObject5;
        if ((i5 == 8) || (i5 == 15) || (i5 == 14))
        {
          merge(i3 + 1, localFrame2, localSubroutine2);
          newControlFlowEdge(i3, i3 + 1);
        }
        else
        {
          localFrame1.init(localFrame2).execute(localAbstractInsnNode, this.interpreter);
          localSubroutine2 = localSubroutine2 == null ? null : localSubroutine2.copy();
          Object localObject4;
          int i7;
          if ((localAbstractInsnNode instanceof JumpInsnNode))
          {
            localObject4 = (JumpInsnNode)localAbstractInsnNode;
            if ((i4 != 167) && (i4 != 168))
            {
              merge(i3 + 1, localFrame1, localSubroutine2);
              newControlFlowEdge(i3, i3 + 1);
            }
            i7 = this.insns.indexOf(((JumpInsnNode)localObject4).label);
            if (i4 == 168) {
              merge(i7, localFrame1, new Subroutine(((JumpInsnNode)localObject4).label, paramMethodNode.maxLocals, (JumpInsnNode)localObject4));
            } else {
              merge(i7, localFrame1, localSubroutine2);
            }
            newControlFlowEdge(i3, i7);
          }
          else
          {
            int i9;
            if ((localAbstractInsnNode instanceof LookupSwitchInsnNode))
            {
              localObject4 = (LookupSwitchInsnNode)localAbstractInsnNode;
              i7 = this.insns.indexOf(((LookupSwitchInsnNode)localObject4).dflt);
              merge(i7, localFrame1, localSubroutine2);
              newControlFlowEdge(i3, i7);
              for (i9 = 0; i9 < ((LookupSwitchInsnNode)localObject4).labels.size(); i9++)
              {
                localObject5 = (LabelNode)((LookupSwitchInsnNode)localObject4).labels.get(i9);
                i7 = this.insns.indexOf((AbstractInsnNode)localObject5);
                merge(i7, localFrame1, localSubroutine2);
                newControlFlowEdge(i3, i7);
              }
            }
            else if ((localAbstractInsnNode instanceof TableSwitchInsnNode))
            {
              localObject4 = (TableSwitchInsnNode)localAbstractInsnNode;
              i7 = this.insns.indexOf(((TableSwitchInsnNode)localObject4).dflt);
              merge(i7, localFrame1, localSubroutine2);
              newControlFlowEdge(i3, i7);
              for (i9 = 0; i9 < ((TableSwitchInsnNode)localObject4).labels.size(); i9++)
              {
                localObject5 = (LabelNode)((TableSwitchInsnNode)localObject4).labels.get(i9);
                i7 = this.insns.indexOf((AbstractInsnNode)localObject5);
                merge(i7, localFrame1, localSubroutine2);
                newControlFlowEdge(i3, i7);
              }
            }
            else
            {
              int i6;
              if (i4 == 169)
              {
                if (localSubroutine2 == null) {
                  throw new AnalyzerException(localAbstractInsnNode, "RET instruction outside of a sub routine");
                }
                for (i6 = 0; i6 < localSubroutine2.callers.size(); i6++)
                {
                  JumpInsnNode localJumpInsnNode2 = (JumpInsnNode)localSubroutine2.callers.get(i6);
                  i9 = this.insns.indexOf(localJumpInsnNode2);
                  if (this.frames[i9] != null)
                  {
                    merge(i9 + 1, this.frames[i9], localFrame1, this.subroutines[i9], localSubroutine2.access);
                    newControlFlowEdge(i3, i9 + 1);
                  }
                }
              }
              else if ((i4 != 191) && ((i4 < 172) || (i4 > 177)))
              {
                if (localSubroutine2 != null) {
                  if ((localAbstractInsnNode instanceof VarInsnNode))
                  {
                    i6 = ((VarInsnNode)localAbstractInsnNode).var;
                    localSubroutine2.access[i6] = true;
                    if ((i4 == 22) || (i4 == 24) || (i4 == 55) || (i4 == 57)) {
                      localSubroutine2.access[(i6 + 1)] = true;
                    }
                  }
                  else if ((localAbstractInsnNode instanceof IincInsnNode))
                  {
                    i6 = ((IincInsnNode)localAbstractInsnNode).var;
                    localSubroutine2.access[i6] = true;
                  }
                }
                merge(i3 + 1, localFrame1, localSubroutine2);
                newControlFlowEdge(i3, i3 + 1);
              }
            }
          }
        }
        List localList = this.handlers[i3];
        if (localList != null) {
          for (int i8 = 0; i8 < localList.size(); i8++)
          {
            TryCatchBlockNode localTryCatchBlockNode = (TryCatchBlockNode)localList.get(i8);
            if (localTryCatchBlockNode.type == null) {
              localObject5 = Type.getObjectType("java/lang/Throwable");
            } else {
              localObject5 = Type.getObjectType(localTryCatchBlockNode.type);
            }
            int i10 = this.insns.indexOf(localTryCatchBlockNode.handler);
            if (newControlFlowExceptionEdge(i3, localTryCatchBlockNode))
            {
              ((Frame)localObject2).init(localFrame2);
              ((Frame)localObject2).clearStack();
              ((Frame)localObject2).push(this.interpreter.newValue((Type)localObject5));
              merge(i10, (Frame)localObject2, localSubroutine2);
            }
          }
        }
      }
      catch (AnalyzerException localAnalyzerException)
      {
        throw new AnalyzerException(localAnalyzerException.node, "Error at instruction " + i3 + ": " + localAnalyzerException.getMessage(), localAnalyzerException);
      }
      catch (Exception localException)
      {
        throw new AnalyzerException(localAbstractInsnNode, "Error at instruction " + i3 + ": " + localException.getMessage(), localException);
      }
    }
    return this.frames;
  }
  
  private void findSubroutine(int paramInt, Subroutine paramSubroutine, List paramList)
    throws AnalyzerException
  {
    for (;;)
    {
      if ((paramInt < 0) || (paramInt >= this.n)) {
        throw new AnalyzerException(null, "Execution can fall off end of the code");
      }
      if (this.subroutines[paramInt] != null) {
        return;
      }
      this.subroutines[paramInt] = paramSubroutine.copy();
      AbstractInsnNode localAbstractInsnNode = this.insns.get(paramInt);
      int i;
      Object localObject2;
      if ((localAbstractInsnNode instanceof JumpInsnNode))
      {
        if (localAbstractInsnNode.getOpcode() == 168)
        {
          paramList.add(localAbstractInsnNode);
        }
        else
        {
          localObject1 = (JumpInsnNode)localAbstractInsnNode;
          findSubroutine(this.insns.indexOf(((JumpInsnNode)localObject1).label), paramSubroutine, paramList);
        }
      }
      else if ((localAbstractInsnNode instanceof TableSwitchInsnNode))
      {
        localObject1 = (TableSwitchInsnNode)localAbstractInsnNode;
        findSubroutine(this.insns.indexOf(((TableSwitchInsnNode)localObject1).dflt), paramSubroutine, paramList);
        for (i = ((TableSwitchInsnNode)localObject1).labels.size() - 1; i >= 0; i--)
        {
          localObject2 = (LabelNode)((TableSwitchInsnNode)localObject1).labels.get(i);
          findSubroutine(this.insns.indexOf((AbstractInsnNode)localObject2), paramSubroutine, paramList);
        }
      }
      else if ((localAbstractInsnNode instanceof LookupSwitchInsnNode))
      {
        localObject1 = (LookupSwitchInsnNode)localAbstractInsnNode;
        findSubroutine(this.insns.indexOf(((LookupSwitchInsnNode)localObject1).dflt), paramSubroutine, paramList);
        for (i = ((LookupSwitchInsnNode)localObject1).labels.size() - 1; i >= 0; i--)
        {
          localObject2 = (LabelNode)((LookupSwitchInsnNode)localObject1).labels.get(i);
          findSubroutine(this.insns.indexOf((AbstractInsnNode)localObject2), paramSubroutine, paramList);
        }
      }
      Object localObject1 = this.handlers[paramInt];
      if (localObject1 != null) {
        for (i = 0; i < ((List)localObject1).size(); i++)
        {
          localObject2 = (TryCatchBlockNode)((List)localObject1).get(i);
          findSubroutine(this.insns.indexOf(((TryCatchBlockNode)localObject2).handler), paramSubroutine, paramList);
        }
      }
      switch (localAbstractInsnNode.getOpcode())
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
    }
  }
  
  public Frame[] getFrames()
  {
    return this.frames;
  }
  
  public List getHandlers(int paramInt)
  {
    return this.handlers[paramInt];
  }
  
  protected void init(String paramString, MethodNode paramMethodNode)
    throws AnalyzerException
  {}
  
  protected Frame newFrame(int paramInt1, int paramInt2)
  {
    return new Frame(paramInt1, paramInt2);
  }
  
  protected Frame newFrame(Frame paramFrame)
  {
    return new Frame(paramFrame);
  }
  
  protected void newControlFlowEdge(int paramInt1, int paramInt2) {}
  
  protected boolean newControlFlowExceptionEdge(int paramInt1, int paramInt2)
  {
    return true;
  }
  
  protected boolean newControlFlowExceptionEdge(int paramInt, TryCatchBlockNode paramTryCatchBlockNode)
  {
    return newControlFlowExceptionEdge(paramInt, this.insns.indexOf(paramTryCatchBlockNode.handler));
  }
  
  private void merge(int paramInt, Frame paramFrame, Subroutine paramSubroutine)
    throws AnalyzerException
  {
    Frame localFrame = this.frames[paramInt];
    Subroutine localSubroutine = this.subroutines[paramInt];
    boolean bool;
    if (localFrame == null)
    {
      this.frames[paramInt] = newFrame(paramFrame);
      bool = true;
    }
    else
    {
      bool = localFrame.merge(paramFrame, this.interpreter);
    }
    if (localSubroutine == null)
    {
      if (paramSubroutine != null)
      {
        this.subroutines[paramInt] = paramSubroutine.copy();
        bool = true;
      }
    }
    else if (paramSubroutine != null) {
      bool |= localSubroutine.merge(paramSubroutine);
    }
    if ((bool) && (this.queued[paramInt] == 0))
    {
      this.queued[paramInt] = true;
      this.queue[(this.top++)] = paramInt;
    }
  }
  
  private void merge(int paramInt, Frame paramFrame1, Frame paramFrame2, Subroutine paramSubroutine, boolean[] paramArrayOfBoolean)
    throws AnalyzerException
  {
    Frame localFrame = this.frames[paramInt];
    Subroutine localSubroutine = this.subroutines[paramInt];
    paramFrame2.merge(paramFrame1, paramArrayOfBoolean);
    boolean bool;
    if (localFrame == null)
    {
      this.frames[paramInt] = newFrame(paramFrame2);
      bool = true;
    }
    else
    {
      bool = localFrame.merge(paramFrame2, this.interpreter);
    }
    if ((localSubroutine != null) && (paramSubroutine != null)) {
      bool |= localSubroutine.merge(paramSubroutine);
    }
    if ((bool) && (this.queued[paramInt] == 0))
    {
      this.queued[paramInt] = true;
      this.queue[(this.top++)] = paramInt;
    }
  }
}
