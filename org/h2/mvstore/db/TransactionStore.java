package org.h2.mvstore.db;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.DataUtils.MapEntry;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVMap.Builder;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;
import org.h2.mvstore.type.ObjectDataType;
import org.h2.util.New;

public class TransactionStore
{
  final MVStore store;
  final MVMap<Integer, Object[]> preparedTransactions;
  final MVMap<Long, Object[]> undoLog;
  private HashMap<Integer, MVMap<Object, VersionedValue>> maps = New.hashMap();
  private final DataType dataType;
  private boolean init;
  private int lastTransactionId;
  private int maxTransactionId = 65535;
  private int nextTempMapId;
  
  public TransactionStore(MVStore paramMVStore)
  {
    this(paramMVStore, new ObjectDataType());
  }
  
  public TransactionStore(MVStore paramMVStore, DataType paramDataType)
  {
    this.store = paramMVStore;
    this.dataType = paramDataType;
    this.preparedTransactions = paramMVStore.openMap("openTransactions", new MVMap.Builder());
    
    VersionedValueType localVersionedValueType = new VersionedValueType(paramDataType);
    ArrayType localArrayType = new ArrayType(new DataType[] { new ObjectDataType(), paramDataType, localVersionedValueType });
    
    MVMap.Builder localBuilder = new MVMap.Builder().valueType(localArrayType);
    
    this.undoLog = paramMVStore.openMap("undoLog", localBuilder);
    if (this.undoLog.getValueType() != localArrayType) {
      throw DataUtils.newIllegalStateException(100, "Undo map open with a different value type", new Object[0]);
    }
  }
  
  public synchronized void init()
  {
    this.init = true;
    for (Iterator localIterator = this.store.getMapNames().iterator(); localIterator.hasNext();)
    {
      localObject1 = (String)localIterator.next();
      if (((String)localObject1).startsWith("temp."))
      {
        MVMap localMVMap = openTempMap((String)localObject1);
        this.store.removeMap(localMVMap);
      }
    }
    synchronized (this.undoLog)
    {
      Object localObject1;
      if (this.undoLog.size() > 0)
      {
        localObject1 = (Long)this.undoLog.firstKey();
        this.lastTransactionId = getTransactionId(((Long)localObject1).longValue());
      }
    }
  }
  
  public void setMaxTransactionId(int paramInt)
  {
    this.maxTransactionId = paramInt;
  }
  
  static long getOperationId(int paramInt, long paramLong)
  {
    DataUtils.checkArgument((paramInt >= 0) && (paramInt < 16777216), "Transaction id out of range: {0}", new Object[] { Integer.valueOf(paramInt) });
    
    DataUtils.checkArgument((paramLong >= 0L) && (paramLong < 1099511627776L), "Transaction log id out of range: {0}", new Object[] { Long.valueOf(paramLong) });
    
    return paramInt << 40 | paramLong;
  }
  
  static int getTransactionId(long paramLong)
  {
    return (int)(paramLong >>> 40);
  }
  
  static long getLogId(long paramLong)
  {
    return paramLong & 0xFFFFFFFFFF;
  }
  
  public List<Transaction> getOpenTransactions()
  {
    synchronized (this.undoLog)
    {
      ArrayList localArrayList = New.arrayList();
      Long localLong = (Long)this.undoLog.firstKey();
      while (localLong != null)
      {
        int i = getTransactionId(localLong.longValue());
        localLong = (Long)this.undoLog.lowerKey(Long.valueOf(getOperationId(i + 1, 0L)));
        long l = getLogId(localLong.longValue()) + 1L;
        Object[] arrayOfObject = (Object[])this.preparedTransactions.get(Integer.valueOf(i));
        int j;
        String str;
        if (arrayOfObject == null)
        {
          if (this.undoLog.containsKey(Long.valueOf(getOperationId(i, 0L)))) {
            j = 1;
          } else {
            j = 3;
          }
          str = null;
        }
        else
        {
          j = ((Integer)arrayOfObject[0]).intValue();
          str = (String)arrayOfObject[1];
        }
        Transaction localTransaction = new Transaction(this, i, j, str, l);
        
        localArrayList.add(localTransaction);
        localLong = (Long)this.undoLog.ceilingKey(Long.valueOf(getOperationId(i + 1, 0L)));
      }
      return localArrayList;
    }
  }
  
  public synchronized void close()
  {
    this.store.commit();
  }
  
  public synchronized Transaction begin()
  {
    if (!this.init) {
      throw DataUtils.newIllegalStateException(103, "Not initialized", new Object[0]);
    }
    int i = ++this.lastTransactionId;
    if (this.lastTransactionId >= this.maxTransactionId) {
      this.lastTransactionId = 0;
    }
    int j = 1;
    return new Transaction(this, i, j, null, 0L);
  }
  
