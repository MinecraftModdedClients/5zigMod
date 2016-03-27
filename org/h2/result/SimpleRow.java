package org.h2.result;

import org.h2.util.StatementBuilder;
import org.h2.value.Value;

public class SimpleRow
  implements SearchRow
{
  private long key;
  private int version;
  private final Value[] data;
  private int memory;
  
  public SimpleRow(Value[] paramArrayOfValue)
  {
    this.data = paramArrayOfValue;
  }
  
  public int getColumnCount()
  {
    return this.data.length;
  }
  
  public long getKey()
  {
    return this.key;
  }
  
  public void setKey(long paramLong)
  {
    this.key = paramLong;
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
  
  public void setValue(int paramInt, Value paramValue)
  {
    this.data[paramInt] = paramValue;
  }
  
  public Value getValue(int paramInt)
  {
    return this.data[paramInt];
  }
  
  public String toString()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("( /* key:");
    localStatementBuilder.append(getKey());
    if (this.version != 0) {
      localStatementBuilder.append(" v:" + this.version);
    }
    localStatementBuilder.append(" */ ");
    for (Value localValue : this.data)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(localValue == null ? "null" : localValue.getTraceSQL());
    }
    return localStatementBuilder.append(')').toString();
  }
  
  public int getMemory()
  {
    if (this.memory == 0)
    {
      int i = this.data.length;
      this.memory = (24 + i * 8);
      for (int j = 0; j < i; j++)
      {
        Value localValue = this.data[j];
        if (localValue != null) {
          this.memory += localValue.getMemory();
        }
      }
    }
    return this.memory;
  }
}
