package org.h2.mvstore;

import org.h2.mvstore.type.DataType;
import org.h2.mvstore.type.ObjectDataType;

public class MVMapConcurrent<K, V>
  extends MVMap<K, V>
{
  public MVMapConcurrent(DataType paramDataType1, DataType paramDataType2)
  {
    super(paramDataType1, paramDataType2);
  }
  
  public static class Builder<K, V>
    implements MVMap.MapBuilder<MVMapConcurrent<K, V>, K, V>
  {
    protected DataType keyType;
    protected DataType valueType;
    
    public Builder<K, V> keyType(DataType paramDataType)
    {
      this.keyType = paramDataType;
      return this;
    }
    
    public Builder<K, V> valueType(DataType paramDataType)
    {
      this.valueType = paramDataType;
      return this;
    }
    
    public MVMapConcurrent<K, V> create()
    {
      if (this.keyType == null) {
        this.keyType = new ObjectDataType();
      }
      if (this.valueType == null) {
        this.valueType = new ObjectDataType();
      }
      return new MVMapConcurrent(this.keyType, this.valueType);
    }
  }
}
