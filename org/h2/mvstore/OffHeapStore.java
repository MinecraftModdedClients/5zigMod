package org.h2.mvstore;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class OffHeapStore
  extends FileStore
{
  private final TreeMap<Long, ByteBuffer> memory = new TreeMap();
  
  public void open(String paramString, boolean paramBoolean, char[] paramArrayOfChar)
  {
    this.memory.clear();
  }
  
  public String toString()
  {
    return this.memory.toString();
  }
  
  public ByteBuffer readFully(long paramLong, int paramInt)
  {
    Map.Entry localEntry = this.memory.floorEntry(Long.valueOf(paramLong));
    if (localEntry == null) {
      throw DataUtils.newIllegalStateException(1, "Could not read from position {0}", new Object[] { Long.valueOf(paramLong) });
    }
    this.readCount += 1L;
    this.readBytes += paramInt;
    ByteBuffer localByteBuffer1 = (ByteBuffer)localEntry.getValue();
    ByteBuffer localByteBuffer2 = localByteBuffer1.duplicate();
    int i = (int)(paramLong - ((Long)localEntry.getKey()).longValue());
    localByteBuffer2.position(i);
    localByteBuffer2.limit(paramInt + i);
    return localByteBuffer2.slice();
  }
  
  public void free(long paramLong, int paramInt)
  {
    this.freeSpace.free(paramLong, paramInt);
    ByteBuffer localByteBuffer = (ByteBuffer)this.memory.remove(Long.valueOf(paramLong));
    if (localByteBuffer != null) {
      if (localByteBuffer.remaining() != paramInt) {
        throw DataUtils.newIllegalStateException(1, "Partial remove is not supported at position {0}", new Object[] { Long.valueOf(paramLong) });
      }
    }
  }
  
  public void writeFully(long paramLong, ByteBuffer paramByteBuffer)
  {
    this.fileSize = Math.max(this.fileSize, paramLong + paramByteBuffer.remaining());
    Map.Entry localEntry = this.memory.floorEntry(Long.valueOf(paramLong));
    if (localEntry == null)
    {
      writeNewEntry(paramLong, paramByteBuffer);
      return;
    }
    long l = ((Long)localEntry.getKey()).longValue();
    ByteBuffer localByteBuffer = (ByteBuffer)localEntry.getValue();
    int i = localByteBuffer.capacity();
    int j = paramByteBuffer.remaining();
    if (l == paramLong)
    {
      if (i != j) {
        throw DataUtils.newIllegalStateException(1, "Could not write to position {0}; partial overwrite is not supported", new Object[] { Long.valueOf(paramLong) });
      }
      this.writeCount += 1L;
      this.writeBytes += j;
      localByteBuffer.rewind();
      localByteBuffer.put(paramByteBuffer);
      return;
    }
    if (l + i > paramLong) {
      throw DataUtils.newIllegalStateException(1, "Could not write to position {0}; partial overwrite is not supported", new Object[] { Long.valueOf(paramLong) });
    }
    writeNewEntry(paramLong, paramByteBuffer);
  }
  
  private void writeNewEntry(long paramLong, ByteBuffer paramByteBuffer)
  {
    int i = paramByteBuffer.remaining();
    this.writeCount += 1L;
    this.writeBytes += i;
    ByteBuffer localByteBuffer = ByteBuffer.allocateDirect(i);
    localByteBuffer.put(paramByteBuffer);
    localByteBuffer.rewind();
    this.memory.put(Long.valueOf(paramLong), localByteBuffer);
  }
  
  public void truncate(long paramLong)
  {
    this.writeCount += 1L;
    if (paramLong == 0L)
    {
      this.fileSize = 0L;
      this.memory.clear();
      return;
    }
    this.fileSize = paramLong;
    for (Iterator localIterator = this.memory.keySet().iterator(); localIterator.hasNext();)
    {
      long l = ((Long)localIterator.next()).longValue();
      if (l < paramLong) {
        break;
      }
      ByteBuffer localByteBuffer = (ByteBuffer)this.memory.get(Long.valueOf(l));
      if (localByteBuffer.capacity() > paramLong) {
        throw DataUtils.newIllegalStateException(1, "Could not truncate to {0}; partial truncate is not supported", new Object[] { Long.valueOf(l) });
      }
      localIterator.remove();
    }
  }
  
  public void close()
  {
    this.memory.clear();
  }
  
  public void sync() {}
  
  public int getDefaultRetentionTime()
  {
    return 0;
  }
}
