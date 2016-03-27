package org.objectweb.asm.xml;

import java.util.ArrayList;
import java.util.HashMap;
import org.objectweb.asm.MethodVisitor;
import org.xml.sax.Attributes;

final class ASMContentHandler$FrameRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$FrameRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public void begin(String paramString, Attributes paramAttributes)
  {
    HashMap localHashMap = new HashMap();
    localHashMap.put("local", new ArrayList());
    localHashMap.put("stack", new ArrayList());
    this.this$0.push(paramAttributes.getValue("type"));
    this.this$0.push(paramAttributes.getValue("count") == null ? "0" : paramAttributes.getValue("count"));
    this.this$0.push(localHashMap);
  }
  
  public void end(String paramString)
  {
    HashMap localHashMap = (HashMap)this.this$0.pop();
    ArrayList localArrayList1 = (ArrayList)localHashMap.get("local");
    int i = localArrayList1.size();
    Object[] arrayOfObject1 = localArrayList1.toArray();
    ArrayList localArrayList2 = (ArrayList)localHashMap.get("stack");
    int j = localArrayList2.size();
    Object[] arrayOfObject2 = localArrayList2.toArray();
    String str1 = (String)this.this$0.pop();
    String str2 = (String)this.this$0.pop();
    if ("NEW".equals(str2)) {
      getCodeVisitor().visitFrame(-1, i, arrayOfObject1, j, arrayOfObject2);
    } else if ("FULL".equals(str2)) {
      getCodeVisitor().visitFrame(0, i, arrayOfObject1, j, arrayOfObject2);
    } else if ("APPEND".equals(str2)) {
      getCodeVisitor().visitFrame(1, i, arrayOfObject1, 0, null);
    } else if ("CHOP".equals(str2)) {
      getCodeVisitor().visitFrame(2, Integer.parseInt(str1), null, 0, null);
    } else if ("SAME".equals(str2)) {
      getCodeVisitor().visitFrame(3, 0, null, 0, null);
    } else if ("SAME1".equals(str2)) {
      getCodeVisitor().visitFrame(4, 0, null, j, arrayOfObject2);
    }
  }
}
