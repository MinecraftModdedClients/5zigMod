package org.objectweb.asm.tree;

import org.objectweb.asm.TypePath;

public class TypeAnnotationNode
  extends AnnotationNode
{
  public int typeRef;
  public TypePath typePath;
  static Class class$org$objectweb$asm$tree$TypeAnnotationNode = class$("org.objectweb.asm.tree.TypeAnnotationNode");
  
  public TypeAnnotationNode(int paramInt, TypePath paramTypePath, String paramString)
  {
    this(327680, paramInt, paramTypePath, paramString);
    if (getClass() != class$org$objectweb$asm$tree$TypeAnnotationNode) {
      throw new IllegalStateException();
    }
  }
  
  public TypeAnnotationNode(int paramInt1, int paramInt2, TypePath paramTypePath, String paramString)
  {
    super(paramInt1, paramString);
    this.typeRef = paramInt2;
    this.typePath = paramTypePath;
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
