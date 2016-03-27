package org.h2.store;

import java.io.OutputStream;
import org.h2.tools.CompressTool;

public class FileStoreOutputStream
  extends OutputStream
{
  private FileStore store;
  private final Data page;
  private final String compressionAlgorithm;
  private final CompressTool compress;
  private final byte[] buffer = { 0 };
  
  public FileStoreOutputStream(FileStore paramFileStore, DataHandler paramDataHandler, String paramString)
  {
    this.store = paramFileStore;
    if (paramString != null)
    {
      this.compress = CompressTool.getInstance();
      this.compressionAlgorithm = paramString;
    }
    else
    {
      this.compress = null;
      this.compressionAlgorithm = null;
    }
    this.page = Data.create(paramDataHandler, 16);
  }
  
  public void write(int paramInt)
  {
    this.buffer[0] = ((byte)paramInt);
    write(this.buffer);
  }
  
  public void write(byte[] paramArrayOfByte)
  {
    write(paramArrayOfByte, 0, paramArrayOfByte.length);
  }
  
  public void write(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    if (paramInt2 > 0)
    {
      this.page.reset();
      if (this.compress != null)
      {
        if ((paramInt1 != 0) || (paramInt2 != paramArrayOfByte.length))
        {
          byte[] arrayOfByte = new byte[paramInt2];
          System.arraycopy(paramArrayOfByte, paramInt1, arrayOfByte, 0, paramInt2);
          paramArrayOfByte = arrayOfByte;
          paramInt1 = 0;
        }
        int i = paramInt2;
        paramArrayOfByte = this.compress.compress(paramArrayOfByte, this.compressionAlgorithm);
        paramInt2 = paramArrayOfByte.length;
        this.page.checkCapacity(8 + paramInt2);
        this.page.writeInt(paramInt2);
        this.page.writeInt(i);
        this.page.write(paramArrayOfByte, paramInt1, paramInt2);
      }
      else
      {
        this.page.checkCapacity(4 + paramInt2);
        this.page.writeInt(paramInt2);
        this.page.write(paramArrayOfByte, paramInt1, paramInt2);
      }
      this.page.fillAligned();
      this.store.write(this.page.getBytes(), 0, this.page.length());
    }
  }
  
  public void close()
  {
    if (this.store != null) {
      try
      {
        this.store.close();
      }
      finally
      {
        this.store = null;
      }
    }
  }
}
