package org.h2.jdbc;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import org.h2.engine.SessionInterface;
import org.h2.message.DbException;
import org.h2.message.TraceObject;
import org.h2.util.IOUtils;
import org.h2.util.Task;
import org.h2.value.Value;

public class JdbcBlob
  extends TraceObject
  implements Blob
{
  Value value;
  private final JdbcConnection conn;
  
  public JdbcBlob(JdbcConnection paramJdbcConnection, Value paramValue, int paramInt)
  {
    setTrace(paramJdbcConnection.getSession().getTrace(), 9, paramInt);
    this.conn = paramJdbcConnection;
    this.value = paramValue;
  }
  
  public long length()
    throws SQLException
  {
    try
    {
      debugCodeCall("length");
      checkClosed();
      if (this.value.getType() == 15)
      {
        long l = this.value.getPrecision();
        if (l > 0L) {
          return l;
        }
      }
      return IOUtils.copyAndCloseInput(this.value.getInputStream(), null);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void truncate(long paramLong)
    throws SQLException
  {
    throw unsupported("LOB update");
  }
  
  public byte[] getBytes(long paramLong, int paramInt)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getBytes(" + paramLong + ", " + paramInt + ");");
      }
      checkClosed();
      ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
      InputStream localInputStream = this.value.getInputStream();
      try
      {
        IOUtils.skipFully(localInputStream, paramLong - 1L);
        IOUtils.copy(localInputStream, localByteArrayOutputStream, paramInt);
      }
      finally
      {
        localInputStream.close();
      }
      return localByteArrayOutputStream.toByteArray();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int setBytes(long paramLong, byte[] paramArrayOfByte)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setBytes(" + paramLong + ", " + quoteBytes(paramArrayOfByte) + ");");
      }
      checkClosed();
      if (paramLong != 1L) {
        throw DbException.getInvalidValueException("pos", Long.valueOf(paramLong));
      }
      this.value = this.conn.createBlob(new ByteArrayInputStream(paramArrayOfByte), -1L);
      return paramArrayOfByte.length;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int setBytes(long paramLong, byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws SQLException
  {
    throw unsupported("LOB update");
  }
  
  public InputStream getBinaryStream()
    throws SQLException
  {
    try
    {
      debugCodeCall("getBinaryStream");
      checkClosed();
      return this.value.getInputStream();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public OutputStream setBinaryStream(long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setBinaryStream(" + paramLong + ");");
      }
      checkClosed();
      if (paramLong != 1L) {
        throw DbException.getInvalidValueException("pos", Long.valueOf(paramLong));
      }
      if (this.value.getPrecision() != 0L) {
        throw DbException.getInvalidValueException("length", Long.valueOf(this.value.getPrecision()));
      }
      final JdbcConnection localJdbcConnection = this.conn;
      final PipedInputStream localPipedInputStream = new PipedInputStream();
      final Task local1 = new Task()
      {
        public void call()
        {
          JdbcBlob.this.value = localJdbcConnection.createBlob(localPipedInputStream, -1L);
        }
      };
      PipedOutputStream local2 = new PipedOutputStream(localPipedInputStream)
      {
        public void close()
          throws IOException
        {
          super.close();
          try
          {
            local1.get();
          }
          catch (Exception localException)
          {
            throw DbException.convertToIOException(localException);
          }
        }
      };
      local1.execute();
      return new BufferedOutputStream(local2);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public long position(byte[] paramArrayOfByte, long paramLong)
    throws SQLException
  {
    if (isDebugEnabled()) {
      debugCode("position(" + quoteBytes(paramArrayOfByte) + ", " + paramLong + ");");
    }
    throw unsupported("LOB search");
  }
  
  public long position(Blob paramBlob, long paramLong)
    throws SQLException
  {
    if (isDebugEnabled()) {
      debugCode("position(blobPattern, " + paramLong + ");");
    }
    throw unsupported("LOB subset");
  }
  
  public void free()
  {
    debugCodeCall("free");
    this.value = null;
  }
  
  public InputStream getBinaryStream(long paramLong1, long paramLong2)
    throws SQLException
  {
    throw unsupported("LOB update");
  }
  
  private void checkClosed()
  {
    this.conn.checkClosed();
    if (this.value == null) {
      throw DbException.get(90007);
    }
  }
  
  public String toString()
  {
    return getTraceObjectName() + ": " + (this.value == null ? "null" : this.value.getTraceSQL());
  }
}
