package org.h2.store;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils;
import org.h2.tools.SimpleResultSet;
import org.h2.util.DateTimeUtils;
import org.h2.util.MathUtils;
import org.h2.value.DataType;
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
import org.h2.value.ValueLob;
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

public class Data
{
  public static final int LENGTH_INT = 4;
  private static final int LENGTH_LONG = 8;
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
  private static final int LOCAL_TIME = 132;
  private static final int LOCAL_DATE = 133;
  private static final int LOCAL_TIMESTAMP = 134;
  private static final long MILLIS_PER_MINUTE = 60000L;
  private byte[] data;
  private int pos;
  private final DataHandler handler;
  
  private Data(DataHandler paramDataHandler, byte[] paramArrayOfByte)
  {
    this.handler = paramDataHandler;
    this.data = paramArrayOfByte;
  }
  
  public void setInt(int paramInt1, int paramInt2)
  {
    byte[] arrayOfByte = this.data;
    arrayOfByte[paramInt1] = ((byte)(paramInt2 >> 24));
    arrayOfByte[(paramInt1 + 1)] = ((byte)(paramInt2 >> 16));
    arrayOfByte[(paramInt1 + 2)] = ((byte)(paramInt2 >> 8));
    arrayOfByte[(paramInt1 + 3)] = ((byte)paramInt2);
  }
  
  public void writeInt(int paramInt)
  {
    byte[] arrayOfByte = this.data;
    arrayOfByte[this.pos] = ((byte)(paramInt >> 24));
    arrayOfByte[(this.pos + 1)] = ((byte)(paramInt >> 16));
    arrayOfByte[(this.pos + 2)] = ((byte)(paramInt >> 8));
    arrayOfByte[(this.pos + 3)] = ((byte)paramInt);
    this.pos += 4;
  }
  
  public int readInt()
  {
    byte[] arrayOfByte = this.data;
    int i = (arrayOfByte[this.pos] << 24) + ((arrayOfByte[(this.pos + 1)] & 0xFF) << 16) + ((arrayOfByte[(this.pos + 2)] & 0xFF) << 8) + (arrayOfByte[(this.pos + 3)] & 0xFF);
    
    this.pos += 4;
    return i;
  }
  
  public static int getStringLen(String paramString)
  {
    int i = paramString.length();
    return getStringWithoutLengthLen(paramString, i) + getVarIntLen(i);
  }
  
  private static int getStringWithoutLengthLen(String paramString, int paramInt)
  {
    int i = 0;
    for (int j = 0; j < paramInt; j++)
    {
      int k = paramString.charAt(j);
      if (k >= 2048) {
        i += 2;
      } else if (k >= 128) {
        i++;
      }
    }
    return paramInt + i;
  }
  
  public String readString()
  {
    int i = readVarInt();
    return readString(i);
  }
  
  private String readString(int paramInt)
  {
    byte[] arrayOfByte = this.data;
    int i = this.pos;
    char[] arrayOfChar = new char[paramInt];
    for (int j = 0; j < paramInt; j++)
    {
      int k = arrayOfByte[(i++)] & 0xFF;
      if (k < 128) {
        arrayOfChar[j] = ((char)k);
      } else if (k >= 224) {
        arrayOfChar[j] = ((char)(((k & 0xF) << 12) + ((arrayOfByte[(i++)] & 0x3F) << 6) + (arrayOfByte[(i++)] & 0x3F)));
      } else {
        arrayOfChar[j] = ((char)(((k & 0x1F) << 6) + (arrayOfByte[(i++)] & 0x3F)));
      }
    }
    this.pos = i;
    return new String(arrayOfChar);
  }
  
  public void writeString(String paramString)
  {
    int i = paramString.length();
    writeVarInt(i);
    writeStringWithoutLength(paramString, i);
  }
  
  private void writeStringWithoutLength(String paramString, int paramInt)
  {
    int i = this.pos;
    byte[] arrayOfByte = this.data;
    for (int j = 0; j < paramInt; j++)
    {
      int k = paramString.charAt(j);
      if (k < 128)
      {
        arrayOfByte[(i++)] = ((byte)k);
      }
      else if (k >= 2048)
      {
        arrayOfByte[(i++)] = ((byte)(0xE0 | k >> 12));
        arrayOfByte[(i++)] = ((byte)(k >> 6 & 0x3F));
        arrayOfByte[(i++)] = ((byte)(k & 0x3F));
      }
      else
      {
        arrayOfByte[(i++)] = ((byte)(0xC0 | k >> 6));
        arrayOfByte[(i++)] = ((byte)(k & 0x3F));
      }
    }
    this.pos = i;
  }
  
