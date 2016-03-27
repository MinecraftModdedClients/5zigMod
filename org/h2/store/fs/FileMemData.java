package org.h2.store.fs;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.h2.compress.CompressLZF;
import org.h2.util.MathUtils;

class FileMemData
{
  private static final int CACHE_SIZE = 8;
  private static final int BLOCK_SIZE_SHIFT = 10;
  private static final int BLOCK_SIZE = 1024;
  private static final int BLOCK_SIZE_MASK = 1023;
  private static final CompressLZF LZF = new CompressLZF();
  private static final byte[] BUFFER = new byte['ࠀ'];
  private static final byte[] COMPRESSED_EMPTY_BLOCK;
  private static final Cache<CompressItem, CompressItem> COMPRESS_LATER = new Cache(8);
  private String name;
  private final boolean compress;
  private long length;
  private byte[][] data;
  private long lastModified;
  private boolean isReadOnly;
  private boolean isLockedExclusive;
  private int sharedLockCount;
  
  static
  {
    byte[] arrayOfByte = new byte['Ѐ'];
    int i = LZF.compress(arrayOfByte, 1024, BUFFER, 0);
    COMPRESSED_EMPTY_BLOCK = new byte[i];
    System.arraycopy(BUFFER, 0, COMPRESSED_EMPTY_BLOCK, 0, i);
  }
  
  FileMemData(String paramString, boolean paramBoolean)
  {
    this.name = paramString;
    this.compress = paramBoolean;
    this.data = new byte[0][];
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
      FileMemData.CompressItem localCompressItem = (FileMemData.CompressItem)paramEntry.getKey();
      FileMemData.compress(localCompressItem.data, localCompressItem.page);
      return true;
    }
  }
  
  static class CompressItem
  {
    byte[][] data;
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
  
  private static void compressLater(byte[][] paramArrayOfByte, int paramInt)
  {
    CompressItem localCompressItem = new CompressItem();
    localCompressItem.data = paramArrayOfByte;
    localCompressItem.page = paramInt;
    synchronized (LZF)
    {
      COMPRESS_LATER.put(localCompressItem, localCompressItem);
    }
  }
  
  private static void expand(byte[][] paramArrayOfByte, int paramInt)
  {
    byte[] arrayOfByte1 = paramArrayOfByte[paramInt];
    if (arrayOfByte1.length == 1024) {
      return;
    }
    byte[] arrayOfByte2 = new byte['Ѐ'];
    if (arrayOfByte1 != COMPRESSED_EMPTY_BLOCK) {
      synchronized (LZF)
      {
        LZF.expand(arrayOfByte1, 0, arrayOfByte1.length, arrayOfByte2, 0, 1024);
      }
    }
    paramArrayOfByte[paramInt] = arrayOfByte2;
  }
  
  static void compress(byte[][] paramArrayOfByte, int paramInt)
  {
    byte[] arrayOfByte = paramArrayOfByte[paramInt];
    synchronized (LZF)
    {
      int i = LZF.compress(arrayOfByte, 1024, BUFFER, 0);
      if (i <= 1024)
      {
        arrayOfByte = new byte[i];
        System.arraycopy(BUFFER, 0, arrayOfByte, 0, i);
        paramArrayOfByte[paramInt] = arrayOfByte;
      }
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
      byte[] arrayOfByte = this.data[i];
      for (int j = (int)(paramLong & 0x3FF); j < 1024; j++) {
        arrayOfByte[j] = 0;
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
      byte[][] arrayOfByte = new byte[i][];
      System.arraycopy(this.data, 0, arrayOfByte, 0, Math.min(this.data.length, arrayOfByte.length));
      for (int j = this.data.length; j < i; j++) {
        arrayOfByte[j] = COMPRESSED_EMPTY_BLOCK;
      }
      this.data = arrayOfByte;
    }
  }
  
  long readWrite(long paramLong, byte[] paramArrayOfByte, int paramInt1, int paramInt2, boolean paramBoolean)
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
      byte[] arrayOfByte = this.data[j];
      int k = (int)(paramLong & 0x3FF);
      if (paramBoolean) {
        System.arraycopy(paramArrayOfByte, paramInt1, arrayOfByte, k, i);
      } else {
        System.arraycopy(arrayOfByte, k, paramArrayOfByte, paramInt1, i);
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
