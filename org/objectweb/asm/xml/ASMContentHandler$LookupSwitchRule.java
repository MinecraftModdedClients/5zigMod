package org.objectweb.asm.xml;

import java.util.ArrayList;
import java.util.HashMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.xml.sax.Attributes;

final class ASMContentHandler$LookupSwitchRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$LookupSwitchRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void begin(String paramString, Attributes paramAttributes)
  {
    HashMap localHashMap = new HashMap();
    localHashMap.put("dflt", paramAttributes.getValue("dflt"));
    localHashMap.put("labels", new ArrayList());
    localHashMap.put("keys", new ArrayList());
    this.this$0.push(localHashMap);
  }
  
  public final void end(String paramString)
  {
    HashMap localHashMap = (HashMap)this.this$0.pop();
    Label localLabel = getLabel(localHashMap.get("dflt"));
    ArrayList localArrayList1 = (ArrayList)localHashMap.get("keys");
    ArrayList localArrayList2 = (ArrayList)localHashMap.get("labels");
    Label[] arrayOfLabel = (Label[])localArrayList2.toArray(new Label[localArrayList2.size()]);
    int[] arrayOfInt = new int[localArrayList1.size()];
    for (int i = 0; i < arrayOfInt.length; i++) {
      arrayOfInt[i] = Integer.parseInt((String)localArrayList1.get(i));
    }
    getCodeVisitor().visitLookupSwitchInsn(localLabel, arrayOfInt, arrayOfLabel);
  }
}
