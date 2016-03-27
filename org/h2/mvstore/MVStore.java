package org.h2.mvstore;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.h2.compress.CompressDeflate;
import org.h2.compress.CompressLZF;
import org.h2.compress.Compressor;
import org.h2.mvstore.cache.CacheLongKeyLIRS;
import org.h2.mvstore.type.StringDataType;
import org.h2.util.MathUtils;
import org.h2.util.New;

public class MVStore
{
  public static final boolean ASSERT = false;
  static final int BLOCK_SIZE = 4096;
  private static final int FORMAT_WRITE = 1;
  private static final int FORMAT_READ = 1;
  private static final int MARKED_FREE = 10000000;
  volatile BackgroundWriterThread backgroundWriterThread;
  private volatile boolean reuseSpace = true;
  private boolean closed;
  private FileStore fileStore;
  private boolean fileStoreIsProvided;
  private final int pageSplitSize;
  private CacheLongKeyLIRS<Page> cache;
  private CacheLongKeyLIRS<Page.PageChildren> cacheChunkRef;
  private Chunk lastChunk;
  private final ConcurrentHashMap<Integer, Chunk> chunks = new ConcurrentHashMap();
  private final ConcurrentHashMap<Long, HashMap<Integer, Chunk>> freedPageSpace = new ConcurrentHashMap();
  private MVMap<String, String> meta;
  private final ConcurrentHashMap<Integer, MVMap<?, ?>> maps = new ConcurrentHashMap();
  private HashMap<String, Object> fileHeader = New.hashMap();
  private WriteBuffer writeBuffer;
  private int lastMapId;
  private int versionsToKeep = 5;
  private final int compressionLevel;
  private Compressor compressorFast;
  private Compressor compressorHigh;
  private final Thread.UncaughtExceptionHandler backgroundExceptionHandler;
  private long currentVersion;
  private long lastStoredVersion;
  private int unsavedMemory;
  private int autoCommitMemory;
  private boolean saveNeeded;
  private long creationTime;
  private int retentionTime;
  private long lastCommitTime;
  private Chunk retainChunk;
  private volatile long currentStoreVersion = -1L;
  private Thread currentStoreThread;
  private volatile boolean metaChanged;
  private int autoCommitDelay;
  private int autoCompactFillRate;
  private long autoCompactLastFileOpCount;
  private Object compactSync = new Object();
  private IllegalStateException panicException;
  
  MVStore(HashMap<String, Object> paramHashMap)
  {
    Object localObject1 = paramHashMap.get("compress");
    this.compressionLevel = (localObject1 == null ? 0 : ((Integer)localObject1).intValue());
    String str = (String)paramHashMap.get("fileName");
    localObject1 = paramHashMap.get("pageSplitSize");
    if (localObject1 == null) {
      this.pageSplitSize = (str == null ? 4096 : 16384);
    } else {
      this.pageSplitSize = ((Integer)localObject1).intValue();
    }
    localObject1 = paramHashMap.get("backgroundExceptionHandler");
    this.backgroundExceptionHandler = ((Thread.UncaughtExceptionHandler)localObject1);
    this.meta = new MVMap(StringDataType.INSTANCE, StringDataType.INSTANCE);
    
    HashMap localHashMap = New.hashMap();
    localHashMap.put("id", Integer.valueOf(0));
    localHashMap.put("createVersion", Long.valueOf(this.currentVersion));
    this.meta.init(this, localHashMap);
    this.fileStore = ((FileStore)paramHashMap.get("fileStore"));
    if ((str == null) && (this.fileStore == null))
    {
      this.cache = null;
      this.cacheChunkRef = null;
      return;
    }
    if (this.fileStore == null)
    {
      this.fileStoreIsProvided = false;
      this.fileStore = new FileStore();
    }
    else
    {
      this.fileStoreIsProvided = true;
    }
    this.retentionTime = this.fileStore.getDefaultRetentionTime();
    boolean bool = paramHashMap.containsKey("readOnly");
    localObject1 = paramHashMap.get("cacheSize");
    int i = localObject1 == null ? 16 : ((Integer)localObject1).intValue();
    if (i > 0)
    {
      j = i * 1024 * 1024;
      int k = Math.max(10, this.pageSplitSize / 2);
      int m = 16;
      int i1 = j / k * 2 / 100;
      this.cache = new CacheLongKeyLIRS(j, k, m, i1);
      
      this.cacheChunkRef = new CacheLongKeyLIRS(j / 4, 20, m, i1);
    }
    localObject1 = paramHashMap.get("autoCommitBufferSize");
    int j = localObject1 == null ? 1024 : ((Integer)localObject1).intValue();
    
    this.autoCommitMemory = (j * 1024 * 19);
    
    localObject1 = paramHashMap.get("autoCompactFillRate");
    this.autoCompactFillRate = (localObject1 == null ? 50 : ((Integer)localObject1).intValue());
    
    char[] arrayOfChar = (char[])paramHashMap.get("encryptionKey");
    try
    {
      if (!this.fileStoreIsProvided) {
        this.fileStore.open(str, bool, arrayOfChar);
      }
      if (this.fileStore.size() == 0L)
      {
        this.creationTime = getTime();
        this.lastCommitTime = this.creationTime;
        this.fileHeader.put("H", Integer.valueOf(2));
        this.fileHeader.put("blockSize", Integer.valueOf(4096));
        this.fileHeader.put("format", Integer.valueOf(1));
        this.fileHeader.put("created", Long.valueOf(this.creationTime));
        writeFileHeader();
      }
      else
      {
        readFileHeader();
      }
    }
    catch (IllegalStateException localIllegalStateException)
    {
      panic(localIllegalStateException);
    }
    finally
    {
      if (arrayOfChar != null) {
        Arrays.fill(arrayOfChar, '\000');
      }
    }
    this.lastCommitTime = getTime();
    
    localObject1 = paramHashMap.get("autoCommitDelay");
    int n = localObject1 == null ? 1000 : ((Integer)localObject1).intValue();
    setAutoCommitDelay(n);
  }
  
  private void panic(IllegalStateException paramIllegalStateException)
  {
    if (this.backgroundExceptionHandler != null) {
      this.backgroundExceptionHandler.uncaughtException(null, paramIllegalStateException);
    }
    this.panicException = paramIllegalStateException;
    closeImmediately();
    throw paramIllegalStateException;
  }
  
  public static MVStore open(String paramString)
  {
    HashMap localHashMap = New.hashMap();
    localHashMap.put("fileName", paramString);
    return new MVStore(localHashMap);
  }
  
  <T extends MVMap<?, ?>> T openMapVersion(long paramLong, int paramInt, MVMap<?, ?> paramMVMap)
  {
    MVMap localMVMap1 = getMetaMap(paramLong);
    long l = getRootPos(localMVMap1, paramInt);
    MVMap localMVMap2 = paramMVMap.openReadOnly();
    localMVMap2.setRootPos(l, paramLong);
    return localMVMap2;
  }
  
  public <K, V> MVMap<K, V> openMap(String paramString)
  {
    return openMap(paramString, new MVMap.Builder());
  }
  
  public synchronized <M extends MVMap<K, V>, K, V> M openMap(String paramString, MVMap.MapBuilder<M, K, V> paramMapBuilder)
  {
    checkOpen();
    String str1 = (String)this.meta.get("name." + paramString);
    int i;
    MVMap localMVMap1;
    HashMap localHashMap;
    long l;
    if (str1 != null)
    {
      i = DataUtils.parseHexInt(str1);
      
      MVMap localMVMap2 = (MVMap)this.maps.get(Integer.valueOf(i));
      if (localMVMap2 != null) {
        return localMVMap2;
      }
      localMVMap1 = paramMapBuilder.create();
      String str2 = (String)this.meta.get(MVMap.getMapKey(i));
      localHashMap = New.hashMap();
      localHashMap.putAll(DataUtils.parseMap(str2));
      localHashMap.put("id", Integer.valueOf(i));
      localMVMap1.init(this, localHashMap);
      l = getRootPos(this.meta, i);
    }
    else
    {
      localHashMap = New.hashMap();
      i = ++this.lastMapId;
      localHashMap.put("id", Integer.valueOf(i));
      localHashMap.put("createVersion", Long.valueOf(this.currentVersion));
      localMVMap1 = paramMapBuilder.create();
      localMVMap1.init(this, localHashMap);
      markMetaChanged();
      str1 = Integer.toHexString(i);
      this.meta.put(MVMap.getMapKey(i), localMVMap1.asString(paramString));
      this.meta.put("name." + paramString, str1);
      l = 0L;
    }
    localMVMap1.setRootPos(l, -1L);
    this.maps.put(Integer.valueOf(i), localMVMap1);
    return localMVMap1;
  }
  