  synchronized void storeTransaction(Transaction paramTransaction)
  {
    if ((paramTransaction.getStatus() == 2) || (paramTransaction.getName() != null))
    {
      Object[] arrayOfObject = { Integer.valueOf(paramTransaction.getStatus()), paramTransaction.getName() };
      this.preparedTransactions.put(Integer.valueOf(paramTransaction.getId()), arrayOfObject);
    }
  }
  
  void log(Transaction paramTransaction, long paramLong, int paramInt, Object paramObject1, Object paramObject2)
  {
    Long localLong = Long.valueOf(getOperationId(paramTransaction.getId(), paramLong));
    Object[] arrayOfObject = { Integer.valueOf(paramInt), paramObject1, paramObject2 };
    synchronized (this.undoLog)
    {
      if ((paramLong == 0L) && 
        (this.undoLog.containsKey(localLong))) {
        throw DataUtils.newIllegalStateException(102, "An old transaction with the same id is still open: {0}", new Object[] { Integer.valueOf(paramTransaction.getId()) });
      }
      this.undoLog.put(localLong, arrayOfObject);
    }
  }
  
  public void logUndo(Transaction paramTransaction, long paramLong)
  {
    long[] arrayOfLong = { paramTransaction.getId(), paramLong };
    synchronized (this.undoLog)
    {
      this.undoLog.remove(arrayOfLong);
    }
  }
  
  synchronized <K, V> void removeMap(TransactionMap<K, V> paramTransactionMap)
  {
    this.maps.remove(Integer.valueOf(paramTransactionMap.mapId));
    this.store.removeMap(paramTransactionMap.map);
  }
  
  void commit(Transaction paramTransaction, long paramLong)
  {
    if (this.store.isClosed()) {
      return;
    }
    synchronized (this.undoLog)
    {
      paramTransaction.setStatus(3);
      for (long l = 0L; l < paramLong; l += 1L)
      {
        Long localLong = Long.valueOf(getOperationId(paramTransaction.getId(), l));
        Object[] arrayOfObject = (Object[])this.undoLog.get(localLong);
        if (arrayOfObject == null)
        {
          localLong = (Long)this.undoLog.ceilingKey(localLong);
          if ((localLong == null) || (getTransactionId(localLong.longValue()) != paramTransaction.getId())) {
            break;
          }
          l = getLogId(localLong.longValue()) - 1L;
        }
        else
        {
          int i = ((Integer)arrayOfObject[0]).intValue();
          MVMap localMVMap = openMap(i);
          if (localMVMap != null)
          {
            Object localObject1 = arrayOfObject[1];
            VersionedValue localVersionedValue1 = (VersionedValue)localMVMap.get(localObject1);
            if (localVersionedValue1 != null) {
              if (localVersionedValue1.value == null)
              {
                localMVMap.remove(localObject1);
              }
              else
              {
                VersionedValue localVersionedValue2 = new VersionedValue();
                localVersionedValue2.value = localVersionedValue1.value;
                localMVMap.put(localObject1, localVersionedValue2);
              }
            }
          }
          this.undoLog.remove(localLong);
        }
      }
    }
    endTransaction(paramTransaction);
  }
  
  synchronized <K> MVMap<K, VersionedValue> openMap(String paramString, DataType paramDataType1, DataType paramDataType2)
  {
    if (paramDataType1 == null) {
      paramDataType1 = new ObjectDataType();
    }
    if (paramDataType2 == null) {
      paramDataType2 = new ObjectDataType();
    }
    VersionedValueType localVersionedValueType = new VersionedValueType(paramDataType2);
    
    MVMap.Builder localBuilder = new MVMap.Builder().keyType(paramDataType1).valueType(localVersionedValueType);
    
    MVMap localMVMap1 = this.store.openMap(paramString, localBuilder);
    
    MVMap localMVMap2 = localMVMap1;
    this.maps.put(Integer.valueOf(localMVMap1.getId()), localMVMap2);
    return localMVMap1;
  }
  
  synchronized MVMap<Object, VersionedValue> openMap(int paramInt)
  {
    MVMap localMVMap = (MVMap)this.maps.get(Integer.valueOf(paramInt));
    if (localMVMap != null) {
      return localMVMap;
    }
    String str = this.store.getMapName(paramInt);
    if (str == null) {
      return null;
    }
    VersionedValueType localVersionedValueType = new VersionedValueType(this.dataType);
    MVMap.Builder localBuilder = new MVMap.Builder().keyType(this.dataType).valueType(localVersionedValueType);
    
    localMVMap = this.store.openMap(str, localBuilder);
    this.maps.put(Integer.valueOf(paramInt), localMVMap);
    return localMVMap;
  }
  
  synchronized MVMap<Object, Integer> createTempMap()
  {
    String str = "temp." + this.nextTempMapId++;
    return openTempMap(str);
  }
  
  MVMap<Object, Integer> openTempMap(String paramString)
  {
    MVMap.Builder localBuilder = new MVMap.Builder().keyType(this.dataType);
    
    return this.store.openMap(paramString, localBuilder);
  }
  
