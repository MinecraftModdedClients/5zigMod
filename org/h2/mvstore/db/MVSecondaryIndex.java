package org.h2.mvstore.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.index.BaseIndex;
import org.h2.index.Cursor;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVMap.Builder;
import org.h2.mvstore.MVStore;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.New;
import org.h2.value.CompareMode;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;

public class MVSecondaryIndex
  extends BaseIndex
  implements MVIndex
{
  final MVTable mvTable;
  private final int keyColumns;
  private final String mapName;
  private TransactionStore.TransactionMap<Value, Value> dataMap;
  
  public MVSecondaryIndex(Database paramDatabase, MVTable paramMVTable, int paramInt, String paramString, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType)
  {
    this.mvTable = paramMVTable;
    initBaseIndex(paramMVTable, paramInt, paramString, paramArrayOfIndexColumn, paramIndexType);
    if (!this.database.isStarting()) {
      checkIndexColumnTypes(paramArrayOfIndexColumn);
    }
    this.keyColumns = (paramArrayOfIndexColumn.length + 1);
    this.mapName = ("index." + getId());
    int[] arrayOfInt = new int[this.keyColumns];
    for (int i = 0; i < paramArrayOfIndexColumn.length; i++) {
      arrayOfInt[i] = paramArrayOfIndexColumn[i].sortType;
    }
    arrayOfInt[(this.keyColumns - 1)] = 0;
    ValueDataType localValueDataType1 = new ValueDataType(paramDatabase.getCompareMode(), paramDatabase, arrayOfInt);
    
    ValueDataType localValueDataType2 = new ValueDataType(null, null, null);
    this.dataMap = this.mvTable.getTransaction(null).openMap(this.mapName, localValueDataType1, localValueDataType2);
    if (!localValueDataType1.equals(this.dataMap.getKeyType())) {
      throw DbException.throwInternalError("Incompatible key type");
    }
  }
  
  public void addRowsToBuffer(List<Row> paramList, String paramString)
  {
    MVMap localMVMap = openMap(paramString);
    for (Row localRow : paramList)
    {
      ValueArray localValueArray = convertToKey(localRow);
      localMVMap.put(localValueArray, ValueNull.INSTANCE);
    }
  }
  
  public void addBufferedRows(List<String> paramList)
  {
    ArrayList localArrayList = New.arrayList(paramList);
    final CompareMode localCompareMode = this.database.getCompareMode();
    
    TreeSet localTreeSet = new TreeSet();
    Object localObject2;
    Object localObject3;
    Object localObject4;
    for (int i = 0; i < paramList.size(); i++)
    {
      localObject2 = openMap((String)paramList.get(i));
      localObject3 = ((MVMap)localObject2).keyIterator(null);
      if (((Iterator)localObject3).hasNext())
      {
        localObject4 = new Comparable()
        {
          Value value;
          Iterator<Value> next;
          int sourceId;
          
          public int compareTo(1Source paramAnonymous1Source)
          {
            int i = this.value.compareTo(paramAnonymous1Source.value, localCompareMode);
            if (i == 0) {
              i = this.sourceId - paramAnonymous1Source.sourceId;
            }
            return i;
          }
        };
        ((1Source)localObject4).value = ((Value)((Iterator)localObject3).next());
        ((1Source)localObject4).next = ((Iterator)localObject3);
        ((1Source)localObject4).sourceId = i;
        localTreeSet.add(localObject4);
      }
    }
    try
    {
      for (;;)
      {
        localObject1 = (1Source)localTreeSet.first();
        localObject2 = ((1Source)localObject1).value;
        if (this.indexType.isUnique())
        {
          localObject3 = ((ValueArray)localObject2).getList();
          
          localObject3 = (Value[])Arrays.copyOf((Object[])localObject3, localObject3.length);
          localObject3[(this.keyColumns - 1)] = ValueLong.get(Long.MIN_VALUE);
          localObject4 = ValueArray.get((Value[])localObject3);
          ValueArray localValueArray = (ValueArray)this.dataMap.getLatestCeilingKey(localObject4);
          if (localValueArray != null)
          {
            SearchRow localSearchRow1 = convertToSearchRow(localValueArray);
            SearchRow localSearchRow2 = convertToSearchRow((ValueArray)localObject2);
            if ((compareRows(localSearchRow2, localSearchRow1) == 0) && 
              (!containsNullAndAllowMultipleNull(localSearchRow1))) {
              throw getDuplicateKeyException(localValueArray.toString());
            }
          }
        }
        this.dataMap.putCommitted(localObject2, ValueNull.INSTANCE);
        
        localObject3 = ((1Source)localObject1).next;
        if (!((Iterator)localObject3).hasNext())
        {
          localTreeSet.remove(localObject1);
          if (localTreeSet.size() == 0) {
            break;
          }
        }
        else
        {
          localObject4 = (Value)((Iterator)localObject3).next();
          localTreeSet.remove(localObject1);
          ((1Source)localObject1).value = ((Value)localObject4);
          localTreeSet.add(localObject1);
        }
      }
      for (localObject1 = localArrayList.iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (String)((Iterator)localObject1).next();
        localObject3 = openMap((String)localObject2);
        ((MVMap)localObject3).getStore().removeMap((MVMap)localObject3);
      }
    }
    finally
    {
      Object localObject1;
      for (String str : localArrayList)
      {
        MVMap localMVMap = openMap(str);
        localMVMap.getStore().removeMap(localMVMap);
      }
    }
  }
  
  private MVMap<Value, Value> openMap(String paramString)
  {
    int[] arrayOfInt = new int[this.keyColumns];
    for (int i = 0; i < this.indexColumns.length; i++) {
      arrayOfInt[i] = this.indexColumns[i].sortType;
    }
    arrayOfInt[(this.keyColumns - 1)] = 0;
    ValueDataType localValueDataType1 = new ValueDataType(this.database.getCompareMode(), this.database, arrayOfInt);
    
    ValueDataType localValueDataType2 = new ValueDataType(null, null, null);
    MVMap.Builder localBuilder = new MVMap.Builder().keyType(localValueDataType1).valueType(localValueDataType2);
    
    MVMap localMVMap = this.database.getMvStore().getStore().openMap(paramString, localBuilder);
    if (!localValueDataType1.equals(localMVMap.getKeyType())) {
      throw DbException.throwInternalError("Incompatible key type");
    }
    return localMVMap;
  }
  
  public void close(Session paramSession) {}
  
  public void add(Session paramSession, Row paramRow)
  {
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    ValueArray localValueArray1 = convertToKey(paramRow);
    ValueArray localValueArray2 = null;
    Object localObject;
    if (this.indexType.isUnique())
    {
      localValueArray2 = convertToKey(paramRow);
      localValueArray2.getList()[(this.keyColumns - 1)] = ValueLong.get(Long.MIN_VALUE);
      ValueArray localValueArray3 = (ValueArray)localTransactionMap.getLatestCeilingKey(localValueArray2);
      if (localValueArray3 != null)
      {
        localObject = convertToSearchRow(localValueArray3);
        if ((compareRows(paramRow, (SearchRow)localObject) == 0) && 
          (!containsNullAndAllowMultipleNull((SearchRow)localObject))) {
          throw getDuplicateKeyException(localValueArray3.toString());
        }
      }
    }
    try
    {
      localTransactionMap.put(localValueArray1, ValueNull.INSTANCE);
    }
    catch (IllegalStateException localIllegalStateException)
    {
      throw DbException.get(90131, localIllegalStateException, new String[] { this.table.getName() });
    }
    if (this.indexType.isUnique())
    {
      Iterator localIterator = localTransactionMap.keyIterator(localValueArray2, true);
      while (localIterator.hasNext())
      {
        localObject = (ValueArray)localIterator.next();
        SearchRow localSearchRow = convertToSearchRow((ValueArray)localObject);
        if (compareRows(paramRow, localSearchRow) == 0) {
          if ((!containsNullAndAllowMultipleNull(localSearchRow)) && 
          
            (!localTransactionMap.isSameTransaction(localObject)))
          {
            if (localTransactionMap.get(localObject) != null) {
              throw getDuplicateKeyException(((ValueArray)localObject).toString());
            }
            throw DbException.get(90131, this.table.getName());
          }
        }
      }
    }
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    ValueArray localValueArray = convertToKey(paramRow);
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    try
    {
      Value localValue = (Value)localTransactionMap.remove(localValueArray);
      if (localValue == null) {
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
    return find(paramSession, paramSearchRow1, false, paramSearchRow2);
  }
  
  private Cursor find(Session paramSession, SearchRow paramSearchRow1, boolean paramBoolean, SearchRow paramSearchRow2)
  {
    Object localObject = convertToKey(paramSearchRow1);
    if (localObject != null) {
      ((ValueArray)localObject).getList()[(this.keyColumns - 1)] = ValueLong.get(Long.MIN_VALUE);
    }
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    if ((paramBoolean) && (localObject != null))
    {
      int i = 1;
      ValueArray localValueArray;
      do
      {
        for (;;)
        {
          localValueArray = (ValueArray)localTransactionMap.relativeKey(localObject, i);
          if (localValueArray != null)
          {
            int j = 0;
            for (int k = 0; k < this.keyColumns - 1; k++)
            {
              int m = this.columnIds[k];
              Value localValue1 = paramSearchRow1.getValue(m);
              if (localValue1 == null) {
                break;
              }
              Value localValue2 = localValueArray.getList()[k];
              if (this.database.compare(localValue2, localValue1) > 0)
              {
                j = 1;
                break;
              }
            }
            if (j == 0)
            {
              i += i;
              localObject = localValueArray;
              continue;
            }
          }
          if (i <= 1) {
            break;
          }
          i /= 2;
        }
        if (localTransactionMap.get(localValueArray) != null) {
          break;
        }
        localObject = (ValueArray)localTransactionMap.higherKey(localObject);
      } while (localObject != null);
      break label220;
      localObject = localValueArray;
      label220:
      if (localObject == null) {
        return new MVStoreCursor(paramSession, Collections.emptyList().iterator(), null);
      }
    }
    return new MVStoreCursor(paramSession, localTransactionMap.keyIterator(localObject), paramSearchRow2);
  }
  
  private ValueArray convertToKey(SearchRow paramSearchRow)
  {
    if (paramSearchRow == null) {
      return null;
    }
    Value[] arrayOfValue = new Value[this.keyColumns];
    for (int i = 0; i < this.columns.length; i++)
    {
      Column localColumn = this.columns[i];
      int j = localColumn.getColumnId();
      Value localValue = paramSearchRow.getValue(j);
      if (localValue != null) {
        arrayOfValue[i] = localValue.convertTo(localColumn.getType());
      }
    }
    arrayOfValue[(this.keyColumns - 1)] = ValueLong.get(paramSearchRow.getKey());
    return ValueArray.get(arrayOfValue);
  }
  
  SearchRow convertToSearchRow(ValueArray paramValueArray)
  {
    Value[] arrayOfValue = paramValueArray.getList();
    Row localRow = this.mvTable.getTemplateRow();
    localRow.setKey(arrayOfValue[(arrayOfValue.length - 1)].getLong());
    Column[] arrayOfColumn = getColumns();
    for (int i = 0; i < arrayOfValue.length - 1; i++)
    {
      Column localColumn = arrayOfColumn[i];
      int j = localColumn.getColumnId();
      Value localValue = arrayOfValue[i];
      localRow.setValue(j, localValue);
    }
    return localRow;
  }
  
  public MVTable getTable()
  {
    return this.mvTable;
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    try
    {
      return 10L * getCostRangeIndex(paramArrayOfInt, this.dataMap.sizeAsLongMax(), paramTableFilter, paramSortOrder);
    }
    catch (IllegalStateException localIllegalStateException)
    {
      throw DbException.get(90007, localIllegalStateException, new String[0]);
    }
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
    localTransactionMap.clear();
  }
  
  public boolean canGetFirstOrLast()
  {
    return true;
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    Value localValue = paramBoolean ? (Value)localTransactionMap.firstKey() : (Value)localTransactionMap.lastKey();
    for (;;)
    {
      if (localValue == null) {
        return new MVStoreCursor(paramSession, Collections.emptyList().iterator(), null);
      }
      if (((ValueArray)localValue).getList()[0] != ValueNull.INSTANCE) {
        break;
      }
      localValue = paramBoolean ? (Value)localTransactionMap.higherKey(localValue) : (Value)localTransactionMap.lowerKey(localValue);
    }
    ArrayList localArrayList = New.arrayList();
    localArrayList.add(localValue);
    MVStoreCursor localMVStoreCursor = new MVStoreCursor(paramSession, localArrayList.iterator(), null);
    localMVStoreCursor.next();
    return localMVStoreCursor;
  }
  
  public boolean needRebuild()
  {
    try
    {
      return this.dataMap.sizeAsLongMax() == 0L;
    }
    catch (IllegalStateException localIllegalStateException)
    {
      throw DbException.get(90007, localIllegalStateException, new String[0]);
    }
  }
  
  public long getRowCount(Session paramSession)
  {
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    return localTransactionMap.sizeAsLong();
  }
  
  public long getRowCountApproximation()
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
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
  
  public boolean canFindNext()
  {
    return true;
  }
  
  public Cursor findNext(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    return find(paramSession, paramSearchRow1, true, paramSearchRow2);
  }
  
  public void checkRename() {}
  
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
    private final Session session;
    private final Iterator<Value> it;
    private final SearchRow last;
    private Value current;
    private SearchRow searchRow;
    private Row row;
    
    public MVStoreCursor(Iterator<Value> paramIterator, SearchRow paramSearchRow)
    {
      this.session = paramIterator;
      this.it = paramSearchRow;
      SearchRow localSearchRow;
      this.last = localSearchRow;
    }
    
    public Row get()
    {
      if (this.row == null)
      {
        SearchRow localSearchRow = getSearchRow();
        if (localSearchRow != null) {
          this.row = MVSecondaryIndex.this.mvTable.getRow(this.session, localSearchRow.getKey());
        }
      }
      return this.row;
    }
    
    public SearchRow getSearchRow()
    {
      if ((this.searchRow == null) && 
        (this.current != null)) {
        this.searchRow = MVSecondaryIndex.this.convertToSearchRow((ValueArray)this.current);
      }
      return this.searchRow;
    }
    
    public boolean next()
    {
      this.current = (this.it.hasNext() ? (Value)this.it.next() : null);
      this.searchRow = null;
      if ((this.current != null) && 
        (this.last != null) && (MVSecondaryIndex.this.compareRows(getSearchRow(), this.last) > 0))
      {
        this.searchRow = null;
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
