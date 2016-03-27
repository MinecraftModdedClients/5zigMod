package org.objectweb.asm.signature;

public class SignatureReader
{
  private final String a;
  
  public SignatureReader(String paramString)
  {
    this.a = paramString;
  }
  
  public void accept(SignatureVisitor paramSignatureVisitor)
  {
    String str = this.a;
    int i = str.length();
    if (str.charAt(0) == '<')
    {
      j = 2;
      int m;
      do
      {
        int k = str.indexOf(':', j);
        paramSignatureVisitor.visitFormalTypeParameter(str.substring(j - 1, k));
        j = k + 1;
        m = str.charAt(j);
        if ((m == 76) || (m == 91) || (m == 84)) {}
        for (j = a(str, j, paramSignatureVisitor.visitClassBound()); (m = str.charAt(j++)) == ':'; j = a(str, j, paramSignatureVisitor.visitInterfaceBound())) {}
      } while (m != 62);
    }
    else
    {
      j = 0;
    }
    if (str.charAt(j) == '(')
    {
      j++;
      while (str.charAt(j) != ')') {
        j = a(str, j, paramSignatureVisitor.visitParameterType());
      }
      for (j = a(str, j + 1, paramSignatureVisitor.visitReturnType()); j < i; j = a(str, j + 1, paramSignatureVisitor.visitExceptionType())) {}
    }
    for (int j = a(str, j, paramSignatureVisitor.visitSuperclass()); j < i; j = a(str, j, paramSignatureVisitor.visitInterface())) {}
  }
  
  public void acceptType(SignatureVisitor paramSignatureVisitor)
  {
    a(this.a, 0, paramSignatureVisitor);
  }
  
  private static int a(String paramString, int paramInt, SignatureVisitor paramSignatureVisitor)
  {
    char c;
    switch (c = paramString.charAt(paramInt++))
    {
    case 'B': 
    case 'C': 
    case 'D': 
    case 'F': 
    case 'I': 
    case 'J': 
    case 'S': 
    case 'V': 
    case 'Z': 
      paramSignatureVisitor.visitBaseType(c);
      return paramInt;
    case '[': 
      return a(paramString, paramInt, paramSignatureVisitor.visitArrayType());
    case 'T': 
      int i = paramString.indexOf(';', paramInt);
      paramSignatureVisitor.visitTypeVariable(paramString.substring(paramInt, i));
      return i + 1;
    }
    int j = paramInt;
    int k = 0;
    int m = 0;
    for (;;)
    {
      String str;
      switch (c = paramString.charAt(paramInt++))
      {
      case '.': 
      case ';': 
        if (k == 0)
        {
          str = paramString.substring(j, paramInt - 1);
          if (m != 0) {
            paramSignatureVisitor.visitInnerClassType(str);
          } else {
            paramSignatureVisitor.visitClassType(str);
          }
        }
        if (c == ';')
        {
          paramSignatureVisitor.visitEnd();
          return paramInt;
        }
        j = paramInt;
        k = 0;
        m = 1;
        break;
      case '<': 
        str = paramString.substring(j, paramInt - 1);
        if (m != 0) {
          paramSignatureVisitor.visitInnerClassType(str);
        } else {
          paramSignatureVisitor.visitClassType(str);
        }
        k = 1;
        for (;;)
        {
          switch (c = paramString.charAt(paramInt))
          {
          case '>': 
            break;
          case '*': 
            paramInt++;
            paramSignatureVisitor.visitTypeArgument();
            break;
          case '+': 
          case '-': 
            paramInt = a(paramString, paramInt + 1, paramSignatureVisitor.visitTypeArgument(c));
            break;
          default: 
            paramInt = a(paramString, paramInt, paramSignatureVisitor.visitTypeArgument('='));
          }
        }
      }
    }
  }
}
