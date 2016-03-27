package org.h2.store;

import java.io.IOException;
import java.io.InputStream;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils;
import org.h2.tools.CompressTool;

public class FileStoreInputStream
  extends InputStream
{
  private FileStore store;
  private final Data page;
  private int remainingInBuffer;
  private final CompressTool compress;
  private boolean endOfFile;
  private final boolean alwaysClose;
  
  public FileStoreInputStream(FileStore paramFileStore, DataHandler paramDataHandler, boolean paramBoolean1, boolean paramBoolean2)
  {
    this.store = paramFileStore;
    this.alwaysClose = paramBoolean2;
    if (paramBoolean1) {
      this.compress = CompressTool.getInstance();
    } else {
      this.compress = null;
    }
    this.page = Data.create(paramDataHandler, 16);
    try
    {
      if (paramFileStore.length() <= 48L) {
        close();
      } else {
        fillBuffer();
      }
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, paramFileStore.name);
    }
  }
  
  public int available()
  {
    return this.remainingInBuffer <= 0 ? 0 : this.remainingInBuffer;
  }
  
  public int read(byte[] paramArrayOfByte)
    throws IOException
  {
    return read(paramArrayOfByte, 0, paramArrayOfByte.length);
  }
  
  public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    if (paramInt2 == 0) {
      return 0;
    }
    int i = 0;
    while (paramInt2 > 0)
    {
      int j = readBlock(paramArrayOfByte, paramInt1, paramInt2);
      if (j < 0) {
        break;
      }
      i += j;
      paramInt1 += j;
      paramInt2 -= j;
    }
    return i == 0 ? -1 : i;
  }
  
  private int readBlock(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    fillBuffer();
    if (this.endOfFile) {
      return -1;
    }
    int i = Math.min(this.remainingInBuffer, paramInt2);
    this.page.read(paramArrayOfByte, paramInt1, i);
    this.remainingInBuffer -= i;
    return i;
  }
  
  private void fillBuffer()
    throws IOException
  {
    if ((this.remainingInBuffer > 0) || (this.endOfFile)) {
      return;
    }
    this.page.reset();
    this.store.openFile();
    if (this.store.length() == this.store.getFilePointer())
    {
      close();
      return;
    }
    this.store.readFully(this.page.getBytes(), 0, 16);
    this.page.reset();
    this.remainingInBuffer = this.page.readInt();
    if (this.remainingInBuffer < 0)
    {
      close();
      return;
    }
    this.page.checkCapacity(this.remainingInBuffer);
    if (this.compress != null)
    {
      this.page.checkCapacity(4);
      this.page.readInt();
    }
    this.page.setPos(this.page.length() + this.remainingInBuffer);
    this.page.fillAligned();
    int i = this.page.length() - 16;
    this.page.reset();
    this.page.readInt();
    this.store.readFully(this.page.getBytes(), 16, i);
    this.page.reset();
    this.page.readInt();
    if (this.compress != null)
    {
      int j = this.page.readInt();
      byte[] arrayOfByte = DataUtils.newBytes(this.remainingInBuffer);
      this.page.read(arrayOfByte, 0, this.remainingInBuffer);
      this.page.reset();
      this.page.checkCapacity(j);
      CompressTool.expand(arrayOfByte, this.page.getBytes(), 0);
      this.remainingInBuffer = j;
    }
    if (this.alwaysClose) {
      this.store.closeFile();
    }
  }
  
  public void close()
  {
    if (this.store != null) {
      try
      {
        this.store.close();
        this.endOfFile = true;
      }
      finally
      {
        this.store = null;
      }
    }
  }
  
  protected void finalize()
  {
    close();
  }
  
  public int read()
    throws IOException
  {
    fillBuffer();
    if (this.endOfFile) {
      return -1;
    }
    int i = this.page.readByte() & 0xFF;
    this.remainingInBuffer -= 1;
    return i;
  }
}
