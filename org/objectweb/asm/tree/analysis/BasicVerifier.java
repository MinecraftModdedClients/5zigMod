package org.objectweb.asm.tree.analysis;

import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class BasicVerifier
  extends BasicInterpreter
{
  public BasicVerifier()
  {
    super(327680);
  }
  
  protected BasicVerifier(int paramInt)
  {
    super(paramInt);
  }
  
  public BasicValue copyOperation(AbstractInsnNode paramAbstractInsnNode, BasicValue paramBasicValue)
    throws AnalyzerException
  {
    BasicValue localBasicValue;
    switch (paramAbstractInsnNode.getOpcode())
    {
    case 21: 
    case 54: 
      localBasicValue = BasicValue.INT_VALUE;
      break;
    case 23: 
    case 56: 
      localBasicValue = BasicValue.FLOAT_VALUE;
      break;
    case 22: 
    case 55: 
      localBasicValue = BasicValue.LONG_VALUE;
      break;
    case 24: 
    case 57: 
      localBasicValue = BasicValue.DOUBLE_VALUE;
      break;
    case 25: 
      if (!paramBasicValue.isReference()) {
        throw new AnalyzerException(paramAbstractInsnNode, null, "an object reference", paramBasicValue);
      }
      return paramBasicValue;
    case 58: 
      if ((!paramBasicValue.isReference()) && (!BasicValue.RETURNADDRESS_VALUE.equals(paramBasicValue))) {
        throw new AnalyzerException(paramAbstractInsnNode, null, "an object reference or a return address", paramBasicValue);
      }
      return paramBasicValue;
    case 26: 
    case 27: 
    case 28: 
    case 29: 
    case 30: 
    case 31: 
    case 32: 
    case 33: 
    case 34: 
    case 35: 
    case 36: 
    case 37: 
    case 38: 
    case 39: 
    case 40: 
    case 41: 
    case 42: 
    case 43: 
    case 44: 
    case 45: 
    case 46: 
    case 47: 
    case 48: 
    case 49: 
    case 50: 
    case 51: 
    case 52: 
    case 53: 
    default: 
      return paramBasicValue;
    }
    if (!localBasicValue.equals(paramBasicValue)) {
      throw new AnalyzerException(paramAbstractInsnNode, null, localBasicValue, paramBasicValue);
    }
    return paramBasicValue;
  }
  
  public BasicValue unaryOperation(AbstractInsnNode paramAbstractInsnNode, BasicValue paramBasicValue)
    throws AnalyzerException
  {
    BasicValue localBasicValue;
    switch (paramAbstractInsnNode.getOpcode())
    {
    case 116: 
    case 132: 
    case 133: 
    case 134: 
    case 135: 
    case 145: 
    case 146: 
    case 147: 
    case 153: 
    case 154: 
    case 155: 
    case 156: 
    case 157: 
    case 158: 
    case 170: 
    case 171: 
    case 172: 
    case 188: 
    case 189: 
      localBasicValue = BasicValue.INT_VALUE;
      break;
    case 118: 
    case 139: 
    case 140: 
    case 141: 
    case 174: 
      localBasicValue = BasicValue.FLOAT_VALUE;
      break;
    case 117: 
    case 136: 
    case 137: 
    case 138: 
    case 173: 
      localBasicValue = BasicValue.LONG_VALUE;
      break;
    case 119: 
    case 142: 
    case 143: 
    case 144: 
    case 175: 
      localBasicValue = BasicValue.DOUBLE_VALUE;
      break;
    case 180: 
      localBasicValue = newValue(Type.getObjectType(((FieldInsnNode)paramAbstractInsnNode).owner));
      break;
    case 192: 
      if (!paramBasicValue.isReference()) {
        throw new AnalyzerException(paramAbstractInsnNode, null, "an object reference", paramBasicValue);
      }
      return super.unaryOperation(paramAbstractInsnNode, paramBasicValue);
    case 190: 
      if (!isArrayValue(paramBasicValue)) {
        throw new AnalyzerException(paramAbstractInsnNode, null, "an array reference", paramBasicValue);
      }
      return super.unaryOperation(paramAbstractInsnNode, paramBasicValue);
    case 176: 
    case 191: 
    case 193: 
    case 194: 
    case 195: 
    case 198: 
    case 199: 
      if (!paramBasicValue.isReference()) {
        throw new AnalyzerException(paramAbstractInsnNode, null, "an object reference", paramBasicValue);
      }
      return super.unaryOperation(paramAbstractInsnNode, paramBasicValue);
    case 179: 
      localBasicValue = newValue(Type.getType(((FieldInsnNode)paramAbstractInsnNode).desc));
      break;
    case 120: 
    case 121: 
    case 122: 
    case 123: 
    case 124: 
    case 125: 
    case 126: 
    case 127: 
    case 128: 
    case 129: 
    case 130: 
    case 131: 
    case 148: 
    case 149: 
    case 150: 
    case 151: 
    case 152: 
    case 159: 
    case 160: 
    case 161: 
    case 162: 
    case 163: 
    case 164: 
    case 165: 
    case 166: 
    case 167: 
    case 168: 
    case 169: 
    case 177: 
    case 178: 
    case 181: 
    case 182: 
    case 183: 
    case 184: 
    case 185: 
    case 186: 
    case 187: 
    case 196: 
    case 197: 
    default: 
      throw new Error("Internal error.");
    }
    if (!isSubTypeOf(paramBasicValue, localBasicValue)) {
      throw new AnalyzerException(paramAbstractInsnNode, null, localBasicValue, paramBasicValue);
    }
    return super.unaryOperation(paramAbstractInsnNode, paramBasicValue);
  }
  
  public BasicValue binaryOperation(AbstractInsnNode paramAbstractInsnNode, BasicValue paramBasicValue1, BasicValue paramBasicValue2)
    throws AnalyzerException
  {
    BasicValue localBasicValue1;
    BasicValue localBasicValue2;
    switch (paramAbstractInsnNode.getOpcode())
    {
    case 46: 
      localBasicValue1 = newValue(Type.getType("[I"));
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 51: 
      if (isSubTypeOf(paramBasicValue1, newValue(Type.getType("[Z")))) {
        localBasicValue1 = newValue(Type.getType("[Z"));
      } else {
        localBasicValue1 = newValue(Type.getType("[B"));
      }
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 52: 
      localBasicValue1 = newValue(Type.getType("[C"));
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 53: 
      localBasicValue1 = newValue(Type.getType("[S"));
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 47: 
      localBasicValue1 = newValue(Type.getType("[J"));
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 48: 
      localBasicValue1 = newValue(Type.getType("[F"));
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 49: 
      localBasicValue1 = newValue(Type.getType("[D"));
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 50: 
      localBasicValue1 = newValue(Type.getType("[Ljava/lang/Object;"));
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 96: 
    case 100: 
    case 104: 
    case 108: 
    case 112: 
    case 120: 
    case 122: 
    case 124: 
    case 126: 
    case 128: 
    case 130: 
    case 159: 
    case 160: 
    case 161: 
    case 162: 
    case 163: 
    case 164: 
      localBasicValue1 = BasicValue.INT_VALUE;
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 98: 
    case 102: 
    case 106: 
    case 110: 
    case 114: 
    case 149: 
    case 150: 
      localBasicValue1 = BasicValue.FLOAT_VALUE;
      localBasicValue2 = BasicValue.FLOAT_VALUE;
      break;
    case 97: 
    case 101: 
    case 105: 
    case 109: 
    case 113: 
    case 127: 
    case 129: 
    case 131: 
    case 148: 
      localBasicValue1 = BasicValue.LONG_VALUE;
      localBasicValue2 = BasicValue.LONG_VALUE;
      break;
    case 121: 
    case 123: 
    case 125: 
      localBasicValue1 = BasicValue.LONG_VALUE;
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 99: 
    case 103: 
    case 107: 
    case 111: 
    case 115: 
    case 151: 
    case 152: 
      localBasicValue1 = BasicValue.DOUBLE_VALUE;
      localBasicValue2 = BasicValue.DOUBLE_VALUE;
      break;
    case 165: 
    case 166: 
      localBasicValue1 = BasicValue.REFERENCE_VALUE;
      localBasicValue2 = BasicValue.REFERENCE_VALUE;
      break;
    case 181: 
      FieldInsnNode localFieldInsnNode = (FieldInsnNode)paramAbstractInsnNode;
      localBasicValue1 = newValue(Type.getObjectType(localFieldInsnNode.owner));
      localBasicValue2 = newValue(Type.getType(localFieldInsnNode.desc));
      break;
    case 54: 
    case 55: 
    case 56: 
    case 57: 
    case 58: 
    case 59: 
    case 60: 
    case 61: 
    case 62: 
    case 63: 
    case 64: 
    case 65: 
    case 66: 
    case 67: 
    case 68: 
    case 69: 
    case 70: 
    case 71: 
    case 72: 
    case 73: 
    case 74: 
    case 75: 
    case 76: 
    case 77: 
    case 78: 
    case 79: 
    case 80: 
    case 81: 
    case 82: 
    case 83: 
    case 84: 
    case 85: 
    case 86: 
    case 87: 
    case 88: 
    case 89: 
    case 90: 
    case 91: 
    case 92: 
    case 93: 
    case 94: 
    case 95: 
    case 116: 
    case 117: 
    case 118: 
    case 119: 
    case 132: 
    case 133: 
    case 134: 
    case 135: 
    case 136: 
    case 137: 
    case 138: 
    case 139: 
    case 140: 
    case 141: 
    case 142: 
    case 143: 
    case 144: 
    case 145: 
    case 146: 
    case 147: 
    case 153: 
    case 154: 
    case 155: 
    case 156: 
    case 157: 
    case 158: 
    case 167: 
    case 168: 
    case 169: 
    case 170: 
    case 171: 
    case 172: 
    case 173: 
    case 174: 
    case 175: 
    case 176: 
    case 177: 
    case 178: 
    case 179: 
    case 180: 
    default: 
      throw new Error("Internal error.");
    }
    if (!isSubTypeOf(paramBasicValue1, localBasicValue1)) {
      throw new AnalyzerException(paramAbstractInsnNode, "First argument", localBasicValue1, paramBasicValue1);
    }
    if (!isSubTypeOf(paramBasicValue2, localBasicValue2)) {
      throw new AnalyzerException(paramAbstractInsnNode, "Second argument", localBasicValue2, paramBasicValue2);
    }
    if (paramAbstractInsnNode.getOpcode() == 50) {
      return getElementValue(paramBasicValue1);
    }
    return super.binaryOperation(paramAbstractInsnNode, paramBasicValue1, paramBasicValue2);
  }
  
  public BasicValue ternaryOperation(AbstractInsnNode paramAbstractInsnNode, BasicValue paramBasicValue1, BasicValue paramBasicValue2, BasicValue paramBasicValue3)
    throws AnalyzerException
  {
    BasicValue localBasicValue1;
    BasicValue localBasicValue2;
    switch (paramAbstractInsnNode.getOpcode())
    {
    case 79: 
      localBasicValue1 = newValue(Type.getType("[I"));
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 84: 
      if (isSubTypeOf(paramBasicValue1, newValue(Type.getType("[Z")))) {
        localBasicValue1 = newValue(Type.getType("[Z"));
      } else {
        localBasicValue1 = newValue(Type.getType("[B"));
      }
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 85: 
      localBasicValue1 = newValue(Type.getType("[C"));
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 86: 
      localBasicValue1 = newValue(Type.getType("[S"));
      localBasicValue2 = BasicValue.INT_VALUE;
      break;
    case 80: 
      localBasicValue1 = newValue(Type.getType("[J"));
      localBasicValue2 = BasicValue.LONG_VALUE;
      break;
    case 81: 
      localBasicValue1 = newValue(Type.getType("[F"));
      localBasicValue2 = BasicValue.FLOAT_VALUE;
      break;
    case 82: 
      localBasicValue1 = newValue(Type.getType("[D"));
      localBasicValue2 = BasicValue.DOUBLE_VALUE;
      break;
    case 83: 
      localBasicValue1 = paramBasicValue1;
      localBasicValue2 = BasicValue.REFERENCE_VALUE;
      break;
    default: 
      throw new Error("Internal error.");
    }
    if (!isSubTypeOf(paramBasicValue1, localBasicValue1)) {
      throw new AnalyzerException(paramAbstractInsnNode, "First argument", "a " + localBasicValue1 + " array reference", paramBasicValue1);
    }
    if (!BasicValue.INT_VALUE.equals(paramBasicValue2)) {
      throw new AnalyzerException(paramAbstractInsnNode, "Second argument", BasicValue.INT_VALUE, paramBasicValue2);
    }
    if (!isSubTypeOf(paramBasicValue3, localBasicValue2)) {
      throw new AnalyzerException(paramAbstractInsnNode, "Third argument", localBasicValue2, paramBasicValue3);
    }
    return null;
  }
  
  public BasicValue naryOperation(AbstractInsnNode paramAbstractInsnNode, List paramList)
    throws AnalyzerException
  {
    int i = paramAbstractInsnNode.getOpcode();
    int j;
    if (i == 197)
    {
      for (j = 0; j < paramList.size(); j++) {
        if (!BasicValue.INT_VALUE.equals(paramList.get(j))) {
          throw new AnalyzerException(paramAbstractInsnNode, null, BasicValue.INT_VALUE, (Value)paramList.get(j));
        }
      }
    }
    else
    {
      j = 0;
      int k = 0;
      if ((i != 184) && (i != 186))
      {
        localObject = Type.getObjectType(((MethodInsnNode)paramAbstractInsnNode).owner);
        if (!isSubTypeOf((BasicValue)paramList.get(j++), newValue((Type)localObject))) {
          throw new AnalyzerException(paramAbstractInsnNode, "Method owner", newValue((Type)localObject), (Value)paramList.get(0));
        }
      }
      Object localObject = i == 186 ? ((InvokeDynamicInsnNode)paramAbstractInsnNode).desc : ((MethodInsnNode)paramAbstractInsnNode).desc;
      Type[] arrayOfType = Type.getArgumentTypes((String)localObject);
      while (j < paramList.size())
      {
        BasicValue localBasicValue1 = newValue(arrayOfType[(k++)]);
        BasicValue localBasicValue2 = (BasicValue)paramList.get(j++);
        if (!isSubTypeOf(localBasicValue2, localBasicValue1)) {
          throw new AnalyzerException(paramAbstractInsnNode, "Argument " + k, localBasicValue1, localBasicValue2);
        }
      }
    }
    return super.naryOperation(paramAbstractInsnNode, paramList);
  }
  
  public void returnOperation(AbstractInsnNode paramAbstractInsnNode, BasicValue paramBasicValue1, BasicValue paramBasicValue2)
    throws AnalyzerException
  {
    if (!isSubTypeOf(paramBasicValue1, paramBasicValue2)) {
      throw new AnalyzerException(paramAbstractInsnNode, "Incompatible return type", paramBasicValue2, paramBasicValue1);
    }
  }
  
  protected boolean isArrayValue(BasicValue paramBasicValue)
  {
    return paramBasicValue.isReference();
  }
  
  protected BasicValue getElementValue(BasicValue paramBasicValue)
    throws AnalyzerException
  {
    return BasicValue.REFERENCE_VALUE;
  }
  
  protected boolean isSubTypeOf(BasicValue paramBasicValue1, BasicValue paramBasicValue2)
  {
    return paramBasicValue1.equals(paramBasicValue2);
  }
}
