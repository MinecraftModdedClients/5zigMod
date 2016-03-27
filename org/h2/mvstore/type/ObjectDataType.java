package org.h2.mvstore.type;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.util.New;

public class ObjectDataType
  implements DataType
{
  static final int TYPE_NULL = 0;
  static final int TYPE_BOOLEAN = 1;
  static final int TYPE_BYTE = 2;
  static final int TYPE_SHORT = 3;
  static final int TYPE_INT = 4;
  static final int TYPE_LONG = 5;
  static final int TYPE_BIG_INTEGER = 6;
  static final int TYPE_FLOAT = 7;
  static final int TYPE_DOUBLE = 8;
  static final int TYPE_BIG_DECIMAL = 9;
  static final int TYPE_CHAR = 10;
  static final int TYPE_STRING = 11;
  static final int TYPE_UUID = 12;
  static final int TYPE_DATE = 13;
  static final int TYPE_ARRAY = 14;
  static final int TYPE_SERIALIZED_OBJECT = 19;
  static final int TAG_BOOLEAN_TRUE = 32;
  static final int TAG_INTEGER_NEGATIVE = 33;
  static final int TAG_INTEGER_FIXED = 34;
  static final int TAG_LONG_NEGATIVE = 35;
  static final int TAG_LONG_FIXED = 36;
  static final int TAG_BIG_INTEGER_0 = 37;
  static final int TAG_BIG_INTEGER_1 = 38;
  static final int TAG_BIG_INTEGER_SMALL = 39;
  static final int TAG_FLOAT_0 = 40;
  static final int TAG_FLOAT_1 = 41;
  static final int TAG_FLOAT_FIXED = 42;
  static final int TAG_DOUBLE_0 = 43;
  static final int TAG_DOUBLE_1 = 44;
  static final int TAG_DOUBLE_FIXED = 45;
  static final int TAG_BIG_DECIMAL_0 = 46;
  static final int TAG_BIG_DECIMAL_1 = 47;
  static final int TAG_BIG_DECIMAL_SMALL = 48;
  static final int TAG_BIG_DECIMAL_SMALL_SCALED = 49;
  static final int TAG_INTEGER_0_15 = 64;
  static final int TAG_LONG_0_7 = 80;
  static final int TAG_STRING_0_15 = 88;
  static final int TAG_BYTE_ARRAY_0_15 = 104;
  static final int FLOAT_ZERO_BITS = Float.floatToIntBits(0.0F);
  static final int FLOAT_ONE_BITS = Float.floatToIntBits(1.0F);
  static final long DOUBLE_ZERO_BITS = Double.doubleToLongBits(0.0D);
  static final long DOUBLE_ONE_BITS = Double.doubleToLongBits(1.0D);
  static final Class<?>[] COMMON_CLASSES = { Boolean.TYPE, Byte.TYPE, Short.TYPE, Character.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Object.class, Boolean.class, Byte.class, Short.class, Character.class, Integer.class, Long.class, BigInteger.class, Float.class, Double.class, BigDecimal.class, String.class, UUID.class, Date.class };
  private static final HashMap<Class<?>, Integer> COMMON_CLASSES_MAP = New.hashMap();
  private AutoDetectDataType last;
  
  public ObjectDataType()
  {
    this.last = new StringType(this);
  }
  
  public int compare(Object paramObject1, Object paramObject2)
  {
    return this.last.compare(paramObject1, paramObject2);
  }
  
  public int getMemory(Object paramObject)
  {
    return this.last.getMemory(paramObject);
  }
  
  public void read(ByteBuffer paramByteBuffer, Object[] paramArrayOfObject, int paramInt, boolean paramBoolean)
  {
    for (int i = 0; i < paramInt; i++) {
      paramArrayOfObject[i] = read(paramByteBuffer);
    }
  }
  
  public void write(WriteBuffer paramWriteBuffer, Object[] paramArrayOfObject, int paramInt, boolean paramBoolean)
  {
    for (int i = 0; i < paramInt; i++) {
      write(paramWriteBuffer, paramArrayOfObject[i]);
    }
  }
  
  public void write(WriteBuffer paramWriteBuffer, Object paramObject)
  {
    this.last.write(paramWriteBuffer, paramObject);
  }
  
  private AutoDetectDataType newType(int paramInt)
  {
    switch (paramInt)
    {
    case 0: 
      return new NullType(this);
    case 1: 
      return new BooleanType(this);
    case 2: 
      return new ByteType(this);
    case 3: 
      return new ShortType(this);
    case 10: 
      return new CharacterType(this);
    case 4: 
      return new IntegerType(this);
    case 5: 
      return new LongType(this);
    case 7: 
      return new FloatType(this);
    case 8: 
      return new DoubleType(this);
    case 6: 
      return new BigIntegerType(this);
    case 9: 
      return new BigDecimalType(this);
    case 11: 
      return new StringType(this);
    case 12: 
      return new UUIDType(this);
    case 13: 
      return new DateType(this);
    case 14: 
      return new ObjectArrayType(this);
    case 19: 
      return new SerializedObjectType(this);
    }
    throw DataUtils.newIllegalStateException(3, "Unsupported type {0}", new Object[] { Integer.valueOf(paramInt) });
  }
  
  public Object read(ByteBuffer paramByteBuffer)
  {
    int i = paramByteBuffer.get();
    int j;
    if (i <= 19) {
      j = i;
    } else {
      switch (i)
      {
      case 32: 
        j = 1;
        break;
      case 33: 
      case 34: 
        j = 4;
        break;
      case 35: 
      case 36: 
        j = 5;
        break;
      case 37: 
      case 38: 
      case 39: 
        j = 6;
        break;
      case 40: 
      case 41: 
      case 42: 
        j = 7;
        break;
      case 43: 
      case 44: 
      case 45: 
        j = 8;
        break;
      case 46: 
      case 47: 
      case 48: 
      case 49: 
        j = 9;
        break;
      default: 
        if ((i >= 64) && (i <= 79)) {
          j = 4;
        } else if ((i >= 88) && (i <= 103)) {
          j = 11;
        } else if ((i >= 80) && (i <= 87)) {
          j = 5;
        } else if ((i >= 104) && (i <= 119)) {
          j = 14;
        } else {
          throw DataUtils.newIllegalStateException(6, "Unknown tag {0}", new Object[] { Integer.valueOf(i) });
        }
        break;
      }
    }
    AutoDetectDataType localAutoDetectDataType = this.last;
    if (j != localAutoDetectDataType.typeId) {
      this.last = (localAutoDetectDataType = newType(j));
    }
    return localAutoDetectDataType.read(paramByteBuffer, i);
  }
  
  private static int getTypeId(Object paramObject)
  {
    if ((paramObject instanceof Integer)) {
      return 4;
    }
    if ((paramObject instanceof String)) {
      return 11;
    }
    if ((paramObject instanceof Long)) {
      return 5;
    }
    if ((paramObject instanceof Double)) {
      return 8;
    }
    if ((paramObject instanceof Float)) {
      return 7;
    }
    if ((paramObject instanceof Boolean)) {
      return 1;
    }
    if ((paramObject instanceof UUID)) {
      return 12;
    }
    if ((paramObject instanceof Byte)) {
      return 2;
    }
    if ((paramObject instanceof Short)) {
      return 3;
    }
    if ((paramObject instanceof Character)) {
      return 10;
    }
    if (paramObject == null) {
      return 0;
    }
    if (isDate(paramObject)) {
      return 13;
    }
    if (isBigInteger(paramObject)) {
      return 6;
    }
    if (isBigDecimal(paramObject)) {
      return 9;
    }
    if (paramObject.getClass().isArray()) {
      return 14;
    }
    return 19;
  }
  
  AutoDetectDataType switchType(Object paramObject)
  {
    int i = getTypeId(paramObject);
    AutoDetectDataType localAutoDetectDataType = this.last;
    if (i != localAutoDetectDataType.typeId) {
      this.last = (localAutoDetectDataType = newType(i));
    }
    return localAutoDetectDataType;
  }
  
  static boolean isBigInteger(Object paramObject)
  {
    return ((paramObject instanceof BigInteger)) && (paramObject.getClass() == BigInteger.class);
  }
  
  static boolean isBigDecimal(Object paramObject)
  {
    return ((paramObject instanceof BigDecimal)) && (paramObject.getClass() == BigDecimal.class);
  }
  
  static boolean isDate(Object paramObject)
  {
    return ((paramObject instanceof Date)) && (paramObject.getClass() == Date.class);
  }
  
  static boolean isArray(Object paramObject)
  {
    return (paramObject != null) && (paramObject.getClass().isArray());
  }
  
  static Integer getCommonClassId(Class<?> paramClass)
  {
    HashMap localHashMap = COMMON_CLASSES_MAP;
    if (localHashMap.size() == 0)
    {
      int i = 0;
      for (int j = COMMON_CLASSES.length; i < j; i++) {
        COMMON_CLASSES_MAP.put(COMMON_CLASSES[i], Integer.valueOf(i));
      }
    }
    return (Integer)localHashMap.get(paramClass);
  }
  
  public static byte[] serialize(Object paramObject)
  {
    try
    {
      ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
      ObjectOutputStream localObjectOutputStream = new ObjectOutputStream(localByteArrayOutputStream);
      localObjectOutputStream.writeObject(paramObject);
      return localByteArrayOutputStream.toByteArray();
    }
    catch (Throwable localThrowable)
    {
      throw DataUtils.newIllegalArgumentException("Could not serialize {0}", new Object[] { paramObject, localThrowable });
    }
  }
  
  public static Object deserialize(byte[] paramArrayOfByte)
  {
    try
    {
      ByteArrayInputStream localByteArrayInputStream = new ByteArrayInputStream(paramArrayOfByte);
      ObjectInputStream localObjectInputStream = new ObjectInputStream(localByteArrayInputStream);
      return localObjectInputStream.readObject();
    }
    catch (Throwable localThrowable)
    {
      throw DataUtils.newIllegalArgumentException("Could not deserialize {0}", new Object[] { Arrays.toString(paramArrayOfByte), localThrowable });
    }
  }
  
  public static int compareNotNull(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
  {
    if (paramArrayOfByte1 == paramArrayOfByte2) {
      return 0;
    }
    int i = Math.min(paramArrayOfByte1.length, paramArrayOfByte2.length);
    for (int j = 0; j < i; j++)
    {
      int k = paramArrayOfByte1[j] & 0xFF;
      int m = paramArrayOfByte2[j] & 0xFF;
      if (k != m) {
        return k > m ? 1 : -1;
      }
    }
    return Integer.signum(paramArrayOfByte1.length - paramArrayOfByte2.length);
  }
  
  static abstract class AutoDetectDataType
    implements DataType
  {
    protected final ObjectDataType base;
    protected final int typeId;
    
    AutoDetectDataType(ObjectDataType paramObjectDataType, int paramInt)
    {
      this.base = paramObjectDataType;
      this.typeId = paramInt;
    }
    
    public int getMemory(Object paramObject)
    {
      return getType(paramObject).getMemory(paramObject);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      AutoDetectDataType localAutoDetectDataType1 = getType(paramObject1);
      AutoDetectDataType localAutoDetectDataType2 = getType(paramObject2);
      if (localAutoDetectDataType1 == localAutoDetectDataType2) {
        return localAutoDetectDataType1.compare(paramObject1, paramObject2);
      }
      int i = localAutoDetectDataType1.typeId - localAutoDetectDataType2.typeId;
      return Integer.signum(i);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object[] paramArrayOfObject, int paramInt, boolean paramBoolean)
    {
      for (int i = 0; i < paramInt; i++) {
        write(paramWriteBuffer, paramArrayOfObject[i]);
      }
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      getType(paramObject).write(paramWriteBuffer, paramObject);
    }
    
    public void read(ByteBuffer paramByteBuffer, Object[] paramArrayOfObject, int paramInt, boolean paramBoolean)
    {
      for (int i = 0; i < paramInt; i++) {
        paramArrayOfObject[i] = read(paramByteBuffer);
      }
    }
    
    public final Object read(ByteBuffer paramByteBuffer)
    {
      throw DataUtils.newIllegalStateException(3, "Internal error", new Object[0]);
    }
    
    AutoDetectDataType getType(Object paramObject)
    {
      return this.base.switchType(paramObject);
    }
    
    abstract Object read(ByteBuffer paramByteBuffer, int paramInt);
  }
  
  static class NullType
    extends ObjectDataType.AutoDetectDataType
  {
    NullType(ObjectDataType paramObjectDataType)
    {
      super(0);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if ((paramObject1 == null) && (paramObject2 == null)) {
        return 0;
      }
      if (paramObject1 == null) {
        return -1;
      }
      if (paramObject2 == null) {
        return 1;
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public int getMemory(Object paramObject)
    {
      return paramObject == null ? 0 : super.getMemory(paramObject);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (paramObject != null)
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      paramWriteBuffer.put((byte)0);
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      return null;
    }
  }
  
  static class BooleanType
    extends ObjectDataType.AutoDetectDataType
  {
    BooleanType(ObjectDataType paramObjectDataType)
    {
      super(1);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (((paramObject1 instanceof Boolean)) && ((paramObject2 instanceof Boolean)))
      {
        Boolean localBoolean1 = (Boolean)paramObject1;
        Boolean localBoolean2 = (Boolean)paramObject2;
        return localBoolean1.compareTo(localBoolean2);
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public int getMemory(Object paramObject)
    {
      return (paramObject instanceof Boolean) ? 0 : super.getMemory(paramObject);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!(paramObject instanceof Boolean))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      int i = ((Boolean)paramObject).booleanValue() ? 32 : 1;
      paramWriteBuffer.put((byte)i);
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      return paramInt == 1 ? Boolean.FALSE : Boolean.TRUE;
    }
  }
  
  static class ByteType
    extends ObjectDataType.AutoDetectDataType
  {
    ByteType(ObjectDataType paramObjectDataType)
    {
      super(2);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (((paramObject1 instanceof Byte)) && ((paramObject2 instanceof Byte)))
      {
        Byte localByte1 = (Byte)paramObject1;
        Byte localByte2 = (Byte)paramObject2;
        return localByte1.compareTo(localByte2);
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public int getMemory(Object paramObject)
    {
      return (paramObject instanceof Byte) ? 0 : super.getMemory(paramObject);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!(paramObject instanceof Byte))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      paramWriteBuffer.put((byte)2);
      paramWriteBuffer.put(((Byte)paramObject).byteValue());
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      return Byte.valueOf(paramByteBuffer.get());
    }
  }
  
  static class CharacterType
    extends ObjectDataType.AutoDetectDataType
  {
    CharacterType(ObjectDataType paramObjectDataType)
    {
      super(10);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (((paramObject1 instanceof Character)) && ((paramObject2 instanceof Character)))
      {
        Character localCharacter1 = (Character)paramObject1;
        Character localCharacter2 = (Character)paramObject2;
        return localCharacter1.compareTo(localCharacter2);
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public int getMemory(Object paramObject)
    {
      return (paramObject instanceof Character) ? 24 : super.getMemory(paramObject);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!(paramObject instanceof Character))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      paramWriteBuffer.put((byte)10);
      paramWriteBuffer.putChar(((Character)paramObject).charValue());
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      return Character.valueOf(paramByteBuffer.getChar());
    }
  }
  
  static class ShortType
    extends ObjectDataType.AutoDetectDataType
  {
    ShortType(ObjectDataType paramObjectDataType)
    {
      super(3);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (((paramObject1 instanceof Short)) && ((paramObject2 instanceof Short)))
      {
        Short localShort1 = (Short)paramObject1;
        Short localShort2 = (Short)paramObject2;
        return localShort1.compareTo(localShort2);
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public int getMemory(Object paramObject)
    {
      return (paramObject instanceof Short) ? 24 : super.getMemory(paramObject);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!(paramObject instanceof Short))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      paramWriteBuffer.put((byte)3);
      paramWriteBuffer.putShort(((Short)paramObject).shortValue());
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      return Short.valueOf(paramByteBuffer.getShort());
    }
  }
  
  static class IntegerType
    extends ObjectDataType.AutoDetectDataType
  {
    IntegerType(ObjectDataType paramObjectDataType)
    {
      super(4);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (((paramObject1 instanceof Integer)) && ((paramObject2 instanceof Integer)))
      {
        Integer localInteger1 = (Integer)paramObject1;
        Integer localInteger2 = (Integer)paramObject2;
        return localInteger1.compareTo(localInteger2);
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public int getMemory(Object paramObject)
    {
      return (paramObject instanceof Integer) ? 24 : super.getMemory(paramObject);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!(paramObject instanceof Integer))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      int i = ((Integer)paramObject).intValue();
      if (i < 0)
      {
        if ((-i < 0) || (-i > 2097151)) {
          paramWriteBuffer.put((byte)34).putInt(i);
        } else {
          paramWriteBuffer.put((byte)33).putVarInt(-i);
        }
      }
      else if (i <= 15) {
        paramWriteBuffer.put((byte)(64 + i));
      } else if (i <= 2097151) {
        paramWriteBuffer.put((byte)4).putVarInt(i);
      } else {
        paramWriteBuffer.put((byte)34).putInt(i);
      }
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      switch (paramInt)
      {
      case 4: 
        return Integer.valueOf(DataUtils.readVarInt(paramByteBuffer));
      case 33: 
        return Integer.valueOf(-DataUtils.readVarInt(paramByteBuffer));
      case 34: 
        return Integer.valueOf(paramByteBuffer.getInt());
      }
      return Integer.valueOf(paramInt - 64);
    }
  }
  
  static class LongType
    extends ObjectDataType.AutoDetectDataType
  {
    LongType(ObjectDataType paramObjectDataType)
    {
      super(5);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (((paramObject1 instanceof Long)) && ((paramObject2 instanceof Long)))
      {
        Long localLong1 = (Long)paramObject1;
        Long localLong2 = (Long)paramObject2;
        return localLong1.compareTo(localLong2);
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public int getMemory(Object paramObject)
    {
      return (paramObject instanceof Long) ? 30 : super.getMemory(paramObject);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!(paramObject instanceof Long))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      long l = ((Long)paramObject).longValue();
      if (l < 0L)
      {
        if ((-l < 0L) || (-l > 562949953421311L))
        {
          paramWriteBuffer.put((byte)36);
          paramWriteBuffer.putLong(l);
        }
        else
        {
          paramWriteBuffer.put((byte)35);
          paramWriteBuffer.putVarLong(-l);
        }
      }
      else if (l <= 7L)
      {
        paramWriteBuffer.put((byte)(int)(80L + l));
      }
      else if (l <= 562949953421311L)
      {
        paramWriteBuffer.put((byte)5);
        paramWriteBuffer.putVarLong(l);
      }
      else
      {
        paramWriteBuffer.put((byte)36);
        paramWriteBuffer.putLong(l);
      }
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      switch (paramInt)
      {
      case 5: 
        return Long.valueOf(DataUtils.readVarLong(paramByteBuffer));
      case 35: 
        return Long.valueOf(-DataUtils.readVarLong(paramByteBuffer));
      case 36: 
        return Long.valueOf(paramByteBuffer.getLong());
      }
      return Long.valueOf(paramInt - 80);
    }
  }
  
  static class FloatType
    extends ObjectDataType.AutoDetectDataType
  {
    FloatType(ObjectDataType paramObjectDataType)
    {
      super(7);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (((paramObject1 instanceof Float)) && ((paramObject2 instanceof Float)))
      {
        Float localFloat1 = (Float)paramObject1;
        Float localFloat2 = (Float)paramObject2;
        return localFloat1.compareTo(localFloat2);
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public int getMemory(Object paramObject)
    {
      return (paramObject instanceof Float) ? 24 : super.getMemory(paramObject);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!(paramObject instanceof Float))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      float f = ((Float)paramObject).floatValue();
      int i = Float.floatToIntBits(f);
      if (i == ObjectDataType.FLOAT_ZERO_BITS)
      {
        paramWriteBuffer.put((byte)40);
      }
      else if (i == ObjectDataType.FLOAT_ONE_BITS)
      {
        paramWriteBuffer.put((byte)41);
      }
      else
      {
        int j = Integer.reverse(i);
        if ((j >= 0) && (j <= 2097151)) {
          paramWriteBuffer.put((byte)7).putVarInt(j);
        } else {
          paramWriteBuffer.put((byte)42).putFloat(f);
        }
      }
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      switch (paramInt)
      {
      case 40: 
        return Float.valueOf(0.0F);
      case 41: 
        return Float.valueOf(1.0F);
      case 42: 
        return Float.valueOf(paramByteBuffer.getFloat());
      }
      return Float.valueOf(Float.intBitsToFloat(Integer.reverse(DataUtils.readVarInt(paramByteBuffer))));
    }
  }
  
  static class DoubleType
    extends ObjectDataType.AutoDetectDataType
  {
    DoubleType(ObjectDataType paramObjectDataType)
    {
      super(8);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (((paramObject1 instanceof Double)) && ((paramObject2 instanceof Double)))
      {
        Double localDouble1 = (Double)paramObject1;
        Double localDouble2 = (Double)paramObject2;
        return localDouble1.compareTo(localDouble2);
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public int getMemory(Object paramObject)
    {
      return (paramObject instanceof Double) ? 30 : super.getMemory(paramObject);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!(paramObject instanceof Double))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      double d = ((Double)paramObject).doubleValue();
      long l1 = Double.doubleToLongBits(d);
      if (l1 == ObjectDataType.DOUBLE_ZERO_BITS)
      {
        paramWriteBuffer.put((byte)43);
      }
      else if (l1 == ObjectDataType.DOUBLE_ONE_BITS)
      {
        paramWriteBuffer.put((byte)44);
      }
      else
      {
        long l2 = Long.reverse(l1);
        if ((l2 >= 0L) && (l2 <= 562949953421311L))
        {
          paramWriteBuffer.put((byte)8);
          paramWriteBuffer.putVarLong(l2);
        }
        else
        {
          paramWriteBuffer.put((byte)45);
          paramWriteBuffer.putDouble(d);
        }
      }
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      switch (paramInt)
      {
      case 43: 
        return Double.valueOf(0.0D);
      case 44: 
        return Double.valueOf(1.0D);
      case 45: 
        return Double.valueOf(paramByteBuffer.getDouble());
      }
      return Double.valueOf(Double.longBitsToDouble(Long.reverse(DataUtils.readVarLong(paramByteBuffer))));
    }
  }
  
  static class BigIntegerType
    extends ObjectDataType.AutoDetectDataType
  {
    BigIntegerType(ObjectDataType paramObjectDataType)
    {
      super(6);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if ((ObjectDataType.isBigInteger(paramObject1)) && (ObjectDataType.isBigInteger(paramObject2)))
      {
        BigInteger localBigInteger1 = (BigInteger)paramObject1;
        BigInteger localBigInteger2 = (BigInteger)paramObject2;
        return localBigInteger1.compareTo(localBigInteger2);
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public int getMemory(Object paramObject)
    {
      return ObjectDataType.isBigInteger(paramObject) ? 100 : super.getMemory(paramObject);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!ObjectDataType.isBigInteger(paramObject))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      BigInteger localBigInteger = (BigInteger)paramObject;
      if (BigInteger.ZERO.equals(localBigInteger))
      {
        paramWriteBuffer.put((byte)37);
      }
      else if (BigInteger.ONE.equals(localBigInteger))
      {
        paramWriteBuffer.put((byte)38);
      }
      else
      {
        int i = localBigInteger.bitLength();
        if (i <= 63)
        {
          paramWriteBuffer.put((byte)39).putVarLong(localBigInteger.longValue());
        }
        else
        {
          byte[] arrayOfByte = localBigInteger.toByteArray();
          paramWriteBuffer.put((byte)6).putVarInt(arrayOfByte.length).put(arrayOfByte);
        }
      }
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      switch (paramInt)
      {
      case 37: 
        return BigInteger.ZERO;
      case 38: 
        return BigInteger.ONE;
      case 39: 
        return BigInteger.valueOf(DataUtils.readVarLong(paramByteBuffer));
      }
      int i = DataUtils.readVarInt(paramByteBuffer);
      byte[] arrayOfByte = DataUtils.newBytes(i);
      paramByteBuffer.get(arrayOfByte);
      return new BigInteger(arrayOfByte);
    }
  }
  
  static class BigDecimalType
    extends ObjectDataType.AutoDetectDataType
  {
    BigDecimalType(ObjectDataType paramObjectDataType)
    {
      super(9);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if ((ObjectDataType.isBigDecimal(paramObject1)) && (ObjectDataType.isBigDecimal(paramObject2)))
      {
        BigDecimal localBigDecimal1 = (BigDecimal)paramObject1;
        BigDecimal localBigDecimal2 = (BigDecimal)paramObject2;
        return localBigDecimal1.compareTo(localBigDecimal2);
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public int getMemory(Object paramObject)
    {
      return ObjectDataType.isBigDecimal(paramObject) ? 150 : super.getMemory(paramObject);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!ObjectDataType.isBigDecimal(paramObject))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      BigDecimal localBigDecimal = (BigDecimal)paramObject;
      if (BigDecimal.ZERO.equals(localBigDecimal))
      {
        paramWriteBuffer.put((byte)46);
      }
      else if (BigDecimal.ONE.equals(localBigDecimal))
      {
        paramWriteBuffer.put((byte)47);
      }
      else
      {
        int i = localBigDecimal.scale();
        BigInteger localBigInteger = localBigDecimal.unscaledValue();
        int j = localBigInteger.bitLength();
        if (j < 64)
        {
          if (i == 0) {
            paramWriteBuffer.put((byte)48);
          } else {
            paramWriteBuffer.put((byte)49).putVarInt(i);
          }
          paramWriteBuffer.putVarLong(localBigInteger.longValue());
        }
        else
        {
          byte[] arrayOfByte = localBigInteger.toByteArray();
          paramWriteBuffer.put((byte)9).putVarInt(i).putVarInt(arrayOfByte.length).put(arrayOfByte);
        }
      }
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      switch (paramInt)
      {
      case 46: 
        return BigDecimal.ZERO;
      case 47: 
        return BigDecimal.ONE;
      case 48: 
        return BigDecimal.valueOf(DataUtils.readVarLong(paramByteBuffer));
      case 49: 
        i = DataUtils.readVarInt(paramByteBuffer);
        return BigDecimal.valueOf(DataUtils.readVarLong(paramByteBuffer), i);
      }
      int i = DataUtils.readVarInt(paramByteBuffer);
      int j = DataUtils.readVarInt(paramByteBuffer);
      byte[] arrayOfByte = DataUtils.newBytes(j);
      paramByteBuffer.get(arrayOfByte);
      BigInteger localBigInteger = new BigInteger(arrayOfByte);
      return new BigDecimal(localBigInteger, i);
    }
  }
  
  static class StringType
    extends ObjectDataType.AutoDetectDataType
  {
    StringType(ObjectDataType paramObjectDataType)
    {
      super(11);
    }
    
    public int getMemory(Object paramObject)
    {
      if (!(paramObject instanceof String)) {
        return super.getMemory(paramObject);
      }
      return 24 + 2 * paramObject.toString().length();
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (((paramObject1 instanceof String)) && ((paramObject2 instanceof String))) {
        return paramObject1.toString().compareTo(paramObject2.toString());
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!(paramObject instanceof String))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      String str = (String)paramObject;
      int i = str.length();
      if (i <= 15) {
        paramWriteBuffer.put((byte)(88 + i));
      } else {
        paramWriteBuffer.put((byte)11).putVarInt(i);
      }
      paramWriteBuffer.putStringData(str, i);
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      int i;
      if (paramInt == 11) {
        i = DataUtils.readVarInt(paramByteBuffer);
      } else {
        i = paramInt - 88;
      }
      return DataUtils.readString(paramByteBuffer, i);
    }
  }
  
  static class UUIDType
    extends ObjectDataType.AutoDetectDataType
  {
    UUIDType(ObjectDataType paramObjectDataType)
    {
      super(12);
    }
    
    public int getMemory(Object paramObject)
    {
      return (paramObject instanceof UUID) ? 40 : super.getMemory(paramObject);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (((paramObject1 instanceof UUID)) && ((paramObject2 instanceof UUID)))
      {
        UUID localUUID1 = (UUID)paramObject1;
        UUID localUUID2 = (UUID)paramObject2;
        return localUUID1.compareTo(localUUID2);
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!(paramObject instanceof UUID))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      paramWriteBuffer.put((byte)12);
      UUID localUUID = (UUID)paramObject;
      paramWriteBuffer.putLong(localUUID.getMostSignificantBits());
      paramWriteBuffer.putLong(localUUID.getLeastSignificantBits());
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      long l1 = paramByteBuffer.getLong();long l2 = paramByteBuffer.getLong();
      return new UUID(l1, l2);
    }
  }
  
  static class DateType
    extends ObjectDataType.AutoDetectDataType
  {
    DateType(ObjectDataType paramObjectDataType)
    {
      super(13);
    }
    
    public int getMemory(Object paramObject)
    {
      return ObjectDataType.isDate(paramObject) ? 40 : super.getMemory(paramObject);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if ((ObjectDataType.isDate(paramObject1)) && (ObjectDataType.isDate(paramObject2)))
      {
        Date localDate1 = (Date)paramObject1;
        Date localDate2 = (Date)paramObject2;
        return localDate1.compareTo(localDate2);
      }
      return super.compare(paramObject1, paramObject2);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!ObjectDataType.isDate(paramObject))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      paramWriteBuffer.put((byte)13);
      Date localDate = (Date)paramObject;
      paramWriteBuffer.putLong(localDate.getTime());
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      long l = paramByteBuffer.getLong();
      return new Date(l);
    }
  }
  
  static class ObjectArrayType
    extends ObjectDataType.AutoDetectDataType
  {
    private final ObjectDataType elementType = new ObjectDataType();
    
    ObjectArrayType(ObjectDataType paramObjectDataType)
    {
      super(14);
    }
    
    public int getMemory(Object paramObject)
    {
      if (!ObjectDataType.isArray(paramObject)) {
        return super.getMemory(paramObject);
      }
      int i = 64;
      Class localClass = paramObject.getClass().getComponentType();
      if (localClass.isPrimitive())
      {
        int j = Array.getLength(paramObject);
        if (localClass == Boolean.TYPE) {
          i += j;
        } else if (localClass == Byte.TYPE) {
          i += j;
        } else if (localClass == Character.TYPE) {
          i += j * 2;
        } else if (localClass == Short.TYPE) {
          i += j * 2;
        } else if (localClass == Integer.TYPE) {
          i += j * 4;
        } else if (localClass == Float.TYPE) {
          i += j * 4;
        } else if (localClass == Double.TYPE) {
          i += j * 8;
        } else if (localClass == Long.TYPE) {
          i += j * 8;
        }
      }
      else
      {
        for (Object localObject : (Object[])paramObject) {
          if (localObject != null) {
            i += this.elementType.getMemory(localObject);
          }
        }
      }
      return i * 2;
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if ((!ObjectDataType.isArray(paramObject1)) || (!ObjectDataType.isArray(paramObject2))) {
        return super.compare(paramObject1, paramObject2);
      }
      if (paramObject1 == paramObject2) {
        return 0;
      }
      Class localClass1 = paramObject1.getClass().getComponentType();
      Class localClass2 = paramObject2.getClass().getComponentType();
      if (localClass1 != localClass2)
      {
        Integer localInteger1 = ObjectDataType.getCommonClassId(localClass1);
        Integer localInteger2 = ObjectDataType.getCommonClassId(localClass2);
        if (localInteger1 != null)
        {
          if (localInteger2 != null) {
            return localInteger1.compareTo(localInteger2);
          }
          return -1;
        }
        if (localInteger2 != null) {
          return 1;
        }
        return localClass1.getName().compareTo(localClass2.getName());
      }
      int i = Array.getLength(paramObject1);
      int j = Array.getLength(paramObject2);
      int k = Math.min(i, j);
      int i3;
      if (localClass1.isPrimitive())
      {
        if (localClass1 == Byte.TYPE)
        {
          byte[] arrayOfByte1 = (byte[])paramObject1;
          byte[] arrayOfByte2 = (byte[])paramObject2;
          return ObjectDataType.compareNotNull(arrayOfByte1, arrayOfByte2);
        }
        for (int m = 0; m < k; m++)
        {
          int n;
          if (localClass1 == Boolean.TYPE)
          {
            n = Integer.signum((((boolean[])(boolean[])paramObject1)[m] != 0 ? 1 : 0) - (((boolean[])(boolean[])paramObject2)[m] != 0 ? 1 : 0));
          }
          else if (localClass1 == Character.TYPE)
          {
            n = Integer.signum(((char[])(char[])paramObject1)[m] - ((char[])(char[])paramObject2)[m]);
          }
          else if (localClass1 == Short.TYPE)
          {
            n = Integer.signum(((short[])(short[])paramObject1)[m] - ((short[])(short[])paramObject2)[m]);
          }
          else if (localClass1 == Integer.TYPE)
          {
            int i1 = ((int[])(int[])paramObject1)[m];
            i3 = ((int[])(int[])paramObject2)[m];
            n = i1 < i3 ? -1 : i1 == i3 ? 0 : 1;
          }
          else if (localClass1 == Float.TYPE)
          {
            n = Float.compare(((float[])(float[])paramObject1)[m], ((float[])(float[])paramObject2)[m]);
          }
          else if (localClass1 == Double.TYPE)
          {
            n = Double.compare(((double[])(double[])paramObject1)[m], ((double[])(double[])paramObject2)[m]);
          }
          else
          {
            long l1 = ((long[])(long[])paramObject1)[m];
            long l2 = ((long[])(long[])paramObject2)[m];
            n = l1 < l2 ? -1 : l1 == l2 ? 0 : 1;
          }
          if (n != 0) {
            return n;
          }
        }
      }
      else
      {
        Object[] arrayOfObject1 = (Object[])paramObject1;
        Object[] arrayOfObject2 = (Object[])paramObject2;
        for (int i2 = 0; i2 < k; i2++)
        {
          i3 = this.elementType.compare(arrayOfObject1[i2], arrayOfObject2[i2]);
          if (i3 != 0) {
            return i3;
          }
        }
      }
      return i < j ? -1 : i == j ? 0 : 1;
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (!ObjectDataType.isArray(paramObject))
      {
        super.write(paramWriteBuffer, paramObject);
        return;
      }
      Class localClass = paramObject.getClass().getComponentType();
      Integer localInteger = ObjectDataType.getCommonClassId(localClass);
      if (localInteger != null)
      {
        if (localClass.isPrimitive())
        {
          if (localClass == Byte.TYPE)
          {
            byte[] arrayOfByte = (byte[])paramObject;
            j = arrayOfByte.length;
            if (j <= 15) {
              paramWriteBuffer.put((byte)(104 + j));
            } else {
              paramWriteBuffer.put((byte)14).put((byte)localInteger.intValue()).putVarInt(j);
            }
            paramWriteBuffer.put(arrayOfByte);
            return;
          }
          int i = Array.getLength(paramObject);
          paramWriteBuffer.put((byte)14).put((byte)localInteger.intValue()).putVarInt(i);
          for (j = 0; j < i; j++) {
            if (localClass == Boolean.TYPE) {
              paramWriteBuffer.put((byte)(((boolean[])(boolean[])paramObject)[j] != 0 ? 1 : 0));
            } else if (localClass == Character.TYPE) {
              paramWriteBuffer.putChar(((char[])(char[])paramObject)[j]);
            } else if (localClass == Short.TYPE) {
              paramWriteBuffer.putShort(((short[])(short[])paramObject)[j]);
            } else if (localClass == Integer.TYPE) {
              paramWriteBuffer.putInt(((int[])(int[])paramObject)[j]);
            } else if (localClass == Float.TYPE) {
              paramWriteBuffer.putFloat(((float[])(float[])paramObject)[j]);
            } else if (localClass == Double.TYPE) {
              paramWriteBuffer.putDouble(((double[])(double[])paramObject)[j]);
            } else {
              paramWriteBuffer.putLong(((long[])(long[])paramObject)[j]);
            }
          }
          return;
        }
        paramWriteBuffer.put((byte)14).put((byte)localInteger.intValue());
      }
      else
      {
        paramWriteBuffer.put((byte)14).put((byte)-1);
        localObject1 = localClass.getName();
        StringDataType.INSTANCE.write(paramWriteBuffer, localObject1);
      }
      Object localObject1 = (Object[])paramObject;
      int j = localObject1.length;
      paramWriteBuffer.putVarInt(j);
      for (Object localObject3 : localObject1) {
        this.elementType.write(paramWriteBuffer, localObject3);
      }
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      if (paramInt != 14)
      {
        int j = paramInt - 104;
        byte[] arrayOfByte = DataUtils.newBytes(j);
        paramByteBuffer.get(arrayOfByte);
        return arrayOfByte;
      }
      int i = paramByteBuffer.get();
      Class localClass;
      if (i == -1)
      {
        String str = StringDataType.INSTANCE.read(paramByteBuffer);
        try
        {
          localClass = Class.forName(str);
        }
        catch (Exception localException1)
        {
          throw DataUtils.newIllegalStateException(8, "Could not get class {0}", new Object[] { str, localException1 });
        }
      }
      else
      {
        localClass = ObjectDataType.COMMON_CLASSES[i];
      }
      int k = DataUtils.readVarInt(paramByteBuffer);
      Object localObject;
      try
      {
        localObject = Array.newInstance(localClass, k);
      }
      catch (Exception localException2)
      {
        throw DataUtils.newIllegalStateException(8, "Could not create array of type {0} length {1}", new Object[] { localClass, Integer.valueOf(k), localException2 });
      }
      if (localClass.isPrimitive())
      {
        for (int m = 0; m < k; m++) {
          if (localClass == Boolean.TYPE) {
            ((boolean[])localObject)[m] = (paramByteBuffer.get() == 1 ? 1 : 0);
          } else if (localClass == Byte.TYPE) {
            ((byte[])localObject)[m] = paramByteBuffer.get();
          } else if (localClass == Character.TYPE) {
            ((char[])localObject)[m] = paramByteBuffer.getChar();
          } else if (localClass == Short.TYPE) {
            ((short[])localObject)[m] = paramByteBuffer.getShort();
          } else if (localClass == Integer.TYPE) {
            ((int[])localObject)[m] = paramByteBuffer.getInt();
          } else if (localClass == Float.TYPE) {
            ((float[])localObject)[m] = paramByteBuffer.getFloat();
          } else if (localClass == Double.TYPE) {
            ((double[])localObject)[m] = paramByteBuffer.getDouble();
          } else {
            ((long[])localObject)[m] = paramByteBuffer.getLong();
          }
        }
      }
      else
      {
        Object[] arrayOfObject = (Object[])localObject;
        for (int n = 0; n < k; n++) {
          arrayOfObject[n] = this.elementType.read(paramByteBuffer);
        }
      }
      return localObject;
    }
  }
  
  static class SerializedObjectType
    extends ObjectDataType.AutoDetectDataType
  {
    private int averageSize = 10000;
    
    SerializedObjectType(ObjectDataType paramObjectDataType)
    {
      super(19);
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (paramObject1 == paramObject2) {
        return 0;
      }
      ObjectDataType.AutoDetectDataType localAutoDetectDataType1 = getType(paramObject1);
      ObjectDataType.AutoDetectDataType localAutoDetectDataType2 = getType(paramObject2);
      if ((localAutoDetectDataType1 != this) || (localAutoDetectDataType2 != this))
      {
        if (localAutoDetectDataType1 == localAutoDetectDataType2) {
          return localAutoDetectDataType1.compare(paramObject1, paramObject2);
        }
        return super.compare(paramObject1, paramObject2);
      }
      if (((paramObject1 instanceof Comparable)) && 
        (paramObject1.getClass().isAssignableFrom(paramObject2.getClass()))) {
        return ((Comparable)paramObject1).compareTo(paramObject2);
      }
      if (((paramObject2 instanceof Comparable)) && 
        (paramObject2.getClass().isAssignableFrom(paramObject1.getClass()))) {
        return -((Comparable)paramObject2).compareTo(paramObject1);
      }
      byte[] arrayOfByte1 = ObjectDataType.serialize(paramObject1);
      byte[] arrayOfByte2 = ObjectDataType.serialize(paramObject2);
      return ObjectDataType.compareNotNull(arrayOfByte1, arrayOfByte2);
    }
    
    public int getMemory(Object paramObject)
    {
      ObjectDataType.AutoDetectDataType localAutoDetectDataType = getType(paramObject);
      if (localAutoDetectDataType == this) {
        return this.averageSize;
      }
      return localAutoDetectDataType.getMemory(paramObject);
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      ObjectDataType.AutoDetectDataType localAutoDetectDataType = getType(paramObject);
      if (localAutoDetectDataType != this)
      {
        localAutoDetectDataType.write(paramWriteBuffer, paramObject);
        return;
      }
      byte[] arrayOfByte = ObjectDataType.serialize(paramObject);
      
      int i = arrayOfByte.length * 2;
      
      this.averageSize = ((i + 15 * this.averageSize) / 16);
      paramWriteBuffer.put((byte)19).putVarInt(arrayOfByte.length).put(arrayOfByte);
    }
    
    public Object read(ByteBuffer paramByteBuffer, int paramInt)
    {
      int i = DataUtils.readVarInt(paramByteBuffer);
      byte[] arrayOfByte = DataUtils.newBytes(i);
      paramByteBuffer.get(arrayOfByte);
      return ObjectDataType.deserialize(arrayOfByte);
    }
  }
}
