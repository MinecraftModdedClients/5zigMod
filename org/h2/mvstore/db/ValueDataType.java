package org.h2.mvstore.db;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.rtree.SpatialDataType;
import org.h2.mvstore.rtree.SpatialKey;
import org.h2.result.SortOrder;
import org.h2.store.DataHandler;
import org.h2.tools.SimpleResultSet;
import org.h2.value.CompareMode;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueByte;
import org.h2.value.ValueBytes;
import org.h2.value.ValueDate;
import org.h2.value.ValueDecimal;
import org.h2.value.ValueDouble;
import org.h2.value.ValueFloat;
import org.h2.value.ValueGeometry;
import org.h2.value.ValueInt;
import org.h2.value.ValueJavaObject;
import org.h2.value.ValueLobDb;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;
import org.h2.value.ValueResultSet;
import org.h2.value.ValueShort;
import org.h2.value.ValueString;
import org.h2.value.ValueStringFixed;
import org.h2.value.ValueStringIgnoreCase;
import org.h2.value.ValueTime;
import org.h2.value.ValueTimestamp;
import org.h2.value.ValueUuid;

public class ValueDataType
  implements org.h2.mvstore.type.DataType
{
  private static final int INT_0_15 = 32;
  private static final int LONG_0_7 = 48;
  private static final int DECIMAL_0_1 = 56;
  private static final int DECIMAL_SMALL_0 = 58;
  private static final int DECIMAL_SMALL = 59;
  private static final int DOUBLE_0_1 = 60;
  private static final int FLOAT_0_1 = 62;
  private static final int BOOLEAN_FALSE = 64;
  private static final int BOOLEAN_TRUE = 65;
  private static final int INT_NEG = 66;
  private static final int LONG_NEG = 67;
  private static final int STRING_0_31 = 68;
  private static final int BYTES_0_31 = 100;
  private static final int SPATIAL_KEY_2D = 132;
  final DataHandler handler;
  final CompareMode compareMode;
  final int[] sortTypes;
  SpatialDataType spatialType;
  
  public ValueDataType(CompareMode paramCompareMode, DataHandler paramDataHandler, int[] paramArrayOfInt)
  {
    this.compareMode = paramCompareMode;
    this.handler = paramDataHandler;
    this.sortTypes = paramArrayOfInt;
  }
  
  private SpatialDataType getSpatialDataType()
  {
    if (this.spatialType == null) {
      this.spatialType = new SpatialDataType(2);
    }
    return this.spatialType;
  }
  
  public int compare(Object paramObject1, Object paramObject2)
  {
    if (paramObject1 == paramObject2) {
      return 0;
    }
    if (((paramObject1 instanceof ValueArray)) && ((paramObject2 instanceof ValueArray)))
    {
      Value[] arrayOfValue1 = ((ValueArray)paramObject1).getList();
      Value[] arrayOfValue2 = ((ValueArray)paramObject2).getList();
      int i = arrayOfValue1.length;
      int j = arrayOfValue2.length;
      int k = Math.min(i, j);
      for (int m = 0; m < k; m++)
      {
        int n = this.sortTypes[m];
        int i1 = compareValues(arrayOfValue1[m], arrayOfValue2[m], n);
        if (i1 != 0) {
          return i1;
        }
      }
      if (k < i) {
        return -1;
      }
      if (k < j) {
        return 1;
      }
      return 0;
    }
    return compareValues((Value)paramObject1, (Value)paramObject2, 0);
  }
  
  private int compareValues(Value paramValue1, Value paramValue2, int paramInt)
  {
    if (paramValue1 == paramValue2) {
      return 0;
    }
    if (paramValue1 == null) {
      return -1;
    }
    if (paramValue2 == null) {
      return 1;
    }
    boolean bool = paramValue1 == ValueNull.INSTANCE;
    int i = paramValue2 == ValueNull.INSTANCE ? 1 : 0;
    if ((bool) || (i != 0)) {
      return SortOrder.compareNull(bool, paramInt);
    }
    int j = compareTypeSave(paramValue1, paramValue2);
    if ((paramInt & 0x1) != 0) {
      j = -j;
    }
    return j;
  }
  
  private int compareTypeSave(Value paramValue1, Value paramValue2)
  {
    if (paramValue1 == paramValue2) {
      return 0;
    }
    return paramValue1.compareTypeSave(paramValue2, this.compareMode);
  }
  
  public int getMemory(Object paramObject)
  {
    if ((paramObject instanceof SpatialKey)) {
      return getSpatialDataType().getMemory(paramObject);
    }
    return getMemory((Value)paramObject);
  }
  
  private static int getMemory(Value paramValue)
  {
    return paramValue == null ? 0 : paramValue.getMemory();
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
  
  public Object read(ByteBuffer paramByteBuffer)
  {
    return readValue(paramByteBuffer);
  }
  
  public void write(WriteBuffer paramWriteBuffer, Object paramObject)
  {
    if ((paramObject instanceof SpatialKey))
    {
      paramWriteBuffer.put((byte)-124);
      getSpatialDataType().write(paramWriteBuffer, paramObject);
      return;
    }
    Value localValue = (Value)paramObject;
    writeValue(paramWriteBuffer, localValue);
  }
  
  private void writeValue(WriteBuffer paramWriteBuffer, Value paramValue)
  {
    if (paramValue == ValueNull.INSTANCE)
    {
      paramWriteBuffer.put((byte)0);
      return;
    }
    int i = paramValue.getType();
    Object localObject1;
    byte[] arrayOfByte2;
    long l3;
    long l5;
    Object localObject2;
    int m;
    Object localObject3;
    Object localObject4;
    switch (i)
    {
    case 1: 
      paramWriteBuffer.put((byte)(paramValue.getBoolean().booleanValue() ? 65 : 64));
      
      break;
    case 2: 
      paramWriteBuffer.put((byte)i).put(paramValue.getByte());
      break;
    case 3: 
      paramWriteBuffer.put((byte)i).putShort(paramValue.getShort());
      break;
    case 4: 
      int j = paramValue.getInt();
      if (j < 0) {
        paramWriteBuffer.put((byte)66).putVarInt(-j);
      } else if (j < 16) {
        paramWriteBuffer.put((byte)(32 + j));
      } else {
        paramWriteBuffer.put((byte)i).putVarInt(j);
      }
      break;
    case 5: 
      long l1 = paramValue.getLong();
      if (l1 < 0L) {
        paramWriteBuffer.put((byte)67).putVarLong(-l1);
      } else if (l1 < 8L) {
        paramWriteBuffer.put((byte)(int)(48L + l1));
      } else {
        paramWriteBuffer.put((byte)i).putVarLong(l1);
      }
      break;
    case 6: 
      localObject1 = paramValue.getBigDecimal();
      if (BigDecimal.ZERO.equals(localObject1))
      {
        paramWriteBuffer.put((byte)56);
      }
      else if (BigDecimal.ONE.equals(localObject1))
      {
        paramWriteBuffer.put((byte)57);
      }
      else
      {
        int k = ((BigDecimal)localObject1).scale();
        BigInteger localBigInteger = ((BigDecimal)localObject1).unscaledValue();
        int i2 = localBigInteger.bitLength();
        if (i2 <= 63)
        {
          if (k == 0) {
            paramWriteBuffer.put((byte)58).putVarLong(localBigInteger.longValue());
          } else {
            paramWriteBuffer.put((byte)59).putVarInt(k).putVarLong(localBigInteger.longValue());
          }
        }
        else
        {
          arrayOfByte2 = localBigInteger.toByteArray();
          paramWriteBuffer.put((byte)i).putVarInt(k).putVarInt(arrayOfByte2.length).put(arrayOfByte2);
        }
      }
      break;
    case 9: 
      localObject1 = (ValueTime)paramValue;
      l3 = ((ValueTime)localObject1).getNanos();
      l5 = l3 / 1000000L;
      l3 -= l5 * 1000000L;
      paramWriteBuffer.put((byte)i).putVarLong(l5).putVarLong(l3);
      
      break;
    case 10: 
      long l2 = ((ValueDate)paramValue).getDateValue();
      paramWriteBuffer.put((byte)i).putVarLong(l2);
      break;
    case 11: 
      localObject2 = (ValueTimestamp)paramValue;
      l3 = ((ValueTimestamp)localObject2).getDateValue();
      l5 = ((ValueTimestamp)localObject2).getTimeNanos();
      long l6 = l5 / 1000000L;
      l5 -= l6 * 1000000L;
      paramWriteBuffer.put((byte)i).putVarLong(l3).putVarLong(l6).putVarLong(l5);
      
      break;
    case 19: 
      localObject2 = paramValue.getBytesNoCopy();
      paramWriteBuffer.put((byte)i).putVarInt(localObject2.length).put((byte[])localObject2);
      
      break;
    case 12: 
      localObject2 = paramValue.getBytesNoCopy();
      m = localObject2.length;
      if (m < 32) {
        paramWriteBuffer.put((byte)(100 + m)).put((byte[])localObject2);
      } else {
        paramWriteBuffer.put((byte)i).putVarInt(localObject2.length).put((byte[])localObject2);
      }
      break;
    case 20: 
      localObject2 = (ValueUuid)paramValue;
      paramWriteBuffer.put((byte)i).putLong(((ValueUuid)localObject2).getHigh()).putLong(((ValueUuid)localObject2).getLow());
      
      break;
    case 13: 
      localObject2 = paramValue.getString();
      m = ((String)localObject2).length();
      if (m < 32)
      {
        paramWriteBuffer.put((byte)(68 + m)).putStringData((String)localObject2, m);
      }
      else
      {
        paramWriteBuffer.put((byte)i);
        writeString(paramWriteBuffer, (String)localObject2);
      }
      break;
    case 14: 
    case 21: 
      paramWriteBuffer.put((byte)i);
      writeString(paramWriteBuffer, paramValue.getString());
      break;
    case 7: 
      double d = paramValue.getDouble();
      if (d == 1.0D)
      {
        paramWriteBuffer.put((byte)61);
      }
      else
      {
        long l4 = Double.doubleToLongBits(d);
        if (l4 == ValueDouble.ZERO_BITS) {
          paramWriteBuffer.put((byte)60);
        } else {
          paramWriteBuffer.put((byte)i).putVarLong(Long.reverse(l4));
        }
      }
      break;
    case 8: 
      float f = paramValue.getFloat();
      if (f == 1.0F)
      {
        paramWriteBuffer.put((byte)63);
      }
      else
      {
        m = Float.floatToIntBits(f);
        if (m == ValueFloat.ZERO_BITS) {
          paramWriteBuffer.put((byte)62);
        } else {
          paramWriteBuffer.put((byte)i).putVarInt(Integer.reverse(m));
        }
      }
      break;
    case 15: 
    case 16: 
      paramWriteBuffer.put((byte)i);
      localObject3 = (ValueLobDb)paramValue;
      localObject4 = ((ValueLobDb)localObject3).getSmall();
      if (localObject4 == null) {
        paramWriteBuffer.putVarInt(-3).putVarInt(((ValueLobDb)localObject3).getTableId()).putVarLong(((ValueLobDb)localObject3).getLobId()).putVarLong(((ValueLobDb)localObject3).getPrecision());
      } else {
        paramWriteBuffer.putVarInt(localObject4.length).put((byte[])localObject4);
      }
      break;
    case 17: 
      localObject3 = ((ValueArray)paramValue).getList();
      paramWriteBuffer.put((byte)i).putVarInt(localObject3.length);
      for (arrayOfByte2 : localObject3) {
        writeValue(paramWriteBuffer, arrayOfByte2);
      }
      break;
    case 18: 
      paramWriteBuffer.put((byte)i);
      try
      {
        localObject3 = ((ValueResultSet)paramValue).getResultSet();
        ((ResultSet)localObject3).beforeFirst();
        localObject4 = ((ResultSet)localObject3).getMetaData();
        ??? = ((ResultSetMetaData)localObject4).getColumnCount();
        paramWriteBuffer.putVarInt(???);
        for (??? = 0; ??? < ???; ???++)
        {
          writeString(paramWriteBuffer, ((ResultSetMetaData)localObject4).getColumnName(??? + 1));
          paramWriteBuffer.putVarInt(((ResultSetMetaData)localObject4).getColumnType(??? + 1)).putVarInt(((ResultSetMetaData)localObject4).getPrecision(??? + 1)).putVarInt(((ResultSetMetaData)localObject4).getScale(??? + 1));
        }
        while (((ResultSet)localObject3).next())
        {
          paramWriteBuffer.put((byte)1);
          for (??? = 0; ??? < ???; ???++)
          {
            int i4 = org.h2.value.DataType.getValueTypeFromResultSet((ResultSetMetaData)localObject4, ??? + 1);
            
            Value localValue = org.h2.value.DataType.readValue(null, (ResultSet)localObject3, ??? + 1, i4);
            
            writeValue(paramWriteBuffer, localValue);
          }
        }
        paramWriteBuffer.put((byte)0);
        ((ResultSet)localObject3).beforeFirst();
      }
      catch (SQLException localSQLException)
      {
        throw DbException.convert(localSQLException);
      }
    case 22: 
      byte[] arrayOfByte1 = paramValue.getBytes();
      int n = arrayOfByte1.length;
      paramWriteBuffer.put((byte)i).putVarInt(n).put(arrayOfByte1);
      
      break;
    default: 
      DbException.throwInternalError("type=" + paramValue.getType());
    }
  }
  
  private static void writeString(WriteBuffer paramWriteBuffer, String paramString)
  {
    int i = paramString.length();
    paramWriteBuffer.putVarInt(i).putStringData(paramString, i);
  }
  
  private Object readValue(ByteBuffer paramByteBuffer)
  {
    int i = paramByteBuffer.get() & 0xFF;
    int j;
    long l1;
    long l2;
    int k;
    byte[] arrayOfByte1;
    int i3;
    int m;
    byte[] arrayOfByte2;
    switch (i)
    {
    case 0: 
      return ValueNull.INSTANCE;
    case 65: 
      return ValueBoolean.get(true);
    case 64: 
      return ValueBoolean.get(false);
    case 66: 
      return ValueInt.get(-readVarInt(paramByteBuffer));
    case 4: 
      return ValueInt.get(readVarInt(paramByteBuffer));
    case 67: 
      return ValueLong.get(-readVarLong(paramByteBuffer));
    case 5: 
      return ValueLong.get(readVarLong(paramByteBuffer));
    case 2: 
      return ValueByte.get(paramByteBuffer.get());
    case 3: 
      return ValueShort.get(paramByteBuffer.getShort());
    case 56: 
      return ValueDecimal.ZERO;
    case 57: 
      return ValueDecimal.ONE;
    case 58: 
      return ValueDecimal.get(BigDecimal.valueOf(readVarLong(paramByteBuffer)));
    case 59: 
      j = readVarInt(paramByteBuffer);
      return ValueDecimal.get(BigDecimal.valueOf(readVarLong(paramByteBuffer), j));
    case 6: 
      j = readVarInt(paramByteBuffer);
      int n = readVarInt(paramByteBuffer);
      byte[] arrayOfByte3 = DataUtils.newBytes(n);
      paramByteBuffer.get(arrayOfByte3, 0, n);
      BigInteger localBigInteger = new BigInteger(arrayOfByte3);
      return ValueDecimal.get(new BigDecimal(localBigInteger, j));
    case 10: 
      return ValueDate.fromDateValue(readVarLong(paramByteBuffer));
    case 9: 
      l1 = readVarLong(paramByteBuffer) * 1000000L + readVarLong(paramByteBuffer);
      return ValueTime.fromNanos(l1);
    case 11: 
      l1 = readVarLong(paramByteBuffer);
      l2 = readVarLong(paramByteBuffer) * 1000000L + readVarLong(paramByteBuffer);
      return ValueTimestamp.fromDateValueAndNanos(l1, l2);
    case 12: 
      k = readVarInt(paramByteBuffer);
      arrayOfByte1 = DataUtils.newBytes(k);
      paramByteBuffer.get(arrayOfByte1, 0, k);
      return ValueBytes.getNoCopy(arrayOfByte1);
    case 19: 
      k = readVarInt(paramByteBuffer);
      arrayOfByte1 = DataUtils.newBytes(k);
      paramByteBuffer.get(arrayOfByte1, 0, k);
      return ValueJavaObject.getNoCopy(null, arrayOfByte1, this.handler);
    case 20: 
      return ValueUuid.get(paramByteBuffer.getLong(), paramByteBuffer.getLong());
    case 13: 
      return ValueString.get(readString(paramByteBuffer));
    case 14: 
      return ValueStringIgnoreCase.get(readString(paramByteBuffer));
    case 21: 
      return ValueStringFixed.get(readString(paramByteBuffer));
    case 62: 
      return ValueFloat.get(0.0F);
    case 63: 
      return ValueFloat.get(1.0F);
    case 60: 
      return ValueDouble.get(0.0D);
    case 61: 
      return ValueDouble.get(1.0D);
    case 7: 
      return ValueDouble.get(Double.longBitsToDouble(Long.reverse(readVarLong(paramByteBuffer))));
    case 8: 
      return ValueFloat.get(Float.intBitsToFloat(Integer.reverse(readVarInt(paramByteBuffer))));
    case 15: 
    case 16: 
      k = readVarInt(paramByteBuffer);
      if (k >= 0)
      {
        arrayOfByte1 = DataUtils.newBytes(k);
        paramByteBuffer.get(arrayOfByte1, 0, k);
        return ValueLobDb.createSmallLob(i, arrayOfByte1);
      }
      if (k == -3)
      {
        int i1 = readVarInt(paramByteBuffer);
        l2 = readVarLong(paramByteBuffer);
        long l3 = readVarLong(paramByteBuffer);
        ValueLobDb localValueLobDb = ValueLobDb.create(i, this.handler, i1, l2, null, l3);
        
        return localValueLobDb;
      }
      throw DbException.get(90030, "lob type: " + k);
    case 17: 
      k = readVarInt(paramByteBuffer);
      Value[] arrayOfValue = new Value[k];
      for (i3 = 0; i3 < k; i3++) {
        arrayOfValue[i3] = ((Value)readValue(paramByteBuffer));
      }
      return ValueArray.get(arrayOfValue);
    case 18: 
      SimpleResultSet localSimpleResultSet = new SimpleResultSet();
      localSimpleResultSet.setAutoClose(false);
      int i2 = readVarInt(paramByteBuffer);
      for (i3 = 0; i3 < i2; i3++) {
        localSimpleResultSet.addColumn(readString(paramByteBuffer), readVarInt(paramByteBuffer), readVarInt(paramByteBuffer), readVarInt(paramByteBuffer));
      }
      while (paramByteBuffer.get() != 0)
      {
        Object[] arrayOfObject = new Object[i2];
        for (int i4 = 0; i4 < i2; i4++) {
          arrayOfObject[i4] = ((Value)readValue(paramByteBuffer)).getObject();
        }
        localSimpleResultSet.addRow(arrayOfObject);
      }
      return ValueResultSet.get(localSimpleResultSet);
    case 22: 
      m = readVarInt(paramByteBuffer);
      arrayOfByte2 = DataUtils.newBytes(m);
      paramByteBuffer.get(arrayOfByte2, 0, m);
      return ValueGeometry.get(arrayOfByte2);
    case 132: 
      return getSpatialDataType().read(paramByteBuffer);
    }
    if ((i >= 32) && (i < 48)) {
      return ValueInt.get(i - 32);
    }
    if ((i >= 48) && (i < 56)) {
      return ValueLong.get(i - 48);
    }
    if ((i >= 100) && (i < 132))
    {
      m = i - 100;
      arrayOfByte2 = DataUtils.newBytes(m);
      paramByteBuffer.get(arrayOfByte2, 0, m);
      return ValueBytes.getNoCopy(arrayOfByte2);
    }
    if ((i >= 68) && (i < 100)) {
      return ValueString.get(readString(paramByteBuffer, i - 68));
    }
    throw DbException.get(90030, "type: " + i);
  }
  
  private static int readVarInt(ByteBuffer paramByteBuffer)
  {
    return DataUtils.readVarInt(paramByteBuffer);
  }
  
  private static long readVarLong(ByteBuffer paramByteBuffer)
  {
    return DataUtils.readVarLong(paramByteBuffer);
  }
  
  private static String readString(ByteBuffer paramByteBuffer, int paramInt)
  {
    return DataUtils.readString(paramByteBuffer, paramInt);
  }
  
  private static String readString(ByteBuffer paramByteBuffer)
  {
    int i = readVarInt(paramByteBuffer);
    return DataUtils.readString(paramByteBuffer, i);
  }
  
  public int hashCode()
  {
    return this.compareMode.hashCode() ^ Arrays.hashCode(this.sortTypes);
  }
  
  public boolean equals(Object paramObject)
  {
    if (paramObject == this) {
      return true;
    }
    if (!(paramObject instanceof ValueDataType)) {
      return false;
    }
    ValueDataType localValueDataType = (ValueDataType)paramObject;
    if (!this.compareMode.equals(localValueDataType.compareMode)) {
      return false;
    }
    return Arrays.equals(this.sortTypes, localValueDataType.sortTypes);
  }
}
