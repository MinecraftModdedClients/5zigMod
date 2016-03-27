package org.objectweb.asm.xml;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

public final class SAXClassAdapter
  extends ClassVisitor
{
  SAXAdapter sa;
  private final boolean singleDocument;
  
  public SAXClassAdapter(ContentHandler paramContentHandler, boolean paramBoolean)
  {
    super(327680);
    this.sa = new SAXAdapter(paramContentHandler);
    this.singleDocument = paramBoolean;
    if (!paramBoolean) {
      this.sa.addDocumentStart();
    }
  }
  
  public void visitSource(String paramString1, String paramString2)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    if (paramString1 != null) {
      localAttributesImpl.addAttribute("", "file", "file", "", encode(paramString1));
    }
    if (paramString2 != null) {
      localAttributesImpl.addAttribute("", "debug", "debug", "", encode(paramString2));
    }
    this.sa.addElement("source", localAttributesImpl);
  }
  
  public void visitOuterClass(String paramString1, String paramString2, String paramString3)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "owner", "owner", "", paramString1);
    if (paramString2 != null) {
      localAttributesImpl.addAttribute("", "name", "name", "", paramString2);
    }
    if (paramString3 != null) {
      localAttributesImpl.addAttribute("", "desc", "desc", "", paramString3);
    }
    this.sa.addElement("outerclass", localAttributesImpl);
  }
  
  public AnnotationVisitor visitAnnotation(String paramString, boolean paramBoolean)
  {
    return new SAXAnnotationAdapter(this.sa, "annotation", paramBoolean ? 1 : -1, null, paramString);
  }
  
  public AnnotationVisitor visitTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return new SAXAnnotationAdapter(this.sa, "typeAnnotation", paramBoolean ? 1 : -1, null, paramString, paramInt, paramTypePath);
  }
  
  public void visit(int paramInt1, int paramInt2, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    StringBuffer localStringBuffer = new StringBuffer();
    appendAccess(paramInt2 | 0x40000, localStringBuffer);
    AttributesImpl localAttributesImpl1 = new AttributesImpl();
    localAttributesImpl1.addAttribute("", "access", "access", "", localStringBuffer.toString());
    if (paramString1 != null) {
      localAttributesImpl1.addAttribute("", "name", "name", "", paramString1);
    }
    if (paramString2 != null) {
      localAttributesImpl1.addAttribute("", "signature", "signature", "", encode(paramString2));
    }
    if (paramString3 != null) {
      localAttributesImpl1.addAttribute("", "parent", "parent", "", paramString3);
    }
    localAttributesImpl1.addAttribute("", "major", "major", "", Integer.toString(paramInt1 & 0xFFFF));
    localAttributesImpl1.addAttribute("", "minor", "minor", "", Integer.toString(paramInt1 >>> 16));
    this.sa.addStart("class", localAttributesImpl1);
    this.sa.addStart("interfaces", new AttributesImpl());
    if ((paramArrayOfString != null) && (paramArrayOfString.length > 0)) {
      for (int i = 0; i < paramArrayOfString.length; i++)
      {
        AttributesImpl localAttributesImpl2 = new AttributesImpl();
        localAttributesImpl2.addAttribute("", "name", "name", "", paramArrayOfString[i]);
        this.sa.addElement("interface", localAttributesImpl2);
      }
    }
    this.sa.addEnd("interfaces");
  }
  
  public FieldVisitor visitField(int paramInt, String paramString1, String paramString2, String paramString3, Object paramObject)
  {
    StringBuffer localStringBuffer = new StringBuffer();
    appendAccess(paramInt | 0x80000, localStringBuffer);
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "access", "access", "", localStringBuffer.toString());
    localAttributesImpl.addAttribute("", "name", "name", "", paramString1);
    localAttributesImpl.addAttribute("", "desc", "desc", "", paramString2);
    if (paramString3 != null) {
      localAttributesImpl.addAttribute("", "signature", "signature", "", encode(paramString3));
    }
    if (paramObject != null) {
      localAttributesImpl.addAttribute("", "value", "value", "", encode(paramObject.toString()));
    }
    return new SAXFieldAdapter(this.sa, localAttributesImpl);
  }
  
  public MethodVisitor visitMethod(int paramInt, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    StringBuffer localStringBuffer = new StringBuffer();
    appendAccess(paramInt, localStringBuffer);
    AttributesImpl localAttributesImpl1 = new AttributesImpl();
    localAttributesImpl1.addAttribute("", "access", "access", "", localStringBuffer.toString());
    localAttributesImpl1.addAttribute("", "name", "name", "", paramString1);
    localAttributesImpl1.addAttribute("", "desc", "desc", "", paramString2);
    if (paramString3 != null) {
      localAttributesImpl1.addAttribute("", "signature", "signature", "", paramString3);
    }
    this.sa.addStart("method", localAttributesImpl1);
    this.sa.addStart("exceptions", new AttributesImpl());
    if ((paramArrayOfString != null) && (paramArrayOfString.length > 0)) {
      for (int i = 0; i < paramArrayOfString.length; i++)
      {
        AttributesImpl localAttributesImpl2 = new AttributesImpl();
        localAttributesImpl2.addAttribute("", "name", "name", "", paramArrayOfString[i]);
        this.sa.addElement("exception", localAttributesImpl2);
      }
    }
    this.sa.addEnd("exceptions");
    return new SAXCodeAdapter(this.sa, paramInt);
  }
  
  public final void visitInnerClass(String paramString1, String paramString2, String paramString3, int paramInt)
  {
    StringBuffer localStringBuffer = new StringBuffer();
    appendAccess(paramInt | 0x100000, localStringBuffer);
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "access", "access", "", localStringBuffer.toString());
    if (paramString1 != null) {
      localAttributesImpl.addAttribute("", "name", "name", "", paramString1);
    }
    if (paramString2 != null) {
      localAttributesImpl.addAttribute("", "outerName", "outerName", "", paramString2);
    }
    if (paramString3 != null) {
      localAttributesImpl.addAttribute("", "innerName", "innerName", "", paramString3);
    }
    this.sa.addElement("innerclass", localAttributesImpl);
  }
  
  public final void visitEnd()
  {
    this.sa.addEnd("class");
    if (!this.singleDocument) {
      this.sa.addDocumentEnd();
    }
  }
  
  static final String encode(String paramString)
  {
    StringBuffer localStringBuffer = new StringBuffer();
    for (int i = 0; i < paramString.length(); i++)
    {
      char c = paramString.charAt(i);
      if (c == '\\')
      {
        localStringBuffer.append("\\\\");
      }
      else if ((c < ' ') || (c > ''))
      {
        localStringBuffer.append("\\u");
        if (c < '\020') {
          localStringBuffer.append("000");
        } else if (c < 'Ā') {
          localStringBuffer.append("00");
        } else if (c < 'က') {
          localStringBuffer.append('0');
        }
        localStringBuffer.append(Integer.toString(c, 16));
      }
      else
      {
        localStringBuffer.append(c);
      }
    }
    return localStringBuffer.toString();
  }
  
  static void appendAccess(int paramInt, StringBuffer paramStringBuffer)
  {
    if ((paramInt & 0x1) != 0) {
      paramStringBuffer.append("public ");
    }
    if ((paramInt & 0x2) != 0) {
      paramStringBuffer.append("private ");
    }
    if ((paramInt & 0x4) != 0) {
      paramStringBuffer.append("protected ");
    }
    if ((paramInt & 0x10) != 0) {
      paramStringBuffer.append("final ");
    }
    if ((paramInt & 0x8) != 0) {
      paramStringBuffer.append("static ");
    }
    if ((paramInt & 0x20) != 0) {
      if ((paramInt & 0x40000) == 0) {
        paramStringBuffer.append("synchronized ");
      } else {
        paramStringBuffer.append("super ");
      }
    }
    if ((paramInt & 0x40) != 0) {
      if ((paramInt & 0x80000) == 0) {
        paramStringBuffer.append("bridge ");
      } else {
        paramStringBuffer.append("volatile ");
      }
    }
    if ((paramInt & 0x80) != 0) {
      if ((paramInt & 0x80000) == 0) {
        paramStringBuffer.append("varargs ");
      } else {
        paramStringBuffer.append("transient ");
      }
    }
    if ((paramInt & 0x100) != 0) {
      paramStringBuffer.append("native ");
    }
    if ((paramInt & 0x800) != 0) {
      paramStringBuffer.append("strict ");
    }
    if ((paramInt & 0x200) != 0) {
      paramStringBuffer.append("interface ");
    }
    if ((paramInt & 0x400) != 0) {
      paramStringBuffer.append("abstract ");
    }
    if ((paramInt & 0x1000) != 0) {
      paramStringBuffer.append("synthetic ");
    }
    if ((paramInt & 0x2000) != 0) {
      paramStringBuffer.append("annotation ");
    }
    if ((paramInt & 0x4000) != 0) {
      paramStringBuffer.append("enum ");
    }
    if ((paramInt & 0x20000) != 0) {
      paramStringBuffer.append("deprecated ");
    }
    if ((paramInt & 0x8000) != 0) {
      paramStringBuffer.append("mandated ");
    }
  }
}
