package org.h2.engine;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import org.h2.api.DatabaseEventListener;
import org.h2.api.JavaObjectSerializer;
import org.h2.command.ddl.CreateTableData;
import org.h2.command.dml.SetTypes;
import org.h2.constraint.Constraint;
import org.h2.index.Cursor;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.message.TraceSystem;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.db.MVTableEngine;
import org.h2.mvstore.db.MVTableEngine.Store;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.schema.Sequence;
import org.h2.schema.TriggerObject;
import org.h2.store.DataHandler;
import org.h2.store.FileLock;
import org.h2.store.FileStore;
import org.h2.store.InDoubtTransaction;
import org.h2.store.LobStorageBackend;
import org.h2.store.LobStorageInterface;
import org.h2.store.LobStorageMap;
import org.h2.store.PageStore;
import org.h2.store.WriterThread;
import org.h2.store.fs.FileUtils;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.MetaTable;
import org.h2.table.Table;
import org.h2.table.TableLinkConnection;
import org.h2.table.TableView;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Server;
import org.h2.util.BitField;
import org.h2.util.Cache;
import org.h2.util.JdbcUtils;
import org.h2.util.MathUtils;
import org.h2.util.NetUtils;
import org.h2.util.New;
import org.h2.util.SmallLRUCache;
import org.h2.util.SourceCompiler;
import org.h2.util.StringUtils;
import org.h2.util.TempFileDeleter;
import org.h2.util.Utils;
import org.h2.value.CaseInsensitiveMap;
import org.h2.value.CompareMode;
import org.h2.value.Value;
import org.h2.value.ValueInt;

