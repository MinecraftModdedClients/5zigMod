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
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

public class Textifier
  extends Printer
{
  public static final int INTERNAL_NAME = 0;
  public static final int FIELD_DESCRIPTOR = 1;
  public static final int FIELD_SIGNATURE = 2;
  public static final int METHOD_DESCRIPTOR = 3;
  public static final int METHOD_SIGNATURE = 4;
  public static final int CLASS_SIGNATURE = 5;
  public static final int TYPE_DECLARATION = 6;
  public static final int CLASS_DECLARATION = 7;
  public static final int PARAMETERS_DECLARATION = 8;
  public static final int HANDLE_DESCRIPTOR = 9;
  protected String tab = "  ";
  protected String tab2 = "    ";
  protected String tab3 = "      ";
  protected String ltab = "   ";
  protected Map labelNames;
  private int access;
  private int valueNumber = 0;
  static Class class$org$objectweb$asm$util$Textifier = class$("org.objectweb.asm.util.Textifier");
  
  public Textifier()
  {
    this(327680);
    if (getClass() != class$org$objectweb$asm$util$Textifier) {
      throw new IllegalStateException();
    }
  }
  
  protected Textifier(int paramInt)
  {
    super(paramInt);
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
      System.err.println("Prints a disassembled view of the given class.");
      System.err.println("Usage: Textifier [-debug] <fully qualified class name or class file name>");
      return;
    }
    ClassReader localClassReader;
    if ((paramArrayOfString[i].endsWith(".class")) || (paramArrayOfString[i].indexOf('\\') > -1) || (paramArrayOfString[i].indexOf('/') > -1)) {
      localClassReader = new ClassReader(new FileInputStream(paramArrayOfString[i]));
    } else {
      localClassReader = new ClassReader(paramArrayOfString[i]);
    }
    localClassReader.accept(new TraceClassVisitor(new PrintWriter(System.out)), j);
  }
  
  public void visit(int paramInt1, int paramInt2, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    this.access = paramInt2;
    int i = paramInt1 & 0xFFFF;
    int j = paramInt1 >>> 16;
    this.buf.setLength(0);
    this.buf.append("// class version ").append(i).append('.').append(j).append(" (").append(paramInt1).append(")\n");
    if ((paramInt2 & 0x20000) != 0) {
      this.buf.append("// DEPRECATED\n");
    }
    this.buf.append("// access flags 0x").append(Integer.toHexString(paramInt2).toUpperCase()).append('\n');
    appendDescriptor(5, paramString2);
    if (paramString2 != null)
    {
      TraceSignatureVisitor localTraceSignatureVisitor = new TraceSignatureVisitor(paramInt2);
      SignatureReader localSignatureReader = new SignatureReader(paramString2);
      localSignatureReader.accept(localTraceSignatureVisitor);
      this.buf.append("// declaration: ").append(paramString1).append(localTraceSignatureVisitor.getDeclaration()).append('\n');
    }
    appendAccess(paramInt2 & 0xFFFFFFDF);
    if ((paramInt2 & 0x2000) != 0) {
      this.buf.append("@interface ");
    } else if ((paramInt2 & 0x200) != 0) {
      this.buf.append("interface ");
    } else if ((paramInt2 & 0x4000) == 0) {
      this.buf.append("class ");
    }
    appendDescriptor(0, paramString1);
    if ((paramString3 != null) && (!"java/lang/Object".equals(paramString3)))
    {
      this.buf.append(" extends ");
      appendDescriptor(0, paramString3);
      this.buf.append(' ');
    }
    if ((paramArrayOfString != null) && (paramArrayOfString.length > 0))
    {
      this.buf.append(" implements ");
      for (int k = 0; k < paramArrayOfString.length; k++)
      {
        appendDescriptor(0, paramArrayOfString[k]);
        this.buf.append(' ');
      }
    }
    this.buf.append(" {\n\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitSource(String paramString1, String paramString2)
  {
    this.buf.setLength(0);
    if (paramString1 != null) {
      this.buf.append(this.tab).append("// compiled from: ").append(paramString1).append('\n');
    }
    if (paramString2 != null) {
      this.buf.append(this.tab).append("// debug info: ").append(paramString2).append('\n');
    }
    if (this.buf.length() > 0) {
      this.text.add(this.buf.toString());
    }
  }
  
  public void visitOuterClass(String paramString1, String paramString2, String paramString3)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab).append("OUTERCLASS ");
    appendDescriptor(0, paramString1);
    this.buf.append(' ');
    if (paramString2 != null) {
      this.buf.append(paramString2).append(' ');
    }
    appendDescriptor(3, paramString3);
    this.buf.append('\n');
    this.text.add(this.buf.toString());
  }
  
  public Textifier visitClassAnnotation(String paramString, boolean paramBoolean)
  {
    this.text.add("\n");
    return visitAnnotation(paramString, paramBoolean);
  }
  
  public Printer visitClassTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    this.text.add("\n");
    return visitTypeAnnotation(paramInt, paramTypePath, paramString, paramBoolean);
  }
  
  public void visitClassAttribute(Attribute paramAttribute)
  {
    this.text.add("\n");
    visitAttribute(paramAttribute);
  }
  
  public void visitInnerClass(String paramString1, String paramString2, String paramString3, int paramInt)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab).append("// access flags 0x");
    this.buf.append(Integer.toHexString(paramInt & 0xFFFFFFDF).toUpperCase()).append('\n');
    this.buf.append(this.tab);
    appendAccess(paramInt);
    this.buf.append("INNERCLASS ");
    appendDescriptor(0, paramString1);
    this.buf.append(' ');
    appendDescriptor(0, paramString2);
    this.buf.append(' ');
    appendDescriptor(0, paramString3);
    this.buf.append('\n');
    this.text.add(this.buf.toString());
  }
  
  public Textifier visitField(int paramInt, String paramString1, String paramString2, String paramString3, Object paramObject)
  {
    this.buf.setLength(0);
    this.buf.append('\n');
    if ((paramInt & 0x20000) != 0) {
      this.buf.append(this.tab).append("// DEPRECATED\n");
    }
    this.buf.append(this.tab).append("// access flags 0x").append(Integer.toHexString(paramInt).toUpperCase()).append('\n');
    if (paramString3 != null)
    {
      this.buf.append(this.tab);
      appendDescriptor(2, paramString3);
      localObject = new TraceSignatureVisitor(0);
      SignatureReader localSignatureReader = new SignatureReader(paramString3);
      localSignatureReader.acceptType((SignatureVisitor)localObject);
      this.buf.append(this.tab).append("// declaration: ").append(((TraceSignatureVisitor)localObject).getDeclaration()).append('\n');
    }
    this.buf.append(this.tab);
    appendAccess(paramInt);
    appendDescriptor(1, paramString2);
    this.buf.append(' ').append(paramString1);
    if (paramObject != null)
    {
      this.buf.append(" = ");
      if ((paramObject instanceof String)) {
        this.buf.append('"').append(paramObject).append('"');
      } else {
        this.buf.append(paramObject);
      }
    }
    this.buf.append('\n');
    this.text.add(this.buf.toString());
    Object localObject = createTextifier();
    this.text.add(((Textifier)localObject).getText());
    return (Textifier)localObject;
  }
  
  public Textifier visitMethod(int paramInt, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    this.buf.setLength(0);
    this.buf.append('\n');
    if ((paramInt & 0x20000) != 0) {
      this.buf.append(this.tab).append("// DEPRECATED\n");
    }
    this.buf.append(this.tab).append("// access flags 0x").append(Integer.toHexString(paramInt).toUpperCase()).append('\n');
    if (paramString3 != null)
    {
      this.buf.append(this.tab);
      appendDescriptor(4, paramString3);
      TraceSignatureVisitor localTraceSignatureVisitor = new TraceSignatureVisitor(0);
      SignatureReader localSignatureReader = new SignatureReader(paramString3);
      localSignatureReader.accept(localTraceSignatureVisitor);
      String str1 = localTraceSignatureVisitor.getDeclaration();
      String str2 = localTraceSignatureVisitor.getReturnType();
      String str3 = localTraceSignatureVisitor.getExceptions();
      this.buf.append(this.tab).append("// declaration: ").append(str2).append(' ').append(paramString1).append(str1);
      if (str3 != null) {
        this.buf.append(" throws ").append(str3);
      }
      this.buf.append('\n');
    }
    this.buf.append(this.tab);
    appendAccess(paramInt & 0xFFFFFFBF);
    if ((paramInt & 0x100) != 0) {
      this.buf.append("native ");
    }
    if ((paramInt & 0x80) != 0) {
      this.buf.append("varargs ");
    }
    if ((paramInt & 0x40) != 0) {
      this.buf.append("bridge ");
    }
    if (((this.access & 0x200) != 0) && ((paramInt & 0x400) == 0) && ((paramInt & 0x8) == 0)) {
      this.buf.append("default ");
    }
    this.buf.append(paramString1);
    appendDescriptor(3, paramString2);
    if ((paramArrayOfString != null) && (paramArrayOfString.length > 0))
    {
      this.buf.append(" throws ");
      for (int i = 0; i < paramArrayOfString.length; i++)
      {
        appendDescriptor(0, paramArrayOfString[i]);
        this.buf.append(' ');
      }
    }
    this.buf.append('\n');
    this.text.add(this.buf.toString());
    Textifier localTextifier = createTextifier();
    this.text.add(localTextifier.getText());
    return localTextifier;
  }
  
  public void visitClassEnd()
  {
    this.text.add("}\n");
  }
  
  public void visit(String paramString, Object paramObject)
  {
    this.buf.setLength(0);
    appendComa(this.valueNumber++);
    if (paramString != null) {
      this.buf.append(paramString).append('=');
    }
    if ((paramObject instanceof String))
    {
      visitString((String)paramObject);
    }
    else if ((paramObject instanceof Type))
    {
      visitType((Type)paramObject);
    }
    else if ((paramObject instanceof Byte))
    {
      visitByte(((Byte)paramObject).byteValue());
    }
    else if ((paramObject instanceof Boolean))
    {
      visitBoolean(((Boolean)paramObject).booleanValue());
    }
    else if ((paramObject instanceof Short))
    {
      visitShort(((Short)paramObject).shortValue());
    }
    else if ((paramObject instanceof Character))
    {
      visitChar(((Character)paramObject).charValue());
    }
    else if ((paramObject instanceof Integer))
    {
      visitInt(((Integer)paramObject).intValue());
    }
    else if ((paramObject instanceof Float))
    {
      visitFloat(((Float)paramObject).floatValue());
    }
    else if ((paramObject instanceof Long))
    {
      visitLong(((Long)paramObject).longValue());
    }
    else if ((paramObject instanceof Double))
    {
      visitDouble(((Double)paramObject).doubleValue());
    }
    else if (paramObject.getClass().isArray())
    {
      this.buf.append('{');
      Object localObject;
      int i;
      if ((paramObject instanceof byte[]))
      {
        localObject = (byte[])paramObject;
        for (i = 0; i < localObject.length; i++)
        {
          appendComa(i);
          visitByte(localObject[i]);
        }
      }
      else if ((paramObject instanceof boolean[]))
      {
        localObject = (boolean[])paramObject;
        for (i = 0; i < localObject.length; i++)
        {
          appendComa(i);
          visitBoolean(localObject[i]);
        }
      }
      else if ((paramObject instanceof short[]))
      {
        localObject = (short[])paramObject;
        for (i = 0; i < localObject.length; i++)
        {
          appendComa(i);
          visitShort(localObject[i]);
        }
      }
      else if ((paramObject instanceof char[]))
      {
        localObject = (char[])paramObject;
        for (i = 0; i < localObject.length; i++)
        {
          appendComa(i);
          visitChar(localObject[i]);
        }
      }
      else if ((paramObject instanceof int[]))
      {
        localObject = (int[])paramObject;
        for (i = 0; i < localObject.length; i++)
        {
          appendComa(i);
          visitInt(localObject[i]);
        }
      }
      else if ((paramObject instanceof long[]))
      {
        localObject = (long[])paramObject;
        for (i = 0; i < localObject.length; i++)
        {
          appendComa(i);
          visitLong(localObject[i]);
        }
      }
      else if ((paramObject instanceof float[]))
      {
        localObject = (float[])paramObject;
        for (i = 0; i < localObject.length; i++)
        {
          appendComa(i);
          visitFloat(localObject[i]);
        }
      }
      else if ((paramObject instanceof double[]))
      {
        localObject = (double[])paramObject;
        for (i = 0; i < localObject.length; i++)
        {
          appendComa(i);
          visitDouble(localObject[i]);
        }
      }
      this.buf.append('}');
    }
    this.text.add(this.buf.toString());
  }
  
  private void visitInt(int paramInt)
  {
    this.buf.append(paramInt);
  }
  
  private void visitLong(long paramLong)
  {
    this.buf.append(paramLong).append('L');
  }
  
  private void visitFloat(float paramFloat)
  {
    this.buf.append(paramFloat).append('F');
  }
  
  private void visitDouble(double paramDouble)
  {
    this.buf.append(paramDouble).append('D');
  }
  
  private void visitChar(char paramChar)
  {
    this.buf.append("(char)").append(paramChar);
  }
  
  private void visitShort(short paramShort)
  {
    this.buf.append("(short)").append(paramShort);
  }
  
  private void visitByte(byte paramByte)
  {
    this.buf.append("(byte)").append(paramByte);
  }
  
  private void visitBoolean(boolean paramBoolean)
  {
    this.buf.append(paramBoolean);
  }
  
  private void visitString(String paramString)
  {
    appendString(this.buf, paramString);
  }
  
  private void visitType(Type paramType)
  {
    this.buf.append(paramType.getClassName()).append(".class");
  }
  
  public void visitEnum(String paramString1, String paramString2, String paramString3)
  {
    this.buf.setLength(0);
    appendComa(this.valueNumber++);
    if (paramString1 != null) {
      this.buf.append(paramString1).append('=');
    }
    appendDescriptor(1, paramString2);
    this.buf.append('.').append(paramString3);
    this.text.add(this.buf.toString());
  }
  
  public Textifier visitAnnotation(String paramString1, String paramString2)
  {
    this.buf.setLength(0);
    appendComa(this.valueNumber++);
    if (paramString1 != null) {
      this.buf.append(paramString1).append('=');
    }
    this.buf.append('@');
    appendDescriptor(1, paramString2);
    this.buf.append('(');
    this.text.add(this.buf.toString());
    Textifier localTextifier = createTextifier();
    this.text.add(localTextifier.getText());
    this.text.add(")");
    return localTextifier;
  }
  
  public Textifier visitArray(String paramString)
  {
    this.buf.setLength(0);
    appendComa(this.valueNumber++);
    if (paramString != null) {
      this.buf.append(paramString).append('=');
    }
    this.buf.append('{');
    this.text.add(this.buf.toString());
    Textifier localTextifier = createTextifier();
    this.text.add(localTextifier.getText());
    this.text.add("}");
    return localTextifier;
  }
  
  public void visitAnnotationEnd() {}
  
  public Textifier visitFieldAnnotation(String paramString, boolean paramBoolean)
  {
    return visitAnnotation(paramString, paramBoolean);
  }
  
  public Printer visitFieldTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return visitTypeAnnotation(paramInt, paramTypePath, paramString, paramBoolean);
  }
  
  public void visitFieldAttribute(Attribute paramAttribute)
  {
    visitAttribute(paramAttribute);
  }
  
  public void visitFieldEnd() {}
  
  public void visitParameter(String paramString, int paramInt)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("// parameter ");
    appendAccess(paramInt);
    this.buf.append(' ').append(paramString == null ? "<no name>" : paramString).append('\n');
    this.text.add(this.buf.toString());
  }
  
  public Textifier visitAnnotationDefault()
  {
    this.text.add(this.tab2 + "default=");
    Textifier localTextifier = createTextifier();
    this.text.add(localTextifier.getText());
    this.text.add("\n");
    return localTextifier;
  }
  
  public Textifier visitMethodAnnotation(String paramString, boolean paramBoolean)
  {
    return visitAnnotation(paramString, paramBoolean);
  }
  
  public Printer visitMethodTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return visitTypeAnnotation(paramInt, paramTypePath, paramString, paramBoolean);
  }
  
  public Textifier visitParameterAnnotation(int paramInt, String paramString, boolean paramBoolean)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append('@');
    appendDescriptor(1, paramString);
    this.buf.append('(');
    this.text.add(this.buf.toString());
    Textifier localTextifier = createTextifier();
    this.text.add(localTextifier.getText());
    this.text.add(paramBoolean ? ") // parameter " : ") // invisible, parameter ");
    this.text.add(new Integer(paramInt));
    this.text.add("\n");
    return localTextifier;
  }
  
  public void visitMethodAttribute(Attribute paramAttribute)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab).append("ATTRIBUTE ");
    appendDescriptor(-1, paramAttribute.type);
    if ((paramAttribute instanceof Textifiable)) {
      ((Textifiable)paramAttribute).textify(this.buf, this.labelNames);
    } else {
      this.buf.append(" : unknown\n");
    }
    this.text.add(this.buf.toString());
  }
  
  public void visitCode() {}
  
  public void visitFrame(int paramInt1, int paramInt2, Object[] paramArrayOfObject1, int paramInt3, Object[] paramArrayOfObject2)
  {
    this.buf.setLength(0);
    this.buf.append(this.ltab);
    this.buf.append("FRAME ");
    switch (paramInt1)
    {
    case -1: 
    case 0: 
      this.buf.append("FULL [");
      appendFrameTypes(paramInt2, paramArrayOfObject1);
      this.buf.append("] [");
      appendFrameTypes(paramInt3, paramArrayOfObject2);
      this.buf.append(']');
      break;
    case 1: 
      this.buf.append("APPEND [");
      appendFrameTypes(paramInt2, paramArrayOfObject1);
      this.buf.append(']');
      break;
    case 2: 
      this.buf.append("CHOP ").append(paramInt2);
      break;
    case 3: 
      this.buf.append("SAME");
      break;
    case 4: 
      this.buf.append("SAME1 ");
      appendFrameTypes(1, paramArrayOfObject2);
    }
    this.buf.append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitInsn(int paramInt)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append(OPCODES[paramInt]).append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitIntInsn(int paramInt1, int paramInt2)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append(OPCODES[paramInt1]).append(' ').append(paramInt1 == 188 ? TYPES[paramInt2] : Integer.toString(paramInt2)).append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitVarInsn(int paramInt1, int paramInt2)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append(OPCODES[paramInt1]).append(' ').append(paramInt2).append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitTypeInsn(int paramInt, String paramString)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append(OPCODES[paramInt]).append(' ');
    appendDescriptor(0, paramString);
    this.buf.append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitFieldInsn(int paramInt, String paramString1, String paramString2, String paramString3)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append(OPCODES[paramInt]).append(' ');
    appendDescriptor(0, paramString1);
    this.buf.append('.').append(paramString2).append(" : ");
    appendDescriptor(1, paramString3);
    this.buf.append('\n');
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
    this.buf.append(this.tab2).append(OPCODES[paramInt]).append(' ');
    appendDescriptor(0, paramString1);
    this.buf.append('.').append(paramString2).append(' ');
    appendDescriptor(3, paramString3);
    this.buf.append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitInvokeDynamicInsn(String paramString1, String paramString2, Handle paramHandle, Object... paramVarArgs)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("INVOKEDYNAMIC").append(' ');
    this.buf.append(paramString1);
    appendDescriptor(3, paramString2);
    this.buf.append(" [");
    this.buf.append('\n');
    this.buf.append(this.tab3);
    appendHandle(paramHandle);
    this.buf.append('\n');
    this.buf.append(this.tab3).append("// arguments:");
    if (paramVarArgs.length == 0)
    {
      this.buf.append(" none");
    }
    else
    {
      this.buf.append('\n');
      for (int i = 0; i < paramVarArgs.length; i++)
      {
        this.buf.append(this.tab3);
        Object localObject = paramVarArgs[i];
        if ((localObject instanceof String))
        {
          Printer.appendString(this.buf, (String)localObject);
        }
        else if ((localObject instanceof Type))
        {
          Type localType = (Type)localObject;
          if (localType.getSort() == 11) {
            appendDescriptor(3, localType.getDescriptor());
          } else {
            this.buf.append(localType.getDescriptor()).append(".class");
          }
        }
        else if ((localObject instanceof Handle))
        {
          appendHandle((Handle)localObject);
        }
        else
        {
          this.buf.append(localObject);
        }
        this.buf.append(", \n");
      }
      this.buf.setLength(this.buf.length() - 3);
    }
    this.buf.append('\n');
    this.buf.append(this.tab2).append("]\n");
    this.text.add(this.buf.toString());
  }
  
  public void visitJumpInsn(int paramInt, Label paramLabel)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append(OPCODES[paramInt]).append(' ');
    appendLabel(paramLabel);
    this.buf.append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitLabel(Label paramLabel)
  {
    this.buf.setLength(0);
    this.buf.append(this.ltab);
    appendLabel(paramLabel);
    this.buf.append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitLdcInsn(Object paramObject)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("LDC ");
    if ((paramObject instanceof String)) {
      Printer.appendString(this.buf, (String)paramObject);
    } else if ((paramObject instanceof Type)) {
      this.buf.append(((Type)paramObject).getDescriptor()).append(".class");
    } else {
      this.buf.append(paramObject);
    }
    this.buf.append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitIincInsn(int paramInt1, int paramInt2)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("IINC ").append(paramInt1).append(' ').append(paramInt2).append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitTableSwitchInsn(int paramInt1, int paramInt2, Label paramLabel, Label... paramVarArgs)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("TABLESWITCH\n");
    for (int i = 0; i < paramVarArgs.length; i++)
    {
      this.buf.append(this.tab3).append(paramInt1 + i).append(": ");
      appendLabel(paramVarArgs[i]);
      this.buf.append('\n');
    }
    this.buf.append(this.tab3).append("default: ");
    appendLabel(paramLabel);
    this.buf.append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitLookupSwitchInsn(Label paramLabel, int[] paramArrayOfInt, Label[] paramArrayOfLabel)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("LOOKUPSWITCH\n");
    for (int i = 0; i < paramArrayOfLabel.length; i++)
    {
      this.buf.append(this.tab3).append(paramArrayOfInt[i]).append(": ");
      appendLabel(paramArrayOfLabel[i]);
      this.buf.append('\n');
    }
    this.buf.append(this.tab3).append("default: ");
    appendLabel(paramLabel);
    this.buf.append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitMultiANewArrayInsn(String paramString, int paramInt)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("MULTIANEWARRAY ");
    appendDescriptor(1, paramString);
    this.buf.append(' ').append(paramInt).append('\n');
    this.text.add(this.buf.toString());
  }
  
  public Printer visitInsnAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    return visitTypeAnnotation(paramInt, paramTypePath, paramString, paramBoolean);
  }
  
  public void visitTryCatchBlock(Label paramLabel1, Label paramLabel2, Label paramLabel3, String paramString)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("TRYCATCHBLOCK ");
    appendLabel(paramLabel1);
    this.buf.append(' ');
    appendLabel(paramLabel2);
    this.buf.append(' ');
    appendLabel(paramLabel3);
    this.buf.append(' ');
    appendDescriptor(0, paramString);
    this.buf.append('\n');
    this.text.add(this.buf.toString());
  }
  
  public Printer visitTryCatchAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("TRYCATCHBLOCK @");
    appendDescriptor(1, paramString);
    this.buf.append('(');
    this.text.add(this.buf.toString());
    Textifier localTextifier = createTextifier();
    this.text.add(localTextifier.getText());
    this.buf.setLength(0);
    this.buf.append(") : ");
    appendTypeReference(paramInt);
    this.buf.append(", ").append(paramTypePath);
    this.buf.append(paramBoolean ? "\n" : " // invisible\n");
    this.text.add(this.buf.toString());
    return localTextifier;
  }
  
  public void visitLocalVariable(String paramString1, String paramString2, String paramString3, Label paramLabel1, Label paramLabel2, int paramInt)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("LOCALVARIABLE ").append(paramString1).append(' ');
    appendDescriptor(1, paramString2);
    this.buf.append(' ');
    appendLabel(paramLabel1);
    this.buf.append(' ');
    appendLabel(paramLabel2);
    this.buf.append(' ').append(paramInt).append('\n');
    if (paramString3 != null)
    {
      this.buf.append(this.tab2);
      appendDescriptor(2, paramString3);
      TraceSignatureVisitor localTraceSignatureVisitor = new TraceSignatureVisitor(0);
      SignatureReader localSignatureReader = new SignatureReader(paramString3);
      localSignatureReader.acceptType(localTraceSignatureVisitor);
      this.buf.append(this.tab2).append("// declaration: ").append(localTraceSignatureVisitor.getDeclaration()).append('\n');
    }
    this.text.add(this.buf.toString());
  }
  
  public Printer visitLocalVariableAnnotation(int paramInt, TypePath paramTypePath, Label[] paramArrayOfLabel1, Label[] paramArrayOfLabel2, int[] paramArrayOfInt, String paramString, boolean paramBoolean)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("LOCALVARIABLE @");
    appendDescriptor(1, paramString);
    this.buf.append('(');
    this.text.add(this.buf.toString());
    Textifier localTextifier = createTextifier();
    this.text.add(localTextifier.getText());
    this.buf.setLength(0);
    this.buf.append(") : ");
    appendTypeReference(paramInt);
    this.buf.append(", ").append(paramTypePath);
    for (int i = 0; i < paramArrayOfLabel1.length; i++)
    {
      this.buf.append(" [ ");
      appendLabel(paramArrayOfLabel1[i]);
      this.buf.append(" - ");
      appendLabel(paramArrayOfLabel2[i]);
      this.buf.append(" - ").append(paramArrayOfInt[i]).append(" ]");
    }
    this.buf.append(paramBoolean ? "\n" : " // invisible\n");
    this.text.add(this.buf.toString());
    return localTextifier;
  }
  
  public void visitLineNumber(int paramInt, Label paramLabel)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("LINENUMBER ").append(paramInt).append(' ');
    appendLabel(paramLabel);
    this.buf.append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitMaxs(int paramInt1, int paramInt2)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("MAXSTACK = ").append(paramInt1).append('\n');
    this.text.add(this.buf.toString());
    this.buf.setLength(0);
    this.buf.append(this.tab2).append("MAXLOCALS = ").append(paramInt2).append('\n');
    this.text.add(this.buf.toString());
  }
  
  public void visitMethodEnd() {}
  
  public Textifier visitAnnotation(String paramString, boolean paramBoolean)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab).append('@');
    appendDescriptor(1, paramString);
    this.buf.append('(');
    this.text.add(this.buf.toString());
    Textifier localTextifier = createTextifier();
    this.text.add(localTextifier.getText());
    this.text.add(paramBoolean ? ")\n" : ") // invisible\n");
    return localTextifier;
  }
  
  public Textifier visitTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab).append('@');
    appendDescriptor(1, paramString);
    this.buf.append('(');
    this.text.add(this.buf.toString());
    Textifier localTextifier = createTextifier();
    this.text.add(localTextifier.getText());
    this.buf.setLength(0);
    this.buf.append(") : ");
    appendTypeReference(paramInt);
    this.buf.append(", ").append(paramTypePath);
    this.buf.append(paramBoolean ? "\n" : " // invisible\n");
    this.text.add(this.buf.toString());
    return localTextifier;
  }
  
  public void visitAttribute(Attribute paramAttribute)
  {
    this.buf.setLength(0);
    this.buf.append(this.tab).append("ATTRIBUTE ");
    appendDescriptor(-1, paramAttribute.type);
    if ((paramAttribute instanceof Textifiable)) {
      ((Textifiable)paramAttribute).textify(this.buf, null);
    } else {
      this.buf.append(" : unknown\n");
    }
    this.text.add(this.buf.toString());
  }
  
  protected Textifier createTextifier()
  {
    return new Textifier();
  }
  
  protected void appendDescriptor(int paramInt, String paramString)
  {
    if ((paramInt == 5) || (paramInt == 2) || (paramInt == 4))
    {
      if (paramString != null) {
        this.buf.append("// signature ").append(paramString).append('\n');
      }
    }
    else {
      this.buf.append(paramString);
    }
  }
  
  protected void appendLabel(Label paramLabel)
  {
    if (this.labelNames == null) {
      this.labelNames = new HashMap();
    }
    String str = (String)this.labelNames.get(paramLabel);
    if (str == null)
    {
      str = "L" + this.labelNames.size();
      this.labelNames.put(paramLabel, str);
    }
    this.buf.append(str);
  }
  
  protected void appendHandle(Handle paramHandle)
  {
    int i = paramHandle.getTag();
    this.buf.append("// handle kind 0x").append(Integer.toHexString(i)).append(" : ");
    int j = 0;
    switch (i)
    {
    case 1: 
      this.buf.append("GETFIELD");
      break;
    case 2: 
      this.buf.append("GETSTATIC");
      break;
    case 3: 
      this.buf.append("PUTFIELD");
      break;
    case 4: 
      this.buf.append("PUTSTATIC");
      break;
    case 9: 
      this.buf.append("INVOKEINTERFACE");
      j = 1;
      break;
    case 7: 
      this.buf.append("INVOKESPECIAL");
      j = 1;
      break;
    case 6: 
      this.buf.append("INVOKESTATIC");
      j = 1;
      break;
    case 5: 
      this.buf.append("INVOKEVIRTUAL");
      j = 1;
      break;
    case 8: 
      this.buf.append("NEWINVOKESPECIAL");
      j = 1;
    }
    this.buf.append('\n');
    this.buf.append(this.tab3);
    appendDescriptor(0, paramHandle.getOwner());
    this.buf.append('.');
    this.buf.append(paramHandle.getName());
    if (j == 0) {
      this.buf.append('(');
    }
    appendDescriptor(9, paramHandle.getDesc());
    if (j == 0) {
      this.buf.append(')');
    }
  }
  
  private void appendAccess(int paramInt)
  {
    if ((paramInt & 0x1) != 0) {
      this.buf.append("public ");
    }
    if ((paramInt & 0x2) != 0) {
      this.buf.append("private ");
    }
    if ((paramInt & 0x4) != 0) {
      this.buf.append("protected ");
    }
    if ((paramInt & 0x10) != 0) {
      this.buf.append("final ");
    }
    if ((paramInt & 0x8) != 0) {
      this.buf.append("static ");
    }
    if ((paramInt & 0x20) != 0) {
      this.buf.append("synchronized ");
    }
    if ((paramInt & 0x40) != 0) {
      this.buf.append("volatile ");
    }
    if ((paramInt & 0x80) != 0) {
      this.buf.append("transient ");
    }
    if ((paramInt & 0x400) != 0) {
      this.buf.append("abstract ");
    }
    if ((paramInt & 0x800) != 0) {
      this.buf.append("strictfp ");
    }
    if ((paramInt & 0x1000) != 0) {
      this.buf.append("synthetic ");
    }
    if ((paramInt & 0x8000) != 0) {
      this.buf.append("mandated ");
    }
    if ((paramInt & 0x4000) != 0) {
      this.buf.append("enum ");
    }
  }
  
  private void appendComa(int paramInt)
  {
    if (paramInt != 0) {
      this.buf.append(", ");
    }
  }
  
  private void appendTypeReference(int paramInt)
  {
    TypeReference localTypeReference = new TypeReference(paramInt);
    switch (localTypeReference.getSort())
    {
    case 0: 
      this.buf.append("CLASS_TYPE_PARAMETER ").append(localTypeReference.getTypeParameterIndex());
      break;
    case 1: 
      this.buf.append("METHOD_TYPE_PARAMETER ").append(localTypeReference.getTypeParameterIndex());
      break;
    case 16: 
      this.buf.append("CLASS_EXTENDS ").append(localTypeReference.getSuperTypeIndex());
      break;
    case 17: 
      this.buf.append("CLASS_TYPE_PARAMETER_BOUND ").append(localTypeReference.getTypeParameterIndex()).append(", ").append(localTypeReference.getTypeParameterBoundIndex());
      break;
    case 18: 
      this.buf.append("METHOD_TYPE_PARAMETER_BOUND ").append(localTypeReference.getTypeParameterIndex()).append(", ").append(localTypeReference.getTypeParameterBoundIndex());
      break;
    case 19: 
      this.buf.append("FIELD");
      break;
    case 20: 
      this.buf.append("METHOD_RETURN");
      break;
    case 21: 
      this.buf.append("METHOD_RECEIVER");
      break;
    case 22: 
      this.buf.append("METHOD_FORMAL_PARAMETER ").append(localTypeReference.getFormalParameterIndex());
      break;
    case 23: 
      this.buf.append("THROWS ").append(localTypeReference.getExceptionIndex());
      break;
    case 64: 
      this.buf.append("LOCAL_VARIABLE");
      break;
    case 65: 
      this.buf.append("RESOURCE_VARIABLE");
      break;
    case 66: 
      this.buf.append("EXCEPTION_PARAMETER ").append(localTypeReference.getTryCatchBlockIndex());
      break;
    case 67: 
      this.buf.append("INSTANCEOF");
      break;
    case 68: 
      this.buf.append("NEW");
      break;
    case 69: 
      this.buf.append("CONSTRUCTOR_REFERENCE");
      break;
    case 70: 
      this.buf.append("METHOD_REFERENCE");
      break;
    case 71: 
      this.buf.append("CAST ").append(localTypeReference.getTypeArgumentIndex());
      break;
    case 72: 
      this.buf.append("CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT ").append(localTypeReference.getTypeArgumentIndex());
      break;
    case 73: 
      this.buf.append("METHOD_INVOCATION_TYPE_ARGUMENT ").append(localTypeReference.getTypeArgumentIndex());
      break;
    case 74: 
      this.buf.append("CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT ").append(localTypeReference.getTypeArgumentIndex());
      break;
    case 75: 
      this.buf.append("METHOD_REFERENCE_TYPE_ARGUMENT ").append(localTypeReference.getTypeArgumentIndex());
    }
  }
  
  private void appendFrameTypes(int paramInt, Object[] paramArrayOfObject)
  {
    for (int i = 0; i < paramInt; i++)
    {
      if (i > 0) {
        this.buf.append(' ');
      }
      if ((paramArrayOfObject[i] instanceof String))
      {
        String str = (String)paramArrayOfObject[i];
        if (str.startsWith("[")) {
          appendDescriptor(1, str);
        } else {
          appendDescriptor(0, str);
        }
      }
      else if ((paramArrayOfObject[i] instanceof Integer))
      {
        switch (((Integer)paramArrayOfObject[i]).intValue())
        {
        case 0: 
          appendDescriptor(1, "T");
          break;
        case 1: 
          appendDescriptor(1, "I");
          break;
        case 2: 
          appendDescriptor(1, "F");
          break;
        case 3: 
          appendDescriptor(1, "D");
          break;
        case 4: 
          appendDescriptor(1, "J");
          break;
        case 5: 
          appendDescriptor(1, "N");
          break;
        case 6: 
          appendDescriptor(1, "U");
        }
      }
      else
      {
        appendLabel((Label)paramArrayOfObject[i]);
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
