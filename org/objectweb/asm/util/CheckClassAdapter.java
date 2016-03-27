package org.objectweb.asm.util;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

public class CheckClassAdapter
  extends ClassVisitor
{
  private int version;
  private boolean start;
  private boolean source;
  private boolean outer;
  private boolean end;
  private Map labels = new HashMap();
  private boolean checkDataFlow;
  static Class class$org$objectweb$asm$util$CheckClassAdapter = class$("org.objectweb.asm.util.CheckClassAdapter");
  
  public static void main(String[] paramArrayOfString)
    throws Exception
  {
    if (paramArrayOfString.length != 1)
    {
      System.err.println("Verifies the given class.");
      System.err.println("Usage: CheckClassAdapter <fully qualified class name or class file name>");
      return;
    }
    ClassReader localClassReader;
    if (paramArrayOfString[0].endsWith(".class")) {
      localClassReader = new ClassReader(new FileInputStream(paramArrayOfString[0]));
    } else {
      localClassReader = new ClassReader(paramArrayOfString[0]);
    }
    verify(localClassReader, false, new PrintWriter(System.err));
  }
  
  public static void verify(ClassReader paramClassReader, ClassLoader paramClassLoader, boolean paramBoolean, PrintWriter paramPrintWriter)
  {
    ClassNode localClassNode = new ClassNode();
    paramClassReader.accept(new CheckClassAdapter(localClassNode, false), 2);
    Type localType = localClassNode.superName == null ? null : Type.getObjectType(localClassNode.superName);
    List localList = localClassNode.methods;
    ArrayList localArrayList = new ArrayList();
    Iterator localIterator = localClassNode.interfaces.iterator();
    while (localIterator.hasNext()) {
      localArrayList.add(Type.getObjectType((String)localIterator.next()));
    }
    for (int i = 0; i < localList.size(); i++)
    {
      MethodNode localMethodNode = (MethodNode)localList.get(i);
      SimpleVerifier localSimpleVerifier = new SimpleVerifier(Type.getObjectType(localClassNode.name), localType, localArrayList, (localClassNode.access & 0x200) != 0);
      Analyzer localAnalyzer = new Analyzer(localSimpleVerifier);
      if (paramClassLoader != null) {
        localSimpleVerifier.setClassLoader(paramClassLoader);
      }
      try
      {
        localAnalyzer.analyze(localClassNode.name, localMethodNode);
        if (!paramBoolean) {
          continue;
        }
      }
      catch (Exception localException)
      {
        localException.printStackTrace(paramPrintWriter);
      }
      printAnalyzerResult(localMethodNode, localAnalyzer, paramPrintWriter);
    }
    paramPrintWriter.flush();
  }
  
  public static void verify(ClassReader paramClassReader, boolean paramBoolean, PrintWriter paramPrintWriter)
  {
    verify(paramClassReader, null, paramBoolean, paramPrintWriter);
  }
  
  static void printAnalyzerResult(MethodNode paramMethodNode, Analyzer paramAnalyzer, PrintWriter paramPrintWriter)
  {
    Frame[] arrayOfFrame = paramAnalyzer.getFrames();
    Textifier localTextifier = new Textifier();
    TraceMethodVisitor localTraceMethodVisitor = new TraceMethodVisitor(localTextifier);
    paramPrintWriter.println(paramMethodNode.name + paramMethodNode.desc);
    for (int i = 0; i < paramMethodNode.instructions.size(); i++)
    {
      paramMethodNode.instructions.get(i).accept(localTraceMethodVisitor);
      StringBuffer localStringBuffer = new StringBuffer();
      Frame localFrame = arrayOfFrame[i];
      if (localFrame == null)
      {
        localStringBuffer.append('?');
      }
      else
      {
        for (int j = 0; j < localFrame.getLocals(); j++) {
          localStringBuffer.append(getShortName(((BasicValue)localFrame.getLocal(j)).toString())).append(' ');
        }
        localStringBuffer.append(" : ");
        for (j = 0; j < localFrame.getStackSize(); j++) {
          localStringBuffer.append(getShortName(((BasicValue)localFrame.getStack(j)).toString())).append(' ');
        }
      }
      while (localStringBuffer.length() < paramMethodNode.maxStack + paramMethodNode.maxLocals + 1) {
        localStringBuffer.append(' ');
      }
      paramPrintWriter.print(Integer.toString(i + 100000).substring(1));
      paramPrintWriter.print(" " + localStringBuffer + " : " + localTextifier.text.get(localTextifier.text.size() - 1));
    }
    for (i = 0; i < paramMethodNode.tryCatchBlocks.size(); i++)
    {
      ((TryCatchBlockNode)paramMethodNode.tryCatchBlocks.get(i)).accept(localTraceMethodVisitor);
      paramPrintWriter.print(" " + localTextifier.text.get(localTextifier.text.size() - 1));
    }
    paramPrintWriter.println();
  }
  
  private static String getShortName(String paramString)
  {
    int i = paramString.lastIndexOf('/');
    int j = paramString.length();
    if (paramString.charAt(j - 1) == ';') {
      j--;
    }
    return i == -1 ? paramString : paramString.substring(i + 1, j);
  }
  
  public CheckClassAdapter(ClassVisitor paramClassVisitor)
  {
    this(paramClassVisitor, true);
  }
  
  public CheckClassAdapter(ClassVisitor paramClassVisitor, boolean paramBoolean)
  {
    this(327680, paramClassVisitor, paramBoolean);
    if (getClass() != class$org$objectweb$asm$util$CheckClassAdapter) {
      throw new IllegalStateException();
    }
  }
  
  protected CheckClassAdapter(int paramInt, ClassVisitor paramClassVisitor, boolean paramBoolean)
  {
    super(paramInt, paramClassVisitor);
    this.checkDataFlow = paramBoolean;
  }
  
  public void visit(int paramInt1, int paramInt2, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    if (this.start) {
      throw new IllegalStateException("visit must be called only once");
    }
    this.start = true;
    checkState();
    checkAccess(paramInt2, 423473);
    if ((paramString1 == null) || (!paramString1.endsWith("package-info"))) {
      CheckMethodAdapter.checkInternalName(paramString1, "class name");
    }
    if ("java/lang/Object".equals(paramString1))
    {
      if (paramString3 != null) {
        throw new IllegalArgumentException("The super class name of the Object class must be 'null'");
      }
    }
    else {
      CheckMethodAdapter.checkInternalName(paramString3, "super class name");
    }
    if (paramString2 != null) {
      checkClassSignature(paramString2);
    }
    if (((paramInt2 & 0x200) != 0) && (!"java/lang/Object".equals(paramString3))) {
      throw new IllegalArgumentException("The super class name of interfaces must be 'java/lang/Object'");
    }
    if (paramArrayOfString != null) {
      for (int i = 0; i < paramArrayOfString.length; i++) {
        CheckMethodAdapter.checkInternalName(paramArrayOfString[i], "interface name at index " + i);
      }
    }
    this.version = paramInt1;
    super.visit(paramInt1, paramInt2, paramString1, paramString2, paramString3, paramArrayOfString);
  }
  
  public void visitSource(String paramString1, String paramString2)
  {
    checkState();
    if (this.source) {
      throw new IllegalStateException("visitSource can be called only once.");
    }
    this.source = true;
    super.visitSource(paramString1, paramString2);
  }
  
  public void visitOuterClass(String paramString1, String paramString2, String paramString3)
  {
    checkState();
    if (this.outer) {
      throw new IllegalStateException("visitOuterClass can be called only once.");
    }
    this.outer = true;
    if (paramString1 == null) {
      throw new IllegalArgumentException("Illegal outer class owner");
    }
    if (paramString3 != null) {
      CheckMethodAdapter.checkMethodDesc(paramString3);
    }
    super.visitOuterClass(paramString1, paramString2, paramString3);
  }
  
  public void visitInnerClass(String paramString1, String paramString2, String paramString3, int paramInt)
  {
    checkState();
    CheckMethodAdapter.checkInternalName(paramString1, "class name");
    if (paramString2 != null) {
      CheckMethodAdapter.checkInternalName(paramString2, "outer class name");
    }
    if (paramString3 != null)
    {
      for (int i = 0; (i < paramString3.length()) && (Character.isDigit(paramString3.charAt(i))); i++) {}
      if ((i == 0) || (i < paramString3.length())) {
        CheckMethodAdapter.checkIdentifier(paramString3, i, -1, "inner class name");
      }
    }
    checkAccess(paramInt, 30239);
    super.visitInnerClass(paramString1, paramString2, paramString3, paramInt);
  }
  
  public FieldVisitor visitField(int paramInt, String paramString1, String paramString2, String paramString3, Object paramObject)
  {
    checkState();
    checkAccess(paramInt, 413919);
    CheckMethodAdapter.checkUnqualifiedName(this.version, paramString1, "field name");
    CheckMethodAdapter.checkDesc(paramString2, false);
    if (paramString3 != null) {
      checkFieldSignature(paramString3);
    }
    if (paramObject != null) {
      CheckMethodAdapter.checkConstant(paramObject);
    }
    FieldVisitor localFieldVisitor = super.visitField(paramInt, paramString1, paramString2, paramString3, paramObject);
    return new CheckFieldAdapter(localFieldVisitor);
  }
  
  public MethodVisitor visitMethod(int paramInt, String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
  {
    checkState();
    checkAccess(paramInt, 400895);
    if ((!"<init>".equals(paramString1)) && (!"<clinit>".equals(paramString1))) {
      CheckMethodAdapter.checkMethodIdentifier(this.version, paramString1, "method name");
    }
    CheckMethodAdapter.checkMethodDesc(paramString2);
    if (paramString3 != null) {
      checkMethodSignature(paramString3);
    }
    if (paramArrayOfString != null) {
      for (int i = 0; i < paramArrayOfString.length; i++) {
        CheckMethodAdapter.checkInternalName(paramArrayOfString[i], "exception name at index " + i);
      }
    }
    CheckMethodAdapter localCheckMethodAdapter;
    if (this.checkDataFlow) {
      localCheckMethodAdapter = new CheckMethodAdapter(paramInt, paramString1, paramString2, super.visitMethod(paramInt, paramString1, paramString2, paramString3, paramArrayOfString), this.labels);
    } else {
      localCheckMethodAdapter = new CheckMethodAdapter(super.visitMethod(paramInt, paramString1, paramString2, paramString3, paramArrayOfString), this.labels);
    }
    localCheckMethodAdapter.version = this.version;
    return localCheckMethodAdapter;
  }
  
  public AnnotationVisitor visitAnnotation(String paramString, boolean paramBoolean)
  {
    checkState();
    CheckMethodAdapter.checkDesc(paramString, false);
    return new CheckAnnotationAdapter(super.visitAnnotation(paramString, paramBoolean));
  }
  
  public AnnotationVisitor visitTypeAnnotation(int paramInt, TypePath paramTypePath, String paramString, boolean paramBoolean)
  {
    checkState();
    int i = paramInt >>> 24;
    if ((i != 0) && (i != 17) && (i != 16)) {
      throw new IllegalArgumentException("Invalid type reference sort 0x" + Integer.toHexString(i));
    }
    checkTypeRefAndPath(paramInt, paramTypePath);
    CheckMethodAdapter.checkDesc(paramString, false);
    return new CheckAnnotationAdapter(super.visitTypeAnnotation(paramInt, paramTypePath, paramString, paramBoolean));
  }
  
  public void visitAttribute(Attribute paramAttribute)
  {
    checkState();
    if (paramAttribute == null) {
      throw new IllegalArgumentException("Invalid attribute (must not be null)");
    }
    super.visitAttribute(paramAttribute);
  }
  
  public void visitEnd()
  {
    checkState();
    this.end = true;
    super.visitEnd();
  }
  
  private void checkState()
  {
    if (!this.start) {
      throw new IllegalStateException("Cannot visit member before visit has been called.");
    }
    if (this.end) {
      throw new IllegalStateException("Cannot visit member after visitEnd has been called.");
    }
  }
  
  static void checkAccess(int paramInt1, int paramInt2)
  {
    if ((paramInt1 & (paramInt2 ^ 0xFFFFFFFF)) != 0) {
      throw new IllegalArgumentException("Invalid access flags: " + paramInt1);
    }
    int i = (paramInt1 & 0x1) == 0 ? 0 : 1;
    int j = (paramInt1 & 0x2) == 0 ? 0 : 1;
    int k = (paramInt1 & 0x4) == 0 ? 0 : 1;
    if (i + j + k > 1) {
      throw new IllegalArgumentException("public private and protected are mutually exclusive: " + paramInt1);
    }
    int m = (paramInt1 & 0x10) == 0 ? 0 : 1;
    int n = (paramInt1 & 0x400) == 0 ? 0 : 1;
    if (m + n > 1) {
      throw new IllegalArgumentException("final and abstract are mutually exclusive: " + paramInt1);
    }
  }
  
  public static void checkClassSignature(String paramString)
  {
    int i = 0;
    if (getChar(paramString, 0) == '<') {
      i = checkFormalTypeParameters(paramString, i);
    }
    for (i = checkClassTypeSignature(paramString, i); getChar(paramString, i) == 'L'; i = checkClassTypeSignature(paramString, i)) {}
    if (i != paramString.length()) {
      throw new IllegalArgumentException(paramString + ": error at index " + i);
    }
  }
  
  public static void checkMethodSignature(String paramString)
  {
    int i = 0;
    if (getChar(paramString, 0) == '<') {
      i = checkFormalTypeParameters(paramString, i);
    }
    for (i = checkChar('(', paramString, i); "ZCBSIFJDL[T".indexOf(getChar(paramString, i)) != -1; i = checkTypeSignature(paramString, i)) {}
    i = checkChar(')', paramString, i);
    if (getChar(paramString, i) == 'V') {
      i++;
    } else {
      i = checkTypeSignature(paramString, i);
    }
    while (getChar(paramString, i) == '^')
    {
      i++;
      if (getChar(paramString, i) == 'L') {
        i = checkClassTypeSignature(paramString, i);
      } else {
        i = checkTypeVariableSignature(paramString, i);
      }
    }
    if (i != paramString.length()) {
      throw new IllegalArgumentException(paramString + ": error at index " + i);
    }
  }
  
  public static void checkFieldSignature(String paramString)
  {
    int i = checkFieldTypeSignature(paramString, 0);
    if (i != paramString.length()) {
      throw new IllegalArgumentException(paramString + ": error at index " + i);
    }
  }
  
  static void checkTypeRefAndPath(int paramInt, TypePath paramTypePath)
  {
    int i = 0;
    switch (paramInt >>> 24)
    {
    case 0: 
    case 1: 
    case 22: 
      i = -65536;
      break;
    case 19: 
    case 20: 
    case 21: 
    case 64: 
    case 65: 
    case 67: 
    case 68: 
    case 69: 
    case 70: 
      i = -16777216;
      break;
    case 16: 
    case 17: 
    case 18: 
    case 23: 
    case 66: 
      i = 65280;
      break;
    case 71: 
    case 72: 
    case 73: 
    case 74: 
    case 75: 
      i = -16776961;
      break;
    case 2: 
    case 3: 
    case 4: 
    case 5: 
    case 6: 
    case 7: 
    case 8: 
    case 9: 
    case 10: 
    case 11: 
    case 12: 
    case 13: 
    case 14: 
    case 15: 
    case 24: 
    case 25: 
    case 26: 
    case 27: 
    case 28: 
    case 29: 
    case 30: 
    case 31: 
    case 32: 
    case 33: 
    case 34: 
    case 35: 
    case 36: 
    case 37: 
    case 38: 
    case 39: 
    case 40: 
    case 41: 
    case 42: 
    case 43: 
    case 44: 
    case 45: 
    case 46: 
    case 47: 
    case 48: 
    case 49: 
    case 50: 
    case 51: 
    case 52: 
    case 53: 
    case 54: 
    case 55: 
    case 56: 
    case 57: 
    case 58: 
    case 59: 
    case 60: 
    case 61: 
    case 62: 
    case 63: 
    default: 
      throw new IllegalArgumentException("Invalid type reference sort 0x" + Integer.toHexString(paramInt >>> 24));
    }
    if ((paramInt & (i ^ 0xFFFFFFFF)) != 0) {
      throw new IllegalArgumentException("Invalid type reference 0x" + Integer.toHexString(paramInt));
    }
    if (paramTypePath != null) {
      for (int j = 0; j < paramTypePath.getLength(); j++)
      {
        int k = paramTypePath.getStep(j);
        if ((k != 0) && (k != 1) && (k != 3) && (k != 2)) {
          throw new IllegalArgumentException("Invalid type path step " + j + " in " + paramTypePath);
        }
        if ((k != 3) && (paramTypePath.getStepArgument(j) != 0)) {
          throw new IllegalArgumentException("Invalid type path step argument for step " + j + " in " + paramTypePath);
        }
      }
    }
  }
  
  private static int checkFormalTypeParameters(String paramString, int paramInt)
  {
    paramInt = checkChar('<', paramString, paramInt);
    for (paramInt = checkFormalTypeParameter(paramString, paramInt); getChar(paramString, paramInt) != '>'; paramInt = checkFormalTypeParameter(paramString, paramInt)) {}
    return paramInt + 1;
  }
  
  private static int checkFormalTypeParameter(String paramString, int paramInt)
  {
    paramInt = checkIdentifier(paramString, paramInt);
    paramInt = checkChar(':', paramString, paramInt);
    if ("L[T".indexOf(getChar(paramString, paramInt)) != -1) {}
    for (paramInt = checkFieldTypeSignature(paramString, paramInt); getChar(paramString, paramInt) == ':'; paramInt = checkFieldTypeSignature(paramString, paramInt + 1)) {}
    return paramInt;
  }
  
  private static int checkFieldTypeSignature(String paramString, int paramInt)
  {
    switch (getChar(paramString, paramInt))
    {
    case 'L': 
      return checkClassTypeSignature(paramString, paramInt);
    case '[': 
      return checkTypeSignature(paramString, paramInt + 1);
    }
    return checkTypeVariableSignature(paramString, paramInt);
  }
  
  private static int checkClassTypeSignature(String paramString, int paramInt)
  {
    paramInt = checkChar('L', paramString, paramInt);
    for (paramInt = checkIdentifier(paramString, paramInt); getChar(paramString, paramInt) == '/'; paramInt = checkIdentifier(paramString, paramInt + 1)) {}
    if (getChar(paramString, paramInt) == '<') {
      paramInt = checkTypeArguments(paramString, paramInt);
    }
    while (getChar(paramString, paramInt) == '.')
    {
      paramInt = checkIdentifier(paramString, paramInt + 1);
      if (getChar(paramString, paramInt) == '<') {
        paramInt = checkTypeArguments(paramString, paramInt);
      }
    }
    return checkChar(';', paramString, paramInt);
  }
  
  private static int checkTypeArguments(String paramString, int paramInt)
  {
    paramInt = checkChar('<', paramString, paramInt);
    for (paramInt = checkTypeArgument(paramString, paramInt); getChar(paramString, paramInt) != '>'; paramInt = checkTypeArgument(paramString, paramInt)) {}
    return paramInt + 1;
  }
  
  private static int checkTypeArgument(String paramString, int paramInt)
  {
    int i = getChar(paramString, paramInt);
    if (i == 42) {
      return paramInt + 1;
    }
    if ((i == 43) || (i == 45)) {
      paramInt++;
    }
    return checkFieldTypeSignature(paramString, paramInt);
  }
  
  private static int checkTypeVariableSignature(String paramString, int paramInt)
  {
    paramInt = checkChar('T', paramString, paramInt);
    paramInt = checkIdentifier(paramString, paramInt);
    return checkChar(';', paramString, paramInt);
  }
  
  private static int checkTypeSignature(String paramString, int paramInt)
  {
    switch (getChar(paramString, paramInt))
    {
    case 'B': 
    case 'C': 
    case 'D': 
    case 'F': 
    case 'I': 
    case 'J': 
    case 'S': 
    case 'Z': 
      return paramInt + 1;
    }
    return checkFieldTypeSignature(paramString, paramInt);
  }
  
  private static int checkIdentifier(String paramString, int paramInt)
  {
    if (!Character.isJavaIdentifierStart(getChar(paramString, paramInt))) {
      throw new IllegalArgumentException(paramString + ": identifier expected at index " + paramInt);
    }
    paramInt++;
    while (Character.isJavaIdentifierPart(getChar(paramString, paramInt))) {
      paramInt++;
    }
    return paramInt;
  }
  
  private static int checkChar(char paramChar, String paramString, int paramInt)
  {
    if (getChar(paramString, paramInt) == paramChar) {
      return paramInt + 1;
    }
    throw new IllegalArgumentException(paramString + ": '" + paramChar + "' expected at index " + paramInt);
  }
  
  private static char getChar(String paramString, int paramInt)
  {
    return paramInt < paramString.length() ? paramString.charAt(paramInt) : '\000';
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
