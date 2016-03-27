package org.h2.store;

import java.io.IOException;
import java.io.InputStream;
import org.h2.message.DbException;
import org.h2.value.ValueLobDb;

class LobStorageRemoteInputStream
  extends InputStream
{
  private final DataHandler handler;
  private final long lob;
  private final byte[] hmac;
  private long pos;
  private long remainingBytes;
  
  public LobStorageRemoteInputStream(DataHandler paramDataHandler, ValueLobDb paramValueLobDb, byte[] paramArrayOfByte, long paramLong)
  {
    this.handler = paramDataHandler;
    this.lob = paramValueLobDb.getLobId();
    this.hmac = paramArrayOfByte;
    this.remainingBytes = paramLong;
  }
  
  public int read()
    throws IOException
  {
    byte[] arrayOfByte = new byte[1];
    int i = read(arrayOfByte, 0, 1);
    return i < 0 ? i : arrayOfByte[0] & 0xFF;
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
    paramInt2 = (int)Math.min(paramInt2, this.remainingBytes);
    if (paramInt2 == 0) {
      return -1;
    }
    try
    {
      paramInt2 = this.handler.readLob(this.lob, this.hmac, this.pos, paramArrayOfByte, paramInt1, paramInt2);
    }
    catch (DbException localDbException)
    {
      throw DbException.convertToIOException(localDbException);
    }
    this.remainingBytes -= paramInt2;
    if (paramInt2 == 0) {
      return -1;
    }
    this.pos += paramInt2;
    return paramInt2;
  }
  
  public long skip(long paramLong)
  {
    this.remainingBytes -= paramLong;
    this.pos += paramLong;
    return paramLong;
  }
}
