package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.h2.message.DbException;
import org.h2.util.MathUtils;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class ValueUuid
  extends Value
{
  private static final int PRECISION = 16;
  private static final int DISPLAY_SIZE = 36;
  private final long high;
  private final long low;
  
  private ValueUuid(long paramLong1, long paramLong2)
  {
    this.high = paramLong1;
    this.low = paramLong2;
  }
  
  public int hashCode()
  {
    return (int)(this.high >>> 32 ^ this.high ^ this.low >>> 32 ^ this.low);
  }
  
  public static ValueUuid getNewRandom()
  {
    long l1 = MathUtils.secureRandomLong();
    long l2 = MathUtils.secureRandomLong();
    
    l1 = l1 & 0xFFFFFFFFFFFF0FFF | 0x4000;
    
    l2 = l2 & 0x3FFFFFFFFFFFFFFF | 0x8000000000000000;
    return new ValueUuid(l1, l2);
  }
  
  public static ValueUuid get(byte[] paramArrayOfByte)
  {
    if (paramArrayOfByte.length < 16) {
      return get(StringUtils.convertBytesToHex(paramArrayOfByte));
    }
    long l1 = Utils.readLong(paramArrayOfByte, 0);
    long l2 = Utils.readLong(paramArrayOfByte, 8);
    return (ValueUuid)Value.cache(new ValueUuid(l1, l2));
  }
  
  public static ValueUuid get(long paramLong1, long paramLong2)
  {
    return (ValueUuid)Value.cache(new ValueUuid(paramLong1, paramLong2));
  }
  
  public static ValueUuid get(String paramString)
  {
    long l1 = 0L;long l2 = 0L;
    int i = 0;int j = 0;
    for (int k = paramString.length(); i < k; i++)
    {
      int m = paramString.charAt(i);
      if ((m >= 48) && (m <= 57))
      {
        l1 = l1 << 4 | m - 48;
      }
      else if ((m >= 97) && (m <= 102))
      {
        l1 = l1 << 4 | m - 97 + 10;
      }
      else
      {
        if (m == 45) {
          continue;
        }
        if ((m >= 65) && (m <= 70))
        {
          l1 = l1 << 4 | m - 65 + 10;
        }
        else
        {
          if (m <= 32) {
            continue;
          }
          throw DbException.get(22018, paramString);
        }
      }
      if (j++ == 15)
      {
        l2 = l1;
        l1 = 0L;
      }
    }
    return (ValueUuid)Value.cache(new ValueUuid(l2, l1));
  }
  
  public String getSQL()
  {
    return StringUtils.quoteStringSQL(getString());
  }
  
  public int getType()
  {
    return 20;
  }
  
  public long getPrecision()
  {
    return 16L;
  }
  
  private static void appendHex(StringBuilder paramStringBuilder, long paramLong, int paramInt)
  {
    for (int i = paramInt * 8 - 4; i >= 0; i -= 8) {
      paramStringBuilder.append(Integer.toHexString((int)(paramLong >> i) & 0xF)).append(Integer.toHexString((int)(paramLong >> i - 4) & 0xF));
    }
  }
  
  public String getString()
  {
    StringBuilder localStringBuilder = new StringBuilder(36);
    appendHex(localStringBuilder, this.high >> 32, 4);
    localStringBuilder.append('-');
    appendHex(localStringBuilder, this.high >> 16, 2);
    localStringBuilder.append('-');
    appendHex(localStringBuilder, this.high, 2);
    localStringBuilder.append('-');
    appendHex(localStringBuilder, this.low >> 48, 2);
    localStringBuilder.append('-');
    appendHex(localStringBuilder, this.low, 6);
    return localStringBuilder.toString();
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    if (paramValue == this) {
      return 0;
    }
    ValueUuid localValueUuid = (ValueUuid)paramValue;
    if (this.high == localValueUuid.high) {
      return MathUtils.compareLong(this.low, localValueUuid.low);
    }
    return this.high > localValueUuid.high ? 1 : -1;
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof ValueUuid)) && (compareSecure((Value)paramObject, null) == 0);
  }
  
  public Object getObject()
  {
    return new UUID(this.high, this.low);
  }
  
  public byte[] getBytes()
  {
    byte[] arrayOfByte = new byte[16];
    for (int i = 0; i < 8; i++)
    {
      arrayOfByte[i] = ((byte)(int)(this.high >> 8 * (7 - i) & 0xFF));
      arrayOfByte[(8 + i)] = ((byte)(int)(this.low >> 8 * (7 - i) & 0xFF));
    }
    return arrayOfByte;
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setBytes(paramInt, getBytes());
  }
  
  public long getHigh()
  {
    return this.high;
  }
  
  public long getLow()
  {
    return this.low;
  }
  
  public int getDisplaySize()
  {
    return 36;
  }
}
