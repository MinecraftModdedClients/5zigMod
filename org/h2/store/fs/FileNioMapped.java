package org.h2.store.fs;

import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import org.h2.engine.SysProperties;

class FileNioMapped
  extends FileBase
{
  private static final long GC_TIMEOUT_MS = 10000L;
  private final String name;
  private final FileChannel.MapMode mode;
  private RandomAccessFile file;
  private MappedByteBuffer mapped;
  private long fileLength;
  private int pos;
  
  FileNioMapped(String paramString1, String paramString2)
    throws IOException
  {
    if ("r".equals(paramString2)) {
      this.mode = FileChannel.MapMode.READ_ONLY;
    } else {
      this.mode = FileChannel.MapMode.READ_WRITE;
    }
    this.name = paramString1;
    this.file = new RandomAccessFile(paramString1, paramString2);
    reMap();
  }
  
  private void unMap()
    throws IOException
  {
    if (this.mapped == null) {
      return;
    }
    this.mapped.force();
    
    int i = 1;
    if (SysProperties.NIO_CLEANER_HACK) {
      try
      {
        Method localMethod1 = this.mapped.getClass().getMethod("cleaner", new Class[0]);
        localMethod1.setAccessible(true);
        Object localObject1 = localMethod1.invoke(this.mapped, new Object[0]);
        if (localObject1 != null)
        {
          Method localMethod2 = localObject1.getClass().getMethod("clean", new Class[0]);
          localMethod2.invoke(localObject1, new Object[0]);
        }
        i = 0;
      }
      catch (Throwable localThrowable) {}finally
      {
        this.mapped = null;
      }
    }
    if (i != 0)
    {
      WeakReference localWeakReference = new WeakReference(this.mapped);
      
      this.mapped = null;
      long l = System.currentTimeMillis();
      while (localWeakReference.get() != null)
      {
        if (System.currentTimeMillis() - l > 10000L) {
          throw new IOException("Timeout (10000 ms) reached while trying to GC mapped buffer");
        }
        System.gc();
        Thread.yield();
      }
    }
  }
  
  private void reMap()
    throws IOException
  {
    int i = 0;
    if (this.mapped != null)
    {
      i = this.pos;
      unMap();
    }
    this.fileLength = this.file.length();
    checkFileSizeLimit(this.fileLength);
    
    this.mapped = this.file.getChannel().map(this.mode, 0L, this.fileLength);
    int j = this.mapped.limit();
    int k = this.mapped.capacity();
    if ((j < this.fileLength) || (k < this.fileLength)) {
      throw new IOException("Unable to map: length=" + j + " capacity=" + k + " length=" + this.fileLength);
    }
    if (SysProperties.NIO_LOAD_MAPPED) {
      this.mapped.load();
    }
    this.pos = Math.min(i, (int)this.fileLength);
  }
  
  private static void checkFileSizeLimit(long paramLong)
    throws IOException
  {
    if (paramLong > 2147483647L) {
      throw new IOException("File over 2GB is not supported yet when using this file system");
    }
  }
  
  public void implCloseChannel()
    throws IOException
  {
    if (this.file != null)
    {
      unMap();
      this.file.close();
      this.file = null;
    }
  }
  
  public long position()
  {
    return this.pos;
  }
  
  public String toString()
  {
    return "nioMapped:" + this.name;
  }
  
  public synchronized long size()
    throws IOException
  {
    return this.fileLength;
  }
  
  public synchronized int read(ByteBuffer paramByteBuffer)
    throws IOException
  {
    try
    {
      int i = paramByteBuffer.remaining();
      if (i == 0) {
        return 0;
      }
      i = (int)Math.min(i, this.fileLength - this.pos);
      if (i <= 0) {
        return -1;
      }
      this.mapped.position(this.pos);
      this.mapped.get(paramByteBuffer.array(), paramByteBuffer.arrayOffset() + paramByteBuffer.position(), i);
      paramByteBuffer.position(paramByteBuffer.position() + i);
      this.pos += i;
      return i;
    }
    catch (IllegalArgumentException localIllegalArgumentException)
    {
      localEOFException = new EOFException("EOF");
      localEOFException.initCause(localIllegalArgumentException);
      throw localEOFException;
    }
    catch (BufferUnderflowException localBufferUnderflowException)
    {
      EOFException localEOFException = new EOFException("EOF");
      localEOFException.initCause(localBufferUnderflowException);
      throw localEOFException;
    }
  }
  
  public FileChannel position(long paramLong)
    throws IOException
  {
    checkFileSizeLimit(paramLong);
    this.pos = ((int)paramLong);
    return this;
  }
  
  public synchronized FileChannel truncate(long paramLong)
    throws IOException
  {
    if (this.mode == FileChannel.MapMode.READ_ONLY) {
      throw new NonWritableChannelException();
    }
    if (paramLong < size()) {
      setFileLength(paramLong);
    }
    return this;
  }
  
  public synchronized void setFileLength(long paramLong)
    throws IOException
  {
    checkFileSizeLimit(paramLong);
    int i = this.pos;
    unMap();
    for (int j = 0;; j++) {
      try
      {
        this.file.setLength(paramLong);
      }
      catch (IOException localIOException)
      {
        if ((j > 16) || (localIOException.toString().indexOf("user-mapped section open") < 0)) {
          throw localIOException;
        }
        System.gc();
      }
    }
    reMap();
    this.pos = ((int)Math.min(paramLong, i));
  }
  
  public void force(boolean paramBoolean)
    throws IOException
  {
    this.mapped.force();
    this.file.getFD().sync();
  }
  
  public synchronized int write(ByteBuffer paramByteBuffer)
    throws IOException
  {
    int i = paramByteBuffer.remaining();
    if (this.mapped.capacity() < this.pos + i) {
      setFileLength(this.pos + i);
    }
    this.mapped.position(this.pos);
    this.mapped.put(paramByteBuffer);
    this.pos += i;
    return i;
  }
  
  public synchronized FileLock tryLock(long paramLong1, long paramLong2, boolean paramBoolean)
    throws IOException
  {
    return this.file.getChannel().tryLock(paramLong1, paramLong2, paramBoolean);
  }
}
