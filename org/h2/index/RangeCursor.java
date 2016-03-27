package org.h2.index;

import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.value.Value;
import org.h2.value.ValueLong;

class RangeCursor
  implements Cursor
{
  private boolean beforeFirst;
  private long current;
  private Row currentRow;
  private final long min;
  private final long max;
  
  RangeCursor(long paramLong1, long paramLong2)
  {
    this.min = paramLong1;
    this.max = paramLong2;
    this.beforeFirst = true;
  }
  
  public Row get()
  {
    return this.currentRow;
  }
  
  public SearchRow getSearchRow()
  {
    return this.currentRow;
  }
  
  public boolean next()
  {
    if (this.beforeFirst)
    {
      this.beforeFirst = false;
      this.current = this.min;
    }
    else
    {
      this.current += 1L;
    }
    this.currentRow = new Row(new Value[] { ValueLong.get(this.current) }, 1);
    return this.current <= this.max;
  }
  
  public boolean previous()
  {
    throw DbException.throwInternalError();
  }
}
