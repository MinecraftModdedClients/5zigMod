package org.h2.store.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.h2.compress.CompressLZF;
import org.h2.util.MathUtils;

class FileNioMemData
{
  private static final int CACHE_SIZE = 8;
  private static final int BLOCK_SIZE_SHIFT = 10;
  private static final int BLOCK_SIZE = 1024;
  private static final int BLOCK_SIZE_MASK = 1023;
  private static final CompressLZF LZF = new CompressLZF();
  private static final byte[] BUFFER = new byte['ࠀ'];
  private static final ByteBuffer COMPRESSED_EMPTY_BLOCK;
  private static final Cache<CompressItem, CompressItem> COMPRESS_LATER = new Cache(8);
  private String name;
  private final boolean compress;
  private long length;
  private ByteBuffer[] data;
  private long lastModified;
  private boolean isReadOnly;
  private boolean isLockedExclusive;
  private int sharedLockCount;
  
  static
  {
    byte[] arrayOfByte = new byte['Ѐ'];
    int i = LZF.compress(arrayOfByte, 1024, BUFFER, 0);
    COMPRESSED_EMPTY_BLOCK = ByteBuffer.allocateDirect(i);
    COMPRESSED_EMPTY_BLOCK.put(BUFFER, 0, i);
  }
  
  FileNioMemData(String paramString, boolean paramBoolean)
  {
    this.name = paramString;
    this.compress = paramBoolean;
    this.data = new ByteBuffer[0];
    this.lastModified = System.currentTimeMillis();
  }
  
  synchronized boolean lockExclusive()
  {
    if ((this.sharedLockCount > 0) || (this.isLockedExclusive)) {
      return false;
    }
    this.isLockedExclusive = true;
    return true;
  }
  
  synchronized boolean lockShared()
  {
    if (this.isLockedExclusive) {
      return false;
    }
    this.sharedLockCount += 1;
    return true;
  }
  
  synchronized void unlock()
  {
    if (this.isLockedExclusive) {
      this.isLockedExclusive = false;
    } else {
      this.sharedLockCount = Math.max(0, this.sharedLockCount - 1);
    }
  }
  
  static class Cache<K, V>
    extends LinkedHashMap<K, V>
  {
    private static final long serialVersionUID = 1L;
    private final int size;
    
    Cache(int paramInt)
    {
      super(0.75F, true);
      this.size = paramInt;
    }
    
    protected boolean removeEldestEntry(Map.Entry<K, V> paramEntry)
    {
      if (size() < this.size) {
        return false;
      }
      FileNioMemData.CompressItem localCompressItem = (FileNioMemData.CompressItem)paramEntry.getKey();
      FileNioMemData.compress(localCompressItem.data, localCompressItem.page);
      return true;
    }
  }
  
  static class CompressItem
  {
    ByteBuffer[] data;
    int page;
    
    public int hashCode()
    {
      return this.page;
    }
    
    public boolean equals(Object paramObject)
    {
      if ((paramObject instanceof CompressItem))
      {
        CompressItem localCompressItem = (CompressItem)paramObject;
        return (localCompressItem.data == this.data) && (localCompressItem.page == this.page);
      }
      return false;
    }
  }
  
  private static void compressLater(ByteBuffer[] paramArrayOfByteBuffer, int paramInt)
  {
    CompressItem localCompressItem = new CompressItem();
    localCompressItem.data = paramArrayOfByteBuffer;
    localCompressItem.page = paramInt;
    synchronized (LZF)
    {
      COMPRESS_LATER.put(localCompressItem, localCompressItem);
    }
  }
  
  private static void expand(ByteBuffer[] paramArrayOfByteBuffer, int paramInt)
  {
    ByteBuffer localByteBuffer1 = paramArrayOfByteBuffer[paramInt];
    if (localByteBuffer1.capacity() == 1024) {
      return;
    }
    ByteBuffer localByteBuffer2 = ByteBuffer.allocateDirect(1024);
    if (localByteBuffer1 != COMPRESSED_EMPTY_BLOCK) {
      synchronized (LZF)
      {
        CompressLZF.expand(localByteBuffer1, localByteBuffer2);
      }
    }
    paramArrayOfByteBuffer[paramInt] = localByteBuffer2;
  }
  
