package org.h2.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import org.h2.command.Command;
import org.h2.command.CommandInterface;
import org.h2.command.Parser;
import org.h2.command.Prepared;
import org.h2.command.dml.SetTypes;
import org.h2.constraint.Constraint;
import org.h2.index.Index;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.message.TraceSystem;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.db.MVTable;
import org.h2.mvstore.db.MVTableEngine.Store;
import org.h2.mvstore.db.TransactionStore;
import org.h2.mvstore.db.TransactionStore.Change;
import org.h2.mvstore.db.TransactionStore.Transaction;
import org.h2.result.LocalResult;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.store.DataHandler;
import org.h2.store.InDoubtTransaction;
import org.h2.table.Table;
import org.h2.util.New;
import org.h2.util.SmallLRUCache;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;

public class Session
  extends SessionWithState
{
  public static final int LOG_WRITTEN = -1;
  private static final String SYSTEM_IDENTIFIER_PREFIX = "_";
  private static int nextSerialId;
  private final int serialId = nextSerialId++;
  private final Database database;
  private ConnectionInfo connectionInfo;
  private final User user;
  private final int id;
  private final ArrayList<Table> locks = New.arrayList();
  private final UndoLog undoLog;
  private boolean autoCommit = true;
  private Random random;
  private int lockTimeout;
  private Value lastIdentity = ValueLong.get(0L);
  private Value lastScopeIdentity = ValueLong.get(0L);
  private int firstUncommittedLog = -1;
  private int firstUncommittedPos = -1;
  private HashMap<String, Savepoint> savepoints;
  private HashMap<String, Table> localTempTables;
  private HashMap<String, Index> localTempTableIndexes;
  private HashMap<String, Constraint> localTempTableConstraints;
  private int throttle;
  private long lastThrottle;
  private Command currentCommand;
  private boolean allowLiterals;
  private String currentSchemaName;
  private String[] schemaSearchPath;
  private Trace trace;
  private HashMap<String, Value> unlinkLobMap;
  private int systemIdentifier;
  private HashMap<String, Procedure> procedures;
  private boolean undoLogEnabled = true;
  private boolean redoLogBinary = true;
  private boolean autoCommitAtTransactionEnd;
  private String currentTransactionName;
  private volatile long cancelAt;
  private boolean closed;
  private final long sessionStart = System.currentTimeMillis();
  private long transactionStart;
  private long currentCommandStart;
  private HashMap<String, Value> variables;
  private HashSet<LocalResult> temporaryResults;
  private int queryTimeout;
  private boolean commitOrRollbackDisabled;
  private Table waitForLock;
  private Thread waitForLockThread;
  private int modificationId;
  private int objectId;
  private final int queryCacheSize;
  private SmallLRUCache<String, Command> queryCache;
  private long modificationMetaID = -1L;
  private ArrayList<Value> temporaryLobs;
  private TransactionStore.Transaction transaction;
  private long startStatement = -1L;
  
  public Session(Database paramDatabase, User paramUser, int paramInt)
  {
    this.database = paramDatabase;
    this.queryTimeout = paramDatabase.getSettings().maxQueryTimeout;
    this.queryCacheSize = paramDatabase.getSettings().queryCacheSize;
    this.undoLog = new UndoLog(this);
    this.user = paramUser;
    this.id = paramInt;
    Setting localSetting = paramDatabase.findSetting(SetTypes.getTypeName(6));
    
    this.lockTimeout = (localSetting == null ? 2000 : localSetting.getIntValue());
    
    this.currentSchemaName = "PUBLIC";
  }
  
  public ArrayList<String> getClusterServers()
  {
    return new ArrayList();
  }
  
  public boolean setCommitOrRollbackDisabled(boolean paramBoolean)
  {
    boolean bool = this.commitOrRollbackDisabled;
    this.commitOrRollbackDisabled = paramBoolean;
    return bool;
  }
  
  private void initVariables()
  {
    if (this.variables == null) {
      this.variables = this.database.newStringMap();
    }
  }
  
  public void setVariable(String paramString, Value paramValue)
  {
    initVariables();
    this.modificationId += 1;
    Value localValue;
    if (paramValue == ValueNull.INSTANCE)
    {
      localValue = (Value)this.variables.remove(paramString);
    }
    else
    {
      paramValue = paramValue.link(this.database, -1);
      
      localValue = (Value)this.variables.put(paramString, paramValue);
    }
    if (localValue != null)
    {
      localValue.unlink(this.database);
      localValue.close();
    }
  }
  
  public Value getVariable(String paramString)
  {
    initVariables();
    Value localValue = (Value)this.variables.get(paramString);
    return localValue == null ? ValueNull.INSTANCE : localValue;
  }
  
  public String[] getVariableNames()
  {
    if (this.variables == null) {
      return new String[0];
    }
    String[] arrayOfString = new String[this.variables.size()];
    this.variables.keySet().toArray(arrayOfString);
    return arrayOfString;
  }
  
  public Table findLocalTempTable(String paramString)
  {
    if (this.localTempTables == null) {
      return null;
    }
    return (Table)this.localTempTables.get(paramString);
  }
  
  public ArrayList<Table> getLocalTempTables()
  {
    if (this.localTempTables == null) {
      return New.arrayList();
    }
    return New.arrayList(this.localTempTables.values());
  }
  
  public void addLocalTempTable(Table paramTable)
  {
    if (this.localTempTables == null) {
      this.localTempTables = this.database.newStringMap();
    }
    if (this.localTempTables.get(paramTable.getName()) != null) {
      throw DbException.get(42101, paramTable.getSQL());
    }
    this.modificationId += 1;
    this.localTempTables.put(paramTable.getName(), paramTable);
  }
  
  public void removeLocalTempTable(Table paramTable)
  {
    this.modificationId += 1;
    this.localTempTables.remove(paramTable.getName());
    synchronized (this.database)
    {
      paramTable.removeChildrenAndResources(this);
    }
  }
  
  public Index findLocalTempTableIndex(String paramString)
  {
    if (this.localTempTableIndexes == null) {
      return null;
    }
    return (Index)this.localTempTableIndexes.get(paramString);
  }
  
  public HashMap<String, Index> getLocalTempTableIndexes()
  {
    if (this.localTempTableIndexes == null) {
      return New.hashMap();
    }
    return this.localTempTableIndexes;
  }
  
  public void addLocalTempTableIndex(Index paramIndex)
  {
    if (this.localTempTableIndexes == null) {
      this.localTempTableIndexes = this.database.newStringMap();
    }
    if (this.localTempTableIndexes.get(paramIndex.getName()) != null) {
      throw DbException.get(42111, paramIndex.getSQL());
    }
    this.localTempTableIndexes.put(paramIndex.getName(), paramIndex);
  }
  
  public void removeLocalTempTableIndex(Index paramIndex)
  {
    if (this.localTempTableIndexes != null)
    {
      this.localTempTableIndexes.remove(paramIndex.getName());
      synchronized (this.database)
      {
        paramIndex.removeChildrenAndResources(this);
      }
    }
  }
  
  public Constraint findLocalTempTableConstraint(String paramString)
  {
    if (this.localTempTableConstraints == null) {
      return null;
    }
    return (Constraint)this.localTempTableConstraints.get(paramString);
  }
  
  public HashMap<String, Constraint> getLocalTempTableConstraints()
  {
    if (this.localTempTableConstraints == null) {
      return New.hashMap();
    }
    return this.localTempTableConstraints;
  }
  
  public void addLocalTempTableConstraint(Constraint paramConstraint)
  {
    if (this.localTempTableConstraints == null) {
      this.localTempTableConstraints = this.database.newStringMap();
    }
    String str = paramConstraint.getName();
    if (this.localTempTableConstraints.get(str) != null) {
      throw DbException.get(90045, paramConstraint.getSQL());
    }
    this.localTempTableConstraints.put(str, paramConstraint);
  }
  
  void removeLocalTempTableConstraint(Constraint paramConstraint)
  {
    if (this.localTempTableConstraints != null)
    {
      this.localTempTableConstraints.remove(paramConstraint.getName());
      synchronized (this.database)
      {
        paramConstraint.removeChildrenAndResources(this);
      }
    }
  }
  
  public boolean getAutoCommit()
  {
    return this.autoCommit;
  }
  
  public User getUser()
  {
    return this.user;
  }
  
  public void setAutoCommit(boolean paramBoolean)
  {
    this.autoCommit = paramBoolean;
  }
  
  public int getLockTimeout()
  {
    return this.lockTimeout;
  }
  
  public void setLockTimeout(int paramInt)
  {
    this.lockTimeout = paramInt;
  }
  
  public synchronized CommandInterface prepareCommand(String paramString, int paramInt)
  {
    return prepareLocal(paramString);
  }
  
  public Prepared prepare(String paramString)
  {
    return prepare(paramString, false);
  }
  
  public Prepared prepare(String paramString, boolean paramBoolean)
  {
    Parser localParser = new Parser(this);
    localParser.setRightsChecked(paramBoolean);
    return localParser.prepare(paramString);
  }
  
  public Command prepareLocal(String paramString)
  {
    if (this.closed) {
      throw DbException.get(90067, "session closed");
    }
    if (this.queryCacheSize > 0) {
      if (this.queryCache == null)
      {
        this.queryCache = SmallLRUCache.newInstance(this.queryCacheSize);
        this.modificationMetaID = this.database.getModificationMetaId();
      }
      else
      {
        long l = this.database.getModificationMetaId();
        if (l != this.modificationMetaID)
        {
          this.queryCache.clear();
          this.modificationMetaID = l;
        }
        localCommand = (Command)this.queryCache.get(paramString);
        if ((localCommand != null) && (localCommand.canReuse()))
        {
          localCommand.reuse();
          return localCommand;
        }
      }
    }
    Parser localParser = new Parser(this);
    Command localCommand = localParser.prepareCommand(paramString);
    if ((this.queryCache != null) && 
      (localCommand.isCacheable())) {
      this.queryCache.put(paramString, localCommand);
    }
    return localCommand;
  }
  
  public Database getDatabase()
  {
    return this.database;
  }
  
  public int getPowerOffCount()
  {
    return this.database.getPowerOffCount();
  }
  
  public void setPowerOffCount(int paramInt)
  {
    this.database.setPowerOffCount(paramInt);
  }
  
  public void commit(boolean paramBoolean)
  {
    checkCommitRollback();
    this.currentTransactionName = null;
    this.transactionStart = 0L;
    Object localObject2;
    if (this.transaction != null)
    {
      if (this.locks.size() > 0)
      {
        int i = 0;
        for (int j = this.locks.size(); i < j; i++)
        {
          localObject2 = (Table)this.locks.get(i);
          if ((localObject2 instanceof MVTable)) {
            ((MVTable)localObject2).commit();
          }
        }
      }
      this.transaction.commit();
      this.transaction = null;
    }
    if (containsUncommitted()) {
      this.database.commit(this);
    }
    Object localObject1;
    if (this.temporaryLobs != null)
    {
      for (localObject1 = this.temporaryLobs.iterator(); ((Iterator)localObject1).hasNext();)
      {
        Value localValue = (Value)((Iterator)localObject1).next();
        if (!localValue.isLinked()) {
          localValue.close();
        }
      }
      this.temporaryLobs.clear();
    }
    if (this.undoLog.size() > 0)
    {
      if (this.database.isMultiVersion())
      {
        localObject1 = New.arrayList();
        synchronized (this.database)
        {
          while (this.undoLog.size() > 0)
          {
            localObject2 = this.undoLog.getLast();
            ((UndoLogRecord)localObject2).commit();
            ((ArrayList)localObject1).add(((UndoLogRecord)localObject2).getRow());
            this.undoLog.removeLast(false);
          }
          int k = 0;
          for (int m = ((ArrayList)localObject1).size(); k < m; k++)
          {
            Row localRow = (Row)((ArrayList)localObject1).get(k);
            localRow.commit();
          }
        }
      }
      this.undoLog.clear();
    }
    if (!paramBoolean)
    {
      cleanTempTables(false);
      if (this.autoCommitAtTransactionEnd)
      {
        this.autoCommit = true;
        this.autoCommitAtTransactionEnd = false;
      }
    }
    endTransaction();
  }
  
  private void checkCommitRollback()
  {
    if ((this.commitOrRollbackDisabled) && (this.locks.size() > 0)) {
      throw DbException.get(90058);
    }
  }
  
  private void endTransaction()
  {
    if ((this.unlinkLobMap != null) && (this.unlinkLobMap.size() > 0))
    {
      this.database.flush();
      for (Value localValue : this.unlinkLobMap.values())
      {
        localValue.unlink(this.database);
        localValue.close();
      }
      this.unlinkLobMap = null;
    }
    unlockAll();
  }
  
  public void rollback()
  {
    checkCommitRollback();
    this.currentTransactionName = null;
    int i = 0;
    if (this.undoLog.size() > 0)
    {
      rollbackTo(null, false);
      i = 1;
    }
    if (this.transaction != null)
    {
      rollbackTo(null, false);
      i = 1;
      
      this.transaction.commit();
      this.transaction = null;
    }
    if ((this.locks.size() > 0) || (i != 0)) {
      this.database.commit(this);
    }
    cleanTempTables(false);
    if (this.autoCommitAtTransactionEnd)
    {
      this.autoCommit = true;
      this.autoCommitAtTransactionEnd = false;
    }
    endTransaction();
  }
  
  public void rollbackTo(Savepoint paramSavepoint, boolean paramBoolean)
  {
    int i = paramSavepoint == null ? 0 : paramSavepoint.logIndex;
    while (this.undoLog.size() > i)
    {
      UndoLogRecord localUndoLogRecord1 = this.undoLog.getLast();
      localUndoLogRecord1.undo(this);
      this.undoLog.removeLast(paramBoolean);
    }
    TransactionStore.Change localChange;
    Object localObject;
    if (this.transaction != null)
    {
      long l1 = paramSavepoint == null ? 0L : paramSavepoint.transactionSavepoint;
      HashMap localHashMap = this.database.getMvStore().getTables();
      
      Iterator localIterator = this.transaction.getChanges(l1);
      while (localIterator.hasNext())
      {
        localChange = (TransactionStore.Change)localIterator.next();
        localObject = (MVTable)localHashMap.get(localChange.mapName);
        if (localObject != null)
        {
          long l2 = ((ValueLong)localChange.key).getLong();
          ValueArray localValueArray = (ValueArray)localChange.value;
          short s;
          Row localRow;
          if (localValueArray == null)
          {
            s = 0;
            localRow = ((MVTable)localObject).getRow(this, l2);
          }
          else
          {
            s = 1;
            localRow = new Row(localValueArray.getList(), -1);
          }
          localRow.setKey(l2);
          UndoLogRecord localUndoLogRecord2 = new UndoLogRecord((Table)localObject, s, localRow);
          localUndoLogRecord2.undo(this);
        }
      }
    }
    if (this.savepoints != null)
    {
      String[] arrayOfString1 = new String[this.savepoints.size()];
      this.savepoints.keySet().toArray(arrayOfString1);
      for (localChange : arrayOfString1)
      {
        localObject = (Savepoint)this.savepoints.get(localChange);
        int m = ((Savepoint)localObject).logIndex;
        if (m > i) {
          this.savepoints.remove(localChange);
        }
      }
    }
  }
  
  public boolean hasPendingTransaction()
  {
    return this.undoLog.size() > 0;
  }
  
  public Savepoint setSavepoint()
  {
    Savepoint localSavepoint = new Savepoint();
    localSavepoint.logIndex = this.undoLog.size();
    if (this.database.getMvStore() != null) {
      localSavepoint.transactionSavepoint = getStatementSavepoint();
    }
    return localSavepoint;
  }
  
  public int getId()
  {
    return this.id;
  }
  
  public void cancel()
  {
    this.cancelAt = System.currentTimeMillis();
  }
  
  public void close()
  {
    if (!this.closed) {
      try
      {
        this.database.checkPowerOff();
        cleanTempTables(true);
        this.undoLog.clear();
        this.database.removeSession(this);
      }
      finally
      {
        this.closed = true;
      }
    }
  }
  
  public void addLock(Table paramTable)
  {
    if ((SysProperties.CHECK) && 
      (this.locks.contains(paramTable))) {
      DbException.throwInternalError();
    }
    this.locks.add(paramTable);
  }
  
  public void log(Table paramTable, short paramShort, Row paramRow)
  {
    if (paramTable.isMVStore()) {
      return;
    }
    Object localObject;
    int i;
    if (this.undoLogEnabled)
    {
      localObject = new UndoLogRecord(paramTable, paramShort, paramRow);
      if (SysProperties.CHECK)
      {
        i = this.database.getLockMode();
        if ((i != 0) && (!this.database.isMultiVersion()))
        {
          String str = ((UndoLogRecord)localObject).getTable().getTableType();
          if ((this.locks.indexOf(((UndoLogRecord)localObject).getTable()) < 0) && (!"TABLE LINK".equals(str)) && (!"EXTERNAL".equals(str))) {
            DbException.throwInternalError();
          }
        }
      }
      this.undoLog.add((UndoLogRecord)localObject);
    }
    else if (this.database.isMultiVersion())
    {
      localObject = paramTable.getIndexes();
      i = 0;
      for (int j = ((ArrayList)localObject).size(); i < j; i++)
      {
        Index localIndex = (Index)((ArrayList)localObject).get(i);
        localIndex.commit(paramShort, paramRow);
      }
      paramRow.commit();
    }
  }
  
  public void unlockReadLocks()
  {
    if (this.database.isMultiVersion()) {
      return;
    }
    for (int i = 0; i < this.locks.size(); i++)
    {
      Table localTable = (Table)this.locks.get(i);
      if (!localTable.isLockedExclusively())
      {
        synchronized (this.database)
        {
          localTable.unlock(this);
          this.locks.remove(i);
        }
        i--;
      }
    }
  }
  
  void unlock(Table paramTable)
  {
    this.locks.remove(paramTable);
  }
  
  private void unlockAll()
  {
    if ((SysProperties.CHECK) && 
      (this.undoLog.size() > 0)) {
      DbException.throwInternalError();
    }
    if (this.locks.size() > 0)
    {
      int i = 0;
      for (int j = this.locks.size(); i < j; i++)
      {
        Table localTable = (Table)this.locks.get(i);
        localTable.unlock(this);
      }
      this.locks.clear();
    }
    this.savepoints = null;
    this.sessionStateChanged = true;
  }
  
  private void cleanTempTables(boolean paramBoolean)
  {
    if ((this.localTempTables != null) && (this.localTempTables.size() > 0)) {
      synchronized (this.database)
      {
        for (Table localTable : New.arrayList(this.localTempTables.values())) {
          if ((paramBoolean) || (localTable.getOnCommitDrop()))
          {
            this.modificationId += 1;
            localTable.setModified();
            this.localTempTables.remove(localTable.getName());
            localTable.removeChildrenAndResources(this);
            if (paramBoolean) {
              this.database.commit(this);
            }
          }
          else if (localTable.getOnCommitTruncate())
          {
            localTable.truncate(this);
          }
        }
      }
    }
  }
  
  public Random getRandom()
  {
    if (this.random == null) {
      this.random = new Random();
    }
    return this.random;
  }
  
  public Trace getTrace()
  {
    if ((this.trace != null) && (!this.closed)) {
      return this.trace;
    }
    String str = "jdbc[" + this.id + "]";
    if (this.closed) {
      return new TraceSystem(null).getTrace(str);
    }
    this.trace = this.database.getTrace(str);
    return this.trace;
  }
  
  public void setLastIdentity(Value paramValue)
  {
    this.lastIdentity = paramValue;
    this.lastScopeIdentity = paramValue;
  }
  
  public Value getLastIdentity()
  {
    return this.lastIdentity;
  }
  
  public void setLastScopeIdentity(Value paramValue)
  {
    this.lastScopeIdentity = paramValue;
  }
  
  public Value getLastScopeIdentity()
  {
    return this.lastScopeIdentity;
  }
  
  public void addLogPos(int paramInt1, int paramInt2)
  {
    if (this.firstUncommittedLog == -1)
    {
      this.firstUncommittedLog = paramInt1;
      this.firstUncommittedPos = paramInt2;
    }
  }
  
  public int getFirstUncommittedLog()
  {
    return this.firstUncommittedLog;
  }
  
  void setAllCommitted()
  {
    this.firstUncommittedLog = -1;
    this.firstUncommittedPos = -1;
  }
  
  public boolean containsUncommitted()
  {
    if (this.database.getMvStore() != null) {
      return this.transaction != null;
    }
    return this.firstUncommittedLog != -1;
  }
  
  public void addSavepoint(String paramString)
  {
    if (this.savepoints == null) {
      this.savepoints = this.database.newStringMap();
    }
    Savepoint localSavepoint = new Savepoint();
    localSavepoint.logIndex = this.undoLog.size();
    if (this.database.getMvStore() != null) {
      localSavepoint.transactionSavepoint = getStatementSavepoint();
    }
    this.savepoints.put(paramString, localSavepoint);
  }
  
  public void rollbackToSavepoint(String paramString)
  {
    checkCommitRollback();
    if (this.savepoints == null) {
      throw DbException.get(90063, paramString);
    }
    Savepoint localSavepoint = (Savepoint)this.savepoints.get(paramString);
    if (localSavepoint == null) {
      throw DbException.get(90063, paramString);
    }
    rollbackTo(localSavepoint, false);
  }
  
  public void prepareCommit(String paramString)
  {
    if (this.transaction != null) {
      this.database.prepareCommit(this, paramString);
    }
    if (containsUncommitted()) {
      this.database.prepareCommit(this, paramString);
    }
    this.currentTransactionName = paramString;
  }
  
  public void setPreparedTransaction(String paramString, boolean paramBoolean)
  {
    if ((this.currentTransactionName != null) && (this.currentTransactionName.equals(paramString)))
    {
      if (paramBoolean) {
        commit(false);
      } else {
        rollback();
      }
    }
    else
    {
      ArrayList localArrayList = this.database.getInDoubtTransactions();
      
      int i = paramBoolean ? 1 : 2;
      
      int j = 0;
      if (localArrayList != null) {
        for (InDoubtTransaction localInDoubtTransaction : localArrayList) {
          if (localInDoubtTransaction.getTransactionName().equals(paramString))
          {
            localInDoubtTransaction.setState(i);
            j = 1;
            break;
          }
        }
      }
      if (j == 0) {
        throw DbException.get(90129, paramString);
      }
    }
  }
  
  public boolean isClosed()
  {
    return this.closed;
  }
  
  public void setThrottle(int paramInt)
  {
    this.throttle = paramInt;
  }
  
  public void throttle()
  {
    if (this.currentCommandStart == 0L) {
      this.currentCommandStart = System.currentTimeMillis();
    }
    if (this.throttle == 0) {
      return;
    }
    long l = System.currentTimeMillis();
    if (this.lastThrottle + 50L > l) {
      return;
    }
    this.lastThrottle = (l + this.throttle);
    try
    {
      Thread.sleep(this.throttle);
    }
    catch (Exception localException) {}
  }
  
  public void setCurrentCommand(Command paramCommand)
  {
    this.currentCommand = paramCommand;
    if ((this.queryTimeout > 0) && (paramCommand != null))
    {
      long l = System.currentTimeMillis();
      this.currentCommandStart = l;
      this.cancelAt = (l + this.queryTimeout);
    }
  }
  
  public void checkCanceled()
  {
    throttle();
    if (this.cancelAt == 0L) {
      return;
    }
    long l = System.currentTimeMillis();
    if (l >= this.cancelAt)
    {
      this.cancelAt = 0L;
      throw DbException.get(57014);
    }
  }
  
  public long getCancel()
  {
    return this.cancelAt;
  }
  
  public Command getCurrentCommand()
  {
    return this.currentCommand;
  }
  
  public long getCurrentCommandStart()
  {
    return this.currentCommandStart;
  }
  
  public boolean getAllowLiterals()
  {
    return this.allowLiterals;
  }
  
  public void setAllowLiterals(boolean paramBoolean)
  {
    this.allowLiterals = paramBoolean;
  }
  
  public void setCurrentSchema(Schema paramSchema)
  {
    this.modificationId += 1;
    this.currentSchemaName = paramSchema.getName();
  }
  
  public String getCurrentSchemaName()
  {
    return this.currentSchemaName;
  }
  
  public JdbcConnection createConnection(boolean paramBoolean)
  {
    String str;
    if (paramBoolean) {
      str = "jdbc:columnlist:connection";
    } else {
      str = "jdbc:default:connection";
    }
    return new JdbcConnection(this, getUser().getName(), str);
  }
  
  public DataHandler getDataHandler()
  {
    return this.database;
  }
  
  public void unlinkAtCommit(Value paramValue)
  {
    if ((SysProperties.CHECK) && (!paramValue.isLinked())) {
      DbException.throwInternalError();
    }
    if (this.unlinkLobMap == null) {
      this.unlinkLobMap = New.hashMap();
    }
    this.unlinkLobMap.put(paramValue.toString(), paramValue);
  }
  
  public void unlinkAtCommitStop(Value paramValue)
  {
    if (this.unlinkLobMap != null) {
      this.unlinkLobMap.remove(paramValue.toString());
    }
  }
  
  public String getNextSystemIdentifier(String paramString)
  {
    String str;
    do
    {
      str = "_" + this.systemIdentifier++;
    } while (paramString.contains(str));
    return str;
  }
  
  public void addProcedure(Procedure paramProcedure)
  {
    if (this.procedures == null) {
      this.procedures = this.database.newStringMap();
    }
    this.procedures.put(paramProcedure.getName(), paramProcedure);
  }
  
  public void removeProcedure(String paramString)
  {
    if (this.procedures != null) {
      this.procedures.remove(paramString);
    }
  }
  
  public Procedure getProcedure(String paramString)
  {
    if (this.procedures == null) {
      return null;
    }
    return (Procedure)this.procedures.get(paramString);
  }
  
  public void setSchemaSearchPath(String[] paramArrayOfString)
  {
    this.modificationId += 1;
    this.schemaSearchPath = paramArrayOfString;
  }
  
  public String[] getSchemaSearchPath()
  {
    return this.schemaSearchPath;
  }
  
  public int hashCode()
  {
    return this.serialId;
  }
  
  public String toString()
  {
    return "#" + this.serialId + " (user: " + this.user.getName() + ")";
  }
  
  public void setUndoLogEnabled(boolean paramBoolean)
  {
    this.undoLogEnabled = paramBoolean;
  }
  
  public void setRedoLogBinary(boolean paramBoolean)
  {
    this.redoLogBinary = paramBoolean;
  }
  
  public boolean isUndoLogEnabled()
  {
    return this.undoLogEnabled;
  }
  
  public void begin()
  {
    this.autoCommitAtTransactionEnd = true;
    this.autoCommit = false;
  }
  
  public long getSessionStart()
  {
    return this.sessionStart;
  }
  
  public long getTransactionStart()
  {
    if (this.transactionStart == 0L) {
      this.transactionStart = System.currentTimeMillis();
    }
    return this.transactionStart;
  }
  
  public Table[] getLocks()
  {
    ArrayList localArrayList = New.arrayList();
    for (int i = 0; i < this.locks.size(); i++) {
      try
      {
        localArrayList.add(this.locks.get(i));
      }
      catch (Exception localException)
      {
        break;
      }
    }
    Table[] arrayOfTable = new Table[localArrayList.size()];
    localArrayList.toArray(arrayOfTable);
    return arrayOfTable;
  }
  
  public void waitIfExclusiveModeEnabled()
  {
    if (this.database.getLobSession() == this) {
      return;
    }
    for (;;)
    {
      Session localSession = this.database.getExclusiveSession();
      if ((localSession == null) || (localSession == this)) {
        break;
      }
      if (Thread.holdsLock(localSession)) {
        break;
      }
      try
      {
        Thread.sleep(100L);
      }
      catch (InterruptedException localInterruptedException) {}
    }
  }
  
  public void addTemporaryResult(LocalResult paramLocalResult)
  {
    if (!paramLocalResult.needToClose()) {
      return;
    }
    if (this.temporaryResults == null) {
      this.temporaryResults = New.hashSet();
    }
    if (this.temporaryResults.size() < 100) {
      this.temporaryResults.add(paramLocalResult);
    }
  }
  
  private void closeTemporaryResults()
  {
    if (this.temporaryResults != null)
    {
      for (LocalResult localLocalResult : this.temporaryResults) {
        localLocalResult.close();
      }
      this.temporaryResults = null;
    }
  }
  
  public void setQueryTimeout(int paramInt)
  {
    int i = this.database.getSettings().maxQueryTimeout;
    if ((i != 0) && ((i < paramInt) || (paramInt == 0))) {
      paramInt = i;
    }
    this.queryTimeout = paramInt;
    
    this.cancelAt = 0L;
  }
  
  public int getQueryTimeout()
  {
    return this.queryTimeout;
  }
  
  public void setWaitForLock(Table paramTable, Thread paramThread)
  {
    this.waitForLock = paramTable;
    this.waitForLockThread = paramThread;
  }
  
  public Table getWaitForLock()
  {
    return this.waitForLock;
  }
  
  public Thread getWaitForLockThread()
  {
    return this.waitForLockThread;
  }
  
  public int getModificationId()
  {
    return this.modificationId;
  }
  
  public boolean isReconnectNeeded(boolean paramBoolean)
  {
    for (;;)
    {
      boolean bool = this.database.isReconnectNeeded();
      if (bool) {
        return true;
      }
      if (paramBoolean)
      {
        if (this.database.beforeWriting()) {
          return false;
        }
      }
      else {
        return false;
      }
    }
  }
  
  public void afterWriting()
  {
    this.database.afterWriting();
  }
  
  public SessionInterface reconnect(boolean paramBoolean)
  {
    readSessionState();
    close();
    Session localSession = Engine.getInstance().createSession(this.connectionInfo);
    localSession.sessionState = this.sessionState;
    localSession.recreateSessionState();
    while ((paramBoolean) && 
      (!localSession.database.beforeWriting())) {}
    return localSession;
  }
  
  public void setConnectionInfo(ConnectionInfo paramConnectionInfo)
  {
    this.connectionInfo = paramConnectionInfo;
  }
  
  public Value getTransactionId()
  {
    if (this.database.getMvStore() != null)
    {
      if (this.transaction == null) {
        return ValueNull.INSTANCE;
      }
      return ValueString.get(Long.toString(getTransaction().getId()));
    }
    if (!this.database.isPersistent()) {
      return ValueNull.INSTANCE;
    }
    if (this.undoLog.size() == 0) {
      return ValueNull.INSTANCE;
    }
    return ValueString.get(this.firstUncommittedLog + "-" + this.firstUncommittedPos + "-" + this.id);
  }
  
  public int nextObjectId()
  {
    return this.objectId++;
  }
  
  public boolean isRedoLogBinaryEnabled()
  {
    return this.redoLogBinary;
  }
  
  public TransactionStore.Transaction getTransaction()
  {
    if (this.transaction == null)
    {
      if (this.database.getMvStore().getStore().isClosed())
      {
        this.database.shutdownImmediately();
        throw DbException.get(90098);
      }
      this.transaction = this.database.getMvStore().getTransactionStore().begin();
      this.startStatement = -1L;
    }
    return this.transaction;
  }
  
  public long getStatementSavepoint()
  {
    if (this.startStatement == -1L) {
      this.startStatement = getTransaction().setSavepoint();
    }
    return this.startStatement;
  }
  
  public void startStatementWithinTransaction()
  {
    this.startStatement = -1L;
  }
  
  public void endStatement()
  {
    this.startStatement = -1L;
    closeTemporaryResults();
  }
  
  public void addTemporaryLob(Value paramValue)
  {
    if (this.temporaryLobs == null) {
      this.temporaryLobs = new ArrayList();
    }
    this.temporaryLobs.add(paramValue);
  }
  
  public static class Savepoint
  {
    int logIndex;
    long transactionSavepoint;
  }
}
