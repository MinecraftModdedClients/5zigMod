package org.h2.mvstore.db;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.util.Iterator;
import java.util.List;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.index.BaseIndex;
import org.h2.index.Cursor;
import org.h2.index.IndexType;
import org.h2.index.SpatialIndex;
import org.h2.index.SpatialTreeIndex;
import org.h2.message.DbException;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.h2.mvstore.rtree.MVRTreeMap.Builder;
import org.h2.mvstore.rtree.MVRTreeMap.RTreeCursor;
import org.h2.mvstore.rtree.SpatialKey;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueGeometry;
import org.h2.value.ValueLong;

public class MVSpatialIndex
  extends BaseIndex
  implements SpatialIndex, MVIndex
{
  final MVTable mvTable;
  private final String mapName;
  private TransactionStore.TransactionMap<SpatialKey, Value> dataMap;
  private MVRTreeMap<TransactionStore.VersionedValue> spatialMap;
  
  public MVSpatialIndex(Database paramDatabase, MVTable paramMVTable, int paramInt, String paramString, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType)
  {
    if (paramArrayOfIndexColumn.length != 1) {
      throw DbException.getUnsupportedException("Can only index one column");
    }
    IndexColumn localIndexColumn = paramArrayOfIndexColumn[0];
    if ((localIndexColumn.sortType & 0x1) != 0) {
      throw DbException.getUnsupportedException("Cannot index in descending order");
    }
    if ((localIndexColumn.sortType & 0x2) != 0) {
      throw DbException.getUnsupportedException("Nulls first is not supported");
    }
    if ((localIndexColumn.sortType & 0x4) != 0) {
      throw DbException.getUnsupportedException("Nulls last is not supported");
    }
    if (localIndexColumn.column.getType() != 22) {
      throw DbException.getUnsupportedException("Spatial index on non-geometry column, " + localIndexColumn.column.getCreateSQL());
    }
    this.mvTable = paramMVTable;
    initBaseIndex(paramMVTable, paramInt, paramString, paramArrayOfIndexColumn, paramIndexType);
    if (!this.database.isStarting()) {
      checkIndexColumnTypes(paramArrayOfIndexColumn);
    }
    this.mapName = ("index." + getId());
    ValueDataType localValueDataType = new ValueDataType(null, null, null);
    TransactionStore.VersionedValueType localVersionedValueType = new TransactionStore.VersionedValueType(localValueDataType);
    MVRTreeMap.Builder localBuilder = new MVRTreeMap.Builder().valueType(localVersionedValueType);
    
    this.spatialMap = ((MVRTreeMap)paramDatabase.getMvStore().getStore().openMap(this.mapName, localBuilder));
    this.dataMap = this.mvTable.getTransaction(null).openMap(this.spatialMap);
  }
  
  public void addRowsToBuffer(List<Row> paramList, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  public void addBufferedRows(List<String> paramList)
  {
    throw DbException.throwInternalError();
  }
  
  public void close(Session paramSession) {}
  
  public void add(Session paramSession, Row paramRow)
  {
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    SpatialKey localSpatialKey1 = getKey(paramRow);
    Iterator localIterator;
    SpatialKey localSpatialKey2;
    if (this.indexType.isUnique())
    {
      MVRTreeMap.RTreeCursor localRTreeCursor1 = this.spatialMap.findContainedKeys(localSpatialKey1);
      localIterator = localTransactionMap.wrapIterator(localRTreeCursor1, false);
      while (localIterator.hasNext())
      {
        localSpatialKey2 = (SpatialKey)localIterator.next();
        if (localSpatialKey2.equalsIgnoringId(localSpatialKey1)) {
          throw getDuplicateKeyException(localSpatialKey1.toString());
        }
      }
    }
    try
    {
      localTransactionMap.put(localSpatialKey1, ValueLong.get(0L));
    }
    catch (IllegalStateException localIllegalStateException)
    {
      throw DbException.get(90131, localIllegalStateException, new String[] { this.table.getName() });
    }
    if (this.indexType.isUnique())
    {
      MVRTreeMap.RTreeCursor localRTreeCursor2 = this.spatialMap.findContainedKeys(localSpatialKey1);
      localIterator = localTransactionMap.wrapIterator(localRTreeCursor2, true);
      while (localIterator.hasNext())
      {
        localSpatialKey2 = (SpatialKey)localIterator.next();
        if (localSpatialKey2.equalsIgnoringId(localSpatialKey1)) {
          if (!localTransactionMap.isSameTransaction(localSpatialKey2))
          {
            localTransactionMap.remove(localSpatialKey1);
            if (localTransactionMap.get(localSpatialKey2) != null) {
              throw getDuplicateKeyException(localSpatialKey2.toString());
            }
            throw DbException.get(90131, this.table.getName());
          }
        }
      }
    }
  }
  
  private SpatialKey getKey(SearchRow paramSearchRow)
  {
    if (paramSearchRow == null) {
      return null;
    }
    Value localValue = paramSearchRow.getValue(this.columnIds[0]);
    Geometry localGeometry = ((ValueGeometry)localValue.convertTo(22)).getGeometryNoCopy();
    Envelope localEnvelope = localGeometry.getEnvelopeInternal();
    return new SpatialKey(paramSearchRow.getKey(), new float[] { (float)localEnvelope.getMinX(), (float)localEnvelope.getMaxX(), (float)localEnvelope.getMinY(), (float)localEnvelope.getMaxY() });
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    SpatialKey localSpatialKey = getKey(paramRow);
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    try
    {
      Value localValue = (Value)localTransactionMap.remove(localSpatialKey);
      if (localValue == null) {
        throw DbException.get(90112, getSQL() + ": " + paramRow.getKey());
      }
    }
    catch (IllegalStateException localIllegalStateException)
    {
      throw DbException.get(90131, localIllegalStateException, new String[] { this.table.getName() });
    }
  }
  
  public Cursor find(TableFilter paramTableFilter, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    return find(paramTableFilter.getSession());
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    return find(paramSession);
  }
  
  private Cursor find(Session paramSession)
  {
    Iterator localIterator1 = this.spatialMap.keyIterator(null);
    TransactionStore.TransactionMap localTransactionMap = getMap(paramSession);
    Iterator localIterator2 = localTransactionMap.wrapIterator(localIterator1, false);
    return new MVStoreCursor(paramSession, localIterator2);
  }
  
  public Cursor findByGeometry(TableFilter paramTableFilter, SearchRow paramSearchRow)
  {
    Session localSession = paramTableFilter.getSession();
    if (paramSearchRow == null) {
      return find(localSession);
    }
    MVRTreeMap.RTreeCursor localRTreeCursor = this.spatialMap.findIntersectingKeys(getEnvelope(paramSearchRow));
    
    TransactionStore.TransactionMap localTransactionMap = getMap(localSession);
    Iterator localIterator = localTransactionMap.wrapIterator(localRTreeCursor, false);
    return new MVStoreCursor(localSession, localIterator);
  }
  
  private SpatialKey getEnvelope(SearchRow paramSearchRow)
  {
    Value localValue = paramSearchRow.getValue(this.columnIds[0]);
    Geometry localGeometry = ((ValueGeometry)localValue.convertTo(22)).getGeometryNoCopy();
    Envelope localEnvelope = localGeometry.getEnvelopeInternal();
    return new SpatialKey(paramSearchRow.getKey(), new float[] { (float)localEnvelope.getMinX(), (float)localEnvelope.getMaxX(), (float)localEnvelope.getMinY(), (float)localEnvelope.getMaxY() });
  }
  
  SearchRow getRow(SpatialKey paramSpatialKey)
  {
    Row localRow = this.mvTable.getTemplateRow();
    localRow.setKey(paramSpatialKey.getId());
    return localRow;
  }
  
  public MVTable getTable()
  {
    return this.mvTable;
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    return getCostRangeIndex(paramArrayOfInt, this.table.getRowCountApproximation(), paramTableFilter, paramSortOrder);
  }
  
  protected long getCostRangeIndex(int[] paramArrayOfInt, long paramLong, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    return SpatialTreeIndex.getCostRangeIndex(paramArrayOfInt, paramLong, this.columns);
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
    if (!paramBoolean) {
      throw DbException.throwInternalError("Spatial Index can only be fetch in ascending order");
    }
    return find(paramSession);
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
  
  public void checkRename() {}
  
  TransactionStore.TransactionMap<SpatialKey, Value> getMap(Session paramSession)
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
    private final Iterator<SpatialKey> it;
    private SpatialKey current;
    private SearchRow searchRow;
    private Row row;
    
    public MVStoreCursor(Iterator<SpatialKey> paramIterator)
    {
      this.session = paramIterator;
      Iterator localIterator;
      this.it = localIterator;
    }
    
    public Row get()
    {
      if (this.row == null)
      {
        SearchRow localSearchRow = getSearchRow();
        if (localSearchRow != null) {
          this.row = MVSpatialIndex.this.mvTable.getRow(this.session, localSearchRow.getKey());
        }
      }
      return this.row;
    }
    
    public SearchRow getSearchRow()
    {
      if ((this.searchRow == null) && 
        (this.current != null)) {
        this.searchRow = MVSpatialIndex.this.getRow(this.current);
      }
      return this.searchRow;
    }
    
    public boolean next()
    {
      this.current = ((SpatialKey)this.it.next());
      this.searchRow = null;
      this.row = null;
      return this.current != null;
    }
    
    public boolean previous()
    {
      throw DbException.getUnsupportedException("previous");
    }
  }
}
