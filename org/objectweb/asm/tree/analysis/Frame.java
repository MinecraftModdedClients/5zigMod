package org.objectweb.asm.tree.analysis;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class Frame
{
  private Value returnValue;
  private Value[] values;
  private int locals;
  private int top;
  
  public Frame(int paramInt1, int paramInt2)
  {
    this.values = ((Value[])new Value[paramInt1 + paramInt2]);
    this.locals = paramInt1;
  }
  
  public Frame(Frame paramFrame)
  {
    this(paramFrame.locals, paramFrame.values.length - paramFrame.locals);
    init(paramFrame);
  }
  
  public Frame init(Frame paramFrame)
  {
    this.returnValue = paramFrame.returnValue;
    System.arraycopy(paramFrame.values, 0, this.values, 0, this.values.length);
    this.top = paramFrame.top;
    return this;
  }
  
  public void setReturn(Value paramValue)
  {
    this.returnValue = paramValue;
  }
  
  public int getLocals()
  {
    return this.locals;
  }
  
  public int getMaxStackSize()
  {
    return this.values.length - this.locals;
  }
  
  public Value getLocal(int paramInt)
    throws IndexOutOfBoundsException
  {
    if (paramInt >= this.locals) {
      throw new IndexOutOfBoundsException("Trying to access an inexistant local variable");
    }
    return this.values[paramInt];
  }
  
  public void setLocal(int paramInt, Value paramValue)
    throws IndexOutOfBoundsException
  {
    if (paramInt >= this.locals) {
      throw new IndexOutOfBoundsException("Trying to access an inexistant local variable " + paramInt);
    }
    this.values[paramInt] = paramValue;
  }
  
  public int getStackSize()
  {
    return this.top;
  }
  
  public Value getStack(int paramInt)
    throws IndexOutOfBoundsException
  {
    return this.values[(paramInt + this.locals)];
  }
  
  public void clearStack()
  {
    this.top = 0;
  }
  
  public Value pop()
    throws IndexOutOfBoundsException
  {
    if (this.top == 0) {
      throw new IndexOutOfBoundsException("Cannot pop operand off an empty stack.");
    }
    return this.values[(--this.top + this.locals)];
  }
  
  public void push(Value paramValue)
    throws IndexOutOfBoundsException
  {
    if (this.top + this.locals >= this.values.length) {
      throw new IndexOutOfBoundsException("Insufficient maximum stack size.");
    }
    this.values[(this.top++ + this.locals)] = paramValue;
  }
  
  public void execute(AbstractInsnNode paramAbstractInsnNode, Interpreter paramInterpreter)
    throws AnalyzerException
  {
    Value localValue1;
    Value localValue2;
    int i;
    Object localObject;
    Value localValue3;
    ArrayList localArrayList;
    int k;
    switch (paramAbstractInsnNode.getOpcode())
    {
    case 0: 
      break;
    case 1: 
    case 2: 
    case 3: 
    case 4: 
    case 5: 
    case 6: 
    case 7: 
    case 8: 
    case 9: 
    case 10: 
    case 11: 
    case 12: 
    case 13: 
    case 14: 
    case 15: 
    case 16: 
    case 17: 
    case 18: 
      push(paramInterpreter.newOperation(paramAbstractInsnNode));
      break;
    case 21: 
    case 22: 
    case 23: 
    case 24: 
    case 25: 
      push(paramInterpreter.copyOperation(paramAbstractInsnNode, getLocal(((VarInsnNode)paramAbstractInsnNode).var)));
      break;
    case 46: 
    case 47: 
    case 48: 
    case 49: 
    case 50: 
    case 51: 
    case 52: 
    case 53: 
      localValue1 = pop();
      localValue2 = pop();
      push(paramInterpreter.binaryOperation(paramAbstractInsnNode, localValue2, localValue1));
      break;
    case 54: 
    case 55: 
    case 56: 
    case 57: 
    case 58: 
      localValue2 = paramInterpreter.copyOperation(paramAbstractInsnNode, pop());
      i = ((VarInsnNode)paramAbstractInsnNode).var;
      setLocal(i, localValue2);
      if (localValue2.getSize() == 2) {
        setLocal(i + 1, paramInterpreter.newValue(null));
      }
      if (i > 0)
      {
        localObject = getLocal(i - 1);
        if ((localObject != null) && (((Value)localObject).getSize() == 2)) {
          setLocal(i - 1, paramInterpreter.newValue(null));
        }
      }
      break;
    case 79: 
    case 80: 
    case 81: 
    case 82: 
    case 83: 
    case 84: 
    case 85: 
    case 86: 
      localValue3 = pop();
      localValue1 = pop();
      localValue2 = pop();
      paramInterpreter.ternaryOperation(paramAbstractInsnNode, localValue2, localValue1, localValue3);
      break;
    case 87: 
      if (pop().getSize() == 2) {
        throw new AnalyzerException(paramAbstractInsnNode, "Illegal use of POP");
      }
      break;
    case 88: 
      if ((pop().getSize() == 1) && (pop().getSize() != 1)) {
        throw new AnalyzerException(paramAbstractInsnNode, "Illegal use of POP2");
      }
      break;
    case 89: 
      localValue2 = pop();
      if (localValue2.getSize() != 1) {
        throw new AnalyzerException(paramAbstractInsnNode, "Illegal use of DUP");
      }
      push(localValue2);
      push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
      break;
    case 90: 
      localValue2 = pop();
      localValue1 = pop();
      if ((localValue2.getSize() != 1) || (localValue1.getSize() != 1)) {
        throw new AnalyzerException(paramAbstractInsnNode, "Illegal use of DUP_X1");
      }
      push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
      push(localValue1);
      push(localValue2);
      break;
    case 91: 
      localValue2 = pop();
      if (localValue2.getSize() == 1)
      {
        localValue1 = pop();
        if (localValue1.getSize() == 1)
        {
          localValue3 = pop();
          if (localValue3.getSize() == 1)
          {
            push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
            push(localValue3);
            push(localValue1);
            push(localValue2);
            break;
          }
        }
        else
        {
          push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
          push(localValue1);
          push(localValue2);
          break;
        }
      }
      throw new AnalyzerException(paramAbstractInsnNode, "Illegal use of DUP_X2");
    case 92: 
      localValue2 = pop();
      if (localValue2.getSize() == 1)
      {
        localValue1 = pop();
        if (localValue1.getSize() == 1)
        {
          push(localValue1);
          push(localValue2);
          push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue1));
          push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
          break;
        }
      }
      else
      {
        push(localValue2);
        push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
        break;
      }
      throw new AnalyzerException(paramAbstractInsnNode, "Illegal use of DUP2");
    case 93: 
      localValue2 = pop();
      if (localValue2.getSize() == 1)
      {
        localValue1 = pop();
        if (localValue1.getSize() == 1)
        {
          localValue3 = pop();
          if (localValue3.getSize() == 1)
          {
            push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue1));
            push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
            push(localValue3);
            push(localValue1);
            push(localValue2);
            break;
          }
        }
      }
      else
      {
        localValue1 = pop();
        if (localValue1.getSize() == 1)
        {
          push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
          push(localValue1);
          push(localValue2);
          break;
        }
      }
      throw new AnalyzerException(paramAbstractInsnNode, "Illegal use of DUP2_X1");
    case 94: 
      localValue2 = pop();
      if (localValue2.getSize() == 1)
      {
        localValue1 = pop();
        if (localValue1.getSize() == 1)
        {
          localValue3 = pop();
          if (localValue3.getSize() == 1)
          {
            Value localValue4 = pop();
            if (localValue4.getSize() == 1)
            {
              push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue1));
              push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
              push(localValue4);
              push(localValue3);
              push(localValue1);
              push(localValue2);
              break;
            }
          }
          else
          {
            push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue1));
            push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
            push(localValue3);
            push(localValue1);
            push(localValue2);
            break;
          }
        }
      }
      else
      {
        localValue1 = pop();
        if (localValue1.getSize() == 1)
        {
          localValue3 = pop();
          if (localValue3.getSize() == 1)
          {
            push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
            push(localValue3);
            push(localValue1);
            push(localValue2);
            break;
          }
        }
        else
        {
          push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
          push(localValue1);
          push(localValue2);
          break;
        }
      }
      throw new AnalyzerException(paramAbstractInsnNode, "Illegal use of DUP2_X2");
    case 95: 
      localValue1 = pop();
      localValue2 = pop();
      if ((localValue2.getSize() != 1) || (localValue1.getSize() != 1)) {
        throw new AnalyzerException(paramAbstractInsnNode, "Illegal use of SWAP");
      }
      push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue1));
      push(paramInterpreter.copyOperation(paramAbstractInsnNode, localValue2));
      break;
    case 96: 
    case 97: 
    case 98: 
    case 99: 
    case 100: 
    case 101: 
    case 102: 
    case 103: 
    case 104: 
    case 105: 
    case 106: 
    case 107: 
    case 108: 
    case 109: 
    case 110: 
    case 111: 
    case 112: 
    case 113: 
    case 114: 
    case 115: 
      localValue1 = pop();
      localValue2 = pop();
      push(paramInterpreter.binaryOperation(paramAbstractInsnNode, localValue2, localValue1));
      break;
    case 116: 
    case 117: 
    case 118: 
    case 119: 
      push(paramInterpreter.unaryOperation(paramAbstractInsnNode, pop()));
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
      localValue1 = pop();
      localValue2 = pop();
      push(paramInterpreter.binaryOperation(paramAbstractInsnNode, localValue2, localValue1));
      break;
    case 132: 
      i = ((IincInsnNode)paramAbstractInsnNode).var;
      setLocal(i, paramInterpreter.unaryOperation(paramAbstractInsnNode, getLocal(i)));
      break;
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
      push(paramInterpreter.unaryOperation(paramAbstractInsnNode, pop()));
      break;
    case 148: 
    case 149: 
    case 150: 
    case 151: 
    case 152: 
      localValue1 = pop();
      localValue2 = pop();
      push(paramInterpreter.binaryOperation(paramAbstractInsnNode, localValue2, localValue1));
      break;
    case 153: 
    case 154: 
    case 155: 
    case 156: 
    case 157: 
    case 158: 
      paramInterpreter.unaryOperation(paramAbstractInsnNode, pop());
      break;
    case 159: 
    case 160: 
    case 161: 
    case 162: 
    case 163: 
    case 164: 
    case 165: 
    case 166: 
      localValue1 = pop();
      localValue2 = pop();
      paramInterpreter.binaryOperation(paramAbstractInsnNode, localValue2, localValue1);
      break;
    case 167: 
      break;
    case 168: 
      push(paramInterpreter.newOperation(paramAbstractInsnNode));
      break;
    case 169: 
      break;
    case 170: 
    case 171: 
      paramInterpreter.unaryOperation(paramAbstractInsnNode, pop());
      break;
    case 172: 
    case 173: 
    case 174: 
    case 175: 
    case 176: 
      localValue2 = pop();
      paramInterpreter.unaryOperation(paramAbstractInsnNode, localValue2);
      paramInterpreter.returnOperation(paramAbstractInsnNode, localValue2, this.returnValue);
      break;
    case 177: 
      if (this.returnValue != null) {
        throw new AnalyzerException(paramAbstractInsnNode, "Incompatible return type");
      }
      break;
    case 178: 
      push(paramInterpreter.newOperation(paramAbstractInsnNode));
      break;
    case 179: 
      paramInterpreter.unaryOperation(paramAbstractInsnNode, pop());
      break;
    case 180: 
      push(paramInterpreter.unaryOperation(paramAbstractInsnNode, pop()));
      break;
    case 181: 
      localValue1 = pop();
      localValue2 = pop();
      paramInterpreter.binaryOperation(paramAbstractInsnNode, localValue2, localValue1);
      break;
    case 182: 
    case 183: 
    case 184: 
    case 185: 
      localArrayList = new ArrayList();
      localObject = ((MethodInsnNode)paramAbstractInsnNode).desc;
      for (k = Type.getArgumentTypes((String)localObject).length; k > 0; k--) {
        localArrayList.add(0, pop());
      }
      if (paramAbstractInsnNode.getOpcode() != 184) {
        localArrayList.add(0, pop());
      }
      if (Type.getReturnType((String)localObject) == Type.VOID_TYPE) {
        paramInterpreter.naryOperation(paramAbstractInsnNode, localArrayList);
      } else {
        push(paramInterpreter.naryOperation(paramAbstractInsnNode, localArrayList));
      }
      break;
    case 186: 
      localArrayList = new ArrayList();
      localObject = ((InvokeDynamicInsnNode)paramAbstractInsnNode).desc;
      for (k = Type.getArgumentTypes((String)localObject).length; k > 0; k--) {
        localArrayList.add(0, pop());
      }
      if (Type.getReturnType((String)localObject) == Type.VOID_TYPE) {
        paramInterpreter.naryOperation(paramAbstractInsnNode, localArrayList);
      } else {
        push(paramInterpreter.naryOperation(paramAbstractInsnNode, localArrayList));
      }
      break;
    case 187: 
      push(paramInterpreter.newOperation(paramAbstractInsnNode));
      break;
    case 188: 
    case 189: 
    case 190: 
      push(paramInterpreter.unaryOperation(paramAbstractInsnNode, pop()));
      break;
    case 191: 
      paramInterpreter.unaryOperation(paramAbstractInsnNode, pop());
      break;
    case 192: 
    case 193: 
      push(paramInterpreter.unaryOperation(paramAbstractInsnNode, pop()));
      break;
    case 194: 
    case 195: 
      paramInterpreter.unaryOperation(paramAbstractInsnNode, pop());
      break;
    case 197: 
      localArrayList = new ArrayList();
      for (int j = ((MultiANewArrayInsnNode)paramAbstractInsnNode).dims; j > 0; j--) {
        localArrayList.add(0, pop());
      }
      push(paramInterpreter.naryOperation(paramAbstractInsnNode, localArrayList));
      break;
    case 198: 
    case 199: 
      paramInterpreter.unaryOperation(paramAbstractInsnNode, pop());
      break;
    case 19: 
    case 20: 
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
    case 196: 
    default: 
      throw new RuntimeException("Illegal opcode " + paramAbstractInsnNode.getOpcode());
    }
  }
  
  public boolean merge(Frame paramFrame, Interpreter paramInterpreter)
    throws AnalyzerException
  {
    if (this.top != paramFrame.top) {
      throw new AnalyzerException(null, "Incompatible stack heights");
    }
    boolean bool = false;
    for (int i = 0; i < this.locals + this.top; i++)
    {
      Value localValue = paramInterpreter.merge(this.values[i], paramFrame.values[i]);
      if (!localValue.equals(this.values[i]))
      {
        this.values[i] = localValue;
        bool = true;
      }
    }
    return bool;
  }
  
  public boolean merge(Frame paramFrame, boolean[] paramArrayOfBoolean)
  {
    boolean bool = false;
    for (int i = 0; i < this.locals; i++) {
      if ((paramArrayOfBoolean[i] == 0) && (!this.values[i].equals(paramFrame.values[i])))
      {
        this.values[i] = paramFrame.values[i];
        bool = true;
      }
    }
    return bool;
  }
  
  public String toString()
  {
    StringBuffer localStringBuffer = new StringBuffer();
    for (int i = 0; i < getLocals(); i++) {
      localStringBuffer.append(getLocal(i));
    }
    localStringBuffer.append(' ');
    for (i = 0; i < getStackSize(); i++) {
      localStringBuffer.append(getStack(i).toString());
    }
    return localStringBuffer.toString();
  }
}
