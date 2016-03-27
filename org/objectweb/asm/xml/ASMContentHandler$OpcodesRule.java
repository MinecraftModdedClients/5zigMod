package org.objectweb.asm.xml;

import java.util.HashMap;
import org.objectweb.asm.MethodVisitor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

final class ASMContentHandler$OpcodesRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$OpcodesRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void begin(String paramString, Attributes paramAttributes)
    throws SAXException
  {
    ASMContentHandler.Opcode localOpcode = (ASMContentHandler.Opcode)ASMContentHandler.OPCODES.get(paramString);
    if (localOpcode == null) {
      throw new SAXException("Invalid element: " + paramString + " at " + this.this$0.match);
    }
    switch (localOpcode.type)
    {
    case 0: 
      getCodeVisitor().visitInsn(localOpcode.opcode);
      break;
    case 4: 
      getCodeVisitor().visitFieldInsn(localOpcode.opcode, paramAttributes.getValue("owner"), paramAttributes.getValue("name"), paramAttributes.getValue("desc"));
      break;
    case 1: 
      getCodeVisitor().visitIntInsn(localOpcode.opcode, Integer.parseInt(paramAttributes.getValue("value")));
      break;
    case 6: 
      getCodeVisitor().visitJumpInsn(localOpcode.opcode, getLabel(paramAttributes.getValue("label")));
      break;
    case 5: 
      getCodeVisitor().visitMethodInsn(localOpcode.opcode, paramAttributes.getValue("owner"), paramAttributes.getValue("name"), paramAttributes.getValue("desc"), paramAttributes.getValue("itf").equals("true"));
      break;
    case 3: 
      getCodeVisitor().visitTypeInsn(localOpcode.opcode, paramAttributes.getValue("desc"));
      break;
    case 2: 
      getCodeVisitor().visitVarInsn(localOpcode.opcode, Integer.parseInt(paramAttributes.getValue("var")));
      break;
    case 8: 
      getCodeVisitor().visitIincInsn(Integer.parseInt(paramAttributes.getValue("var")), Integer.parseInt(paramAttributes.getValue("inc")));
      break;
    case 7: 
      getCodeVisitor().visitLdcInsn(getValue(paramAttributes.getValue("desc"), paramAttributes.getValue("cst")));
      break;
    case 9: 
      getCodeVisitor().visitMultiANewArrayInsn(paramAttributes.getValue("desc"), Integer.parseInt(paramAttributes.getValue("dims")));
      break;
    default: 
      throw new Error("Internal error");
    }
  }
}
