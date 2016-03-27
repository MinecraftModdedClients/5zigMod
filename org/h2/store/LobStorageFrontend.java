package org.h2.store;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import org.h2.value.Value;
import org.h2.value.ValueLobDb;

public class LobStorageFrontend
  implements LobStorageInterface
{
  public static final int TABLE_ID_SESSION_VARIABLE = -1;
  public static final int TABLE_TEMP = -2;
  public static final int TABLE_RESULT = -3;
  private final DataHandler handler;
  
  public LobStorageFrontend(DataHandler paramDataHandler)
  {
    this.handler = paramDataHandler;
  }
  
  public void removeLob(ValueLobDb paramValueLobDb) {}
  
  public InputStream getInputStream(ValueLobDb paramValueLobDb, byte[] paramArrayOfByte, long paramLong)
    throws IOException
  {
    if (paramLong < 0L) {
      paramLong = Long.MAX_VALUE;
    }
    return new BufferedInputStream(new LobStorageRemoteInputStream(this.handler, paramValueLobDb, paramArrayOfByte, paramLong));
  }
  
  public boolean isReadOnly()
  {
    return false;
  }
  
  public ValueLobDb copyLob(ValueLobDb paramValueLobDb, int paramInt, long paramLong)
  {
    throw new UnsupportedOperationException();
  }
  
  public void setTable(ValueLobDb paramValueLobDb, int paramInt)
  {
    throw new UnsupportedOperationException();
  }
  
  public void removeAllForTable(int paramInt)
  {
    throw new UnsupportedOperationException();
  }
  
  public Value createBlob(InputStream paramInputStream, long paramLong)
  {
    return ValueLobDb.createTempBlob(paramInputStream, paramLong, this.handler);
  }
  
  public Value createClob(Reader paramReader, long paramLong)
  {
    return ValueLobDb.createTempClob(paramReader, paramLong, this.handler);
  }
  
  public void init() {}
}
