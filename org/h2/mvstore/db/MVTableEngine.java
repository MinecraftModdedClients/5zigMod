package org.h2.mvstore.db;

import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.h2.api.TableEngine;
import org.h2.command.ddl.CreateTableData;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.FileStore;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStore.Builder;
import org.h2.mvstore.MVStoreTool;
import org.h2.store.InDoubtTransaction;
import org.h2.store.fs.FileChannelInputStream;
import org.h2.store.fs.FileUtils;
import org.h2.table.TableBase;
import org.h2.util.BitField;
import org.h2.util.New;

public class MVTableEngine
  implements TableEngine
{
  public static Store init(Database paramDatabase)
  {
    Store localStore = paramDatabase.getMvStore();
    if (localStore != null) {
      return localStore;
    }
    byte[] arrayOfByte = paramDatabase.getFileEncryptionKey();
    String str1 = paramDatabase.getDatabasePath();
    MVStore.Builder localBuilder = new MVStore.Builder();
    if (str1 == null)
    {
      localStore = new Store(paramDatabase, localBuilder);
    }
    else
    {
      String str2 = str1 + ".mv.db";
      MVStoreTool.compactCleanUp(str2);
      localBuilder.fileName(str2);
      localBuilder.pageSplitSize(paramDatabase.getPageSize());
      if (paramDatabase.isReadOnly())
      {
        localBuilder.readOnly();
      }
      else
      {
        boolean bool = FileUtils.exists(str2);
        if ((!bool) || (FileUtils.canWrite(str2)))
        {
          String str3 = FileUtils.getParent(str2);
          FileUtils.createDirectories(str3);
        }
      }
      int i;
      if (arrayOfByte != null)
      {
        char[] arrayOfChar = new char[arrayOfByte.length / 2];
        for (i = 0; i < arrayOfChar.length; i++) {
          arrayOfChar[i] = ((char)((arrayOfByte[(i + i)] & 0xFF) << 16 | arrayOfByte[(i + i + 1)] & 0xFF));
        }
        localBuilder.encryptionKey(arrayOfChar);
      }
      if (paramDatabase.getSettings().compressData)
      {
        localBuilder.compress();
        
        localBuilder.pageSplitSize(65536);
      }
      localBuilder.backgroundExceptionHandler(new Thread.UncaughtExceptionHandler()
      {
        public void uncaughtException(Thread paramAnonymousThread, Throwable paramAnonymousThrowable)
        {
          this.val$db.setBackgroundException(DbException.convert(paramAnonymousThrowable));
        }
      });
      try
      {
        localStore = new Store(paramDatabase, localBuilder);
      }
      catch (IllegalStateException localIllegalStateException)
      {
        i = DataUtils.getErrorCode(localIllegalStateException.getMessage());
        if (i == 6)
        {
          if (arrayOfByte != null) {
            throw DbException.get(90049, localIllegalStateException, new String[] { str2 });
          }
        }
        else
        {
          if (i == 7) {
            throw DbException.get(90020, localIllegalStateException, new String[] { str2 });
          }
          if (i == 1) {
            throw DbException.get(90028, localIllegalStateException, new String[] { str2 });
          }
        }
        throw DbException.get(90030, localIllegalStateException, new String[] { str2 });
      }
    }
    paramDatabase.setMvStore(localStore);
    return localStore;
  }
  
  public TableBase createTable(CreateTableData paramCreateTableData)
  {
    Database localDatabase = paramCreateTableData.session.getDatabase();
    Store localStore = init(localDatabase);
    MVTable localMVTable = new MVTable(paramCreateTableData, localStore);
    localMVTable.init(paramCreateTableData.session);
    localStore.tableMap.put(localMVTable.getMapName(), localMVTable);
    return localMVTable;
  }
  
  public static class Store
  {
    final ConcurrentHashMap<String, MVTable> tableMap = new ConcurrentHashMap();
    private final MVStore store;
    private final TransactionStore transactionStore;
    private long statisticsStart;
    private int temporaryMapId;
    
    public Store(Database paramDatabase, MVStore.Builder paramBuilder)
    {
      this.store = paramBuilder.open();
      this.transactionStore = new TransactionStore(this.store, new ValueDataType(null, paramDatabase, null));
      
      this.transactionStore.init();
    }
    
    public MVStore getStore()
    {
      return this.store;
    }
    
    public TransactionStore getTransactionStore()
    {
      return this.transactionStore;
    }
    
    public HashMap<String, MVTable> getTables()
    {
      return new HashMap(this.tableMap);
    }
    
    public void removeTable(MVTable paramMVTable)
    {
      this.tableMap.remove(paramMVTable.getMapName());
    }
    
    public void flush()
    {
      FileStore localFileStore = this.store.getFileStore();
      if ((localFileStore == null) || (localFileStore.isReadOnly())) {
        return;
      }
      if (!this.store.compact(50, 4194304)) {
        this.store.commit();
      }
    }
    
    public void closeImmediately()
    {
      if (this.store.isClosed()) {
        return;
      }
      this.store.closeImmediately();
    }
    
    public void initTransactions()
    {
      List localList = this.transactionStore.getOpenTransactions();
      for (TransactionStore.Transaction localTransaction : localList) {
        if (localTransaction.getStatus() == 3) {
          localTransaction.commit();
        } else if (localTransaction.getStatus() != 2) {
          localTransaction.rollback();
        }
      }
    }
    
    public void removeTemporaryMaps(BitField paramBitField)
    {
      for (String str : this.store.getMapNames()) {
        if (str.startsWith("temp."))
        {
          MVMap localMVMap = this.store.openMap(str);
          this.store.removeMap(localMVMap);
        }
        else if ((str.startsWith("table.")) || (str.startsWith("index.")))
        {
          int i = Integer.parseInt(str.substring(1 + str.indexOf(".")));
          if (!paramBitField.get(i))
          {
            ValueDataType localValueDataType1 = new ValueDataType(null, null, null);
            ValueDataType localValueDataType2 = new ValueDataType(null, null, null);
            TransactionStore.Transaction localTransaction = this.transactionStore.begin();
            TransactionStore.TransactionMap localTransactionMap = localTransaction.openMap(str, localValueDataType1, localValueDataType2);
            this.transactionStore.removeMap(localTransactionMap);
            localTransaction.commit();
          }
        }
      }
    }
    
    public synchronized String nextTemporaryMapName()
    {
      return "temp." + this.temporaryMapId++;
    }
    
    public void prepareCommit(Session paramSession, String paramString)
    {
      TransactionStore.Transaction localTransaction = paramSession.getTransaction();
      localTransaction.setName(paramString);
      localTransaction.prepare();
      this.store.commit();
    }
    
    public ArrayList<InDoubtTransaction> getInDoubtTransactions()
    {
      List localList = this.transactionStore.getOpenTransactions();
      ArrayList localArrayList = New.arrayList();
      for (TransactionStore.Transaction localTransaction : localList) {
        if (localTransaction.getStatus() == 2) {
          localArrayList.add(new MVTableEngine.MVInDoubtTransaction(this.store, localTransaction));
        }
      }
      return localArrayList;
    }
    
    public void setCacheSize(int paramInt)
    {
      this.store.setCacheSize(Math.max(1, paramInt / 1024));
    }
    
    public InputStream getInputStream()
    {
      FileChannel localFileChannel = this.store.getFileStore().getEncryptedFile();
      if (localFileChannel == null) {
        localFileChannel = this.store.getFileStore().getFile();
      }
      return new FileChannelInputStream(localFileChannel, false);
    }
    
    public void sync()
    {
      flush();
      this.store.sync();
    }
    
    public void compactFile(long paramLong)
    {
      this.store.setRetentionTime(0);
      long l1 = System.currentTimeMillis();
      while (this.store.compact(95, 16777216))
      {
        this.store.sync();
        this.store.compactMoveChunks(95, 16777216L);
        long l2 = System.currentTimeMillis() - l1;
        if (l2 > paramLong) {
          break;
        }
      }
    }
    
    public void close(long paramLong)
    {
      try
      {
        if ((!this.store.isClosed()) && (this.store.getFileStore() != null))
        {
          int i = 0;
          if (!this.store.getFileStore().isReadOnly())
          {
            this.transactionStore.close();
            if (paramLong == Long.MAX_VALUE) {
              i = 1;
            }
          }
          String str = this.store.getFileStore().getFileName();
          this.store.close();
          if ((i != 0) && (FileUtils.exists(str))) {
            MVStoreTool.compact(str, true);
          }
        }
      }
      catch (IllegalStateException localIllegalStateException)
      {
        int j = DataUtils.getErrorCode(localIllegalStateException.getMessage());
        if (j != 2) {
          if (j != 6) {}
        }
        this.store.closeImmediately();
        throw DbException.get(90028, localIllegalStateException, new String[] { "Closing" });
      }
    }
    
    public void statisticsStart()
    {
      FileStore localFileStore = this.store.getFileStore();
      this.statisticsStart = (localFileStore == null ? 0L : localFileStore.getReadCount());
    }
    
    public Map<String, Integer> statisticsEnd()
    {
      HashMap localHashMap = New.hashMap();
      FileStore localFileStore = this.store.getFileStore();
      int i = localFileStore == null ? 0 : (int)(localFileStore.getReadCount() - this.statisticsStart);
      localHashMap.put("reads", Integer.valueOf(i));
      return localHashMap;
    }
  }
  
  private static class MVInDoubtTransaction
    implements InDoubtTransaction
  {
    private final MVStore store;
    private final TransactionStore.Transaction transaction;
    private int state = 0;
    
    MVInDoubtTransaction(MVStore paramMVStore, TransactionStore.Transaction paramTransaction)
    {
      this.store = paramMVStore;
      this.transaction = paramTransaction;
    }
    
    public void setState(int paramInt)
    {
      if (paramInt == 1) {
        this.transaction.commit();
      } else {
        this.transaction.rollback();
      }
      this.store.commit();
      this.state = paramInt;
    }
    
    public String getState()
    {
      switch (this.state)
      {
      case 0: 
        return "IN_DOUBT";
      case 1: 
        return "COMMIT";
      case 2: 
        return "ROLLBACK";
      }
      throw DbException.throwInternalError("state=" + this.state);
    }
    
    public String getTransactionName()
    {
      return this.transaction.getName();
    }
  }
}
