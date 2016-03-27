package org.h2.store;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.message.DbException;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.StreamStore;
import org.h2.mvstore.db.MVTableEngine.Store;
import org.h2.util.IOUtils;
import org.h2.util.New;
import org.h2.value.Value;
import org.h2.value.ValueLobDb;

public class LobStorageMap
  implements LobStorageInterface
{
  private static final boolean TRACE = false;
  private final Database database;
  private boolean init;
  private Object nextLobIdSync = new Object();
  private long nextLobId;
  private MVMap<Long, Object[]> lobMap;
  private MVMap<Object[], Boolean> refMap;
  private MVMap<Long, byte[]> dataMap;
  private StreamStore streamStore;
  
  public LobStorageMap(Database paramDatabase)
  {
    this.database = paramDatabase;
  }
  
  public void init()
  {
    if (this.init) {
      return;
    }
    this.init = true;
    MVTableEngine.Store localStore = this.database.getMvStore();
    MVStore localMVStore;
    if (localStore == null) {
      localMVStore = MVStore.open(null);
    } else {
      localMVStore = localStore.getStore();
    }
    this.lobMap = localMVStore.openMap("lobMap");
    this.refMap = localMVStore.openMap("lobRef");
    this.dataMap = localMVStore.openMap("lobData");
    this.streamStore = new StreamStore(this.dataMap);
  }
  
  public Value createBlob(InputStream paramInputStream, long paramLong)
  {
    init();
    int i = 15;
    if (paramLong < 0L) {
      paramLong = Long.MAX_VALUE;
    }
    int j = (int)Math.min(paramLong, this.database.getMaxLengthInplaceLob());
    try
    {
      if ((j != 0) && (j < Integer.MAX_VALUE))
      {
        BufferedInputStream localBufferedInputStream = new BufferedInputStream(paramInputStream, j);
        localBufferedInputStream.mark(j);
        byte[] arrayOfByte = new byte[j];
        int k = IOUtils.readFully(localBufferedInputStream, arrayOfByte, j);
        if (k < j)
        {
          if (k < arrayOfByte.length) {
            arrayOfByte = Arrays.copyOf(arrayOfByte, k);
          }
          return ValueLobDb.createSmallLob(i, arrayOfByte);
        }
        localBufferedInputStream.reset();
        paramInputStream = localBufferedInputStream;
      }
      return createLob(paramInputStream, i);
    }
    catch (IllegalStateException localIllegalStateException)
    {
      throw DbException.get(90007, localIllegalStateException, new String[0]);
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
  }
  
  public Value createClob(Reader paramReader, long paramLong)
  {
    init();
    int i = 16;
    if (paramLong < 0L) {
      paramLong = Long.MAX_VALUE;
    }
    int j = (int)Math.min(paramLong, this.database.getMaxLengthInplaceLob());
    try
    {
      if ((j != 0) && (j < Integer.MAX_VALUE))
      {
        localObject1 = new BufferedReader(paramReader, j);
        ((BufferedReader)localObject1).mark(j);
        localObject2 = new char[j];
        int k = IOUtils.readFully((Reader)localObject1, (char[])localObject2, j);
        if (k < j)
        {
          if (k < localObject2.length) {
            localObject2 = Arrays.copyOf((char[])localObject2, k);
          }
          byte[] arrayOfByte = new String((char[])localObject2, 0, k).getBytes(Constants.UTF8);
          return ValueLobDb.createSmallLob(i, arrayOfByte);
        }
        ((BufferedReader)localObject1).reset();
        paramReader = (Reader)localObject1;
      }
      Object localObject1 = new CountingReaderInputStream(paramReader, paramLong);
      
      Object localObject2 = createLob((InputStream)localObject1, i);
      
      return ValueLobDb.create(i, this.database, ((ValueLobDb)localObject2).getTableId(), ((ValueLobDb)localObject2).getLobId(), null, ((CountingReaderInputStream)localObject1).getLength());
    }
    catch (IllegalStateException localIllegalStateException)
    {
      throw DbException.get(90007, localIllegalStateException, new String[0]);
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
  }
  
  private ValueLobDb createLob(InputStream paramInputStream, int paramInt)
    throws IOException
  {
    byte[] arrayOfByte;
    try
    {
      arrayOfByte = this.streamStore.put(paramInputStream);
    }
    catch (Exception localException)
    {
      throw DbException.convertToIOException(localException);
    }
    long l1 = generateLobId();
    long l2 = this.streamStore.length(arrayOfByte);
    int i = -2;
    Object[] arrayOfObject1 = { arrayOfByte, Integer.valueOf(i), Long.valueOf(l2), Integer.valueOf(0) };
    this.lobMap.put(Long.valueOf(l1), arrayOfObject1);
    Object[] arrayOfObject2 = { arrayOfByte, Long.valueOf(l1) };
    this.refMap.put(arrayOfObject2, Boolean.TRUE);
    ValueLobDb localValueLobDb = ValueLobDb.create(paramInt, this.database, i, l1, null, l2);
    
    return localValueLobDb;
  }
  
  private long generateLobId()
  {
    synchronized (this.nextLobIdSync)
    {
      if (this.nextLobId == 0L)
      {
        Long localLong = (Long)this.lobMap.lastKey();
        this.nextLobId = (localLong == null ? 1L : localLong.longValue() + 1L);
      }
      return this.nextLobId++;
    }
  }
  
  public boolean isReadOnly()
  {
    return this.database.isReadOnly();
  }
  
  public ValueLobDb copyLob(ValueLobDb paramValueLobDb, int paramInt, long paramLong)
  {
    init();
    int i = paramValueLobDb.getType();
    long l1 = paramValueLobDb.getLobId();
    long l2 = paramValueLobDb.getPrecision();
    if (l2 != paramLong) {
      throw DbException.throwInternalError("Length is different");
    }
    Object[] arrayOfObject1 = (Object[])this.lobMap.get(Long.valueOf(l1));
    arrayOfObject1 = Arrays.copyOf(arrayOfObject1, arrayOfObject1.length);
    byte[] arrayOfByte = (byte[])arrayOfObject1[0];
    long l3 = generateLobId();
    arrayOfObject1[1] = Integer.valueOf(paramInt);
    this.lobMap.put(Long.valueOf(l3), arrayOfObject1);
    Object[] arrayOfObject2 = { arrayOfByte, Long.valueOf(l3) };
    this.refMap.put(arrayOfObject2, Boolean.TRUE);
    ValueLobDb localValueLobDb = ValueLobDb.create(i, this.database, paramInt, l3, null, paramLong);
    
    return localValueLobDb;
  }
  
  public InputStream getInputStream(ValueLobDb paramValueLobDb, byte[] paramArrayOfByte, long paramLong)
    throws IOException
  {
    init();
    Object[] arrayOfObject = (Object[])this.lobMap.get(Long.valueOf(paramValueLobDb.getLobId()));
    if (arrayOfObject == null) {
      throw DbException.throwInternalError("Lob not found: " + paramValueLobDb.getLobId());
    }
    byte[] arrayOfByte = (byte[])arrayOfObject[0];
    return this.streamStore.get(arrayOfByte);
  }
  
  public void setTable(ValueLobDb paramValueLobDb, int paramInt)
  {
    init();
    long l = paramValueLobDb.getLobId();
    Object[] arrayOfObject = (Object[])this.lobMap.remove(Long.valueOf(l));
    
    arrayOfObject[1] = Integer.valueOf(paramInt);
    this.lobMap.put(Long.valueOf(l), arrayOfObject);
  }
  
  public void removeAllForTable(int paramInt)
  {
    init();
    
    ArrayList localArrayList = New.arrayList();
    for (Map.Entry localEntry : this.lobMap.entrySet())
    {
      Object[] arrayOfObject = (Object[])localEntry.getValue();
      int i = ((Integer)arrayOfObject[1]).intValue();
      if (i == paramInt) {
        localArrayList.add(localEntry.getKey());
      }
    }
    for (??? = localArrayList.iterator(); ???.hasNext();)
    {
      long l = ((Long)???.next()).longValue();
      removeLob(paramInt, l);
    }
  }
  
  public void removeLob(ValueLobDb paramValueLobDb)
  {
    init();
    int i = paramValueLobDb.getTableId();
    long l = paramValueLobDb.getLobId();
    removeLob(i, l);
  }
  
  private void removeLob(int paramInt, long paramLong)
  {
    Object[] arrayOfObject1 = (Object[])this.lobMap.remove(Long.valueOf(paramLong));
    if (arrayOfObject1 == null) {
      return;
    }
    byte[] arrayOfByte1 = (byte[])arrayOfObject1[0];
    Object[] arrayOfObject2 = { arrayOfByte1, Long.valueOf(paramLong) };
    this.refMap.remove(arrayOfObject2);
    
    arrayOfObject2 = new Object[] { arrayOfByte1, Integer.valueOf(0) };
    arrayOfObject1 = (Object[])this.refMap.ceilingKey(arrayOfObject2);
    int i = 0;
    if (arrayOfObject1 != null)
    {
      byte[] arrayOfByte2 = (byte[])arrayOfObject1[0];
      if (Arrays.equals(arrayOfByte1, arrayOfByte2)) {
        i = 1;
      }
    }
    if (i == 0) {
      this.streamStore.remove(arrayOfByte1);
    }
  }
  
  private static void trace(String paramString)
  {
    System.out.println(Thread.currentThread().getName() + " LOB " + paramString);
  }
}
