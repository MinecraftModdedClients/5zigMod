package org.h2.store;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.CRC32;
import org.h2.command.ddl.CreateTableData;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.index.Cursor;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.index.MultiVersionIndex;
import org.h2.index.PageBtreeIndex;
import org.h2.index.PageBtreeLeaf;
import org.h2.index.PageBtreeNode;
import org.h2.index.PageDataIndex;
import org.h2.index.PageDataLeaf;
import org.h2.index.PageDataNode;
import org.h2.index.PageDataOverflow;
import org.h2.index.PageDelegateIndex;
import org.h2.index.PageIndex;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.store.fs.FileUtils;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.RegularTable;
import org.h2.table.Table;
import org.h2.util.BitField;
import org.h2.util.Cache;
import org.h2.util.CacheLRU;
import org.h2.util.CacheObject;
import org.h2.util.CacheWriter;
import org.h2.util.IntArray;
import org.h2.util.IntIntHashMap;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.CompareMode;
import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.h2.value.ValueString;

public class PageStore
  implements CacheWriter
{
  public static final int PAGE_SIZE_MIN = 64;
  public static final int PAGE_SIZE_MAX = 32768;
  public static final int LOG_MODE_OFF = 0;
  public static final int LOG_MODE_SYNC = 2;
  private static final int PAGE_ID_FREE_LIST_ROOT = 3;
  private static final int PAGE_ID_META_ROOT = 4;
  private static final int MIN_PAGE_COUNT = 5;
  private static final int INCREMENT_KB = 1024;
  private static final int INCREMENT_PERCENT_MIN = 35;
  private static final int READ_VERSION = 3;
  private static final int WRITE_VERSION = 3;
  private static final int META_TYPE_DATA_INDEX = 0;
  private static final int META_TYPE_BTREE_INDEX = 1;
  private static final int META_TABLE_ID = -1;
  private static final int COMPACT_BLOCK_SIZE = 1536;
  private final Database database;
  private final Trace trace;
  private final String fileName;
  private FileStore file;
  private String accessMode;
  private int pageSize = 4096;
  private int pageSizeShift;
  private long writeCountBase;
  private long writeCount;
  private long readCount;
  private int logKey;
  private int logFirstTrunkPage;
  private int logFirstDataPage;
  private final Cache cache;
  private int freeListPagesPerList;
  private boolean recoveryRunning;
  private boolean ignoreBigLog;
  private int firstFreeListIndex;
  private long fileLength;
  private int pageCount;
  private PageLog log;
  private Schema metaSchema;
  private RegularTable metaTable;
  private PageDataIndex metaIndex;
  private final IntIntHashMap metaRootPageId = new IntIntHashMap();
  private final HashMap<Integer, PageIndex> metaObjects = New.hashMap();
  private HashMap<Integer, PageIndex> tempObjects;
  private HashMap<Integer, Integer> reservedPages;
  private boolean isNew;
  private long maxLogSize = 16777216L;
  private final Session pageStoreSession;
  private final BitField freed = new BitField();
  private final ArrayList<PageFreeList> freeLists = New.arrayList();
  private boolean recordPageReads;
  private ArrayList<Integer> recordedPagesList;
  private IntIntHashMap recordedPagesIndex;
  private long changeCount = 1L;
  private Data emptyPage;
  private long logSizeBase;
  private HashMap<String, Integer> statistics;
  private int logMode = 2;
  private boolean lockFile;
  private boolean readMode;
  private int backupLevel;
  
  public PageStore(Database paramDatabase, String paramString1, String paramString2, int paramInt)
  {
    this.fileName = paramString1;
    this.accessMode = paramString2;
    this.database = paramDatabase;
    this.trace = paramDatabase.getTrace("pageStore");
    
    String str = paramDatabase.getCacheType();
    this.cache = CacheLRU.getCache(this, str, paramInt);
    this.pageStoreSession = new Session(paramDatabase, null, 0);
  }
  
  public void statisticsStart()
  {
    this.statistics = New.hashMap();
  }
  
  public HashMap<String, Integer> statisticsEnd()
  {
    HashMap localHashMap = this.statistics;
    this.statistics = null;
    return localHashMap;
  }
  
  private void statisticsIncrement(String paramString)
  {
    if (this.statistics != null)
    {
      Integer localInteger = (Integer)this.statistics.get(paramString);
      this.statistics.put(paramString, Integer.valueOf(localInteger == null ? 1 : localInteger.intValue() + 1));
    }
  }
  
  public synchronized int copyDirect(int paramInt, OutputStream paramOutputStream)
    throws IOException
  {
    byte[] arrayOfByte = new byte[this.pageSize];
    if (paramInt >= this.pageCount) {
      return -1;
    }
    this.file.seek(paramInt << this.pageSizeShift);
    this.file.readFullyDirect(arrayOfByte, 0, this.pageSize);
    this.readCount += 1L;
    paramOutputStream.write(arrayOfByte, 0, this.pageSize);
    return paramInt + 1;
  }
  
  public synchronized void open()
  {
    try
    {
      this.metaRootPageId.put(-1, 4);
      if (FileUtils.exists(this.fileName))
      {
        long l = FileUtils.size(this.fileName);
        if (l < 320L)
        {
          if (this.database.isReadOnly()) {
            throw DbException.get(90030, this.fileName + " length: " + l);
          }
          openNew();
        }
        else
        {
          openExisting();
        }
      }
      else
      {
        openNew();
      }
    }
    catch (DbException localDbException)
    {
      close();
      throw localDbException;
    }
  }
  
  private void openNew()
  {
    setPageSize(this.pageSize);
    this.freeListPagesPerList = PageFreeList.getPagesAddressed(this.pageSize);
    this.file = this.database.openFile(this.fileName, this.accessMode, false);
    lockFile();
    this.recoveryRunning = true;
    writeStaticHeader();
    writeVariableHeader();
    this.log = new PageLog(this);
    increaseFileSize(5);
    openMetaIndex();
    this.logFirstTrunkPage = allocatePage();
    this.log.openForWriting(this.logFirstTrunkPage, false);
    this.isNew = true;
    this.recoveryRunning = false;
    increaseFileSize();
  }
  
  private void lockFile()
  {
    if ((this.lockFile) && 
      (!this.file.tryLock())) {
      throw DbException.get(90020, this.fileName);
    }
  }
  
  private void openExisting()
  {
    try
    {
      this.file = this.database.openFile(this.fileName, this.accessMode, true);
    }
    catch (DbException localDbException)
    {
      if ((localDbException.getErrorCode() == 90031) && 
        (localDbException.getMessage().contains("locked"))) {
        throw DbException.get(90020, localDbException, new String[] { this.fileName });
      }
      throw localDbException;
    }
    lockFile();
    readStaticHeader();
    this.freeListPagesPerList = PageFreeList.getPagesAddressed(this.pageSize);
    this.fileLength = this.file.length();
    this.pageCount = ((int)(this.fileLength / this.pageSize));
    if (this.pageCount < 5)
    {
      if (this.database.isReadOnly()) {
        throw DbException.get(90030, this.fileName + " pageCount: " + this.pageCount);
      }
      this.file.releaseLock();
      this.file.close();
      FileUtils.delete(this.fileName);
      openNew();
      return;
    }
    readVariableHeader();
    this.log = new PageLog(this);
    this.log.openForReading(this.logKey, this.logFirstTrunkPage, this.logFirstDataPage);
    boolean bool1 = this.database.isMultiVersion();
    
    this.database.setMultiVersion(false);
    boolean bool2 = recover();
    this.database.setMultiVersion(bool1);
    if (!this.database.isReadOnly())
    {
      this.readMode = true;
      if ((!bool2) || (!SysProperties.MODIFY_ON_WRITE) || (this.tempObjects != null))
      {
        openForWriting();
        removeOldTempIndexes();
      }
    }
  }
  
  private void openForWriting()
  {
    if ((!this.readMode) || (this.database.isReadOnly())) {
      return;
    }
    this.readMode = false;
    this.recoveryRunning = true;
    this.log.free();
    this.logFirstTrunkPage = allocatePage();
    this.log.openForWriting(this.logFirstTrunkPage, false);
    this.recoveryRunning = false;
    this.freed.set(0, this.pageCount, true);
    checkpoint();
  }
  
  private void removeOldTempIndexes()
  {
    if (this.tempObjects != null)
    {
      this.metaObjects.putAll(this.tempObjects);
      for (PageIndex localPageIndex : this.tempObjects.values()) {
        if (localPageIndex.getTable().isTemporary())
        {
          localPageIndex.truncate(this.pageStoreSession);
          localPageIndex.remove(this.pageStoreSession);
        }
      }
      this.pageStoreSession.commit(true);
      this.tempObjects = null;
    }
    this.metaObjects.clear();
    this.metaObjects.put(Integer.valueOf(-1), this.metaIndex);
  }
  
  private void writeIndexRowCounts()
  {
    for (PageIndex localPageIndex : this.metaObjects.values()) {
      localPageIndex.writeRowCount();
    }
  }
  
  private void writeBack()
  {
    ArrayList localArrayList = this.cache.getAllChanged();
    Collections.sort(localArrayList);
    int i = 0;
    for (int j = localArrayList.size(); i < j; i++) {
      writeBack((CacheObject)localArrayList.get(i));
    }
  }
  
  public synchronized void checkpoint()
  {
    this.trace.debug("checkpoint");
    if ((this.log == null) || (this.readMode) || (this.database.isReadOnly()) || (this.backupLevel > 0)) {
      return;
    }
    this.database.checkPowerOff();
    writeIndexRowCounts();
    
    this.log.checkpoint();
    writeBack();
    
    int i = getFirstUncommittedSection();
    
    this.log.removeUntil(i);
    
    writeBack();
    
    this.log.checkpoint();
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("writeFree");
    }
    byte[] arrayOfByte1 = new byte[16];
    byte[] arrayOfByte2 = new byte[this.pageSize];
    for (int j = 3; j < this.pageCount; j++) {
      if (isUsed(j))
      {
        this.freed.clear(j);
      }
      else if (!this.freed.get(j))
      {
        if (this.trace.isDebugEnabled()) {
          this.trace.debug("free " + j);
        }
        this.file.seek(j << this.pageSizeShift);
        this.file.readFully(arrayOfByte1, 0, 16);
        if (arrayOfByte1[0] != 0)
        {
          this.file.seek(j << this.pageSizeShift);
          this.file.write(arrayOfByte2, 0, this.pageSize);
          this.writeCount += 1L;
        }
        this.freed.set(j);
      }
    }
  }
  
  public synchronized void compact(int paramInt)
  {
    if (!this.database.getSettings().pageStoreTrim) {
      return;
    }
    if ((SysProperties.MODIFY_ON_WRITE) && (this.readMode) && (paramInt == 0)) {
      return;
    }
    openForWriting();
    
    int i = -1;
    for (int j = getFreeListId(this.pageCount); j >= 0; j--)
    {
      i = getFreeList(j).getLastUsed();
      if (i != -1) {
        break;
      }
    }
    writeBack();
    this.log.free();
    this.recoveryRunning = true;
    try
    {
      this.logFirstTrunkPage = (i + 1);
      allocatePage(this.logFirstTrunkPage);
      this.log.openForWriting(this.logFirstTrunkPage, true);
      
      this.log.checkpoint();
    }
    finally
    {
      this.recoveryRunning = false;
    }
    long l1 = System.currentTimeMillis();
    int k = paramInt == 82 ? 1 : 0;
    
    int m = paramInt == 84 ? 1 : 0;
    if (this.database.getSettings().defragAlways) {
      k = m = 1;
    }
    int n = this.database.getSettings().maxCompactTime;
    int i1 = this.database.getSettings().maxCompactCount;
    if ((k != 0) || (m != 0))
    {
      n = Integer.MAX_VALUE;
      i1 = Integer.MAX_VALUE;
    }
    int i2 = k != 0 ? 1536 : 1;
    int i3 = 5;
    int i4 = i;int i6 = 0;
    for (; (i4 > 5) && (i6 < i1); i4 -= i2) {
      for (int i7 = i4 - i2 + 1; i7 <= i4; i7++) {
        if ((i7 > 5) && (isUsed(i7))) {
          synchronized (this)
          {
            i3 = getFirstFree(i3);
            if ((i3 == -1) || (i3 >= i7))
            {
              i6 = i1;
              break;
            }
            if (compact(i7, i3))
            {
              i6++;
              long l3 = System.currentTimeMillis();
              if (l3 > l1 + n)
              {
                i6 = i1;
                break;
              }
            }
          }
        }
      }
    }
    if (m != 0)
    {
      this.log.checkpoint();
      writeBack();
      this.cache.clear();
      ArrayList localArrayList = this.database.getAllTablesAndViews(false);
      this.recordedPagesList = New.arrayList();
      this.recordedPagesIndex = new IntIntHashMap();
      this.recordPageReads = true;
      Session localSession = this.database.getSystemSession();
      for (Iterator localIterator1 = localArrayList.iterator(); localIterator1.hasNext();)
      {
        ??? = (Table)localIterator1.next();
        if ((!((Table)???).isTemporary()) && ("TABLE".equals(((Table)???).getTableType())))
        {
          localIndex = ((Table)???).getScanIndex(localSession);
          localCursor = localIndex.find(localSession, null, null);
          while (localCursor.next()) {
            localCursor.get();
          }
          for (localIterator2 = ((Table)???).getIndexes().iterator(); localIterator2.hasNext();)
          {
            localObject3 = (Index)localIterator2.next();
            if ((localObject3 != localIndex) && (((Index)localObject3).canScan()))
            {
              localCursor = ((Index)localObject3).find(localSession, null, null);
              while (localCursor.next()) {}
            }
          }
        }
      }
      Index localIndex;
      Cursor localCursor;
      Iterator localIterator2;
      Object localObject3;
      this.recordPageReads = false;
      int i8 = 4;
      int i9 = 0;
      int i10 = 0;
      for (int i11 = this.recordedPagesList.size(); i10 < i11; i10++)
      {
        this.log.checkpoint();
        writeBack();
        int i12 = ((Integer)this.recordedPagesList.get(i10)).intValue();
        localObject3 = getPage(i12);
        if (((Page)localObject3).canMove())
        {
          for (;;)
          {
            Page localPage = getPage(++i8);
            if ((localPage == null) || (localPage.canMove())) {
              break;
            }
          }
          if (i8 != i12)
          {
            i9 = getFirstFree(i9);
            if (i9 == -1) {
              DbException.throwInternalError("no free page for defrag");
            }
            this.cache.clear();
            swap(i12, i8, i9);
            int i13 = this.recordedPagesIndex.get(i8);
            if (i13 != -1)
            {
              this.recordedPagesList.set(i13, Integer.valueOf(i12));
              this.recordedPagesIndex.put(i12, i13);
            }
            this.recordedPagesList.set(i10, Integer.valueOf(i8));
            this.recordedPagesIndex.put(i8, i10);
          }
        }
      }
      this.recordedPagesList = null;
      this.recordedPagesIndex = null;
    }
    checkpoint();
    this.log.checkpoint();
    writeIndexRowCounts();
    this.log.checkpoint();
    writeBack();
    commit(this.pageStoreSession);
    writeBack();
    this.log.checkpoint();
    
    this.log.free();
    
    this.recoveryRunning = true;
    try
    {
      setLogFirstPage(++this.logKey, 0, 0);
    }
    finally
    {
      this.recoveryRunning = false;
    }
    writeBack();
    for (int i5 = getFreeListId(this.pageCount); i5 >= 0; i5--)
    {
      i = getFreeList(i5).getLastUsed();
      if (i != -1) {
        break;
      }
    }
    i5 = i + 1;
    if (i5 < this.pageCount) {
      this.freed.set(i5, this.pageCount, false);
    }
    this.pageCount = i5;
    
    this.freeLists.clear();
    this.trace.debug("pageCount: " + this.pageCount);
    long l2 = this.pageCount << this.pageSizeShift;
    if (this.file.length() != l2)
    {
      this.file.setLength(l2);
      this.writeCount += 1L;
    }
  }
  
  private int getFirstFree(int paramInt)
  {
    int i = -1;
    for (int j = getFreeListId(paramInt); paramInt < this.pageCount; j++)
    {
      i = getFreeList(j).getFirstFree(paramInt);
      if (i != -1) {
        break;
      }
    }
    return i;
  }
  
  private void swap(int paramInt1, int paramInt2, int paramInt3)
  {
    if ((paramInt1 < 5) || (paramInt2 < 5))
    {
      System.out.println(isUsed(paramInt1) + " " + isUsed(paramInt2));
      DbException.throwInternalError("can't swap " + paramInt1 + " and " + paramInt2);
    }
    Page localPage1 = (Page)this.cache.get(paramInt3);
    if (localPage1 != null) {
      DbException.throwInternalError("not free: " + localPage1);
    }
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("swap " + paramInt1 + " and " + paramInt2 + " via " + paramInt3);
    }
    Page localPage2 = null;
    if (isUsed(paramInt1))
    {
      localPage2 = getPage(paramInt1);
      if (localPage2 != null) {
        localPage2.moveTo(this.pageStoreSession, paramInt3);
      }
      free(paramInt1);
    }
    if (paramInt3 != paramInt2)
    {
      if (isUsed(paramInt2))
      {
        Page localPage3 = getPage(paramInt2);
        if (localPage3 != null) {
          localPage3.moveTo(this.pageStoreSession, paramInt1);
        }
        free(paramInt2);
      }
      if (localPage2 != null)
      {
        localPage1 = getPage(paramInt3);
        if (localPage1 != null) {
          localPage1.moveTo(this.pageStoreSession, paramInt2);
        }
        free(paramInt3);
      }
    }
  }
  
  private boolean compact(int paramInt1, int paramInt2)
  {
    if ((paramInt1 < 5) || (paramInt2 == -1) || (paramInt2 >= paramInt1) || (!isUsed(paramInt1))) {
      return false;
    }
    Page localPage1 = (Page)this.cache.get(paramInt2);
    if (localPage1 != null) {
      DbException.throwInternalError("not free: " + localPage1);
    }
    Page localPage2 = getPage(paramInt1);
    if (localPage2 == null)
    {
      freePage(paramInt1);
    }
    else if (((localPage2 instanceof PageStreamData)) || ((localPage2 instanceof PageStreamTrunk)))
    {
      if (localPage2.getPos() < this.log.getMinPageId()) {
        freePage(paramInt1);
      }
    }
    else
    {
      if (this.trace.isDebugEnabled()) {
        this.trace.debug("move " + localPage2.getPos() + " to " + paramInt2);
      }
      try
      {
        localPage2.moveTo(this.pageStoreSession, paramInt2);
      }
      finally
      {
        this.changeCount += 1L;
        if ((SysProperties.CHECK) && (this.changeCount < 0L)) {
          throw DbException.throwInternalError("changeCount has wrapped");
        }
      }
    }
    return true;
  }
  
  public synchronized Page getPage(int paramInt)
  {
    Object localObject1 = (Page)this.cache.get(paramInt);
    if (localObject1 != null) {
      return (Page)localObject1;
    }
    Data localData = createData();
    readPage(paramInt, localData);
    int i = localData.readByte();
    if (i == 0) {
      return null;
    }
    localData.readShortInt();
    localData.readInt();
    if (!checksumTest(localData.getBytes(), paramInt, this.pageSize)) {
      throw DbException.get(90030, "wrong checksum");
    }
    int j;
    PageIndex localPageIndex;
    Object localObject2;
    switch (i & 0xFFFFFFEF)
    {
    case 6: 
      localObject1 = PageFreeList.read(this, localData, paramInt);
      break;
    case 1: 
      j = localData.readVarInt();
      localPageIndex = (PageIndex)this.metaObjects.get(Integer.valueOf(j));
      if (localPageIndex == null) {
        throw DbException.get(90030, "index not found " + j);
      }
      if (!(localPageIndex instanceof PageDataIndex)) {
        throw DbException.get(90030, "not a data index " + j + " " + localPageIndex);
      }
      localObject2 = (PageDataIndex)localPageIndex;
      if (this.statistics != null) {
        statisticsIncrement(((PageDataIndex)localObject2).getTable().getName() + "." + ((PageDataIndex)localObject2).getName() + " read");
      }
      localObject1 = PageDataLeaf.read((PageDataIndex)localObject2, localData, paramInt);
      break;
    case 2: 
      j = localData.readVarInt();
      localPageIndex = (PageIndex)this.metaObjects.get(Integer.valueOf(j));
      if (localPageIndex == null) {
        throw DbException.get(90030, "index not found " + j);
      }
      if (!(localPageIndex instanceof PageDataIndex)) {
        throw DbException.get(90030, "not a data index " + j + " " + localPageIndex);
      }
      localObject2 = (PageDataIndex)localPageIndex;
      if (this.statistics != null) {
        statisticsIncrement(((PageDataIndex)localObject2).getTable().getName() + "." + ((PageDataIndex)localObject2).getName() + " read");
      }
      localObject1 = PageDataNode.read((PageDataIndex)localObject2, localData, paramInt);
      break;
    case 3: 
      localObject1 = PageDataOverflow.read(this, localData, paramInt);
      if (this.statistics != null) {
        statisticsIncrement("overflow read");
      }
      break;
    case 4: 
      j = localData.readVarInt();
      localPageIndex = (PageIndex)this.metaObjects.get(Integer.valueOf(j));
      if (localPageIndex == null) {
        throw DbException.get(90030, "index not found " + j);
      }
      if (!(localPageIndex instanceof PageBtreeIndex)) {
        throw DbException.get(90030, "not a btree index " + j + " " + localPageIndex);
      }
      localObject2 = (PageBtreeIndex)localPageIndex;
      if (this.statistics != null) {
        statisticsIncrement(((PageBtreeIndex)localObject2).getTable().getName() + "." + ((PageBtreeIndex)localObject2).getName() + " read");
      }
      localObject1 = PageBtreeLeaf.read((PageBtreeIndex)localObject2, localData, paramInt);
      break;
    case 5: 
      j = localData.readVarInt();
      localPageIndex = (PageIndex)this.metaObjects.get(Integer.valueOf(j));
      if (localPageIndex == null) {
        throw DbException.get(90030, "index not found " + j);
      }
      if (!(localPageIndex instanceof PageBtreeIndex)) {
        throw DbException.get(90030, "not a btree index " + j + " " + localPageIndex);
      }
      localObject2 = (PageBtreeIndex)localPageIndex;
      if (this.statistics != null) {
        statisticsIncrement(((PageBtreeIndex)localObject2).getTable().getName() + "." + ((PageBtreeIndex)localObject2).getName() + " read");
      }
      localObject1 = PageBtreeNode.read((PageBtreeIndex)localObject2, localData, paramInt);
      break;
    case 7: 
      localObject1 = PageStreamTrunk.read(this, localData, paramInt);
      break;
    case 8: 
      localObject1 = PageStreamData.read(this, localData, paramInt);
      break;
    default: 
      throw DbException.get(90030, "page=" + paramInt + " type=" + i);
    }
    this.cache.put((CacheObject)localObject1);
    return (Page)localObject1;
  }
  
  private int getFirstUncommittedSection()
  {
    this.trace.debug("getFirstUncommittedSection");
    Session[] arrayOfSession1 = this.database.getSessions(true);
    int i = this.log.getLogSectionId();
    for (Session localSession : arrayOfSession1)
    {
      int m = localSession.getFirstUncommittedLog();
      if ((m != -1) && 
        (m < i)) {
        i = m;
      }
    }
    return i;
  }
  
  private void readStaticHeader()
  {
    this.file.seek(48L);
    Data localData = Data.create(this.database, new byte[16]);
    
    this.file.readFully(localData.getBytes(), 0, 16);
    
    this.readCount += 1L;
    setPageSize(localData.readInt());
    int i = localData.readByte();
    int j = localData.readByte();
    if (j > 3) {
      throw DbException.get(90048, this.fileName);
    }
    if (i > 3)
    {
      close();
      this.database.setReadOnly(true);
      this.accessMode = "r";
      this.file = this.database.openFile(this.fileName, this.accessMode, true);
    }
  }
  
  private void readVariableHeader()
  {
    Data localData = createData();
    for (int i = 1;; i++)
    {
      if (i == 3) {
        throw DbException.get(90030, this.fileName);
      }
      localData.reset();
      readPage(i, localData);
      CRC32 localCRC32 = new CRC32();
      localCRC32.update(localData.getBytes(), 4, this.pageSize - 4);
      int j = (int)localCRC32.getValue();
      int k = localData.readInt();
      if (j == k)
      {
        this.writeCountBase = localData.readLong();
        this.logKey = localData.readInt();
        this.logFirstTrunkPage = localData.readInt();
        this.logFirstDataPage = localData.readInt();
        break;
      }
    }
  }
  
  public void setPageSize(int paramInt)
  {
    if ((paramInt < 64) || (paramInt > 32768)) {
      throw DbException.get(90030, this.fileName + " pageSize: " + paramInt);
    }
    int i = 0;
    int j = 0;
    for (int k = 1; k <= paramInt;)
    {
      if (paramInt == k)
      {
        i = 1;
        break;
      }
      j++;
      k += k;
    }
    if (i == 0) {
      throw DbException.get(90030, this.fileName);
    }
    this.pageSize = paramInt;
    this.emptyPage = createData();
    this.pageSizeShift = j;
  }
  
  private void writeStaticHeader()
  {
    Data localData = Data.create(this.database, new byte[this.pageSize - 48]);
    localData.writeInt(this.pageSize);
    localData.writeByte((byte)3);
    localData.writeByte((byte)3);
    this.file.seek(48L);
    this.file.write(localData.getBytes(), 0, this.pageSize - 48);
    this.writeCount += 1L;
  }
  
  void setLogFirstPage(int paramInt1, int paramInt2, int paramInt3)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("setLogFirstPage key: " + paramInt1 + " trunk: " + paramInt2 + " data: " + paramInt3);
    }
    this.logKey = paramInt1;
    this.logFirstTrunkPage = paramInt2;
    this.logFirstDataPage = paramInt3;
    writeVariableHeader();
  }
  
  private void writeVariableHeader()
  {
    this.trace.debug("writeVariableHeader");
    if (this.logMode == 2) {
      this.file.sync();
    }
    Data localData = createData();
    localData.writeInt(0);
    localData.writeLong(getWriteCountTotal());
    localData.writeInt(this.logKey);
    localData.writeInt(this.logFirstTrunkPage);
    localData.writeInt(this.logFirstDataPage);
    CRC32 localCRC32 = new CRC32();
    localCRC32.update(localData.getBytes(), 4, this.pageSize - 4);
    localData.setInt(0, (int)localCRC32.getValue());
    this.file.seek(this.pageSize);
    this.file.write(localData.getBytes(), 0, this.pageSize);
    this.file.seek(this.pageSize + this.pageSize);
    this.file.write(localData.getBytes(), 0, this.pageSize);
  }
  
  public synchronized void close()
  {
    this.trace.debug("close");
    if (this.log != null)
    {
      this.log.close();
      this.log = null;
    }
    if (this.file != null) {
      try
      {
        this.file.releaseLock();
        this.file.close();
      }
      finally
      {
        this.file = null;
      }
    }
  }
  
  public synchronized void flushLog()
  {
    if (this.file != null) {
      this.log.flush();
    }
  }
  
  public synchronized void sync()
  {
    if (this.file != null)
    {
      this.log.flush();
      this.file.sync();
    }
  }
  
  public Trace getTrace()
  {
    return this.trace;
  }
  
  public synchronized void writeBack(CacheObject paramCacheObject)
  {
    Page localPage = (Page)paramCacheObject;
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("writeBack " + localPage);
    }
    localPage.write();
    localPage.setChanged(false);
  }
  
  public synchronized void logUndo(Page paramPage, Data paramData)
  {
    if (this.logMode == 0) {
      return;
    }
    checkOpen();
    this.database.checkWritingAllowed();
    if (!this.recoveryRunning)
    {
      int i = paramPage.getPos();
      if (!this.log.getUndo(i))
      {
        if (paramData == null) {
          paramData = readPage(i);
        }
        openForWriting();
        this.log.addUndo(i, paramData);
      }
    }
  }
  
  public synchronized void update(Page paramPage)
  {
    if ((this.trace.isDebugEnabled()) && 
      (!paramPage.isChanged())) {
      this.trace.debug("updateRecord " + paramPage.toString());
    }
    checkOpen();
    this.database.checkWritingAllowed();
    paramPage.setChanged(true);
    int i = paramPage.getPos();
    if ((SysProperties.CHECK) && (!this.recoveryRunning)) {
      if (this.logMode != 0) {
        this.log.addUndo(i, null);
      }
    }
    allocatePage(i);
    this.cache.update(i, paramPage);
  }
  
  private int getFreeListId(int paramInt)
  {
    return (paramInt - 3) / this.freeListPagesPerList;
  }
  
  private PageFreeList getFreeListForPage(int paramInt)
  {
    return getFreeList(getFreeListId(paramInt));
  }
  
  private PageFreeList getFreeList(int paramInt)
  {
    PageFreeList localPageFreeList = null;
    if (paramInt < this.freeLists.size())
    {
      localPageFreeList = (PageFreeList)this.freeLists.get(paramInt);
      if (localPageFreeList != null) {
        return localPageFreeList;
      }
    }
    int i = 3 + paramInt * this.freeListPagesPerList;
    while (i >= this.pageCount) {
      increaseFileSize();
    }
    if (i < this.pageCount) {
      localPageFreeList = (PageFreeList)getPage(i);
    }
    if (localPageFreeList == null)
    {
      localPageFreeList = PageFreeList.create(this, i);
      this.cache.put(localPageFreeList);
    }
    while (this.freeLists.size() <= paramInt) {
      this.freeLists.add(null);
    }
    this.freeLists.set(paramInt, localPageFreeList);
    return localPageFreeList;
  }
  
  private void freePage(int paramInt)
  {
    int i = getFreeListId(paramInt);
    PageFreeList localPageFreeList = getFreeList(i);
    this.firstFreeListIndex = Math.min(i, this.firstFreeListIndex);
    localPageFreeList.free(paramInt);
  }
  
  void allocatePage(int paramInt)
  {
    PageFreeList localPageFreeList = getFreeListForPage(paramInt);
    localPageFreeList.allocate(paramInt);
  }
  
  private boolean isUsed(int paramInt)
  {
    return getFreeListForPage(paramInt).isUsed(paramInt);
  }
  
  void allocatePages(IntArray paramIntArray, int paramInt1, BitField paramBitField, int paramInt2)
  {
    paramIntArray.ensureCapacity(paramIntArray.size() + paramInt1);
    for (int i = 0; i < paramInt1; i++)
    {
      int j = allocatePage(paramBitField, paramInt2);
      paramInt2 = j;
      paramIntArray.add(j);
    }
  }
  
  public synchronized int allocatePage()
  {
    openForWriting();
    int i = allocatePage(null, 0);
    if ((!this.recoveryRunning) && 
      (this.logMode != 0)) {
      this.log.addUndo(i, this.emptyPage);
    }
    return i;
  }
  
  private int allocatePage(BitField paramBitField, int paramInt)
  {
    int i;
    for (int j = this.firstFreeListIndex;; j++)
    {
      PageFreeList localPageFreeList = getFreeList(j);
      i = localPageFreeList.allocate(paramBitField, paramInt);
      if (i >= 0)
      {
        this.firstFreeListIndex = j;
        break;
      }
    }
    while (i >= this.pageCount) {
      increaseFileSize();
    }
    if (this.trace.isDebugEnabled()) {}
    return i;
  }
  
  private void increaseFileSize()
  {
    int i = 1048576 / this.pageSize;
    int j = this.pageCount * 35 / 100;
    if (i < j) {
      i = (1 + j / i) * i;
    }
    int k = this.database.getSettings().pageStoreMaxGrowth;
    if (k < i) {
      i = k;
    }
    increaseFileSize(i);
  }
  
  private void increaseFileSize(int paramInt)
  {
    for (int i = this.pageCount; i < this.pageCount + paramInt; i++) {
      this.freed.set(i);
    }
    this.pageCount += paramInt;
    long l = this.pageCount << this.pageSizeShift;
    this.file.setLength(l);
    this.writeCount += 1L;
    this.fileLength = l;
  }
  
  public synchronized void free(int paramInt)
  {
    free(paramInt, true);
  }
  
  void free(int paramInt, boolean paramBoolean)
  {
    if (this.trace.isDebugEnabled()) {}
    this.cache.remove(paramInt);
    if ((SysProperties.CHECK) && (!this.recoveryRunning) && (paramBoolean)) {
      if (this.logMode != 0) {
        this.log.addUndo(paramInt, null);
      }
    }
    freePage(paramInt);
    if (this.recoveryRunning)
    {
      writePage(paramInt, createData());
      if ((this.reservedPages != null) && (this.reservedPages.containsKey(Integer.valueOf(paramInt))))
      {
        int i = ((Integer)this.reservedPages.get(Integer.valueOf(paramInt))).intValue();
        if (i > this.log.getLogPos()) {
          allocatePage(paramInt);
        }
      }
    }
  }
  
  void freeUnused(int paramInt)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("freeUnused " + paramInt);
    }
    this.cache.remove(paramInt);
    freePage(paramInt);
    this.freed.set(paramInt);
  }
  
  public Data createData()
  {
    return Data.create(this.database, new byte[this.pageSize]);
  }
  
  public synchronized Data readPage(int paramInt)
  {
    Data localData = createData();
    readPage(paramInt, localData);
    return localData;
  }
  
  void readPage(int paramInt, Data paramData)
  {
    if ((this.recordPageReads) && 
      (paramInt >= 5) && (this.recordedPagesIndex.get(paramInt) == -1))
    {
      this.recordedPagesIndex.put(paramInt, this.recordedPagesList.size());
      this.recordedPagesList.add(Integer.valueOf(paramInt));
    }
    if ((paramInt < 0) || (paramInt >= this.pageCount)) {
      throw DbException.get(90030, paramInt + " of " + this.pageCount);
    }
    this.file.seek(paramInt << this.pageSizeShift);
    this.file.readFully(paramData.getBytes(), 0, this.pageSize);
    this.readCount += 1L;
  }
  
  public int getPageSize()
  {
    return this.pageSize;
  }
  
  public int getPageCount()
  {
    return this.pageCount;
  }
  
  public synchronized void writePage(int paramInt, Data paramData)
  {
    if (paramInt <= 0) {
      DbException.throwInternalError("write to page " + paramInt);
    }
    byte[] arrayOfByte = paramData.getBytes();
    if (SysProperties.CHECK)
    {
      int i = (paramInt - 3) % this.freeListPagesPerList == 0 ? 1 : 0;
      
      int j = arrayOfByte[0] == 6 ? 1 : 0;
      if ((arrayOfByte[0] != 0) && (i != j)) {
        throw DbException.throwInternalError();
      }
    }
    checksumSet(arrayOfByte, paramInt);
    this.file.seek(paramInt << this.pageSizeShift);
    this.file.write(arrayOfByte, 0, this.pageSize);
    this.writeCount += 1L;
  }
  
  public synchronized void removeFromCache(int paramInt)
  {
    this.cache.remove(paramInt);
  }
  
  Database getDatabase()
  {
    return this.database;
  }
  
  private boolean recover()
  {
    this.trace.debug("log recover");
    this.recoveryRunning = true;
    boolean bool = true;
    bool &= this.log.recover(0);
    Iterator localIterator1;
    if (this.reservedPages != null) {
      for (localIterator1 = this.reservedPages.keySet().iterator(); localIterator1.hasNext();)
      {
        j = ((Integer)localIterator1.next()).intValue();
        if (this.trace.isDebugEnabled()) {
          this.trace.debug("reserve " + j);
        }
        allocatePage(j);
      }
    }
    int j;
    bool &= this.log.recover(1);
    openMetaIndex();
    readMetaData();
    bool &= this.log.recover(2);
    int i = 0;
    if (!this.database.isReadOnly()) {
      if (this.log.getInDoubtTransactions().size() == 0)
      {
        this.log.recoverEnd();
        j = getFirstUncommittedSection();
        this.log.removeUntil(j);
      }
      else
      {
        i = 1;
      }
    }
    PageDataIndex localPageDataIndex = (PageDataIndex)this.metaObjects.get(Integer.valueOf(0));
    this.isNew = (localPageDataIndex == null);
    for (PageIndex localPageIndex : this.metaObjects.values()) {
      if (localPageIndex.getTable().isTemporary())
      {
        if (this.tempObjects == null) {
          this.tempObjects = New.hashMap();
        }
        this.tempObjects.put(Integer.valueOf(localPageIndex.getId()), localPageIndex);
      }
      else
      {
        localPageIndex.close(this.pageStoreSession);
      }
    }
    allocatePage(4);
    writeIndexRowCounts();
    this.recoveryRunning = false;
    this.reservedPages = null;
    
    writeBack();
    
    this.cache.clear();
    this.freeLists.clear();
    
    this.metaObjects.clear();
    this.metaObjects.put(Integer.valueOf(-1), this.metaIndex);
    if (i != 0) {
      this.database.setReadOnly(true);
    }
    this.trace.debug("log recover done");
    return bool;
  }
  
  public synchronized void logAddOrRemoveRow(Session paramSession, int paramInt, Row paramRow, boolean paramBoolean)
  {
    if ((this.logMode != 0) && 
      (!this.recoveryRunning)) {
      this.log.logAddOrRemoveRow(paramSession, paramInt, paramRow, paramBoolean);
    }
  }
  
  public synchronized void commit(Session paramSession)
  {
    checkOpen();
    openForWriting();
    this.log.commit(paramSession.getId());
    long l1 = this.log.getSize();
    if (l1 - this.logSizeBase > this.maxLogSize / 2L)
    {
      int i = this.log.getLogFirstSectionId();
      checkpoint();
      if (this.ignoreBigLog) {
        return;
      }
      int j = this.log.getLogSectionId();
      if (j - i <= 2) {
        return;
      }
      long l2 = this.log.getSize();
      if ((l2 < l1) || (l1 < this.maxLogSize))
      {
        this.ignoreBigLog = false;
        return;
      }
      this.ignoreBigLog = true;
      this.trace.error(null, "Transaction log could not be truncated; size: " + l2 / 1024L / 1024L + " MB");
      
      this.logSizeBase = this.log.getSize();
    }
  }
  
  public synchronized void prepareCommit(Session paramSession, String paramString)
  {
    this.log.prepareCommit(paramSession, paramString);
  }
  
  public boolean isNew()
  {
    return this.isNew;
  }
  
  void allocateIfIndexRoot(int paramInt1, int paramInt2, Row paramRow)
  {
    if (paramInt2 == -1)
    {
      int i = paramRow.getValue(3).getInt();
      if (this.reservedPages == null) {
        this.reservedPages = New.hashMap();
      }
      this.reservedPages.put(Integer.valueOf(i), Integer.valueOf(paramInt1));
    }
  }
  
  void redoDelete(int paramInt, long paramLong)
  {
    Index localIndex = (Index)this.metaObjects.get(Integer.valueOf(paramInt));
    PageDataIndex localPageDataIndex = (PageDataIndex)localIndex;
    Row localRow = localPageDataIndex.getRowWithKey(paramLong);
    if ((localRow == null) || (localRow.getKey() != paramLong))
    {
      this.trace.error(null, "Entry not found: " + paramLong + " found instead: " + localRow + " - ignoring");
      
      return;
    }
    redo(paramInt, localRow, false);
  }
  
  void redo(int paramInt, Row paramRow, boolean paramBoolean)
  {
    if (paramInt == -1) {
      if (paramBoolean) {
        addMeta(paramRow, this.pageStoreSession, true);
      } else {
        removeMeta(paramRow);
      }
    }
    Index localIndex = (Index)this.metaObjects.get(Integer.valueOf(paramInt));
    if (localIndex == null) {
      throw DbException.throwInternalError("Table not found: " + paramInt + " " + paramRow + " " + paramBoolean);
    }
    Table localTable = localIndex.getTable();
    if (paramBoolean) {
      localTable.addRow(this.pageStoreSession, paramRow);
    } else {
      localTable.removeRow(this.pageStoreSession, paramRow);
    }
  }
  
  void redoTruncate(int paramInt)
  {
    Index localIndex = (Index)this.metaObjects.get(Integer.valueOf(paramInt));
    Table localTable = localIndex.getTable();
    localTable.truncate(this.pageStoreSession);
  }
  
  private void openMetaIndex()
  {
    CreateTableData localCreateTableData = new CreateTableData();
    ArrayList localArrayList = localCreateTableData.columns;
    localArrayList.add(new Column("ID", 4));
    localArrayList.add(new Column("TYPE", 4));
    localArrayList.add(new Column("PARENT", 4));
    localArrayList.add(new Column("HEAD", 4));
    localArrayList.add(new Column("OPTIONS", 13));
    localArrayList.add(new Column("COLUMNS", 13));
    this.metaSchema = new Schema(this.database, 0, "", null, true);
    localCreateTableData.schema = this.metaSchema;
    localCreateTableData.tableName = "PAGE_INDEX";
    localCreateTableData.id = -1;
    localCreateTableData.temporary = false;
    localCreateTableData.persistData = true;
    localCreateTableData.persistIndexes = true;
    localCreateTableData.create = false;
    localCreateTableData.session = this.pageStoreSession;
    this.metaTable = new RegularTable(localCreateTableData);
    this.metaIndex = ((PageDataIndex)this.metaTable.getScanIndex(this.pageStoreSession));
    
    this.metaObjects.clear();
    this.metaObjects.put(Integer.valueOf(-1), this.metaIndex);
  }
  
  private void readMetaData()
  {
    Cursor localCursor = this.metaIndex.find(this.pageStoreSession, null, null);
    Row localRow;
    int i;
    while (localCursor.next())
    {
      localRow = localCursor.get();
      i = localRow.getValue(1).getInt();
      if (i == 0) {
        addMeta(localRow, this.pageStoreSession, false);
      }
    }
    localCursor = this.metaIndex.find(this.pageStoreSession, null, null);
    while (localCursor.next())
    {
      localRow = localCursor.get();
      i = localRow.getValue(1).getInt();
      if (i != 0) {
        addMeta(localRow, this.pageStoreSession, false);
      }
    }
  }
  
  private void removeMeta(Row paramRow)
  {
    int i = paramRow.getValue(0).getInt();
    PageIndex localPageIndex = (PageIndex)this.metaObjects.get(Integer.valueOf(i));
    localPageIndex.getTable().removeIndex(localPageIndex);
    if (((localPageIndex instanceof PageBtreeIndex)) || ((localPageIndex instanceof PageDelegateIndex))) {
      if (localPageIndex.isTemporary()) {
        this.pageStoreSession.removeLocalTempTableIndex(localPageIndex);
      } else {
        localPageIndex.getSchema().remove(localPageIndex);
      }
    }
    localPageIndex.remove(this.pageStoreSession);
    this.metaObjects.remove(Integer.valueOf(i));
  }
  
  private void addMeta(Row paramRow, Session paramSession, boolean paramBoolean)
  {
    int i = paramRow.getValue(0).getInt();
    int j = paramRow.getValue(1).getInt();
    int k = paramRow.getValue(2).getInt();
    int m = paramRow.getValue(3).getInt();
    String[] arrayOfString1 = StringUtils.arraySplit(paramRow.getValue(4).getString(), ',', false);
    
    String str = paramRow.getValue(5).getString();
    String[] arrayOfString2 = StringUtils.arraySplit(str, ',', false);
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("addMeta id=" + i + " type=" + j + " root=" + m + " parent=" + k + " columns=" + str);
    }
    if ((paramBoolean) && (m != 0))
    {
      writePage(m, createData());
      allocatePage(m);
    }
    this.metaRootPageId.put(i, m);
    Object localObject1;
    RegularTable localRegularTable;
    Index localIndex;
    if (j == 0)
    {
      localObject1 = new CreateTableData();
      if ((SysProperties.CHECK) && 
        (arrayOfString2 == null)) {
        throw DbException.throwInternalError(paramRow.toString());
      }
      int n = 0;
      for (int i1 = arrayOfString2.length; n < i1; n++)
      {
        localObject2 = new Column("C" + n, 4);
        ((CreateTableData)localObject1).columns.add(localObject2);
      }
      ((CreateTableData)localObject1).schema = this.metaSchema;
      ((CreateTableData)localObject1).tableName = ("T" + i);
      ((CreateTableData)localObject1).id = i;
      ((CreateTableData)localObject1).temporary = arrayOfString1[2].equals("temp");
      ((CreateTableData)localObject1).persistData = true;
      ((CreateTableData)localObject1).persistIndexes = true;
      ((CreateTableData)localObject1).create = false;
      ((CreateTableData)localObject1).session = paramSession;
      localRegularTable = new RegularTable((CreateTableData)localObject1);
      boolean bool = SysProperties.SORT_BINARY_UNSIGNED;
      if (arrayOfString1.length > 3) {
        bool = Boolean.parseBoolean(arrayOfString1[3]);
      }
      Object localObject2 = CompareMode.getInstance(arrayOfString1[0], Integer.parseInt(arrayOfString1[1]), bool);
      
      localRegularTable.setCompareMode((CompareMode)localObject2);
      localIndex = localRegularTable.getScanIndex(paramSession);
    }
    else
    {
      localObject1 = (Index)this.metaObjects.get(Integer.valueOf(k));
      if (localObject1 == null) {
        throw DbException.get(90030, "Table not found:" + k + " for " + paramRow + " meta:" + this.metaObjects);
      }
      localRegularTable = (RegularTable)((Index)localObject1).getTable();
      Column[] arrayOfColumn = localRegularTable.getColumns();
      int i2 = arrayOfString2.length;
      IndexColumn[] arrayOfIndexColumn = new IndexColumn[i2];
      Object localObject3;
      Object localObject4;
      int i4;
      for (int i3 = 0; i3 < i2; i3++)
      {
        localObject3 = arrayOfString2[i3];
        localObject4 = new IndexColumn();
        i4 = ((String)localObject3).indexOf('/');
        if (i4 >= 0)
        {
          localObject5 = ((String)localObject3).substring(i4 + 1);
          ((IndexColumn)localObject4).sortType = Integer.parseInt((String)localObject5);
          localObject3 = ((String)localObject3).substring(0, i4);
        }
        Object localObject5 = arrayOfColumn[Integer.parseInt(localObject3)];
        ((IndexColumn)localObject4).column = ((Column)localObject5);
        arrayOfIndexColumn[i3] = localObject4;
      }
      IndexType localIndexType;
      if (arrayOfString1[3].equals("d"))
      {
        localIndexType = IndexType.createPrimaryKey(true, false);
        localObject3 = localRegularTable.getColumns();
        for (Object localObject6 : arrayOfIndexColumn) {
          localObject3[localObject6.column.getColumnId()].setNullable(false);
        }
      }
      else
      {
        localIndexType = IndexType.createNonUnique(true);
      }
      localIndex = localRegularTable.addIndex(paramSession, "I" + i, i, arrayOfIndexColumn, localIndexType, false, null);
    }
    if ((localIndex instanceof MultiVersionIndex)) {
      localObject1 = (PageIndex)((MultiVersionIndex)localIndex).getBaseIndex();
    } else {
      localObject1 = (PageIndex)localIndex;
    }
    this.metaObjects.put(Integer.valueOf(i), localObject1);
  }
  
  public synchronized void addIndex(PageIndex paramPageIndex)
  {
    this.metaObjects.put(Integer.valueOf(paramPageIndex.getId()), paramPageIndex);
  }
  
  public void addMeta(PageIndex paramPageIndex, Session paramSession)
  {
    Table localTable = paramPageIndex.getTable();
    if ((SysProperties.CHECK) && 
      (!localTable.isTemporary())) {
      synchronized (this.database)
      {
        synchronized (this)
        {
          this.database.verifyMetaLocked(paramSession);
        }
      }
    }
    synchronized (this)
    {
      int i = (paramPageIndex instanceof PageDataIndex) ? 0 : 1;
      
      IndexColumn[] arrayOfIndexColumn = paramPageIndex.getIndexColumns();
      StatementBuilder localStatementBuilder = new StatementBuilder();
      for (localRow : arrayOfIndexColumn)
      {
        localStatementBuilder.appendExceptFirst(",");
        int m = localRow.column.getColumnId();
        localStatementBuilder.append(m);
        int n = localRow.sortType;
        if (n != 0)
        {
          localStatementBuilder.append('/');
          localStatementBuilder.append(n);
        }
      }
      ??? = localStatementBuilder.toString();
      CompareMode localCompareMode = localTable.getCompareMode();
      String str = localCompareMode.getName() + "," + localCompareMode.getStrength() + ",";
      if (localTable.isTemporary()) {
        str = str + "temp";
      }
      str = str + ",";
      if ((paramPageIndex instanceof PageDelegateIndex)) {
        str = str + "d";
      }
      str = str + "," + localCompareMode.isBinaryUnsigned();
      Row localRow = this.metaTable.getTemplateRow();
      localRow.setValue(0, ValueInt.get(paramPageIndex.getId()));
      localRow.setValue(1, ValueInt.get(i));
      localRow.setValue(2, ValueInt.get(localTable.getId()));
      localRow.setValue(3, ValueInt.get(paramPageIndex.getRootPageId()));
      localRow.setValue(4, ValueString.get(str));
      localRow.setValue(5, ValueString.get((String)???));
      localRow.setKey(paramPageIndex.getId() + 1);
      this.metaIndex.add(paramSession, localRow);
    }
  }
  
  public void removeMeta(Index paramIndex, Session paramSession)
  {
    if ((SysProperties.CHECK) && 
      (!paramIndex.getTable().isTemporary())) {
      synchronized (this.database)
      {
        synchronized (this)
        {
          this.database.verifyMetaLocked(paramSession);
        }
      }
    }
    synchronized (this)
    {
      if (!this.recoveryRunning)
      {
        removeMetaIndex(paramIndex, paramSession);
        this.metaObjects.remove(Integer.valueOf(paramIndex.getId()));
      }
    }
  }
  
  private void removeMetaIndex(Index paramIndex, Session paramSession)
  {
    int i = paramIndex.getId() + 1;
    Row localRow = this.metaIndex.getRow(paramSession, i);
    if (localRow.getKey() != i) {
      throw DbException.get(90030, "key: " + i + " index: " + paramIndex + " table: " + paramIndex.getTable() + " row: " + localRow);
    }
    this.metaIndex.remove(paramSession, localRow);
  }
  
  public void setMaxLogSize(long paramLong)
  {
    this.maxLogSize = paramLong;
  }
  
  public synchronized void setInDoubtTransactionState(int paramInt1, int paramInt2, boolean paramBoolean)
  {
    boolean bool = this.database.isReadOnly();
    try
    {
      this.database.setReadOnly(false);
      this.log.setInDoubtTransactionState(paramInt1, paramInt2, paramBoolean);
    }
    finally
    {
      this.database.setReadOnly(bool);
    }
  }
  
  public ArrayList<InDoubtTransaction> getInDoubtTransactions()
  {
    return this.log.getInDoubtTransactions();
  }
  
  public boolean isRecoveryRunning()
  {
    return this.recoveryRunning;
  }
  
  private void checkOpen()
  {
    if (this.file == null) {
      throw DbException.get(90098);
    }
  }
  
  public long getWriteCountTotal()
  {
    return this.writeCount + this.writeCountBase;
  }
  
  public long getWriteCount()
  {
    return this.writeCount;
  }
  
  public long getReadCount()
  {
    return this.readCount;
  }
  
  public synchronized void logTruncate(Session paramSession, int paramInt)
  {
    if (!this.recoveryRunning)
    {
      openForWriting();
      this.log.logTruncate(paramSession, paramInt);
    }
  }
  
  public int getRootPageId(int paramInt)
  {
    return this.metaRootPageId.get(paramInt);
  }
  
  public Cache getCache()
  {
    return this.cache;
  }
  
  private void checksumSet(byte[] paramArrayOfByte, int paramInt)
  {
    int i = this.pageSize;
    int j = paramArrayOfByte[0];
    if (j == 0) {
      return;
    }
    int k = 255 + (j & 0xFF);int m = 255 + k;
    m += k += (paramArrayOfByte[6] & 0xFF);
    m += k += (paramArrayOfByte[((i >> 1) - 1)] & 0xFF);
    m += k += (paramArrayOfByte[(i >> 1)] & 0xFF);
    m += k += (paramArrayOfByte[(i - 2)] & 0xFF);
    m += k += (paramArrayOfByte[(i - 1)] & 0xFF);
    paramArrayOfByte[1] = ((byte)((k & 0xFF) + (k >> 8) ^ paramInt));
    paramArrayOfByte[2] = ((byte)((m & 0xFF) + (m >> 8) ^ paramInt >> 8));
  }
  
  public static boolean checksumTest(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    int i = paramInt2;
    int j = 255 + (paramArrayOfByte[0] & 0xFF);int k = 255 + j;
    k += j += (paramArrayOfByte[6] & 0xFF);
    k += j += (paramArrayOfByte[((i >> 1) - 1)] & 0xFF);
    k += j += (paramArrayOfByte[(i >> 1)] & 0xFF);
    k += j += (paramArrayOfByte[(i - 2)] & 0xFF);
    k += j += (paramArrayOfByte[(i - 1)] & 0xFF);
    if ((paramArrayOfByte[1] != (byte)((j & 0xFF) + (j >> 8) ^ paramInt1)) || (paramArrayOfByte[2] != (byte)((k & 0xFF) + (k >> 8) ^ paramInt1 >> 8))) {
      return false;
    }
    return true;
  }
  
  public void incrementChangeCount()
  {
    this.changeCount += 1L;
    if ((SysProperties.CHECK) && (this.changeCount < 0L)) {
      throw DbException.throwInternalError("changeCount has wrapped");
    }
  }
  
  public long getChangeCount()
  {
    return this.changeCount;
  }
  
  public void setLogMode(int paramInt)
  {
    this.logMode = paramInt;
  }
  
  public int getLogMode()
  {
    return this.logMode;
  }
  
  public void setLockFile(boolean paramBoolean)
  {
    this.lockFile = paramBoolean;
  }
  
  public BitField getObjectIds()
  {
    BitField localBitField = new BitField();
    Cursor localCursor = this.metaIndex.find(this.pageStoreSession, null, null);
    while (localCursor.next())
    {
      Row localRow = localCursor.get();
      int i = localRow.getValue(0).getInt();
      if (i > 0) {
        localBitField.set(i);
      }
    }
    return localBitField;
  }
  
  public Session getPageStoreSession()
  {
    return this.pageStoreSession;
  }
  
  public synchronized void setBackup(boolean paramBoolean)
  {
    this.backupLevel += (paramBoolean ? 1 : -1);
  }
}