  private void writeStringWithoutLength(char[] paramArrayOfChar, int paramInt)
  {
    int i = this.pos;
    byte[] arrayOfByte = this.data;
    for (int j = 0; j < paramInt; j++)
    {
      int k = paramArrayOfChar[j];
      if (k < 128)
      {
        arrayOfByte[(i++)] = ((byte)k);
      }
      else if (k >= 2048)
      {
        arrayOfByte[(i++)] = ((byte)(0xE0 | k >> 12));
        arrayOfByte[(i++)] = ((byte)(k >> 6 & 0x3F));
        arrayOfByte[(i++)] = ((byte)(k & 0x3F));
      }
      else
      {
        arrayOfByte[(i++)] = ((byte)(0xC0 | k >> 6));
        arrayOfByte[(i++)] = ((byte)(k & 0x3F));
      }
    }
    this.pos = i;
  }
  
  public static Data create(DataHandler paramDataHandler, int paramInt)
  {
    return new Data(paramDataHandler, new byte[paramInt]);
  }
  
  public static Data create(DataHandler paramDataHandler, byte[] paramArrayOfByte)
  {
    return new Data(paramDataHandler, paramArrayOfByte);
  }
  
  public int length()
  {
    return this.pos;
  }
  
  public byte[] getBytes()
  {
    return this.data;
  }
  
  public void reset()
  {
    this.pos = 0;
  }
  
  public void write(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    System.arraycopy(paramArrayOfByte, paramInt1, this.data, this.pos, paramInt2);
    this.pos += paramInt2;
  }
  
