package org.h2.index;

import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.table.Table;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class ViewCursor
  implements Cursor
{
  private final Table table;
  private final ViewIndex index;
  private final LocalResult result;
  private final SearchRow first;
  private final SearchRow last;
  private Row current;
  
  ViewCursor(ViewIndex paramViewIndex, LocalResult paramLocalResult, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    this.table = paramViewIndex.getTable();
    this.index = paramViewIndex;
    this.result = paramLocalResult;
    this.first = paramSearchRow1;
    this.last = paramSearchRow2;
  }
  
  public Row get()
  {
    return this.current;
  }
  
  public SearchRow getSearchRow()
  {
    return this.current;
  }
  
  public boolean next()
  {
    int i;
    do
    {
      do
      {
        boolean bool = this.result.next();
        if (!bool)
        {
          if (this.index.isRecursive()) {
            this.result.reset();
          } else {
            this.result.close();
          }
          this.current = null;
          return false;
        }
        this.current = this.table.getTemplateRow();
        Value[] arrayOfValue = this.result.currentRow();
        i = 0;
        for (int j = this.current.getColumnCount(); i < j; i++)
        {
          ValueNull localValueNull = i < arrayOfValue.length ? arrayOfValue[i] : ValueNull.INSTANCE;
          this.current.setValue(i, localValueNull);
        }
        if (this.first == null) {
          break;
        }
        i = this.index.compareRows(this.current, this.first);
      } while (i < 0);
      if (this.last == null) {
        break;
      }
      i = this.index.compareRows(this.current, this.last);
    } while (i > 0);
    return true;
  }
  
  public boolean previous()
  {
    throw DbException.throwInternalError();
  }
}
