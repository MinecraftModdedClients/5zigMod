package org.h2.mvstore;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import org.h2.compress.Compressor;
import org.h2.mvstore.type.DataType;
import org.h2.util.New;

public class Page
{
  public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
  private final MVMap<?, ?> map;
  private long version;
  private long pos;
  private long totalCount;
  private int cachedCompare;
  private int memory;
  private Object[] keys;
  private Object[] values;
  private PageReference[] children;
  private volatile boolean removedInMemory;
  
  Page(MVMap<?, ?> paramMVMap, long paramLong)
  {
    this.map = paramMVMap;
    this.version = paramLong;
  }
  
  static Page createEmpty(MVMap<?, ?> paramMVMap, long paramLong)
  {
    return create(paramMVMap, paramLong, EMPTY_OBJECT_ARRAY, EMPTY_OBJECT_ARRAY, null, 0L, 128);
  }
  
  public static Page create(MVMap<?, ?> paramMVMap, long paramLong1, Object[] paramArrayOfObject1, Object[] paramArrayOfObject2, PageReference[] paramArrayOfPageReference, long paramLong2, int paramInt)
  {
    Page localPage = new Page(paramMVMap, paramLong1);
    
    localPage.keys = paramArrayOfObject1;
    localPage.values = paramArrayOfObject2;
    localPage.children = paramArrayOfPageReference;
    localPage.totalCount = paramLong2;
    if (paramInt == 0) {
      localPage.recalculateMemory();
    } else {
      localPage.addMemory(paramInt);
    }
    MVStore localMVStore = paramMVMap.store;
    if (localMVStore != null) {
      localMVStore.registerUnsavedPage(localPage.memory);
    }
    return localPage;
  }
  
  public static Page create(MVMap<?, ?> paramMVMap, long paramLong, Page paramPage)
  {
    Page localPage = new Page(paramMVMap, paramLong);
    
    localPage.keys = paramPage.keys;
    localPage.values = paramPage.values;
    localPage.children = paramPage.children;
    localPage.totalCount = paramPage.totalCount;
    localPage.memory = paramPage.memory;
    MVStore localMVStore = paramMVMap.store;
    if (localMVStore != null) {
      localMVStore.registerUnsavedPage(localPage.memory);
    }
    return localPage;
  }
  
  static Page read(FileStore paramFileStore, long paramLong1, MVMap<?, ?> paramMVMap, long paramLong2, long paramLong3)
  {
    int i = DataUtils.getPageMaxLength(paramLong1);
    if (i == 2097152)
    {
      localByteBuffer = paramFileStore.readFully(paramLong2, 128);
      i = localByteBuffer.getInt();
    }
    i = (int)Math.min(paramLong3 - paramLong2, i);
    int j = i;
    if (j < 0) {
      throw DataUtils.newIllegalStateException(6, "Illegal page length {0} reading at {1}; max pos {2} ", new Object[] { Integer.valueOf(j), Long.valueOf(paramLong2), Long.valueOf(paramLong3) });
    }
    ByteBuffer localByteBuffer = paramFileStore.readFully(paramLong2, j);
    Page localPage = new Page(paramMVMap, 0L);
    localPage.pos = paramLong1;
    int k = DataUtils.getPageChunkId(paramLong1);
    int m = DataUtils.getPageOffset(paramLong1);
    localPage.read(localByteBuffer, k, m, i);
    return localPage;
  }
  
  public Object getKey(int paramInt)
  {
    return this.keys[paramInt];
  }
  
  public Page getChildPage(int paramInt)
  {
    PageReference localPageReference = this.children[paramInt];
    return localPageReference.page != null ? localPageReference.page : this.map.readPage(localPageReference.pos);
  }
  
  public long getChildPagePos(int paramInt)
  {
    return this.children[paramInt].pos;
  }
  
  public Object getValue(int paramInt)
  {
    return this.values[paramInt];
  }
  
  public int getKeyCount()
  {
    return this.keys.length;
  }
  