  synchronized void endTransaction(Transaction paramTransaction)
  {
    if (paramTransaction.getStatus() == 2) {
      this.preparedTransactions.remove(Integer.valueOf(paramTransaction.getId()));
    }
    paramTransaction.setStatus(0);
    if (this.store.getAutoCommitDelay() == 0)
    {
      this.store.commit();
      return;
    }
    if (this.undoLog.isEmpty())
    {
      int i = this.store.getUnsavedMemory();
      int j = this.store.getAutoCommitMemory();
      if (i * 4 > j * 3) {
        this.store.commit();
      }
    }
  }
  
  void rollbackTo(Transaction paramTransaction, long paramLong1, long paramLong2)
  {
    synchronized (this.undoLog)
    {
      for (long l = paramLong1 - 1L; l >= paramLong2; l -= 1L)
      {
        Long localLong = Long.valueOf(getOperationId(paramTransaction.getId(), l));
        Object[] arrayOfObject = (Object[])this.undoLog.get(localLong);
        if (arrayOfObject == null)
        {
          localLong = (Long)this.undoLog.floorKey(localLong);
          if ((localLong == null) || (getTransactionId(localLong.longValue()) != paramTransaction.getId())) {
            break;
          }
          l = getLogId(localLong.longValue()) + 1L;
        }
        else
        {
          int i = ((Integer)arrayOfObject[0]).intValue();
          MVMap localMVMap = openMap(i);
          if (localMVMap != null)
          {
            Object localObject1 = arrayOfObject[1];
            VersionedValue localVersionedValue = (VersionedValue)arrayOfObject[2];
            if (localVersionedValue == null) {
              localMVMap.remove(localObject1);
            } else {
              localMVMap.put(localObject1, localVersionedValue);
            }
          }
          this.undoLog.remove(localLong);
        }
      }
    }
  }
  
  Iterator<Change> getChanges(final Transaction paramTransaction, final long paramLong1, long paramLong2)
  {
    new Iterator()
    {
      private long logId;
      private TransactionStore.Change current;
      
      private void fetchNext()
      {
        synchronized (TransactionStore.this.undoLog)
        {
          while (this.logId >= paramTransaction)
          {
            Long localLong = Long.valueOf(TransactionStore.getOperationId(this.val$t.getId(), this.logId));
            Object[] arrayOfObject = (Object[])TransactionStore.this.undoLog.get(localLong);
            this.logId -= 1L;
            if (arrayOfObject == null)
            {
              localLong = (Long)TransactionStore.this.undoLog.floorKey(localLong);
              if ((localLong == null) || (TransactionStore.getTransactionId(localLong.longValue()) != this.val$t.getId())) {
                break;
              }
              this.logId = TransactionStore.getLogId(localLong.longValue());
            }
            else
            {
              int i = ((Integer)arrayOfObject[0]).intValue();
              MVMap localMVMap = TransactionStore.this.openMap(i);
              if (localMVMap != null)
              {
                this.current = new TransactionStore.Change();
                this.current.mapName = localMVMap.getName();
                this.current.key = arrayOfObject[1];
                TransactionStore.VersionedValue localVersionedValue = (TransactionStore.VersionedValue)arrayOfObject[2];
                this.current.value = (localVersionedValue == null ? null : localVersionedValue.value);
                
                return;
              }
            }
          }
        }
        this.current = null;
      }
      
      public boolean hasNext()
      {
        return this.current != null;
      }
      
      public TransactionStore.Change next()
      {
        if (this.current == null) {
          throw DataUtils.newUnsupportedOperationException("no data");
        }
        TransactionStore.Change localChange = this.current;
        fetchNext();
        return localChange;
      }
      
      public void remove()
      {
        throw DataUtils.newUnsupportedOperationException("remove");
      }
    };
  }
  
  public static class Change
  {
    public String mapName;
    public Object key;
    public Object value;
  }
  
  public static class Transaction
  {
    public static final int STATUS_CLOSED = 0;
    public static final int STATUS_OPEN = 1;
    public static final int STATUS_PREPARED = 2;
    public static final int STATUS_COMMITTING = 3;
    final TransactionStore store;
    final int transactionId;
    long logId;
    private int status;
    private String name;
    
    Transaction(TransactionStore paramTransactionStore, int paramInt1, int paramInt2, String paramString, long paramLong)
    {
      this.store = paramTransactionStore;
      this.transactionId = paramInt1;
      this.status = paramInt2;
      this.name = paramString;
      this.logId = paramLong;
    }
    
    public int getId()
    {
      return this.transactionId;
    }
    
    public int getStatus()
    {
      return this.status;
    }
    
    void setStatus(int paramInt)
    {
      this.status = paramInt;
    }
    
