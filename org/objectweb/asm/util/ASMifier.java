package org.objectweb.asm.util;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

public class ASMifier
  extends Printer
{
  protected final String name;
  protected final int id;
  protected Map labelNames;
  static Class class$org$objectweb$asm$util$ASMifier = class$("org.objectweb.asm.util.ASMifier");
  
  public ASMifier()
  {
    this(327680, "cw", 0);
    if (getClass() != class$org$objectweb$asm$util$ASMifier) {
      throw new IllegalStateException();
    }
  }
  
  protected ASMifier(int paramInt1, String paramString, int paramInt2)
  {
    super(paramInt1);
    this.name = paramString;
    this.id = paramInt2;
  }
  
  public static void main(String[] paramArrayOfString)
    throws Exception
  {
    int i = 0;
    int j = 2;
    int k = 1;
    if ((paramArrayOfString.length < 1) || (paramArrayOfString.length > 2)) {
      k = 0;
    }
    if ((k != 0) && ("-debug".equals(paramArrayOfString[0])))
    {
      i = 1;
      j = 0;
      if (paramArrayOfString.length != 2) {
        k = 0;
      }
    }
    if (k == 0)
    {
      System.err.println("Prints the ASM code to generate the given class.");
      System.err.println("Usage: ASMifier [-debug] <fully qualified class name or class file name>");
      return;
    }
    ClassReader localClassReader;
    if ((paramArrayOfString[i].endsWith(".class")) || (paramArrayOfString[i].indexOf('\\') > -1) || (paramArrayOfString[i].indexOf('/') > -1)) {
      localClassReader = new ClassReader(new FileInputStream(paramArrayOfString[i]));
    } else {
      localClassReader = new ClassReader(paramArrayOfString[i]);
    }
    localClassReader.accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(System.out)), j);
  }
  
  public void visit(int paramInt1, int paramInt2, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    int i = paramString1.lastIndexOf('/');
    String str;
    if (i == -1)
    {
      str = paramString1;
    }
    else
    {
      this.text.add("package asm." + paramString1.substring(0, i).replace('/', '.') + ";\n");
      str = paramString1.substring(i + 1);
    }
    this.text.add("import java.util.*;\n");
    this.text.add("import org.objectweb.asm.*;\n");
    this.text.add("public class " + str + "Dump implements Opcodes {\n\n");
    this.text.add("public static byte[] dump () throws Exception {\n\n");
    this.text.add("ClassWriter cw = new ClassWriter(0);\n");
    this.text.add("FieldVisitor fv;\n");
    this.text.add("MethodVisitor mv;\n");
    this.text.add("AnnotationVisitor av0;\n\n");
    this.buf.setLength(0);
    this.buf.append("cw.visit(");
    switch (paramInt1)
    {
    case 196653: 
      this.buf.append("V1_1");
      break;
    case 46: 
      this.buf.append("V1_2");
      break;
    case 47: 
      this.buf.append("V1_3");
      break;
    case 48: 
      this.buf.append("V1_4");
      break;
    case 49: 
      this.buf.append("V1_5");
      break;
    case 50: 
      this.buf.append("V1_6");
      break;
    case 51: 
      this.buf.append("V1_7");
      break;
    default: 
      this.buf.append(paramInt1);
    }
    this.buf.append(", ");
    appendAccess(paramInt2 | 0x40000);
    this.buf.append(", ");
    appendConstant(paramString1);
    this.buf.append(", ");
    appendConstant(paramString2);
    this.buf.append(", ");
    appendConstant(paramString3);
    this.buf.append(", ");
    if ((paramArrayOfString != null) && (paramArrayOfString.length > 0))
    {
      this.buf.append("new String[] {");
      for (int j = 0; j < paramArrayOfString.length; j++)
      {
        this.buf.append(j == 0 ? " " : ", ");
        appendConstant(paramArrayOfString[j]);
      }
      this.buf.append(" }");
    }
    else
    {
      this.buf.append("null");
    }
    this.buf.append(");\n\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitSource(String paramString1, String paramString2)
  {
    this.buf.setLength(0);
    this.buf.append("cw.visitSource(");
    appendConstant(paramString1);
    this.buf.append(", ");
    appendConstant(paramString2);
    this.buf.append(");\n\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitOuterClass(String paramString1, String paramString2, String paramString3)
  {
    this.buf.setLength(0);
    this.buf.append("cw.visitOuterClass(");
    appendConstant(paramString1);
    this.buf.append(", ");
    appendConstant(paramString2);
    this.buf.append(", ");
    appendConstant(paramString3);
    this.buf.append(");\n\n");
    this.text.add(this.buf.toString());
  }
  
  public ASMifier visitClassAnnotation(String paramString, boolean paramBoolean)
  {
    return visitAnnotation(paramString, paramBoolean);
  }
  
  public ASMifier visitClassTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return visitTypeAnnotation(paramInt, paramTypePath, paramString, paramBoolean);
  }
  
  public void visitClassAttribute(Attribute paramAttribute)
  {
    visitAttribute(paramAttribute);
  }
  
  public void visitInnerClass(String paramString1, String paramString2, String paramString3, int paramInt)
  {
    this.buf.setLength(0);
    this.buf.append("cw.visitInnerClass(");
    appendConstant(paramString1);
    this.buf.append(", ");
    appendConstant(paramString2);
    this.buf.append(", ");
    appendConstant(paramString3);
    this.buf.append(", ");
    appendAccess(paramInt | 0x100000);
    this.buf.append(");\n\n");
    this.text.add(this.buf.toString());
  }
  
  public ASMifier visitField(int paramInt, String paramString1, String paramString2, String paramString3, Object paramObject)
  {
    this.buf.setLength(0);
    this.buf.append("{\n");
    this.buf.append("fv = cw.visitField(");
    appendAccess(paramInt | 0x80000);
    this.buf.append(", ");
    appendConstant(paramString1);
    this.buf.append(", ");
    appendConstant(paramString2);
    this.buf.append(", ");
    appendConstant(paramString3);
    this.buf.append(", ");
    appendConstant(paramObject);
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
    ASMifier localASMifier = createASMifier("fv", 0);
    this.text.add(localASMifier.getText());
    this.text.add("}\n");
    return localASMifier;
  }
  
  public ASMifier visitMethod(int paramInt, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    this.buf.setLength(0);
    this.buf.append("{\n");
    this.buf.append("mv = cw.visitMethod(");
    appendAccess(paramInt);
    this.buf.append(", ");
    appendConstant(paramString1);
    this.buf.append(", ");
    appendConstant(paramString2);
    this.buf.append(", ");
    appendConstant(paramString3);
    this.buf.append(", ");
    if ((paramArrayOfString != null) && (paramArrayOfString.length > 0))
    {
      this.buf.append("new String[] {");
      for (int i = 0; i < paramArrayOfString.length; i++)
      {
        this.buf.append(i == 0 ? " " : ", ");
        appendConstant(paramArrayOfString[i]);
      }
      this.buf.append(" }");
    }
    else
    {
      this.buf.append("null");
    }
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
    ASMifier localASMifier = createASMifier("mv", 0);
    this.text.add(localASMifier.getText());
    this.text.add("}\n");
    return localASMifier;
  }
  
  public void visitClassEnd()
  {
    this.text.add("cw.visitEnd();\n\n");
    this.text.add("return cw.toByteArray();\n");
    this.text.add("}\n");
    this.text.add("}\n");
  }
  
  public void visit(String paramString, Object paramObject)
  {
    this.buf.setLength(0);
    this.buf.append("av").append(this.id).append(".visit(");
    appendConstant(this.buf, paramString);
    this.buf.append(", ");
    appendConstant(this.buf, paramObject);
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitEnum(String paramString1, String paramString2, String paramString3)
  {
    this.buf.setLength(0);
    this.buf.append("av").append(this.id).append(".visitEnum(");
    appendConstant(this.buf, paramString1);
    this.buf.append(", ");
    appendConstant(this.buf, paramString2);
    this.buf.append(", ");
    appendConstant(this.buf, paramString3);
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public ASMifier visitAnnotation(String paramString1, String paramString2)
  {
    this.buf.setLength(0);
    this.buf.append("{\n");
    this.buf.append("AnnotationVisitor av").append(this.id + 1).append(" = av");
    this.buf.append(this.id).append(".visitAnnotation(");
    appendConstant(this.buf, paramString1);
    this.buf.append(", ");
    appendConstant(this.buf, paramString2);
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
    ASMifier localASMifier = createASMifier("av", this.id + 1);
    this.text.add(localASMifier.getText());
    this.text.add("}\n");
    return localASMifier;
  }
  
  public ASMifier visitArray(String paramString)
  {
    this.buf.setLength(0);
    this.buf.append("{\n");
    this.buf.append("AnnotationVisitor av").append(this.id + 1).append(" = av");
    this.buf.append(this.id).append(".visitArray(");
    appendConstant(this.buf, paramString);
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
    ASMifier localASMifier = createASMifier("av", this.id + 1);
    this.text.add(localASMifier.getText());
    this.text.add("}\n");
    return localASMifier;
  }
  
  public void visitAnnotationEnd()
  {
    this.buf.setLength(0);
    this.buf.append("av").append(this.id).append(".visitEnd();\n");
    this.text.add(this.buf.toString());
  }
  
  public ASMifier visitFieldAnnotation(String paramString, boolean paramBoolean)
  {
    return visitAnnotation(paramString, paramBoolean);
  }
  
  public ASMifier visitFieldTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return visitTypeAnnotation(paramInt, paramTypePath, paramString, paramBoolean);
  }
  
  public void visitFieldAttribute(Attribute paramAttribute)
  {
    visitAttribute(paramAttribute);
  }
  
  public void visitFieldEnd()
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitEnd();\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitParameter(String paramString, int paramInt)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitParameter(");
    appendString(this.buf, paramString);
    this.buf.append(", ");
    appendAccess(paramInt);
    this.text.add(");\n");
  }
  
  public ASMifier visitAnnotationDefault()
  {
    this.buf.setLength(0);
    this.buf.append("{\n").append("av0 = ").append(this.name).append(".visitAnnotationDefault();\n");
    this.text.add(this.buf.toString());
    ASMifier localASMifier = createASMifier("av", 0);
    this.text.add(localASMifier.getText());
    this.text.add("}\n");
    return localASMifier;
  }
  
  public ASMifier visitMethodAnnotation(String paramString, boolean paramBoolean)
  {
    return visitAnnotation(paramString, paramBoolean);
  }
  
  public ASMifier visitMethodTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return visitTypeAnnotation(paramInt, paramTypePath, paramString, paramBoolean);
  }
  
  public ASMifier visitParameterAnnotation(int paramInt, String paramString, boolean paramBoolean)
  {
    this.buf.setLength(0);
    this.buf.append("{\n").append("av0 = ").append(this.name).append(".visitParameterAnnotation(").append(paramInt).append(", ");
    appendConstant(paramString);
    this.buf.append(", ").append(paramBoolean).append(");\n");
    this.text.add(this.buf.toString());
    ASMifier localASMifier = createASMifier("av", 0);
    this.text.add(localASMifier.getText());
    this.text.add("}\n");
    return localASMifier;
  }
  
  public void visitMethodAttribute(Attribute paramAttribute)
  {
    visitAttribute(paramAttribute);
  }
  
  public void visitCode()
  {
    this.text.add(this.name + ".visitCode();\n");
  }
  
  public void visitFrame(int paramInt1, int paramInt2, Object[] paramArrayOfObject1, int paramInt3, Object[] paramArrayOfObject2)
  {
    this.buf.setLength(0);
    switch (paramInt1)
    {
    case -1: 
    case 0: 
      declareFrameTypes(paramInt2, paramArrayOfObject1);
      declareFrameTypes(paramInt3, paramArrayOfObject2);
      if (paramInt1 == -1) {
        this.buf.append(this.name).append(".visitFrame(Opcodes.F_NEW, ");
      } else {
        this.buf.append(this.name).append(".visitFrame(Opcodes.F_FULL, ");
      }
      this.buf.append(paramInt2).append(", new Object[] {");
      appendFrameTypes(paramInt2, paramArrayOfObject1);
      this.buf.append("}, ").append(paramInt3).append(", new Object[] {");
      appendFrameTypes(paramInt3, paramArrayOfObject2);
      this.buf.append('}');
      break;
    case 1: 
      declareFrameTypes(paramInt2, paramArrayOfObject1);
      this.buf.append(this.name).append(".visitFrame(Opcodes.F_APPEND,").append(paramInt2).append(", new Object[] {");
      appendFrameTypes(paramInt2, paramArrayOfObject1);
      this.buf.append("}, 0, null");
      break;
    case 2: 
      this.buf.append(this.name).append(".visitFrame(Opcodes.F_CHOP,").append(paramInt2).append(", null, 0, null");
      break;
    case 3: 
      this.buf.append(this.name).append(".visitFrame(Opcodes.F_SAME, 0, null, 0, null");
      break;
    case 4: 
      declareFrameTypes(1, paramArrayOfObject2);
      this.buf.append(this.name).append(".visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {");
      appendFrameTypes(1, paramArrayOfObject2);
      this.buf.append('}');
    }
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitInsn(int paramInt)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitInsn(").append(OPCODES[paramInt]).append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitIntInsn(int paramInt1, int paramInt2)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitIntInsn(").append(OPCODES[paramInt1]).append(", ").append(paramInt1 == 188 ? TYPES[paramInt2] : Integer.toString(paramInt2)).append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitVarInsn(int paramInt1, int paramInt2)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitVarInsn(").append(OPCODES[paramInt1]).append(", ").append(paramInt2).append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitTypeInsn(int paramInt, String paramString)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitTypeInsn(").append(OPCODES[paramInt]).append(", ");
    appendConstant(paramString);
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitFieldInsn(int paramInt, String paramString1, String paramString2, String paramString3)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitFieldInsn(").append(OPCODES[paramInt]).append(", ");
    appendConstant(paramString1);
    this.buf.append(", ");
    appendConstant(paramString2);
    this.buf.append(", ");
    appendConstant(paramString3);
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
  }
  
  /**
   * @deprecated
   */
  public void visitMethodInsn(int paramInt, String paramString1, String paramString2, String paramString3)
  {
    if (this.api >= 327680)
    {
      super.visitMethodInsn(paramInt, paramString1, paramString2, paramString3);
      return;
    }
    doVisitMethodInsn(paramInt, paramString1, paramString2, paramString3, paramInt == 185);
  }
  
  public void visitMethodInsn(int paramInt, String paramString1, String paramString2, String paramString3, boolean paramBoolean)
  {
    if (this.api < 327680)
    {
      super.visitMethodInsn(paramInt, paramString1, paramString2, paramString3, paramBoolean);
      return;
    }
    doVisitMethodInsn(paramInt, paramString1, paramString2, paramString3, paramBoolean);
  }
  
  private void doVisitMethodInsn(int paramInt, String paramString1, String paramString2, String paramString3, boolean paramBoolean)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitMethodInsn(").append(OPCODES[paramInt]).append(", ");
    appendConstant(paramString1);
    this.buf.append(", ");
    appendConstant(paramString2);
    this.buf.append(", ");
    appendConstant(paramString3);
    this.buf.append(", ");
    this.buf.append(paramBoolean ? "true" : "false");
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitInvokeDynamicInsn(String paramString1, String paramString2, Handle paramHandle, Object... paramVarArgs)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitInvokeDynamicInsn(");
    appendConstant(paramString1);
    this.buf.append(", ");
    appendConstant(paramString2);
    this.buf.append(", ");
    appendConstant(paramHandle);
    this.buf.append(", new Object[]{");
    for (int i = 0; i < paramVarArgs.length; i++)
    {
      appendConstant(paramVarArgs[i]);
      if (i != paramVarArgs.length - 1) {
        this.buf.append(", ");
      }
    }
    this.buf.append("});\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitJumpInsn(int paramInt, Label paramLabel)
  {
    this.buf.setLength(0);
    declareLabel(paramLabel);
    this.buf.append(this.name).append(".visitJumpInsn(").append(OPCODES[paramInt]).append(", ");
    appendLabel(paramLabel);
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitLabel(Label paramLabel)
  {
    this.buf.setLength(0);
    declareLabel(paramLabel);
    this.buf.append(this.name).append(".visitLabel(");
    appendLabel(paramLabel);
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitLdcInsn(Object paramObject)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitLdcInsn(");
    appendConstant(paramObject);
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitIincInsn(int paramInt1, int paramInt2)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitIincInsn(").append(paramInt1).append(", ").append(paramInt2).append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitTableSwitchInsn(int paramInt1, int paramInt2, Label paramLabel, Label... paramVarArgs)
  {
    this.buf.setLength(0);
    for (int i = 0; i < paramVarArgs.length; i++) {
      declareLabel(paramVarArgs[i]);
    }
    declareLabel(paramLabel);
    this.buf.append(this.name).append(".visitTableSwitchInsn(").append(paramInt1).append(", ").append(paramInt2).append(", ");
    appendLabel(paramLabel);
    this.buf.append(", new Label[] {");
    for (i = 0; i < paramVarArgs.length; i++)
    {
      this.buf.append(i == 0 ? " " : ", ");
      appendLabel(paramVarArgs[i]);
    }
    this.buf.append(" });\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitLookupSwitchInsn(Label paramLabel, int[] paramArrayOfInt, Label[] paramArrayOfLabel)
  {
    this.buf.setLength(0);
    for (int i = 0; i < paramArrayOfLabel.length; i++) {
      declareLabel(paramArrayOfLabel[i]);
    }
    declareLabel(paramLabel);
    this.buf.append(this.name).append(".visitLookupSwitchInsn(");
    appendLabel(paramLabel);
    this.buf.append(", new int[] {");
    for (i = 0; i < paramArrayOfInt.length; i++) {
      this.buf.append(i == 0 ? " " : ", ").append(paramArrayOfInt[i]);
    }
    this.buf.append(" }, new Label[] {");
    for (i = 0; i < paramArrayOfLabel.length; i++)
    {
      this.buf.append(i == 0 ? " " : ", ");
      appendLabel(paramArrayOfLabel[i]);
    }
    this.buf.append(" });\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitMultiANewArrayInsn(String paramString, int paramInt)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitMultiANewArrayInsn(");
    appendConstant(paramString);
    this.buf.append(", ").append(paramInt).append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public ASMifier visitInsnAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return visitTypeAnnotation("visitInsnAnnotation", paramInt, paramTypePath, paramString, paramBoolean);
  }
  
  public void visitTryCatchBlock(Label paramLabel1, Label paramLabel2, Label paramLabel3, String paramString)
  {
    this.buf.setLength(0);
    declareLabel(paramLabel1);
    declareLabel(paramLabel2);
    declareLabel(paramLabel3);
    this.buf.append(this.name).append(".visitTryCatchBlock(");
    appendLabel(paramLabel1);
    this.buf.append(", ");
    appendLabel(paramLabel2);
    this.buf.append(", ");
    appendLabel(paramLabel3);
    this.buf.append(", ");
    appendConstant(paramString);
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public ASMifier visitTryCatchAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return visitTypeAnnotation("visitTryCatchAnnotation", paramInt, paramTypePath, paramString, paramBoolean);
  }
  
  public void visitLocalVariable(String paramString1, String paramString2, String paramString3, Label paramLabel1, Label paramLabel2, int paramInt)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitLocalVariable(");
    appendConstant(paramString1);
    this.buf.append(", ");
    appendConstant(paramString2);
    this.buf.append(", ");
    appendConstant(paramString3);
    this.buf.append(", ");
    appendLabel(paramLabel1);
    this.buf.append(", ");
    appendLabel(paramLabel2);
    this.buf.append(", ").append(paramInt).append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public Printer visitLocalVariableAnnotation(int paramInt, TypePath paramTypePath, Label[] paramArrayOfLabel1, Label[] paramArrayOfLabel2, int[] paramArrayOfInt, String paramString, boolean paramBoolean)
  {
    this.buf.setLength(0);
    this.buf.append("{\n").append("av0 = ").append(this.name).append(".visitLocalVariableAnnotation(");
    this.buf.append(paramInt);
    this.buf.append(", TypePath.fromString(\"").append(paramTypePath).append("\"), ");
    this.buf.append("new Label[] {");
    for (int i = 0; i < paramArrayOfLabel1.length; i++)
    {
      this.buf.append(i == 0 ? " " : ", ");
      appendLabel(paramArrayOfLabel1[i]);
    }
    this.buf.append(" }, new Label[] {");
    for (i = 0; i < paramArrayOfLabel2.length; i++)
    {
      this.buf.append(i == 0 ? " " : ", ");
      appendLabel(paramArrayOfLabel2[i]);
    }
    this.buf.append(" }, new int[] {");
    for (i = 0; i < paramArrayOfInt.length; i++) {
      this.buf.append(i == 0 ? " " : ", ").append(paramArrayOfInt[i]);
    }
    this.buf.append(" }, ");
    appendConstant(paramString);
    this.buf.append(", ").append(paramBoolean).append(");\n");
    this.text.add(this.buf.toString());
    ASMifier localASMifier = createASMifier("av", 0);
    this.text.add(localASMifier.getText());
    this.text.add("}\n");
    return localASMifier;
  }
  
  public void visitLineNumber(int paramInt, Label paramLabel)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitLineNumber(").append(paramInt).append(", ");
    appendLabel(paramLabel);
    this.buf.append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitMaxs(int paramInt1, int paramInt2)
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitMaxs(").append(paramInt1).append(", ").append(paramInt2).append(");\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitMethodEnd()
  {
    this.buf.setLength(0);
    this.buf.append(this.name).append(".visitEnd();\n");
    this.text.add(this.buf.toString());
  }
  
  public ASMifier visitAnnotation(String paramString, boolean paramBoolean)
  {
    this.buf.setLength(0);
    this.buf.append("{\n").append("av0 = ").append(this.name).append(".visitAnnotation(");
    appendConstant(paramString);
    this.buf.append(", ").append(paramBoolean).append(");\n");
    this.text.add(this.buf.toString());
    ASMifier localASMifier = createASMifier("av", 0);
    this.text.add(localASMifier.getText());
    this.text.add("}\n");
    return localASMifier;
  }
  
  public ASMifier visitTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return visitTypeAnnotation("visitTypeAnnotation", paramInt, paramTypePath, paramString, paramBoolean);
  }
  
  public ASMifier visitTypeAnnotation(String paramString1, int paramInt, TypePath paramTypePath, String paramString2, boolean paramBoolean)
  {
    this.buf.setLength(0);
    this.buf.append("{\n").append("av0 = ").append(this.name).append(".").append(paramString1).append("(");
    this.buf.append(paramInt);
    this.buf.append(", TypePath.fromString(\"").append(paramTypePath).append("\"), ");
    appendConstant(paramString2);
    this.buf.append(", ").append(paramBoolean).append(");\n");
    this.text.add(this.buf.toString());
    ASMifier localASMifier = createASMifier("av", 0);
    this.text.add(localASMifier.getText());
    this.text.add("}\n");
    return localASMifier;
  }
  
  public void visitAttribute(Attribute paramAttribute)
  {
    this.buf.setLength(0);
    this.buf.append("// ATTRIBUTE ").append(paramAttribute.type).append('\n');
    if ((paramAttribute instanceof ASMifiable))
    {
      if (this.labelNames == null) {
        this.labelNames = new HashMap();
      }
      this.buf.append("{\n");
      ((ASMifiable)paramAttribute).asmify(this.buf, "attr", this.labelNames);
      this.buf.append(this.name).append(".visitAttribute(attr);\n");
      this.buf.append("}\n");
    }
    this.text.add(this.buf.toString());
  }
  
  protected ASMifier createASMifier(String paramString, int paramInt)
  {
    return new ASMifier(327680, paramString, paramInt);
  }
  
  void appendAccess(int paramInt)
  {
    int i = 1;
    if ((paramInt & 0x1) != 0)
    {
      this.buf.append("ACC_PUBLIC");
      i = 0;
    }
    if ((paramInt & 0x2) != 0)
    {
      this.buf.append("ACC_PRIVATE");
      i = 0;
    }
    if ((paramInt & 0x4) != 0)
    {
      this.buf.append("ACC_PROTECTED");
      i = 0;
    }
    if ((paramInt & 0x10) != 0)
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_FINAL");
      i = 0;
    }
    if ((paramInt & 0x8) != 0)
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_STATIC");
      i = 0;
    }
    if ((paramInt & 0x20) != 0)
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      if ((paramInt & 0x40000) == 0) {
        this.buf.append("ACC_SYNCHRONIZED");
      } else {
        this.buf.append("ACC_SUPER");
      }
      i = 0;
    }
    if (((paramInt & 0x40) != 0) && ((paramInt & 0x80000) != 0))
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_VOLATILE");
      i = 0;
    }
    if (((paramInt & 0x40) != 0) && ((paramInt & 0x40000) == 0) && ((paramInt & 0x80000) == 0))
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_BRIDGE");
      i = 0;
    }
    if (((paramInt & 0x80) != 0) && ((paramInt & 0x40000) == 0) && ((paramInt & 0x80000) == 0))
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_VARARGS");
      i = 0;
    }
    if (((paramInt & 0x80) != 0) && ((paramInt & 0x80000) != 0))
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_TRANSIENT");
      i = 0;
    }
    if (((paramInt & 0x100) != 0) && ((paramInt & 0x40000) == 0) && ((paramInt & 0x80000) == 0))
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_NATIVE");
      i = 0;
    }
    if (((paramInt & 0x4000) != 0) && (((paramInt & 0x40000) != 0) || ((paramInt & 0x80000) != 0) || ((paramInt & 0x100000) != 0)))
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_ENUM");
      i = 0;
    }
    if (((paramInt & 0x2000) != 0) && (((paramInt & 0x40000) != 0) || ((paramInt & 0x100000) != 0)))
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_ANNOTATION");
      i = 0;
    }
    if ((paramInt & 0x400) != 0)
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_ABSTRACT");
      i = 0;
    }
    if ((paramInt & 0x200) != 0)
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_INTERFACE");
      i = 0;
    }
    if ((paramInt & 0x800) != 0)
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_STRICT");
      i = 0;
    }
    if ((paramInt & 0x1000) != 0)
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_SYNTHETIC");
      i = 0;
    }
    if ((paramInt & 0x20000) != 0)
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_DEPRECATED");
      i = 0;
    }
    if ((paramInt & 0x8000) != 0)
    {
      if (i == 0) {
        this.buf.append(" + ");
      }
      this.buf.append("ACC_MANDATED");
      i = 0;
    }
    if (i != 0) {
      this.buf.append('0');
    }
  }
  
  protected void appendConstant(Object paramObject)
  {
    appendConstant(this.buf, paramObject);
  }
  
  static void appendConstant(StringBuffer paramStringBuffer, Object paramObject)
  {
    if (paramObject == null)
    {
      paramStringBuffer.append("null");
    }
    else if ((paramObject instanceof String))
    {
      appendString(paramStringBuffer, (String)paramObject);
    }
    else if ((paramObject instanceof Type))
    {
      paramStringBuffer.append("Type.getType(\"");
      paramStringBuffer.append(((Type)paramObject).getDescriptor());
      paramStringBuffer.append("\")");
    }
    else if ((paramObject instanceof Handle))
    {
      paramStringBuffer.append("new Handle(");
      Handle localHandle = (Handle)paramObject;
      paramStringBuffer.append("Opcodes.").append(HANDLE_TAG[localHandle.getTag()]).append(", \"");
      paramStringBuffer.append(localHandle.getOwner()).append("\", \"");
      paramStringBuffer.append(localHandle.getName()).append("\", \"");
      paramStringBuffer.append(localHandle.getDesc()).append("\")");
    }
    else if ((paramObject instanceof Byte))
    {
      paramStringBuffer.append("new Byte((byte)").append(paramObject).append(')');
    }
    else if ((paramObject instanceof Boolean))
    {
      paramStringBuffer.append(((Boolean)paramObject).booleanValue() ? "Boolean.TRUE" : "Boolean.FALSE");
    }
    else if ((paramObject instanceof Short))
    {
      paramStringBuffer.append("new Short((short)").append(paramObject).append(')');
    }
    else if ((paramObject instanceof Character))
    {
      int i = ((Character)paramObject).charValue();
      paramStringBuffer.append("new Character((char)").append(i).append(')');
    }
    else if ((paramObject instanceof Integer))
    {
      paramStringBuffer.append("new Integer(").append(paramObject).append(')');
    }
    else if ((paramObject instanceof Float))
    {
      paramStringBuffer.append("new Float(\"").append(paramObject).append("\")");
    }
    else if ((paramObject instanceof Long))
    {
      paramStringBuffer.append("new Long(").append(paramObject).append("L)");
    }
    else if ((paramObject instanceof Double))
    {
      paramStringBuffer.append("new Double(\"").append(paramObject).append("\")");
    }
    else
    {
      Object localObject;
      int j;
      if ((paramObject instanceof byte[]))
      {
        localObject = (byte[])paramObject;
        paramStringBuffer.append("new byte[] {");
        for (j = 0; j < localObject.length; j++) {
          paramStringBuffer.append(j == 0 ? "" : ",").append(localObject[j]);
        }
        paramStringBuffer.append('}');
      }
      else if ((paramObject instanceof boolean[]))
      {
        localObject = (boolean[])paramObject;
        paramStringBuffer.append("new boolean[] {");
        for (j = 0; j < localObject.length; j++) {
          paramStringBuffer.append(j == 0 ? "" : ",").append(localObject[j]);
        }
        paramStringBuffer.append('}');
      }
      else if ((paramObject instanceof short[]))
      {
        localObject = (short[])paramObject;
        paramStringBuffer.append("new short[] {");
        for (j = 0; j < localObject.length; j++) {
          paramStringBuffer.append(j == 0 ? "" : ",").append("(short)").append(localObject[j]);
        }
        paramStringBuffer.append('}');
      }
      else if ((paramObject instanceof char[]))
      {
        localObject = (char[])paramObject;
        paramStringBuffer.append("new char[] {");
        for (j = 0; j < localObject.length; j++) {
          paramStringBuffer.append(j == 0 ? "" : ",").append("(char)").append(localObject[j]);
        }
        paramStringBuffer.append('}');
      }
      else if ((paramObject instanceof int[]))
      {
        localObject = (int[])paramObject;
        paramStringBuffer.append("new int[] {");
        for (j = 0; j < localObject.length; j++) {
          paramStringBuffer.append(j == 0 ? "" : ",").append(localObject[j]);
        }
        paramStringBuffer.append('}');
      }
      else if ((paramObject instanceof long[]))
      {
        localObject = (long[])paramObject;
        paramStringBuffer.append("new long[] {");
        for (j = 0; j < localObject.length; j++) {
          paramStringBuffer.append(j == 0 ? "" : ",").append(localObject[j]).append('L');
        }
        paramStringBuffer.append('}');
      }
      else if ((paramObject instanceof float[]))
      {
        localObject = (float[])paramObject;
        paramStringBuffer.append("new float[] {");
        for (j = 0; j < localObject.length; j++) {
          paramStringBuffer.append(j == 0 ? "" : ",").append(localObject[j]).append('f');
        }
        paramStringBuffer.append('}');
      }
      else if ((paramObject instanceof double[]))
      {
        localObject = (double[])paramObject;
        paramStringBuffer.append("new double[] {");
        for (j = 0; j < localObject.length; j++) {
          paramStringBuffer.append(j == 0 ? "" : ",").append(localObject[j]).append('d');
        }
        paramStringBuffer.append('}');
      }
    }
  }
  
  private void declareFrameTypes(int paramInt, Object[] paramArrayOfObject)
  {
    for (int i = 0; i < paramInt; i++) {
      if ((paramArrayOfObject[i] instanceof Label)) {
        declareLabel((Label)paramArrayOfObject[i]);
      }
    }
  }
  
  private void appendFrameTypes(int paramInt, Object[] paramArrayOfObject)
  {
    for (int i = 0; i < paramInt; i++)
    {
      if (i > 0) {
        this.buf.append(", ");
      }
      if ((paramArrayOfObject[i] instanceof String)) {
        appendConstant(paramArrayOfObject[i]);
      } else if ((paramArrayOfObject[i] instanceof Integer)) {
        switch (((Integer)paramArrayOfObject[i]).intValue())
        {
        case 0: 
          this.buf.append("Opcodes.TOP");
          break;
        case 1: 
          this.buf.append("Opcodes.INTEGER");
          break;
        case 2: 
          this.buf.append("Opcodes.FLOAT");
          break;
        case 3: 
          this.buf.append("Opcodes.DOUBLE");
          break;
        case 4: 
          this.buf.append("Opcodes.LONG");
          break;
        case 5: 
          this.buf.append("Opcodes.NULL");
          break;
        case 6: 
          this.buf.append("Opcodes.UNINITIALIZED_THIS");
        }
      } else {
        appendLabel((Label)paramArrayOfObject[i]);
      }
    }
  }
  
  protected void declareLabel(Label paramLabel)
  {
    if (this.labelNames == null) {
      this.labelNames = new HashMap();
    }
    String str = (String)this.labelNames.get(paramLabel);
    if (str == null)
    {
      str = "l" + this.labelNames.size();
      this.labelNames.put(paramLabel, str);
      this.buf.append("Label ").append(str).append(" = new Label();\n");
    }
  }
  
  protected void appendLabel(Label paramLabel)
  {
    this.buf.append((String)this.labelNames.get(paramLabel));
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