  public synchronized Set<String> getMapNames()
  {
    HashSet localHashSet = New.hashSet();
    checkOpen();
    for (Iterator localIterator = this.meta.keyIterator("name."); localIterator.hasNext();)
    {
      String str = (String)localIterator.next();
      if (!str.startsWith("name.")) {
        break;
      }
      localHashSet.add(str.substring("name.".length()));
    }
    return localHashSet;
  }
  
  public MVMap<String, String> getMetaMap()
  {
    checkOpen();
    return this.meta;
  }
  
  private MVMap<String, String> getMetaMap(long paramLong)
  {
    Chunk localChunk = getChunkForVersion(paramLong);
    DataUtils.checkArgument(localChunk != null, "Unknown version {0}", new Object[] { Long.valueOf(paramLong) });
    localChunk = readChunkHeader(localChunk.block);
    MVMap localMVMap = this.meta.openReadOnly();
    localMVMap.setRootPos(localChunk.metaRootPos, paramLong);
    return localMVMap;
  }
  
  private Chunk getChunkForVersion(long paramLong)
  {
    Chunk localChunk = this.lastChunk;
    for (;;)
    {
      if ((localChunk == null) || (localChunk.version <= paramLong)) {
        return localChunk;
      }
      localChunk = (Chunk)this.chunks.get(Integer.valueOf(localChunk.id - 1));
    }
  }
  
  public boolean hasMap(String paramString)
  {
    return this.meta.containsKey("name." + paramString);
  }
  
  private void markMetaChanged()
  {
    this.metaChanged = true;
  }
  
  private synchronized void readFileHeader()
  {
    int i = 0;
    
    long l1 = -1L;
    long l2 = -1L;
    
    ByteBuffer localByteBuffer = this.fileStore.readFully(0L, 8192);
    byte[] arrayOfByte = new byte['က'];
    for (int j = 0; j <= 4096; j += 4096)
    {
      localByteBuffer.get(arrayOfByte);
      try
      {
        String str = new String(arrayOfByte, 0, 4096, DataUtils.LATIN).trim();
        
        HashMap localHashMap = DataUtils.parseMap(str);
        int k = DataUtils.readHexInt(localHashMap, "blockSize", 4096);
        if (k != 4096) {
          throw DataUtils.newIllegalStateException(5, "Block size {0} is currently not supported", new Object[] { Integer.valueOf(k) });
        }
        m = DataUtils.readHexInt(localHashMap, "fletcher", 0);
        localHashMap.remove("fletcher");
        str = str.substring(0, str.lastIndexOf("fletcher") - 1);
        localObject1 = str.getBytes(DataUtils.LATIN);
        int n = DataUtils.getFletcher32((byte[])localObject1, localObject1.length);
        if (m == n)
        {
          long l5 = DataUtils.readHexLong(localHashMap, "version", 0L);
          if (l5 > l1)
          {
            l1 = l5;
            this.fileHeader.putAll(localHashMap);
            l2 = DataUtils.readHexLong(localHashMap, "block", 0L);
            this.creationTime = DataUtils.readHexLong(localHashMap, "created", 0L);
            i = 1;
          }
        }
      }
      catch (Exception localException1) {}
    }
    if (i == 0) {
      throw DataUtils.newIllegalStateException(6, "Store header is corrupt: {0}", new Object[] { this.fileStore });
    }
    long l3 = DataUtils.readHexLong(this.fileHeader, "format", 1L);
    if ((l3 > 1L) && (!this.fileStore.isReadOnly())) {
      throw DataUtils.newIllegalStateException(5, "The write format {0} is larger than the supported format {1}, and the file was not opened in read-only mode", new Object[] { Long.valueOf(l3), Integer.valueOf(1) });
    }
    l3 = DataUtils.readHexLong(this.fileHeader, "formatRead", l3);
    if (l3 > 1L) {
      throw DataUtils.newIllegalStateException(5, "The read format {0} is larger than the supported format {1}", new Object[] { Long.valueOf(l3), Integer.valueOf(1) });
    }
    this.lastStoredVersion = -1L;
    this.chunks.clear();
    long l4 = System.currentTimeMillis();
    
    int m = 1970 + (int)(l4 / 31557600000L);
    if (m < 2014)
    {
      this.creationTime = (l4 - this.fileStore.getDefaultRetentionTime());
    }
    else if (l4 < this.creationTime)
    {
      this.creationTime = l4;
      this.fileHeader.put("created", Long.valueOf(this.creationTime));
    }
    Object localObject1 = readChunkFooter(this.fileStore.size());
    if ((localObject1 != null) && 
      (((Chunk)localObject1).version > l1))
    {
      l1 = ((Chunk)localObject1).version;
      l2 = ((Chunk)localObject1).block;
    }
    if (l2 <= 0L) {
      return;
    }
    this.lastChunk = null;
    for (;;)
    {
      try
      {
        localObject2 = readChunkHeader(l2);
      }
      catch (Exception localException2)
      {
        break;
      }
      if (((Chunk)localObject2).version < l1) {
        break;
      }
      localObject1 = readChunkFooter((l2 + ((Chunk)localObject2).len) * 4096L);
      if ((localObject1 == null) || (((Chunk)localObject1).id != ((Chunk)localObject2).id)) {
        break;
      }
      this.lastChunk = ((Chunk)localObject2);
      l1 = ((Chunk)localObject2).version;
      if ((((Chunk)localObject2).next == 0L) || (((Chunk)localObject2).next >= this.fileStore.size() / 4096L)) {
        break;
      }
      l2 = ((Chunk)localObject2).next;
    }
    if (this.lastChunk == null) {
      return;
    }
    this.lastMapId = this.lastChunk.mapId;
    this.currentVersion = this.lastChunk.version;
    setWriteVersion(this.currentVersion);
    this.chunks.put(Integer.valueOf(this.lastChunk.id), this.lastChunk);
    this.meta.setRootPos(this.lastChunk.metaRootPos, -1L);
    for (Object localObject2 = this.meta.keyIterator("chunk."); ((Iterator)localObject2).hasNext();)
    {
      localObject3 = (String)((Iterator)localObject2).next();
      if (!((String)localObject3).startsWith("chunk.")) {
        break;
      }
      localObject3 = (String)this.meta.get(localObject3);
      Chunk localChunk = Chunk.fromString((String)localObject3);
      if (!this.chunks.containsKey(Integer.valueOf(localChunk.id)))
      {
        if (localChunk.block == Long.MAX_VALUE) {
          throw DataUtils.newIllegalStateException(6, "Chunk {0} is invalid", new Object[] { Integer.valueOf(localChunk.id) });
        }
        this.chunks.put(Integer.valueOf(localChunk.id), localChunk);
      }
    }
    Object localObject3;
    for (localObject2 = this.chunks.values().iterator(); ((Iterator)localObject2).hasNext();)
    {
      localObject3 = (Chunk)((Iterator)localObject2).next();
      if (((Chunk)localObject3).pageCountLive == 0) {
        registerFreePage(this.currentVersion, ((Chunk)localObject3).id, 0L, 0);
      }
      long l6 = ((Chunk)localObject3).block * 4096L;
      int i1 = ((Chunk)localObject3).len * 4096;
      this.fileStore.markUsed(l6, i1);
    }
  }
  
  private Chunk readChunkFooter(long paramLong)
  {
    try
    {
      ByteBuffer localByteBuffer = this.fileStore.readFully(paramLong - 128L, 128);
      
      byte[] arrayOfByte1 = new byte[''];
      localByteBuffer.get(arrayOfByte1);
      String str = new String(arrayOfByte1, DataUtils.LATIN).trim();
      HashMap localHashMap = DataUtils.parseMap(str);
      int i = DataUtils.readHexInt(localHashMap, "fletcher", 0);
      localHashMap.remove("fletcher");
      str = str.substring(0, str.lastIndexOf("fletcher") - 1);
      byte[] arrayOfByte2 = str.getBytes(DataUtils.LATIN);
      int j = DataUtils.getFletcher32(arrayOfByte2, arrayOfByte2.length);
      if (i == j)
      {
        int k = DataUtils.readHexInt(localHashMap, "chunk", 0);
        Chunk localChunk = new Chunk(k);
        localChunk.version = DataUtils.readHexLong(localHashMap, "version", 0L);
        localChunk.block = DataUtils.readHexLong(localHashMap, "block", 0L);
        return localChunk;
      }
    }
    catch (Exception localException) {}
    return null;
  }
  
