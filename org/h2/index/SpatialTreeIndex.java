package org.h2.index;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.util.Iterator;
import java.util.Set;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.db.MVTableEngine;
import org.h2.mvstore.db.MVTableEngine.Store;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.h2.mvstore.rtree.MVRTreeMap.Builder;
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

public class SpatialTreeIndex
  extends BaseIndex
  implements SpatialIndex
{
  private static final String MAP_PREFIX = "RTREE_";
  private final MVRTreeMap<Long> treeMap;
  private final MVStore store;
  private boolean closed;
  private boolean needRebuild;
  
  public SpatialTreeIndex(Table paramTable, int paramInt, String paramString, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType, boolean paramBoolean1, boolean paramBoolean2, Session paramSession)
  {
    if (paramIndexType.isUnique()) {
      throw DbException.getUnsupportedException("not unique");
    }
    if ((!paramBoolean1) && (!paramBoolean2)) {
      throw DbException.getUnsupportedException("Non persistent index called with create==false");
    }
    if (paramArrayOfIndexColumn.length > 1) {
      throw DbException.getUnsupportedException("can only do one column");
    }
    if ((paramArrayOfIndexColumn[0].sortType & 0x1) != 0) {
      throw DbException.getUnsupportedException("cannot do descending");
    }
    if ((paramArrayOfIndexColumn[0].sortType & 0x2) != 0) {
      throw DbException.getUnsupportedException("cannot do nulls first");
    }
    if ((paramArrayOfIndexColumn[0].sortType & 0x4) != 0) {
      throw DbException.getUnsupportedException("cannot do nulls last");
    }
    initBaseIndex(paramTable, paramInt, paramString, paramArrayOfIndexColumn, paramIndexType);
    this.needRebuild = paramBoolean2;
    this.table = paramTable;
    if ((!this.database.isStarting()) && 
      (paramArrayOfIndexColumn[0].column.getType() != 22)) {
      throw DbException.getUnsupportedException("spatial index on non-geometry column, " + paramArrayOfIndexColumn[0].column.getCreateSQL());
    }
    if (!paramBoolean1)
    {
      this.store = MVStore.open(null);
      this.treeMap = ((MVRTreeMap)this.store.openMap("spatialIndex", new MVRTreeMap.Builder()));
    }
    else
    {
      if (paramInt < 0) {
        throw DbException.getUnsupportedException("Persistent index with id<0");
      }
      MVTableEngine.init(paramSession.getDatabase());
      this.store = paramSession.getDatabase().getMvStore().getStore();
      
      this.treeMap = ((MVRTreeMap)this.store.openMap("RTREE_" + getId(), new MVRTreeMap.Builder()));
      if (this.treeMap.isEmpty()) {
        this.needRebuild = true;
      }
    }
  }
  
  public void close(Session paramSession)
  {
    this.store.close();
    this.closed = true;
  }
  
  public void add(Session paramSession, Row paramRow)
  {
    if (this.closed) {
      throw DbException.throwInternalError();
    }
    this.treeMap.add(getEnvelope(paramRow), Long.valueOf(paramRow.getKey()));
  }
  
  private SpatialKey getEnvelope(SearchRow paramSearchRow)
  {
    Value localValue = paramSearchRow.getValue(this.columnIds[0]);
    Geometry localGeometry = ((ValueGeometry)localValue.convertTo(22)).getGeometryNoCopy();
    Envelope localEnvelope = localGeometry.getEnvelopeInternal();
    return new SpatialKey(paramSearchRow.getKey(), new float[] { (float)localEnvelope.getMinX(), (float)localEnvelope.getMaxX(), (float)localEnvelope.getMinY(), (float)localEnvelope.getMaxY() });
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    if (this.closed) {
      throw DbException.throwInternalError();
    }
    if (!this.treeMap.remove(getEnvelope(paramRow), Long.valueOf(paramRow.getKey()))) {
      throw DbException.throwInternalError("row not found");
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
    return new SpatialCursor(this.treeMap.keySet().iterator(), this.table, paramSession);
  }
  
  public Cursor findByGeometry(TableFilter paramTableFilter, SearchRow paramSearchRow)
  {
    if (paramSearchRow == null) {
      return find(paramTableFilter.getSession());
    }
    return new SpatialCursor(this.treeMap.findIntersectingKeys(getEnvelope(paramSearchRow)), this.table, paramTableFilter.getSession());
  }
  
  protected long getCostRangeIndex(int[] paramArrayOfInt, long paramLong, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    return getCostRangeIndex(paramArrayOfInt, paramLong, this.columns);
  }
  
  public static long getCostRangeIndex(int[] paramArrayOfInt, long paramLong, Column[] paramArrayOfColumn)
  {
    paramLong += 1000L;
    long l = paramLong;
    if (paramArrayOfInt == null) {
      return l;
    }
    for (Column localColumn : paramArrayOfColumn)
    {
      int k = localColumn.getColumnId();
      int m = paramArrayOfInt[k];
      if ((m & 0x10) != 0) {
        l = 3L + paramLong / 4L;
      }
    }
    return 10L * l;
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    return getCostRangeIndex(paramArrayOfInt, this.table.getRowCountApproximation(), paramTableFilter, paramSortOrder);
  }
  
  public void remove(Session paramSession)
  {
    if (!this.treeMap.isClosed()) {
      this.store.removeMap(this.treeMap);
    }
  }
  
  public void truncate(Session paramSession)
  {
    this.treeMap.clear();
  }
  
  public void checkRename() {}
  
  public boolean needRebuild()
  {
    return this.needRebuild;
  }
  
  public boolean canGetFirstOrLast()
  {
    return true;
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    if (this.closed) {
      throw DbException.throwInternalError();
    }
    if (!paramBoolean) {
      throw DbException.throwInternalError("Spatial Index can only be fetch by ascending order");
    }
    return find(paramSession);
  }
  
  public long getRowCount(Session paramSession)
  {
    return this.treeMap.sizeAsLong();
  }
  
  public long getRowCountApproximation()
  {
    return this.treeMap.sizeAsLong();
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
  
  private static final class SpatialCursor
    implements Cursor
  {
    private final Iterator<SpatialKey> it;
    private SpatialKey current;
    private final Table table;
    private Session session;
    
    public SpatialCursor(Iterator<SpatialKey> paramIterator, Table paramTable, Session paramSession)
    {
      this.it = paramIterator;
      this.table = paramTable;
      this.session = paramSession;
    }
    
    public Row get()
    {
      return this.table.getRow(this.session, this.current.getId());
    }
    
    public SearchRow getSearchRow()
    {
      return get();
    }
    
    public boolean next()
    {
      if (!this.it.hasNext()) {
        return false;
      }
      this.current = ((SpatialKey)this.it.next());
      return true;
    }
    
    public boolean previous()
    {
      return false;
    }
  }
}