  public void read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    System.arraycopy(this.data, this.pos, paramArrayOfByte, paramInt1, paramInt2);
    this.pos += paramInt2;
  }
  
  public void writeByte(byte paramByte)
  {
    this.data[(this.pos++)] = paramByte;
  }
  
  public byte readByte()
  {
    return this.data[(this.pos++)];
  }
  
  public long readLong()
  {
    return (readInt() << 32) + (readInt() & 0xFFFFFFFF);
  }
  
  public void writeLong(long paramLong)
  {
    writeInt((int)(paramLong >>> 32));
    writeInt((int)paramLong);
  }
  
  public void writeValue(Value paramValue)
  {
    int i = this.pos;
    if (paramValue == ValueNull.INSTANCE)
    {
      this.data[(this.pos++)] = 0;
      return;
    }
    int j = paramValue.getType();
    Object localObject1;
    byte[] arrayOfByte;
    long l3;
    long l5;
    Object localObject2;
    int n;
    Object localObject3;
    Object localObject4;
    int i1;
    switch (j)
    {
    case 1: 
      writeByte((byte)(paramValue.getBoolean().booleanValue() ? 65 : 64));
      
      break;
    case 2: 
      writeByte((byte)j);
      writeByte(paramValue.getByte());
      break;
    case 3: 
      writeByte((byte)j);
      writeShortInt(paramValue.getShort());
      break;
    case 4: 
      int k = paramValue.getInt();
      if (k < 0)
      {
        writeByte((byte)66);
        writeVarInt(-k);
      }
      else if (k < 16)
      {
        writeByte((byte)(32 + k));
      }
      else
      {
        writeByte((byte)j);
        writeVarInt(k);
      }
      break;
    case 5: 
      long l1 = paramValue.getLong();
      if (l1 < 0L)
      {
        writeByte((byte)67);
        writeVarLong(-l1);
      }
      else if (l1 < 8L)
      {
        writeByte((byte)(int)(48L + l1));
      }
      else
      {
        writeByte((byte)j);
        writeVarLong(l1);
      }
      break;
    case 6: 
      localObject1 = paramValue.getBigDecimal();
      if (BigDecimal.ZERO.equals(localObject1))
      {
        writeByte((byte)56);
      }
      else if (BigDecimal.ONE.equals(localObject1))
      {
        writeByte((byte)57);
      }
      else
      {
        int m = ((BigDecimal)localObject1).scale();
        BigInteger localBigInteger = ((BigDecimal)localObject1).unscaledValue();
        int i2 = localBigInteger.bitLength();
        if (i2 <= 63)
        {
          if (m == 0)
          {
            writeByte((byte)58);
            writeVarLong(localBigInteger.longValue());
          }
          else
          {
            writeByte((byte)59);
            writeVarInt(m);
            writeVarLong(localBigInteger.longValue());
          }
        }
        else
        {
          writeByte((byte)j);
          writeVarInt(m);
          arrayOfByte = localBigInteger.toByteArray();
          writeVarInt(arrayOfByte.length);
          write(arrayOfByte, 0, arrayOfByte.length);
        }
      }
      break;
    case 9: 
      if (SysProperties.STORE_LOCAL_TIME)
      {
        writeByte((byte)-124);
        localObject1 = (ValueTime)paramValue;
        l3 = ((ValueTime)localObject1).getNanos();
        l5 = l3 / 1000000L;
        l3 -= l5 * 1000000L;
        writeVarLong(l5);
        writeVarLong(l3);
      }
      else
      {
        writeByte((byte)j);
        writeVarLong(DateTimeUtils.getTimeLocalWithoutDst(paramValue.getTime()));
      }
      break;
    case 10: 
      long l2;
      if (SysProperties.STORE_LOCAL_TIME)
      {
        writeByte((byte)-123);
        l2 = ((ValueDate)paramValue).getDateValue();
        writeVarLong(l2);
      }
      else
      {
        writeByte((byte)j);
        l2 = DateTimeUtils.getTimeLocalWithoutDst(paramValue.getDate());
        writeVarLong(l2 / 60000L);
      }
      break;
    case 11: 
      if (SysProperties.STORE_LOCAL_TIME)
      {
        writeByte((byte)-122);
        localObject2 = (ValueTimestamp)paramValue;
        l3 = ((ValueTimestamp)localObject2).getDateValue();
        writeVarLong(l3);
        l5 = ((ValueTimestamp)localObject2).getTimeNanos();
        long l6 = l5 / 1000000L;
        l5 -= l6 * 1000000L;
        writeVarLong(l6);
        writeVarLong(l5);
      }
      else
      {
        localObject2 = paramValue.getTimestamp();
        writeByte((byte)j);
        writeVarLong(DateTimeUtils.getTimeLocalWithoutDst((Date)localObject2));
        writeVarInt(((Timestamp)localObject2).getNanos() % 1000000);
      }
      break;
    case 19: 
    case 22: 
      writeByte((byte)j);
      localObject2 = paramValue.getBytesNoCopy();
      n = localObject2.length;
      writeVarInt(n);
      write((byte[])localObject2, 0, n);
      break;
    case 12: 
      localObject2 = paramValue.getBytesNoCopy();
      n = localObject2.length;
      if (n < 32)
      {
        writeByte((byte)(100 + n));
        write((byte[])localObject2, 0, n);
      }
      else
      {
        writeByte((byte)j);
        writeVarInt(n);
        write((byte[])localObject2, 0, n);
      }
      break;
    case 20: 
      writeByte((byte)j);
      localObject2 = (ValueUuid)paramValue;
      writeLong(((ValueUuid)localObject2).getHigh());
      writeLong(((ValueUuid)localObject2).getLow());
      break;
    case 13: 
      localObject2 = paramValue.getString();
      n = ((String)localObject2).length();
      if (n < 32)
      {
        writeByte((byte)(68 + n));
        writeStringWithoutLength((String)localObject2, n);
      }
      else
      {
        writeByte((byte)j);
        writeString((String)localObject2);
      }
      break;
    case 14: 
    case 21: 
      writeByte((byte)j);
      writeString(paramValue.getString());
      break;
    case 7: 
      double d = paramValue.getDouble();
      if (d == 1.0D)
      {
        writeByte((byte)61);
      }
      else
      {
        long l4 = Double.doubleToLongBits(d);
        if (l4 == ValueDouble.ZERO_BITS)
        {
          writeByte((byte)60);
        }
        else
        {
          writeByte((byte)j);
          writeVarLong(Long.reverse(l4));
        }
      }
      break;
    case 8: 
      float f = paramValue.getFloat();
      if (f == 1.0F)
      {
        writeByte((byte)63);
      }
      else
      {
        n = Float.floatToIntBits(f);
        if (n == ValueFloat.ZERO_BITS)
        {
          writeByte((byte)62);
        }
        else
        {
          writeByte((byte)j);
          writeVarInt(Integer.reverse(n));
        }
      }
      break;
    case 15: 
    case 16: 
      writeByte((byte)j);
      if ((paramValue instanceof ValueLob))
      {
        localObject3 = (ValueLob)paramValue;
        ((ValueLob)localObject3).convertToFileIfRequired(this.handler);
        localObject4 = ((ValueLob)localObject3).getSmall();
        if (localObject4 == null)
        {
          i1 = -1;
          if (!((ValueLob)localObject3).isLinked()) {
            i1 = -2;
          }
          writeVarInt(i1);
          writeVarInt(((ValueLob)localObject3).getTableId());
          writeVarInt(((ValueLob)localObject3).getObjectId());
          writeVarLong(((ValueLob)localObject3).getPrecision());
          writeByte((byte)(((ValueLob)localObject3).isCompressed() ? 1 : 0));
          if (i1 == -2) {
            writeString(((ValueLob)localObject3).getFileName());
          }
        }
        else
        {
          writeVarInt(localObject4.length);
          write((byte[])localObject4, 0, localObject4.length);
        }
      }
      else
      {
        localObject3 = (ValueLobDb)paramValue;
        localObject4 = ((ValueLobDb)localObject3).getSmall();
        if (localObject4 == null)
        {
          writeVarInt(-3);
          writeVarInt(((ValueLobDb)localObject3).getTableId());
          writeVarLong(((ValueLobDb)localObject3).getLobId());
          writeVarLong(((ValueLobDb)localObject3).getPrecision());
        }
        else
        {
          writeVarInt(localObject4.length);
          write((byte[])localObject4, 0, localObject4.length);
        }
      }
      break;
    case 17: 
      writeByte((byte)j);
      localObject3 = ((ValueArray)paramValue).getList();
      writeVarInt(localObject3.length);
      for (arrayOfByte : localObject3) {
        writeValue(arrayOfByte);
      }
      break;
    case 18: 
      writeByte((byte)j);
      try
      {
        localObject3 = ((ValueResultSet)paramValue).getResultSet();
        ((ResultSet)localObject3).beforeFirst();
        localObject4 = ((ResultSet)localObject3).getMetaData();
        i1 = ((ResultSetMetaData)localObject4).getColumnCount();
        writeVarInt(i1);
        for (??? = 0; ??? < i1; ???++)
        {
          writeString(((ResultSetMetaData)localObject4).getColumnName(??? + 1));
          writeVarInt(((ResultSetMetaData)localObject4).getColumnType(??? + 1));
          writeVarInt(((ResultSetMetaData)localObject4).getPrecision(??? + 1));
          writeVarInt(((ResultSetMetaData)localObject4).getScale(??? + 1));
        }
        while (((ResultSet)localObject3).next())
        {
          writeByte((byte)1);
          for (??? = 0; ??? < i1; ???++)
          {
            int i4 = DataType.getValueTypeFromResultSet((ResultSetMetaData)localObject4, ??? + 1);
            Value localValue = DataType.readValue(null, (ResultSet)localObject3, ??? + 1, i4);
            writeValue(localValue);
          }
        }
        writeByte((byte)0);
        ((ResultSet)localObject3).beforeFirst();
      }
      catch (SQLException localSQLException)
      {
        throw DbException.convert(localSQLException);
      }
    default: 
      DbException.throwInternalError("type=" + paramValue.getType());
    }
    if ((SysProperties.CHECK2) && 
      (this.pos - i != getValueLen(paramValue, this.handler))) {
      throw DbException.throwInternalError("value size error: got " + (this.pos - i) + " expected " + getValueLen(paramValue, this.handler));
    }
  }
  
  public Value readValue()
  {
    int i = this.data[(this.pos++)] & 0xFF;
    int j;
    long l1;
    long l2;
    int k;
    byte[] arrayOfByte1;
    int i3;
    switch (i)
    {
    case 0: 
      return ValueNull.INSTANCE;
    case 65: 
      return ValueBoolean.get(true);
    case 64: 
      return ValueBoolean.get(false);
    case 66: 
      return ValueInt.get(-readVarInt());
    case 4: 
      return ValueInt.get(readVarInt());
    case 67: 
      return ValueLong.get(-readVarLong());
    case 5: 
      return ValueLong.get(readVarLong());
    case 2: 
      return ValueByte.get(readByte());
    case 3: 
      return ValueShort.get(readShortInt());
    case 56: 
      return (ValueDecimal)ValueDecimal.ZERO;
    case 57: 
      return (ValueDecimal)ValueDecimal.ONE;
    case 58: 
      return ValueDecimal.get(BigDecimal.valueOf(readVarLong()));
    case 59: 
      j = readVarInt();
      return ValueDecimal.get(BigDecimal.valueOf(readVarLong(), j));
    case 6: 
      j = readVarInt();
      int n = readVarInt();
      byte[] arrayOfByte3 = DataUtils.newBytes(n);
      read(arrayOfByte3, 0, n);
      BigInteger localBigInteger = new BigInteger(arrayOfByte3);
      return ValueDecimal.get(new BigDecimal(localBigInteger, j));
    case 133: 
      return ValueDate.fromDateValue(readVarLong());
    case 10: 
      l1 = readVarLong() * 60000L;
      return ValueDate.fromMillis(DateTimeUtils.getTimeUTCWithoutDst(l1));
    case 132: 
      l1 = readVarLong() * 1000000L + readVarLong();
      return ValueTime.fromNanos(l1);
    case 9: 
      return ValueTime.fromMillis(DateTimeUtils.getTimeUTCWithoutDst(readVarLong()));
    case 134: 
      l1 = readVarLong();
      l2 = readVarLong() * 1000000L + readVarLong();
      return ValueTimestamp.fromDateValueAndNanos(l1, l2);
    case 11: 
      return ValueTimestamp.fromMillisNanos(DateTimeUtils.getTimeUTCWithoutDst(readVarLong()), readVarInt());
    case 12: 
      k = readVarInt();
      arrayOfByte1 = DataUtils.newBytes(k);
      read(arrayOfByte1, 0, k);
      return ValueBytes.getNoCopy(arrayOfByte1);
    case 22: 
      k = readVarInt();
      arrayOfByte1 = DataUtils.newBytes(k);
      read(arrayOfByte1, 0, k);
      return ValueGeometry.get(arrayOfByte1);
    case 19: 
      k = readVarInt();
      arrayOfByte1 = DataUtils.newBytes(k);
      read(arrayOfByte1, 0, k);
      return ValueJavaObject.getNoCopy(null, arrayOfByte1, this.handler);
    case 20: 
      return ValueUuid.get(readLong(), readLong());
    case 13: 
      return ValueString.get(readString());
    case 14: 
      return ValueStringIgnoreCase.get(readString());
    case 21: 
      return ValueStringFixed.get(readString());
    case 62: 
      return ValueFloat.get(0.0F);
    case 63: 
      return ValueFloat.get(1.0F);
    case 60: 
      return ValueDouble.get(0.0D);
    case 61: 
      return ValueDouble.get(1.0D);
    case 7: 
      return ValueDouble.get(Double.longBitsToDouble(Long.reverse(readVarLong())));
    case 8: 
      return ValueFloat.get(Float.intBitsToFloat(Integer.reverse(readVarInt())));
    case 15: 
    case 16: 
      k = readVarInt();
      if (k >= 0)
      {
        arrayOfByte1 = DataUtils.newBytes(k);
        read(arrayOfByte1, 0, k);
        return ValueLobDb.createSmallLob(i, arrayOfByte1);
      }
      Object localObject;
      if (k == -3)
      {
        i1 = readVarInt();
        l2 = readVarLong();
        long l4 = readVarLong();
        localObject = ValueLobDb.create(i, this.handler, i1, l2, null, l4);
        
        return (Value)localObject;
      }
      int i1 = readVarInt();
      i3 = readVarInt();
      long l3 = 0L;
      boolean bool = false;
      if ((k == -1) || (k == -2))
      {
        l3 = readVarLong();
        bool = readByte() == 1;
      }
      if (k == -2)
      {
        localObject = readString();
        return ValueLob.openUnlinked(i, this.handler, i1, i3, l3, bool, (String)localObject);
      }
      return ValueLob.openLinked(i, this.handler, i1, i3, l3, bool);
    case 17: 
      k = readVarInt();
      Value[] arrayOfValue = new Value[k];
      for (i3 = 0; i3 < k; i3++) {
        arrayOfValue[i3] = readValue();
      }
      return ValueArray.get(arrayOfValue);
    case 18: 
      SimpleResultSet localSimpleResultSet = new SimpleResultSet();
      localSimpleResultSet.setAutoClose(false);
      int i2 = readVarInt();
      for (i3 = 0; i3 < i2; i3++) {
        localSimpleResultSet.addColumn(readString(), readVarInt(), readVarInt(), readVarInt());
      }
      while (readByte() != 0)
      {
        Object[] arrayOfObject = new Object[i2];
        for (int i4 = 0; i4 < i2; i4++) {
          arrayOfObject[i4] = readValue().getObject();
        }
        localSimpleResultSet.addRow(arrayOfObject);
      }
      return ValueResultSet.get(localSimpleResultSet);
    }
    if ((i >= 32) && (i < 48)) {
      return ValueInt.get(i - 32);
    }
    if ((i >= 48) && (i < 56)) {
      return ValueLong.get(i - 48);
    }
    if ((i >= 100) && (i < 132))
    {
      int m = i - 100;
      byte[] arrayOfByte2 = DataUtils.newBytes(m);
      read(arrayOfByte2, 0, m);
      return ValueBytes.getNoCopy(arrayOfByte2);
    }
    if ((i >= 68) && (i < 100)) {
      return ValueString.get(readString(i - 68));
    }
    throw DbException.get(90030, "type: " + i);
  }
  
  public int getValueLen(Value paramValue)
  {
    return getValueLen(paramValue, this.handler);
  }
  
  public static int getValueLen(Value paramValue, DataHandler paramDataHandler)
  {
    if (paramValue == ValueNull.INSTANCE) {
      return 1;
    }
    int m;
    Object localObject1;
    long l2;
    Object localObject2;
    Object localObject4;
    int i3;
    switch (paramValue.getType())
    {
    case 1: 
      return 1;
    case 2: 
      return 2;
    case 3: 
      return 3;
    case 4: 
      int i = paramValue.getInt();
      if (i < 0) {
        return 1 + getVarIntLen(-i);
      }
      if (i < 16) {
        return 1;
      }
      return 1 + getVarIntLen(i);
    case 5: 
      long l1 = paramValue.getLong();
      if (l1 < 0L) {
        return 1 + getVarLongLen(-l1);
      }
      if (l1 < 8L) {
        return 1;
      }
      return 1 + getVarLongLen(l1);
    case 7: 
      double d = paramValue.getDouble();
      if (d == 1.0D) {
        return 1;
      }
      long l4 = Double.doubleToLongBits(d);
      if (l4 == ValueDouble.ZERO_BITS) {
        return 1;
      }
      return 1 + getVarLongLen(Long.reverse(l4));
    case 8: 
      float f = paramValue.getFloat();
      if (f == 1.0F) {
        return 1;
      }
      m = Float.floatToIntBits(f);
      if (m == ValueFloat.ZERO_BITS) {
        return 1;
      }
      return 1 + getVarIntLen(Integer.reverse(m));
    case 13: 
      localObject1 = paramValue.getString();
      m = ((String)localObject1).length();
      if (m < 32) {
        return 1 + getStringWithoutLengthLen((String)localObject1, m);
      }
      return 1 + getStringLen((String)localObject1);
    case 14: 
    case 21: 
      return 1 + getStringLen(paramValue.getString());
    case 6: 
      localObject1 = paramValue.getBigDecimal();
      if (BigDecimal.ZERO.equals(localObject1)) {
        return 1;
      }
      if (BigDecimal.ONE.equals(localObject1)) {
        return 1;
      }
      m = ((BigDecimal)localObject1).scale();
      BigInteger localBigInteger = ((BigDecimal)localObject1).unscaledValue();
      int i2 = localBigInteger.bitLength();
      if (i2 <= 63)
      {
        if (m == 0) {
          return 1 + getVarLongLen(localBigInteger.longValue());
        }
        return 1 + getVarIntLen(m) + getVarLongLen(localBigInteger.longValue());
      }
      byte[] arrayOfByte = localBigInteger.toByteArray();
      return 1 + getVarIntLen(m) + getVarIntLen(arrayOfByte.length) + arrayOfByte.length;
    case 9: 
      if (SysProperties.STORE_LOCAL_TIME)
      {
        l2 = ((ValueTime)paramValue).getNanos();
        long l5 = l2 / 1000000L;
        l2 -= l5 * 1000000L;
        return 1 + getVarLongLen(l5) + getVarLongLen(l2);
      }
      return 1 + getVarLongLen(DateTimeUtils.getTimeLocalWithoutDst(paramValue.getTime()));
    case 10: 
      if (SysProperties.STORE_LOCAL_TIME)
      {
        l2 = ((ValueDate)paramValue).getDateValue();
        return 1 + getVarLongLen(l2);
      }
      l2 = DateTimeUtils.getTimeLocalWithoutDst(paramValue.getDate());
      return 1 + getVarLongLen(l2 / 60000L);
    case 11: 
      if (SysProperties.STORE_LOCAL_TIME)
      {
        localObject2 = (ValueTimestamp)paramValue;
        long l3 = ((ValueTimestamp)localObject2).getDateValue();
        long l6 = ((ValueTimestamp)localObject2).getTimeNanos();
        long l7 = l6 / 1000000L;
        l6 -= l7 * 1000000L;
        return 1 + getVarLongLen(l3) + getVarLongLen(l7) + getVarLongLen(l6);
      }
      localObject2 = paramValue.getTimestamp();
      return 1 + getVarLongLen(DateTimeUtils.getTimeLocalWithoutDst((Date)localObject2)) + getVarIntLen(((Timestamp)localObject2).getNanos() % 1000000);
    case 19: 
    case 22: 
      localObject2 = paramValue.getBytesNoCopy();
      return 1 + getVarIntLen(localObject2.length) + localObject2.length;
    case 12: 
      localObject2 = paramValue.getBytesNoCopy();
      int n = localObject2.length;
      if (n < 32) {
        return 1 + localObject2.length;
      }
      return 1 + getVarIntLen(localObject2.length) + localObject2.length;
    case 20: 
      return 17;
    case 15: 
    case 16: 
      int j = 1;
      Object localObject3;
      if ((paramValue instanceof ValueLob))
      {
        localObject3 = (ValueLob)paramValue;
        ((ValueLob)localObject3).convertToFileIfRequired(paramDataHandler);
        localObject4 = ((ValueLob)localObject3).getSmall();
        if (localObject4 == null)
        {
          i3 = -1;
          if (!((ValueLob)localObject3).isLinked()) {
            i3 = -2;
          }
          j += getVarIntLen(i3);
          j += getVarIntLen(((ValueLob)localObject3).getTableId());
          j += getVarIntLen(((ValueLob)localObject3).getObjectId());
          j += getVarLongLen(((ValueLob)localObject3).getPrecision());
          j++;
          if (i3 == -2) {
            j += getStringLen(((ValueLob)localObject3).getFileName());
          }
        }
        else
        {
          j += getVarIntLen(localObject4.length);
          j += localObject4.length;
        }
      }
      else
      {
        localObject3 = (ValueLobDb)paramValue;
        localObject4 = ((ValueLobDb)localObject3).getSmall();
        if (localObject4 == null)
        {
          j += getVarIntLen(-3);
          j += getVarIntLen(((ValueLobDb)localObject3).getTableId());
          j += getVarLongLen(((ValueLobDb)localObject3).getLobId());
          j += getVarLongLen(((ValueLobDb)localObject3).getPrecision());
        }
        else
        {
          j += getVarIntLen(localObject4.length);
          j += localObject4.length;
        }
      }
      return j;
    case 17: 
      Value[] arrayOfValue = ((ValueArray)paramValue).getList();
      int i1 = 1 + getVarIntLen(arrayOfValue.length);
      for (Value localValue1 : arrayOfValue) {
        i1 += getValueLen(localValue1, paramDataHandler);
      }
      return i1;
    case 18: 
      int k = 1;
      try
      {
        ResultSet localResultSet = ((ValueResultSet)paramValue).getResultSet();
        localResultSet.beforeFirst();
        localObject4 = localResultSet.getMetaData();
        i3 = ((ResultSetMetaData)localObject4).getColumnCount();
        k += getVarIntLen(i3);
        for (??? = 0; ??? < i3; ???++)
        {
          k += getStringLen(((ResultSetMetaData)localObject4).getColumnName(??? + 1));
          k += getVarIntLen(((ResultSetMetaData)localObject4).getColumnType(??? + 1));
          k += getVarIntLen(((ResultSetMetaData)localObject4).getPrecision(??? + 1));
          k += getVarIntLen(((ResultSetMetaData)localObject4).getScale(??? + 1));
        }
        while (localResultSet.next())
        {
          k++;
          for (??? = 0; ??? < i3; ???++)
          {
            int i5 = DataType.getValueTypeFromResultSet((ResultSetMetaData)localObject4, ??? + 1);
            Value localValue2 = DataType.readValue(null, localResultSet, ??? + 1, i5);
            k += getValueLen(localValue2, paramDataHandler);
          }
        }
        k++;
        localResultSet.beforeFirst();
      }
      catch (SQLException localSQLException)
      {
        throw DbException.convert(localSQLException);
      }
      return k;
    }
    throw DbException.throwInternalError("type=" + paramValue.getType());
  }
  
  public void setPos(int paramInt)
  {
    this.pos = paramInt;
  }
  
  public void writeShortInt(int paramInt)
  {
    byte[] arrayOfByte = this.data;
    arrayOfByte[(this.pos++)] = ((byte)(paramInt >> 8));
    arrayOfByte[(this.pos++)] = ((byte)paramInt);
  }
  
  public short readShortInt()
  {
    byte[] arrayOfByte = this.data;
    return (short)(((arrayOfByte[(this.pos++)] & 0xFF) << 8) + (arrayOfByte[(this.pos++)] & 0xFF));
  }
  
  public void truncate(int paramInt)
  {
    if (this.pos > paramInt)
    {
      byte[] arrayOfByte = new byte[paramInt];
      System.arraycopy(this.data, 0, arrayOfByte, 0, paramInt);
      this.pos = paramInt;
      this.data = arrayOfByte;
    }
  }
  
  private static int getVarIntLen(int paramInt)
  {
    if ((paramInt & 0xFFFFFF80) == 0) {
      return 1;
    }
    if ((paramInt & 0xC000) == 0) {
      return 2;
    }
    if ((paramInt & 0xFFE00000) == 0) {
      return 3;
    }
    if ((paramInt & 0xF0000000) == 0) {
      return 4;
    }
    return 5;
  }
  
  public void writeVarInt(int paramInt)
  {
    while ((paramInt & 0xFFFFFF80) != 0)
    {
      this.data[(this.pos++)] = ((byte)(0x80 | paramInt & 0x7F));
      paramInt >>>= 7;
    }
    this.data[(this.pos++)] = ((byte)paramInt);
  }
  
  public int readVarInt()
  {
    int i = this.data[this.pos];
    if (i >= 0)
    {
      this.pos += 1;
      return i;
    }
    return readVarIntRest(i);
  }
  
  private int readVarIntRest(int paramInt)
  {
    int i = paramInt & 0x7F;
    paramInt = this.data[(this.pos + 1)];
    if (paramInt >= 0)
    {
      this.pos += 2;
      return i | paramInt << 7;
    }
    i |= (paramInt & 0x7F) << 7;
    paramInt = this.data[(this.pos + 2)];
    if (paramInt >= 0)
    {
      this.pos += 3;
      return i | paramInt << 14;
    }
    i |= (paramInt & 0x7F) << 14;
    paramInt = this.data[(this.pos + 3)];
    if (paramInt >= 0)
    {
      this.pos += 4;
      return i | paramInt << 21;
    }
    i |= (paramInt & 0x7F) << 21 | this.data[(this.pos + 4)] << 28;
    this.pos += 5;
    return i;
  }
  
  public static int getVarLongLen(long paramLong)
  {
    int i = 1;
    for (;;)
    {
      paramLong >>>= 7;
      if (paramLong == 0L) {
        return i;
      }
      i++;
    }
  }
  
  public void writeVarLong(long paramLong)
  {
    while ((paramLong & 0xFFFFFFFFFFFFFF80) != 0L)
    {
      this.data[(this.pos++)] = ((byte)(int)(paramLong & 0x7F | 0x80));
      paramLong >>>= 7;
    }
    this.data[(this.pos++)] = ((byte)(int)paramLong);
  }
  
  public long readVarLong()
  {
    long l1 = this.data[(this.pos++)];
    if (l1 >= 0L) {
      return l1;
    }
    l1 &= 0x7F;
    for (int i = 7;; i += 7)
    {
      long l2 = this.data[(this.pos++)];
      l1 |= (l2 & 0x7F) << i;
      if (l2 >= 0L) {
        return l1;
      }
    }
  }
  
  public void checkCapacity(int paramInt)
  {
    if (this.pos + paramInt >= this.data.length) {
      expand(paramInt);
    }
  }
  
  private void expand(int paramInt)
  {
    byte[] arrayOfByte = DataUtils.newBytes((this.data.length + paramInt) * 2);
    
    System.arraycopy(this.data, 0, arrayOfByte, 0, this.data.length);
    this.data = arrayOfByte;
  }
  
  public void fillAligned()
  {
    int i = MathUtils.roundUpInt(this.pos + 2, 16);
    this.pos = i;
    if (this.data.length < i) {
      checkCapacity(i - this.data.length);
    }
  }
  
  public static void copyString(Reader paramReader, OutputStream paramOutputStream)
    throws IOException
  {
    char[] arrayOfChar = new char['က'];
    Data localData = new Data(null, new byte['　']);
    for (;;)
    {
      int i = paramReader.read(arrayOfChar);
      if (i < 0) {
        break;
      }
      localData.writeStringWithoutLength(arrayOfChar, i);
      paramOutputStream.write(localData.data, 0, localData.pos);
      localData.reset();
    }
  }
  
  public DataHandler getHandler()
  {
    return this.handler;
  }
}