    public void setName(String paramString)
    {
      checkNotClosed();
      this.name = paramString;
      this.store.storeTransaction(this);
    }
    
    public String getName()
    {
      return this.name;
    }
    
    public long setSavepoint()
    {
      return this.logId;
    }
    
    void log(int paramInt, Object paramObject1, Object paramObject2)
    {
      this.store.log(this, this.logId, paramInt, paramObject1, paramObject2);
      
      this.logId += 1L;
    }
    
    void logUndo()
    {
      this.store.logUndo(this, --this.logId);
    }
    
    public <K, V> TransactionStore.TransactionMap<K, V> openMap(String paramString)
    {
      return openMap(paramString, null, null);
    }
    
    public <K, V> TransactionStore.TransactionMap<K, V> openMap(String paramString, DataType paramDataType1, DataType paramDataType2)
    {
      checkNotClosed();
      MVMap localMVMap = this.store.openMap(paramString, paramDataType1, paramDataType2);
      
      int i = localMVMap.getId();
      return new TransactionStore.TransactionMap(this, localMVMap, i);
    }
    
    public <K, V> TransactionStore.TransactionMap<K, V> openMap(MVMap<K, TransactionStore.VersionedValue> paramMVMap)
    {
      checkNotClosed();
      int i = paramMVMap.getId();
      return new TransactionStore.TransactionMap(this, paramMVMap, i);
    }
    
    public void prepare()
    {
      checkNotClosed();
      this.status = 2;
      this.store.storeTransaction(this);
    }
    
    public void commit()
    {
      checkNotClosed();
      this.store.commit(this, this.logId);
    }
    
    public void rollbackToSavepoint(long paramLong)
    {
      checkNotClosed();
      this.store.rollbackTo(this, this.logId, paramLong);
      this.logId = paramLong;
    }
    
    public void rollback()
    {
      checkNotClosed();
      this.store.rollbackTo(this, this.logId, 0L);
      this.store.endTransaction(this);
    }
    
    public Iterator<TransactionStore.Change> getChanges(long paramLong)
    {
      return this.store.getChanges(this, this.logId, paramLong);
    }
    
    void checkNotClosed()
    {
      if (this.status == 0) {
        throw DataUtils.newIllegalStateException(4, "Transaction is closed", new Object[0]);
      }
    }
    
    public <K, V> void removeMap(TransactionStore.TransactionMap<K, V> paramTransactionMap)
    {
      this.store.removeMap(paramTransactionMap);
    }
    
    public String toString()
    {
      return "" + this.transactionId;
    }
  }
  
  public static class TransactionMap<K, V>
  {
    final int mapId;
    long readLogId = Long.MAX_VALUE;
    final MVMap<K, TransactionStore.VersionedValue> map;
    private TransactionStore.Transaction transaction;
    
    TransactionMap(TransactionStore.Transaction paramTransaction, MVMap<K, TransactionStore.VersionedValue> paramMVMap, int paramInt)
    {
      this.transaction = paramTransaction;
      this.map = paramMVMap;
      this.mapId = paramInt;
    }
    
    public void setSavepoint(long paramLong)
    {
      this.readLogId = paramLong;
    }
    
    public TransactionMap<K, V> getInstance(TransactionStore.Transaction paramTransaction, long paramLong)
    {
      TransactionMap localTransactionMap = new TransactionMap(paramTransaction, this.map, this.mapId);
      
      localTransactionMap.setSavepoint(paramLong);
      return localTransactionMap;
    }
    
    public long sizeAsLongMax()
    {
      return this.map.sizeAsLong();
    }
    
    public long sizeAsLong()
    {
      long l1 = this.map.sizeAsLong();
      MVMap localMVMap = this.transaction.store.undoLog;
      long l2;
      synchronized (localMVMap)
      {
        l2 = localMVMap.sizeAsLong();
      }
      if (l2 == 0L) {
        return l1;
      }
      Object localObject2;
      Object localObject3;
      if (l2 > l1)
      {
        ??? = 0L;
        Cursor localCursor = this.map.cursor(null);
        while (localCursor.hasNext())
        {
          localObject2 = localCursor.next();
          localObject3 = (TransactionStore.VersionedValue)localCursor.getValue();
          localObject3 = getValue(localObject2, this.readLogId, (TransactionStore.VersionedValue)localObject3);
          if ((localObject3 != null) && (((TransactionStore.VersionedValue)localObject3).value != null)) {
            ??? += 1L;
          }
        }
        return ???;
      }
      synchronized (localMVMap)
      {
        long l3 = this.map.sizeAsLong();
        localObject2 = this.transaction.store.createTempMap();
        try
        {
          for (localObject3 = localMVMap.entrySet().iterator(); ((Iterator)localObject3).hasNext();)
          {
            Map.Entry localEntry = (Map.Entry)((Iterator)localObject3).next();
            Object[] arrayOfObject = (Object[])localEntry.getValue();
            int i = ((Integer)arrayOfObject[0]).intValue();
            if (i == this.mapId)
            {
              Object localObject4 = arrayOfObject[1];
              if (get(localObject4) == null)
              {
                Integer localInteger = (Integer)((MVMap)localObject2).put(localObject4, Integer.valueOf(1));
                if (localInteger == null) {
                  l3 -= 1L;
                }
              }
            }
          }
        }
        finally
        {
          this.transaction.store.store.removeMap((MVMap)localObject2);
        }
        return l3;
      }
    }
    
