package org.h2.schema;

import java.math.BigInteger;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.table.Table;

public class Sequence
  extends SchemaObjectBase
{
  public static final int DEFAULT_CACHE_SIZE = 32;
  private long value;
  private long valueWithMargin;
  private long increment;
  private long cacheSize;
  private long minValue;
  private long maxValue;
  private boolean cycle;
  private boolean belongsToTable;
  
  public Sequence(Schema paramSchema, int paramInt, String paramString, long paramLong1, long paramLong2)
  {
    this(paramSchema, paramInt, paramString, Long.valueOf(paramLong1), Long.valueOf(paramLong2), null, null, null, false, true);
  }
  
  public Sequence(Schema paramSchema, int paramInt, String paramString, Long paramLong1, Long paramLong2, Long paramLong3, Long paramLong4, Long paramLong5, boolean paramBoolean1, boolean paramBoolean2)
  {
    initSchemaObjectBase(paramSchema, paramInt, paramString, "sequence");
    this.increment = (paramLong2 != null ? paramLong2.longValue() : 1L);
    
    this.minValue = (paramLong4 != null ? paramLong4.longValue() : getDefaultMinValue(paramLong1, this.increment));
    
    this.maxValue = (paramLong5 != null ? paramLong5.longValue() : getDefaultMaxValue(paramLong1, this.increment));
    
    this.value = (paramLong1 != null ? paramLong1.longValue() : getDefaultStartValue(this.increment));
    
    this.valueWithMargin = this.value;
    this.cacheSize = (paramLong3 != null ? Math.max(1L, paramLong3.longValue()) : 32L);
    
    this.cycle = paramBoolean1;
    this.belongsToTable = paramBoolean2;
    if (!isValid(this.value, this.minValue, this.maxValue, this.increment)) {
      throw DbException.get(90009, new String[] { paramString, String.valueOf(this.value), String.valueOf(this.minValue), String.valueOf(this.maxValue), String.valueOf(this.increment) });
    }
  }
  
  public synchronized void modify(Long paramLong1, Long paramLong2, Long paramLong3, Long paramLong4)
  {
    if (paramLong1 == null) {
      paramLong1 = Long.valueOf(this.value);
    }
    if (paramLong2 == null) {
      paramLong2 = Long.valueOf(this.minValue);
    }
    if (paramLong3 == null) {
      paramLong3 = Long.valueOf(this.maxValue);
    }
    if (paramLong4 == null) {
      paramLong4 = Long.valueOf(this.increment);
    }
    if (!isValid(paramLong1.longValue(), paramLong2.longValue(), paramLong3.longValue(), paramLong4.longValue())) {
      throw DbException.get(90009, new String[] { getName(), String.valueOf(paramLong1), String.valueOf(paramLong2), String.valueOf(paramLong3), String.valueOf(paramLong4) });
    }
    this.value = paramLong1.longValue();
    this.valueWithMargin = paramLong1.longValue();
    this.minValue = paramLong2.longValue();
    this.maxValue = paramLong3.longValue();
    this.increment = paramLong4.longValue();
  }
  
  private static boolean isValid(long paramLong1, long paramLong2, long paramLong3, long paramLong4)
  {
    return (paramLong2 <= paramLong1) && (paramLong3 >= paramLong1) && (paramLong3 > paramLong2) && (paramLong4 != 0L) && (BigInteger.valueOf(paramLong4).abs().compareTo(BigInteger.valueOf(paramLong3).subtract(BigInteger.valueOf(paramLong2))) < 0);
  }
  
  private static long getDefaultMinValue(Long paramLong, long paramLong1)
  {
    long l = paramLong1 >= 0L ? 1L : Long.MIN_VALUE;
    if ((paramLong != null) && (paramLong1 >= 0L) && (paramLong.longValue() < l)) {
      l = paramLong.longValue();
    }
    return l;
  }
  
  private static long getDefaultMaxValue(Long paramLong, long paramLong1)
  {
    long l = paramLong1 >= 0L ? Long.MAX_VALUE : -1L;
    if ((paramLong != null) && (paramLong1 < 0L) && (paramLong.longValue() > l)) {
      l = paramLong.longValue();
    }
    return l;
  }
  
  private long getDefaultStartValue(long paramLong)
  {
    return paramLong >= 0L ? this.minValue : this.maxValue;
  }
  
  public boolean getBelongsToTable()
  {
    return this.belongsToTable;
  }
  
  public long getIncrement()
  {
    return this.increment;
  }
  
  public long getMinValue()
  {
    return this.minValue;
  }
  
  public long getMaxValue()
  {
    return this.maxValue;
  }
  
  public boolean getCycle()
  {
    return this.cycle;
  }
  
  public void setCycle(boolean paramBoolean)
  {
    this.cycle = paramBoolean;
  }
  
  public String getDropSQL()
  {
    if (getBelongsToTable()) {
      return null;
    }
    return "DROP SEQUENCE IF EXISTS " + getSQL();
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  public synchronized String getCreateSQL()
  {
    StringBuilder localStringBuilder = new StringBuilder("CREATE SEQUENCE ");
    localStringBuilder.append(getSQL()).append(" START WITH ").append(this.value);
    if (this.increment != 1L) {
      localStringBuilder.append(" INCREMENT BY ").append(this.increment);
    }
    if (this.minValue != getDefaultMinValue(Long.valueOf(this.value), this.increment)) {
      localStringBuilder.append(" MINVALUE ").append(this.minValue);
    }
    if (this.maxValue != getDefaultMaxValue(Long.valueOf(this.value), this.increment)) {
      localStringBuilder.append(" MAXVALUE ").append(this.maxValue);
    }
    if (this.cycle) {
      localStringBuilder.append(" CYCLE");
    }
    if (this.cacheSize != 32L) {
      localStringBuilder.append(" CACHE ").append(this.cacheSize);
    }
    if (this.belongsToTable) {
      localStringBuilder.append(" BELONGS_TO_TABLE");
    }
    return localStringBuilder.toString();
  }
  
  public synchronized long getNext(Session paramSession)
  {
    int i = 0;
    if (((this.increment > 0L) && (this.value >= this.valueWithMargin)) || ((this.increment < 0L) && (this.value <= this.valueWithMargin)))
    {
      this.valueWithMargin += this.increment * this.cacheSize;
      i = 1;
    }
    if (((this.increment > 0L) && (this.value > this.maxValue)) || ((this.increment < 0L) && (this.value < this.minValue))) {
      if (this.cycle)
      {
        this.value = (this.increment > 0L ? this.minValue : this.maxValue);
        this.valueWithMargin = (this.value + this.increment * this.cacheSize);
        i = 1;
      }
      else
      {
        throw DbException.get(90006, getName());
      }
    }
    if (i != 0) {
      flush(paramSession);
    }
    long l = this.value;
    this.value += this.increment;
    return l;
  }
  
  public void flushWithoutMargin()
  {
    if (this.valueWithMargin != this.value)
    {
      this.valueWithMargin = this.value;
      flush(null);
    }
  }
  
  public synchronized void flush(Session paramSession)
  {
    if ((paramSession == null) || (!this.database.isSysTableLocked()))
    {
      Session localSession = this.database.getSystemSession();
      synchronized (localSession)
      {
        flushInternal(localSession);
        localSession.commit(false);
      }
    }
    else
    {
      synchronized (paramSession)
      {
        flushInternal(paramSession);
      }
    }
  }
  
  private void flushInternal(Session paramSession)
  {
    long l = this.value;
    try
    {
      this.value = this.valueWithMargin;
      if (!isTemporary()) {
        this.database.updateMeta(paramSession, this);
      }
    }
    finally
    {
      this.value = l;
    }
  }
  
  public void close()
  {
    flushWithoutMargin();
  }
  
  public int getType()
  {
    return 3;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    this.database.removeMeta(paramSession, getId());
    invalidate();
  }
  
  public void checkRename() {}
  
  public synchronized long getCurrentValue()
  {
    return this.value - this.increment;
  }
  
  public void setBelongsToTable(boolean paramBoolean)
  {
    this.belongsToTable = paramBoolean;
  }
  
  public void setCacheSize(long paramLong)
  {
    this.cacheSize = Math.max(1L, paramLong);
  }
  
  public long getCacheSize()
  {
    return this.cacheSize;
  }
}
