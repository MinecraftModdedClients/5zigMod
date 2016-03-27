package org.h2.table;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.h2.command.ddl.Analyze;
import org.h2.command.ddl.CreateTableData;
import org.h2.constraint.Constraint;
import org.h2.constraint.ConstraintReferential;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.index.Cursor;
import org.h2.index.HashIndex;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.index.MultiVersionIndex;
import org.h2.index.NonUniqueHashIndex;
import org.h2.index.PageBtreeIndex;
import org.h2.index.PageDataIndex;
import org.h2.index.PageDelegateIndex;
import org.h2.index.ScanIndex;
import org.h2.index.SpatialTreeIndex;
import org.h2.index.TreeIndex;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.store.LobStorageInterface;
import org.h2.store.PageStore;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.value.CompareMode;
import org.h2.value.DataType;
import org.h2.value.Value;

public class RegularTable
  extends TableBase
{
  private Index scanIndex;
  private long rowCount;
  private volatile Session lockExclusiveSession;
  private HashSet<Session> lockSharedSessions = New.hashSet();
  private final ArrayDeque<Session> waitingSessions = new ArrayDeque();
  private final Trace traceLock;
  private final ArrayList<Index> indexes = New.arrayList();
  private long lastModificationId;
  private boolean containsLargeObject;
  private final PageDataIndex mainIndex;
  private int changesSinceAnalyze;
  private int nextAnalyze;
  private Column rowIdColumn;
  
  public RegularTable(CreateTableData paramCreateTableData)
  {
    super(paramCreateTableData);
    this.nextAnalyze = this.database.getSettings().analyzeAuto;
    this.isHidden = paramCreateTableData.isHidden;
    for (Column localColumn : getColumns()) {
      if (DataType.isLargeObject(localColumn.getType())) {
        this.containsLargeObject = true;
      }
    }
    if ((paramCreateTableData.persistData) && (this.database.isPersistent()))
    {
      this.mainIndex = new PageDataIndex(this, paramCreateTableData.id, IndexColumn.wrap(getColumns()), IndexType.createScan(paramCreateTableData.persistData), paramCreateTableData.create, paramCreateTableData.session);
      
      this.scanIndex = this.mainIndex;
    }
    else
    {
      this.mainIndex = null;
      this.scanIndex = new ScanIndex(this, paramCreateTableData.id, IndexColumn.wrap(getColumns()), IndexType.createScan(paramCreateTableData.persistData));
    }
    this.indexes.add(this.scanIndex);
    this.traceLock = this.database.getTrace("lock");
  }
  
  public void close(Session paramSession)
  {
    for (Index localIndex : this.indexes) {
      localIndex.close(paramSession);
    }
  }
  
  public Row getRow(Session paramSession, long paramLong)
  {
    return this.scanIndex.getRow(paramSession, paramLong);
  }
  
  public void addRow(Session paramSession, Row paramRow)
  {
    this.lastModificationId = this.database.getNextModificationDataId();
    if (this.database.isMultiVersion()) {
      paramRow.setSessionId(paramSession.getId());
    }
    int i = 0;
    try
    {
      for (int j = this.indexes.size(); i < j; i++)
      {
        localIndex1 = (Index)this.indexes.get(i);
        localIndex1.add(paramSession, paramRow);
        checkRowCount(paramSession, localIndex1, 1);
      }
      this.rowCount += 1L;
    }
    catch (Throwable localThrowable)
    {
      try
      {
        for (;;)
        {
          i--;
          if (i < 0) {
            break;
          }
          Index localIndex1 = (Index)this.indexes.get(i);
          localIndex1.remove(paramSession, paramRow);
          checkRowCount(paramSession, localIndex1, 0);
        }
      }
      catch (DbException localDbException1)
      {
        this.trace.error(localDbException1, "could not undo operation");
        throw localDbException1;
      }
      DbException localDbException2 = DbException.convert(localThrowable);
      if (localDbException2.getErrorCode() == 23505) {
        for (int k = 0; k < this.indexes.size(); k++)
        {
          Index localIndex2 = (Index)this.indexes.get(k);
          if ((localIndex2.getIndexType().isUnique()) && ((localIndex2 instanceof MultiVersionIndex)))
          {
            MultiVersionIndex localMultiVersionIndex = (MultiVersionIndex)localIndex2;
            if (localMultiVersionIndex.isUncommittedFromOtherSession(paramSession, paramRow)) {
              throw DbException.get(90131, localIndex2.getName());
            }
          }
        }
      }
      throw localDbException2;
    }
    analyzeIfRequired(paramSession);
  }
  
  public void commit(short paramShort, Row paramRow)
  {
    this.lastModificationId = this.database.getNextModificationDataId();
    int i = 0;
    for (int j = this.indexes.size(); i < j; i++)
    {
      Index localIndex = (Index)this.indexes.get(i);
      localIndex.commit(paramShort, paramRow);
    }
  }
  
  private void checkRowCount(Session paramSession, Index paramIndex, int paramInt)
  {
    if ((SysProperties.CHECK) && (!this.database.isMultiVersion()) && 
      (!(paramIndex instanceof PageDelegateIndex)))
    {
      long l = paramIndex.getRowCount(paramSession);
      if (l != this.rowCount + paramInt) {
        DbException.throwInternalError("rowCount expected " + (this.rowCount + paramInt) + " got " + l + " " + getName() + "." + paramIndex.getName());
      }
    }
  }
  
  public Index getScanIndex(Session paramSession)
  {
    return (Index)this.indexes.get(0);
  }
  
  public Index getUniqueIndex()
  {
    for (Index localIndex : this.indexes) {
      if (localIndex.getIndexType().isUnique()) {
        return localIndex;
      }
    }
    return null;
  }
  
  public ArrayList<Index> getIndexes()
  {
    return this.indexes;
  }
  
  public Index addIndex(Session paramSession, String paramString1, int paramInt, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType, boolean paramBoolean, String paramString2)
  {
    if (paramIndexType.isPrimaryKey()) {
      for (IndexColumn localIndexColumn : paramArrayOfIndexColumn)
      {
        Column localColumn = localIndexColumn.column;
        if (localColumn.isNullable()) {
          throw DbException.get(90023, localColumn.getName());
        }
        localColumn.setPrimaryKey(true);
      }
    }
    int i = (isTemporary()) && (!isGlobalTemporary()) ? 1 : 0;
    if (i == 0) {
      this.database.lockMeta(paramSession);
    }
    Object localObject;
    if ((isPersistIndexes()) && (paramIndexType.isPersistent()))
    {
      if ((this.database.isStarting()) && (this.database.getPageStore().getRootPageId(paramInt) != 0)) {
        ??? = -1;
      } else if ((!this.database.isStarting()) && (this.mainIndex.getRowCount(paramSession) != 0L)) {
        ??? = -1;
      } else {
        ??? = getMainIndexColumn(paramIndexType, paramArrayOfIndexColumn);
      }
      if (??? != -1)
      {
        this.mainIndex.setMainIndexColumn(???);
        localObject = new PageDelegateIndex(this, paramInt, paramString1, paramIndexType, this.mainIndex, paramBoolean, paramSession);
      }
      else if (paramIndexType.isSpatial())
      {
        localObject = new SpatialTreeIndex(this, paramInt, paramString1, paramArrayOfIndexColumn, paramIndexType, true, paramBoolean, paramSession);
      }
      else
      {
        localObject = new PageBtreeIndex(this, paramInt, paramString1, paramArrayOfIndexColumn, paramIndexType, paramBoolean, paramSession);
      }
    }
    else if (paramIndexType.isHash())
    {
      if (paramArrayOfIndexColumn.length != 1) {
        throw DbException.getUnsupportedException("hash indexes may index only one column");
      }
      if (paramIndexType.isUnique()) {
        localObject = new HashIndex(this, paramInt, paramString1, paramArrayOfIndexColumn, paramIndexType);
      } else {
        localObject = new NonUniqueHashIndex(this, paramInt, paramString1, paramArrayOfIndexColumn, paramIndexType);
      }
    }
    else if (paramIndexType.isSpatial())
    {
      localObject = new SpatialTreeIndex(this, paramInt, paramString1, paramArrayOfIndexColumn, paramIndexType, false, true, paramSession);
    }
    else
    {
      localObject = new TreeIndex(this, paramInt, paramString1, paramArrayOfIndexColumn, paramIndexType);
    }
    if (this.database.isMultiVersion()) {
      localObject = new MultiVersionIndex((Index)localObject, this);
    }
    if ((((Index)localObject).needRebuild()) && (this.rowCount > 0L)) {
      try
      {
        Index localIndex = getScanIndex(paramSession);
        long l1 = localIndex.getRowCount(paramSession);
        long l2 = l1;
        Cursor localCursor = localIndex.find(paramSession, null, null);
        long l3 = 0L;
        int m = (int)Math.min(this.rowCount, this.database.getMaxMemoryRows());
        ArrayList localArrayList = New.arrayList(m);
        String str = getName() + ":" + ((Index)localObject).getName();
        int n = MathUtils.convertLongToInt(l2);
        while (localCursor.next())
        {
          this.database.setProgress(1, str, MathUtils.convertLongToInt(l3++), n);
          
          Row localRow = localCursor.get();
          localArrayList.add(localRow);
          if (localArrayList.size() >= m) {
            addRowsToIndex(paramSession, localArrayList, (Index)localObject);
          }
          l1 -= 1L;
        }
        addRowsToIndex(paramSession, localArrayList, (Index)localObject);
        if ((SysProperties.CHECK) && (l1 != 0L)) {
          DbException.throwInternalError("rowcount remaining=" + l1 + " " + getName());
        }
      }
      catch (DbException localDbException1)
      {
        getSchema().freeUniqueName(paramString1);
        try
        {
          ((Index)localObject).remove(paramSession);
        }
        catch (DbException localDbException2)
        {
          this.trace.error(localDbException2, "could not remove index");
          throw localDbException2;
        }
        throw localDbException1;
      }
    }
    ((Index)localObject).setTemporary(isTemporary());
    if (((Index)localObject).getCreateSQL() != null)
    {
      ((Index)localObject).setComment(paramString2);
      if (i != 0) {
        paramSession.addLocalTempTableIndex((Index)localObject);
      } else {
        this.database.addSchemaObject(paramSession, (SchemaObject)localObject);
      }
    }
    this.indexes.add(localObject);
    setModified();
    return (Index)localObject;
  }
  
  private int getMainIndexColumn(IndexType paramIndexType, IndexColumn[] paramArrayOfIndexColumn)
  {
    if (this.mainIndex.getMainIndexColumn() != -1) {
      return -1;
    }
    if ((!paramIndexType.isPrimaryKey()) || (paramArrayOfIndexColumn.length != 1)) {
      return -1;
    }
    IndexColumn localIndexColumn = paramArrayOfIndexColumn[0];
    if (localIndexColumn.sortType != 0) {
      return -1;
    }
    switch (localIndexColumn.column.getType())
    {
    case 2: 
    case 3: 
    case 4: 
    case 5: 
      break;
    default: 
      return -1;
    }
    return localIndexColumn.column.getColumnId();
  }
  
  public boolean canGetRowCount()
  {
    return true;
  }
  
  private static void addRowsToIndex(Session paramSession, ArrayList<Row> paramArrayList, Index paramIndex)
  {
    Index localIndex = paramIndex;
    Collections.sort(paramArrayList, new Comparator()
    {
      public int compare(Row paramAnonymousRow1, Row paramAnonymousRow2)
      {
        return this.val$idx.compareRows(paramAnonymousRow1, paramAnonymousRow2);
      }
    });
    for (Row localRow : paramArrayList) {
      paramIndex.add(paramSession, localRow);
    }
    paramArrayList.clear();
  }
  
  public boolean canDrop()
  {
    return true;
  }
  
  public long getRowCount(Session paramSession)
  {
    if (this.database.isMultiVersion()) {
      return getScanIndex(paramSession).getRowCount(paramSession);
    }
    return this.rowCount;
  }
  
  public void removeRow(Session paramSession, Row paramRow)
  {
    if (this.database.isMultiVersion())
    {
      if (paramRow.isDeleted()) {
        throw DbException.get(90131, getName());
      }
      i = paramRow.getSessionId();
      int j = paramSession.getId();
      if (i == 0) {
        paramRow.setSessionId(j);
      } else if (i != j) {
        throw DbException.get(90131, getName());
      }
    }
    this.lastModificationId = this.database.getNextModificationDataId();
    int i = this.indexes.size() - 1;
    try
    {
      for (; i >= 0; i--)
      {
        Index localIndex1 = (Index)this.indexes.get(i);
        localIndex1.remove(paramSession, paramRow);
        checkRowCount(paramSession, localIndex1, -1);
      }
      this.rowCount -= 1L;
    }
    catch (Throwable localThrowable)
    {
      try
      {
        for (;;)
        {
          i++;
          if (i >= this.indexes.size()) {
            break;
          }
          Index localIndex2 = (Index)this.indexes.get(i);
          localIndex2.add(paramSession, paramRow);
          checkRowCount(paramSession, localIndex2, 0);
        }
      }
      catch (DbException localDbException)
      {
        this.trace.error(localDbException, "could not undo operation");
        throw localDbException;
      }
      throw DbException.convert(localThrowable);
    }
    analyzeIfRequired(paramSession);
  }
  
  public void truncate(Session paramSession)
  {
    this.lastModificationId = this.database.getNextModificationDataId();
    for (int i = this.indexes.size() - 1; i >= 0; i--)
    {
      Index localIndex = (Index)this.indexes.get(i);
      localIndex.truncate(paramSession);
    }
    this.rowCount = 0L;
    this.changesSinceAnalyze = 0;
  }
  
  private void analyzeIfRequired(Session paramSession)
  {
    if ((this.nextAnalyze == 0) || (this.nextAnalyze > this.changesSinceAnalyze++)) {
      return;
    }
    this.changesSinceAnalyze = 0;
    int i = 2 * this.nextAnalyze;
    if (i > 0) {
      this.nextAnalyze = i;
    }
    int j = paramSession.getDatabase().getSettings().analyzeSample / 10;
    Analyze.analyzeTable(paramSession, this, j, false);
  }
  
  public boolean isLockedExclusivelyBy(Session paramSession)
  {
    return this.lockExclusiveSession == paramSession;
  }
  
  public boolean lock(Session paramSession, boolean paramBoolean1, boolean paramBoolean2)
  {
    int i = this.database.getLockMode();
    if (i == 0) {
      return this.lockExclusiveSession != null;
    }
    if ((!paramBoolean2) && (this.database.isMultiVersion())) {
      if (paramBoolean1) {
        paramBoolean1 = false;
      } else if (this.lockExclusiveSession == null) {
        return false;
      }
    }
    if (this.lockExclusiveSession == paramSession) {
      return true;
    }
    synchronized (this.database)
    {
      if (this.lockExclusiveSession == paramSession) {
        return true;
      }
      if ((!paramBoolean1) && (this.lockSharedSessions.contains(paramSession))) {
        return true;
      }
      paramSession.setWaitForLock(this, Thread.currentThread());
      this.waitingSessions.addLast(paramSession);
      try
      {
        doLock1(paramSession, i, paramBoolean1);
      }
      finally
      {
        paramSession.setWaitForLock(null, null);
        this.waitingSessions.remove(paramSession);
      }
    }
    return false;
  }
  
  private void doLock1(Session paramSession, int paramInt, boolean paramBoolean)
  {
    traceLock(paramSession, paramBoolean, "requesting for");
    
    long l1 = 0L;
    int i = 0;
    for (;;)
    {
      if ((this.waitingSessions.getFirst() == paramSession) && 
        (doLock2(paramSession, paramInt, paramBoolean))) {
        return;
      }
      if (i != 0)
      {
        ArrayList localArrayList = checkDeadlock(paramSession, null, null);
        if (localArrayList != null) {
          throw DbException.get(40001, getDeadlockDetails(localArrayList, paramBoolean));
        }
      }
      else
      {
        i = 1;
      }
      long l2 = System.currentTimeMillis();
      if (l1 == 0L)
      {
        l1 = l2 + paramSession.getLockTimeout();
      }
      else if (l2 >= l1)
      {
        traceLock(paramSession, paramBoolean, "timeout after " + paramSession.getLockTimeout());
        throw DbException.get(50200, getName());
      }
      try
      {
        traceLock(paramSession, paramBoolean, "waiting for");
        if (this.database.getLockMode() == 2) {
          for (int j = 0; j < 20; j++)
          {
            long l4 = Runtime.getRuntime().freeMemory();
            System.gc();
            long l5 = Runtime.getRuntime().freeMemory();
            if (l4 == l5) {
              break;
            }
          }
        }
        long l3 = Math.min(100L, l1 - l2);
        if (l3 == 0L) {
          l3 = 1L;
        }
        this.database.wait(l3);
      }
      catch (InterruptedException localInterruptedException) {}
    }
  }
  
  private boolean doLock2(Session paramSession, int paramInt, boolean paramBoolean)
  {
    if (paramBoolean)
    {
      if (this.lockExclusiveSession == null)
      {
        if (this.lockSharedSessions.isEmpty())
        {
          traceLock(paramSession, paramBoolean, "added for");
          paramSession.addLock(this);
          this.lockExclusiveSession = paramSession;
          return true;
        }
        if ((this.lockSharedSessions.size() == 1) && (this.lockSharedSessions.contains(paramSession)))
        {
          traceLock(paramSession, paramBoolean, "add (upgraded) for ");
          this.lockExclusiveSession = paramSession;
          return true;
        }
      }
    }
    else if (this.lockExclusiveSession == null)
    {
      if ((paramInt == 3) && 
        (!this.database.isMultiThreaded()) && (!this.database.isMultiVersion())) {
        return true;
      }
      if (!this.lockSharedSessions.contains(paramSession))
      {
        traceLock(paramSession, paramBoolean, "ok");
        paramSession.addLock(this);
        this.lockSharedSessions.add(paramSession);
      }
      return true;
    }
    return false;
  }
  
  private static String getDeadlockDetails(ArrayList<Session> paramArrayList, boolean paramBoolean)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    for (Session localSession : paramArrayList)
    {
      Table localTable1 = localSession.getWaitForLock();
      Thread localThread = localSession.getWaitForLockThread();
      localStringBuilder.append("\nSession ").append(localSession.toString()).append(" on thread ").append(localThread.getName()).append(" is waiting to lock ").append(localTable1.toString()).append(paramBoolean ? " (exclusive)" : " (shared)").append(" while locking ");
      
      int i = 0;
      for (Table localTable2 : localSession.getLocks())
      {
        if (i++ > 0) {
          localStringBuilder.append(", ");
        }
        localStringBuilder.append(localTable2.toString());
        if ((localTable2 instanceof RegularTable)) {
          if (((RegularTable)localTable2).lockExclusiveSession == localSession) {
            localStringBuilder.append(" (exclusive)");
          } else {
            localStringBuilder.append(" (shared)");
          }
        }
      }
      localStringBuilder.append('.');
    }
    return localStringBuilder.toString();
  }
  
  public ArrayList<Session> checkDeadlock(Session paramSession1, Session paramSession2, Set<Session> paramSet)
  {
    synchronized (RegularTable.class)
    {
      if (paramSession2 == null)
      {
        paramSession2 = paramSession1;
        paramSet = New.hashSet();
      }
      else
      {
        if (paramSession2 == paramSession1) {
          return New.arrayList();
        }
        if (paramSet.contains(paramSession1)) {
          return null;
        }
      }
      paramSet.add(paramSession1);
      ArrayList localArrayList = null;
      for (Object localObject1 = this.lockSharedSessions.iterator(); ((Iterator)localObject1).hasNext();)
      {
        Session localSession = (Session)((Iterator)localObject1).next();
        if (localSession != paramSession1)
        {
          Table localTable = localSession.getWaitForLock();
          if (localTable != null)
          {
            localArrayList = localTable.checkDeadlock(localSession, paramSession2, paramSet);
            if (localArrayList != null)
            {
              localArrayList.add(paramSession1);
              break;
            }
          }
        }
      }
      if ((localArrayList == null) && (this.lockExclusiveSession != null))
      {
        localObject1 = this.lockExclusiveSession.getWaitForLock();
        if (localObject1 != null)
        {
          localArrayList = ((Table)localObject1).checkDeadlock(this.lockExclusiveSession, paramSession2, paramSet);
          if (localArrayList != null) {
            localArrayList.add(paramSession1);
          }
        }
      }
      return localArrayList;
    }
  }
  
  private void traceLock(Session paramSession, boolean paramBoolean, String paramString)
  {
    if (this.traceLock.isDebugEnabled()) {
      this.traceLock.debug("{0} {1} {2} {3}", new Object[] { Integer.valueOf(paramSession.getId()), paramBoolean ? "exclusive write lock" : "shared read lock", paramString, getName() });
    }
  }
  
  public boolean isLockedExclusively()
  {
    return this.lockExclusiveSession != null;
  }
  
  public void unlock(Session paramSession)
  {
    if (this.database != null)
    {
      traceLock(paramSession, this.lockExclusiveSession == paramSession, "unlock");
      if (this.lockExclusiveSession == paramSession) {
        this.lockExclusiveSession = null;
      }
      if (this.lockSharedSessions.size() > 0) {
        this.lockSharedSessions.remove(paramSession);
      }
      synchronized (this.database)
      {
        if (!this.waitingSessions.isEmpty()) {
          this.database.notifyAll();
        }
      }
    }
  }
  
  public static Row createRow(Value[] paramArrayOfValue)
  {
    return new Row(paramArrayOfValue, -1);
  }
  
  public void setRowCount(long paramLong)
  {
    this.rowCount = paramLong;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    if (this.containsLargeObject)
    {
      truncate(paramSession);
      this.database.getLobStorage().removeAllForTable(getId());
      this.database.lockMeta(paramSession);
    }
    super.removeChildrenAndResources(paramSession);
    Object localObject;
    while (this.indexes.size() > 1)
    {
      localObject = (Index)this.indexes.get(1);
      if (((Index)localObject).getName() != null) {
        this.database.removeSchemaObject(paramSession, (SchemaObject)localObject);
      }
      this.indexes.remove(localObject);
    }
    if (SysProperties.CHECK) {
      for (localObject = this.database.getAllSchemaObjects(1).iterator(); ((Iterator)localObject).hasNext();)
      {
        SchemaObject localSchemaObject = (SchemaObject)((Iterator)localObject).next();
        Index localIndex = (Index)localSchemaObject;
        if (localIndex.getTable() == this) {
          DbException.throwInternalError("index not dropped: " + localIndex.getName());
        }
      }
    }
    this.scanIndex.remove(paramSession);
    this.database.removeMeta(paramSession, getId());
    this.scanIndex = null;
    this.lockExclusiveSession = null;
    this.lockSharedSessions = null;
    invalidate();
  }
  
  public String toString()
  {
    return getSQL();
  }
  
  public void checkRename() {}
  
  public void checkSupportAlter() {}
  
  public boolean canTruncate()
  {
    if ((getCheckForeignKeyConstraints()) && (this.database.getReferentialIntegrity()))
    {
      ArrayList localArrayList = getConstraints();
      if (localArrayList != null)
      {
        int i = 0;
        for (int j = localArrayList.size(); i < j; i++)
        {
          Constraint localConstraint = (Constraint)localArrayList.get(i);
          if (localConstraint.getConstraintType().equals("REFERENTIAL"))
          {
            ConstraintReferential localConstraintReferential = (ConstraintReferential)localConstraint;
            if (localConstraintReferential.getRefTable() == this) {
              return false;
            }
          }
        }
      }
    }
    return true;
  }
  
  public String getTableType()
  {
    return "TABLE";
  }
  
  public long getMaxDataModificationId()
  {
    return this.lastModificationId;
  }
  
  public boolean getContainsLargeObject()
  {
    return this.containsLargeObject;
  }
  
  public long getRowCountApproximation()
  {
    return this.scanIndex.getRowCountApproximation();
  }
  
  public long getDiskSpaceUsed()
  {
    return this.scanIndex.getDiskSpaceUsed();
  }
  
  public void setCompareMode(CompareMode paramCompareMode)
  {
    this.compareMode = paramCompareMode;
  }
  
  public boolean isDeterministic()
  {
    return true;
  }
  
  public Column getRowIdColumn()
  {
    if (this.rowIdColumn == null)
    {
      this.rowIdColumn = new Column("_ROWID_", 5);
      this.rowIdColumn.setTable(this, -1);
    }
    return this.rowIdColumn;
  }
}