    public V remove(K paramK)
    {
      return (V)set(paramK, null);
    }
    
    public V put(K paramK, V paramV)
    {
      DataUtils.checkArgument(paramV != null, "The value may not be null", new Object[0]);
      return (V)set(paramK, paramV);
    }
    
    public V putCommitted(K paramK, V paramV)
    {
      DataUtils.checkArgument(paramV != null, "The value may not be null", new Object[0]);
      TransactionStore.VersionedValue localVersionedValue1 = new TransactionStore.VersionedValue();
      localVersionedValue1.value = paramV;
      TransactionStore.VersionedValue localVersionedValue2 = (TransactionStore.VersionedValue)this.map.put(paramK, localVersionedValue1);
      return localVersionedValue2 == null ? null : localVersionedValue2.value;
    }
    
    private V set(K paramK, V paramV)
    {
      this.transaction.checkNotClosed();
      Object localObject = get(paramK);
      boolean bool = trySet(paramK, paramV, false);
      if (bool) {
        return (V)localObject;
      }
      throw DataUtils.newIllegalStateException(101, "Entry is locked", new Object[0]);
    }
    
    public boolean tryRemove(K paramK)
    {
      return trySet(paramK, null, false);
    }
    
    public boolean tryPut(K paramK, V paramV)
    {
      DataUtils.checkArgument(paramV != null, "The value may not be null", new Object[0]);
      return trySet(paramK, paramV, false);
    }
    
    public boolean trySet(K paramK, V paramV, boolean paramBoolean)
    {
      TransactionStore.VersionedValue localVersionedValue1 = (TransactionStore.VersionedValue)this.map.get(paramK);
      if (paramBoolean)
      {
        localVersionedValue2 = getValue(paramK, this.readLogId);
        if (!this.map.areValuesEqual(localVersionedValue2, localVersionedValue1))
        {
          long l1 = TransactionStore.getTransactionId(localVersionedValue1.operationId);
          if (l1 == this.transaction.transactionId)
          {
            if (paramV == null) {
              return true;
            }
            if (localVersionedValue1.value != null) {
              return false;
            }
          }
          else
          {
            return false;
          }
        }
      }
      TransactionStore.VersionedValue localVersionedValue2 = new TransactionStore.VersionedValue();
      localVersionedValue2.operationId = TransactionStore.getOperationId(this.transaction.transactionId, this.transaction.logId);
      
      localVersionedValue2.value = paramV;
      if (localVersionedValue1 == null)
      {
        this.transaction.log(this.mapId, paramK, localVersionedValue1);
        TransactionStore.VersionedValue localVersionedValue3 = (TransactionStore.VersionedValue)this.map.putIfAbsent(paramK, localVersionedValue2);
        if (localVersionedValue3 != null)
        {
          this.transaction.logUndo();
          return false;
        }
        return true;
      }
      long l2 = localVersionedValue1.operationId;
      if (l2 == 0L)
      {
        this.transaction.log(this.mapId, paramK, localVersionedValue1);
        if (!this.map.replace(paramK, localVersionedValue1, localVersionedValue2))
        {
          this.transaction.logUndo();
          return false;
        }
        return true;
      }
      int i = TransactionStore.getTransactionId(localVersionedValue1.operationId);
      if (i == this.transaction.transactionId)
      {
        this.transaction.log(this.mapId, paramK, localVersionedValue1);
        if (!this.map.replace(paramK, localVersionedValue1, localVersionedValue2))
        {
          this.transaction.logUndo();
          return false;
        }
        return true;
      }
      return false;
    }
    
    public V get(K paramK)
    {
      return (V)get(paramK, this.readLogId);
    }
    
    public V getLatest(K paramK)
    {
      return (V)get(paramK, Long.MAX_VALUE);
    }
    
    public boolean containsKey(K paramK)
    {
      return get(paramK) != null;
    }
    
    public V get(K paramK, long paramLong)
    {
      TransactionStore.VersionedValue localVersionedValue = getValue(paramK, paramLong);
      return localVersionedValue == null ? null : localVersionedValue.value;
    }
    
    public boolean isSameTransaction(K paramK)
    {
      TransactionStore.VersionedValue localVersionedValue = (TransactionStore.VersionedValue)this.map.get(paramK);
      if (localVersionedValue == null) {
        return false;
      }
      int i = TransactionStore.getTransactionId(localVersionedValue.operationId);
      return i == this.transaction.transactionId;
    }
    
