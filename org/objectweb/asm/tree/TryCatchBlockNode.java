package org.objectweb.asm.tree;

import java.util.Iterator;
import java.util.List;
import org.objectweb.asm.MethodVisitor;

public class TryCatchBlockNode
{
  public LabelNode start;
  public LabelNode end;
  public LabelNode handler;
  public String type;
  public List visibleTypeAnnotations;
  public List invisibleTypeAnnotations;
  
  public TryCatchBlockNode(LabelNode paramLabelNode1, LabelNode paramLabelNode2, LabelNode paramLabelNode3, String paramString)
  {
    this.start = paramLabelNode1;
    this.end = paramLabelNode2;
    this.handler = paramLabelNode3;
    this.type = paramString;
  }
  
  public void updateIndex(int paramInt)
  {
    int i = 0x42000000 | paramInt << 8;
    Iterator localIterator;
    TypeAnnotationNode localTypeAnnotationNode;
    if (this.visibleTypeAnnotations != null)
    {
      localIterator = this.visibleTypeAnnotations.iterator();
      while (localIterator.hasNext())
      {
        localTypeAnnotationNode = (TypeAnnotationNode)localIterator.next();
        localTypeAnnotationNode.typeRef = i;
      }
    }
    if (this.invisibleTypeAnnotations != null)
    {
      localIterator = this.invisibleTypeAnnotations.iterator();
      while (localIterator.hasNext())
      {
        localTypeAnnotationNode = (TypeAnnotationNode)localIterator.next();
        localTypeAnnotationNode.typeRef = i;
      }
    }
  }
  
  public void accept(MethodVisitor paramMethodVisitor)
  {
    paramMethodVisitor.visitTryCatchBlock(this.start.getLabel(), this.end.getLabel(), this.handler == null ? null : this.handler.getLabel(), this.type);
    int i = this.visibleTypeAnnotations == null ? 0 : this.visibleTypeAnnotations.size();
    TypeAnnotationNode localTypeAnnotationNode;
    for (int j = 0; j < i; j++)
    {
      localTypeAnnotationNode = (TypeAnnotationNode)this.visibleTypeAnnotations.get(j);
      localTypeAnnotationNode.accept(paramMethodVisitor.visitTryCatchAnnotation(localTypeAnnotationNode.typeRef, localTypeAnnotationNode.typePath, localTypeAnnotationNode.desc, true));
    }
    i = this.invisibleTypeAnnotations == null ? 0 : this.invisibleTypeAnnotations.size();
    for (j = 0; j < i; j++)
    {
      localTypeAnnotationNode = (TypeAnnotationNode)this.invisibleTypeAnnotations.get(j);
      localTypeAnnotationNode.accept(paramMethodVisitor.visitTryCatchAnnotation(localTypeAnnotationNode.typeRef, localTypeAnnotationNode.typePath, localTypeAnnotationNode.desc, false));
    }
  }
}
