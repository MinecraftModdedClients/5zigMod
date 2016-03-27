package org.objectweb.asm.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;

public class LocalVariableAnnotationNode
  extends TypeAnnotationNode
{
  public List start;
  public List end;
  public List index;
  
  public LocalVariableAnnotationNode(int paramInt, TypePath paramTypePath, LabelNode[] paramArrayOfLabelNode1, LabelNode[] paramArrayOfLabelNode2, int[] paramArrayOfInt, String paramString)
  {
    this(327680, paramInt, paramTypePath, paramArrayOfLabelNode1, paramArrayOfLabelNode2, paramArrayOfInt, paramString);
  }
  
  public LocalVariableAnnotationNode(int paramInt1, int paramInt2, TypePath paramTypePath, LabelNode[] paramArrayOfLabelNode1, LabelNode[] paramArrayOfLabelNode2, int[] paramArrayOfInt, String paramString)
  {
    super(paramInt1, paramInt2, paramTypePath, paramString);
    this.start = new ArrayList(paramArrayOfLabelNode1.length);
    this.start.addAll(Arrays.asList(paramArrayOfLabelNode1));
    this.end = new ArrayList(paramArrayOfLabelNode2.length);
    this.end.addAll(Arrays.asList(paramArrayOfLabelNode2));
    this.index = new ArrayList(paramArrayOfInt.length);
    int[] arrayOfInt = paramArrayOfInt;
    int i = arrayOfInt.length;
    for (int j = 0; j < i; j++)
    {
      int k = arrayOfInt[j];
      this.index.add(Integer.valueOf(k));
    }
  }
  
  public void accept(MethodVisitor paramMethodVisitor, boolean paramBoolean)
  {
    Label[] arrayOfLabel1 = new Label[this.start.size()];
    Label[] arrayOfLabel2 = new Label[this.end.size()];
    int[] arrayOfInt = new int[this.index.size()];
    for (int i = 0; i < arrayOfLabel1.length; i++)
    {
      arrayOfLabel1[i] = ((LabelNode)this.start.get(i)).getLabel();
      arrayOfLabel2[i] = ((LabelNode)this.end.get(i)).getLabel();
      arrayOfInt[i] = ((Integer)this.index.get(i)).intValue();
    }
    accept(paramMethodVisitor.visitLocalVariableAnnotation(this.typeRef, this.typePath, arrayOfLabel1, arrayOfLabel2, arrayOfInt, this.desc, true));
  }
}