    private TransactionStore.VersionedValue getValue(K paramK, long paramLong)
    {
      TransactionStore.VersionedValue localVersionedValue = (TransactionStore.VersionedValue)this.map.get(paramK);
      return getValue(paramK, paramLong, localVersionedValue);
    }
    
    TransactionStore.VersionedValue getValue(K paramK, long paramLong, TransactionStore.VersionedValue paramVersionedValue)
    {
      for (;;)
      {
        if (paramVersionedValue == null) {
          return null;
        }
        long l1 = paramVersionedValue.operationId;
        if (l1 == 0L) {
          return paramVersionedValue;
        }
        int i = TransactionStore.getTransactionId(l1);
        if (i == this.transaction.transactionId) {
          if (TransactionStore.getLogId(l1) < paramLong) {
            return paramVersionedValue;
          }
        }
        Object[] arrayOfObject;
        synchronized (this.transaction.store.undoLog)
        {
          arrayOfObject = (Object[])this.transaction.store.undoLog.get(Long.valueOf(l1));
        }
        if (arrayOfObject == null)
        {
          paramVersionedValue = (TransactionStore.VersionedValue)this.map.get(paramK);
          if ((paramVersionedValue != null) && (paramVersionedValue.operationId == l1)) {
            throw DataUtils.newIllegalStateException(100, "The transaction log might be corrupt for key {0}", new Object[] { paramK });
          }
        }
        else
        {
          paramVersionedValue = (TransactionStore.VersionedValue)arrayOfObject[2];
        }
        if (paramVersionedValue != null)
        {
          long l2 = paramVersionedValue.operationId;
          if (l2 != 0L)
          {
            int j = TransactionStore.getTransactionId(l2);
            if ((j == i) && 
            
              (TransactionStore.getLogId(l2) > TransactionStore.getLogId(l1))) {
              break;
            }
          }
        }
      }
      throw DataUtils.newIllegalStateException(100, "The transaction log might be corrupt for key {0}", new Object[] { paramK });
    }
    
    public boolean isClosed()
    {
      return this.map.isClosed();
    }
    
    public void clear()
    {
      this.map.clear();
    }
    
    public K firstKey()
    {
      Iterator localIterator = keyIterator(null);
      return (K)(localIterator.hasNext() ? localIterator.next() : null);
    }
    
    public K lastKey()
    {
      Object localObject = this.map.lastKey();
      for (;;)
      {
        if (localObject == null) {
          return null;
        }
        if (get(localObject) != null) {
          return (K)localObject;
        }
        localObject = this.map.lowerKey(localObject);
      }
    }
    
    public K getLatestCeilingKey(K paramK)
    {
      Iterator localIterator = this.map.keyIterator(paramK);
      while (localIterator.hasNext())
      {
        paramK = localIterator.next();
        if (get(paramK, Long.MAX_VALUE) != null) {
          return paramK;
        }
      }
      return null;
    }
    
    public K higherKey(K paramK)
    {
      for (;;)
      {
        Object localObject = this.map.higherKey(paramK);
        if ((localObject == null) || (get(localObject) != null)) {
          return (K)localObject;
        }
        paramK = (K)localObject;
      }
    }
    
    public K relativeKey(K paramK, long paramLong)
    {
      Object localObject = paramLong > 0L ? this.map.ceilingKey(paramK) : this.map.floorKey(paramK);
      if (localObject == null) {
        return (K)localObject;
      }
      long l = this.map.getKeyIndex(localObject);
      return (K)this.map.getKey(l + paramLong);
    }
    
    public K lowerKey(K paramK)
    {
      for (;;)
      {
        Object localObject = this.map.lowerKey(paramK);
        if ((localObject == null) || (get(localObject) != null)) {
          return (K)localObject;
        }
        paramK = (K)localObject;
      }
    }
    
    public Iterator<K> keyIterator(K paramK)
    {
      return keyIterator(paramK, false);
    }
    
    public Iterator<K> keyIterator(final K paramK, final boolean paramBoolean)
    {
      new Iterator()
      {
        private K currentKey;
        private Cursor<K, TransactionStore.VersionedValue> cursor;
        
        private void fetchNext()
        {
          while (this.cursor.hasNext())
          {
            Object localObject;
            try
            {
              localObject = this.cursor.next();
            }
            catch (IllegalStateException localIllegalStateException)
            {
              if (DataUtils.getErrorCode(localIllegalStateException.getMessage()) == 9)
              {
                this.cursor = TransactionStore.TransactionMap.this.map.cursor(this.currentKey);
                if (!this.cursor.hasNext()) {
                  break;
                }
                this.cursor.next();
                if (!this.cursor.hasNext()) {
                  break;
                }
                localObject = this.cursor.next();
              }
              else
              {
                throw localIllegalStateException;
              }
            }
            this.currentKey = localObject;
            if (paramBoolean) {
              return;
            }
            if (TransactionStore.TransactionMap.this.containsKey(localObject)) {
              return;
            }
          }
          this.currentKey = null;
        }
        
        public boolean hasNext()
        {
          return this.currentKey != null;
        }
        
        public K next()
        {
          Object localObject = this.currentKey;
          fetchNext();
          return (K)localObject;
        }
        
        public void remove()
        {
          throw DataUtils.newUnsupportedOperationException("Removing is not supported");
        }
      };
    }
    
