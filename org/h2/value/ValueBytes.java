package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import org.h2.engine.SysProperties;
import org.h2.util.MathUtils;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class ValueBytes
  extends Value
{
  private static final ValueBytes EMPTY = new ValueBytes(Utils.EMPTY_BYTES);
  protected byte[] value;
  protected int hash;
  
  protected ValueBytes(byte[] paramArrayOfByte)
  {
    this.value = paramArrayOfByte;
  }
  
  public static ValueBytes get(byte[] paramArrayOfByte)
  {
    if (paramArrayOfByte.length == 0) {
      return EMPTY;
    }
    paramArrayOfByte = Utils.cloneByteArray(paramArrayOfByte);
    return getNoCopy(paramArrayOfByte);
  }
  
  public static ValueBytes getNoCopy(byte[] paramArrayOfByte)
  {
    if (paramArrayOfByte.length == 0) {
      return EMPTY;
    }
    ValueBytes localValueBytes = new ValueBytes(paramArrayOfByte);
    if (paramArrayOfByte.length > SysProperties.OBJECT_CACHE_MAX_PER_ELEMENT_SIZE) {
      return localValueBytes;
    }
    return (ValueBytes)Value.cache(localValueBytes);
  }
  
  public int getType()
  {
    return 12;
  }
  
  public String getSQL()
  {
    return "X'" + StringUtils.convertBytesToHex(getBytesNoCopy()) + "'";
  }
  
  public byte[] getBytesNoCopy()
  {
    return this.value;
  }
  
  public byte[] getBytes()
  {
    return Utils.cloneByteArray(getBytesNoCopy());
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    byte[] arrayOfByte = ((ValueBytes)paramValue).value;
    if (paramCompareMode.isBinaryUnsigned()) {
      return Utils.compareNotNullUnsigned(this.value, arrayOfByte);
    }
    return Utils.compareNotNullSigned(this.value, arrayOfByte);
  }
  
  public String getString()
  {
    return StringUtils.convertBytesToHex(this.value);
  }
  
  public long getPrecision()
  {
    return this.value.length;
  }
  
  public int hashCode()
  {
    if (this.hash == 0) {
      this.hash = Utils.getByteArrayHash(this.value);
    }
    return this.hash;
  }
  
  public Object getObject()
  {
    return getBytes();
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setBytes(paramInt, this.value);
  }
  
  public int getDisplaySize()
  {
    return MathUtils.convertLongToInt(this.value.length * 2L);
  }
  
  public int getMemory()
  {
    return this.value.length + 24;
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof ValueBytes)) && (Arrays.equals(this.value, ((ValueBytes)paramObject).value));
  }
  
  public Value convertPrecision(long paramLong, boolean paramBoolean)
  {
    if (this.value.length <= paramLong) {
      return this;
    }
    int i = MathUtils.convertLongToInt(paramLong);
    byte[] arrayOfByte = new byte[i];
    System.arraycopy(this.value, 0, arrayOfByte, 0, i);
    return get(arrayOfByte);
  }
}
