package org.h2.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;
import org.h2.engine.SessionInterface;
import org.h2.message.DbException;
import org.h2.message.TraceObject;
import org.h2.util.IOUtils;
import org.h2.util.Task;
import org.h2.value.Value;

public class JdbcClob
  extends TraceObject
  implements NClob
{
  Value value;
  private final JdbcConnection conn;
  
  public JdbcClob(JdbcConnection paramJdbcConnection, Value paramValue, int paramInt)
  {
    setTrace(paramJdbcConnection.getSession().getTrace(), 10, paramInt);
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
      if (this.value.getType() == 16)
      {
        long l = this.value.getPrecision();
        if (l > 0L) {
          return l;
        }
      }
      return IOUtils.copyAndCloseInput(this.value.getReader(), null, Long.MAX_VALUE);
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
  
  public InputStream getAsciiStream()
    throws SQLException
  {
    try
    {
      debugCodeCall("getAsciiStream");
      checkClosed();
      String str = this.value.getString();
      return IOUtils.getInputStreamFromString(str);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public OutputStream setAsciiStream(long paramLong)
    throws SQLException
  {
    throw unsupported("LOB update");
  }
  
  public Reader getCharacterStream()
    throws SQLException
  {
    try
    {
      debugCodeCall("getCharacterStream");
      checkClosed();
      return this.value.getReader();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Writer setCharacterStream(long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCodeCall("setCharacterStream(" + paramLong + ");");
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
          JdbcClob.this.value = localJdbcConnection.createClob(IOUtils.getReader(localPipedInputStream), -1L);
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
      return IOUtils.getBufferedWriter(local2);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getSubString(long paramLong, int paramInt)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getSubString(" + paramLong + ", " + paramInt + ");");
      }
      checkClosed();
      if (paramLong < 1L) {
        throw DbException.getInvalidValueException("pos", Long.valueOf(paramLong));
      }
      if (paramInt < 0) {
        throw DbException.getInvalidValueException("length", Integer.valueOf(paramInt));
      }
      StringWriter localStringWriter = new StringWriter(Math.min(4096, paramInt));
      
      Reader localReader = this.value.getReader();
      try
      {
        IOUtils.skipFully(localReader, paramLong - 1L);
        IOUtils.copyAndCloseInput(localReader, localStringWriter, paramInt);
      }
      finally
      {
        localReader.close();
      }
      return localStringWriter.toString();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int setString(long paramLong, String paramString)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setString(" + paramLong + ", " + quote(paramString) + ");");
      }
      checkClosed();
      if (paramLong != 1L) {
        throw DbException.getInvalidValueException("pos", Long.valueOf(paramLong));
      }
      if (paramString == null) {
        throw DbException.getInvalidValueException("str", paramString);
      }
      this.value = this.conn.createClob(new StringReader(paramString), -1L);
      return paramString.length();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int setString(long paramLong, String paramString, int paramInt1, int paramInt2)
    throws SQLException
  {
    throw unsupported("LOB update");
  }
  
  public long position(String paramString, long paramLong)
    throws SQLException
  {
    throw unsupported("LOB search");
  }
  
  public long position(Clob paramClob, long paramLong)
    throws SQLException
  {
    throw unsupported("LOB search");
  }
  
  public void free()
  {
    debugCodeCall("free");
    this.value = null;
  }
  
  public Reader getCharacterStream(long paramLong1, long paramLong2)
    throws SQLException
  {
    throw unsupported("LOB subset");
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
