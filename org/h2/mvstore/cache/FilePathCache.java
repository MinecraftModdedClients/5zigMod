package org.h2.mvstore.cache;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import org.h2.store.fs.FileBase;
import org.h2.store.fs.FilePath;
import org.h2.store.fs.FilePathWrapper;

public class FilePathCache
  extends FilePathWrapper
{
  public static FileChannel wrap(FileChannel paramFileChannel)
  {
    return new FileCache(paramFileChannel);
  }
  
  public FileChannel open(String paramString)
    throws IOException
  {
    return new FileCache(getBase().open(paramString));
  }
  
  public String getScheme()
  {
    return "cache";
  }
  
  public static class FileCache
    extends FileBase
  {
    private static final int CACHE_BLOCK_SIZE = 4096;
    private final FileChannel base;
    private final CacheLongKeyLIRS<ByteBuffer> cache = new CacheLongKeyLIRS(256);
    
    FileCache(FileChannel paramFileChannel)
    {
      this.base = paramFileChannel;
    }
    
    protected void implCloseChannel()
      throws IOException
    {
      this.base.close();
    }
    
    public FileChannel position(long paramLong)
      throws IOException
    {
      this.base.position(paramLong);
      return this;
    }
    
    public long position()
      throws IOException
    {
      return this.base.position();
    }
    
    public int read(ByteBuffer paramByteBuffer)
      throws IOException
    {
      return this.base.read(paramByteBuffer);
    }
    
    public int read(ByteBuffer paramByteBuffer, long paramLong)
      throws IOException
    {
      long l1 = getCachePos(paramLong);
      int i = (int)(paramLong - l1);
      int j = 4096 - i;
      j = Math.min(j, paramByteBuffer.remaining());
      ByteBuffer localByteBuffer = (ByteBuffer)this.cache.get(l1);
      if (localByteBuffer == null)
      {
        localByteBuffer = ByteBuffer.allocate(4096);
        long l2 = l1;
        for (;;)
        {
          k = this.base.read(localByteBuffer, l2);
          if (k <= 0) {
            break;
          }
          if (localByteBuffer.remaining() == 0) {
            break;
          }
          l2 += k;
        }
        int k = localByteBuffer.position();
        if (k == 4096)
        {
          this.cache.put(l1, localByteBuffer);
        }
        else
        {
          if (k <= 0) {
            return -1;
          }
          j = Math.min(j, k - i);
        }
      }
      paramByteBuffer.put(localByteBuffer.array(), i, j);
      return j == 0 ? -1 : j;
    }
    
    private static long getCachePos(long paramLong)
    {
      return paramLong / 4096L * 4096L;
    }
    
    public long size()
      throws IOException
    {
      return this.base.size();
    }
    
    public FileChannel truncate(long paramLong)
      throws IOException
    {
      this.cache.clear();
      this.base.truncate(paramLong);
      return this;
    }
    
    public int write(ByteBuffer paramByteBuffer, long paramLong)
      throws IOException
    {
      clearCache(paramByteBuffer, paramLong);
      return this.base.write(paramByteBuffer, paramLong);
    }
    
    public int write(ByteBuffer paramByteBuffer)
      throws IOException
    {
      clearCache(paramByteBuffer, position());
      return this.base.write(paramByteBuffer);
    }
    
    private void clearCache(ByteBuffer paramByteBuffer, long paramLong)
    {
      if (this.cache.size() > 0)
      {
        int i = paramByteBuffer.remaining();
        long l = getCachePos(paramLong);
        while (i > 0)
        {
          this.cache.remove(l);
          l += 4096L;
          i -= 4096;
        }
      }
    }
    
    public void force(boolean paramBoolean)
      throws IOException
    {
      this.base.force(paramBoolean);
    }
    
    public FileLock tryLock(long paramLong1, long paramLong2, boolean paramBoolean)
      throws IOException
    {
      return this.base.tryLock(paramLong1, paramLong2, paramBoolean);
    }
    
    public String toString()
    {
      return "cache:" + this.base.toString();
    }
  }
}
