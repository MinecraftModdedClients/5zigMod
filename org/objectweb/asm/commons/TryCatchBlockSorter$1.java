package org.objectweb.asm.commons;

import java.util.Comparator;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

class TryCatchBlockSorter$1
  implements Comparator
{
  final TryCatchBlockSorter this$0;
  
  TryCatchBlockSorter$1(TryCatchBlockSorter paramTryCatchBlockSorter) {}
  
  public int compare(TryCatchBlockNode paramTryCatchBlockNode1, TryCatchBlockNode paramTryCatchBlockNode2)
  {
    int i = blockLength(paramTryCatchBlockNode1);
    int j = blockLength(paramTryCatchBlockNode2);
    return i - j;
  }
  
  private int blockLength(TryCatchBlockNode paramTryCatchBlockNode)
  {
    int i = this.this$0.instructions.indexOf(paramTryCatchBlockNode.start);
    int j = this.this$0.instructions.indexOf(paramTryCatchBlockNode.end);
    return j - i;
  }
}
