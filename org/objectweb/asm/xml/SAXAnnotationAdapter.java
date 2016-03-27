package org.objectweb.asm.xml;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.xml.sax.helpers.AttributesImpl;

public final class SAXAnnotationAdapter
  extends AnnotationVisitor
{
  SAXAdapter sa;
  private final String elementName;
  
  public SAXAnnotationAdapter(SAXAdapter paramSAXAdapter, String paramString1, int paramInt, String paramString2, String paramString3)
  {
    this(327680, paramSAXAdapter, paramString1, paramInt, paramString3, paramString2, -1, -1, null, null, null, null);
  }
  
  public SAXAnnotationAdapter(SAXAdapter paramSAXAdapter, String paramString1, int paramInt1, int paramInt2, String paramString2)
  {
    this(327680, paramSAXAdapter, paramString1, paramInt1, paramString2, null, paramInt2, -1, null, null, null, null);
  }
  
  public SAXAnnotationAdapter(SAXAdapter paramSAXAdapter, String paramString1, int paramInt1, String paramString2, String paramString3, int paramInt2, TypePath paramTypePath)
  {
    this(327680, paramSAXAdapter, paramString1, paramInt1, paramString3, paramString2, -1, paramInt2, paramTypePath, null, null, null);
  }
  
  public SAXAnnotationAdapter(SAXAdapter paramSAXAdapter, String paramString1, int paramInt1, String paramString2, String paramString3, int paramInt2, TypePath paramTypePath, String[] paramArrayOfString1, String[] paramArrayOfString2, int[] paramArrayOfInt)
  {
    this(327680, paramSAXAdapter, paramString1, paramInt1, paramString3, paramString2, -1, paramInt2, paramTypePath, paramArrayOfString1, paramArrayOfString2, paramArrayOfInt);
  }
  
  protected SAXAnnotationAdapter(int paramInt1, SAXAdapter paramSAXAdapter, String paramString1, int paramInt2, String paramString2, String paramString3, int paramInt3)
  {
    this(paramInt1, paramSAXAdapter, paramString1, paramInt2, paramString2, paramString3, paramInt3, -1, null, null, null, null);
  }
  
  protected SAXAnnotationAdapter(int paramInt1, SAXAdapter paramSAXAdapter, String paramString1, int paramInt2, String paramString2, String paramString3, int paramInt3, int paramInt4, TypePath paramTypePath, String[] paramArrayOfString1, String[] paramArrayOfString2, int[] paramArrayOfInt)
  {
    super(paramInt1);
    this.sa = paramSAXAdapter;
    this.elementName = paramString1;
    AttributesImpl localAttributesImpl = new AttributesImpl();
    if (paramString3 != null) {
      localAttributesImpl.addAttribute("", "name", "name", "", paramString3);
    }
    if (paramInt2 != 0) {
      localAttributesImpl.addAttribute("", "visible", "visible", "", paramInt2 > 0 ? "true" : "false");
    }
    if (paramInt3 != -1) {
      localAttributesImpl.addAttribute("", "parameter", "parameter", "", Integer.toString(paramInt3));
    }
    if (paramString2 != null) {
      localAttributesImpl.addAttribute("", "desc", "desc", "", paramString2);
    }
    if (paramInt4 != -1) {
      localAttributesImpl.addAttribute("", "typeRef", "typeRef", "", Integer.toString(paramInt4));
    }
    if (paramTypePath != null) {
      localAttributesImpl.addAttribute("", "typePath", "typePath", "", paramTypePath.toString());
    }
    StringBuffer localStringBuffer;
    int i;
    if (paramArrayOfString1 != null)
    {
      localStringBuffer = new StringBuffer(paramArrayOfString1[0]);
      for (i = 1; i < paramArrayOfString1.length; i++) {
        localStringBuffer.append(" ").append(paramArrayOfString1[i]);
      }
      localAttributesImpl.addAttribute("", "start", "start", "", localStringBuffer.toString());
    }
    if (paramArrayOfString2 != null)
    {
      localStringBuffer = new StringBuffer(paramArrayOfString2[0]);
      for (i = 1; i < paramArrayOfString2.length; i++) {
        localStringBuffer.append(" ").append(paramArrayOfString2[i]);
      }
      localAttributesImpl.addAttribute("", "end", "end", "", localStringBuffer.toString());
    }
    if (paramArrayOfInt != null)
    {
      localStringBuffer = new StringBuffer();
      localStringBuffer.append(paramArrayOfInt[0]);
      for (i = 1; i < paramArrayOfInt.length; i++) {
        localStringBuffer.append(" ").append(paramArrayOfInt[i]);
      }
      localAttributesImpl.addAttribute("", "index", "index", "", localStringBuffer.toString());
    }
    paramSAXAdapter.addStart(paramString1, localAttributesImpl);
  }
  
  public void visit(String paramString, Object paramObject)
  {
    Class localClass = paramObject.getClass();
    if (localClass.isArray())
    {
      AnnotationVisitor localAnnotationVisitor = visitArray(paramString);
      Object localObject;
      int i;
      if ((paramObject instanceof byte[]))
      {
        localObject = (byte[])paramObject;
        for (i = 0; i < localObject.length; i++) {
          localAnnotationVisitor.visit(null, new Byte(localObject[i]));
        }
      }
      else if ((paramObject instanceof char[]))
      {
        localObject = (char[])paramObject;
        for (i = 0; i < localObject.length; i++) {
          localAnnotationVisitor.visit(null, new Character(localObject[i]));
        }
      }
      else if ((paramObject instanceof short[]))
      {
        localObject = (short[])paramObject;
        for (i = 0; i < localObject.length; i++) {
          localAnnotationVisitor.visit(null, new Short(localObject[i]));
        }
      }
      else if ((paramObject instanceof boolean[]))
      {
        localObject = (boolean[])paramObject;
        for (i = 0; i < localObject.length; i++) {
          localAnnotationVisitor.visit(null, Boolean.valueOf(localObject[i]));
        }
      }
      else if ((paramObject instanceof int[]))
      {
        localObject = (int[])paramObject;
        for (i = 0; i < localObject.length; i++) {
          localAnnotationVisitor.visit(null, new Integer(localObject[i]));
        }
      }
      else if ((paramObject instanceof long[]))
      {
        localObject = (long[])paramObject;
        for (i = 0; i < localObject.length; i++) {
          localAnnotationVisitor.visit(null, new Long(localObject[i]));
        }
      }
      else if ((paramObject instanceof float[]))
      {
        localObject = (float[])paramObject;
        for (i = 0; i < localObject.length; i++) {
          localAnnotationVisitor.visit(null, new Float(localObject[i]));
        }
      }
      else if ((paramObject instanceof double[]))
      {
        localObject = (double[])paramObject;
        for (i = 0; i < localObject.length; i++) {
          localAnnotationVisitor.visit(null, new Double(localObject[i]));
        }
      }
      localAnnotationVisitor.visitEnd();
    }
    else
    {
      addValueElement("annotationValue", paramString, Type.getDescriptor(localClass), paramObject.toString());
    }
  }
  
  public void visitEnum(String paramString1, String paramString2, String paramString3)
  {
    addValueElement("annotationValueEnum", paramString1, paramString2, paramString3);
  }
  
  public AnnotationVisitor visitAnnotation(String paramString1, String paramString2)
  {
    return new SAXAnnotationAdapter(this.sa, "annotationValueAnnotation", 0, paramString1, paramString2);
  }
  
  public AnnotationVisitor visitArray(String paramString)
  {
    return new SAXAnnotationAdapter(this.sa, "annotationValueArray", 0, paramString, null);
  }
  
  public void visitEnd()
  {
    this.sa.addEnd(this.elementName);
  }
  
  private void addValueElement(String paramString1, String paramString2, String paramString3, String paramString4)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    if (paramString2 != null) {
      localAttributesImpl.addAttribute("", "name", "name", "", paramString2);
    }
    if (paramString3 != null) {
      localAttributesImpl.addAttribute("", "desc", "desc", "", paramString3);
    }
    if (paramString4 != null) {
      localAttributesImpl.addAttribute("", "value", "value", "", SAXClassAdapter.encode(paramString4));
    }
    this.sa.addElement(paramString1, localAttributesImpl);
  }
}
