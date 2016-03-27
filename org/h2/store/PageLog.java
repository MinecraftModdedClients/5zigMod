package org.h2.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.h2.compress.CompressLZF;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.Row;
import org.h2.util.BitField;
import org.h2.util.IntArray;
import org.h2.util.IntIntHashMap;
import org.h2.util.New;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class PageLog
{
  public static final int NOOP = 0;
  public static final int UNDO = 1;
  public static final int COMMIT = 2;
  public static final int PREPARE_COMMIT = 3;
  public static final int ROLLBACK = 4;
  public static final int ADD = 5;
  public static final int REMOVE = 6;
  public static final int TRUNCATE = 7;
  public static final int CHECKPOINT = 8;
  public static final int FREE_LOG = 9;
  static final int RECOVERY_STAGE_UNDO = 0;
  static final int RECOVERY_STAGE_ALLOCATE = 1;
  static final int RECOVERY_STAGE_REDO = 2;
  private static final boolean COMPRESS_UNDO = true;
  private final PageStore store;
  private final Trace trace;
  private Data writeBuffer;
  private PageOutputStream pageOut;
  private int firstTrunkPage;
  private int firstDataPage;
  private final Data dataBuffer;
  private int logKey;
  private int logSectionId;
  private int logPos;
  private int firstSectionId;
  private final CompressLZF compress;
  private final byte[] compressBuffer;
  private BitField undo = new BitField();
  private final BitField undoAll = new BitField();
  private final IntIntHashMap logSectionPageMap = new IntIntHashMap();
  private HashMap<Integer, SessionState> sessionStates = New.hashMap();
  private BitField usedLogPages;
  private boolean freeing;
  
  PageLog(PageStore paramPageStore)
  {
    this.store = paramPageStore;
    this.dataBuffer = paramPageStore.createData();
    this.trace = paramPageStore.getTrace();
    this.compress = new CompressLZF();
    this.compressBuffer = new byte[paramPageStore.getPageSize() * 2];
  }
  
  void openForWriting(int paramInt, boolean paramBoolean)
  {
    this.trace.debug("log openForWriting firstPage: " + paramInt);
    this.firstTrunkPage = paramInt;
    this.logKey += 1;
    this.pageOut = new PageOutputStream(this.store, paramInt, this.undoAll, this.logKey, paramBoolean);
    
    this.pageOut.reserve(1);
    
    this.store.setLogFirstPage(this.logKey, paramInt, this.pageOut.getCurrentDataPageId());
    
    this.writeBuffer = this.store.createData();
  }
  
  void free()
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("log free");
    }
    int i = 0;
    if (this.pageOut != null)
    {
      i = this.pageOut.getCurrentDataPageId();
      this.pageOut.freeReserved();
    }
    try
    {
      this.freeing = true;
      int j = 0;
      int k = 1024;int m = 0;
      PageStreamTrunk.Iterator localIterator = new PageStreamTrunk.Iterator(this.store, this.firstTrunkPage);
      while ((this.firstTrunkPage != 0) && (this.firstTrunkPage < this.store.getPageCount()))
      {
        PageStreamTrunk localPageStreamTrunk = localIterator.next();
        if (localPageStreamTrunk == null)
        {
          if (!localIterator.canDelete()) {
            break;
          }
          this.store.free(this.firstTrunkPage, false); break;
        }
        if (m++ >= k)
        {
          j = localPageStreamTrunk.getPos();
          m = 0;
          k *= 2;
        }
        else if ((j != 0) && (j == localPageStreamTrunk.getPos()))
        {
          throw DbException.throwInternalError("endless loop at " + localPageStreamTrunk);
        }
        localPageStreamTrunk.free(i);
        this.firstTrunkPage = localPageStreamTrunk.getNextTrunk();
      }
    }
    finally
    {
      this.freeing = false;
    }
  }
  
  void openForReading(int paramInt1, int paramInt2, int paramInt3)
  {
    this.logKey = paramInt1;
    this.firstTrunkPage = paramInt2;
    this.firstDataPage = paramInt3;
  }
  
  boolean recover(int paramInt)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("log recover stage: " + paramInt);
    }
    if (paramInt == 1)
    {
      localPageInputStream = new PageInputStream(this.store, this.logKey, this.firstTrunkPage, this.firstDataPage);
      
      this.usedLogPages = localPageInputStream.allocateAllPages();
      localPageInputStream.close();
      return true;
    }
    PageInputStream localPageInputStream = new PageInputStream(this.store, this.logKey, this.firstTrunkPage, this.firstDataPage);
    
    DataReader localDataReader = new DataReader(localPageInputStream);
    int i = 0;
    Data localData = this.store.createData();
    boolean bool = true;
    try
    {
      int j = 0;
      for (;;)
      {
        int k = localDataReader.readByte();
        if (k < 0) {
          break;
        }
        j++;
        bool = false;
        int m;
        int n;
        if (k == 1)
        {
          m = localDataReader.readVarInt();
          n = localDataReader.readVarInt();
          if (n == 0)
          {
            localDataReader.readFully(localData.getBytes(), this.store.getPageSize());
          }
          else if (n == 1)
          {
            Arrays.fill(localData.getBytes(), 0, this.store.getPageSize(), (byte)0);
          }
          else
          {
            localDataReader.readFully(this.compressBuffer, n);
            try
            {
              this.compress.expand(this.compressBuffer, 0, n, localData.getBytes(), 0, this.store.getPageSize());
            }
            catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException)
            {
              DbException.convertToIOException(localArrayIndexOutOfBoundsException);
            }
          }
          if (paramInt == 0) {
            if (!this.undo.get(m))
            {
              if (this.trace.isDebugEnabled()) {
                this.trace.debug("log undo {0}", new Object[] { Integer.valueOf(m) });
              }
              this.store.writePage(m, localData);
              this.undo.set(m);
              this.undoAll.set(m);
            }
            else if (this.trace.isDebugEnabled())
            {
              this.trace.debug("log undo skip {0}", new Object[] { Integer.valueOf(m) });
            }
          }
        }
        else if (k == 5)
        {
          m = localDataReader.readVarInt();
          n = localDataReader.readVarInt();
          Row localRow = readRow(localDataReader, localData);
          if (paramInt == 0) {
            this.store.allocateIfIndexRoot(j, n, localRow);
          } else if (paramInt == 2) {
            if (isSessionCommitted(m, i, j))
            {
              if (this.trace.isDebugEnabled()) {
                this.trace.debug("log redo + table: " + n + " s: " + m + " " + localRow);
              }
              this.store.redo(n, localRow, true);
            }
            else if (this.trace.isDebugEnabled())
            {
              this.trace.debug("log ignore s: " + m + " + table: " + n + " " + localRow);
            }
          }
        }
        else if (k == 6)
        {
          m = localDataReader.readVarInt();
          n = localDataReader.readVarInt();
          long l = localDataReader.readVarLong();
          if (paramInt == 2) {
            if (isSessionCommitted(m, i, j))
            {
              if (this.trace.isDebugEnabled()) {
                this.trace.debug("log redo - table: " + n + " s:" + m + " key: " + l);
              }
              this.store.redoDelete(n, l);
            }
            else if (this.trace.isDebugEnabled())
            {
              this.trace.debug("log ignore s: " + m + " - table: " + n + " " + l);
            }
          }
        }
        else if (k == 7)
        {
          m = localDataReader.readVarInt();
          n = localDataReader.readVarInt();
          if (paramInt == 2) {
            if (isSessionCommitted(m, i, j))
            {
              if (this.trace.isDebugEnabled()) {
                this.trace.debug("log redo truncate table: " + n);
              }
              this.store.redoTruncate(n);
            }
            else if (this.trace.isDebugEnabled())
            {
              this.trace.debug("log ignore s: " + m + " truncate table: " + n);
            }
          }
        }
        else
        {
          int i2;
          if (k == 3)
          {
            m = localDataReader.readVarInt();
            String str = localDataReader.readString();
            if (this.trace.isDebugEnabled()) {
              this.trace.debug("log prepare commit " + m + " " + str + " pos: " + j);
            }
            if (paramInt == 0)
            {
              i2 = localPageInputStream.getDataPage();
              setPrepareCommit(m, i2, str);
            }
          }
          else if (k == 4)
          {
            m = localDataReader.readVarInt();
            if (this.trace.isDebugEnabled()) {
              this.trace.debug("log rollback " + m + " pos: " + j);
            }
          }
          else if (k == 2)
          {
            m = localDataReader.readVarInt();
            if (this.trace.isDebugEnabled()) {
              this.trace.debug("log commit " + m + " pos: " + j);
            }
            if (paramInt == 0) {
              setLastCommitForSession(m, i, j);
            }
          }
          else if (k != 0)
          {
            if (k == 8)
            {
              i++;
            }
            else if (k == 9)
            {
              m = localDataReader.readVarInt();
              for (int i1 = 0; i1 < m; i1++)
              {
                i2 = localDataReader.readVarInt();
                if ((paramInt == 2) && 
                  (!this.usedLogPages.get(i2))) {
                  this.store.free(i2, false);
                }
              }
            }
            else if (this.trace.isDebugEnabled())
            {
              this.trace.debug("log end");
              break;
            }
          }
        }
      }
    }
    catch (DbException localDbException)
    {
      if (localDbException.getErrorCode() == 90030) {
        this.trace.debug("log recovery stopped");
      } else {
        throw localDbException;
      }
    }
    catch (IOException localIOException)
    {
      this.trace.debug("log recovery completed");
    }
    this.undo = new BitField();
    if (paramInt == 2) {
      this.usedLogPages = null;
    }
    return bool;
  }
  
  private void setPrepareCommit(int paramInt1, int paramInt2, String paramString)
  {
    SessionState localSessionState = getOrAddSessionState(paramInt1);
    PageStoreInDoubtTransaction localPageStoreInDoubtTransaction;
    if (paramString == null) {
      localPageStoreInDoubtTransaction = null;
    } else {
      localPageStoreInDoubtTransaction = new PageStoreInDoubtTransaction(this.store, paramInt1, paramInt2, paramString);
    }
    localSessionState.inDoubtTransaction = localPageStoreInDoubtTransaction;
  }
  
  public static Row readRow(DataReader paramDataReader, Data paramData)
    throws IOException
  {
    long l = paramDataReader.readVarLong();
    int i = paramDataReader.readVarInt();
    paramData.reset();
    paramData.checkCapacity(i);
    paramDataReader.readFully(paramData.getBytes(), i);
    int j = paramData.readVarInt();
    Value[] arrayOfValue = new Value[j];
    for (int k = 0; k < j; k++) {
      arrayOfValue[k] = paramData.readValue();
    }
    Row localRow = new Row(arrayOfValue, -1);
    localRow.setKey(l);
    return localRow;
  }
  
  boolean getUndo(int paramInt)
  {
    return this.undo.get(paramInt);
  }
  
  void addUndo(int paramInt, Data paramData)
  {
    if ((this.undo.get(paramInt)) || (this.freeing)) {
      return;
    }
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("log undo " + paramInt);
    }
    if ((SysProperties.CHECK) && 
      (paramData == null)) {
      DbException.throwInternalError("Undo entry not written");
    }
    this.undo.set(paramInt);
    this.undoAll.set(paramInt);
    Data localData = getBuffer();
    localData.writeByte((byte)1);
    localData.writeVarInt(paramInt);
    if (paramData.getBytes()[0] == 0)
    {
      localData.writeVarInt(1);
    }
    else
    {
      int i = this.store.getPageSize();
      
      int j = this.compress.compress(paramData.getBytes(), i, this.compressBuffer, 0);
      if (j < i)
      {
        localData.writeVarInt(j);
        localData.checkCapacity(j);
        localData.write(this.compressBuffer, 0, j);
      }
      else
      {
        localData.writeVarInt(0);
        localData.checkCapacity(i);
        localData.write(paramData.getBytes(), 0, i);
      }
    }
    write(localData);
  }
  
  private void freeLogPages(IntArray paramIntArray)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("log frees " + paramIntArray.get(0) + ".." + paramIntArray.get(paramIntArray.size() - 1));
    }
    Data localData = getBuffer();
    localData.writeByte((byte)9);
    int i = paramIntArray.size();
    localData.writeVarInt(i);
    for (int j = 0; j < i; j++) {
      localData.writeVarInt(paramIntArray.get(j));
    }
    write(localData);
  }
  
  private void write(Data paramData)
  {
    this.pageOut.write(paramData.getBytes(), 0, paramData.length());
    paramData.reset();
  }
  
  void commit(int paramInt)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("log commit s: " + paramInt);
    }
    if (this.store.getDatabase().getPageStore() == null) {
      return;
    }
    Data localData = getBuffer();
    localData.writeByte((byte)2);
    localData.writeVarInt(paramInt);
    write(localData);
    if (this.store.getDatabase().getFlushOnEachCommit()) {
      flush();
    }
  }
  
  void prepareCommit(Session paramSession, String paramString)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("log prepare commit s: " + paramSession.getId() + ", " + paramString);
    }
    if (this.store.getDatabase().getPageStore() == null) {
      return;
    }
    int i = this.store.getPageSize();
    this.pageOut.flush();
    this.pageOut.fillPage();
    Data localData = getBuffer();
    localData.writeByte((byte)3);
    localData.writeVarInt(paramSession.getId());
    localData.writeString(paramString);
    if (localData.length() >= PageStreamData.getCapacity(i)) {
      throw DbException.getInvalidValueException("transaction name (too long)", paramString);
    }
    write(localData);
    
    flushOut();
    this.pageOut.fillPage();
    if (this.store.getDatabase().getFlushOnEachCommit()) {
      flush();
    }
  }
  
  void logAddOrRemoveRow(Session paramSession, int paramInt, Row paramRow, boolean paramBoolean)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("log " + (paramBoolean ? "+" : "-") + " s: " + paramSession.getId() + " table: " + paramInt + " row: " + paramRow);
    }
    paramSession.addLogPos(this.logSectionId, this.logPos);
    this.logPos += 1;
    Data localData1 = this.dataBuffer;
    localData1.reset();
    int i = paramRow.getColumnCount();
    localData1.writeVarInt(i);
    localData1.checkCapacity(paramRow.getByteCount(localData1));
    int j;
    if (paramSession.isRedoLogBinaryEnabled()) {
      for (j = 0; j < i; j++) {
        localData1.writeValue(paramRow.getValue(j));
      }
    } else {
      for (j = 0; j < i; j++)
      {
        Value localValue = paramRow.getValue(j);
        if (localValue.getType() == 12) {
          localData1.writeValue(ValueNull.INSTANCE);
        } else {
          localData1.writeValue(localValue);
        }
      }
    }
    Data localData2 = getBuffer();
    localData2.writeByte((byte)(paramBoolean ? 5 : 6));
    localData2.writeVarInt(paramSession.getId());
    localData2.writeVarInt(paramInt);
    localData2.writeVarLong(paramRow.getKey());
    if (paramBoolean)
    {
      localData2.writeVarInt(localData1.length());
      localData2.checkCapacity(localData1.length());
      localData2.write(localData1.getBytes(), 0, localData1.length());
    }
    write(localData2);
  }
  
  void logTruncate(Session paramSession, int paramInt)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("log truncate s: " + paramSession.getId() + " table: " + paramInt);
    }
    paramSession.addLogPos(this.logSectionId, this.logPos);
    this.logPos += 1;
    Data localData = getBuffer();
    localData.writeByte((byte)7);
    localData.writeVarInt(paramSession.getId());
    localData.writeVarInt(paramInt);
    write(localData);
  }
  
  void flush()
  {
    if (this.pageOut != null) {
      flushOut();
    }
  }
  
  void checkpoint()
  {
    Data localData = getBuffer();
    localData.writeByte((byte)8);
    write(localData);
    this.undo = new BitField();
    this.logSectionId += 1;
    this.logPos = 0;
    this.pageOut.flush();
    this.pageOut.fillPage();
    int i = this.pageOut.getCurrentDataPageId();
    this.logSectionPageMap.put(this.logSectionId, i);
  }
  
  int getLogSectionId()
  {
    return this.logSectionId;
  }
  
  int getLogFirstSectionId()
  {
    return this.firstSectionId;
  }
  
  int getLogPos()
  {
    return this.logPos;
  }
  
  void removeUntil(int paramInt)
  {
    if (paramInt == 0) {
      return;
    }
    int i = this.logSectionPageMap.get(paramInt);
    this.firstTrunkPage = removeUntil(this.firstTrunkPage, i);
    this.store.setLogFirstPage(this.logKey, this.firstTrunkPage, i);
    while (this.firstSectionId < paramInt)
    {
      if (this.firstSectionId > 0) {
        this.logSectionPageMap.remove(this.firstSectionId);
      }
      this.firstSectionId += 1;
    }
  }
  
  private int removeUntil(int paramInt1, int paramInt2)
  {
    this.trace.debug("log.removeUntil " + paramInt1 + " " + paramInt2);
    int i = paramInt1;
    for (;;)
    {
      Page localPage = this.store.getPage(paramInt1);
      PageStreamTrunk localPageStreamTrunk = (PageStreamTrunk)localPage;
      if (localPageStreamTrunk == null) {
        throw DbException.throwInternalError("log.removeUntil not found: " + paramInt2 + " last " + i);
      }
      this.logKey = localPageStreamTrunk.getLogKey();
      i = localPageStreamTrunk.getPos();
      if (localPageStreamTrunk.contains(paramInt2)) {
        return i;
      }
      paramInt1 = localPageStreamTrunk.getNextTrunk();
      IntArray localIntArray = new IntArray();
      localIntArray.add(localPageStreamTrunk.getPos());
      for (int j = 0;; j++)
      {
        int k = localPageStreamTrunk.getPageData(j);
        if (k == -1) {
          break;
        }
        localIntArray.add(k);
      }
      freeLogPages(localIntArray);
      this.pageOut.free(localPageStreamTrunk);
    }
  }
  
  void close()
  {
    this.trace.debug("log close");
    if (this.pageOut != null)
    {
      this.pageOut.close();
      this.pageOut = null;
    }
    this.writeBuffer = null;
  }
  
  private boolean isSessionCommitted(int paramInt1, int paramInt2, int paramInt3)
  {
    SessionState localSessionState = (SessionState)this.sessionStates.get(Integer.valueOf(paramInt1));
    if (localSessionState == null) {
      return false;
    }
    return localSessionState.isCommitted(paramInt2, paramInt3);
  }
  
  private void setLastCommitForSession(int paramInt1, int paramInt2, int paramInt3)
  {
    SessionState localSessionState = getOrAddSessionState(paramInt1);
    localSessionState.lastCommitLog = paramInt2;
    localSessionState.lastCommitPos = paramInt3;
    localSessionState.inDoubtTransaction = null;
  }
  
  private SessionState getOrAddSessionState(int paramInt)
  {
    Integer localInteger = Integer.valueOf(paramInt);
    SessionState localSessionState = (SessionState)this.sessionStates.get(localInteger);
    if (localSessionState == null)
    {
      localSessionState = new SessionState();
      this.sessionStates.put(localInteger, localSessionState);
      localSessionState.sessionId = paramInt;
    }
    return localSessionState;
  }
  
  long getSize()
  {
    return this.pageOut == null ? 0L : this.pageOut.getSize();
  }
  
  ArrayList<InDoubtTransaction> getInDoubtTransactions()
  {
    ArrayList localArrayList = New.arrayList();
    for (SessionState localSessionState : this.sessionStates.values())
    {
      PageStoreInDoubtTransaction localPageStoreInDoubtTransaction = localSessionState.inDoubtTransaction;
      if (localPageStoreInDoubtTransaction != null) {
        localArrayList.add(localPageStoreInDoubtTransaction);
      }
    }
    return localArrayList;
  }
  
  void setInDoubtTransactionState(int paramInt1, int paramInt2, boolean paramBoolean)
  {
    PageStreamData localPageStreamData = (PageStreamData)this.store.getPage(paramInt2);
    localPageStreamData.initWrite();
    Data localData = this.store.createData();
    localData.writeByte((byte)(paramBoolean ? 2 : 4));
    localData.writeVarInt(paramInt1);
    byte[] arrayOfByte = localData.getBytes();
    localPageStreamData.write(arrayOfByte, 0, arrayOfByte.length);
    arrayOfByte = new byte[localPageStreamData.getRemaining()];
    localPageStreamData.write(arrayOfByte, 0, arrayOfByte.length);
    localPageStreamData.write();
  }
  
  void recoverEnd()
  {
    this.sessionStates = New.hashMap();
  }
  
  private void flushOut()
  {
    this.pageOut.flush();
  }
  
  private Data getBuffer()
  {
    if (this.writeBuffer.length() == 0) {
      return this.writeBuffer;
    }
    return this.store.createData();
  }
  
  int getMinPageId()
  {
    return this.pageOut == null ? 0 : this.pageOut.getMinPageId();
  }
}
