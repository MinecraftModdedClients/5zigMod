package org.objectweb.asm.tree.analysis;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

class Subroutine
{
  LabelNode start;
  boolean[] access;
  List callers;
  
  private Subroutine() {}
  
  Subroutine(LabelNode paramLabelNode, int paramInt, JumpInsnNode paramJumpInsnNode)
  {
    this.start = paramLabelNode;
    this.access = new boolean[paramInt];
    this.callers = new ArrayList();
    this.callers.add(paramJumpInsnNode);
  }
  
  public Subroutine copy()
  {
    Subroutine localSubroutine = new Subroutine();
    localSubroutine.start = this.start;
    localSubroutine.access = new boolean[this.access.length];
    System.arraycopy(this.access, 0, localSubroutine.access, 0, this.access.length);
    localSubroutine.callers = new ArrayList(this.callers);
    return localSubroutine;
  }
  
  public boolean merge(Subroutine paramSubroutine)
    throws AnalyzerException
  {
    boolean bool = false;
    for (int i = 0; i < this.access.length; i++) {
      if ((paramSubroutine.access[i] != 0) && (this.access[i] == 0))
      {
        this.access[i] = true;
        bool = true;
      }
    }
    if (paramSubroutine.start == this.start) {
      for (i = 0; i < paramSubroutine.callers.size(); i++)
      {
        JumpInsnNode localJumpInsnNode = (JumpInsnNode)paramSubroutine.callers.get(i);
        if (!this.callers.contains(localJumpInsnNode))
        {
          this.callers.add(localJumpInsnNode);
          bool = true;
        }
      }
    }
    return bool;
  }
}