    public Iterator<Map.Entry<K, V>> entryIterator(final K paramK)
    {
      new Iterator()
      {
        private Map.Entry<K, V> current;
        private K currentKey;
        private Cursor<K, TransactionStore.VersionedValue> cursor;
        
        private void fetchNext()
        {
          while (this.cursor.hasNext())
          {
            Object localObject1;
            try
            {
              localObject1 = this.cursor.next();
            }
            catch (IllegalStateException localIllegalStateException)
            {
              if (DataUtils.getErrorCode(localIllegalStateException.getMessage()) == 9)
              {
                this.cursor = TransactionStore.TransactionMap.this.map.cursor(this.currentKey);
                if (!this.cursor.hasNext()) {
                  break;
                }
                this.cursor.next();
                if (!this.cursor.hasNext()) {
                  break;
                }
                localObject1 = this.cursor.next();
              }
              else
              {
                throw localIllegalStateException;
              }
            }
            Object localObject2 = localObject1;
            TransactionStore.VersionedValue localVersionedValue = (TransactionStore.VersionedValue)this.cursor.getValue();
            localVersionedValue = TransactionStore.TransactionMap.this.getValue(localObject2, TransactionStore.TransactionMap.this.readLogId, localVersionedValue);
            if ((localVersionedValue != null) && (localVersionedValue.value != null))
            {
              Object localObject3 = localVersionedValue.value;
              this.current = new DataUtils.MapEntry(localObject2, localObject3);
              this.currentKey = localObject2;
              return;
            }
          }
          this.current = null;
          this.currentKey = null;
        }
        
        public boolean hasNext()
        {
          return this.current != null;
        }
        
        public Map.Entry<K, V> next()
        {
          Map.Entry localEntry = this.current;
          fetchNext();
          return localEntry;
        }
        
        public void remove()
        {
          throw DataUtils.newUnsupportedOperationException("Removing is not supported");
        }
      };
    }
    
    public Iterator<K> wrapIterator(final Iterator<K> paramIterator, final boolean paramBoolean)
    {
      new Iterator()
      {
        private K current;
        
        private void fetchNext()
        {
          while (paramIterator.hasNext())
          {
            this.current = paramIterator.next();
            if (paramBoolean) {
              return;
            }
            if (TransactionStore.TransactionMap.this.containsKey(this.current)) {
              return;
            }
          }
          this.current = null;
        }
        
        public boolean hasNext()
        {
          return this.current != null;
        }
        
        public K next()
        {
          Object localObject = this.current;
          fetchNext();
          return (K)localObject;
        }
        
        public void remove()
        {
          throw DataUtils.newUnsupportedOperationException("Removing is not supported");
        }
      };
    }
    
    public TransactionStore.Transaction getTransaction()
    {
      return this.transaction;
    }
    
    public DataType getKeyType()
    {
      return this.map.getKeyType();
    }
  }
  
  static class VersionedValue
  {
    public long operationId;
    public Object value;
    
    public String toString()
    {
      return this.value + (this.operationId == 0L ? "" : new StringBuilder().append(" ").append(TransactionStore.getTransactionId(this.operationId)).append("/").append(TransactionStore.getLogId(this.operationId)).toString());
    }
  }
  
