package org.objectweb.asm.commons;

import java.util.Collections;
import java.util.List;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class TryCatchBlockSorter
  extends MethodNode
{
  public TryCatchBlockSorter(MethodVisitor paramMethodVisitor, int paramInt, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    this(327680, paramMethodVisitor, paramInt, paramString1, paramString2, paramString3, paramArrayOfString);
  }
  
  protected TryCatchBlockSorter(int paramInt1, MethodVisitor paramMethodVisitor, int paramInt2, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    super(paramInt1, paramInt2, paramString1, paramString2, paramString3, paramArrayOfString);
    this.mv = paramMethodVisitor;
  }
  
  public void visitEnd()
  {
    TryCatchBlockSorter.1 local1 = new TryCatchBlockSorter.1(this);
    Collections.sort(this.tryCatchBlocks, local1);
    for (int i = 0; i < this.tryCatchBlocks.size(); i++) {
      ((TryCatchBlockNode)this.tryCatchBlocks.get(i)).updateIndex(i);
    }
    if (this.mv != null) {
      accept(this.mv);
    }
  }
}
