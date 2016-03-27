package org.objectweb.asm.xml;

import java.util.Map;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public abstract class ASMContentHandler$Rule
{
  final ASMContentHandler this$0;
  static Class class$org$objectweb$asm$Type = class$("org.objectweb.asm.Type");
  static Class class$org$objectweb$asm$Handle = class$("org.objectweb.asm.Handle");
  
  protected ASMContentHandler$Rule(ASMContentHandler paramASMContentHandler) {}
  
  public void begin(String paramString, Attributes paramAttributes)
    throws SAXException
  {}
  
  public void end(String paramString) {}
  
  protected final Object getValue(String paramString1, String paramString2)
    throws SAXException
  {
    Object localObject = null;
    if (paramString2 != null) {
      if ("Ljava/lang/String;".equals(paramString1)) {
        localObject = decode(paramString2);
      } else if (("Ljava/lang/Integer;".equals(paramString1)) || ("I".equals(paramString1)) || ("S".equals(paramString1)) || ("B".equals(paramString1)) || ("C".equals(paramString1)) || ("Z".equals(paramString1))) {
        localObject = new Integer(paramString2);
      } else if ("Ljava/lang/Short;".equals(paramString1)) {
        localObject = new Short(paramString2);
      } else if ("Ljava/lang/Byte;".equals(paramString1)) {
        localObject = new Byte(paramString2);
      } else if ("Ljava/lang/Character;".equals(paramString1)) {
        localObject = new Character(decode(paramString2).charAt(0));
      } else if ("Ljava/lang/Boolean;".equals(paramString1)) {
        localObject = Boolean.valueOf(paramString2);
      } else if (("Ljava/lang/Long;".equals(paramString1)) || ("J".equals(paramString1))) {
        localObject = new Long(paramString2);
      } else if (("Ljava/lang/Float;".equals(paramString1)) || ("F".equals(paramString1))) {
        localObject = new Float(paramString2);
      } else if (("Ljava/lang/Double;".equals(paramString1)) || ("D".equals(paramString1))) {
        localObject = new Double(paramString2);
      } else if (Type.getDescriptor(class$org$objectweb$asm$Type).equals(paramString1)) {
        localObject = Type.getType(paramString2);
      } else if (Type.getDescriptor(class$org$objectweb$asm$Handle).equals(paramString1)) {
        localObject = decodeHandle(paramString2);
      } else {
        throw new SAXException("Invalid value:" + paramString2 + " desc:" + paramString1 + " ctx:" + this);
      }
    }
    return localObject;
  }
  
  Handle decodeHandle(String paramString)
    throws SAXException
  {
    try
    {
      int i = paramString.indexOf('.');
      int j = paramString.indexOf('(', i + 1);
      int k = paramString.lastIndexOf('(');
      int m = Integer.parseInt(paramString.substring(k + 1, paramString.length() - 1));
      String str1 = paramString.substring(0, i);
      String str2 = paramString.substring(i + 1, j);
      String str3 = paramString.substring(j, k - 1);
      return new Handle(m, str1, str2, str3);
    }
    catch (RuntimeException localRuntimeException)
    {
      throw new SAXException("Malformed handle " + paramString, localRuntimeException);
    }
  }
  
  private final String decode(String paramString)
    throws SAXException
  {
    StringBuffer localStringBuffer = new StringBuffer(paramString.length());
    try
    {
      for (int i = 0; i < paramString.length(); i++)
      {
        char c = paramString.charAt(i);
        if (c == '\\')
        {
          i++;
          c = paramString.charAt(i);
          if (c == '\\')
          {
            localStringBuffer.append('\\');
          }
          else
          {
            i++;
            localStringBuffer.append((char)Integer.parseInt(paramString.substring(i, i + 4), 16));
            i += 3;
          }
        }
        else
        {
          localStringBuffer.append(c);
        }
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      throw new SAXException(localRuntimeException);
    }
    return localStringBuffer.toString();
  }
  
  protected final Label getLabel(Object paramObject)
  {
    Label localLabel = (Label)this.this$0.labels.get(paramObject);
    if (localLabel == null)
    {
      localLabel = new Label();
      this.this$0.labels.put(paramObject, localLabel);
    }
    return localLabel;
  }
  
  protected final MethodVisitor getCodeVisitor()
  {
    return (MethodVisitor)this.this$0.peek();
  }
  
  protected final int getAccess(String paramString)
  {
    int i = 0;
    if (paramString.indexOf("public") != -1) {
      i |= 0x1;
    }
    if (paramString.indexOf("private") != -1) {
      i |= 0x2;
    }
    if (paramString.indexOf("protected") != -1) {
      i |= 0x4;
    }
    if (paramString.indexOf("static") != -1) {
      i |= 0x8;
    }
    if (paramString.indexOf("final") != -1) {
      i |= 0x10;
    }
    if (paramString.indexOf("super") != -1) {
      i |= 0x20;
    }
    if (paramString.indexOf("synchronized") != -1) {
      i |= 0x20;
    }
    if (paramString.indexOf("volatile") != -1) {
      i |= 0x40;
    }
    if (paramString.indexOf("bridge") != -1) {
      i |= 0x40;
    }
    if (paramString.indexOf("varargs") != -1) {
      i |= 0x80;
    }
    if (paramString.indexOf("transient") != -1) {
      i |= 0x80;
    }
    if (paramString.indexOf("native") != -1) {
      i |= 0x100;
    }
    if (paramString.indexOf("interface") != -1) {
      i |= 0x200;
    }
    if (paramString.indexOf("abstract") != -1) {
      i |= 0x400;
    }
    if (paramString.indexOf("strict") != -1) {
      i |= 0x800;
    }
    if (paramString.indexOf("synthetic") != -1) {
      i |= 0x1000;
    }
    if (paramString.indexOf("annotation") != -1) {
      i |= 0x2000;
    }
    if (paramString.indexOf("enum") != -1) {
      i |= 0x4000;
    }
    if (paramString.indexOf("deprecated") != -1) {
      i |= 0x20000;
    }
    if (paramString.indexOf("mandated") != -1) {
      i |= 0x8000;
    }
    return i;
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
