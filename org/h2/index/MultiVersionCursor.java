package org.h2.index;

import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.util.MathUtils;

public class MultiVersionCursor
  implements Cursor
{
  private final MultiVersionIndex index;
  private final Session session;
  private final Cursor baseCursor;
  private final Cursor deltaCursor;
  private final Object sync;
  private SearchRow baseRow;
  private Row deltaRow;
  private boolean onBase;
  private boolean end;
  private boolean needNewDelta;
  private boolean needNewBase;
  private boolean reverse;
  
  MultiVersionCursor(Session paramSession, MultiVersionIndex paramMultiVersionIndex, Cursor paramCursor1, Cursor paramCursor2, Object paramObject)
  {
    this.session = paramSession;
    this.index = paramMultiVersionIndex;
    this.baseCursor = paramCursor1;
    this.deltaCursor = paramCursor2;
    this.sync = paramObject;
    this.needNewDelta = true;
    this.needNewBase = true;
  }
  
  void loadCurrent()
  {
    synchronized (this.sync)
    {
      this.baseRow = this.baseCursor.getSearchRow();
      this.deltaRow = this.deltaCursor.get();
      this.needNewDelta = false;
      this.needNewBase = false;
    }
  }
  
  private void loadNext(boolean paramBoolean)
  {
    synchronized (this.sync)
    {
      if (paramBoolean)
      {
        if (step(this.baseCursor)) {
          this.baseRow = this.baseCursor.getSearchRow();
        } else {
          this.baseRow = null;
        }
      }
      else if (step(this.deltaCursor)) {
        this.deltaRow = this.deltaCursor.get();
      } else {
        this.deltaRow = null;
      }
    }
  }
  
  private boolean step(Cursor paramCursor)
  {
    return this.reverse ? paramCursor.previous() : paramCursor.next();
  }
  
  public Row get()
  {
    synchronized (this.sync)
    {
      if (this.end) {
        return null;
      }
      return this.onBase ? this.baseCursor.get() : this.deltaCursor.get();
    }
  }
  
  public SearchRow getSearchRow()
  {
    synchronized (this.sync)
    {
      if (this.end) {
        return null;
      }
      return this.onBase ? this.baseCursor.getSearchRow() : this.deltaCursor.getSearchRow();
    }
  }
  
  public boolean next()
  {
    synchronized (this.sync)
    {
      if ((SysProperties.CHECK) && (this.end)) {
        DbException.throwInternalError();
      }
      int k;
      for (;;)
      {
        if (this.needNewDelta)
        {
          loadNext(false);
          this.needNewDelta = false;
        }
        if (this.needNewBase)
        {
          loadNext(true);
          this.needNewBase = false;
        }
        if (this.deltaRow == null)
        {
          if (this.baseRow == null)
          {
            this.end = true;
            return false;
          }
          this.onBase = true;
          this.needNewBase = true;
          return true;
        }
        int i = this.deltaRow.getSessionId();
        int j = i == this.session.getId() ? 1 : 0;
        boolean bool = this.deltaRow.isDeleted();
        if ((j != 0) && (bool))
        {
          this.needNewDelta = true;
        }
        else
        {
          if (this.baseRow == null)
          {
            if (bool)
            {
              if (j != 0)
              {
                this.end = true;
                return false;
              }
              this.onBase = false;
              this.needNewDelta = true;
              return true;
            }
            DbException.throwInternalError();
          }
          k = this.index.compareRows(this.deltaRow, this.baseRow);
          if (k == 0)
          {
            long l1 = this.deltaRow.getKey();
            long l2 = this.baseRow.getKey();
            k = MathUtils.compareLong(l1, l2);
          }
          if (k != 0) {
            break;
          }
          if (bool)
          {
            if (j == 0) {
              break;
            }
            DbException.throwInternalError(); break;
          }
          if (j != 0)
          {
            this.onBase = false;
            this.needNewBase = true;
            this.needNewDelta = true;
            return true;
          }
          this.needNewBase = true;
          this.needNewDelta = true;
        }
      }
      if (k > 0)
      {
        this.onBase = true;
        this.needNewBase = true;
        return true;
      }
      this.onBase = false;
      this.needNewDelta = true;
      return true;
    }
  }
  
  public boolean previous()
  {
    this.reverse = true;
    try
    {
      return next();
    }
    finally
    {
      this.reverse = false;
    }
  }
}
