package org.h2.index;

import java.util.ArrayList;
import java.util.HashSet;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueGeometry;
import org.h2.value.ValueNull;

public class IndexCursor
  implements Cursor
{
  private Session session;
  private final TableFilter tableFilter;
  private Index index;
  private Table table;
  private IndexColumn[] indexColumns;
  private boolean alwaysFalse;
  private SearchRow start;
  private SearchRow end;
  private SearchRow intersects;
  private Cursor cursor;
  private Column inColumn;
  private int inListIndex;
  private Value[] inList;
  private ResultInterface inResult;
  private HashSet<Value> inResultTested;
  
  public IndexCursor(TableFilter paramTableFilter)
  {
    this.tableFilter = paramTableFilter;
  }
  
  public void setIndex(Index paramIndex)
  {
    this.index = paramIndex;
    this.table = paramIndex.getTable();
    Column[] arrayOfColumn = this.table.getColumns();
    this.indexColumns = new IndexColumn[arrayOfColumn.length];
    IndexColumn[] arrayOfIndexColumn = paramIndex.getIndexColumns();
    if (arrayOfIndexColumn != null)
    {
      int i = 0;
      for (int j = arrayOfColumn.length; i < j; i++)
      {
        int k = paramIndex.getColumnIndex(arrayOfColumn[i]);
        if (k >= 0) {
          this.indexColumns[i] = arrayOfIndexColumn[k];
        }
      }
    }
  }
  
  public void find(Session paramSession, ArrayList<IndexCondition> paramArrayList)
  {
    this.session = paramSession;
    this.alwaysFalse = false;
    this.start = (this.end = null);
    this.inList = null;
    this.inColumn = null;
    this.inResult = null;
    this.inResultTested = null;
    this.intersects = null;
    
    int i = 0;
    for (int j = paramArrayList.size(); i < j; i++)
    {
      IndexCondition localIndexCondition = (IndexCondition)paramArrayList.get(i);
      if (localIndexCondition.isAlwaysFalse())
      {
        this.alwaysFalse = true;
        break;
      }
      Column localColumn = localIndexCondition.getColumn();
      if (localIndexCondition.getCompareType() == 9)
      {
        if ((this.start == null) && (this.end == null) && 
          (canUseIndexForIn(localColumn)))
        {
          this.inColumn = localColumn;
          this.inList = localIndexCondition.getCurrentValueList(paramSession);
          this.inListIndex = 0;
        }
      }
      else if (localIndexCondition.getCompareType() == 10)
      {
        if ((this.start == null) && (this.end == null) && 
          (canUseIndexForIn(localColumn)))
        {
          this.inColumn = localColumn;
          this.inResult = localIndexCondition.getCurrentResult();
        }
      }
      else
      {
        Value localValue = localIndexCondition.getCurrentValue(paramSession);
        boolean bool1 = localIndexCondition.isStart();
        boolean bool2 = localIndexCondition.isEnd();
        boolean bool3 = localIndexCondition.isSpatialIntersects();
        int k = localColumn.getColumnId();
        if (k >= 0)
        {
          IndexColumn localIndexColumn = this.indexColumns[k];
          if ((localIndexColumn != null) && ((localIndexColumn.sortType & 0x1) != 0))
          {
            boolean bool4 = bool1;
            bool1 = bool2;
            bool2 = bool4;
          }
        }
        if (bool1) {
          this.start = getSearchRow(this.start, k, localValue, true);
        }
        if (bool2) {
          this.end = getSearchRow(this.end, k, localValue, false);
        }
        if (bool3) {
          this.intersects = getSpatialSearchRow(this.intersects, k, localValue);
        }
        if ((bool1) || (bool2))
        {
          this.inColumn = null;
          this.inList = null;
          this.inResult = null;
        }
        if ((!this.session.getDatabase().getSettings().optimizeIsNull) && 
          (bool1) && (bool2) && 
          (localValue == ValueNull.INSTANCE)) {
          this.alwaysFalse = true;
        }
      }
    }
    if (this.inColumn != null) {
      return;
    }
    if (!this.alwaysFalse) {
      if ((this.intersects != null) && ((this.index instanceof SpatialIndex))) {
        this.cursor = ((SpatialIndex)this.index).findByGeometry(this.tableFilter, this.intersects);
      } else {
        this.cursor = this.index.find(this.tableFilter, this.start, this.end);
      }
    }
  }
  
  private boolean canUseIndexForIn(Column paramColumn)
  {
    if (this.inColumn != null) {
      return false;
    }
    IndexColumn[] arrayOfIndexColumn = this.index.getIndexColumns();
    if (arrayOfIndexColumn == null) {
      return true;
    }
    IndexColumn localIndexColumn = arrayOfIndexColumn[0];
    return (localIndexColumn == null) || (localIndexColumn.column == paramColumn);
  }
  
  private SearchRow getSpatialSearchRow(SearchRow paramSearchRow, int paramInt, Value paramValue)
  {
    if (paramSearchRow == null)
    {
      paramSearchRow = this.table.getTemplateRow();
    }
    else if (paramSearchRow.getValue(paramInt) != null)
    {
      ValueGeometry localValueGeometry = (ValueGeometry)paramSearchRow.getValue(paramInt).convertTo(22);
      
      paramValue = ((ValueGeometry)paramValue.convertTo(22)).getEnvelopeUnion(localValueGeometry);
    }
    if (paramInt < 0) {
      paramSearchRow.setKey(paramValue.getLong());
    } else {
      paramSearchRow.setValue(paramInt, paramValue);
    }
    return paramSearchRow;
  }
  
  private SearchRow getSearchRow(SearchRow paramSearchRow, int paramInt, Value paramValue, boolean paramBoolean)
  {
    if (paramSearchRow == null) {
      paramSearchRow = this.table.getTemplateRow();
    } else {
      paramValue = getMax(paramSearchRow.getValue(paramInt), paramValue, paramBoolean);
    }
    if (paramInt < 0) {
      paramSearchRow.setKey(paramValue.getLong());
    } else {
      paramSearchRow.setValue(paramInt, paramValue);
    }
    return paramSearchRow;
  }
  
  private Value getMax(Value paramValue1, Value paramValue2, boolean paramBoolean)
  {
    if (paramValue1 == null) {
      return paramValue2;
    }
    if (paramValue2 == null) {
      return paramValue1;
    }
    if (this.session.getDatabase().getSettings().optimizeIsNull)
    {
      if (paramValue1 == ValueNull.INSTANCE) {
        return paramValue2;
      }
      if (paramValue2 == ValueNull.INSTANCE) {
        return paramValue1;
      }
    }
    int i = paramValue1.compareTo(paramValue2, this.table.getDatabase().getCompareMode());
    if (i == 0) {
      return paramValue1;
    }
    if (((paramValue1 == ValueNull.INSTANCE) || (paramValue2 == ValueNull.INSTANCE)) && 
      (this.session.getDatabase().getSettings().optimizeIsNull)) {
      return null;
    }
    if (!paramBoolean) {
      i = -i;
    }
    return i > 0 ? paramValue1 : paramValue2;
  }
  
  public boolean isAlwaysFalse()
  {
    return this.alwaysFalse;
  }
  
  public Row get()
  {
    if (this.cursor == null) {
      return null;
    }
    return this.cursor.get();
  }
  
  public SearchRow getSearchRow()
  {
    return this.cursor.getSearchRow();
  }
  
  public boolean next()
  {
    for (;;)
    {
      if (this.cursor == null)
      {
        nextCursor();
        if (this.cursor == null) {
          return false;
        }
      }
      if (this.cursor.next()) {
        return true;
      }
      this.cursor = null;
    }
  }
  
  private void nextCursor()
  {
    Value localValue;
    if (this.inList != null) {
      while (this.inListIndex < this.inList.length)
      {
        localValue = this.inList[(this.inListIndex++)];
        if (localValue != ValueNull.INSTANCE)
        {
          find(localValue);
          break;
        }
      }
    }
    if (this.inResult != null) {
      while (this.inResult.next())
      {
        localValue = this.inResult.currentRow()[0];
        if (localValue != ValueNull.INSTANCE)
        {
          localValue = this.inColumn.convert(localValue);
          if (this.inResultTested == null) {
            this.inResultTested = new HashSet();
          }
          if (this.inResultTested.add(localValue))
          {
            find(localValue);
            break;
          }
        }
      }
    }
  }
  
  private void find(Value paramValue)
  {
    paramValue = this.inColumn.convert(paramValue);
    int i = this.inColumn.getColumnId();
    if (this.start == null) {
      this.start = this.table.getTemplateRow();
    }
    this.start.setValue(i, paramValue);
    this.cursor = this.index.find(this.tableFilter, this.start, this.start);
  }
  
  public boolean previous()
  {
    throw DbException.throwInternalError();
  }
}
