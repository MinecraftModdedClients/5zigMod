package org.objectweb.asm.xml;

import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

final class ASMContentHandler$InvokeDynamicBsmArgumentsRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$InvokeDynamicBsmArgumentsRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void begin(String paramString, Attributes paramAttributes)
    throws SAXException
  {
    ArrayList localArrayList = (ArrayList)this.this$0.peek();
    localArrayList.add(getValue(paramAttributes.getValue("desc"), paramAttributes.getValue("cst")));
  }
}
