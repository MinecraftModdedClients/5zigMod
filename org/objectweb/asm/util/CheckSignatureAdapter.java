package org.objectweb.asm.util;

import org.objectweb.asm.signature.SignatureVisitor;

public class CheckSignatureAdapter
  extends SignatureVisitor
{
  public static final int CLASS_SIGNATURE = 0;
  public static final int METHOD_SIGNATURE = 1;
  public static final int TYPE_SIGNATURE = 2;
  private final int type;
  private int state;
  private boolean canBeVoid;
  private final SignatureVisitor sv;
  
  public CheckSignatureAdapter(int paramInt, SignatureVisitor paramSignatureVisitor)
  {
    this(327680, paramInt, paramSignatureVisitor);
  }
  
  protected CheckSignatureAdapter(int paramInt1, int paramInt2, SignatureVisitor paramSignatureVisitor)
  {
    super(paramInt1);
    this.type = paramInt2;
    this.state = 1;
    this.sv = paramSignatureVisitor;
  }
  
  public void visitFormalTypeParameter(String paramString)
  {
    if ((this.type == 2) || ((this.state != 1) && (this.state != 2) && (this.state != 4))) {
      throw new IllegalStateException();
    }
    CheckMethodAdapter.checkIdentifier(paramString, "formal type parameter");
    this.state = 2;
    if (this.sv != null) {
      this.sv.visitFormalTypeParameter(paramString);
    }
  }
  
  public SignatureVisitor visitClassBound()
  {
    if (this.state != 2) {
      throw new IllegalStateException();
    }
    this.state = 4;
    SignatureVisitor localSignatureVisitor = this.sv == null ? null : this.sv.visitClassBound();
    return new CheckSignatureAdapter(2, localSignatureVisitor);
  }
  
  public SignatureVisitor visitInterfaceBound()
  {
    if ((this.state != 2) && (this.state != 4)) {
      throw new IllegalArgumentException();
    }
    SignatureVisitor localSignatureVisitor = this.sv == null ? null : this.sv.visitInterfaceBound();
    return new CheckSignatureAdapter(2, localSignatureVisitor);
  }
  
  public SignatureVisitor visitSuperclass()
  {
    if ((this.type != 0) || ((this.state & 0x7) == 0)) {
      throw new IllegalArgumentException();
    }
    this.state = 8;
    SignatureVisitor localSignatureVisitor = this.sv == null ? null : this.sv.visitSuperclass();
    return new CheckSignatureAdapter(2, localSignatureVisitor);
  }
  
  public SignatureVisitor visitInterface()
  {
    if (this.state != 8) {
      throw new IllegalStateException();
    }
    SignatureVisitor localSignatureVisitor = this.sv == null ? null : this.sv.visitInterface();
    return new CheckSignatureAdapter(2, localSignatureVisitor);
  }
  
  public SignatureVisitor visitParameterType()
  {
    if ((this.type != 1) || ((this.state & 0x17) == 0)) {
      throw new IllegalArgumentException();
    }
    this.state = 16;
    SignatureVisitor localSignatureVisitor = this.sv == null ? null : this.sv.visitParameterType();
    return new CheckSignatureAdapter(2, localSignatureVisitor);
  }
  
  public SignatureVisitor visitReturnType()
  {
    if ((this.type != 1) || ((this.state & 0x17) == 0)) {
      throw new IllegalArgumentException();
    }
    this.state = 32;
    SignatureVisitor localSignatureVisitor = this.sv == null ? null : this.sv.visitReturnType();
    CheckSignatureAdapter localCheckSignatureAdapter = new CheckSignatureAdapter(2, localSignatureVisitor);
    localCheckSignatureAdapter.canBeVoid = true;
    return localCheckSignatureAdapter;
  }
  
  public SignatureVisitor visitExceptionType()
  {
    if (this.state != 32) {
      throw new IllegalStateException();
    }
    SignatureVisitor localSignatureVisitor = this.sv == null ? null : this.sv.visitExceptionType();
    return new CheckSignatureAdapter(2, localSignatureVisitor);
  }
  
  public void visitBaseType(char paramChar)
  {
    if ((this.type != 2) || (this.state != 1)) {
      throw new IllegalStateException();
    }
    if (paramChar == 'V')
    {
      if (!this.canBeVoid) {
        throw new IllegalArgumentException();
      }
    }
    else if ("ZCBSIFJD".indexOf(paramChar) == -1) {
      throw new IllegalArgumentException();
    }
    this.state = 64;
    if (this.sv != null) {
      this.sv.visitBaseType(paramChar);
    }
  }
  
  public void visitTypeVariable(String paramString)
  {
    if ((this.type != 2) || (this.state != 1)) {
      throw new IllegalStateException();
    }
    CheckMethodAdapter.checkIdentifier(paramString, "type variable");
    this.state = 64;
    if (this.sv != null) {
      this.sv.visitTypeVariable(paramString);
    }
  }
  
  public SignatureVisitor visitArrayType()
  {
    if ((this.type != 2) || (this.state != 1)) {
      throw new IllegalStateException();
    }
    this.state = 64;
    SignatureVisitor localSignatureVisitor = this.sv == null ? null : this.sv.visitArrayType();
    return new CheckSignatureAdapter(2, localSignatureVisitor);
  }
  
  public void visitClassType(String paramString)
  {
    if ((this.type != 2) || (this.state != 1)) {
      throw new IllegalStateException();
    }
    CheckMethodAdapter.checkInternalName(paramString, "class name");
    this.state = 128;
    if (this.sv != null) {
      this.sv.visitClassType(paramString);
    }
  }
  
  public void visitInnerClassType(String paramString)
  {
    if (this.state != 128) {
      throw new IllegalStateException();
    }
    CheckMethodAdapter.checkIdentifier(paramString, "inner class name");
    if (this.sv != null) {
      this.sv.visitInnerClassType(paramString);
    }
  }
  
  public void visitTypeArgument()
  {
    if (this.state != 128) {
      throw new IllegalStateException();
    }
    if (this.sv != null) {
      this.sv.visitTypeArgument();
    }
  }
  
  public SignatureVisitor visitTypeArgument(char paramChar)
  {
    if (this.state != 128) {
      throw new IllegalStateException();
    }
    if ("+-=".indexOf(paramChar) == -1) {
      throw new IllegalArgumentException();
    }
    SignatureVisitor localSignatureVisitor = this.sv == null ? null : this.sv.visitTypeArgument(paramChar);
    return new CheckSignatureAdapter(2, localSignatureVisitor);
  }
  
  public void visitEnd()
  {
    if (this.state != 128) {
      throw new IllegalStateException();
    }
    this.state = 256;
    if (this.sv != null) {
      this.sv.visitEnd();
    }
  }
}
