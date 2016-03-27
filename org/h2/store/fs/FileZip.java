package org.h2.store.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.h2.util.IOUtils;

class FileZip
  extends FileBase
{
  private static final byte[] SKIP_BUFFER = new byte['Ѐ'];
  private final ZipFile file;
  private final ZipEntry entry;
  private long pos;
  private InputStream in;
  private long inPos;
  private final long length;
  private boolean skipUsingRead;
  
  FileZip(ZipFile paramZipFile, ZipEntry paramZipEntry)
  {
    this.file = paramZipFile;
    this.entry = paramZipEntry;
    this.length = paramZipEntry.getSize();
  }
  
  public long position()
  {
    return this.pos;
  }
  
  public long size()
  {
    return this.length;
  }
  
  public int read(ByteBuffer paramByteBuffer)
    throws IOException
  {
    seek();
    int i = this.in.read(paramByteBuffer.array(), paramByteBuffer.arrayOffset() + paramByteBuffer.position(), paramByteBuffer.remaining());
    if (i > 0)
    {
      paramByteBuffer.position(paramByteBuffer.position() + i);
      this.pos += i;
      this.inPos += i;
    }
    return i;
  }
  
  private void seek()
    throws IOException
  {
    if (this.inPos > this.pos)
    {
      if (this.in != null) {
        this.in.close();
      }
      this.in = null;
    }
    if (this.in == null)
    {
      this.in = this.file.getInputStream(this.entry);
      this.inPos = 0L;
    }
    if (this.inPos < this.pos)
    {
      long l = this.pos - this.inPos;
      if (!this.skipUsingRead) {
        try
        {
          IOUtils.skipFully(this.in, l);
        }
        catch (NullPointerException localNullPointerException)
        {
          this.skipUsingRead = true;
        }
      }
      if (this.skipUsingRead) {
        while (l > 0L)
        {
          int i = (int)Math.min(SKIP_BUFFER.length, l);
          i = this.in.read(SKIP_BUFFER, 0, i);
          l -= i;
        }
      }
      this.inPos = this.pos;
    }
  }
  
  public FileChannel position(long paramLong)
  {
    this.pos = paramLong;
    return this;
  }
  
  public FileChannel truncate(long paramLong)
    throws IOException
  {
    throw new IOException("File is read-only");
  }
  
  public void force(boolean paramBoolean)
    throws IOException
  {}
  
  public int write(ByteBuffer paramByteBuffer)
    throws IOException
  {
    throw new IOException("File is read-only");
  }
  
  public synchronized FileLock tryLock(long paramLong1, long paramLong2, boolean paramBoolean)
    throws IOException
  {
    if (paramBoolean) {
      new FileLock((FileChannel)null, paramLong1, paramLong2, paramBoolean)
      {
        public boolean isValid()
        {
          return true;
        }
        
        public void release()
          throws IOException
        {}
      };
    }
    return null;
  }
  
  protected void implCloseChannel()
    throws IOException
  {
    if (this.in != null)
    {
      this.in.close();
      this.in = null;
    }
    this.file.close();
  }
}
