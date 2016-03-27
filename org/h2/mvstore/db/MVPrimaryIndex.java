package org.h2.mvstore.db;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.index.BaseIndex;
import org.h2.index.Cursor;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils.MapEntry;
import org.h2.mvstore.MVMap;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.store.LobStorageInterface;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;

public class MVPrimaryIndex
  extends BaseIndex
{
  static final ValueLong MIN = ValueLong.get(Long.MIN_VALUE);
  static final ValueLong MAX = ValueLong.get(Long.MAX_VALUE);
  static final ValueLong ZERO = ValueLong.get(0L);
  private final MVTable mvTable;
  private final String mapName;
  private TransactionStore.TransactionMap<Value, Value> dataMap;
  private long lastKey;
  private int mainIndexColumn = -1;
  
  public MVPrimaryIndex(Database paramDatabase, MVTable paramMVTable, int paramInt, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType)
  {
    this.mvTable = paramMVTable;
    initBaseIndex(paramMVTable, paramInt, paramMVTable.getName() + "_DATA", paramArrayOfIndexColumn, paramIndexType);
    int[] arrayOfInt = new int[paramArrayOfIndexColumn.length];
    for (int i = 0; i < paramArrayOfIndexColumn.length; i++) {
      arrayOfInt[i] = 0;
    }
    ValueDataType localValueDataType1 = new ValueDataType(null, null, null);
    ValueDataType localValueDataType2 = new ValueDataType(paramDatabase.getCompareMode(), paramDatabase, arrayOfInt);
    
    this.mapName = ("table." + getId());
    this.dataMap = this.mvTable.getTransaction(null).openMap(this.mapName, localValueDataType1, localValueDataType2);
    if (!paramMVTable.isPersistData()) {
      this.dataMap.map.setVolatile(true);
    }
    Value localValue = (Value)this.dataMap.lastKey();
    this.lastKey = (localValue == null ? 0L : localValue.getLong());
  }
  
  public String getCreateSQL()
  {
    return null;
  }
  
  public String getPlanSQL()
  {
    return this.table.getSQL() + ".tableScan";
  }
  
  public void setMainIndexColumn(int paramInt)
  {
    this.mainIndexColumn = paramInt;
  }
  
  public int getMainIndexColumn()
  {
    return this.mainIndexColumn;
  }
  
  public void close(Session paramSession) {}
  
  public void add(Session paramSession, Row paramRow)
  {
    if (this.mainIndexColumn == -1)
    {
      if (paramRow.getKey() == 0L) {
        paramRow.setKey(++this.lastKey);
      }
    }
    else
    {
      long l = paramRow.getValue(this.mainIndexColumn).getLong();
      paramRow.setKey(l);
    }
    Object localObject;
    if (this.mvTable.getContainsLargeObject())
    {
      int i = 0;
      for (int j = paramRow.getColumnCount(); i < j; i++)
      {
        localValue = paramRow.getValue(i);
        localObject = localValue.link(this.database, getId());
        if (((Value)localObject).isLinked()) {
          paramSession.unlinkAtCommitStop((Value)localObject);
        }
        if (localValue != localObject) {
          paramRow.setValue(i, (Value)localObject);
        }
      }
    }
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    ValueLong localValueLong = ValueLong.get(paramRow.getKey());
    Value localValue = (Value)localTransactionMap.getLatest(localValueLong);
    if (localValue != null)
    {
      localObject = "PRIMARY KEY ON " + this.table.getSQL();
      if ((this.mainIndexColumn >= 0) && (this.mainIndexColumn < this.indexColumns.length)) {
        localObject = (String)localObject + "(" + this.indexColumns[this.mainIndexColumn].getSQL() + ")";
      }
      DbException localDbException = DbException.get(23505, (String)localObject);
      localDbException.setSource(this);
      throw localDbException;
    }
    try
    {
      localTransactionMap.put(localValueLong, ValueArray.get(paramRow.getValueList()));
    }
    catch (IllegalStateException localIllegalStateException)
    {
      throw DbException.get(90131, localIllegalStateException, new String[] { this.table.getName() });
    }
    this.lastKey = Math.max(this.lastKey, paramRow.getKey());
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    if (this.mvTable.getContainsLargeObject())
    {
      int i = 0;
      for (int j = paramRow.getColumnCount(); i < j; i++)
      {
        Value localValue2 = paramRow.getValue(i);
        if (localValue2.isLinked()) {
          paramSession.unlinkAtCommit(localValue2);
        }
      }
    }
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    try
    {
      Value localValue1 = (Value)localTransactionMap.remove(ValueLong.get(paramRow.getKey()));
      if (localValue1 == null) {
        throw DbException.get(90112, getSQL() + ": " + paramRow.getKey());
      }
    }
    catch (IllegalStateException localIllegalStateException)
    {
      throw DbException.get(90131, localIllegalStateException, new String[] { this.table.getName() });
    }
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    Object localObject1;
    if (paramSearchRow1 == null)
    {
      localObject1 = MIN;
    }
    else if (this.mainIndexColumn < 0)
    {
      localObject1 = ValueLong.get(paramSearchRow1.getKey());
    }
    else
    {
      localObject3 = (ValueLong)paramSearchRow1.getValue(this.mainIndexColumn);
      if (localObject3 == null) {
        localObject1 = ValueLong.get(paramSearchRow1.getKey());
      } else {
        localObject1 = localObject3;
      }
    }
    Object localObject2;
    if (paramSearchRow2 == null)
    {
      localObject2 = MAX;
    }
    else if (this.mainIndexColumn < 0)
    {
      localObject2 = ValueLong.get(paramSearchRow2.getKey());
    }
    else
    {
      localObject3 = (ValueLong)paramSearchRow2.getValue(this.mainIndexColumn);
      if (localObject3 == null) {
        localObject2 = ValueLong.get(paramSearchRow2.getKey());
      } else {
        localObject2 = localObject3;
      }
    }
    Object localObject3 = getMap(paramSession);
    return new MVStoreCursor(((TransactionStore.TransactionMap)localObject3).entryIterator(localObject1), (ValueLong)localObject2);
  }
  
  public MVTable getTable()
  {
    return this.mvTable;
  }
  
  public Row getRow(Session paramSession, long paramLong)
  {
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    Value localValue = (Value)localTransactionMap.get(ValueLong.get(paramLong));
    ValueArray localValueArray = (ValueArray)localValue;
    Row localRow = new Row(localValueArray.getList(), 0);
    localRow.setKey(paramLong);
    return localRow;
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    try
    {
      long l = 10L * (this.dataMap.sizeAsLongMax() + 1000L);
      return l;
    }
    catch (IllegalStateException localIllegalStateException)
    {
      throw DbException.get(90007, localIllegalStateException, new String[0]);
    }
  }
  
  public int getColumnIndex(Column paramColumn)
  {
    return -1;
  }
  
  public void remove(Session paramSession)
  {
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    if (!localTransactionMap.isClosed())
    {
      TransactionStore.Transaction localTransaction = this.mvTable.getTransaction(paramSession);
      localTransaction.removeMap(localTransactionMap);
    }
  }
  
  public void truncate(Session paramSession)
  {
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    if (this.mvTable.getContainsLargeObject()) {
      this.database.getLobStorage().removeAllForTable(this.table.getId());
    }
    localTransactionMap.clear();
  }
  
  public boolean canGetFirstOrLast()
  {
    return true;
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    ValueLong localValueLong = (ValueLong)(paramBoolean ? (Value)localTransactionMap.firstKey() : (Value)localTransactionMap.lastKey());
    if (localValueLong == null) {
      return new MVStoreCursor(Collections.emptyList().iterator(), null);
    }
    Value localValue = (Value)localTransactionMap.get(localValueLong);
    DataUtils.MapEntry localMapEntry = new DataUtils.MapEntry(localValueLong, localValue);
    
    List localList = Arrays.asList(new Map.Entry[] { localMapEntry });
    MVStoreCursor localMVStoreCursor = new MVStoreCursor(localList.iterator(), localValueLong);
    localMVStoreCursor.next();
    return localMVStoreCursor;
  }
  
  public boolean needRebuild()
  {
    return false;
  }
  
  public long getRowCount(Session paramSession)
  {
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    return localTransactionMap.sizeAsLong();
  }
  
  public long getRowCountMax()
  {
    try
    {
      return this.dataMap.sizeAsLongMax();
    }
    catch (IllegalStateException localIllegalStateException)
    {
      throw DbException.get(90007, localIllegalStateException, new String[0]);
    }
  }
  
  public long getRowCountApproximation()
  {
    return getRowCountMax();
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
  
  public String getMapName()
  {
    return this.mapName;
  }
  
  public void checkRename() {}
  
  ValueLong getKey(SearchRow paramSearchRow, ValueLong paramValueLong1, ValueLong paramValueLong2)
  {
    if (paramSearchRow == null) {
      return paramValueLong1;
    }
    Value localValue = paramSearchRow.getValue(this.mainIndexColumn);
    if (localValue == null) {
      throw DbException.throwInternalError(paramSearchRow.toString());
    }
    if (localValue == ValueNull.INSTANCE) {
      return paramValueLong2;
    }
    return (ValueLong)localValue.convertTo(5);
  }
  
  Cursor find(Session paramSession, ValueLong paramValueLong1, ValueLong paramValueLong2)
  {
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    return new MVStoreCursor(localTransactionMap.entryIterator(paramValueLong1), paramValueLong2);
  }
  
  public boolean isRowIdIndex()
  {
    return true;
  }
  
  TransactionStore.TransactionMap<Value, Value> getMap(Session paramSession)
  {
    if (paramSession == null) {
      return this.dataMap;
    }
    TransactionStore.Transaction localTransaction = this.mvTable.getTransaction(paramSession);
    return this.dataMap.getInstance(localTransaction, Long.MAX_VALUE);
  }
  
  class MVStoreCursor
    implements Cursor
  {
    private final Iterator<Map.Entry<Value, Value>> it;
    private final ValueLong last;
    private Map.Entry<Value, Value> current;
    private Row row;
    
    public MVStoreCursor(ValueLong paramValueLong)
    {
      this.it = paramValueLong;
      ValueLong localValueLong;
      this.last = localValueLong;
    }
    
    public Row get()
    {
      if ((this.row == null) && 
        (this.current != null))
      {
        ValueArray localValueArray = (ValueArray)this.current.getValue();
        this.row = new Row(localValueArray.getList(), 0);
        this.row.setKey(((Value)this.current.getKey()).getLong());
      }
      return this.row;
    }
    
    public SearchRow getSearchRow()
    {
      return get();
    }
    
    public boolean next()
    {
      this.current = (this.it.hasNext() ? (Map.Entry)this.it.next() : null);
      if ((this.current != null) && (((Value)this.current.getKey()).getLong() > this.last.getLong())) {
        this.current = null;
      }
      this.row = null;
      return this.current != null;
    }
    
    public boolean previous()
    {
      throw DbException.getUnsupportedException("previous");
    }
  }
}
