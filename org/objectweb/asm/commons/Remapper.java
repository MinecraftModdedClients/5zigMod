package org.objectweb.asm.commons;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

public abstract class Remapper
{
  public String mapDesc(String paramString)
  {
    Type localType = Type.getType(paramString);
    switch (localType.getSort())
    {
    case 9: 
      String str1 = mapDesc(localType.getElementType().getDescriptor());
      for (int i = 0; i < localType.getDimensions(); i++) {
        str1 = '[' + str1;
      }
      return str1;
    case 10: 
      String str2 = map(localType.getInternalName());
      if (str2 != null) {
        return 'L' + str2 + ';';
      }
      break;
    }
    return paramString;
  }
  
  private Type mapType(Type paramType)
  {
    String str;
    switch (paramType.getSort())
    {
    case 9: 
      str = mapDesc(paramType.getElementType().getDescriptor());
      for (int i = 0; i < paramType.getDimensions(); i++) {
        str = '[' + str;
      }
      return Type.getType(str);
    case 10: 
      str = map(paramType.getInternalName());
      return str != null ? Type.getObjectType(str) : paramType;
    case 11: 
      return Type.getMethodType(mapMethodDesc(paramType.getDescriptor()));
    }
    return paramType;
  }
  
  public String mapType(String paramString)
  {
    if (paramString == null) {
      return null;
    }
    return mapType(Type.getObjectType(paramString)).getInternalName();
  }
  
  public String[] mapTypes(String[] paramArrayOfString)
  {
    String[] arrayOfString = null;
    int i = 0;
    for (int j = 0; j < paramArrayOfString.length; j++)
    {
      String str1 = paramArrayOfString[j];
      String str2 = map(str1);
      if ((str2 != null) && (arrayOfString == null))
      {
        arrayOfString = new String[paramArrayOfString.length];
        if (j > 0) {
          System.arraycopy(paramArrayOfString, 0, arrayOfString, 0, j);
        }
        i = 1;
      }
      if (i != 0) {
        arrayOfString[j] = (str2 == null ? str1 : str2);
      }
    }
    return i != 0 ? arrayOfString : paramArrayOfString;
  }
  
  public String mapMethodDesc(String paramString)
  {
    if ("()V".equals(paramString)) {
      return paramString;
    }
    Type[] arrayOfType = Type.getArgumentTypes(paramString);
    StringBuffer localStringBuffer = new StringBuffer("(");
    for (int i = 0; i < arrayOfType.length; i++) {
      localStringBuffer.append(mapDesc(arrayOfType[i].getDescriptor()));
    }
    Type localType = Type.getReturnType(paramString);
    if (localType == Type.VOID_TYPE)
    {
      localStringBuffer.append(")V");
      return localStringBuffer.toString();
    }
    localStringBuffer.append(')').append(mapDesc(localType.getDescriptor()));
    return localStringBuffer.toString();
  }
  
  public Object mapValue(Object paramObject)
  {
    if ((paramObject instanceof Type)) {
      return mapType((Type)paramObject);
    }
    if ((paramObject instanceof Handle))
    {
      Handle localHandle = (Handle)paramObject;
      return new Handle(localHandle.getTag(), mapType(localHandle.getOwner()), mapMethodName(localHandle.getOwner(), localHandle.getName(), localHandle.getDesc()), mapMethodDesc(localHandle.getDesc()));
    }
    return paramObject;
  }
  
  public String mapSignature(String paramString, boolean paramBoolean)
  {
    if (paramString == null) {
      return null;
    }
    SignatureReader localSignatureReader = new SignatureReader(paramString);
    SignatureWriter localSignatureWriter = new SignatureWriter();
    SignatureVisitor localSignatureVisitor = createRemappingSignatureAdapter(localSignatureWriter);
    if (paramBoolean) {
      localSignatureReader.acceptType(localSignatureVisitor);
    } else {
      localSignatureReader.accept(localSignatureVisitor);
    }
    return localSignatureWriter.toString();
  }
  
  protected SignatureVisitor createRemappingSignatureAdapter(SignatureVisitor paramSignatureVisitor)
  {
    return new RemappingSignatureAdapter(paramSignatureVisitor, this);
  }
  
  public String mapMethodName(String paramString1, String paramString2, String paramString3)
  {
    return paramString2;
  }
  
  public String mapInvokeDynamicMethodName(String paramString1, String paramString2)
  {
    return paramString1;
  }
  
  public String mapFieldName(String paramString1, String paramString2, String paramString3)
  {
    return paramString2;
  }
  
  public String map(String paramString)
  {
    return paramString;
  }
}
