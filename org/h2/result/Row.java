package org.h2.result;

import org.h2.store.Data;
import org.h2.util.StatementBuilder;
import org.h2.value.Value;
import org.h2.value.ValueLong;

public class Row
  implements SearchRow
{
  public static final int MEMORY_CALCULATE = -1;
  public static final Row[] EMPTY_ARRAY = new Row[0];
  private long key;
  private final Value[] data;
  private int memory;
  private int version;
  private boolean deleted;
  private int sessionId;
  
  public Row(Value[] paramArrayOfValue, int paramInt)
  {
    this.data = paramArrayOfValue;
    this.memory = paramInt;
  }
  
  public Row getCopy()
  {
    Value[] arrayOfValue = new Value[this.data.length];
    System.arraycopy(this.data, 0, arrayOfValue, 0, this.data.length);
    Row localRow = new Row(arrayOfValue, this.memory);
    localRow.key = this.key;
    this.version += 1;
    localRow.sessionId = this.sessionId;
    return localRow;
  }
  
  public void setKeyAndVersion(SearchRow paramSearchRow)
  {
    setKey(paramSearchRow.getKey());
    setVersion(paramSearchRow.getVersion());
  }
  
  public int getVersion()
  {
    return this.version;
  }
  
  public void setVersion(int paramInt)
  {
    this.version = paramInt;
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
    return paramInt == -1 ? ValueLong.get(this.key) : this.data[paramInt];
  }
  
  public int getByteCount(Data paramData)
  {
    int i = 0;
    for (Value localValue : this.data) {
      i += paramData.getValueLen(localValue);
    }
    return i;
  }
  
  public void setValue(int paramInt, Value paramValue)
  {
    if (paramInt == -1) {
      this.key = paramValue.getLong();
    } else {
      this.data[paramInt] = paramValue;
    }
  }
  
  public boolean isEmpty()
  {
    return this.data == null;
  }
  
  public int getColumnCount()
  {
    return this.data.length;
  }
  
  public int getMemory()
  {
    if (this.memory != -1) {
      return this.memory;
    }
    int i = 40;
    if (this.data != null)
    {
      int j = this.data.length;
      i += 24 + j * 8;
      for (int k = 0; k < j; k++)
      {
        Value localValue = this.data[k];
        if (localValue != null) {
          i += localValue.getMemory();
        }
      }
    }
    this.memory = i;
    return i;
  }
  
  public String toString()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("( /* key:");
    localStatementBuilder.append(getKey());
    if (this.version != 0) {
      localStatementBuilder.append(" v:" + this.version);
    }
    if (isDeleted()) {
      localStatementBuilder.append(" deleted");
    }
    localStatementBuilder.append(" */ ");
    if (this.data != null) {
      for (Value localValue : this.data)
      {
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append(localValue == null ? "null" : localValue.getTraceSQL());
      }
    }
    return localStatementBuilder.append(')').toString();
  }
  
  public void setDeleted(boolean paramBoolean)
  {
    this.deleted = paramBoolean;
  }
  
  public void setSessionId(int paramInt)
  {
    this.sessionId = paramInt;
  }
  
  public int getSessionId()
  {
    return this.sessionId;
  }
  
  public void commit()
  {
    this.sessionId = 0;
  }
  
  public boolean isDeleted()
  {
    return this.deleted;
  }
  
  public Value[] getValueList()
  {
    return this.data;
  }
}