  private void writeFileHeader()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    if (this.lastChunk != null)
    {
      this.fileHeader.put("block", Long.valueOf(this.lastChunk.block));
      this.fileHeader.put("chunk", Integer.valueOf(this.lastChunk.id));
      this.fileHeader.put("version", Long.valueOf(this.lastChunk.version));
    }
    DataUtils.appendMap(localStringBuilder, this.fileHeader);
    byte[] arrayOfByte = localStringBuilder.toString().getBytes(DataUtils.LATIN);
    int i = DataUtils.getFletcher32(arrayOfByte, arrayOfByte.length);
    DataUtils.appendMap(localStringBuilder, "fletcher", Integer.valueOf(i));
    localStringBuilder.append("\n");
    arrayOfByte = localStringBuilder.toString().getBytes(DataUtils.LATIN);
    ByteBuffer localByteBuffer = ByteBuffer.allocate(8192);
    localByteBuffer.put(arrayOfByte);
    localByteBuffer.position(4096);
    localByteBuffer.put(arrayOfByte);
    localByteBuffer.rewind();
    write(0L, localByteBuffer);
  }
  
  private void write(long paramLong, ByteBuffer paramByteBuffer)
  {
    try
    {
      this.fileStore.writeFully(paramLong, paramByteBuffer);
    }
    catch (IllegalStateException localIllegalStateException)
    {
      panic(localIllegalStateException);
      throw localIllegalStateException;
    }
  }
  
  public void close()
  {
    if (this.closed) {
      return;
    }
    if ((this.fileStore != null) && (!this.fileStore.isReadOnly()))
    {
      stopBackgroundThread();
      if (hasUnsavedChanges()) {
        commitAndSave();
      }
    }
    closeStore(true);
  }
  
  public void closeImmediately()
  {
    try
    {
      closeStore(false);
    }
    catch (Exception localException)
    {
      if (this.backgroundExceptionHandler != null) {
        this.backgroundExceptionHandler.uncaughtException(null, localException);
      }
    }
  }
  
  private void closeStore(boolean paramBoolean)
  {
    if (this.closed) {
      return;
    }
    stopBackgroundThread();
    this.closed = true;
    if (this.fileStore == null) {
      return;
    }
    synchronized (this)
    {
      if (paramBoolean) {
        shrinkFileIfPossible(0);
      }
      this.cache = null;
      this.cacheChunkRef = null;
      for (MVMap localMVMap : New.arrayList(this.maps.values())) {
        localMVMap.close();
      }
      this.meta = null;
      this.chunks.clear();
      this.maps.clear();
      try
      {
        if (!this.fileStoreIsProvided) {
          this.fileStore.close();
        }
      }
      finally
      {
        this.fileStore = null;
      }
    }
  }
  
  boolean isChunkLive(int paramInt)
  {
    String str = (String)this.meta.get(Chunk.getMetaKey(paramInt));
    return str != null;
  }
  
  private Chunk getChunk(long paramLong)
  {
    Chunk localChunk = getChunkIfFound(paramLong);
    if (localChunk == null)
    {
      int i = DataUtils.getPageChunkId(paramLong);
      throw DataUtils.newIllegalStateException(6, "Chunk {0} not found", new Object[] { Integer.valueOf(i) });
    }
    return localChunk;
  }
  
  private Chunk getChunkIfFound(long paramLong)
  {
    int i = DataUtils.getPageChunkId(paramLong);
    Chunk localChunk = (Chunk)this.chunks.get(Integer.valueOf(i));
    if (localChunk == null)
    {
      if (!Thread.holdsLock(this)) {
        throw DataUtils.newIllegalStateException(9, "Chunk {0} no longer exists", new Object[] { Integer.valueOf(i) });
      }
      String str = (String)this.meta.get(Chunk.getMetaKey(i));
      if (str == null) {
        return null;
      }
      localChunk = Chunk.fromString(str);
      if (localChunk.block == Long.MAX_VALUE) {
        throw DataUtils.newIllegalStateException(6, "Chunk {0} is invalid", new Object[] { Integer.valueOf(i) });
      }
      this.chunks.put(Integer.valueOf(localChunk.id), localChunk);
    }
    return localChunk;
  }
  
  private void setWriteVersion(long paramLong)
  {
    for (MVMap localMVMap : this.maps.values()) {
      localMVMap.setWriteVersion(paramLong);
    }
    this.meta.setWriteVersion(paramLong);
  }
  
  public long commit()
  {
    if (this.fileStore != null) {
      return commitAndSave();
    }
    long l = ++this.currentVersion;
    setWriteVersion(l);
    return l;
  }
  
  private synchronized long commitAndSave()
  {
    if (this.closed) {
      return this.currentVersion;
    }
    if (this.fileStore == null) {
      throw DataUtils.newIllegalStateException(2, "This is an in-memory store", new Object[0]);
    }
    if (this.currentStoreVersion >= 0L) {
      return this.currentVersion;
    }
    if (!hasUnsavedChanges()) {
      return this.currentVersion;
    }
    if (this.fileStore.isReadOnly()) {
      throw DataUtils.newIllegalStateException(2, "This store is read-only", new Object[0]);
    }
    try
    {
      this.currentStoreVersion = this.currentVersion;
      this.currentStoreThread = Thread.currentThread();
      return storeNow();
    }
    finally
    {
      this.currentStoreVersion = -1L;
      this.currentStoreThread = null;
    }
  }
  
  private long storeNow()
  {
    freeUnusedChunks();
    int i = this.unsavedMemory;
    long l1 = this.currentStoreVersion;
    long l2 = ++this.currentVersion;
    setWriteVersion(l2);
    long l3 = getTime();
    this.lastCommitTime = l3;
    this.retainChunk = null;
    int j;
    if (this.lastChunk == null)
    {
      j = 0;
    }
    else
    {
      j = this.lastChunk.id;
      this.meta.put(Chunk.getMetaKey(j), this.lastChunk.asString());
      
      l3 = Math.max(this.lastChunk.time, l3);
    }
    int k = j;
    do
    {
      k = (k + 1) % 67108863;
    } while (this.chunks.containsKey(Integer.valueOf(k)));
    Chunk localChunk1 = new Chunk(k);
    
    localChunk1.pageCount = Integer.MAX_VALUE;
    localChunk1.pageCountLive = Integer.MAX_VALUE;
    localChunk1.maxLen = Long.MAX_VALUE;
    localChunk1.maxLenLive = Long.MAX_VALUE;
    localChunk1.metaRootPos = Long.MAX_VALUE;
    localChunk1.block = Long.MAX_VALUE;
    localChunk1.len = Integer.MAX_VALUE;
    localChunk1.time = l3;
    localChunk1.version = l2;
    localChunk1.mapId = this.lastMapId;
    localChunk1.next = Long.MAX_VALUE;
    this.chunks.put(Integer.valueOf(localChunk1.id), localChunk1);
    
    this.meta.put(Chunk.getMetaKey(localChunk1.id), localChunk1.asString());
    this.meta.remove(Chunk.getMetaKey(localChunk1.id));
    ArrayList localArrayList1 = New.arrayList(this.maps.values());
    ArrayList localArrayList2 = New.arrayList();
    for (Object localObject1 = localArrayList1.iterator(); ((Iterator)localObject1).hasNext();)
    {
      MVMap localMVMap1 = (MVMap)((Iterator)localObject1).next();
      localMVMap1.setWriteVersion(l2);
      long l4 = localMVMap1.getVersion();
      if ((localMVMap1.getCreateVersion() <= l1) && 
      
        (!localMVMap1.isVolatile())) {
        if ((l4 >= 0L) && (l4 >= this.lastStoredVersion))
        {
          localObject3 = localMVMap1.openVersion(l1);
          if (((MVMap)localObject3).getRoot().getPos() == 0L) {
            localArrayList2.add(localObject3);
          }
        }
      }
    }
    Object localObject3;
    applyFreedSpace(l1);
    localObject1 = getWriteBuffer();
    
    localChunk1.writeChunkHeader((WriteBuffer)localObject1, 0);
    int m = ((WriteBuffer)localObject1).position();
    localChunk1.pageCount = 0;
    localChunk1.pageCountLive = 0;
    localChunk1.maxLen = 0L;
    localChunk1.maxLenLive = 0L;
    for (Object localObject2 = localArrayList2.iterator(); ((Iterator)localObject2).hasNext();)
    {
      MVMap localMVMap2 = (MVMap)((Iterator)localObject2).next();
      localObject3 = localMVMap2.getRoot();
      String str = MVMap.getMapRootKey(localMVMap2.getId());
      if (((Page)localObject3).getTotalCount() == 0L)
      {
        this.meta.put(str, "0");
      }
      else
      {
        ((Page)localObject3).writeUnsavedRecursive(localChunk1, (WriteBuffer)localObject1);
        long l6 = ((Page)localObject3).getPos();
        this.meta.put(str, Long.toHexString(l6));
      }
    }
    this.meta.setWriteVersion(l2);
    
    localObject2 = this.meta.getRoot();
    ((Page)localObject2).writeUnsavedRecursive(localChunk1, (WriteBuffer)localObject1);
    
    int n = ((WriteBuffer)localObject1).position();
    
    int i1 = MathUtils.roundUpInt(n + 128, 4096);
    
    ((WriteBuffer)localObject1).limit(i1);
    
    long l5 = getFileLengthInUse();
    long l7;
    if (this.reuseSpace) {
      l7 = this.fileStore.allocate(i1);
    } else {
      l7 = l5;
    }
    int i2 = l7 + i1 >= this.fileStore.size() ? 1 : 0;
    if (!this.reuseSpace) {
      this.fileStore.markUsed(l5, i1);
    }
    localChunk1.block = (l7 / 4096L);
    localChunk1.len = (i1 / 4096);
    localChunk1.metaRootPos = ((Page)localObject2).getPos();
    long l8;
    if (this.reuseSpace)
    {
      i3 = localChunk1.len;
      l8 = this.fileStore.allocate(i3 * 4096);
      
      this.fileStore.free(l8, i3 * 4096);
      localChunk1.next = (l8 / 4096L);
    }
    else
    {
      localChunk1.next = 0L;
    }
    ((WriteBuffer)localObject1).position(0);
    localChunk1.writeChunkHeader((WriteBuffer)localObject1, m);
    revertTemp(l1);
    
    ((WriteBuffer)localObject1).position(((WriteBuffer)localObject1).limit() - 128);
    ((WriteBuffer)localObject1).put(localChunk1.getFooterBytes());
    
    ((WriteBuffer)localObject1).position(0);
    write(l7, ((WriteBuffer)localObject1).getBuffer());
    releaseWriteBuffer((WriteBuffer)localObject1);
    
    int i3 = 0;
    if (i2 == 0) {
      if (this.lastChunk == null)
      {
        i3 = 1;
      }
      else if (this.lastChunk.next != localChunk1.block)
      {
        i3 = 1;
      }
      else
      {
        l8 = DataUtils.readHexLong(this.fileHeader, "version", 0L);
        if (this.lastChunk.version - l8 > 20L)
        {
          i3 = 1;
        }
        else
        {
          int i4 = DataUtils.readHexInt(this.fileHeader, "chunk", 0);
          for (;;)
          {
            Chunk localChunk2 = (Chunk)this.chunks.get(Integer.valueOf(i4));
            if (localChunk2 == null)
            {
              i3 = 1;
              break;
            }
            if (i4 == this.lastChunk.id) {
              break;
            }
            i4++;
          }
        }
      }
    }
    this.lastChunk = localChunk1;
    if (i3 != 0) {
      writeFileHeader();
    }
    if (i2 == 0) {
      shrinkFileIfPossible(1);
    }
    try
    {
      for (MVMap localMVMap3 : localArrayList2)
      {
        Page localPage = localMVMap3.getRoot();
        if (localPage.getTotalCount() > 0L) {
          localPage.writeEnd();
        }
      }
      ((Page)localObject2).writeEnd();
    }
    catch (IllegalStateException localIllegalStateException)
    {
      panic(localIllegalStateException);
    }
    this.unsavedMemory = Math.max(0, this.unsavedMemory - i);
    
    this.metaChanged = false;
    this.lastStoredVersion = l1;
    
    return l2;
  }
  
  private void freeUnusedChunks()
  {
    if (this.lastChunk == null) {
      return;
    }
    Set localSet = collectReferencedChunks();
    ArrayList localArrayList = New.arrayList();
    long l1 = getTime();
    for (Iterator localIterator = this.chunks.values().iterator(); localIterator.hasNext();)
    {
      localChunk = (Chunk)localIterator.next();
      if (!localSet.contains(Integer.valueOf(localChunk.id))) {
        localArrayList.add(localChunk);
      }
    }
    Chunk localChunk;
    for (localIterator = localArrayList.iterator(); localIterator.hasNext();)
    {
      localChunk = (Chunk)localIterator.next();
      if (canOverwriteChunk(localChunk, l1))
      {
        this.chunks.remove(Integer.valueOf(localChunk.id));
        markMetaChanged();
        this.meta.remove(Chunk.getMetaKey(localChunk.id));
        long l2 = localChunk.block * 4096L;
        int i = localChunk.len * 4096;
        this.fileStore.free(l2, i);
      }
      else if (localChunk.unused == 0L)
      {
        localChunk.unused = l1;
        this.meta.put(Chunk.getMetaKey(localChunk.id), localChunk.asString());
        markMetaChanged();
      }
    }
  }
  
  private Set<Integer> collectReferencedChunks()
  {
    long l1 = this.lastChunk.version;
    DataUtils.checkArgument(l1 > 0L, "Collect references on version 0", new Object[0]);
    long l2 = getFileStore().readCount;
    HashSet localHashSet = New.hashSet();
    for (Cursor localCursor = this.meta.cursor("root."); localCursor.hasNext();)
    {
      String str = (String)localCursor.next();
      if (!str.startsWith("root.")) {
        break;
      }
      long l4 = DataUtils.parseHexLong((String)localCursor.getValue());
      if (l4 != 0L)
      {
        int i = DataUtils.parseHexInt(str.substring("root.".length()));
        collectReferencedChunks(localHashSet, i, l4);
      }
    }
    long l3 = this.lastChunk.metaRootPos;
    collectReferencedChunks(localHashSet, 0, l3);
    l2 = this.fileStore.readCount - l2;
    return localHashSet;
  }
  
  private int collectReferencedChunks(Set<Integer> paramSet, int paramInt, long paramLong)
  {
    paramSet.add(Integer.valueOf(DataUtils.getPageChunkId(paramLong)));
    if (DataUtils.getPageType(paramLong) == 0) {
      return 1;
    }
    Page.PageChildren localPageChildren = readPageChunkReferences(paramInt, paramLong, -1);
    int i = 0;
    if (localPageChildren != null) {
      for (long l : localPageChildren.children) {
        i += collectReferencedChunks(paramSet, paramInt, l);
      }
    }
    return i;
  }
  
  private Page.PageChildren readPageChunkReferences(int paramInt1, long paramLong, int paramInt2)
  {
    if (DataUtils.getPageType(paramLong) == 0) {
      return null;
    }
    Page.PageChildren localPageChildren = (Page.PageChildren)this.cacheChunkRef.get(paramLong);
    if (localPageChildren == null)
    {
      Page localPage = (Page)this.cache.get(paramLong);
      if (localPage == null)
      {
        Chunk localChunk = getChunk(paramLong);
        long l1 = localChunk.block * 4096L;
        l1 += DataUtils.getPageOffset(paramLong);
        if (l1 < 0L) {
          throw DataUtils.newIllegalStateException(6, "Negative position {0}", new Object[] { Long.valueOf(l1) });
        }
        long l2 = (localChunk.block + localChunk.len) * 4096L;
        localPageChildren = Page.PageChildren.read(this.fileStore, paramLong, paramInt1, l1, l2);
      }
      else
      {
        localPageChildren = new Page.PageChildren(localPage);
      }
      localPageChildren.removeDuplicateChunkReferences();
      this.cacheChunkRef.put(paramLong, localPageChildren);
    }
    if (localPageChildren.children.length == 0)
    {
      int i = DataUtils.getPageChunkId(paramLong);
      if (i == paramInt2) {
        return null;
      }
    }
    return localPageChildren;
  }
  
  private WriteBuffer getWriteBuffer()
  {
    WriteBuffer localWriteBuffer;
    if (this.writeBuffer != null)
    {
      localWriteBuffer = this.writeBuffer;
      localWriteBuffer.clear();
    }
    else
    {
      localWriteBuffer = new WriteBuffer();
    }
    return localWriteBuffer;
  }
  
  private void releaseWriteBuffer(WriteBuffer paramWriteBuffer)
  {
    if (paramWriteBuffer.capacity() <= 4194304) {
      this.writeBuffer = paramWriteBuffer;
    }
  }
  
  private boolean canOverwriteChunk(Chunk paramChunk, long paramLong)
  {
    if (paramChunk.time + this.retentionTime > paramLong) {
      return false;
    }
    if ((paramChunk.unused == 0L) || (paramChunk.unused + this.retentionTime / 2 > paramLong)) {
      return false;
    }
    Chunk localChunk = this.retainChunk;
    if ((localChunk != null) && (paramChunk.version > localChunk.version)) {
      return false;
    }
    return true;
  }
  
  private long getTime()
  {
    return System.currentTimeMillis() - this.creationTime;
  }
  
  private Set<Chunk> applyFreedSpace(long paramLong)
  {
    HashSet localHashSet = New.hashSet();
    for (;;)
    {
      ArrayList localArrayList = New.arrayList();
      
      Iterator localIterator1 = this.freedPageSpace.entrySet().iterator();
      while (localIterator1.hasNext())
      {
        localObject = (Map.Entry)localIterator1.next();
        long l = ((Long)((Map.Entry)localObject).getKey()).longValue();
        if (l <= paramLong)
        {
          HashMap localHashMap = (HashMap)((Map.Entry)localObject).getValue();
          for (Chunk localChunk2 : localHashMap.values())
          {
            Chunk localChunk3 = (Chunk)this.chunks.get(Integer.valueOf(localChunk2.id));
            if (localChunk3 != null)
            {
              localChunk3.maxLenLive += localChunk2.maxLenLive;
              localChunk3.pageCountLive += localChunk2.pageCountLive;
              if ((localChunk3.pageCountLive < 0) && (localChunk3.pageCountLive > -10000000)) {
                throw DataUtils.newIllegalStateException(3, "Corrupt page count {0}", new Object[] { Integer.valueOf(localChunk3.pageCountLive) });
              }
              if ((localChunk3.maxLenLive < 0L) && (localChunk3.maxLenLive > -10000000L)) {
                throw DataUtils.newIllegalStateException(3, "Corrupt max length {0}", new Object[] { Long.valueOf(localChunk3.maxLenLive) });
              }
              if (((localChunk3.pageCountLive <= 0) && (localChunk3.maxLenLive > 0L)) || ((localChunk3.maxLenLive <= 0L) && (localChunk3.pageCountLive > 0))) {
                throw DataUtils.newIllegalStateException(3, "Corrupt max length {0}", new Object[] { Long.valueOf(localChunk3.maxLenLive) });
              }
              localArrayList.add(localChunk3);
            }
          }
          localIterator1.remove();
        }
      }
      for (Object localObject = localArrayList.iterator(); ((Iterator)localObject).hasNext();)
      {
        Chunk localChunk1 = (Chunk)((Iterator)localObject).next();
        this.meta.put(Chunk.getMetaKey(localChunk1.id), localChunk1.asString());
      }
      if (localArrayList.size() == 0) {
        break;
      }
    }
    return localHashSet;
  }
  
  private void shrinkFileIfPossible(int paramInt)
  {
    long l1 = getFileLengthInUse();
    long l2 = this.fileStore.size();
    if (l1 >= l2) {
      return;
    }
    if ((paramInt > 0) && (l2 - l1 < 4096L)) {
      return;
    }
    int i = (int)(100L - l1 * 100L / l2);
    if (i < paramInt) {
      return;
    }
    this.fileStore.truncate(l1);
  }
  
  private long getFileLengthInUse()
  {
    long l1 = 8192L;
    for (Chunk localChunk : this.chunks.values()) {
      if (localChunk.len != Integer.MAX_VALUE)
      {
        long l2 = (localChunk.block + localChunk.len) * 4096L;
        l1 = Math.max(l1, l2);
      }
    }
    return l1;
  }
  
  public boolean hasUnsavedChanges()
  {
    checkOpen();
    if (this.metaChanged) {
      return true;
    }
    for (MVMap localMVMap : this.maps.values()) {
      if (!localMVMap.isClosed())
      {
        long l = localMVMap.getVersion();
        if ((l >= 0L) && (l > this.lastStoredVersion)) {
          return true;
        }
      }
    }
    return false;
  }
  
  private Chunk readChunkHeader(long paramLong)
  {
    long l = paramLong * 4096L;
    ByteBuffer localByteBuffer = this.fileStore.readFully(l, 1024);
    return Chunk.readChunkHeader(localByteBuffer, l);
  }
  
  public synchronized boolean compactRewriteFully()
  {
    checkOpen();
    if (this.lastChunk == null) {
      return false;
    }
    for (MVMap localMVMap1 : this.maps.values())
    {
      MVMap localMVMap2 = localMVMap1;
      Cursor localCursor = localMVMap2.cursor(null);
      Object localObject1 = null;
      while (localCursor.hasNext())
      {
        localCursor.next();
        Page localPage = localCursor.getPage();
        if (localPage != localObject1)
        {
          Object localObject2 = localPage.getKey(0);
          Object localObject3 = localPage.getValue(0);
          localMVMap2.put(localObject2, localObject3);
          localObject1 = localPage;
        }
      }
    }
    commitAndSave();
    return true;
  }
  
  public synchronized boolean compactMoveChunks()
  {
    return compactMoveChunks(100, Long.MAX_VALUE);
  }
  
  public synchronized boolean compactMoveChunks(int paramInt, long paramLong)
  {
    checkOpen();
    if (this.lastChunk == null) {
      return false;
    }
    int i = this.retentionTime;
    boolean bool1 = this.reuseSpace;
    try
    {
      this.retentionTime = 0;
      freeUnusedChunks();
      if (this.fileStore.getFillRate() > paramInt) {
        return false;
      }
      long l = this.fileStore.getFirstFree() / 4096L;
      ArrayList localArrayList = compactGetMoveBlocks(l, paramLong);
      compactMoveChunks(localArrayList);
      freeUnusedChunks();
      storeNow();
    }
    finally
    {
      this.reuseSpace = bool1;
      this.retentionTime = i;
    }
    return true;
  }
  
  private ArrayList<Chunk> compactGetMoveBlocks(long paramLong1, long paramLong2)
  {
    ArrayList localArrayList = New.arrayList();
    for (Chunk localChunk1 : this.chunks.values()) {
      if (localChunk1.block > paramLong1) {
        localArrayList.add(localChunk1);
      }
    }
    Collections.sort(localArrayList, new Comparator()
    {
      public int compare(Chunk paramAnonymousChunk1, Chunk paramAnonymousChunk2)
      {
        return Long.signum(paramAnonymousChunk1.block - paramAnonymousChunk2.block);
      }
    });
    int i = 0;
    long l1 = 0L;
    for (Chunk localChunk2 : localArrayList)
    {
      long l2 = localChunk2.len * 4096L;
      if (l1 + l2 > paramLong2) {
        break;
      }
      l1 += l2;
      i++;
    }
    while ((localArrayList.size() > i) && (localArrayList.size() > 1)) {
      localArrayList.remove(1);
    }
    return localArrayList;
  }
  
  private void compactMoveChunks(ArrayList<Chunk> paramArrayList)
  {
    for (Iterator localIterator = paramArrayList.iterator(); localIterator.hasNext();)
    {
      localChunk = (Chunk)localIterator.next();
      localWriteBuffer = getWriteBuffer();
      l1 = localChunk.block * 4096L;
      i = localChunk.len * 4096;
      localWriteBuffer.limit(i);
      localByteBuffer = this.fileStore.readFully(l1, i);
      Chunk.readChunkHeader(localByteBuffer, l1);
      j = localByteBuffer.position();
      localWriteBuffer.position(j);
      localWriteBuffer.put(localByteBuffer);
      l2 = getFileLengthInUse();
      this.fileStore.markUsed(l2, i);
      this.fileStore.free(l1, i);
      localChunk.block = (l2 / 4096L);
      localChunk.next = 0L;
      localWriteBuffer.position(0);
      localChunk.writeChunkHeader(localWriteBuffer, j);
      localWriteBuffer.position(i - 128);
      localWriteBuffer.put(this.lastChunk.getFooterBytes());
      localWriteBuffer.position(0);
      write(l2, localWriteBuffer.getBuffer());
      releaseWriteBuffer(localWriteBuffer);
      markMetaChanged();
      this.meta.put(Chunk.getMetaKey(localChunk.id), localChunk.asString());
    }
    Chunk localChunk;
    WriteBuffer localWriteBuffer;
    long l1;
    int i;
    ByteBuffer localByteBuffer;
    int j;
    long l2;
    this.reuseSpace = false;
    commitAndSave();
    
    sync();
    
    this.reuseSpace = true;
    for (localIterator = paramArrayList.iterator(); localIterator.hasNext();)
    {
      localChunk = (Chunk)localIterator.next();
      if (this.chunks.containsKey(Integer.valueOf(localChunk.id)))
      {
        localWriteBuffer = getWriteBuffer();
        l1 = localChunk.block * 4096L;
        i = localChunk.len * 4096;
        localWriteBuffer.limit(i);
        localByteBuffer = this.fileStore.readFully(l1, i);
        Chunk.readChunkHeader(localByteBuffer, 0L);
        j = localByteBuffer.position();
        localWriteBuffer.position(j);
        localWriteBuffer.put(localByteBuffer);
        l2 = this.fileStore.allocate(i);
        this.fileStore.free(l1, i);
        localWriteBuffer.position(0);
        localChunk.block = (l2 / 4096L);
        localChunk.writeChunkHeader(localWriteBuffer, j);
        localWriteBuffer.position(i - 128);
        localWriteBuffer.put(this.lastChunk.getFooterBytes());
        localWriteBuffer.position(0);
        write(l2, localWriteBuffer.getBuffer());
        releaseWriteBuffer(localWriteBuffer);
        markMetaChanged();
        this.meta.put(Chunk.getMetaKey(localChunk.id), localChunk.asString());
      }
    }
    commitAndSave();
    sync();
    shrinkFileIfPossible(0);
  }
  
  public void sync()
  {
    this.fileStore.sync();
  }
  
  public boolean compact(int paramInt1, int paramInt2)
  {
    synchronized (this.compactSync)
    {
      checkOpen();
      ArrayList localArrayList;
      synchronized (this)
      {
        localArrayList = compactGetOldChunks(paramInt1, paramInt2);
      }
      if ((localArrayList == null) || (localArrayList.size() == 0)) {
        return false;
      }
      compactRewrite(localArrayList);
      return true;
    }
  }
  
  private ArrayList<Chunk> compactGetOldChunks(int paramInt1, int paramInt2)
  {
    if (this.lastChunk == null) {
      return null;
    }
    long l1 = 0L;
    long l2 = 0L;
    
    long l3 = getTime();
    for (Iterator localIterator1 = this.chunks.values().iterator(); localIterator1.hasNext();)
    {
      localObject1 = (Chunk)localIterator1.next();
      if (((Chunk)localObject1).time + this.retentionTime <= l3)
      {
        l1 += ((Chunk)localObject1).maxLen;
        l2 += ((Chunk)localObject1).maxLenLive;
      }
    }
    if (l2 < 0L) {
      return null;
    }
    if (l1 <= 0L) {
      l1 = 1L;
    }
    int i = (int)(100L * l2 / l1);
    if (i >= paramInt1) {
      return null;
    }
    Object localObject1 = New.arrayList();
    Chunk localChunk1 = (Chunk)this.chunks.get(Integer.valueOf(this.lastChunk.id));
    for (Chunk localChunk2 : this.chunks.values()) {
      if (localChunk2.time + this.retentionTime <= l3)
      {
        long l5 = localChunk1.version - localChunk2.version + 1L;
        localChunk2.collectPriority = ((int)(localChunk2.getFillRate() * 1000 / l5));
        ((ArrayList)localObject1).add(localChunk2);
      }
    }
    if (((ArrayList)localObject1).size() == 0) {
      return null;
    }
    Collections.sort((List)localObject1, new Comparator()
    {
      public int compare(Chunk paramAnonymousChunk1, Chunk paramAnonymousChunk2)
      {
        int i = new Integer(paramAnonymousChunk1.collectPriority).compareTo(Integer.valueOf(paramAnonymousChunk2.collectPriority));
        if (i == 0) {
          i = new Long(paramAnonymousChunk1.maxLenLive).compareTo(Long.valueOf(paramAnonymousChunk2.maxLenLive));
        }
        return i;
      }
    });
    long l4 = 0L;
    int j = 0;
    Object localObject2 = null;
    for (Iterator localIterator3 = ((ArrayList)localObject1).iterator(); localIterator3.hasNext();)
    {
      localObject3 = (Chunk)localIterator3.next();
      if ((localObject2 != null) && 
        (((Chunk)localObject3).collectPriority > 0) && (l4 > paramInt2)) {
        break;
      }
      l4 += ((Chunk)localObject3).maxLenLive;
      j++;
      localObject2 = localObject3;
    }
    if (j < 1) {
      return null;
    }
    int k = 0;
    for (Object localObject3 = ((ArrayList)localObject1).iterator(); ((Iterator)localObject3).hasNext();)
    {
      Chunk localChunk3 = (Chunk)((Iterator)localObject3).next();
      if (localObject2 == localChunk3) {
        k = 1;
      } else if (k != 0) {
        ((Iterator)localObject3).remove();
      }
    }
    return (ArrayList<Chunk>)localObject1;
  }
  
  private void compactRewrite(ArrayList<Chunk> paramArrayList)
  {
    HashSet localHashSet = New.hashSet();
    for (Iterator localIterator = paramArrayList.iterator(); localIterator.hasNext();)
    {
      localObject1 = (Chunk)localIterator.next();
      localHashSet.add(Integer.valueOf(((Chunk)localObject1).id));
    }
    Object localObject1;
    for (localIterator = this.maps.values().iterator(); localIterator.hasNext();)
    {
      localObject1 = (MVMap)localIterator.next();
      
      Object localObject2 = localObject1;
      if (!((MVMap)localObject2).rewrite(localHashSet)) {
        return;
      }
    }
    if (!this.meta.rewrite(localHashSet)) {
      return;
    }
    freeUnusedChunks();
    commitAndSave();
  }
  
  Page readPage(MVMap<?, ?> paramMVMap, long paramLong)
  {
    if (paramLong == 0L) {
      throw DataUtils.newIllegalStateException(6, "Position 0", new Object[0]);
    }
    Page localPage = this.cache == null ? null : (Page)this.cache.get(paramLong);
    if (localPage == null)
    {
      Chunk localChunk = getChunk(paramLong);
      long l1 = localChunk.block * 4096L;
      l1 += DataUtils.getPageOffset(paramLong);
      if (l1 < 0L) {
        throw DataUtils.newIllegalStateException(6, "Negative position {0}", new Object[] { Long.valueOf(l1) });
      }
      long l2 = (localChunk.block + localChunk.len) * 4096L;
      localPage = Page.read(this.fileStore, paramLong, paramMVMap, l1, l2);
      cachePage(paramLong, localPage, localPage.getMemory());
    }
    return localPage;
  }
  
  void removePage(MVMap<?, ?> paramMVMap, long paramLong, int paramInt)
  {
    if (paramLong == 0L)
    {
      this.unsavedMemory = Math.max(0, this.unsavedMemory - paramInt);
      return;
    }
    if ((this.cache != null) && 
      (DataUtils.getPageType(paramLong) == 0)) {
      this.cache.remove(paramLong);
    }
    Chunk localChunk = getChunk(paramLong);
    long l = this.currentVersion;
    if ((paramMVMap == this.meta) && (this.currentStoreVersion >= 0L) && 
      (Thread.currentThread() == this.currentStoreThread)) {
      l = this.currentStoreVersion;
    }
    registerFreePage(l, localChunk.id, DataUtils.getPageMaxLength(paramLong), 1);
  }
  
  private void registerFreePage(long paramLong1, int paramInt1, long paramLong2, int paramInt2)
  {
    Object localObject1 = (HashMap)this.freedPageSpace.get(Long.valueOf(paramLong1));
    if (localObject1 == null)
    {
      localObject1 = New.hashMap();
      HashMap localHashMap = (HashMap)this.freedPageSpace.putIfAbsent(Long.valueOf(paramLong1), localObject1);
      if (localHashMap != null) {
        localObject1 = localHashMap;
      }
    }
    synchronized (localObject1)
    {
      Chunk localChunk = (Chunk)((HashMap)localObject1).get(Integer.valueOf(paramInt1));
      if (localChunk == null)
      {
        localChunk = new Chunk(paramInt1);
        ((HashMap)localObject1).put(Integer.valueOf(paramInt1), localChunk);
      }
      localChunk.maxLenLive -= paramLong2;
      localChunk.pageCountLive -= paramInt2;
    }
  }
  
  Compressor getCompressorFast()
  {
    if (this.compressorFast == null) {
      this.compressorFast = new CompressLZF();
    }
    return this.compressorFast;
  }
  
  Compressor getCompressorHigh()
  {
    if (this.compressorHigh == null) {
      this.compressorHigh = new CompressDeflate();
    }
    return this.compressorHigh;
  }
  
  int getCompressionLevel()
  {
    return this.compressionLevel;
  }
  
  public int getPageSplitSize()
  {
    return this.pageSplitSize;
  }
  
  public boolean getReuseSpace()
  {
    return this.reuseSpace;
  }
  
  public void setReuseSpace(boolean paramBoolean)
  {
    this.reuseSpace = paramBoolean;
  }
  
  public int getRetentionTime()
  {
    return this.retentionTime;
  }
  
  public void setRetentionTime(int paramInt)
  {
    this.retentionTime = paramInt;
  }
  
  public void setVersionsToKeep(int paramInt)
  {
    this.versionsToKeep = paramInt;
  }
  
  public long getVersionsToKeep()
  {
    return this.versionsToKeep;
  }
  
  long getOldestVersionToKeep()
  {
    long l1 = this.currentVersion;
    if (this.fileStore == null) {
      return l1 - this.versionsToKeep;
    }
    long l2 = this.currentStoreVersion;
    if (l2 > -1L) {
      l1 = Math.min(l1, l2);
    }
    return l1;
  }
  
  private boolean isKnownVersion(long paramLong)
  {
    if ((paramLong > this.currentVersion) || (paramLong < 0L)) {
      return false;
    }
    if ((paramLong == this.currentVersion) || (this.chunks.size() == 0)) {
      return true;
    }
    Chunk localChunk = getChunkForVersion(paramLong);
    if (localChunk == null) {
      return false;
    }
    MVMap localMVMap = getMetaMap(paramLong);
    if (localMVMap == null) {
      return false;
    }
    Iterator localIterator = localMVMap.keyIterator("chunk.");
    while (localIterator.hasNext())
    {
      String str = (String)localIterator.next();
      if (!str.startsWith("chunk.")) {
        break;
      }
      if (!this.meta.containsKey(str)) {
        return false;
      }
    }
    return true;
  }
  
  void registerUnsavedPage(int paramInt)
  {
    this.unsavedMemory += paramInt;
    int i = this.unsavedMemory;
    if ((i > this.autoCommitMemory) && (this.autoCommitMemory > 0)) {
      this.saveNeeded = true;
    }
  }
  
  void beforeWrite(MVMap<?, ?> paramMVMap)
  {
    if (this.saveNeeded)
    {
      if (paramMVMap == this.meta) {
        return;
      }
      this.saveNeeded = false;
      if ((this.unsavedMemory > this.autoCommitMemory) && (this.autoCommitMemory > 0)) {
        commitAndSave();
      }
    }
  }
  
  public int getStoreVersion()
  {
    checkOpen();
    String str = (String)this.meta.get("setting.storeVersion");
    return str == null ? 0 : DataUtils.parseHexInt(str);
  }
  
  public synchronized void setStoreVersion(int paramInt)
  {
    checkOpen();
    markMetaChanged();
    this.meta.put("setting.storeVersion", Integer.toHexString(paramInt));
  }
  
  public void rollback()
  {
    rollbackTo(this.currentVersion);
  }
  
  public synchronized void rollbackTo(long paramLong)
  {
    checkOpen();
    if (paramLong == 0L)
    {
      for (localIterator1 = this.maps.values().iterator(); localIterator1.hasNext();)
      {
        localObject = (MVMap)localIterator1.next();
        ((MVMap)localObject).close();
      }
      this.meta.clear();
      this.chunks.clear();
      if (this.fileStore != null) {
        this.fileStore.clear();
      }
      this.maps.clear();
      this.freedPageSpace.clear();
      this.currentVersion = paramLong;
      setWriteVersion(paramLong);
      this.metaChanged = false;
      return;
    }
    DataUtils.checkArgument(isKnownVersion(paramLong), "Unknown version {0}", new Object[] { Long.valueOf(paramLong) });
    for (Iterator localIterator1 = this.maps.values().iterator(); localIterator1.hasNext();)
    {
      localObject = (MVMap)localIterator1.next();
      ((MVMap)localObject).rollbackTo(paramLong);
    }
    for (long l1 = this.currentVersion; (l1 >= paramLong) && 
          (this.freedPageSpace.size() != 0); l1 -= 1L) {
      this.freedPageSpace.remove(Long.valueOf(l1));
    }
    this.meta.rollbackTo(paramLong);
    this.metaChanged = false;
    int i = 0;
    
    Object localObject = null;
    Chunk localChunk1 = this.lastChunk;
    while ((localChunk1 != null) && (localChunk1.version >= paramLong))
    {
      localObject = localChunk1;
      localChunk1 = (Chunk)this.chunks.get(Integer.valueOf(localChunk1.id - 1));
    }
    Chunk localChunk2 = this.lastChunk;
    int j;
    if ((localObject != null) && (localChunk2.version > ((Chunk)localObject).version))
    {
      revertTemp(paramLong);
      i = 1;
      for (;;)
      {
        localChunk2 = this.lastChunk;
        if (localChunk2 == null) {
          break;
        }
        if (localChunk2.version <= ((Chunk)localObject).version) {
          break;
        }
        this.chunks.remove(Integer.valueOf(this.lastChunk.id));
        long l2 = localChunk2.block * 4096L;
        j = localChunk2.len * 4096;
        this.fileStore.free(l2, j);
        
        WriteBuffer localWriteBuffer = getWriteBuffer();
        localWriteBuffer.limit(j);
        
        Arrays.fill(localWriteBuffer.getBuffer().array(), (byte)0);
        write(l2, localWriteBuffer.getBuffer());
        releaseWriteBuffer(localWriteBuffer);
        this.lastChunk = ((Chunk)this.chunks.get(Integer.valueOf(this.lastChunk.id - 1)));
      }
      writeFileHeader();
      readFileHeader();
    }
    for (MVMap localMVMap : New.arrayList(this.maps.values()))
    {
      j = localMVMap.getId();
      if (localMVMap.getCreateVersion() >= paramLong)
      {
        localMVMap.close();
        this.maps.remove(Integer.valueOf(j));
      }
      else if (i != 0)
      {
        localMVMap.setRootPos(getRootPos(this.meta, j), -1L);
      }
    }
    if (this.lastChunk != null)
    {
      localChunk1 = (Chunk)this.chunks.get(Integer.valueOf(this.lastChunk.id - 1));
      if (localChunk1 != null) {
        this.meta.put(Chunk.getMetaKey(localChunk1.id), localChunk1.asString());
      }
    }
    this.currentVersion = paramLong;
    setWriteVersion(paramLong);
  }
  
  private static long getRootPos(MVMap<String, String> paramMVMap, int paramInt)
  {
    String str = (String)paramMVMap.get(MVMap.getMapRootKey(paramInt));
    return str == null ? 0L : DataUtils.parseHexLong(str);
  }
  
  private void revertTemp(long paramLong)
  {
    Iterator localIterator = this.freedPageSpace.keySet().iterator();
    while (localIterator.hasNext())
    {
      long l = ((Long)localIterator.next()).longValue();
      if (l <= paramLong) {
        localIterator.remove();
      }
    }
    for (MVMap localMVMap : this.maps.values()) {
      localMVMap.removeUnusedOldVersions();
    }
  }
  
  public long getCurrentVersion()
  {
    return this.currentVersion;
  }
  
  public FileStore getFileStore()
  {
    return this.fileStore;
  }
  
  public Map<String, Object> getStoreHeader()
  {
    return this.fileHeader;
  }
  
  private void checkOpen()
  {
    if (this.closed) {
      throw DataUtils.newIllegalStateException(4, "This store is closed", new Object[] { this.panicException });
    }
  }
  
  public synchronized void renameMap(MVMap<?, ?> paramMVMap, String paramString)
  {
    checkOpen();
    DataUtils.checkArgument(paramMVMap != this.meta, "Renaming the meta map is not allowed", new Object[0]);
    
    int i = paramMVMap.getId();
    String str1 = getMapName(i);
    if (str1.equals(paramString)) {
      return;
    }
    DataUtils.checkArgument(!this.meta.containsKey("name." + paramString), "A map named {0} already exists", new Object[] { paramString });
    
    markMetaChanged();
    String str2 = Integer.toHexString(i);
    this.meta.remove("name." + str1);
    this.meta.put(MVMap.getMapKey(i), paramMVMap.asString(paramString));
    this.meta.put("name." + paramString, str2);
  }
  
  public synchronized void removeMap(MVMap<?, ?> paramMVMap)
  {
    checkOpen();
    DataUtils.checkArgument(paramMVMap != this.meta, "Removing the meta map is not allowed", new Object[0]);
    
    paramMVMap.clear();
    int i = paramMVMap.getId();
    String str = getMapName(i);
    markMetaChanged();
    this.meta.remove(MVMap.getMapKey(i));
    this.meta.remove("name." + str);
    this.meta.remove(MVMap.getMapRootKey(i));
    this.maps.remove(Integer.valueOf(i));
  }
  
  public synchronized String getMapName(int paramInt)
  {
    checkOpen();
    String str = (String)this.meta.get(MVMap.getMapKey(paramInt));
    return str == null ? null : (String)DataUtils.parseMap(str).get("name");
  }
  
  void writeInBackground()
  {
    if (this.closed) {
      return;
    }
    long l1 = getTime();
    if (l1 <= this.lastCommitTime + this.autoCommitDelay) {
      return;
    }
    if (hasUnsavedChanges()) {
      try
      {
        commitAndSave();
      }
      catch (Exception localException1)
      {
        if (this.backgroundExceptionHandler != null)
        {
          this.backgroundExceptionHandler.uncaughtException(null, localException1);
          return;
        }
      }
    }
    if (this.autoCompactFillRate > 0) {
      try
      {
        long l2 = this.fileStore.getWriteCount() + this.fileStore.getReadCount();
        int i;
        if (this.autoCompactLastFileOpCount != l2) {
          i = 1;
        } else {
          i = 0;
        }
        int j = i != 0 ? this.autoCompactFillRate / 3 : this.autoCompactFillRate;
        
        compact(j, this.autoCommitMemory);
        this.autoCompactLastFileOpCount = (this.fileStore.getWriteCount() + this.fileStore.getReadCount());
      }
      catch (Exception localException2)
      {
        if (this.backgroundExceptionHandler != null) {
          this.backgroundExceptionHandler.uncaughtException(null, localException2);
        }
      }
    }
  }
  
  public void setCacheSize(int paramInt)
  {
    if (this.cache != null)
    {
      this.cache.setMaxMemory(paramInt * 1024L * 1024L);
      this.cache.clear();
    }
  }
  
  public boolean isClosed()
  {
    return this.closed;
  }
  
  private void stopBackgroundThread()
  {
    BackgroundWriterThread localBackgroundWriterThread = this.backgroundWriterThread;
    if (localBackgroundWriterThread == null) {
      return;
    }
    this.backgroundWriterThread = null;
    if (Thread.currentThread() == localBackgroundWriterThread) {
      return;
    }
    synchronized (localBackgroundWriterThread.sync)
    {
      localBackgroundWriterThread.sync.notifyAll();
    }
    if (Thread.holdsLock(this)) {
      return;
    }
    try
    {
      localBackgroundWriterThread.join();
    }
    catch (Exception localException) {}
  }
  
  public void setAutoCommitDelay(int paramInt)
  {
    if (this.autoCommitDelay == paramInt) {
      return;
    }
    this.autoCommitDelay = paramInt;
    if ((this.fileStore == null) || (this.fileStore.isReadOnly())) {
      return;
    }
    stopBackgroundThread();
    if (paramInt > 0)
    {
      int i = Math.max(1, paramInt / 10);
      BackgroundWriterThread localBackgroundWriterThread = new BackgroundWriterThread(this, i, this.fileStore.toString());
      
      localBackgroundWriterThread.start();
      this.backgroundWriterThread = localBackgroundWriterThread;
    }
  }
  
  public int getAutoCommitDelay()
  {
    return this.autoCommitDelay;
  }
  
  public int getAutoCommitMemory()
  {
    return this.autoCommitMemory;
  }
  
  public int getUnsavedMemory()
  {
    return this.unsavedMemory;
  }
  
  void cachePage(long paramLong, Page paramPage, int paramInt)
  {
    if (this.cache != null) {
      this.cache.put(paramLong, paramPage, paramInt);
    }
  }
  
  public int getCacheSizeUsed()
  {
    if (this.cache == null) {
      return 0;
    }
    return (int)(this.cache.getUsedMemory() / 1024L / 1024L);
  }
  
  public int getCacheSize()
  {
    if (this.cache == null) {
      return 0;
    }
    return (int)(this.cache.getMaxMemory() / 1024L / 1024L);
  }
  
  public CacheLongKeyLIRS<Page> getCache()
  {
    return this.cache;
  }
  
  private static class BackgroundWriterThread
    extends Thread
  {
    public final Object sync = new Object();
    private final MVStore store;
    private final int sleep;
    
    BackgroundWriterThread(MVStore paramMVStore, int paramInt, String paramString)
    {
      super();
      this.store = paramMVStore;
      this.sleep = paramInt;
      setDaemon(true);
    }
    
    public void run()
    {
      for (;;)
      {
        BackgroundWriterThread localBackgroundWriterThread = this.store.backgroundWriterThread;
        if (localBackgroundWriterThread == null) {
          break;
        }
        synchronized (this.sync)
        {
          try
          {
            this.sync.wait(this.sleep);
          }
          catch (InterruptedException localInterruptedException) {}
          continue;
        }
        this.store.writeInBackground();
      }
    }
  }
  
  public static class Builder
  {
    private final HashMap<String, Object> config = New.hashMap();
    
    private Builder set(String paramString, Object paramObject)
    {
      this.config.put(paramString, paramObject);
      return this;
    }
    
    public Builder autoCommitDisabled()
    {
      set("autoCommitBufferSize", Integer.valueOf(0));
      return set("autoCommitDelay", Integer.valueOf(0));
    }
    
    public Builder autoCommitBufferSize(int paramInt)
    {
      return set("autoCommitBufferSize", Integer.valueOf(paramInt));
    }
    
    public Builder autoCompactFillRate(int paramInt)
    {
      return set("autoCompactFillRate", Integer.valueOf(paramInt));
    }
    
    public Builder fileName(String paramString)
    {
      return set("fileName", paramString);
    }
    
    public Builder encryptionKey(char[] paramArrayOfChar)
    {
      return set("encryptionKey", paramArrayOfChar);
    }
    
    public Builder readOnly()
    {
      return set("readOnly", Integer.valueOf(1));
    }
    
    public Builder cacheSize(int paramInt)
    {
      return set("cacheSize", Integer.valueOf(paramInt));
    }
    
    public Builder compress()
    {
      return set("compress", Integer.valueOf(1));
    }
    
    public Builder compressHigh()
    {
      return set("compress", Integer.valueOf(2));
    }
    
    public Builder pageSplitSize(int paramInt)
    {
      return set("pageSplitSize", Integer.valueOf(paramInt));
    }
    
    public Builder backgroundExceptionHandler(Thread.UncaughtExceptionHandler paramUncaughtExceptionHandler)
    {
      return set("backgroundExceptionHandler", paramUncaughtExceptionHandler);
    }
    
    public Builder fileStore(FileStore paramFileStore)
    {
      return set("fileStore", paramFileStore);
    }
    
    public MVStore open()
    {
      return new MVStore(this.config);
    }
    
    public String toString()
    {
      return DataUtils.appendMap(new StringBuilder(), this.config).toString();
    }
    
    public static Builder fromString(String paramString)
    {
      HashMap localHashMap = DataUtils.parseMap(paramString);
      Builder localBuilder = new Builder();
      localBuilder.config.putAll(localHashMap);
      return localBuilder;
    }
  }
}