  public static class VersionedValueType
    implements DataType
  {
    private final DataType valueType;
    
    VersionedValueType(DataType paramDataType)
    {
      this.valueType = paramDataType;
    }
    
    public int getMemory(Object paramObject)
    {
      TransactionStore.VersionedValue localVersionedValue = (TransactionStore.VersionedValue)paramObject;
      return this.valueType.getMemory(localVersionedValue.value) + 8;
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (paramObject1 == paramObject2) {
        return 0;
      }
      TransactionStore.VersionedValue localVersionedValue1 = (TransactionStore.VersionedValue)paramObject1;
      TransactionStore.VersionedValue localVersionedValue2 = (TransactionStore.VersionedValue)paramObject2;
      long l = localVersionedValue1.operationId - localVersionedValue2.operationId;
      if (l == 0L) {
        return this.valueType.compare(localVersionedValue1.value, localVersionedValue2.value);
      }
      return Long.signum(l);
    }
    
    public void read(ByteBuffer paramByteBuffer, Object[] paramArrayOfObject, int paramInt, boolean paramBoolean)
    {
      int i;
      if (paramByteBuffer.get() == 0) {
        for (i = 0; i < paramInt; i++)
        {
          TransactionStore.VersionedValue localVersionedValue = new TransactionStore.VersionedValue();
          localVersionedValue.value = this.valueType.read(paramByteBuffer);
          paramArrayOfObject[i] = localVersionedValue;
        }
      } else {
        for (i = 0; i < paramInt; i++) {
          paramArrayOfObject[i] = read(paramByteBuffer);
        }
      }
    }
    
    public Object read(ByteBuffer paramByteBuffer)
    {
      TransactionStore.VersionedValue localVersionedValue = new TransactionStore.VersionedValue();
      localVersionedValue.operationId = DataUtils.readVarLong(paramByteBuffer);
      if (paramByteBuffer.get() == 1) {
        localVersionedValue.value = this.valueType.read(paramByteBuffer);
      }
      return localVersionedValue;
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object[] paramArrayOfObject, int paramInt, boolean paramBoolean)
    {
      int i = 1;
      TransactionStore.VersionedValue localVersionedValue;
      for (int j = 0; j < paramInt; j++)
      {
        localVersionedValue = (TransactionStore.VersionedValue)paramArrayOfObject[j];
        if ((localVersionedValue.operationId != 0L) || (localVersionedValue.value == null)) {
          i = 0;
        }
      }
      if (i != 0)
      {
        paramWriteBuffer.put((byte)0);
        for (j = 0; j < paramInt; j++)
        {
          localVersionedValue = (TransactionStore.VersionedValue)paramArrayOfObject[j];
          this.valueType.write(paramWriteBuffer, localVersionedValue.value);
        }
      }
      else
      {
        paramWriteBuffer.put((byte)1);
        for (j = 0; j < paramInt; j++) {
          write(paramWriteBuffer, paramArrayOfObject[j]);
        }
      }
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      TransactionStore.VersionedValue localVersionedValue = (TransactionStore.VersionedValue)paramObject;
      paramWriteBuffer.putVarLong(localVersionedValue.operationId);
      if (localVersionedValue.value == null)
      {
        paramWriteBuffer.put((byte)0);
      }
      else
      {
        paramWriteBuffer.put((byte)1);
        this.valueType.write(paramWriteBuffer, localVersionedValue.value);
      }
    }
  }
  
  public static class ArrayType
    implements DataType
  {
    private final int arrayLength;
    private final DataType[] elementTypes;
    
    ArrayType(DataType[] paramArrayOfDataType)
    {
      this.arrayLength = paramArrayOfDataType.length;
      this.elementTypes = paramArrayOfDataType;
    }
    
    public int getMemory(Object paramObject)
    {
      Object[] arrayOfObject = (Object[])paramObject;
      int i = 0;
      for (int j = 0; j < this.arrayLength; j++)
      {
        DataType localDataType = this.elementTypes[j];
        Object localObject = arrayOfObject[j];
        if (localObject != null) {
          i += localDataType.getMemory(localObject);
        }
      }
      return i;
    }
    
    public int compare(Object paramObject1, Object paramObject2)
    {
      if (paramObject1 == paramObject2) {
        return 0;
      }
      Object[] arrayOfObject1 = (Object[])paramObject1;
      Object[] arrayOfObject2 = (Object[])paramObject2;
      for (int i = 0; i < this.arrayLength; i++)
      {
        DataType localDataType = this.elementTypes[i];
        int j = localDataType.compare(arrayOfObject1[i], arrayOfObject2[i]);
        if (j != 0) {
          return j;
        }
      }
      return 0;
    }
    
    public void read(ByteBuffer paramByteBuffer, Object[] paramArrayOfObject, int paramInt, boolean paramBoolean)
    {
      for (int i = 0; i < paramInt; i++) {
        paramArrayOfObject[i] = read(paramByteBuffer);
      }
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object[] paramArrayOfObject, int paramInt, boolean paramBoolean)
    {
      for (int i = 0; i < paramInt; i++) {
        write(paramWriteBuffer, paramArrayOfObject[i]);
      }
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      Object[] arrayOfObject = (Object[])paramObject;
      for (int i = 0; i < this.arrayLength; i++)
      {
        DataType localDataType = this.elementTypes[i];
        Object localObject = arrayOfObject[i];
        if (localObject == null)
        {
          paramWriteBuffer.put((byte)0);
        }
        else
        {
          paramWriteBuffer.put((byte)1);
          localDataType.write(paramWriteBuffer, localObject);
        }
      }
    }
    
    public Object read(ByteBuffer paramByteBuffer)
    {
      Object[] arrayOfObject = new Object[this.arrayLength];
      for (int i = 0; i < this.arrayLength; i++)
      {
        DataType localDataType = this.elementTypes[i];
        if (paramByteBuffer.get() == 1) {
          arrayOfObject[i] = localDataType.read(paramByteBuffer);
        }
      }
      return arrayOfObject;
    }
  }
}
