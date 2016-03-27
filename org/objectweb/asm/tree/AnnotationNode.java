package org.objectweb.asm.tree;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;

public class AnnotationNode
  extends AnnotationVisitor
{
  public String desc;
  public List values;
  static Class class$org$objectweb$asm$tree$AnnotationNode = class$("org.objectweb.asm.tree.AnnotationNode");
  
  public AnnotationNode(String paramString)
  {
    this(327680, paramString);
    if (getClass() != class$org$objectweb$asm$tree$AnnotationNode) {
      throw new IllegalStateException();
    }
  }
  
  public AnnotationNode(int paramInt, String paramString)
  {
    super(paramInt);
    this.desc = paramString;
  }
  
  AnnotationNode(List paramList)
  {
    super(327680);
    this.values = paramList;
  }
  
  public void visit(String paramString, Object paramObject)
  {
    if (this.values == null) {
      this.values = new ArrayList(this.desc != null ? 2 : 1);
    }
    if (this.desc != null) {
      this.values.add(paramString);
    }
    this.values.add(paramObject);
  }
  
  public void visitEnum(String paramString1, String paramString2, String paramString3)
  {
    if (this.values == null) {
      this.values = new ArrayList(this.desc != null ? 2 : 1);
    }
    if (this.desc != null) {
      this.values.add(paramString1);
    }
    this.values.add(new String[] { paramString2, paramString3 });
  }
  
  public AnnotationVisitor visitAnnotation(String paramString1, String paramString2)
  {
    if (this.values == null) {
      this.values = new ArrayList(this.desc != null ? 2 : 1);
    }
    if (this.desc != null) {
      this.values.add(paramString1);
    }
    AnnotationNode localAnnotationNode = new AnnotationNode(paramString2);
    this.values.add(localAnnotationNode);
    return localAnnotationNode;
  }
  
  public AnnotationVisitor visitArray(String paramString)
  {
    if (this.values == null) {
      this.values = new ArrayList(this.desc != null ? 2 : 1);
    }
    if (this.desc != null) {
      this.values.add(paramString);
    }
    ArrayList localArrayList = new ArrayList();
    this.values.add(localArrayList);
    return new AnnotationNode(localArrayList);
  }
  
  public void visitEnd() {}
  
  public void check(int paramInt) {}
  
  public void accept(AnnotationVisitor paramAnnotationVisitor)
  {
    if (paramAnnotationVisitor != null)
    {
      if (this.values != null) {
        for (int i = 0; i < this.values.size(); i += 2)
        {
          String str = (String)this.values.get(i);
          Object localObject = this.values.get(i + 1);
          accept(paramAnnotationVisitor, str, localObject);
        }
      }
      paramAnnotationVisitor.visitEnd();
    }
  }
  
  static void accept(AnnotationVisitor paramAnnotationVisitor, String paramString, Object paramObject)
  {
    if (paramAnnotationVisitor != null)
    {
      Object localObject;
      if ((paramObject instanceof String[]))
      {
        localObject = (String[])paramObject;
        paramAnnotationVisitor.visitEnum(paramString, localObject[0], localObject[1]);
      }
      else if ((paramObject instanceof AnnotationNode))
      {
        localObject = (AnnotationNode)paramObject;
        ((AnnotationNode)localObject).accept(paramAnnotationVisitor.visitAnnotation(paramString, ((AnnotationNode)localObject).desc));
      }
      else if ((paramObject instanceof List))
      {
        localObject = paramAnnotationVisitor.visitArray(paramString);
        List localList = (List)paramObject;
        for (int i = 0; i < localList.size(); i++) {
          accept((AnnotationVisitor)localObject, null, localList.get(i));
        }
        ((AnnotationVisitor)localObject).visitEnd();
      }
      else
      {
        paramAnnotationVisitor.visit(paramString, paramObject);
      }
    }
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
