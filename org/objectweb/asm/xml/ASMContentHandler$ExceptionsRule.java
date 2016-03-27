package org.objectweb.asm.xml;

import java.util.ArrayList;
import java.util.HashMap;
import org.objectweb.asm.ClassVisitor;

final class ASMContentHandler$ExceptionsRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$ExceptionsRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void end(String paramString)
  {
    HashMap localHashMap = (HashMap)this.this$0.pop();
    int i = getAccess((String)localHashMap.get("access"));
    String str1 = (String)localHashMap.get("name");
    String str2 = (String)localHashMap.get("desc");
    String str3 = (String)localHashMap.get("signature");
    ArrayList localArrayList = (ArrayList)localHashMap.get("exceptions");
    String[] arrayOfString = (String[])localArrayList.toArray(new String[localArrayList.size()]);
    this.this$0.push(this.this$0.cv.visitMethod(i, str1, str2, str3, arrayOfString));
  }
}