public class Database
  implements DataHandler
{
  private static int initialPowerOffCount;
  private static final String SYSTEM_USER_NAME = "DBA";
  private final boolean persistent;
  private final String databaseName;
  private final String databaseShortName;
  private final String databaseURL;
  private final String cipher;
  private final byte[] filePasswordHash;
  private final byte[] fileEncryptionKey;
  private final HashMap<String, Role> roles = New.hashMap();
  private final HashMap<String, User> users = New.hashMap();
  private final HashMap<String, Setting> settings = New.hashMap();
  private final HashMap<String, Schema> schemas = New.hashMap();
  private final HashMap<String, Right> rights = New.hashMap();
  private final HashMap<String, UserDataType> userDataTypes = New.hashMap();
  private final HashMap<String, UserAggregate> aggregates = New.hashMap();
  private final HashMap<String, Comment> comments = New.hashMap();
  private final Set<Session> userSessions = Collections.synchronizedSet(new HashSet());
  private Session exclusiveSession;
  private final BitField objectIds = new BitField();
  private final Object lobSyncObject = new Object();
  private Schema mainSchema;
  private Schema infoSchema;
  private int nextSessionId;
  private int nextTempTableId;
  private User systemUser;
  private Session systemSession;
  private Session lobSession;
  private Table meta;
  private Index metaIdIndex;
  private FileLock lock;
  private WriterThread writer;
  private boolean starting;
  private TraceSystem traceSystem;
  private Trace trace;
  private final int fileLockMethod;
  private Role publicRole;
  private long modificationDataId;
  private long modificationMetaId;
  private CompareMode compareMode;
  private String cluster = "''";
  private boolean readOnly;
  private int writeDelay = 500;
  private DatabaseEventListener eventListener;
  private int maxMemoryRows = SysProperties.MAX_MEMORY_ROWS;
  private int maxMemoryUndo = 50000;
  private int lockMode = 3;
  private int maxLengthInplaceLob;
  private int allowLiterals = 2;
  private int powerOffCount = initialPowerOffCount;
  private int closeDelay;
  private DatabaseCloser delayedCloser;
  private volatile boolean closing;
  private boolean ignoreCase;
  private boolean deleteFilesOnDisconnect;
  private String lobCompressionAlgorithm;
  private boolean optimizeReuseResults = true;
  private final String cacheType;
  private final String accessModeData;
  private boolean referentialIntegrity = true;
  private boolean multiVersion;
  private DatabaseCloser closeOnExit;
  private Mode mode = Mode.getInstance("REGULAR");
  private boolean multiThreaded;
  private int maxOperationMemory = 100000;
  private SmallLRUCache<String, String[]> lobFileListCache;
  private final boolean autoServerMode;
  private final int autoServerPort;
  private Server server;
  private HashMap<TableLinkConnection, TableLinkConnection> linkConnections;
  private final TempFileDeleter tempFileDeleter = TempFileDeleter.getInstance();
  private PageStore pageStore;
  private Properties reconnectLastLock;
  private volatile long reconnectCheckNext;
  private volatile boolean reconnectChangePending;
  private volatile int checkpointAllowed;
  private volatile boolean checkpointRunning;
  private final Object reconnectSync = new Object();
  private int cacheSize;
  private int compactMode;
  private SourceCompiler compiler;
  private volatile boolean metaTablesInitialized;
  private boolean flushOnEachCommit;
  private LobStorageInterface lobStorage;
  private final int pageSize;
  private int defaultTableType = 0;
  private final DbSettings dbSettings;
  private final int reconnectCheckDelay;
  private int logMode;
  private MVTableEngine.Store mvStore;
  private int retentionTime;
  private DbException backgroundException;
  private JavaObjectSerializer javaObjectSerializer;
  private String javaObjectSerializerName;
  private volatile boolean javaObjectSerializerInitialized;
  private boolean queryStatistics;
  private QueryStatisticsData queryStatisticsData;
  
  public Database(ConnectionInfo paramConnectionInfo, String paramString)
  {
    String str1 = paramConnectionInfo.getName();
    this.dbSettings = paramConnectionInfo.getDbSettings();
    this.reconnectCheckDelay = this.dbSettings.reconnectCheckDelay;
    this.compareMode = CompareMode.getInstance(null, 0);
    this.persistent = paramConnectionInfo.isPersistent();
    this.filePasswordHash = paramConnectionInfo.getFilePasswordHash();
    this.fileEncryptionKey = paramConnectionInfo.getFileEncryptionKey();
    this.databaseName = str1;
    this.databaseShortName = parseDatabaseShortName();
    this.maxLengthInplaceLob = 128;
    this.cipher = paramString;
    String str2 = paramConnectionInfo.getProperty("FILE_LOCK", null);
    this.accessModeData = StringUtils.toLowerEnglish(paramConnectionInfo.getProperty("ACCESS_MODE_DATA", "rw"));
    
    this.autoServerMode = paramConnectionInfo.getProperty("AUTO_SERVER", false);
    this.autoServerPort = paramConnectionInfo.getProperty("AUTO_SERVER_PORT", 0);
    int i = Utils.scaleForAvailableMemory(65536);
    
    this.cacheSize = paramConnectionInfo.getProperty("CACHE_SIZE", i);
    
    this.pageSize = paramConnectionInfo.getProperty("PAGE_SIZE", 4096);
    if ("r".equals(this.accessModeData)) {
      this.readOnly = true;
    }
    if ((this.dbSettings.mvStore) && (str2 == null))
    {
      if (this.autoServerMode) {
        this.fileLockMethod = 1;
      } else {
        this.fileLockMethod = 4;
      }
    }
    else {
      this.fileLockMethod = FileLock.getFileLockMethod(str2);
    }
    if ((this.dbSettings.mvStore) && (this.fileLockMethod == 3)) {
      throw DbException.getUnsupportedException("MV_STORE combined with FILE_LOCK=SERIALIZED");
    }
    this.databaseURL = paramConnectionInfo.getURL();
    String str3 = paramConnectionInfo.removeProperty("DATABASE_EVENT_LISTENER", null);
    if (str3 != null)
    {
      str3 = StringUtils.trim(str3, true, true, "'");
      setEventListenerClass(str3);
    }
    String str4 = paramConnectionInfo.removeProperty("MODE", null);
    if (str4 != null) {
      this.mode = Mode.getInstance(str4);
    }
    this.multiVersion = paramConnectionInfo.getProperty("MVCC", this.dbSettings.mvStore);
    
    this.logMode = paramConnectionInfo.getProperty("LOG", 2);
    
    this.javaObjectSerializerName = paramConnectionInfo.getProperty("JAVA_OBJECT_SERIALIZER", null);
    
    this.multiThreaded = paramConnectionInfo.getProperty("MULTI_THREADED", false);
    
    boolean bool = this.dbSettings.dbCloseOnExit;
    
    int j = paramConnectionInfo.getIntProperty(10, 1);
    
    int k = paramConnectionInfo.getIntProperty(9, 0);
    
    this.cacheType = StringUtils.toUpperEnglish(paramConnectionInfo.removeProperty("CACHE_TYPE", "LRU"));
    
    openDatabase(j, k, bool);
  }
  
  private void openDatabase(int paramInt1, int paramInt2, boolean paramBoolean)
  {
    try
    {
      open(paramInt1, paramInt2);
      if (paramBoolean) {
        try
        {
          this.closeOnExit = new DatabaseCloser(this, 0, true);
          Runtime.getRuntime().addShutdownHook(this.closeOnExit);
        }
        catch (IllegalStateException localIllegalStateException) {}catch (SecurityException localSecurityException) {}
      }
    }
    catch (Throwable localThrowable)
    {
      if ((localThrowable instanceof OutOfMemoryError)) {
        localThrowable.fillInStackTrace();
      }
      if (this.traceSystem != null)
      {
        if ((localThrowable instanceof SQLException))
        {
          SQLException localSQLException = (SQLException)localThrowable;
          if (localSQLException.getErrorCode() != 90020) {
            this.trace.error(localThrowable, "opening {0}", new Object[] { this.databaseName });
          }
        }
        this.traceSystem.close();
      }
      closeOpenFilesAndUnlock(false);
      throw DbException.convert(localThrowable);
    }
  }
  
  public static void setInitialPowerOffCount(int paramInt)
  {
    initialPowerOffCount = paramInt;
  }
  
  public void setPowerOffCount(int paramInt)
  {
    if (this.powerOffCount == -1) {
      return;
    }
    this.powerOffCount = paramInt;
  }
  
  public MVTableEngine.Store getMvStore()
  {
    return this.mvStore;
  }
  
  public void setMvStore(MVTableEngine.Store paramStore)
  {
    this.mvStore = paramStore;
    this.retentionTime = paramStore.getStore().getRetentionTime();
  }
  
  public boolean areEqual(Value paramValue1, Value paramValue2)
  {
    return paramValue1.compareTo(paramValue2, this.compareMode) == 0;
  }
  
  public int compare(Value paramValue1, Value paramValue2)
  {
    return paramValue1.compareTo(paramValue2, this.compareMode);
  }
  
  public int compareTypeSave(Value paramValue1, Value paramValue2)
  {
    return paramValue1.compareTypeSave(paramValue2, this.compareMode);
  }
  
  public long getModificationDataId()
  {
    return this.modificationDataId;
  }
  
  private synchronized boolean reconnectModified(boolean paramBoolean)
  {
    if ((this.readOnly) || (this.lock == null) || (this.fileLockMethod != 3)) {
      return true;
    }
    try
    {
      Object localObject2;
      if (paramBoolean == this.reconnectChangePending)
      {
        long l = System.currentTimeMillis();
        if (l > this.reconnectCheckNext)
        {
          if (paramBoolean)
          {
            localObject2 = "" + this.pageStore.getWriteCountTotal();
            
            this.lock.setProperty("logPos", (String)localObject2);
            this.lock.save();
          }
          this.reconnectCheckNext = (l + this.reconnectCheckDelay);
        }
        return true;
      }
      Properties localProperties = this.lock.load();
      if (paramBoolean)
      {
        if (localProperties.getProperty("changePending") != null) {
          return false;
        }
        this.trace.debug("wait before writing");
        Thread.sleep((int)(this.reconnectCheckDelay * 1.1D));
        localObject1 = this.lock.load();
        if (!((Properties)localObject1).equals(localProperties)) {
          return false;
        }
      }
      Object localObject1 = "" + this.pageStore.getWriteCountTotal();
      
      this.lock.setProperty("logPos", (String)localObject1);
      if (paramBoolean) {
        this.lock.setProperty("changePending", "true-" + Math.random());
      } else {
        this.lock.setProperty("changePending", null);
      }
      this.reconnectCheckNext = (System.currentTimeMillis() + 2 * this.reconnectCheckDelay);
      
      localProperties = this.lock.save();
      if (paramBoolean)
      {
        this.trace.debug("wait before writing again");
        Thread.sleep((int)(this.reconnectCheckDelay * 1.1D));
        localObject2 = this.lock.load();
        if (!((Properties)localObject2).equals(localProperties)) {
          return false;
        }
      }
      else
      {
        Thread.sleep(1L);
      }
      this.reconnectLastLock = localProperties;
      this.reconnectChangePending = paramBoolean;
      this.reconnectCheckNext = (System.currentTimeMillis() + this.reconnectCheckDelay);
      
      return true;
    }
    catch (Exception localException)
    {
      this.trace.error(localException, "pending {0}", new Object[] { Boolean.valueOf(paramBoolean) });
    }
    return false;
  }
  
  public long getNextModificationDataId()
  {
    return ++this.modificationDataId;
  }
  
  public long getModificationMetaId()
  {
    return this.modificationMetaId;
  }
  
  public long getNextModificationMetaId()
  {
    this.modificationDataId += 1L;
    return this.modificationMetaId++;
  }
  
  public int getPowerOffCount()
  {
    return this.powerOffCount;
  }
  
  public void checkPowerOff()
  {
    if (this.powerOffCount == 0) {
      return;
    }
    if (this.powerOffCount > 1)
    {
      this.powerOffCount -= 1;
      return;
    }
    if (this.powerOffCount != -1) {
      try
      {
        this.powerOffCount = -1;
        stopWriter();
        if (this.mvStore != null) {
          this.mvStore.closeImmediately();
        }
        if (this.pageStore != null)
        {
          try
          {
            this.pageStore.close();
          }
          catch (DbException localDbException1) {}
          this.pageStore = null;
        }
        if (this.lock != null)
        {
          stopServer();
          if (this.fileLockMethod != 3) {
            this.lock.unlock();
          }
          this.lock = null;
        }
        if (this.traceSystem != null) {
          this.traceSystem.close();
        }
      }
      catch (DbException localDbException2)
      {
        DbException.traceThrowable(localDbException2);
      }
    }
    Engine.getInstance().close(this.databaseName);
    throw DbException.get(90098);
  }
  
  static boolean exists(String paramString)
  {
    if (FileUtils.exists(paramString + ".h2.db")) {
      return true;
    }
    return FileUtils.exists(paramString + ".mv.db");
  }
  
  public Trace getTrace(String paramString)
  {
    return this.traceSystem.getTrace(paramString);
  }
  
  public FileStore openFile(String paramString1, String paramString2, boolean paramBoolean)
  {
    if ((paramBoolean) && (!FileUtils.exists(paramString1))) {
      throw DbException.get(90124, paramString1);
    }
    FileStore localFileStore = FileStore.open(this, paramString1, paramString2, this.cipher, this.filePasswordHash);
    try
    {
      localFileStore.init();
    }
    catch (DbException localDbException)
    {
      localFileStore.closeSilently();
      throw localDbException;
    }
    return localFileStore;
  }
  
  boolean validateFilePasswordHash(String paramString, byte[] paramArrayOfByte)
  {
    if (!StringUtils.equals(paramString, this.cipher)) {
      return false;
    }
    return Utils.compareSecure(paramArrayOfByte, this.filePasswordHash);
  }
  
  private String parseDatabaseShortName()
  {
    String str = this.databaseName;
    if (str.endsWith(":")) {
      str = null;
    }
    if (str != null)
    {
      StringTokenizer localStringTokenizer = new StringTokenizer(str, "/\\:,;");
      while (localStringTokenizer.hasMoreTokens()) {
        str = localStringTokenizer.nextToken();
      }
    }
    if ((str == null) || (str.length() == 0)) {
      str = "unnamed";
    }
    return this.dbSettings.databaseToUpper ? StringUtils.toUpperEnglish(str) : str;
  }
  
  private synchronized void open(int paramInt1, int paramInt2)
  {
    if (this.persistent)
    {
      localObject1 = this.databaseName + ".data.db";
      boolean bool1 = FileUtils.exists((String)localObject1);
      localObject2 = this.databaseName + ".h2.db";
      String str = this.databaseName + ".mv.db";
      boolean bool3 = FileUtils.exists((String)localObject2);
      boolean bool4 = FileUtils.exists(str);
      if ((bool1) && (!bool3) && (!bool4)) {
        throw DbException.get(90048, "Old database: " + (String)localObject1 + " - please convert the database " + "to a SQL script and re-create it.");
      }
      if ((bool3) && (!FileUtils.canWrite((String)localObject2))) {
        this.readOnly = true;
      }
      if ((bool4) && (!FileUtils.canWrite(str))) {
        this.readOnly = true;
      }
      if ((bool3) && (!bool4)) {
        this.dbSettings.mvStore = false;
      }
      if (this.readOnly)
      {
        if (paramInt1 >= 3)
        {
          localObject3 = Utils.getProperty("java.io.tmpdir", ".") + "/" + "h2_" + System.currentTimeMillis();
          
          this.traceSystem = new TraceSystem((String)localObject3 + ".trace.db");
        }
        else
        {
          this.traceSystem = new TraceSystem(null);
        }
      }
      else {
        this.traceSystem = new TraceSystem(this.databaseName + ".trace.db");
      }
      this.traceSystem.setLevelFile(paramInt1);
      this.traceSystem.setLevelSystemOut(paramInt2);
      this.trace = this.traceSystem.getTrace("database");
      this.trace.info("opening {0} (build {1})", new Object[] { this.databaseName, Integer.valueOf(183) });
      if ((this.autoServerMode) && (
        (this.readOnly) || (this.fileLockMethod == 0) || (this.fileLockMethod == 3) || (this.fileLockMethod == 4) || (!this.persistent))) {
        throw DbException.getUnsupportedException("autoServerMode && (readOnly || fileLockMethod == NO || fileLockMethod == SERIALIZED || inMemory)");
      }
      localObject3 = this.databaseName + ".lock.db";
      if ((this.readOnly) && 
        (FileUtils.exists((String)localObject3))) {
        throw DbException.get(90020, "Lock file exists: " + (String)localObject3);
      }
      if ((!this.readOnly) && (this.fileLockMethod != 0) && 
        (this.fileLockMethod != 4))
      {
        this.lock = new FileLock(this.traceSystem, (String)localObject3, 1000);
        this.lock.lock(this.fileLockMethod);
        if (this.autoServerMode) {
          startServer(this.lock.getUniqueId());
        }
      }
      if (SysProperties.MODIFY_ON_WRITE) {
        while (isReconnectNeeded()) {}
      }
      while ((isReconnectNeeded()) && (!beforeWriting())) {}
      deleteOldTempFiles();
      this.starting = true;
      if (SysProperties.MODIFY_ON_WRITE) {
        try
        {
          getPageStore();
        }
        catch (DbException localDbException)
        {
          if (localDbException.getErrorCode() != 90097) {
            throw localDbException;
          }
          this.pageStore = null;
          while (!beforeWriting()) {}
          getPageStore();
        }
      } else {
        getPageStore();
      }
      this.starting = false;
      if (this.mvStore == null) {
        this.writer = WriterThread.create(this, this.writeDelay);
      } else {
        setWriteDelay(this.writeDelay);
      }
    }
    else
    {
      if (this.autoServerMode) {
        throw DbException.getUnsupportedException("autoServerMode && inMemory");
      }
      this.traceSystem = new TraceSystem(null);
      this.trace = this.traceSystem.getTrace("database");
      if (this.dbSettings.mvStore) {
        getPageStore();
      }
    }
    this.systemUser = new User(this, 0, "DBA", true);
    this.mainSchema = new Schema(this, 0, "PUBLIC", this.systemUser, true);
    this.infoSchema = new Schema(this, -1, "INFORMATION_SCHEMA", this.systemUser, true);
    this.schemas.put(this.mainSchema.getName(), this.mainSchema);
    this.schemas.put(this.infoSchema.getName(), this.infoSchema);
    this.publicRole = new Role(this, 0, "PUBLIC", true);
    this.roles.put("PUBLIC", this.publicRole);
    this.systemUser.setAdmin(true);
    this.systemSession = new Session(this, this.systemUser, ++this.nextSessionId);
    this.lobSession = new Session(this, this.systemUser, ++this.nextSessionId);
    Object localObject1 = new CreateTableData();
    ArrayList localArrayList = ((CreateTableData)localObject1).columns;
    Object localObject2 = new Column("ID", 4);
    ((Column)localObject2).setNullable(false);
    localArrayList.add(localObject2);
    localArrayList.add(new Column("HEAD", 4));
    localArrayList.add(new Column("TYPE", 4));
    localArrayList.add(new Column("SQL", 13));
    boolean bool2 = true;
    if (this.pageStore != null) {
      bool2 = this.pageStore.isNew();
    }
    ((CreateTableData)localObject1).tableName = "SYS";
    ((CreateTableData)localObject1).id = 0;
    ((CreateTableData)localObject1).temporary = false;
    ((CreateTableData)localObject1).persistData = this.persistent;
    ((CreateTableData)localObject1).persistIndexes = this.persistent;
    ((CreateTableData)localObject1).create = bool2;
    ((CreateTableData)localObject1).isHidden = true;
    ((CreateTableData)localObject1).session = this.systemSession;
    this.meta = this.mainSchema.createTable((CreateTableData)localObject1);
    IndexColumn[] arrayOfIndexColumn = IndexColumn.wrap(new Column[] { localObject2 });
    this.metaIdIndex = this.meta.addIndex(this.systemSession, "SYS_ID", 0, arrayOfIndexColumn, IndexType.createPrimaryKey(false, false), true, null);
    
    this.objectIds.set(0);
    this.starting = true;
    Cursor localCursor = this.metaIdIndex.find(this.systemSession, null, null);
    Object localObject3 = New.arrayList();
    while (localCursor.next())
    {
      MetaRecord localMetaRecord1 = new MetaRecord(localCursor.get());
      this.objectIds.set(localMetaRecord1.getId());
      ((ArrayList)localObject3).add(localMetaRecord1);
    }
    Collections.sort((List)localObject3);
    Object localObject4;
    synchronized (this.systemSession)
    {
      for (localObject4 = ((ArrayList)localObject3).iterator(); ((Iterator)localObject4).hasNext();)
      {
        MetaRecord localMetaRecord2 = (MetaRecord)((Iterator)localObject4).next();
        localMetaRecord2.execute(this, this.systemSession, this.eventListener);
      }
    }
    if (this.mvStore != null)
    {
      this.mvStore.initTransactions();
      this.mvStore.removeTemporaryMaps(this.objectIds);
    }
    recompileInvalidViews(this.systemSession);
    this.starting = false;
    if (!this.readOnly)
    {
      ??? = SetTypes.getTypeName(34);
      if (this.settings.get(???) == null)
      {
        localObject4 = new Setting(this, allocateObjectId(), (String)???);
        ((Setting)localObject4).setIntValue(183);
        lockMeta(this.systemSession);
        addDatabaseObject(this.systemSession, (DbObject)localObject4);
      }
      if (this.pageStore != null)
      {
        localObject4 = this.pageStore.getObjectIds();
        int i = 0;
        for (int j = ((BitField)localObject4).length(); i < j; i++) {
          if ((((BitField)localObject4).get(i)) && (!this.objectIds.get(i)))
          {
            this.trace.info("unused object id: " + i);
            this.objectIds.set(i);
          }
        }
      }
    }
    getLobStorage().init();
    this.systemSession.commit(true);
    
    this.trace.info("opened {0}", new Object[] { this.databaseName });
    if (this.checkpointAllowed > 0) {
      afterWriting();
    }
  }
  
  private void startServer(String paramString)
  {
    try
    {
      this.server = Server.createTcpServer(new String[] { "-tcpPort", Integer.toString(this.autoServerPort), "-tcpAllowOthers", "-tcpDaemon", "-key", paramString, this.databaseName });
      
      this.server.start();
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
    String str = NetUtils.getLocalAddress() + ":" + this.server.getPort();
    this.lock.setProperty("server", str);
    this.lock.save();
  }
  
  private void stopServer()
  {
    if (this.server != null)
    {
      Server localServer = this.server;
      
      this.server = null;
      localServer.stop();
    }
  }
  
  private void recompileInvalidViews(Session paramSession)
  {
    int i;
    Table localTable;
    TableView localTableView;
    do
    {
      i = 0;
      for (localIterator = getAllTablesAndViews(false).iterator(); localIterator.hasNext();)
      {
        localTable = (Table)localIterator.next();
        if ((localTable instanceof TableView))
        {
          localTableView = (TableView)localTable;
          if (localTableView.isInvalid())
          {
            localTableView.recompile(paramSession, true);
            if (!localTableView.isInvalid()) {
              i = 1;
            }
          }
        }
      }
    } while (i != 0);
    for (Iterator localIterator = getAllTablesAndViews(false).iterator(); localIterator.hasNext();)
    {
      localTable = (Table)localIterator.next();
      if ((localTable instanceof TableView))
      {
        localTableView = (TableView)localTable;
        if (!localTableView.isInvalid()) {
          localTableView.recompile(this.systemSession, true);
        }
      }
    }
  }
  
  private void initMetaTables()
  {
    if (this.metaTablesInitialized) {
      return;
    }
    synchronized (this.infoSchema)
    {
      if (!this.metaTablesInitialized)
      {
        int i = 0;int j = MetaTable.getMetaTableTypeCount();
        for (; i < j; i++)
        {
          MetaTable localMetaTable = new MetaTable(this.infoSchema, -1 - i, i);
          this.infoSchema.add(localMetaTable);
        }
        this.metaTablesInitialized = true;
      }
    }
  }
  
  private synchronized void addMeta(Session paramSession, DbObject paramDbObject)
  {
    int i = paramDbObject.getId();
    if ((i > 0) && (!this.starting) && (!paramDbObject.isTemporary()))
    {
      Row localRow = this.meta.getTemplateRow();
      MetaRecord localMetaRecord = new MetaRecord(paramDbObject);
      localMetaRecord.setRecord(localRow);
      this.objectIds.set(i);
      if (SysProperties.CHECK) {
        verifyMetaLocked(paramSession);
      }
      this.meta.addRow(paramSession, localRow);
      if (isMultiVersion()) {
        paramSession.log(this.meta, (short)0, localRow);
      }
    }
  }
  
  public void verifyMetaLocked(Session paramSession)
  {
    if ((this.meta != null) && (!this.meta.isLockedExclusivelyBy(paramSession)) && (this.lockMode != 0)) {
      throw DbException.throwInternalError();
    }
  }
  
  public boolean lockMeta(Session paramSession)
  {
    if (this.meta == null) {
      return true;
    }
    boolean bool = this.meta.lock(paramSession, true, true);
    return bool;
  }
  
  public synchronized void removeMeta(Session paramSession, int paramInt)
  {
    if ((paramInt > 0) && (!this.starting))
    {
      SearchRow localSearchRow = this.meta.getTemplateSimpleRow(false);
      localSearchRow.setValue(0, ValueInt.get(paramInt));
      boolean bool = lockMeta(paramSession);
      Cursor localCursor = this.metaIdIndex.find(paramSession, localSearchRow, localSearchRow);
      if (localCursor.next())
      {
        if ((SysProperties.CHECK) && 
          (this.lockMode != 0) && (!bool)) {
          throw DbException.throwInternalError();
        }
        Row localRow = localCursor.get();
        this.meta.removeRow(paramSession, localRow);
        if (isMultiVersion()) {
          paramSession.log(this.meta, (short)1, localRow);
        }
        this.objectIds.clear(paramInt);
        if (SysProperties.CHECK) {
          checkMetaFree(paramSession, paramInt);
        }
      }
      else if (!bool)
      {
        this.meta.unlock(paramSession);
        paramSession.unlock(this.meta);
      }
    }
  }
  
  private HashMap<String, DbObject> getMap(int paramInt)
  {
    HashMap localHashMap;
    switch (paramInt)
    {
    case 2: 
      localHashMap = this.users;
      break;
    case 6: 
      localHashMap = this.settings;
      break;
    case 7: 
      localHashMap = this.roles;
      break;
    case 8: 
      localHashMap = this.rights;
      break;
    case 10: 
      localHashMap = this.schemas;
      break;
    case 12: 
      localHashMap = this.userDataTypes;
      break;
    case 13: 
      localHashMap = this.comments;
      break;
    case 14: 
      localHashMap = this.aggregates;
      break;
    case 3: 
    case 4: 
    case 5: 
    case 9: 
    case 11: 
    default: 
      throw DbException.throwInternalError("type=" + paramInt);
    }
    return localHashMap;
  }
  
  public synchronized void addSchemaObject(Session paramSession, SchemaObject paramSchemaObject)
  {
    int i = paramSchemaObject.getId();
    if ((i > 0) && (!this.starting)) {
      checkWritingAllowed();
    }
    lockMeta(paramSession);
    paramSchemaObject.getSchema().add(paramSchemaObject);
    addMeta(paramSession, paramSchemaObject);
  }
  
  public synchronized void addDatabaseObject(Session paramSession, DbObject paramDbObject)
  {
    int i = paramDbObject.getId();
    if ((i > 0) && (!this.starting)) {
      checkWritingAllowed();
    }
    HashMap localHashMap = getMap(paramDbObject.getType());
    if (paramDbObject.getType() == 2)
    {
      localObject = (User)paramDbObject;
      if ((((User)localObject).isAdmin()) && (this.systemUser.getName().equals("DBA"))) {
        this.systemUser.rename(((User)localObject).getName());
      }
    }
    Object localObject = paramDbObject.getName();
    if ((SysProperties.CHECK) && (localHashMap.get(localObject) != null)) {
      DbException.throwInternalError("object already exists");
    }
    lockMeta(paramSession);
    addMeta(paramSession, paramDbObject);
    localHashMap.put(localObject, paramDbObject);
  }
  
  public UserAggregate findAggregate(String paramString)
  {
    return (UserAggregate)this.aggregates.get(paramString);
  }
  
  public Comment findComment(DbObject paramDbObject)
  {
    if (paramDbObject.getType() == 13) {
      return null;
    }
    String str = Comment.getKey(paramDbObject);
    return (Comment)this.comments.get(str);
  }
  
  public Role findRole(String paramString)
  {
    return (Role)this.roles.get(paramString);
  }
  
  public Schema findSchema(String paramString)
  {
    Schema localSchema = (Schema)this.schemas.get(paramString);
    if (localSchema == this.infoSchema) {
      initMetaTables();
    }
    return localSchema;
  }
  
  public Setting findSetting(String paramString)
  {
    return (Setting)this.settings.get(paramString);
  }
  
  public User findUser(String paramString)
  {
    return (User)this.users.get(paramString);
  }
  
  public UserDataType findUserDataType(String paramString)
  {
    return (UserDataType)this.userDataTypes.get(paramString);
  }
  
  public User getUser(String paramString)
  {
    User localUser = findUser(paramString);
    if (localUser == null) {
      throw DbException.get(90032, paramString);
    }
    return localUser;
  }
  
  synchronized Session createSession(User paramUser)
  {
    if (this.exclusiveSession != null) {
      throw DbException.get(90135);
    }
    Session localSession = new Session(this, paramUser, ++this.nextSessionId);
    this.userSessions.add(localSession);
    this.trace.info("connecting session #{0} to {1}", new Object[] { Integer.valueOf(localSession.getId()), this.databaseName });
    if (this.delayedCloser != null)
    {
      this.delayedCloser.reset();
      this.delayedCloser = null;
    }
    return localSession;
  }
  
  public synchronized void removeSession(Session paramSession)
  {
    if (paramSession != null)
    {
      if (this.exclusiveSession == paramSession) {
        this.exclusiveSession = null;
      }
      this.userSessions.remove(paramSession);
      if ((paramSession != this.systemSession) && (paramSession != this.lobSession)) {
        this.trace.info("disconnecting session #{0}", new Object[] { Integer.valueOf(paramSession.getId()) });
      }
    }
    if ((this.userSessions.size() == 0) && (paramSession != this.systemSession) && (paramSession != this.lobSession)) {
      if (this.closeDelay == 0)
      {
        close(false);
      }
      else
      {
        if (this.closeDelay < 0) {
          return;
        }
        this.delayedCloser = new DatabaseCloser(this, this.closeDelay * 1000, false);
        this.delayedCloser.setName("H2 Close Delay " + getShortName());
        this.delayedCloser.setDaemon(true);
        this.delayedCloser.start();
      }
    }
    if ((paramSession != this.systemSession) && (paramSession != this.lobSession) && (paramSession != null)) {
      this.trace.info("disconnected session #{0}", new Object[] { Integer.valueOf(paramSession.getId()) });
    }
  }
  
  private synchronized void closeAllSessionsException(Session paramSession)
  {
    Session[] arrayOfSession1 = new Session[this.userSessions.size()];
    this.userSessions.toArray(arrayOfSession1);
    for (Session localSession : arrayOfSession1) {
      if (localSession != paramSession) {
        try
        {
          localSession.rollback();
          localSession.close();
        }
        catch (DbException localDbException)
        {
          this.trace.error(localDbException, "disconnecting session #{0}", new Object[] { Integer.valueOf(localSession.getId()) });
        }
      }
    }
  }
  
  synchronized void close(boolean paramBoolean)
  {
    if (this.closing) {
      return;
    }
    throwLastBackgroundException();
    if ((this.fileLockMethod == 3) && (!this.reconnectChangePending))
    {
      try
      {
        closeOpenFilesAndUnlock(false);
      }
      catch (DbException localDbException1) {}
      this.traceSystem.close();
      Engine.getInstance().close(this.databaseName);
      return;
    }
    this.closing = true;
    stopServer();
    if (this.userSessions.size() > 0)
    {
      if (!paramBoolean) {
        return;
      }
      this.trace.info("closing {0} from shutdown hook", new Object[] { this.databaseName });
      closeAllSessionsException(null);
    }
    this.trace.info("closing {0}", new Object[] { this.databaseName });
    if (this.eventListener != null)
    {
      this.closing = false;
      DatabaseEventListener localDatabaseEventListener = this.eventListener;
      
      this.eventListener = null;
      localDatabaseEventListener.closingDatabase();
      if (this.userSessions.size() > 0) {
        return;
      }
      this.closing = true;
    }
    if (this.persistent)
    {
      int i = this.infoSchema.findTableOrView(this.systemSession, "LOB_DATA") != null ? 1 : 0;
      if (i != 0) {
        try
        {
          getLobStorage();
          this.lobStorage.removeAllForTable(-1);
        }
        catch (DbException localDbException4)
        {
          this.trace.error(localDbException4, "close");
        }
      }
    }
    Object localObject1;
    try
    {
      if (this.systemSession != null)
      {
        if (this.powerOffCount != -1)
        {
          for (localIterator = getAllTablesAndViews(false).iterator(); localIterator.hasNext();)
          {
            localObject1 = (Table)localIterator.next();
            if (((Table)localObject1).isGlobalTemporary()) {
              ((Table)localObject1).removeChildrenAndResources(this.systemSession);
            } else {
              ((Table)localObject1).close(this.systemSession);
            }
          }
          for (localIterator = getAllSchemaObjects(3).iterator(); localIterator.hasNext();)
          {
            localObject1 = (SchemaObject)localIterator.next();
            
            localObject2 = (Sequence)localObject1;
            ((Sequence)localObject2).close();
          }
        }
        Object localObject2;
        for (Iterator localIterator = getAllSchemaObjects(4).iterator(); localIterator.hasNext();)
        {
          localObject1 = (SchemaObject)localIterator.next();
          
          localObject2 = (TriggerObject)localObject1;
          try
          {
            ((TriggerObject)localObject2).close();
          }
          catch (SQLException localSQLException)
          {
            this.trace.error(localSQLException, "close");
          }
        }
        if (this.powerOffCount != -1)
        {
          this.meta.close(this.systemSession);
          this.systemSession.commit(true);
        }
      }
    }
    catch (DbException localDbException2)
    {
      this.trace.error(localDbException2, "close");
    }
    this.tempFileDeleter.deleteAll();
    try
    {
      closeOpenFilesAndUnlock(true);
    }
    catch (DbException localDbException3)
    {
      this.trace.error(localDbException3, "close");
    }
    this.trace.info("closed");
    this.traceSystem.close();
    if (this.closeOnExit != null)
    {
      this.closeOnExit.reset();
      try
      {
        Runtime.getRuntime().removeShutdownHook(this.closeOnExit);
      }
      catch (IllegalStateException localIllegalStateException) {}catch (SecurityException localSecurityException) {}
      this.closeOnExit = null;
    }
    Engine.getInstance().close(this.databaseName);
    if ((this.deleteFilesOnDisconnect) && (this.persistent))
    {
      this.deleteFilesOnDisconnect = false;
      try
      {
        String str = FileUtils.getParent(this.databaseName);
        localObject1 = FileUtils.getName(this.databaseName);
        DeleteDbFiles.execute(str, (String)localObject1, true);
      }
      catch (Exception localException) {}
    }
  }
  
  private void stopWriter()
  {
    if (this.writer != null)
    {
      this.writer.stopThread();
      this.writer = null;
    }
  }
  
  private synchronized void closeOpenFilesAndUnlock(boolean paramBoolean)
  {
    stopWriter();
    if ((this.pageStore != null) && 
      (paramBoolean)) {
      try
      {
        this.pageStore.checkpoint();
        if (!this.readOnly)
        {
          lockMeta(this.pageStore.getPageStoreSession());
          this.pageStore.compact(this.compactMode);
        }
      }
      catch (DbException localDbException)
      {
        if (SysProperties.CHECK2)
        {
          int i = localDbException.getErrorCode();
          if ((i != 90098) && (i != 50200) && (i != 90031)) {
            localDbException.printStackTrace();
          }
        }
        this.trace.error(localDbException, "close");
      }
      catch (Throwable localThrowable)
      {
        if (SysProperties.CHECK2) {
          localThrowable.printStackTrace();
        }
        this.trace.error(localThrowable, "close");
      }
    }
    reconnectModified(false);
    if (this.mvStore != null)
    {
      long l = this.dbSettings.maxCompactTime;
      if (this.compactMode == 82) {
        this.mvStore.compactFile(this.dbSettings.maxCompactTime);
      } else if (this.compactMode == 84) {
        l = Long.MAX_VALUE;
      } else if (getSettings().defragAlways) {
        l = Long.MAX_VALUE;
      }
      this.mvStore.close(l);
    }
    closeFiles();
    if ((this.persistent) && (this.lock == null) && (this.fileLockMethod != 0) && (this.fileLockMethod != 4)) {
      return;
    }
    if (this.persistent) {
      deleteOldTempFiles();
    }
    if (this.systemSession != null)
    {
      this.systemSession.close();
      this.systemSession = null;
    }
    if (this.lobSession != null)
    {
      this.lobSession.close();
      this.lobSession = null;
    }
    if (this.lock != null)
    {
      if (this.fileLockMethod == 3) {
        if (this.lock.load().containsKey("changePending")) {
          try
          {
            Thread.sleep((int)(this.reconnectCheckDelay * 1.1D));
          }
          catch (InterruptedException localInterruptedException)
          {
            this.trace.error(localInterruptedException, "close");
          }
        }
      }
      this.lock.unlock();
      this.lock = null;
    }
  }
  
  private synchronized void closeFiles()
  {
    try
    {
      if (this.mvStore != null) {
        this.mvStore.closeImmediately();
      }
      if (this.pageStore != null)
      {
        this.pageStore.close();
        this.pageStore = null;
      }
    }
    catch (DbException localDbException)
    {
      this.trace.error(localDbException, "close");
    }
  }
  
  private void checkMetaFree(Session paramSession, int paramInt)
  {
    SearchRow localSearchRow = this.meta.getTemplateSimpleRow(false);
    localSearchRow.setValue(0, ValueInt.get(paramInt));
    Cursor localCursor = this.metaIdIndex.find(paramSession, localSearchRow, localSearchRow);
    if (localCursor.next()) {
      DbException.throwInternalError();
    }
  }
  
  public synchronized int allocateObjectId()
  {
    int i = this.objectIds.nextClearBit(0);
    this.objectIds.set(i);
    return i;
  }
  
  public ArrayList<UserAggregate> getAllAggregates()
  {
    return New.arrayList(this.aggregates.values());
  }
  
  public ArrayList<Comment> getAllComments()
  {
    return New.arrayList(this.comments.values());
  }
  
  public int getAllowLiterals()
  {
    if (this.starting) {
      return 2;
    }
    return this.allowLiterals;
  }
  
  public ArrayList<Right> getAllRights()
  {
    return New.arrayList(this.rights.values());
  }
  
  public ArrayList<Role> getAllRoles()
  {
    return New.arrayList(this.roles.values());
  }
  
  public ArrayList<SchemaObject> getAllSchemaObjects()
  {
    initMetaTables();
    ArrayList localArrayList = New.arrayList();
    for (Schema localSchema : this.schemas.values()) {
      localArrayList.addAll(localSchema.getAll());
    }
    return localArrayList;
  }
  
  public ArrayList<SchemaObject> getAllSchemaObjects(int paramInt)
  {
    if (paramInt == 0) {
      initMetaTables();
    }
    ArrayList localArrayList = New.arrayList();
    for (Schema localSchema : this.schemas.values()) {
      localArrayList.addAll(localSchema.getAll(paramInt));
    }
    return localArrayList;
  }
  
  public ArrayList<Table> getAllTablesAndViews(boolean paramBoolean)
  {
    if (paramBoolean) {
      initMetaTables();
    }
    ArrayList localArrayList = New.arrayList();
    for (Schema localSchema : this.schemas.values()) {
      localArrayList.addAll(localSchema.getAllTablesAndViews());
    }
    return localArrayList;
  }
  
  public ArrayList<Schema> getAllSchemas()
  {
    initMetaTables();
    return New.arrayList(this.schemas.values());
  }
  
  public ArrayList<Setting> getAllSettings()
  {
    return New.arrayList(this.settings.values());
  }
  
  public ArrayList<UserDataType> getAllUserDataTypes()
  {
    return New.arrayList(this.userDataTypes.values());
  }
  
  public ArrayList<User> getAllUsers()
  {
    return New.arrayList(this.users.values());
  }
  
  public String getCacheType()
  {
    return this.cacheType;
  }
  
  public String getCluster()
  {
    return this.cluster;
  }
  
  public CompareMode getCompareMode()
  {
    return this.compareMode;
  }
  
  public String getDatabasePath()
  {
    if (this.persistent) {
      return FileUtils.toRealPath(this.databaseName);
    }
    return null;
  }
  
  public String getShortName()
  {
    return this.databaseShortName;
  }
  
  public String getName()
  {
    return this.databaseName;
  }
  
  public Session[] getSessions(boolean paramBoolean)
  {
    ArrayList localArrayList;
    synchronized (this.userSessions)
    {
      localArrayList = New.arrayList(this.userSessions);
    }
    ??? = this.systemSession;
    Session localSession = this.lobSession;
    if ((paramBoolean) && (??? != null)) {
      localArrayList.add(???);
    }
    if ((paramBoolean) && (localSession != null)) {
      localArrayList.add(localSession);
    }
    Session[] arrayOfSession = new Session[localArrayList.size()];
    localArrayList.toArray(arrayOfSession);
    return arrayOfSession;
  }
  
  public synchronized void updateMeta(Session paramSession, DbObject paramDbObject)
  {
    lockMeta(paramSession);
    int i = paramDbObject.getId();
    removeMeta(paramSession, i);
    addMeta(paramSession, paramDbObject);
  }
  
  public synchronized void renameSchemaObject(Session paramSession, SchemaObject paramSchemaObject, String paramString)
  {
    checkWritingAllowed();
    paramSchemaObject.getSchema().rename(paramSchemaObject, paramString);
    updateMetaAndFirstLevelChildren(paramSession, paramSchemaObject);
  }
  
  private synchronized void updateMetaAndFirstLevelChildren(Session paramSession, DbObject paramDbObject)
  {
    ArrayList localArrayList = paramDbObject.getChildren();
    Comment localComment = findComment(paramDbObject);
    if (localComment != null) {
      DbException.throwInternalError();
    }
    updateMeta(paramSession, paramDbObject);
    if (localArrayList != null) {
      for (DbObject localDbObject : localArrayList) {
        if (localDbObject.getCreateSQL() != null) {
          updateMeta(paramSession, localDbObject);
        }
      }
    }
  }
  
  public synchronized void renameDatabaseObject(Session paramSession, DbObject paramDbObject, String paramString)
  {
    checkWritingAllowed();
    int i = paramDbObject.getType();
    HashMap localHashMap = getMap(i);
    if (SysProperties.CHECK)
    {
      if (!localHashMap.containsKey(paramDbObject.getName())) {
        DbException.throwInternalError("not found: " + paramDbObject.getName());
      }
      if ((paramDbObject.getName().equals(paramString)) || (localHashMap.containsKey(paramString))) {
        DbException.throwInternalError("object already exists: " + paramString);
      }
    }
    paramDbObject.checkRename();
    int j = paramDbObject.getId();
    lockMeta(paramSession);
    removeMeta(paramSession, j);
    localHashMap.remove(paramDbObject.getName());
    paramDbObject.rename(paramString);
    localHashMap.put(paramString, paramDbObject);
    updateMetaAndFirstLevelChildren(paramSession, paramDbObject);
  }
  
  public String createTempFile()
  {
    try
    {
      boolean bool = this.readOnly;
      String str = this.databaseName;
      if (!this.persistent) {
        str = "memFS:" + str;
      }
      return FileUtils.createTempFile(str, ".temp.db", true, bool);
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, this.databaseName);
    }
  }
  
  private void deleteOldTempFiles()
  {
    String str1 = FileUtils.getParent(this.databaseName);
    for (String str2 : FileUtils.newDirectoryStream(str1)) {
      if ((str2.endsWith(".temp.db")) && (str2.startsWith(this.databaseName))) {
        FileUtils.tryDelete(str2);
      }
    }
  }
  
  public Schema getSchema(String paramString)
  {
    Schema localSchema = findSchema(paramString);
    if (localSchema == null) {
      throw DbException.get(90079, paramString);
    }
    return localSchema;
  }
  
  public synchronized void removeDatabaseObject(Session paramSession, DbObject paramDbObject)
  {
    checkWritingAllowed();
    String str = paramDbObject.getName();
    int i = paramDbObject.getType();
    HashMap localHashMap = getMap(i);
    if ((SysProperties.CHECK) && (!localHashMap.containsKey(str))) {
      DbException.throwInternalError("not found: " + str);
    }
    Comment localComment = findComment(paramDbObject);
    lockMeta(paramSession);
    if (localComment != null) {
      removeDatabaseObject(paramSession, localComment);
    }
    int j = paramDbObject.getId();
    paramDbObject.removeChildrenAndResources(paramSession);
    localHashMap.remove(str);
    removeMeta(paramSession, j);
  }
  
  public Table getDependentTable(SchemaObject paramSchemaObject, Table paramTable)
  {
    switch (paramSchemaObject.getType())
    {
    case 1: 
    case 2: 
    case 4: 
    case 5: 
    case 8: 
    case 13: 
      return null;
    }
    HashSet localHashSet = New.hashSet();
    for (Table localTable : getAllTablesAndViews(false)) {
      if ((paramTable != localTable) && 
      
        (!"VIEW".equals(localTable.getTableType())))
      {
        localHashSet.clear();
        localTable.addDependencies(localHashSet);
        if (localHashSet.contains(paramSchemaObject)) {
          return localTable;
        }
      }
    }
    return null;
  }
  
  public synchronized void removeSchemaObject(Session paramSession, SchemaObject paramSchemaObject)
  {
    int i = paramSchemaObject.getType();
    if (i == 0)
    {
      localObject = (Table)paramSchemaObject;
      if ((((Table)localObject).isTemporary()) && (!((Table)localObject).isGlobalTemporary()))
      {
        paramSession.removeLocalTempTable((Table)localObject);
        return;
      }
    }
    else
    {
      Table localTable1;
      if (i == 1)
      {
        localObject = (Index)paramSchemaObject;
        localTable1 = ((Index)localObject).getTable();
        if ((localTable1.isTemporary()) && (!localTable1.isGlobalTemporary()))
        {
          paramSession.removeLocalTempTableIndex((Index)localObject);
          return;
        }
      }
      else if (i == 5)
      {
        localObject = (Constraint)paramSchemaObject;
        localTable1 = ((Constraint)localObject).getTable();
        if ((localTable1.isTemporary()) && (!localTable1.isGlobalTemporary()))
        {
          paramSession.removeLocalTempTableConstraint((Constraint)localObject);
          return;
        }
      }
    }
    checkWritingAllowed();
    lockMeta(paramSession);
    Object localObject = findComment(paramSchemaObject);
    if (localObject != null) {
      removeDatabaseObject(paramSession, (DbObject)localObject);
    }
    paramSchemaObject.getSchema().remove(paramSchemaObject);
    int j = paramSchemaObject.getId();
    if (!this.starting)
    {
      Table localTable2 = getDependentTable(paramSchemaObject, null);
      if (localTable2 != null)
      {
        paramSchemaObject.getSchema().add(paramSchemaObject);
        throw DbException.get(90107, new String[] { paramSchemaObject.getSQL(), localTable2.getSQL() });
      }
      paramSchemaObject.removeChildrenAndResources(paramSession);
    }
    removeMeta(paramSession, j);
  }
  
  public boolean isPersistent()
  {
    return this.persistent;
  }
  
  public TraceSystem getTraceSystem()
  {
    return this.traceSystem;
  }
  
  public synchronized void setCacheSize(int paramInt)
  {
    if (this.starting)
    {
      int i = MathUtils.convertLongToInt(Utils.getMemoryMax()) / 2;
      paramInt = Math.min(paramInt, i);
    }
    this.cacheSize = paramInt;
    if (this.pageStore != null) {
      this.pageStore.getCache().setMaxMemory(paramInt);
    }
    if (this.mvStore != null) {
      this.mvStore.setCacheSize(Math.max(1, paramInt / 1024));
    }
  }
  
  public synchronized void setMasterUser(User paramUser)
  {
    lockMeta(this.systemSession);
    addDatabaseObject(this.systemSession, paramUser);
    this.systemSession.commit(true);
  }
  
  public Role getPublicRole()
  {
    return this.publicRole;
  }
  
  public synchronized String getTempTableName(String paramString, Session paramSession)
  {
    String str;
    do
    {
      str = paramString + "_COPY_" + paramSession.getId() + "_" + this.nextTempTableId++;
    } while (this.mainSchema.findTableOrView(paramSession, str) != null);
    return str;
  }
  
  public void setCompareMode(CompareMode paramCompareMode)
  {
    this.compareMode = paramCompareMode;
  }
  
  public void setCluster(String paramString)
  {
    this.cluster = paramString;
  }
  
  public void checkWritingAllowed()
  {
    if (this.readOnly) {
      throw DbException.get(90097);
    }
    if ((this.fileLockMethod == 3) && 
      (!this.reconnectChangePending)) {
      throw DbException.get(90097);
    }
  }
  
  public boolean isReadOnly()
  {
    return this.readOnly;
  }
  
  public void setWriteDelay(int paramInt)
  {
    this.writeDelay = paramInt;
    if (this.writer != null)
    {
      this.writer.setWriteDelay(paramInt);
      
      this.flushOnEachCommit = (this.writeDelay < 5);
    }
    if (this.mvStore != null)
    {
      int i = paramInt < 0 ? 0 : paramInt;
      this.mvStore.getStore().setAutoCommitDelay(i);
    }
  }
  
  public int getRetentionTime()
  {
    return this.retentionTime;
  }
  
  public void setRetentionTime(int paramInt)
  {
    this.retentionTime = paramInt;
    if (this.mvStore != null) {
      this.mvStore.getStore().setRetentionTime(paramInt);
    }
  }
  
  public boolean getFlushOnEachCommit()
  {
    return this.flushOnEachCommit;
  }
  
  public ArrayList<InDoubtTransaction> getInDoubtTransactions()
  {
    if (this.mvStore != null) {
      return this.mvStore.getInDoubtTransactions();
    }
    return this.pageStore == null ? null : this.pageStore.getInDoubtTransactions();
  }
  
  synchronized void prepareCommit(Session paramSession, String paramString)
  {
    if (this.readOnly) {
      return;
    }
    if (this.mvStore != null)
    {
      this.mvStore.prepareCommit(paramSession, paramString);
      return;
    }
    if (this.pageStore != null)
    {
      this.pageStore.flushLog();
      this.pageStore.prepareCommit(paramSession, paramString);
    }
  }
  
  synchronized void commit(Session paramSession)
  {
    throwLastBackgroundException();
    if (this.readOnly) {
      return;
    }
    if (this.pageStore != null) {
      this.pageStore.commit(paramSession);
    }
    paramSession.setAllCommitted();
  }
  
  private void throwLastBackgroundException()
  {
    if (this.backgroundException != null)
    {
      DbException localDbException = this.backgroundException;
      this.backgroundException = null;
      if (localDbException != null) {
        throw localDbException;
      }
    }
  }
  
  public void setBackgroundException(DbException paramDbException)
  {
    if (this.backgroundException == null)
    {
      this.backgroundException = paramDbException;
      TraceSystem localTraceSystem = getTraceSystem();
      if (localTraceSystem != null) {
        localTraceSystem.getTrace("database").error(paramDbException, "flush");
      }
    }
  }
  
  public synchronized void flush()
  {
    if (this.readOnly) {
      return;
    }
    if (this.pageStore != null) {
      this.pageStore.flushLog();
    }
    if (this.mvStore != null) {
      try
      {
        this.mvStore.flush();
      }
      catch (RuntimeException localRuntimeException)
      {
        this.backgroundException = DbException.convert(localRuntimeException);
        throw localRuntimeException;
      }
    }
  }
  
  public void setEventListener(DatabaseEventListener paramDatabaseEventListener)
  {
    this.eventListener = paramDatabaseEventListener;
  }
  
  public void setEventListenerClass(String paramString)
  {
    if ((paramString == null) || (paramString.length() == 0)) {
      this.eventListener = null;
    } else {
      try
      {
        this.eventListener = ((DatabaseEventListener)JdbcUtils.loadUserClass(paramString).newInstance());
        
        String str = this.databaseURL;
        if (this.cipher != null) {
          str = str + ";CIPHER=" + this.cipher;
        }
        this.eventListener.init(str);
      }
      catch (Throwable localThrowable)
      {
        throw DbException.get(90099, localThrowable, new String[] { paramString, localThrowable.toString() });
      }
    }
  }
  
  public void setProgress(int paramInt1, String paramString, int paramInt2, int paramInt3)
  {
    if (this.eventListener != null) {
      try
      {
        this.eventListener.setProgress(paramInt1, paramString, paramInt2, paramInt3);
      }
      catch (Exception localException) {}
    }
  }
  
  public void exceptionThrown(SQLException paramSQLException, String paramString)
  {
    if (this.eventListener != null) {
      try
      {
        this.eventListener.exceptionThrown(paramSQLException, paramString);
      }
      catch (Exception localException) {}
    }
  }
  
  public synchronized void sync()
  {
    if (this.readOnly) {
      return;
    }
    if (this.mvStore != null) {
      this.mvStore.sync();
    }
    if (this.pageStore != null) {
      this.pageStore.sync();
    }
  }
  
  public int getMaxMemoryRows()
  {
    return this.maxMemoryRows;
  }
  
  public void setMaxMemoryRows(int paramInt)
  {
    this.maxMemoryRows = paramInt;
  }
  
  public void setMaxMemoryUndo(int paramInt)
  {
    this.maxMemoryUndo = paramInt;
  }
  
  public int getMaxMemoryUndo()
  {
    return this.maxMemoryUndo;
  }
  
  public void setLockMode(int paramInt)
  {
    switch (paramInt)
    {
    case 0: 
      if (this.multiThreaded) {
        throw DbException.get(90021, "LOCK_MODE=0 & MULTI_THREADED");
      }
      break;
    case 1: 
    case 2: 
    case 3: 
      break;
    default: 
      throw DbException.getInvalidValueException("lock mode", Integer.valueOf(paramInt));
    }
    this.lockMode = paramInt;
  }
  
  public int getLockMode()
  {
    return this.lockMode;
  }
  
  public synchronized void setCloseDelay(int paramInt)
  {
    this.closeDelay = paramInt;
  }
  
  public Session getSystemSession()
  {
    return this.systemSession;
  }
  
  public boolean isClosing()
  {
    return this.closing;
  }
  
  public void setMaxLengthInplaceLob(int paramInt)
  {
    this.maxLengthInplaceLob = paramInt;
  }
  
  public int getMaxLengthInplaceLob()
  {
    return this.maxLengthInplaceLob;
  }
  
  public void setIgnoreCase(boolean paramBoolean)
  {
    this.ignoreCase = paramBoolean;
  }
  
  public boolean getIgnoreCase()
  {
    if (this.starting) {
      return false;
    }
    return this.ignoreCase;
  }
  
  public synchronized void setDeleteFilesOnDisconnect(boolean paramBoolean)
  {
    this.deleteFilesOnDisconnect = paramBoolean;
  }
  
  public String getLobCompressionAlgorithm(int paramInt)
  {
    return this.lobCompressionAlgorithm;
  }
  
  public void setLobCompressionAlgorithm(String paramString)
  {
    this.lobCompressionAlgorithm = paramString;
  }
  
  public synchronized void setMaxLogSize(long paramLong)
  {
    if (this.pageStore != null) {
      this.pageStore.setMaxLogSize(paramLong);
    }
  }
  
  public void setAllowLiterals(int paramInt)
  {
    this.allowLiterals = paramInt;
  }
  
  public boolean getOptimizeReuseResults()
  {
    return this.optimizeReuseResults;
  }
  
  public void setOptimizeReuseResults(boolean paramBoolean)
  {
    this.optimizeReuseResults = paramBoolean;
  }
  
  public Object getLobSyncObject()
  {
    return this.lobSyncObject;
  }
  
  public int getSessionCount()
  {
    return this.userSessions.size();
  }
  
  public void setReferentialIntegrity(boolean paramBoolean)
  {
    this.referentialIntegrity = paramBoolean;
  }
  
  public boolean getReferentialIntegrity()
  {
    return this.referentialIntegrity;
  }
  
  public void setQueryStatistics(boolean paramBoolean)
  {
    this.queryStatistics = paramBoolean;
    synchronized (this)
    {
      this.queryStatisticsData = null;
    }
  }
  
  public boolean getQueryStatistics()
  {
    return this.queryStatistics;
  }
  
  public QueryStatisticsData getQueryStatisticsData()
  {
    if (!this.queryStatistics) {
      return null;
    }
    if (this.queryStatisticsData == null) {
      synchronized (this)
      {
        if (this.queryStatisticsData == null) {
          this.queryStatisticsData = new QueryStatisticsData();
        }
      }
    }
    return this.queryStatisticsData;
  }
  
  public boolean isStarting()
  {
    return this.starting;
  }
  
  public boolean isMultiVersion()
  {
    return this.multiVersion;
  }
  
  void opened()
  {
    if (this.eventListener != null) {
      this.eventListener.opened();
    }
    if (this.writer != null) {
      this.writer.startThread();
    }
  }
  
  public void setMode(Mode paramMode)
  {
    this.mode = paramMode;
  }
  
  public Mode getMode()
  {
    return this.mode;
  }
  
  public boolean isMultiThreaded()
  {
    return this.multiThreaded;
  }
  
  public void setMultiThreaded(boolean paramBoolean)
  {
    if ((paramBoolean) && (this.multiThreaded != paramBoolean))
    {
      if ((this.multiVersion) && (this.mvStore == null)) {
        throw DbException.get(90021, "MVCC & MULTI_THREADED");
      }
      if (this.lockMode == 0) {
        throw DbException.get(90021, "LOCK_MODE=0 & MULTI_THREADED");
      }
    }
    this.multiThreaded = paramBoolean;
  }
  
  public void setMaxOperationMemory(int paramInt)
  {
    this.maxOperationMemory = paramInt;
  }
  
  public int getMaxOperationMemory()
  {
    return this.maxOperationMemory;
  }
  
  public Session getExclusiveSession()
  {
    return this.exclusiveSession;
  }
  
  public void setExclusiveSession(Session paramSession, boolean paramBoolean)
  {
    this.exclusiveSession = paramSession;
    if (paramBoolean) {
      closeAllSessionsException(paramSession);
    }
  }
  
  public SmallLRUCache<String, String[]> getLobFileListCache()
  {
    if (this.lobFileListCache == null) {
      this.lobFileListCache = SmallLRUCache.newInstance(128);
    }
    return this.lobFileListCache;
  }
  
  public boolean isSysTableLocked()
  {
    return (this.meta == null) || (this.meta.isLockedExclusively());
  }
  
  public TableLinkConnection getLinkConnection(String paramString1, String paramString2, String paramString3, String paramString4)
  {
    if (this.linkConnections == null) {
      this.linkConnections = New.hashMap();
    }
    return TableLinkConnection.open(this.linkConnections, paramString1, paramString2, paramString3, paramString4, this.dbSettings.shareLinkedConnections);
  }
  
  public String toString()
  {
    return this.databaseShortName + ":" + super.toString();
  }
  
  public void shutdownImmediately()
  {
    setPowerOffCount(1);
    try
    {
      checkPowerOff();
    }
    catch (DbException localDbException) {}
    closeFiles();
  }
  
  public TempFileDeleter getTempFileDeleter()
  {
    return this.tempFileDeleter;
  }
  
  public PageStore getPageStore()
  {
    if (this.dbSettings.mvStore)
    {
      if (this.mvStore == null) {
        this.mvStore = MVTableEngine.init(this);
      }
      return null;
    }
    if (this.pageStore == null)
    {
      this.pageStore = new PageStore(this, this.databaseName + ".h2.db", this.accessModeData, this.cacheSize);
      if (this.pageSize != 4096) {
        this.pageStore.setPageSize(this.pageSize);
      }
      if ((!this.readOnly) && (this.fileLockMethod == 4)) {
        this.pageStore.setLockFile(true);
      }
      this.pageStore.setLogMode(this.logMode);
      this.pageStore.open();
    }
    return this.pageStore;
  }
  
  public Table getFirstUserTable()
  {
    for (Table localTable : getAllTablesAndViews(false)) {
      if (localTable.getCreateSQL() != null) {
        if (!localTable.isHidden()) {
          return localTable;
        }
      }
    }
    return null;
  }
  
  public boolean isReconnectNeeded()
  {
    if (this.fileLockMethod != 3) {
      return false;
    }
    if (this.reconnectChangePending) {
      return false;
    }
    long l = System.currentTimeMillis();
    if (l < this.reconnectCheckNext) {
      return false;
    }
    this.reconnectCheckNext = (l + this.reconnectCheckDelay);
    if (this.lock == null) {
      this.lock = new FileLock(this.traceSystem, this.databaseName + ".lock.db", 1000);
    }
    try
    {
      Properties localProperties1 = this.lock.load();Properties localProperties2 = localProperties1;
      for (;;)
      {
        if (localProperties1.equals(this.reconnectLastLock)) {
          return false;
        }
        if (localProperties1.getProperty("changePending", null) == null) {
          break;
        }
        if (System.currentTimeMillis() > l + this.reconnectCheckDelay * 10) {
          if (localProperties2.equals(localProperties1))
          {
            this.lock.setProperty("changePending", null);
            this.lock.save();
            break;
          }
        }
        this.trace.debug("delay (change pending)");
        Thread.sleep(this.reconnectCheckDelay);
        localProperties1 = this.lock.load();
      }
      this.reconnectLastLock = localProperties1;
    }
    catch (Exception localException)
    {
      this.trace.error(localException, "readOnly {0}", new Object[] { Boolean.valueOf(this.readOnly) });
    }
    return true;
  }
  
  public void checkpointIfRequired()
  {
    if ((this.fileLockMethod != 3) || (this.readOnly) || (!this.reconnectChangePending) || (this.closing)) {
      return;
    }
    long l = System.currentTimeMillis();
    if (l > this.reconnectCheckNext + this.reconnectCheckDelay)
    {
      if ((SysProperties.CHECK) && (this.checkpointAllowed < 0)) {
        DbException.throwInternalError();
      }
      synchronized (this.reconnectSync)
      {
        if (this.checkpointAllowed > 0) {
          return;
        }
        this.checkpointRunning = true;
      }
      synchronized (this)
      {
        this.trace.debug("checkpoint start");
        flushSequences();
        checkpoint();
        reconnectModified(false);
        this.trace.debug("checkpoint end");
      }
      synchronized (this.reconnectSync)
      {
        this.checkpointRunning = false;
      }
    }
  }
  
  public boolean isFileLockSerialized()
  {
    return this.fileLockMethod == 3;
  }
  
  private void flushSequences()
  {
    for (SchemaObject localSchemaObject : getAllSchemaObjects(3))
    {
      Sequence localSequence = (Sequence)localSchemaObject;
      localSequence.flushWithoutMargin();
    }
  }
  
  public void checkpoint()
  {
    if (this.persistent)
    {
      synchronized (this)
      {
        if (this.pageStore != null) {
          this.pageStore.checkpoint();
        }
      }
      if (this.mvStore != null) {
        this.mvStore.flush();
      }
    }
    getTempFileDeleter().deleteUnused();
  }
  
  public boolean beforeWriting()
  {
    if (this.fileLockMethod != 3) {
      return true;
    }
    while (this.checkpointRunning) {
      try
      {
        Thread.sleep(10 + (int)(Math.random() * 10.0D));
      }
      catch (Exception localException) {}
    }
    synchronized (this.reconnectSync)
    {
      if (reconnectModified(true))
      {
        this.checkpointAllowed += 1;
        if ((SysProperties.CHECK) && (this.checkpointAllowed > 20)) {
          throw DbException.throwInternalError();
        }
        return true;
      }
    }
    this.reconnectCheckNext = (System.currentTimeMillis() - 1L);
    this.reconnectLastLock = null;
    return false;
  }
  
  public void afterWriting()
  {
    if (this.fileLockMethod != 3) {
      return;
    }
    synchronized (this.reconnectSync)
    {
      this.checkpointAllowed -= 1;
    }
    if ((SysProperties.CHECK) && (this.checkpointAllowed < 0)) {
      throw DbException.throwInternalError();
    }
  }
  
  public void setReadOnly(boolean paramBoolean)
  {
    this.readOnly = paramBoolean;
  }
  
  public void setCompactMode(int paramInt)
  {
    this.compactMode = paramInt;
  }
  
  public SourceCompiler getCompiler()
  {
    if (this.compiler == null) {
      this.compiler = new SourceCompiler();
    }
    return this.compiler;
  }
  
  public LobStorageInterface getLobStorage()
  {
    if (this.lobStorage == null) {
      if (this.dbSettings.mvStore) {
        this.lobStorage = new LobStorageMap(this);
      } else {
        this.lobStorage = new LobStorageBackend(this);
      }
    }
    return this.lobStorage;
  }
  
  public JdbcConnection getLobConnectionForInit()
  {
    String str = "jdbc:default:connection";
    JdbcConnection localJdbcConnection = new JdbcConnection(this.systemSession, this.systemUser.getName(), str);
    
    localJdbcConnection.setTraceLevel(0);
    return localJdbcConnection;
  }
  
  public JdbcConnection getLobConnectionForRegularUse()
  {
    String str = "jdbc:default:connection";
    JdbcConnection localJdbcConnection = new JdbcConnection(this.lobSession, this.systemUser.getName(), str);
    
    localJdbcConnection.setTraceLevel(0);
    return localJdbcConnection;
  }
  
  public Session getLobSession()
  {
    return this.lobSession;
  }
  
  public void setLogMode(int paramInt)
  {
    if ((paramInt < 0) || (paramInt > 2)) {
      throw DbException.getInvalidValueException("LOG", Integer.valueOf(paramInt));
    }
    if (this.pageStore != null)
    {
      if ((paramInt != 2) || (this.pageStore.getLogMode() != 2)) {
        this.trace.error(null, "log {0}", new Object[] { Integer.valueOf(paramInt) });
      }
      this.logMode = paramInt;
      this.pageStore.setLogMode(paramInt);
    }
    if (this.mvStore != null) {
      this.logMode = paramInt;
    }
  }
  
  public int getLogMode()
  {
    if (this.pageStore != null) {
      return this.pageStore.getLogMode();
    }
    if (this.mvStore != null) {
      return this.logMode;
    }
    return 0;
  }
  
  public int getDefaultTableType()
  {
    return this.defaultTableType;
  }
  
  public void setDefaultTableType(int paramInt)
  {
    this.defaultTableType = paramInt;
  }
  
  public void setMultiVersion(boolean paramBoolean)
  {
    this.multiVersion = paramBoolean;
  }
  
  public DbSettings getSettings()
  {
    return this.dbSettings;
  }
  
  public <V> HashMap<String, V> newStringMap()
  {
    return this.dbSettings.databaseToUpper ? new HashMap() : new CaseInsensitiveMap();
  }
  
  public boolean equalsIdentifiers(String paramString1, String paramString2)
  {
    if ((paramString1 == paramString2) || (paramString1.equals(paramString2))) {
      return true;
    }
    if ((!this.dbSettings.databaseToUpper) && (paramString1.equalsIgnoreCase(paramString2))) {
      return true;
    }
    return false;
  }
  
  public int readLob(long paramLong1, byte[] paramArrayOfByte1, long paramLong2, byte[] paramArrayOfByte2, int paramInt1, int paramInt2)
  {
    throw DbException.throwInternalError();
  }
  
  public byte[] getFileEncryptionKey()
  {
    return this.fileEncryptionKey;
  }
  
  public int getPageSize()
  {
    return this.pageSize;
  }
  
  public JavaObjectSerializer getJavaObjectSerializer()
  {
    initJavaObjectSerializer();
    return this.javaObjectSerializer;
  }
  
  private void initJavaObjectSerializer()
  {
    if (this.javaObjectSerializerInitialized) {
      return;
    }
    synchronized (this)
    {
      if (this.javaObjectSerializerInitialized) {
        return;
      }
      String str = this.javaObjectSerializerName;
      if (str != null)
      {
        str = str.trim();
        if ((!str.isEmpty()) && (!str.equals("null"))) {
          try
          {
            this.javaObjectSerializer = ((JavaObjectSerializer)JdbcUtils.loadUserClass(str).newInstance());
          }
          catch (Exception localException)
          {
            throw DbException.convert(localException);
          }
        }
      }
      this.javaObjectSerializerInitialized = true;
    }
  }
  
  public void setJavaObjectSerializerName(String paramString)
  {
    synchronized (this)
    {
      this.javaObjectSerializerInitialized = false;
      this.javaObjectSerializerName = paramString;
    }
  }
}
