package org.objectweb.asm.xml;

import java.util.ArrayList;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

final class ASMContentHandler$InvokeDynamicRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$InvokeDynamicRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void begin(String paramString, Attributes paramAttributes)
    throws SAXException
  {
    this.this$0.push(paramAttributes.getValue("name"));
    this.this$0.push(paramAttributes.getValue("desc"));
    this.this$0.push(decodeHandle(paramAttributes.getValue("bsm")));
    this.this$0.push(new ArrayList());
  }
  
  public final void end(String paramString)
  {
    ArrayList localArrayList = (ArrayList)this.this$0.pop();
    Handle localHandle = (Handle)this.this$0.pop();
    String str1 = (String)this.this$0.pop();
    String str2 = (String)this.this$0.pop();
    getCodeVisitor().visitInvokeDynamicInsn(str2, str1, localHandle, localArrayList.toArray());
  }
}