  public boolean isLeaf()
  {
    return this.children == null;
  }
  
  public long getPos()
  {
    return this.pos;
  }
  
  public String toString()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append("id: ").append(System.identityHashCode(this)).append('\n');
    localStringBuilder.append("version: ").append(Long.toHexString(this.version)).append("\n");
    localStringBuilder.append("pos: ").append(Long.toHexString(this.pos)).append("\n");
    if (this.pos != 0L)
    {
      i = DataUtils.getPageChunkId(this.pos);
      localStringBuilder.append("chunk: ").append(Long.toHexString(i)).append("\n");
    }
    for (int i = 0; i <= this.keys.length; i++)
    {
      if (i > 0) {
        localStringBuilder.append(" ");
      }
      if (this.children != null) {
        localStringBuilder.append("[" + Long.toHexString(this.children[i].pos) + "] ");
      }
      if (i < this.keys.length)
      {
        localStringBuilder.append(this.keys[i]);
        if (this.values != null)
        {
          localStringBuilder.append(':');
          localStringBuilder.append(this.values[i]);
        }
      }
    }
    return localStringBuilder.toString();
  }
  
  public Page copy(long paramLong)
  {
    Page localPage = create(this.map, paramLong, this.keys, this.values, this.children, this.totalCount, getMemory());
    
    removePage();
    localPage.cachedCompare = this.cachedCompare;
    return localPage;
  }
  
  public int binarySearch(Object paramObject)
  {
    int i = 0;int j = this.keys.length - 1;
    
    int k = this.cachedCompare - 1;
    if ((k < 0) || (k > j)) {
      k = j >>> 1;
    }
    Object[] arrayOfObject = this.keys;
    while (i <= j)
    {
      int m = this.map.compare(paramObject, arrayOfObject[k]);
      if (m > 0)
      {
        i = k + 1;
      }
      else if (m < 0)
      {
        j = k - 1;
      }
      else
      {
        this.cachedCompare = (k + 1);
        return k;
      }
      k = i + j >>> 1;
    }
    this.cachedCompare = i;
    return -(i + 1);
  }
  
  Page split(int paramInt)
  {
    return isLeaf() ? splitLeaf(paramInt) : splitNode(paramInt);
  }
  
  private Page splitLeaf(int paramInt)
  {
    int i = paramInt;int j = this.keys.length - i;
    Object[] arrayOfObject1 = new Object[i];
    Object[] arrayOfObject2 = new Object[j];
    System.arraycopy(this.keys, 0, arrayOfObject1, 0, i);
    System.arraycopy(this.keys, i, arrayOfObject2, 0, j);
    this.keys = arrayOfObject1;
    Object[] arrayOfObject3 = new Object[i];
    Object[] arrayOfObject4 = new Object[j];
    arrayOfObject4 = new Object[j];
    System.arraycopy(this.values, 0, arrayOfObject3, 0, i);
    System.arraycopy(this.values, i, arrayOfObject4, 0, j);
    this.values = arrayOfObject3;
    this.totalCount = i;
    Page localPage = create(this.map, this.version, arrayOfObject2, arrayOfObject4, null, arrayOfObject2.length, 0);
    
    recalculateMemory();
    localPage.recalculateMemory();
    return localPage;
  }
  
  private Page splitNode(int paramInt)
  {
    int i = paramInt;int j = this.keys.length - i;
    
    Object[] arrayOfObject1 = new Object[i];
    Object[] arrayOfObject2 = new Object[j - 1];
    System.arraycopy(this.keys, 0, arrayOfObject1, 0, i);
    System.arraycopy(this.keys, i + 1, arrayOfObject2, 0, j - 1);
    this.keys = arrayOfObject1;
    
    PageReference[] arrayOfPageReference1 = new PageReference[i + 1];
    PageReference[] arrayOfPageReference2 = new PageReference[j];
    System.arraycopy(this.children, 0, arrayOfPageReference1, 0, i + 1);
    System.arraycopy(this.children, i + 1, arrayOfPageReference2, 0, j);
    this.children = arrayOfPageReference1;
    
    long l = 0L;
    Object localObject2;
    for (localObject2 : arrayOfPageReference1) {
      l += ((PageReference)localObject2).count;
    }
    this.totalCount = l;
    l = 0L;
    for (localObject2 : arrayOfPageReference2) {
      l += ((PageReference)localObject2).count;
    }
    ??? = create(this.map, this.version, arrayOfObject2, null, arrayOfPageReference2, l, 0);
    
    recalculateMemory();
    ((Page)???).recalculateMemory();
    return (Page)???;
  }
  
  public long getTotalCount()
  {
    return this.totalCount;
  }
  
  long getCounts(int paramInt)
  {
    return this.children[paramInt].count;
  }
  
  public void setChild(int paramInt, Page paramPage)
  {
    long l;
    PageReference localPageReference;
    if (paramPage == null)
    {
      l = this.children[paramInt].count;
      this.children = ((PageReference[])Arrays.copyOf(this.children, this.children.length));
      localPageReference = new PageReference(null, 0L, 0L);
      this.children[paramInt] = localPageReference;
      this.totalCount -= l;
    }
    else if ((paramPage != this.children[paramInt].page) || (paramPage.getPos() != this.children[paramInt].pos))
    {
      l = this.children[paramInt].count;
      this.children = ((PageReference[])Arrays.copyOf(this.children, this.children.length));
      localPageReference = new PageReference(paramPage, paramPage.pos, paramPage.totalCount);
      this.children[paramInt] = localPageReference;
      this.totalCount += paramPage.totalCount - l;
    }
  }
  
  public void setKey(int paramInt, Object paramObject)
  {
    this.keys = Arrays.copyOf(this.keys, this.keys.length);
    Object localObject = this.keys[paramInt];
    DataType localDataType = this.map.getKeyType();
    int i = localDataType.getMemory(paramObject);
    if (localObject != null) {
      i -= localDataType.getMemory(localObject);
    }
    addMemory(i);
    this.keys[paramInt] = paramObject;
  }
  
  public Object setValue(int paramInt, Object paramObject)
  {
    Object localObject = this.values[paramInt];
    this.values = Arrays.copyOf(this.values, this.values.length);
    DataType localDataType = this.map.getValueType();
    addMemory(localDataType.getMemory(paramObject) - localDataType.getMemory(localObject));
    
    this.values[paramInt] = paramObject;
    return localObject;
  }
  
  void removeAllRecursive()
  {
    if (this.children != null)
    {
      int i = 0;
      for (int j = this.map.getChildPageCount(this); i < j; i++)
      {
        PageReference localPageReference = this.children[i];
        if (localPageReference.page != null)
        {
          localPageReference.page.removeAllRecursive();
        }
        else
        {
          long l = this.children[i].pos;
          int k = DataUtils.getPageType(l);
          if (k == 0)
          {
            int m = DataUtils.getPageMaxLength(l);
            this.map.removePage(l, m);
          }
          else
          {
            this.map.readPage(l).removeAllRecursive();
          }
        }
      }
    }
    removePage();
  }
  
  public void insertLeaf(int paramInt, Object paramObject1, Object paramObject2)
  {
    int i = this.keys.length + 1;
    Object[] arrayOfObject1 = new Object[i];
    DataUtils.copyWithGap(this.keys, arrayOfObject1, i - 1, paramInt);
    this.keys = arrayOfObject1;
    Object[] arrayOfObject2 = new Object[i];
    DataUtils.copyWithGap(this.values, arrayOfObject2, i - 1, paramInt);
    this.values = arrayOfObject2;
    this.keys[paramInt] = paramObject1;
    this.values[paramInt] = paramObject2;
    this.totalCount += 1L;
    addMemory(this.map.getKeyType().getMemory(paramObject1) + this.map.getValueType().getMemory(paramObject2));
  }
  
  public void insertNode(int paramInt, Object paramObject, Page paramPage)
  {
    Object[] arrayOfObject = new Object[this.keys.length + 1];
    DataUtils.copyWithGap(this.keys, arrayOfObject, this.keys.length, paramInt);
    arrayOfObject[paramInt] = paramObject;
    this.keys = arrayOfObject;
    
    int i = this.children.length;
    PageReference[] arrayOfPageReference = new PageReference[i + 1];
    DataUtils.copyWithGap(this.children, arrayOfPageReference, i, paramInt);
    arrayOfPageReference[paramInt] = new PageReference(paramPage, paramPage.getPos(), paramPage.totalCount);
    
    this.children = arrayOfPageReference;
    
    this.totalCount += paramPage.totalCount;
    addMemory(this.map.getKeyType().getMemory(paramObject) + 16);
  }
  
  public void remove(int paramInt)
  {
    int i = this.keys.length;
    int j = paramInt >= i ? paramInt - 1 : paramInt;
    Object localObject = this.keys[j];
    addMemory(-this.map.getKeyType().getMemory(localObject));
    Object[] arrayOfObject1 = new Object[i - 1];
    DataUtils.copyExcept(this.keys, arrayOfObject1, i, j);
    this.keys = arrayOfObject1;
    if (this.values != null)
    {
      localObject = this.values[paramInt];
      addMemory(-this.map.getValueType().getMemory(localObject));
      Object[] arrayOfObject2 = new Object[i - 1];
      DataUtils.copyExcept(this.values, arrayOfObject2, i, paramInt);
      this.values = arrayOfObject2;
      this.totalCount -= 1L;
    }
    if (this.children != null)
    {
      addMemory(-16);
      long l = this.children[paramInt].count;
      
      int k = this.children.length;
      PageReference[] arrayOfPageReference = new PageReference[k - 1];
      DataUtils.copyExcept(this.children, arrayOfPageReference, k, paramInt);
      this.children = arrayOfPageReference;
      
      this.totalCount -= l;
    }
  }
  
  void read(ByteBuffer paramByteBuffer, int paramInt1, int paramInt2, int paramInt3)
  {
    int i = paramByteBuffer.position();
    int j = paramByteBuffer.getInt();
    if (j > paramInt3) {
      throw DataUtils.newIllegalStateException(6, "File corrupted in chunk {0}, expected page length =< {1}, got {2}", new Object[] { Integer.valueOf(paramInt1), Integer.valueOf(paramInt3), Integer.valueOf(j) });
    }
    paramByteBuffer.limit(i + j);
    short s = paramByteBuffer.getShort();
    int k = DataUtils.readVarInt(paramByteBuffer);
    if (k != this.map.getId()) {
      throw DataUtils.newIllegalStateException(6, "File corrupted in chunk {0}, expected map id {1}, got {2}", new Object[] { Integer.valueOf(paramInt1), Integer.valueOf(this.map.getId()), Integer.valueOf(k) });
    }
    int m = DataUtils.getCheckValue(paramInt1) ^ DataUtils.getCheckValue(paramInt2) ^ DataUtils.getCheckValue(j);
    if (s != (short)m) {
      throw DataUtils.newIllegalStateException(6, "File corrupted in chunk {0}, expected check value {1}, got {2}", new Object[] { Integer.valueOf(paramInt1), Integer.valueOf(m), Short.valueOf(s) });
    }
    int n = DataUtils.readVarInt(paramByteBuffer);
    this.keys = new Object[n];
    int i1 = paramByteBuffer.get();
    int i2 = (i1 & 0x1) == 1 ? 1 : 0;
    int i6;
    if (i2 != 0)
    {
      this.children = new PageReference[n + 1];
      long[] arrayOfLong = new long[n + 1];
      for (int i4 = 0; i4 <= n; i4++) {
        arrayOfLong[i4] = paramByteBuffer.getLong();
      }
      long l1 = 0L;
      for (i6 = 0; i6 <= n; i6++)
      {
        long l2 = DataUtils.readVarLong(paramByteBuffer);
        l1 += l2;
        this.children[i6] = new PageReference(null, arrayOfLong[i6], l2);
      }
      this.totalCount = l1;
    }
    int i3 = (i1 & 0x2) != 0 ? 1 : 0;
    if (i3 != 0)
    {
      Compressor localCompressor;
      if ((i1 & 0x6) == 6) {
        localCompressor = this.map.getStore().getCompressorHigh();
      } else {
        localCompressor = this.map.getStore().getCompressorFast();
      }
      int i5 = DataUtils.readVarInt(paramByteBuffer);
      i6 = j + i - paramByteBuffer.position();
      byte[] arrayOfByte = DataUtils.newBytes(i6);
      paramByteBuffer.get(arrayOfByte);
      int i7 = i6 + i5;
      paramByteBuffer = ByteBuffer.allocate(i7);
      localCompressor.expand(arrayOfByte, 0, i6, paramByteBuffer.array(), paramByteBuffer.arrayOffset(), i7);
    }
    this.map.getKeyType().read(paramByteBuffer, this.keys, n, true);
    if (i2 == 0)
    {
      this.values = new Object[n];
      this.map.getValueType().read(paramByteBuffer, this.values, n, false);
      this.totalCount = n;
    }
    recalculateMemory();
  }
  
  private int write(Chunk paramChunk, WriteBuffer paramWriteBuffer)
  {
    int i = paramWriteBuffer.position();
    int j = this.keys.length;
    int k = this.children != null ? 1 : 0;
    
    paramWriteBuffer.putInt(0).putShort((short)0).putVarInt(this.map.getId()).putVarInt(j);
    
    int m = paramWriteBuffer.position();
    paramWriteBuffer.put((byte)k);
    if (k == 1)
    {
      writeChildren(paramWriteBuffer);
      for (n = 0; n <= j; n++) {
        paramWriteBuffer.putVarLong(this.children[n].count);
      }
    }
    int n = paramWriteBuffer.position();
    this.map.getKeyType().write(paramWriteBuffer, this.keys, j, true);
    if (k == 0) {
      this.map.getValueType().write(paramWriteBuffer, this.values, j, false);
    }
    MVStore localMVStore = this.map.getStore();
    int i1 = paramWriteBuffer.position() - n;
    if (i1 > 16)
    {
      i2 = localMVStore.getCompressionLevel();
      if (i2 > 0)
      {
        Compressor localCompressor;
        if (i2 == 1)
        {
          localCompressor = this.map.getStore().getCompressorFast();
          i4 = 2;
        }
        else
        {
          localCompressor = this.map.getStore().getCompressorHigh();
          i4 = 6;
        }
        byte[] arrayOfByte1 = new byte[i1];
        paramWriteBuffer.position(n).get(arrayOfByte1);
        byte[] arrayOfByte2 = new byte[i1 * 2];
        int i5 = localCompressor.compress(arrayOfByte1, i1, arrayOfByte2, 0);
        int i6 = DataUtils.getVarIntLen(i5 - i1);
        if (i5 + i6 < i1)
        {
          paramWriteBuffer.position(m).put((byte)(k + i4));
          
          paramWriteBuffer.position(n).putVarInt(i1 - i5).put(arrayOfByte2, 0, i5);
        }
      }
    }
    int i2 = paramWriteBuffer.position() - i;
    int i3 = paramChunk.id;
    int i4 = DataUtils.getCheckValue(i3) ^ DataUtils.getCheckValue(i) ^ DataUtils.getCheckValue(i2);
    
    paramWriteBuffer.putInt(i, i2).putShort(i + 4, (short)i4);
    if (this.pos != 0L) {
      throw DataUtils.newIllegalStateException(3, "Page already stored", new Object[0]);
    }
    this.pos = DataUtils.getPagePos(i3, i, i2, k);
    localMVStore.cachePage(this.pos, this, getMemory());
    if (k == 1) {
      localMVStore.cachePage(this.pos, this, getMemory());
    }
    long l = DataUtils.getPageMaxLength(this.pos);
    paramChunk.maxLen += l;
    paramChunk.maxLenLive += l;
    paramChunk.pageCount += 1;
    paramChunk.pageCountLive += 1;
    if (this.removedInMemory) {
      this.map.removePage(this.pos, this.memory);
    }
    return m + 1;
  }
  
  private void writeChildren(WriteBuffer paramWriteBuffer)
  {
    int i = this.keys.length;
    for (int j = 0; j <= i; j++) {
      paramWriteBuffer.putLong(this.children[j].pos);
    }
  }
  
  void writeUnsavedRecursive(Chunk paramChunk, WriteBuffer paramWriteBuffer)
  {
    if (this.pos != 0L) {
      return;
    }
    int i = write(paramChunk, paramWriteBuffer);
    if (!isLeaf())
    {
      int j = this.children.length;
      for (int k = 0; k < j; k++)
      {
        Page localPage = this.children[k].page;
        if (localPage != null)
        {
          localPage.writeUnsavedRecursive(paramChunk, paramWriteBuffer);
          this.children[k] = new PageReference(localPage, localPage.getPos(), localPage.totalCount);
        }
      }
      k = paramWriteBuffer.position();
      paramWriteBuffer.position(i);
      writeChildren(paramWriteBuffer);
      paramWriteBuffer.position(k);
    }
  }
  
  void writeEnd()
  {
    if (isLeaf()) {
      return;
    }
    int i = this.children.length;
    for (int j = 0; j < i; j++)
    {
      PageReference localPageReference = this.children[j];
      if (localPageReference.page != null)
      {
        if (localPageReference.page.getPos() == 0L) {
          throw DataUtils.newIllegalStateException(3, "Page not written", new Object[0]);
        }
        localPageReference.page.writeEnd();
        this.children[j] = new PageReference(null, localPageReference.pos, localPageReference.count);
      }
    }
  }
  
  long getVersion()
  {
    return this.version;
  }
  
  public int getRawChildPageCount()
  {
    return this.children.length;
  }
  
  public boolean equals(Object paramObject)
  {
    if (paramObject == this) {
      return true;
    }
    if ((paramObject instanceof Page))
    {
      if ((this.pos != 0L) && (((Page)paramObject).pos == this.pos)) {
        return true;
      }
      return this == paramObject;
    }
    return false;
  }
  
  public int hashCode()
  {
    return this.pos != 0L ? (int)(this.pos | this.pos >>> 32) : super.hashCode();
  }
  
  public int getMemory()
  {
    return this.memory;
  }
  
  private void addMemory(int paramInt)
  {
    this.memory += paramInt;
  }
  
  private void recalculateMemory()
  {
    int i = 128;
    DataType localDataType1 = this.map.getKeyType();
    for (int j = 0; j < this.keys.length; j++) {
      i += localDataType1.getMemory(this.keys[j]);
    }
    if (isLeaf())
    {
      DataType localDataType2 = this.map.getValueType();
      for (int k = 0; k < this.keys.length; k++) {
        i += localDataType2.getMemory(this.values[k]);
      }
    }
    else
    {
      i += getRawChildPageCount() * 16;
    }
    addMemory(i - this.memory);
  }
  
  void setVersion(long paramLong)
  {
    this.version = paramLong;
  }
  
  public void removePage()
  {
    long l = this.pos;
    if (l == 0L) {
      this.removedInMemory = true;
    }
    this.map.removePage(l, this.memory);
  }
  
  public static class PageReference
  {
    final long pos;
    final Page page;
    final long count;
    
    public PageReference(Page paramPage, long paramLong1, long paramLong2)
    {
      this.page = paramPage;
      this.pos = paramLong1;
      this.count = paramLong2;
    }
  }
  
  public static class PageChildren
  {
    public static final long[] EMPTY_ARRAY = new long[0];
    final long pos;
    long[] children;
    
    private PageChildren(long paramLong, long[] paramArrayOfLong)
    {
      this.pos = paramLong;
      this.children = paramArrayOfLong;
    }
    
    PageChildren(Page paramPage)
    {
      this.pos = paramPage.getPos();
      int i = paramPage.getRawChildPageCount();
      this.children = new long[i];
      for (int j = 0; j < i; j++) {
        this.children[j] = paramPage.getChildPagePos(j);
      }
    }
    
    int getMemory()
    {
      return 64 + 8 * this.children.length;
    }
    
    static PageChildren read(FileStore paramFileStore, long paramLong1, int paramInt, long paramLong2, long paramLong3)
    {
      int i = DataUtils.getPageMaxLength(paramLong1);
      if (i == 2097152)
      {
        localByteBuffer = paramFileStore.readFully(paramLong2, 128);
        i = localByteBuffer.getInt();
      }
      i = (int)Math.min(paramLong3 - paramLong2, i);
      int j = i;
      if (j < 0) {
        throw DataUtils.newIllegalStateException(6, "Illegal page length {0} reading at {1}; max pos {2} ", new Object[] { Integer.valueOf(j), Long.valueOf(paramLong2), Long.valueOf(paramLong3) });
      }
      ByteBuffer localByteBuffer = paramFileStore.readFully(paramLong2, j);
      int k = DataUtils.getPageChunkId(paramLong1);
      int m = DataUtils.getPageOffset(paramLong1);
      int n = localByteBuffer.position();
      int i1 = localByteBuffer.getInt();
      if (i1 > i) {
        throw DataUtils.newIllegalStateException(6, "File corrupted in chunk {0}, expected page length =< {1}, got {2}", new Object[] { Integer.valueOf(k), Integer.valueOf(i), Integer.valueOf(i1) });
      }
      localByteBuffer.limit(n + i1);
      short s = localByteBuffer.getShort();
      int i2 = DataUtils.readVarInt(localByteBuffer);
      if (i2 != paramInt) {
        throw DataUtils.newIllegalStateException(6, "File corrupted in chunk {0}, expected map id {1}, got {2}", new Object[] { Integer.valueOf(k), Integer.valueOf(paramInt), Integer.valueOf(i2) });
      }
      int i3 = DataUtils.getCheckValue(k) ^ DataUtils.getCheckValue(m) ^ DataUtils.getCheckValue(i1);
      if (s != (short)i3) {
        throw DataUtils.newIllegalStateException(6, "File corrupted in chunk {0}, expected check value {1}, got {2}", new Object[] { Integer.valueOf(k), Integer.valueOf(i3), Short.valueOf(s) });
      }
      int i4 = DataUtils.readVarInt(localByteBuffer);
      int i5 = localByteBuffer.get();
      int i6 = (i5 & 0x1) == 1 ? 1 : 0;
      if (i6 == 0) {
        return null;
      }
      long[] arrayOfLong = new long[i4 + 1];
      for (int i7 = 0; i7 <= i4; i7++) {
        arrayOfLong[i7] = localByteBuffer.getLong();
      }
      return new PageChildren(paramLong1, arrayOfLong);
    }
    
    void removeDuplicateChunkReferences()
    {
      HashSet localHashSet = New.hashSet();
      
      localHashSet.add(Integer.valueOf(DataUtils.getPageChunkId(this.pos)));
      for (int i = 0; i < this.children.length; i++)
      {
        long l = this.children[i];
        if (DataUtils.getPageType(l) != 1)
        {
          int j = DataUtils.getPageChunkId(l);
          if (!localHashSet.add(Integer.valueOf(j)))
          {
            long[] arrayOfLong = new long[this.children.length - 1];
            DataUtils.copyExcept(this.children, arrayOfLong, this.children.length, i);
            this.children = arrayOfLong;
            i--;
          }
        }
      }
      if (this.children.length == 0) {
        this.children = EMPTY_ARRAY;
      }
    }
  }
}
