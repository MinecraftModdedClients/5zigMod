package org.objectweb.asm.tree.analysis;

import java.util.List;
import org.objectweb.asm.Type;

public class SimpleVerifier
  extends BasicVerifier
{
  private final Type currentClass;
  private final Type currentSuperClass;
  private final List currentClassInterfaces;
  private final boolean isInterface;
  private ClassLoader loader = getClass().getClassLoader();
  static Class class$java$lang$Object = class$("java.lang.Object");
  
  public SimpleVerifier()
  {
    this(null, null, false);
  }
  
  public SimpleVerifier(Type paramType1, Type paramType2, boolean paramBoolean)
  {
    this(paramType1, paramType2, null, paramBoolean);
  }
  
  public SimpleVerifier(Type paramType1, Type paramType2, List paramList, boolean paramBoolean)
  {
    this(327680, paramType1, paramType2, paramList, paramBoolean);
  }
  
  protected SimpleVerifier(int paramInt, Type paramType1, Type paramType2, List paramList, boolean paramBoolean)
  {
    super(paramInt);
    this.currentClass = paramType1;
    this.currentSuperClass = paramType2;
    this.currentClassInterfaces = paramList;
    this.isInterface = paramBoolean;
  }
  
  public void setClassLoader(ClassLoader paramClassLoader)
  {
    this.loader = paramClassLoader;
  }
  
  public BasicValue newValue(Type paramType)
  {
    if (paramType == null) {
      return BasicValue.UNINITIALIZED_VALUE;
    }
    int i = paramType.getSort() == 9 ? 1 : 0;
    if (i != 0) {
      switch (paramType.getElementType().getSort())
      {
      case 1: 
      case 2: 
      case 3: 
      case 4: 
        return new BasicValue(paramType);
      }
    }
    BasicValue localBasicValue = super.newValue(paramType);
    if (BasicValue.REFERENCE_VALUE.equals(localBasicValue)) {
      if (i != 0)
      {
        localBasicValue = newValue(paramType.getElementType());
        String str = localBasicValue.getType().getDescriptor();
        for (int j = 0; j < paramType.getDimensions(); j++) {
          str = '[' + str;
        }
        localBasicValue = new BasicValue(Type.getType(str));
      }
      else
      {
        localBasicValue = new BasicValue(paramType);
      }
    }
    return localBasicValue;
  }
  
  protected boolean isArrayValue(BasicValue paramBasicValue)
  {
    Type localType = paramBasicValue.getType();
    return (localType != null) && (("Lnull;".equals(localType.getDescriptor())) || (localType.getSort() == 9));
  }
  
  protected BasicValue getElementValue(BasicValue paramBasicValue)
    throws AnalyzerException
  {
    Type localType = paramBasicValue.getType();
    if (localType != null)
    {
      if (localType.getSort() == 9) {
        return newValue(Type.getType(localType.getDescriptor().substring(1)));
      }
      if ("Lnull;".equals(localType.getDescriptor())) {
        return paramBasicValue;
      }
    }
    throw new Error("Internal error");
  }
  
  protected boolean isSubTypeOf(BasicValue paramBasicValue1, BasicValue paramBasicValue2)
  {
    Type localType1 = paramBasicValue2.getType();
    Type localType2 = paramBasicValue1.getType();
    switch (localType1.getSort())
    {
    case 5: 
    case 6: 
    case 7: 
    case 8: 
      return localType2.equals(localType1);
    case 9: 
    case 10: 
      if ("Lnull;".equals(localType2.getDescriptor())) {
        return true;
      }
      if ((localType2.getSort() == 10) || (localType2.getSort() == 9)) {
        return isAssignableFrom(localType1, localType2);
      }
      return false;
    }
    throw new Error("Internal error");
  }
  
  public BasicValue merge(BasicValue paramBasicValue1, BasicValue paramBasicValue2)
  {
    if (!paramBasicValue1.equals(paramBasicValue2))
    {
      Type localType1 = paramBasicValue1.getType();
      Type localType2 = paramBasicValue2.getType();
      if ((localType1 != null) && ((localType1.getSort() == 10) || (localType1.getSort() == 9)) && (localType2 != null) && ((localType2.getSort() == 10) || (localType2.getSort() == 9)))
      {
        if ("Lnull;".equals(localType1.getDescriptor())) {
          return paramBasicValue2;
        }
        if ("Lnull;".equals(localType2.getDescriptor())) {
          return paramBasicValue1;
        }
        if (isAssignableFrom(localType1, localType2)) {
          return paramBasicValue1;
        }
        if (isAssignableFrom(localType2, localType1)) {
          return paramBasicValue2;
        }
        do
        {
          if ((localType1 == null) || (isInterface(localType1))) {
            return BasicValue.REFERENCE_VALUE;
          }
          localType1 = getSuperClass(localType1);
        } while (!isAssignableFrom(localType1, localType2));
        return newValue(localType1);
      }
      return BasicValue.UNINITIALIZED_VALUE;
    }
    return paramBasicValue1;
  }
  
  protected boolean isInterface(Type paramType)
  {
    if ((this.currentClass != null) && (paramType.equals(this.currentClass))) {
      return this.isInterface;
    }
    return getClass(paramType).isInterface();
  }
  
  protected Type getSuperClass(Type paramType)
  {
    if ((this.currentClass != null) && (paramType.equals(this.currentClass))) {
      return this.currentSuperClass;
    }
    Class localClass = getClass(paramType).getSuperclass();
    return localClass == null ? null : Type.getType(localClass);
  }
  
  protected boolean isAssignableFrom(Type paramType1, Type paramType2)
  {
    if (paramType1.equals(paramType2)) {
      return true;
    }
    if ((this.currentClass != null) && (paramType1.equals(this.currentClass)))
    {
      if (getSuperClass(paramType2) == null) {
        return false;
      }
      if (this.isInterface) {
        return (paramType2.getSort() == 10) || (paramType2.getSort() == 9);
      }
      return isAssignableFrom(paramType1, getSuperClass(paramType2));
    }
    if ((this.currentClass != null) && (paramType2.equals(this.currentClass)))
    {
      if (isAssignableFrom(paramType1, this.currentSuperClass)) {
        return true;
      }
      if (this.currentClassInterfaces != null) {
        for (int i = 0; i < this.currentClassInterfaces.size(); i++)
        {
          Type localType = (Type)this.currentClassInterfaces.get(i);
          if (isAssignableFrom(paramType1, localType)) {
            return true;
          }
        }
      }
      return false;
    }
    Class localClass = getClass(paramType1);
    if (localClass.isInterface()) {
      localClass = class$java$lang$Object;
    }
    return localClass.isAssignableFrom(getClass(paramType2));
  }
  
  protected Class getClass(Type paramType)
  {
    try
    {
      if (paramType.getSort() == 9) {
        return Class.forName(paramType.getDescriptor().replace('/', '.'), false, this.loader);
      }
      return Class.forName(paramType.getClassName(), false, this.loader);
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      throw new RuntimeException(localClassNotFoundException.toString());
    }
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
