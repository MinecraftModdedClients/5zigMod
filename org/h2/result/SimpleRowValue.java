package org.h2.result;

import org.h2.value.Value;

public class SimpleRowValue
  implements SearchRow
{
  private long key;
  private int version;
  private int index;
  private final int virtualColumnCount;
  private Value data;
  
  public SimpleRowValue(int paramInt)
  {
    this.virtualColumnCount = paramInt;
  }
  
  public void setKeyAndVersion(SearchRow paramSearchRow)
  {
    this.key = paramSearchRow.getKey();
    this.version = paramSearchRow.getVersion();
  }
  
  public int getVersion()
  {
    return this.version;
  }
  
  public int getColumnCount()
  {
    return this.virtualColumnCount;
  }
  
  public long getKey()
  {
    return this.key;
  }
  
  public void setKey(long paramLong)
  {
    this.key = paramLong;
  }
  
  public Value getValue(int paramInt)
  {
    return paramInt == this.index ? this.data : null;
  }
  
  public void setValue(int paramInt, Value paramValue)
  {
    this.index = paramInt;
    this.data = paramValue;
  }
  
  public String toString()
  {
    return "( /* " + this.key + " */ " + (this.data == null ? "null" : this.data.getTraceSQL()) + " )";
  }
  
  public int getMemory()
  {
    return 24 + (this.data == null ? 0 : this.data.getMemory());
  }
}
