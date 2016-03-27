package org.objectweb.asm.xml;

import java.util.ArrayList;
import java.util.HashMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.xml.sax.Attributes;

final class ASMContentHandler$TableSwitchRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$TableSwitchRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void begin(String paramString, Attributes paramAttributes)
  {
    HashMap localHashMap = new HashMap();
    localHashMap.put("min", paramAttributes.getValue("min"));
    localHashMap.put("max", paramAttributes.getValue("max"));
    localHashMap.put("dflt", paramAttributes.getValue("dflt"));
    localHashMap.put("labels", new ArrayList());
    this.this$0.push(localHashMap);
  }
  
  public final void end(String paramString)
  {
    HashMap localHashMap = (HashMap)this.this$0.pop();
    int i = Integer.parseInt((String)localHashMap.get("min"));
    int j = Integer.parseInt((String)localHashMap.get("max"));
    Label localLabel = getLabel(localHashMap.get("dflt"));
    ArrayList localArrayList = (ArrayList)localHashMap.get("labels");
    Label[] arrayOfLabel = (Label[])localArrayList.toArray(new Label[localArrayList.size()]);
    getCodeVisitor().visitTableSwitchInsn(i, j, localLabel, arrayOfLabel);
  }
}
