package org.objectweb.asm.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ASMContentHandler
  extends DefaultHandler
  implements Opcodes
{
  private final ArrayList stack = new ArrayList();
  String match = "";
  protected ClassVisitor cv;
  protected Map labels;
  private static final String BASE = "class";
  private final ASMContentHandler.RuleSet RULES = new ASMContentHandler.RuleSet();
  static final HashMap OPCODES;
  static final HashMap TYPES;
  
  private static void addOpcode(String paramString, int paramInt1, int paramInt2)
  {
    OPCODES.put(paramString, new ASMContentHandler.Opcode(paramInt1, paramInt2));
  }
  
  public ASMContentHandler(ClassVisitor paramClassVisitor)
  {
    this.RULES.add("class", new ASMContentHandler.ClassRule(this));
    this.RULES.add("class/interfaces/interface", new ASMContentHandler.InterfaceRule(this));
    this.RULES.add("class/interfaces", new ASMContentHandler.InterfacesRule(this));
    this.RULES.add("class/outerclass", new ASMContentHandler.OuterClassRule(this));
    this.RULES.add("class/innerclass", new ASMContentHandler.InnerClassRule(this));
    this.RULES.add("class/source", new ASMContentHandler.SourceRule(this));
    this.RULES.add("class/field", new ASMContentHandler.FieldRule(this));
    this.RULES.add("class/method", new ASMContentHandler.MethodRule(this));
    this.RULES.add("class/method/exceptions/exception", new ASMContentHandler.ExceptionRule(this));
    this.RULES.add("class/method/exceptions", new ASMContentHandler.ExceptionsRule(this));
    this.RULES.add("class/method/parameter", new ASMContentHandler.MethodParameterRule(this));
    this.RULES.add("class/method/annotationDefault", new ASMContentHandler.AnnotationDefaultRule(this));
    this.RULES.add("class/method/code/*", new ASMContentHandler.OpcodesRule(this));
    this.RULES.add("class/method/code/frame", new ASMContentHandler.FrameRule(this));
    this.RULES.add("class/method/code/frame/local", new ASMContentHandler.FrameTypeRule(this));
    this.RULES.add("class/method/code/frame/stack", new ASMContentHandler.FrameTypeRule(this));
    this.RULES.add("class/method/code/TABLESWITCH", new ASMContentHandler.TableSwitchRule(this));
    this.RULES.add("class/method/code/TABLESWITCH/label", new ASMContentHandler.TableSwitchLabelRule(this));
    this.RULES.add("class/method/code/LOOKUPSWITCH", new ASMContentHandler.LookupSwitchRule(this));
    this.RULES.add("class/method/code/LOOKUPSWITCH/label", new ASMContentHandler.LookupSwitchLabelRule(this));
    this.RULES.add("class/method/code/INVOKEDYNAMIC", new ASMContentHandler.InvokeDynamicRule(this));
    this.RULES.add("class/method/code/INVOKEDYNAMIC/bsmArg", new ASMContentHandler.InvokeDynamicBsmArgumentsRule(this));
    this.RULES.add("class/method/code/Label", new ASMContentHandler.LabelRule(this));
    this.RULES.add("class/method/code/TryCatch", new ASMContentHandler.TryCatchRule(this));
    this.RULES.add("class/method/code/LineNumber", new ASMContentHandler.LineNumberRule(this));
    this.RULES.add("class/method/code/LocalVar", new ASMContentHandler.LocalVarRule(this));
    this.RULES.add("class/method/code/Max", new ASMContentHandler.MaxRule(this));
    this.RULES.add("*/annotation", new ASMContentHandler.AnnotationRule(this));
    this.RULES.add("*/typeAnnotation", new ASMContentHandler.TypeAnnotationRule(this));
    this.RULES.add("*/parameterAnnotation", new ASMContentHandler.AnnotationParameterRule(this));
    this.RULES.add("*/insnAnnotation", new ASMContentHandler.InsnAnnotationRule(this));
    this.RULES.add("*/tryCatchAnnotation", new ASMContentHandler.TryCatchAnnotationRule(this));
    this.RULES.add("*/localVariableAnnotation", new ASMContentHandler.LocalVariableAnnotationRule(this));
    this.RULES.add("*/annotationValue", new ASMContentHandler.AnnotationValueRule(this));
    this.RULES.add("*/annotationValueAnnotation", new ASMContentHandler.AnnotationValueAnnotationRule(this));
    this.RULES.add("*/annotationValueEnum", new ASMContentHandler.AnnotationValueEnumRule(this));
    this.RULES.add("*/annotationValueArray", new ASMContentHandler.AnnotationValueArrayRule(this));
    this.cv = paramClassVisitor;
  }
  
  public final void startElement(String paramString1, String paramString2, String paramString3, Attributes paramAttributes)
    throws SAXException
  {
    String str = (paramString2 == null) || (paramString2.length() == 0) ? paramString3 : paramString2;
    StringBuffer localStringBuffer = new StringBuffer(this.match);
    if (this.match.length() > 0) {
      localStringBuffer.append('/');
    }
    localStringBuffer.append(str);
    this.match = localStringBuffer.toString();
    ASMContentHandler.Rule localRule = (ASMContentHandler.Rule)this.RULES.match(this.match);
    if (localRule != null) {
      localRule.begin(str, paramAttributes);
    }
  }
  
  public final void endElement(String paramString1, String paramString2, String paramString3)
    throws SAXException
  {
    String str = (paramString2 == null) || (paramString2.length() == 0) ? paramString3 : paramString2;
    ASMContentHandler.Rule localRule = (ASMContentHandler.Rule)this.RULES.match(this.match);
    if (localRule != null) {
      localRule.end(str);
    }
    int i = this.match.lastIndexOf('/');
    if (i >= 0) {
      this.match = this.match.substring(0, i);
    } else {
      this.match = "";
    }
  }
  
  final Object peek()
  {
    int i = this.stack.size();
    return i == 0 ? null : this.stack.get(i - 1);
  }
  
  final Object pop()
  {
    int i = this.stack.size();
    return i == 0 ? null : this.stack.remove(i - 1);
  }
  
  final void push(Object paramObject)
  {
    this.stack.add(paramObject);
  }
  
  static
  {
    _clinit_();
    OPCODES = new HashMap();
    addOpcode("NOP", 0, 0);
    addOpcode("ACONST_NULL", 1, 0);
    addOpcode("ICONST_M1", 2, 0);
    addOpcode("ICONST_0", 3, 0);
    addOpcode("ICONST_1", 4, 0);
    addOpcode("ICONST_2", 5, 0);
    addOpcode("ICONST_3", 6, 0);
    addOpcode("ICONST_4", 7, 0);
    addOpcode("ICONST_5", 8, 0);
    addOpcode("LCONST_0", 9, 0);
    addOpcode("LCONST_1", 10, 0);
    addOpcode("FCONST_0", 11, 0);
    addOpcode("FCONST_1", 12, 0);
    addOpcode("FCONST_2", 13, 0);
    addOpcode("DCONST_0", 14, 0);
    addOpcode("DCONST_1", 15, 0);
    addOpcode("BIPUSH", 16, 1);
    addOpcode("SIPUSH", 17, 1);
    addOpcode("LDC", 18, 7);
    addOpcode("ILOAD", 21, 2);
    addOpcode("LLOAD", 22, 2);
    addOpcode("FLOAD", 23, 2);
    addOpcode("DLOAD", 24, 2);
    addOpcode("ALOAD", 25, 2);
    addOpcode("IALOAD", 46, 0);
    addOpcode("LALOAD", 47, 0);
    addOpcode("FALOAD", 48, 0);
    addOpcode("DALOAD", 49, 0);
    addOpcode("AALOAD", 50, 0);
    addOpcode("BALOAD", 51, 0);
    addOpcode("CALOAD", 52, 0);
    addOpcode("SALOAD", 53, 0);
    addOpcode("ISTORE", 54, 2);
    addOpcode("LSTORE", 55, 2);
    addOpcode("FSTORE", 56, 2);
    addOpcode("DSTORE", 57, 2);
    addOpcode("ASTORE", 58, 2);
    addOpcode("IASTORE", 79, 0);
    addOpcode("LASTORE", 80, 0);
    addOpcode("FASTORE", 81, 0);
    addOpcode("DASTORE", 82, 0);
    addOpcode("AASTORE", 83, 0);
    addOpcode("BASTORE", 84, 0);
    addOpcode("CASTORE", 85, 0);
    addOpcode("SASTORE", 86, 0);
    addOpcode("POP", 87, 0);
    addOpcode("POP2", 88, 0);
    addOpcode("DUP", 89, 0);
    addOpcode("DUP_X1", 90, 0);
    addOpcode("DUP_X2", 91, 0);
    addOpcode("DUP2", 92, 0);
    addOpcode("DUP2_X1", 93, 0);
    addOpcode("DUP2_X2", 94, 0);
    addOpcode("SWAP", 95, 0);
    addOpcode("IADD", 96, 0);
    addOpcode("LADD", 97, 0);
    addOpcode("FADD", 98, 0);
    addOpcode("DADD", 99, 0);
    addOpcode("ISUB", 100, 0);
    addOpcode("LSUB", 101, 0);
    addOpcode("FSUB", 102, 0);
    addOpcode("DSUB", 103, 0);
    addOpcode("IMUL", 104, 0);
    addOpcode("LMUL", 105, 0);
    addOpcode("FMUL", 106, 0);
    addOpcode("DMUL", 107, 0);
    addOpcode("IDIV", 108, 0);
    addOpcode("LDIV", 109, 0);
    addOpcode("FDIV", 110, 0);
    addOpcode("DDIV", 111, 0);
    addOpcode("IREM", 112, 0);
    addOpcode("LREM", 113, 0);
    addOpcode("FREM", 114, 0);
    addOpcode("DREM", 115, 0);
    addOpcode("INEG", 116, 0);
    addOpcode("LNEG", 117, 0);
    addOpcode("FNEG", 118, 0);
    addOpcode("DNEG", 119, 0);
    addOpcode("ISHL", 120, 0);
    addOpcode("LSHL", 121, 0);
    addOpcode("ISHR", 122, 0);
    addOpcode("LSHR", 123, 0);
    addOpcode("IUSHR", 124, 0);
    addOpcode("LUSHR", 125, 0);
    addOpcode("IAND", 126, 0);
    addOpcode("LAND", 127, 0);
    addOpcode("IOR", 128, 0);
    addOpcode("LOR", 129, 0);
    addOpcode("IXOR", 130, 0);
    addOpcode("LXOR", 131, 0);
    addOpcode("IINC", 132, 8);
    addOpcode("I2L", 133, 0);
    addOpcode("I2F", 134, 0);
    addOpcode("I2D", 135, 0);
    addOpcode("L2I", 136, 0);
    addOpcode("L2F", 137, 0);
    addOpcode("L2D", 138, 0);
    addOpcode("F2I", 139, 0);
    addOpcode("F2L", 140, 0);
    addOpcode("F2D", 141, 0);
    addOpcode("D2I", 142, 0);
    addOpcode("D2L", 143, 0);
    addOpcode("D2F", 144, 0);
    addOpcode("I2B", 145, 0);
    addOpcode("I2C", 146, 0);
    addOpcode("I2S", 147, 0);
    addOpcode("LCMP", 148, 0);
    addOpcode("FCMPL", 149, 0);
    addOpcode("FCMPG", 150, 0);
    addOpcode("DCMPL", 151, 0);
    addOpcode("DCMPG", 152, 0);
    addOpcode("IFEQ", 153, 6);
    addOpcode("IFNE", 154, 6);
    addOpcode("IFLT", 155, 6);
    addOpcode("IFGE", 156, 6);
    addOpcode("IFGT", 157, 6);
    addOpcode("IFLE", 158, 6);
    addOpcode("IF_ICMPEQ", 159, 6);
    addOpcode("IF_ICMPNE", 160, 6);
    addOpcode("IF_ICMPLT", 161, 6);
    addOpcode("IF_ICMPGE", 162, 6);
    addOpcode("IF_ICMPGT", 163, 6);
    addOpcode("IF_ICMPLE", 164, 6);
    addOpcode("IF_ACMPEQ", 165, 6);
    addOpcode("IF_ACMPNE", 166, 6);
    addOpcode("GOTO", 167, 6);
    addOpcode("JSR", 168, 6);
    addOpcode("RET", 169, 2);
    addOpcode("IRETURN", 172, 0);
    addOpcode("LRETURN", 173, 0);
    addOpcode("FRETURN", 174, 0);
    addOpcode("DRETURN", 175, 0);
    addOpcode("ARETURN", 176, 0);
    addOpcode("RETURN", 177, 0);
    addOpcode("GETSTATIC", 178, 4);
    addOpcode("PUTSTATIC", 179, 4);
    addOpcode("GETFIELD", 180, 4);
    addOpcode("PUTFIELD", 181, 4);
    addOpcode("INVOKEVIRTUAL", 182, 5);
    addOpcode("INVOKESPECIAL", 183, 5);
    addOpcode("INVOKESTATIC", 184, 5);
    addOpcode("INVOKEINTERFACE", 185, 5);
    addOpcode("NEW", 187, 3);
    addOpcode("NEWARRAY", 188, 1);
    addOpcode("ANEWARRAY", 189, 3);
    addOpcode("ARRAYLENGTH", 190, 0);
    addOpcode("ATHROW", 191, 0);
    addOpcode("CHECKCAST", 192, 3);
    addOpcode("INSTANCEOF", 193, 3);
    addOpcode("MONITORENTER", 194, 0);
    addOpcode("MONITOREXIT", 195, 0);
    addOpcode("MULTIANEWARRAY", 197, 9);
    addOpcode("IFNULL", 198, 6);
    addOpcode("IFNONNULL", 199, 6);
    TYPES = new HashMap();
    String[] arrayOfString = SAXCodeAdapter.TYPES;
    for (int i = 0; i < arrayOfString.length; i++) {
      TYPES.put(arrayOfString[i], new Integer(i));
    }
  }
  
  static void _clinit_() {}
}
