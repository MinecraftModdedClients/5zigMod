package org.h2.mvstore.db;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.h2.command.ddl.Analyze;
import org.h2.command.ddl.CreateTableData;
import org.h2.constraint.Constraint;
import org.h2.constraint.ConstraintReferential;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.index.Cursor;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.index.MultiVersionIndex;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.mvstore.MVStore;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.store.LobStorageInterface;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableBase;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.value.DataType;

public class MVTable
  extends TableBase
{
  private MVPrimaryIndex primaryIndex;
  private final ArrayList<Index> indexes = New.arrayList();
  private long lastModificationId;
  private volatile Session lockExclusiveSession;
  private final ConcurrentHashMap<Session, Session> lockSharedSessions = new ConcurrentHashMap();
  private final ArrayDeque<Session> waitingSessions = new ArrayDeque();
  private final Trace traceLock;
  private int changesSinceAnalyze;
  private int nextAnalyze;
  private boolean containsLargeObject;
  private Column rowIdColumn;
  private final TransactionStore store;
  
  public MVTable(CreateTableData paramCreateTableData, MVTableEngine.Store paramStore)
  {
    super(paramCreateTableData);
    this.nextAnalyze = this.database.getSettings().analyzeAuto;
    this.store = paramStore.getTransactionStore();
    this.isHidden = paramCreateTableData.isHidden;
    for (Column localColumn : getColumns()) {
      if (DataType.isLargeObject(localColumn.getType())) {
        this.containsLargeObject = true;
      }
    }
    this.traceLock = this.database.getTrace("lock");
  }
  
  void init(Session paramSession)
  {
    this.primaryIndex = new MVPrimaryIndex(paramSession.getDatabase(), this, getId(), IndexColumn.wrap(getColumns()), IndexType.createScan(true));
    
    this.indexes.add(this.primaryIndex);
  }
  
  public String getMapName()
  {
    return this.primaryIndex.getMapName();
  }
  
  public boolean lock(Session paramSession, boolean paramBoolean1, boolean paramBoolean2)
  {
    int i = this.database.getLockMode();
    if (i == 0) {
      return false;
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
    if ((!paramBoolean1) && (this.lockSharedSessions.contains(paramSession))) {
      return true;
    }
    synchronized (getLockSyncObject())
    {
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
  
  private Object getLockSyncObject()
  {
    if (this.database.isMultiThreaded()) {
      return this;
    }
    return this.database;
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
        getLockSyncObject().wait(l3);
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
        this.lockSharedSessions.put(paramSession, paramSession);
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
        if ((localTable2 instanceof MVTable)) {
          if (((MVTable)localTable2).lockExclusiveSession == localSession) {
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
    synchronized (MVTable.class)
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
      for (Object localObject1 = this.lockSharedSessions.keySet().iterator(); ((Iterator)localObject1).hasNext();)
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
  
  public boolean isLockedExclusivelyBy(Session paramSession)
  {
    return this.lockExclusiveSession == paramSession;
  }
  
  public void unlock(Session paramSession)
  {
    if (this.database != null)
    {
      traceLock(paramSession, this.lockExclusiveSession == paramSession, "unlock");
      if (this.lockExclusiveSession == paramSession) {
        this.lockExclusiveSession = null;
      }
      synchronized (getLockSyncObject())
      {
        if (this.lockSharedSessions.size() > 0) {
          this.lockSharedSessions.remove(paramSession);
        }
        if (!this.waitingSessions.isEmpty()) {
          getLockSyncObject().notifyAll();
        }
      }
    }
  }
  
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
  
  public void close(Session paramSession) {}
  
  public Row getRow(Session paramSession, long paramLong)
  {
    return this.primaryIndex.getRow(paramSession, paramLong);
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
    ??? = getMainIndexColumn(paramIndexType, paramArrayOfIndexColumn);
    if (this.database.isStarting())
    {
      if (this.store.store.hasMap("index." + paramInt)) {
        ??? = -1;
      }
    }
    else if (this.primaryIndex.getRowCountMax() != 0L) {
      ??? = -1;
    }
    Object localObject;
    if (??? != -1)
    {
      this.primaryIndex.setMainIndexColumn(???);
      localObject = new MVDelegateIndex(this, paramInt, paramString1, this.primaryIndex, paramIndexType);
    }
    else if (paramIndexType.isSpatial())
    {
      localObject = new MVSpatialIndex(paramSession.getDatabase(), this, paramInt, paramString1, paramArrayOfIndexColumn, paramIndexType);
    }
    else
    {
      localObject = new MVSecondaryIndex(paramSession.getDatabase(), this, paramInt, paramString1, paramArrayOfIndexColumn, paramIndexType);
    }
    if (((MVIndex)localObject).needRebuild()) {
      rebuildIndex(paramSession, (MVIndex)localObject, paramString1);
    }
    ((MVIndex)localObject).setTemporary(isTemporary());
    if (((MVIndex)localObject).getCreateSQL() != null)
    {
      ((MVIndex)localObject).setComment(paramString2);
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
  
  private void rebuildIndex(Session paramSession, MVIndex paramMVIndex, String paramString)
  {
    try
    {
      if ((paramSession.getDatabase().getMvStore() == null) || ((paramMVIndex instanceof MVSpatialIndex))) {
        rebuildIndexBuffered(paramSession, paramMVIndex);
      } else {
        rebuildIndexBlockMerge(paramSession, paramMVIndex);
      }
    }
    catch (DbException localDbException1)
    {
      getSchema().freeUniqueName(paramString);
      try
      {
        paramMVIndex.remove(paramSession);
      }
      catch (DbException localDbException2)
      {
        this.trace.error(localDbException2, "could not remove index");
        throw localDbException2;
      }
      throw localDbException1;
    }
  }
  
  private void rebuildIndexBlockMerge(Session paramSession, MVIndex paramMVIndex)
  {
    if ((paramMVIndex instanceof MVSpatialIndex)) {
      rebuildIndexBuffered(paramSession, paramMVIndex);
    }
    Index localIndex = getScanIndex(paramSession);
    long l1 = localIndex.getRowCount(paramSession);
    long l2 = l1;
    Cursor localCursor = localIndex.find(paramSession, null, null);
    long l3 = 0L;
    MVTableEngine.Store localStore = paramSession.getDatabase().getMvStore();
    
    int i = this.database.getMaxMemoryRows() / 2;
    ArrayList localArrayList1 = New.arrayList(i);
    String str1 = getName() + ":" + paramMVIndex.getName();
    int j = MathUtils.convertLongToInt(l2);
    ArrayList localArrayList2 = New.arrayList();
    Object localObject;
    while (localCursor.next())
    {
      localObject = localCursor.get();
      localArrayList1.add(localObject);
      this.database.setProgress(1, str1, MathUtils.convertLongToInt(l3++), j);
      if (localArrayList1.size() >= i)
      {
        sortRows(localArrayList1, paramMVIndex);
        String str2 = localStore.nextTemporaryMapName();
        paramMVIndex.addRowsToBuffer(localArrayList1, str2);
        localArrayList2.add(str2);
        localArrayList1.clear();
      }
      l1 -= 1L;
    }
    sortRows(localArrayList1, paramMVIndex);
    if (localArrayList2.size() > 0)
    {
      localObject = localStore.nextTemporaryMapName();
      paramMVIndex.addRowsToBuffer(localArrayList1, (String)localObject);
      localArrayList2.add(localObject);
      localArrayList1.clear();
      paramMVIndex.addBufferedRows(localArrayList2);
    }
    else
    {
      addRowsToIndex(paramSession, localArrayList1, paramMVIndex);
    }
    if ((SysProperties.CHECK) && (l1 != 0L)) {
      DbException.throwInternalError("rowcount remaining=" + l1 + " " + getName());
    }
  }
  
  private void rebuildIndexBuffered(Session paramSession, Index paramIndex)
  {
    Index localIndex = getScanIndex(paramSession);
    long l1 = localIndex.getRowCount(paramSession);
    long l2 = l1;
    Cursor localCursor = localIndex.find(paramSession, null, null);
    long l3 = 0L;
    int i = (int)Math.min(l2, this.database.getMaxMemoryRows());
    ArrayList localArrayList = New.arrayList(i);
    String str = getName() + ":" + paramIndex.getName();
    int j = MathUtils.convertLongToInt(l2);
    while (localCursor.next())
    {
      Row localRow = localCursor.get();
      localArrayList.add(localRow);
      this.database.setProgress(1, str, MathUtils.convertLongToInt(l3++), j);
      if (localArrayList.size() >= i) {
        addRowsToIndex(paramSession, localArrayList, paramIndex);
      }
      l1 -= 1L;
    }
    addRowsToIndex(paramSession, localArrayList, paramIndex);
    if ((SysProperties.CHECK) && (l1 != 0L)) {
      DbException.throwInternalError("rowcount remaining=" + l1 + " " + getName());
    }
  }
  
  private int getMainIndexColumn(IndexType paramIndexType, IndexColumn[] paramArrayOfIndexColumn)
  {
    if (this.primaryIndex.getMainIndexColumn() != -1) {
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
  
  private static void addRowsToIndex(Session paramSession, ArrayList<Row> paramArrayList, Index paramIndex)
  {
    sortRows(paramArrayList, paramIndex);
    for (Row localRow : paramArrayList) {
      paramIndex.add(paramSession, localRow);
    }
    paramArrayList.clear();
  }
  
  private static void sortRows(ArrayList<Row> paramArrayList, Index paramIndex)
  {
    Collections.sort(paramArrayList, new Comparator()
    {
      public int compare(Row paramAnonymousRow1, Row paramAnonymousRow2)
      {
        return this.val$index.compareRows(paramAnonymousRow1, paramAnonymousRow2);
      }
    });
  }
  
  public void removeRow(Session paramSession, Row paramRow)
  {
    this.lastModificationId = this.database.getNextModificationDataId();
    TransactionStore.Transaction localTransaction = getTransaction(paramSession);
    long l = localTransaction.setSavepoint();
    try
    {
      for (int i = this.indexes.size() - 1; i >= 0; i--)
      {
        Index localIndex = (Index)this.indexes.get(i);
        localIndex.remove(paramSession, paramRow);
      }
    }
    catch (Throwable localThrowable)
    {
      localTransaction.rollbackToSavepoint(l);
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
    this.changesSinceAnalyze = 0;
  }
  
  public void addRow(Session paramSession, Row paramRow)
  {
    this.lastModificationId = this.database.getNextModificationDataId();
    TransactionStore.Transaction localTransaction = getTransaction(paramSession);
    long l = localTransaction.setSavepoint();
    try
    {
      int i = 0;
      for (int j = this.indexes.size(); i < j; i++)
      {
        Index localIndex1 = (Index)this.indexes.get(i);
        localIndex1.add(paramSession, paramRow);
      }
    }
    catch (Throwable localThrowable)
    {
      localTransaction.rollbackToSavepoint(l);
      DbException localDbException = DbException.convert(localThrowable);
      if (localDbException.getErrorCode() == 23505) {
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
      throw localDbException;
    }
    analyzeIfRequired(paramSession);
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
  
  public void checkSupportAlter() {}
  
  public String getTableType()
  {
    return "TABLE";
  }
  
  public Index getScanIndex(Session paramSession)
  {
    return this.primaryIndex;
  }
  
  public Index getUniqueIndex()
  {
    return this.primaryIndex;
  }
  
  public ArrayList<Index> getIndexes()
  {
    return this.indexes;
  }
  
  public long getMaxDataModificationId()
  {
    return this.lastModificationId;
  }
  
  public boolean getContainsLargeObject()
  {
    return this.containsLargeObject;
  }
  
  public boolean isDeterministic()
  {
    return true;
  }
  
  public boolean canGetRowCount()
  {
    return true;
  }
  
  public boolean canDrop()
  {
    return true;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    if (this.containsLargeObject)
    {
      truncate(paramSession);
      this.database.getLobStorage().removeAllForTable(getId());
      this.database.lockMeta(paramSession);
    }
    this.database.getMvStore().removeTable(this);
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
    this.primaryIndex.remove(paramSession);
    this.database.removeMeta(paramSession, getId());
    close(paramSession);
    invalidate();
  }
  
  public long getRowCount(Session paramSession)
  {
    return this.primaryIndex.getRowCount(paramSession);
  }
  
  public long getRowCountApproximation()
  {
    return this.primaryIndex.getRowCountApproximation();
  }
  
  public long getDiskSpaceUsed()
  {
    return this.primaryIndex.getDiskSpaceUsed();
  }
  
  public void checkRename() {}
  
  TransactionStore.Transaction getTransaction(Session paramSession)
  {
    if (paramSession == null) {
      return this.store.begin();
    }
    return paramSession.getTransaction();
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
  
  public String toString()
  {
    return getSQL();
  }
  
  public boolean isMVStore()
  {
    return true;
  }
  
  public void commit()
  {
    if (this.database != null) {
      this.lastModificationId = this.database.getNextModificationDataId();
    }
  }
}
