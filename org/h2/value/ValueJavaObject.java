package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.h2.engine.SysProperties;
import org.h2.store.DataHandler;
import org.h2.util.JdbcUtils;
import org.h2.util.Utils;

public class ValueJavaObject
  extends ValueBytes
{
  private static final ValueJavaObject EMPTY = new ValueJavaObject(Utils.EMPTY_BYTES, null);
  private final DataHandler dataHandler;
  
  protected ValueJavaObject(byte[] paramArrayOfByte, DataHandler paramDataHandler)
  {
    super(paramArrayOfByte);
    this.dataHandler = paramDataHandler;
  }
  
  public static ValueJavaObject getNoCopy(Object paramObject, byte[] paramArrayOfByte, DataHandler paramDataHandler)
  {
    if ((paramArrayOfByte != null) && (paramArrayOfByte.length == 0)) {
      return EMPTY;
    }
    Object localObject;
    if (SysProperties.serializeJavaObject)
    {
      if (paramArrayOfByte == null) {
        paramArrayOfByte = JdbcUtils.serialize(paramObject, paramDataHandler);
      }
      localObject = new ValueJavaObject(paramArrayOfByte, paramDataHandler);
    }
    else
    {
      localObject = new NotSerialized(paramObject, paramArrayOfByte, paramDataHandler);
    }
    if ((paramArrayOfByte == null) || (paramArrayOfByte.length > SysProperties.OBJECT_CACHE_MAX_PER_ELEMENT_SIZE)) {
      return (ValueJavaObject)localObject;
    }
    return (ValueJavaObject)Value.cache((Value)localObject);
  }
  
  public int getType()
  {
    return 19;
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    Object localObject = JdbcUtils.deserialize(getBytesNoCopy(), getDataHandler());
    paramPreparedStatement.setObject(paramInt, localObject, 2000);
  }
  
  private static class NotSerialized
    extends ValueJavaObject
  {
    private Object javaObject;
    private int displaySize = -1;
    
    NotSerialized(Object paramObject, byte[] paramArrayOfByte, DataHandler paramDataHandler)
    {
      super(paramDataHandler);
      this.javaObject = paramObject;
    }
    
    public void set(PreparedStatement paramPreparedStatement, int paramInt)
      throws SQLException
    {
      paramPreparedStatement.setObject(paramInt, getObject(), 2000);
    }
    
    public byte[] getBytesNoCopy()
    {
      if (this.value == null) {
        this.value = JdbcUtils.serialize(this.javaObject, null);
      }
      return this.value;
    }
    
    protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
    {
      Object localObject1 = getObject();
      Object localObject2 = paramValue.getObject();
      
      boolean bool1 = localObject1 instanceof Comparable;
      boolean bool2 = localObject2 instanceof Comparable;
      if ((bool1) && (bool2) && (Utils.haveCommonComparableSuperclass(localObject1.getClass(), localObject2.getClass())))
      {
        Comparable localComparable = (Comparable)localObject1;
        return localComparable.compareTo(localObject2);
      }
      if (localObject1.getClass() != localObject2.getClass())
      {
        if (bool1 != bool2) {
          return bool1 ? -1 : 1;
        }
        return localObject1.getClass().getName().compareTo(localObject2.getClass().getName());
      }
      int i = hashCode();
      int j = paramValue.hashCode();
      if (i == j)
      {
        if (localObject1.equals(localObject2)) {
          return 0;
        }
        return Utils.compareNotNullSigned(getBytesNoCopy(), paramValue.getBytesNoCopy());
      }
      return i > j ? 1 : -1;
    }
    
    public String getString()
    {
      String str = getObject().toString();
      if (this.displaySize == -1) {
        this.displaySize = str.length();
      }
      return str;
    }
    
    public long getPrecision()
    {
      return 0L;
    }
    
    public int hashCode()
    {
      if (this.hash == 0) {
        this.hash = getObject().hashCode();
      }
      return this.hash;
    }
    
    public Object getObject()
    {
      if (this.javaObject == null) {
        this.javaObject = JdbcUtils.deserialize(this.value, getDataHandler());
      }
      return this.javaObject;
    }
    
    public int getDisplaySize()
    {
      if (this.displaySize == -1) {
        this.displaySize = getString().length();
      }
      return this.displaySize;
    }
    
    public int getMemory()
    {
      if (this.value == null) {
        return DataType.getDataType(getType()).memory;
      }
      int i = super.getMemory();
      if (this.javaObject != null) {
        i *= 2;
      }
      return i;
    }
    
    public boolean equals(Object paramObject)
    {
      if (!(paramObject instanceof NotSerialized)) {
        return false;
      }
      return getObject().equals(((NotSerialized)paramObject).getObject());
    }
    
    public Value convertPrecision(long paramLong, boolean paramBoolean)
    {
      return this;
    }
  }
  
  protected DataHandler getDataHandler()
  {
    return this.dataHandler;
  }
}