  static void compress(ByteBuffer[] paramArrayOfByteBuffer, int paramInt)
  {
    ByteBuffer localByteBuffer = paramArrayOfByteBuffer[paramInt];
    synchronized (LZF)
    {
      int i = LZF.compress(localByteBuffer, BUFFER, 0);
      localByteBuffer = ByteBuffer.allocateDirect(i);
      localByteBuffer.put(BUFFER, 0, i);
      paramArrayOfByteBuffer[paramInt] = localByteBuffer;
    }
  }
  
  void touch(boolean paramBoolean)
    throws IOException
  {
    if ((this.isReadOnly) || (paramBoolean)) {
      throw new IOException("Read only");
    }
    this.lastModified = System.currentTimeMillis();
  }
  
  long length()
  {
    return this.length;
  }
  
  void truncate(long paramLong)
  {
    changeLength(paramLong);
    long l = MathUtils.roundUpLong(paramLong, 1024L);
    if (l != paramLong)
    {
      int i = (int)(paramLong >>> 10);
      expand(this.data, i);
      ByteBuffer localByteBuffer = this.data[i];
      for (int j = (int)(paramLong & 0x3FF); j < 1024; j++) {
        localByteBuffer.put(j, (byte)0);
      }
      if (this.compress) {
        compressLater(this.data, i);
      }
    }
  }
  
  private void changeLength(long paramLong)
  {
    this.length = paramLong;
    paramLong = MathUtils.roundUpLong(paramLong, 1024L);
    int i = (int)(paramLong >>> 10);
    if (i != this.data.length)
    {
      ByteBuffer[] arrayOfByteBuffer = new ByteBuffer[i];
      System.arraycopy(this.data, 0, arrayOfByteBuffer, 0, Math.min(this.data.length, arrayOfByteBuffer.length));
      for (int j = this.data.length; j < i; j++) {
        arrayOfByteBuffer[j] = COMPRESSED_EMPTY_BLOCK;
      }
      this.data = arrayOfByteBuffer;
    }
  }
  
  long readWrite(long paramLong, ByteBuffer paramByteBuffer, int paramInt1, int paramInt2, boolean paramBoolean)
  {
    long l = paramLong + paramInt2;
    if (l > this.length) {
      if (paramBoolean) {
        changeLength(l);
      } else {
        paramInt2 = (int)(this.length - paramLong);
      }
    }
    while (paramInt2 > 0)
    {
      int i = (int)Math.min(paramInt2, 1024L - (paramLong & 0x3FF));
      int j = (int)(paramLong >>> 10);
      expand(this.data, j);
      ByteBuffer localByteBuffer1 = this.data[j];
      int k = (int)(paramLong & 0x3FF);
      ByteBuffer localByteBuffer2;
      if (paramBoolean)
      {
        localByteBuffer2 = paramByteBuffer.slice();
        localByteBuffer2.position(paramInt1);
        localByteBuffer2.limit(paramInt1 + i);
        localByteBuffer1.position(k);
        localByteBuffer1.put(localByteBuffer2);
      }
      else
      {
        localByteBuffer1.position(k);
        localByteBuffer2 = localByteBuffer1.slice();
        localByteBuffer2.limit(i);
        int m = paramByteBuffer.position();
        paramByteBuffer.position(paramInt1);
        paramByteBuffer.put(localByteBuffer2);
        
        paramByteBuffer.position(m);
      }
      if (this.compress) {
        compressLater(this.data, j);
      }
      paramInt1 += i;
      paramLong += i;
      paramInt2 -= i;
    }
    return paramLong;
  }
  
  void setName(String paramString)
  {
    this.name = paramString;
  }
  
  String getName()
  {
    return this.name;
  }
  
  long getLastModified()
  {
    return this.lastModified;
  }
  
  boolean canWrite()
  {
    return !this.isReadOnly;
  }
  
  boolean setReadOnly()
  {
    this.isReadOnly = true;
    return true;
  }
}
