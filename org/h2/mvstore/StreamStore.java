package org.h2.mvstore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class StreamStore
{
  private final Map<Long, byte[]> map;
  private int minBlockSize = 256;
  private int maxBlockSize = 262144;
  private final AtomicLong nextKey = new AtomicLong();
  private final AtomicReference<byte[]> nextBuffer = new AtomicReference();
  
  public StreamStore(Map<Long, byte[]> paramMap)
  {
    this.map = paramMap;
  }
  
  public Map<Long, byte[]> getMap()
  {
    return this.map;
  }
  
  public void setNextKey(long paramLong)
  {
    this.nextKey.set(paramLong);
  }
  
  public long getNextKey()
  {
    return this.nextKey.get();
  }
  
  public void setMinBlockSize(int paramInt)
  {
    this.minBlockSize = paramInt;
  }
  
  public int getMinBlockSize()
  {
    return this.minBlockSize;
  }
  
  public void setMaxBlockSize(int paramInt)
  {
    this.maxBlockSize = paramInt;
  }
  
  public long getMaxBlockSize()
  {
    return this.maxBlockSize;
  }
  
  public byte[] put(InputStream paramInputStream)
    throws IOException
  {
    ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
    int i = 0;
    try
    {
      while (!put(localByteArrayOutputStream, paramInputStream, i)) {
        if (localByteArrayOutputStream.size() > this.maxBlockSize / 2)
        {
          localByteArrayOutputStream = putIndirectId(localByteArrayOutputStream);
          i++;
        }
      }
    }
    catch (IOException localIOException)
    {
      remove(localByteArrayOutputStream.toByteArray());
      throw localIOException;
    }
    if (localByteArrayOutputStream.size() > this.minBlockSize * 2) {
      localByteArrayOutputStream = putIndirectId(localByteArrayOutputStream);
    }
    return localByteArrayOutputStream.toByteArray();
  }
  
  private boolean put(ByteArrayOutputStream paramByteArrayOutputStream, InputStream paramInputStream, int paramInt)
    throws IOException
  {
    if (paramInt > 0)
    {
      localObject = new ByteArrayOutputStream();
      for (;;)
      {
        boolean bool1 = put((ByteArrayOutputStream)localObject, paramInputStream, paramInt - 1);
        if (((ByteArrayOutputStream)localObject).size() > this.maxBlockSize / 2)
        {
          localObject = putIndirectId((ByteArrayOutputStream)localObject);
          ((ByteArrayOutputStream)localObject).writeTo(paramByteArrayOutputStream);
          return bool1;
        }
        if (bool1)
        {
          ((ByteArrayOutputStream)localObject).writeTo(paramByteArrayOutputStream);
          return true;
        }
      }
    }
    Object localObject = (byte[])this.nextBuffer.getAndSet(null);
    if (localObject == null) {
      localObject = new byte[this.maxBlockSize];
    }
    byte[] arrayOfByte = read(paramInputStream, (byte[])localObject);
    if (arrayOfByte != localObject) {
      this.nextBuffer.set(localObject);
    }
    int i = arrayOfByte.length;
    if (i == 0) {
      return true;
    }
    boolean bool2 = i < this.maxBlockSize;
    if (i < this.minBlockSize)
    {
      paramByteArrayOutputStream.write(0);
      DataUtils.writeVarInt(paramByteArrayOutputStream, i);
      paramByteArrayOutputStream.write(arrayOfByte);
    }
    else
    {
      paramByteArrayOutputStream.write(1);
      DataUtils.writeVarInt(paramByteArrayOutputStream, i);
      DataUtils.writeVarLong(paramByteArrayOutputStream, writeBlock(arrayOfByte));
    }
    return bool2;
  }
  
  private static byte[] read(InputStream paramInputStream, byte[] paramArrayOfByte)
    throws IOException
  {
    int i = 0;
    int j = paramArrayOfByte.length;
    while (j > 0) {
      try
      {
        int k = paramInputStream.read(paramArrayOfByte, i, j);
        if (k < 0) {
          return Arrays.copyOf(paramArrayOfByte, i);
        }
        i += k;
        j -= k;
      }
      catch (RuntimeException localRuntimeException)
      {
        throw new IOException(localRuntimeException);
      }
    }
    return paramArrayOfByte;
  }
  
  private ByteArrayOutputStream putIndirectId(ByteArrayOutputStream paramByteArrayOutputStream)
    throws IOException
  {
    byte[] arrayOfByte = paramByteArrayOutputStream.toByteArray();
    paramByteArrayOutputStream = new ByteArrayOutputStream();
    
    paramByteArrayOutputStream.write(2);
    DataUtils.writeVarLong(paramByteArrayOutputStream, length(arrayOfByte));
    DataUtils.writeVarLong(paramByteArrayOutputStream, writeBlock(arrayOfByte));
    return paramByteArrayOutputStream;
  }
  
  private long writeBlock(byte[] paramArrayOfByte)
  {
    long l = getAndIncrementNextKey();
    this.map.put(Long.valueOf(l), paramArrayOfByte);
    onStore(paramArrayOfByte.length);
    return l;
  }
  
  protected void onStore(int paramInt) {}
  
  private long getAndIncrementNextKey()
  {
    long l1 = this.nextKey.getAndIncrement();
    if (!this.map.containsKey(Long.valueOf(l1))) {
      return l1;
    }
    synchronized (this)
    {
      long l2 = l1;long l3 = Long.MAX_VALUE;
      while (l2 < l3)
      {
        long l4 = l2 + l3 >>> 1;
        if (this.map.containsKey(Long.valueOf(l4))) {
          l2 = l4 + 1L;
        } else {
          l3 = l4;
        }
      }
      l1 = l2;
      this.nextKey.set(l1 + 1L);
      return l1;
    }
  }
  
  public void remove(byte[] paramArrayOfByte)
  {
    ByteBuffer localByteBuffer = ByteBuffer.wrap(paramArrayOfByte);
    while (localByteBuffer.hasRemaining()) {
      switch (localByteBuffer.get())
      {
      case 0: 
        int i = DataUtils.readVarInt(localByteBuffer);
        localByteBuffer.position(localByteBuffer.position() + i);
        break;
      case 1: 
        DataUtils.readVarInt(localByteBuffer);
        long l1 = DataUtils.readVarLong(localByteBuffer);
        this.map.remove(Long.valueOf(l1));
        break;
      case 2: 
        DataUtils.readVarLong(localByteBuffer);
        long l2 = DataUtils.readVarLong(localByteBuffer);
        
        remove((byte[])this.map.get(Long.valueOf(l2)));
        this.map.remove(Long.valueOf(l2));
        break;
      default: 
        throw DataUtils.newIllegalArgumentException("Unsupported id {0}", new Object[] { Arrays.toString(paramArrayOfByte) });
      }
    }
  }
  
  public long length(byte[] paramArrayOfByte)
  {
    ByteBuffer localByteBuffer = ByteBuffer.wrap(paramArrayOfByte);
    long l = 0L;
    while (localByteBuffer.hasRemaining()) {
      switch (localByteBuffer.get())
      {
      case 0: 
        int i = DataUtils.readVarInt(localByteBuffer);
        localByteBuffer.position(localByteBuffer.position() + i);
        l += i;
        break;
      case 1: 
        l += DataUtils.readVarInt(localByteBuffer);
        DataUtils.readVarLong(localByteBuffer);
        break;
      case 2: 
        l += DataUtils.readVarLong(localByteBuffer);
        DataUtils.readVarLong(localByteBuffer);
        break;
      default: 
        throw DataUtils.newIllegalArgumentException("Unsupported id {0}", new Object[] { Arrays.toString(paramArrayOfByte) });
      }
    }
    return l;
  }
  
  public boolean isInPlace(byte[] paramArrayOfByte)
  {
    ByteBuffer localByteBuffer = ByteBuffer.wrap(paramArrayOfByte);
    while (localByteBuffer.hasRemaining())
    {
      if (localByteBuffer.get() != 0) {
        return false;
      }
      int i = DataUtils.readVarInt(localByteBuffer);
      localByteBuffer.position(localByteBuffer.position() + i);
    }
    return true;
  }
  
  public InputStream get(byte[] paramArrayOfByte)
  {
    return new Stream(this, paramArrayOfByte);
  }
  
  byte[] getBlock(long paramLong)
  {
    return (byte[])this.map.get(Long.valueOf(paramLong));
  }
  
  static class Stream
    extends InputStream
  {
    private final StreamStore store;
    private byte[] oneByteBuffer;
    private ByteBuffer idBuffer;
    private ByteArrayInputStream buffer;
    private long skip;
    private final long length;
    private long pos;
    
    Stream(StreamStore paramStreamStore, byte[] paramArrayOfByte)
    {
      this.store = paramStreamStore;
      this.length = paramStreamStore.length(paramArrayOfByte);
      this.idBuffer = ByteBuffer.wrap(paramArrayOfByte);
    }
    
    public int read()
    {
      byte[] arrayOfByte = this.oneByteBuffer;
      if (arrayOfByte == null) {
        arrayOfByte = this.oneByteBuffer = new byte[1];
      }
      int i = read(arrayOfByte, 0, 1);
      return i == -1 ? -1 : arrayOfByte[0] & 0xFF;
    }
    
    public long skip(long paramLong)
    {
      paramLong = Math.min(this.length - this.pos, paramLong);
      if (paramLong == 0L) {
        return 0L;
      }
      if (this.buffer != null)
      {
        long l = this.buffer.skip(paramLong);
        if (l > 0L)
        {
          paramLong = l;
        }
        else
        {
          this.buffer = null;
          this.skip += paramLong;
        }
      }
      else
      {
        this.skip += paramLong;
      }
      this.pos += paramLong;
      return paramLong;
    }
    
    public void close()
    {
      this.buffer = null;
      this.idBuffer.position(this.idBuffer.limit());
      this.pos = this.length;
    }
    
    public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    {
      if (paramInt2 <= 0) {
        return 0;
      }
      for (;;)
      {
        if (this.buffer == null)
        {
          this.buffer = nextBuffer();
          if (this.buffer == null) {
            return -1;
          }
        }
        int i = this.buffer.read(paramArrayOfByte, paramInt1, paramInt2);
        if (i > 0)
        {
          this.pos += i;
          return i;
        }
        this.buffer = null;
      }
    }
    
    private ByteArrayInputStream nextBuffer()
    {
      while (this.idBuffer.hasRemaining())
      {
        int i;
        switch (this.idBuffer.get())
        {
        case 0: 
          i = DataUtils.readVarInt(this.idBuffer);
          if (this.skip >= i)
          {
            this.skip -= i;
            this.idBuffer.position(this.idBuffer.position() + i);
          }
          else
          {
            int j = (int)(this.idBuffer.position() + this.skip);
            int k = (int)(i - this.skip);
            this.idBuffer.position(j + k);
            return new ByteArrayInputStream(this.idBuffer.array(), j, k);
          }
          break;
        case 1: 
          i = DataUtils.readVarInt(this.idBuffer);
          long l2 = DataUtils.readVarLong(this.idBuffer);
          if (this.skip >= i)
          {
            this.skip -= i;
          }
          else
          {
            byte[] arrayOfByte1 = this.store.getBlock(l2);
            int m = (int)this.skip;
            this.skip = 0L;
            return new ByteArrayInputStream(arrayOfByte1, m, arrayOfByte1.length - m);
          }
          break;
        case 2: 
          long l1 = DataUtils.readVarLong(this.idBuffer);
          long l3 = DataUtils.readVarLong(this.idBuffer);
          if (this.skip >= l1)
          {
            this.skip -= l1;
          }
          else
          {
            byte[] arrayOfByte2 = this.store.getBlock(l3);
            ByteBuffer localByteBuffer = ByteBuffer.allocate(arrayOfByte2.length + this.idBuffer.limit() - this.idBuffer.position());
            
            localByteBuffer.put(arrayOfByte2);
            localByteBuffer.put(this.idBuffer);
            localByteBuffer.flip();
            this.idBuffer = localByteBuffer;
            return nextBuffer();
          }
          break;
        default: 
          throw DataUtils.newIllegalArgumentException("Unsupported id {0}", new Object[] { Arrays.toString(this.idBuffer.array()) });
        }
      }
      return null;
    }
  }
}
