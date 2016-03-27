package org.objectweb.asm.xml;

import java.util.ArrayList;
import java.util.HashMap;
import org.objectweb.asm.ClassVisitor;

final class ASMContentHandler$InterfacesRule
  extends ASMContentHandler.Rule
{
  final ASMContentHandler this$0;
  
  ASMContentHandler$InterfacesRule(ASMContentHandler paramASMContentHandler)
  {
    super(paramASMContentHandler);
  }
  
  public final void end(String paramString)
  {
    HashMap localHashMap = (HashMap)this.this$0.pop();
    int i = ((Integer)localHashMap.get("version")).intValue();
    int j = getAccess((String)localHashMap.get("access"));
    String str1 = (String)localHashMap.get("name");
    String str2 = (String)localHashMap.get("signature");
    String str3 = (String)localHashMap.get("parent");
    ArrayList localArrayList = (ArrayList)localHashMap.get("interfaces");
    String[] arrayOfString = (String[])localArrayList.toArray(new String[localArrayList.size()]);
    this.this$0.cv.visit(i, j, str1, str2, str3, arrayOfString);
    this.this$0.push(this.this$0.cv);
  }
}
