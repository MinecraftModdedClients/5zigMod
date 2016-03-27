package org.objectweb.asm.xml;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.xml.sax.helpers.AttributesImpl;

public final class SAXCodeAdapter
  extends MethodVisitor
{
  static final String[] TYPES = { "top", "int", "float", "double", "long", "null", "uninitializedThis" };
  SAXAdapter sa;
  int access;
  private final Map labelNames;
  
  public SAXCodeAdapter(SAXAdapter paramSAXAdapter, int paramInt)
  {
    super(327680);
    this.sa = paramSAXAdapter;
    this.access = paramInt;
    this.labelNames = new HashMap();
  }
  
  public void visitParameter(String paramString, int paramInt)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    if (paramString != null) {
      localAttributesImpl.addAttribute("", "name", "name", "", paramString);
    }
    StringBuffer localStringBuffer = new StringBuffer();
    SAXClassAdapter.appendAccess(paramInt, localStringBuffer);
    localAttributesImpl.addAttribute("", "access", "access", "", localStringBuffer.toString());
    this.sa.addElement("parameter", localAttributesImpl);
  }
  
  public final void visitCode()
  {
    if ((this.access & 0x700) == 0) {
      this.sa.addStart("code", new AttributesImpl());
    }
  }
  
  public void visitFrame(int paramInt1, int paramInt2, Object[] paramArrayOfObject1, int paramInt3, Object[] paramArrayOfObject2)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    switch (paramInt1)
    {
    case -1: 
    case 0: 
      if (paramInt1 == -1) {
        localAttributesImpl.addAttribute("", "type", "type", "", "NEW");
      } else {
        localAttributesImpl.addAttribute("", "type", "type", "", "FULL");
      }
      this.sa.addStart("frame", localAttributesImpl);
      appendFrameTypes(true, paramInt2, paramArrayOfObject1);
      appendFrameTypes(false, paramInt3, paramArrayOfObject2);
      break;
    case 1: 
      localAttributesImpl.addAttribute("", "type", "type", "", "APPEND");
      this.sa.addStart("frame", localAttributesImpl);
      appendFrameTypes(true, paramInt2, paramArrayOfObject1);
      break;
    case 2: 
      localAttributesImpl.addAttribute("", "type", "type", "", "CHOP");
      localAttributesImpl.addAttribute("", "count", "count", "", Integer.toString(paramInt2));
      this.sa.addStart("frame", localAttributesImpl);
      break;
    case 3: 
      localAttributesImpl.addAttribute("", "type", "type", "", "SAME");
      this.sa.addStart("frame", localAttributesImpl);
      break;
    case 4: 
      localAttributesImpl.addAttribute("", "type", "type", "", "SAME1");
      this.sa.addStart("frame", localAttributesImpl);
      appendFrameTypes(false, 1, paramArrayOfObject2);
    }
    this.sa.addEnd("frame");
  }
  
  private void appendFrameTypes(boolean paramBoolean, int paramInt, Object[] paramArrayOfObject)
  {
    for (int i = 0; i < paramInt; i++)
    {
      Object localObject = paramArrayOfObject[i];
      AttributesImpl localAttributesImpl = new AttributesImpl();
      if ((localObject instanceof String))
      {
        localAttributesImpl.addAttribute("", "type", "type", "", (String)localObject);
      }
      else if ((localObject instanceof Integer))
      {
        localAttributesImpl.addAttribute("", "type", "type", "", TYPES[((Integer)localObject).intValue()]);
      }
      else
      {
        localAttributesImpl.addAttribute("", "type", "type", "", "uninitialized");
        localAttributesImpl.addAttribute("", "label", "label", "", getLabel((Label)localObject));
      }
      this.sa.addElement(paramBoolean ? "local" : "stack", localAttributesImpl);
    }
  }
  
  public final void visitInsn(int paramInt)
  {
    this.sa.addElement(org.objectweb.asm.util.Printer.OPCODES[paramInt], new AttributesImpl());
  }
  
  public final void visitIntInsn(int paramInt1, int paramInt2)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "value", "value", "", Integer.toString(paramInt2));
    this.sa.addElement(org.objectweb.asm.util.Printer.OPCODES[paramInt1], localAttributesImpl);
  }
  
  public final void visitVarInsn(int paramInt1, int paramInt2)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "var", "var", "", Integer.toString(paramInt2));
    this.sa.addElement(org.objectweb.asm.util.Printer.OPCODES[paramInt1], localAttributesImpl);
  }
  
  public final void visitTypeInsn(int paramInt, String paramString)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "desc", "desc", "", paramString);
    this.sa.addElement(org.objectweb.asm.util.Printer.OPCODES[paramInt], localAttributesImpl);
  }
  
  public final void visitFieldInsn(int paramInt, String paramString1, String paramString2, String paramString3)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "owner", "owner", "", paramString1);
    localAttributesImpl.addAttribute("", "name", "name", "", paramString2);
    localAttributesImpl.addAttribute("", "desc", "desc", "", paramString3);
    this.sa.addElement(org.objectweb.asm.util.Printer.OPCODES[paramInt], localAttributesImpl);
  }
  
  public final void visitMethodInsn(int paramInt, String paramString1, String paramString2, String paramString3, boolean paramBoolean)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "owner", "owner", "", paramString1);
    localAttributesImpl.addAttribute("", "name", "name", "", paramString2);
    localAttributesImpl.addAttribute("", "desc", "desc", "", paramString3);
    localAttributesImpl.addAttribute("", "itf", "itf", "", paramBoolean ? "true" : "false");
    this.sa.addElement(org.objectweb.asm.util.Printer.OPCODES[paramInt], localAttributesImpl);
  }
  
  public void visitInvokeDynamicInsn(String paramString1, String paramString2, Handle paramHandle, Object... paramVarArgs)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "name", "name", "", paramString1);
    localAttributesImpl.addAttribute("", "desc", "desc", "", paramString2);
    localAttributesImpl.addAttribute("", "bsm", "bsm", "", SAXClassAdapter.encode(paramHandle.toString()));
    this.sa.addStart("INVOKEDYNAMIC", localAttributesImpl);
    for (int i = 0; i < paramVarArgs.length; i++) {
      this.sa.addElement("bsmArg", getConstantAttribute(paramVarArgs[i]));
    }
    this.sa.addEnd("INVOKEDYNAMIC");
  }
  
  public final void visitJumpInsn(int paramInt, Label paramLabel)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "label", "label", "", getLabel(paramLabel));
    this.sa.addElement(org.objectweb.asm.util.Printer.OPCODES[paramInt], localAttributesImpl);
  }
  
  public final void visitLabel(Label paramLabel)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "name", "name", "", getLabel(paramLabel));
    this.sa.addElement("Label", localAttributesImpl);
  }
  
  public final void visitLdcInsn(Object paramObject)
  {
    this.sa.addElement(org.objectweb.asm.util.Printer.OPCODES[18], getConstantAttribute(paramObject));
  }
  
  private static AttributesImpl getConstantAttribute(Object paramObject)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "cst", "cst", "", SAXClassAdapter.encode(paramObject.toString()));
    localAttributesImpl.addAttribute("", "desc", "desc", "", Type.getDescriptor(paramObject.getClass()));
    return localAttributesImpl;
  }
  
  public final void visitIincInsn(int paramInt1, int paramInt2)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "var", "var", "", Integer.toString(paramInt1));
    localAttributesImpl.addAttribute("", "inc", "inc", "", Integer.toString(paramInt2));
    this.sa.addElement(org.objectweb.asm.util.Printer.OPCODES[''], localAttributesImpl);
  }
  
  public final void visitTableSwitchInsn(int paramInt1, int paramInt2, Label paramLabel, Label... paramVarArgs)
  {
    AttributesImpl localAttributesImpl1 = new AttributesImpl();
    localAttributesImpl1.addAttribute("", "min", "min", "", Integer.toString(paramInt1));
    localAttributesImpl1.addAttribute("", "max", "max", "", Integer.toString(paramInt2));
    localAttributesImpl1.addAttribute("", "dflt", "dflt", "", getLabel(paramLabel));
    String str = org.objectweb.asm.util.Printer.OPCODES['ª'];
    this.sa.addStart(str, localAttributesImpl1);
    for (int i = 0; i < paramVarArgs.length; i++)
    {
      AttributesImpl localAttributesImpl2 = new AttributesImpl();
      localAttributesImpl2.addAttribute("", "name", "name", "", getLabel(paramVarArgs[i]));
      this.sa.addElement("label", localAttributesImpl2);
    }
    this.sa.addEnd(str);
  }
  
  public final void visitLookupSwitchInsn(Label paramLabel, int[] paramArrayOfInt, Label[] paramArrayOfLabel)
  {
    AttributesImpl localAttributesImpl1 = new AttributesImpl();
    localAttributesImpl1.addAttribute("", "dflt", "dflt", "", getLabel(paramLabel));
    String str = org.objectweb.asm.util.Printer.OPCODES['«'];
    this.sa.addStart(str, localAttributesImpl1);
    for (int i = 0; i < paramArrayOfLabel.length; i++)
    {
      AttributesImpl localAttributesImpl2 = new AttributesImpl();
      localAttributesImpl2.addAttribute("", "name", "name", "", getLabel(paramArrayOfLabel[i]));
      localAttributesImpl2.addAttribute("", "key", "key", "", Integer.toString(paramArrayOfInt[i]));
      this.sa.addElement("label", localAttributesImpl2);
    }
    this.sa.addEnd(str);
  }
  
  public final void visitMultiANewArrayInsn(String paramString, int paramInt)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "desc", "desc", "", paramString);
    localAttributesImpl.addAttribute("", "dims", "dims", "", Integer.toString(paramInt));
    this.sa.addElement(org.objectweb.asm.util.Printer.OPCODES['Å'], localAttributesImpl);
  }
  
  public final void visitTryCatchBlock(Label paramLabel1, Label paramLabel2, Label paramLabel3, String paramString)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "start", "start", "", getLabel(paramLabel1));
    localAttributesImpl.addAttribute("", "end", "end", "", getLabel(paramLabel2));
    localAttributesImpl.addAttribute("", "handler", "handler", "", getLabel(paramLabel3));
    if (paramString != null) {
      localAttributesImpl.addAttribute("", "type", "type", "", paramString);
    }
    this.sa.addElement("TryCatch", localAttributesImpl);
  }
  
  public final void visitMaxs(int paramInt1, int paramInt2)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "maxStack", "maxStack", "", Integer.toString(paramInt1));
    localAttributesImpl.addAttribute("", "maxLocals", "maxLocals", "", Integer.toString(paramInt2));
    this.sa.addElement("Max", localAttributesImpl);
    this.sa.addEnd("code");
  }
  
  public void visitLocalVariable(String paramString1, String paramString2, String paramString3, Label paramLabel1, Label paramLabel2, int paramInt)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "name", "name", "", paramString1);
    localAttributesImpl.addAttribute("", "desc", "desc", "", paramString2);
    if (paramString3 != null) {
      localAttributesImpl.addAttribute("", "signature", "signature", "", SAXClassAdapter.encode(paramString3));
    }
    localAttributesImpl.addAttribute("", "start", "start", "", getLabel(paramLabel1));
    localAttributesImpl.addAttribute("", "end", "end", "", getLabel(paramLabel2));
    localAttributesImpl.addAttribute("", "var", "var", "", Integer.toString(paramInt));
    this.sa.addElement("LocalVar", localAttributesImpl);
  }
  
  public final void visitLineNumber(int paramInt, Label paramLabel)
  {
    AttributesImpl localAttributesImpl = new AttributesImpl();
    localAttributesImpl.addAttribute("", "line", "line", "", Integer.toString(paramInt));
    localAttributesImpl.addAttribute("", "start", "start", "", getLabel(paramLabel));
    this.sa.addElement("LineNumber", localAttributesImpl);
  }
  
  public AnnotationVisitor visitAnnotationDefault()
  {
    return new SAXAnnotationAdapter(this.sa, "annotationDefault", 0, null, null);
  }
  
  public AnnotationVisitor visitAnnotation(String paramString, boolean paramBoolean)
  {
    return new SAXAnnotationAdapter(this.sa, "annotation", paramBoolean ? 1 : -1, null, paramString);
  }
  
  public AnnotationVisitor visitTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return new SAXAnnotationAdapter(this.sa, "typeAnnotation", paramBoolean ? 1 : -1, null, paramString, paramInt, paramTypePath);
  }
  
  public AnnotationVisitor visitParameterAnnotation(int paramInt, String paramString, boolean paramBoolean)
  {
    return new SAXAnnotationAdapter(this.sa, "parameterAnnotation", paramBoolean ? 1 : -1, paramInt, paramString);
  }
  
  public AnnotationVisitor visitInsnAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return new SAXAnnotationAdapter(this.sa, "insnAnnotation", paramBoolean ? 1 : -1, null, paramString, paramInt, paramTypePath);
  }
  
  public AnnotationVisitor visitTryCatchAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return new SAXAnnotationAdapter(this.sa, "tryCatchAnnotation", paramBoolean ? 1 : -1, null, paramString, paramInt, paramTypePath);
  }
  
  public AnnotationVisitor visitLocalVariableAnnotation(int paramInt, TypePath paramTypePath, Label[] paramArrayOfLabel1, Label[] paramArrayOfLabel2, int[] paramArrayOfInt, String paramString, boolean paramBoolean)
  {
    String[] arrayOfString1 = new String[paramArrayOfLabel1.length];
    String[] arrayOfString2 = new String[paramArrayOfLabel2.length];
    for (int i = 0; i < arrayOfString1.length; i++) {
      arrayOfString1[i] = getLabel(paramArrayOfLabel1[i]);
    }
    for (i = 0; i < arrayOfString2.length; i++) {
      arrayOfString2[i] = getLabel(paramArrayOfLabel2[i]);
    }
    return new SAXAnnotationAdapter(this.sa, "localVariableAnnotation", paramBoolean ? 1 : -1, null, paramString, paramInt, paramTypePath, arrayOfString1, arrayOfString2, paramArrayOfInt);
  }
  
  public void visitEnd()
  {
    this.sa.addEnd("method");
  }
  
  private final String getLabel(Label paramLabel)
  {
    String str = (String)this.labelNames.get(paramLabel);
    if (str == null)
    {
      str = Integer.toString(this.labelNames.size());
      this.labelNames.put(paramLabel, str);
    }
    return str;
  }
  
  static {}
  
  static void _clinit_() {}
}
