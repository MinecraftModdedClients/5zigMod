package org.h2.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.h2.command.CommandInterface;
import org.h2.engine.SessionInterface;
import org.h2.expression.ParameterInterface;
import org.h2.message.DbException;
import org.h2.util.BitField;
import org.h2.util.New;
import org.h2.value.ValueNull;

public class JdbcCallableStatement
  extends JdbcPreparedStatement
  implements CallableStatement
{
  private BitField outParameters;
  private int maxOutParameters;
  private HashMap<String, Integer> namedParameters;
  
  JdbcCallableStatement(JdbcConnection paramJdbcConnection, String paramString, int paramInt1, int paramInt2, int paramInt3)
  {
    super(paramJdbcConnection, paramString, paramInt1, paramInt2, paramInt3, false);
    setTrace(this.session.getTrace(), 0, paramInt1);
  }
  
  public int executeUpdate()
    throws SQLException
  {
    try
    {
      checkClosed();
      if (this.command.isQuery())
      {
        super.executeQuery();
        return 0;
      }
      return super.executeUpdate();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void registerOutParameter(int paramInt1, int paramInt2)
    throws SQLException
  {
    registerOutParameter(paramInt1);
  }
  
  public void registerOutParameter(int paramInt1, int paramInt2, String paramString)
    throws SQLException
  {
    registerOutParameter(paramInt1);
  }
  
  public void registerOutParameter(int paramInt1, int paramInt2, int paramInt3)
    throws SQLException
  {
    registerOutParameter(paramInt1);
  }
  
  public void registerOutParameter(String paramString1, int paramInt, String paramString2)
    throws SQLException
  {
    registerOutParameter(getIndexForName(paramString1), paramInt, paramString2);
  }
  
  public void registerOutParameter(String paramString, int paramInt1, int paramInt2)
    throws SQLException
  {
    registerOutParameter(getIndexForName(paramString), paramInt1, paramInt2);
  }
  
  public void registerOutParameter(String paramString, int paramInt)
    throws SQLException
  {
    registerOutParameter(getIndexForName(paramString), paramInt);
  }
  
  public boolean wasNull()
    throws SQLException
  {
    return getOpenResultSet().wasNull();
  }
  
  public URL getURL(int paramInt)
    throws SQLException
  {
    throw unsupported("url");
  }
  
  public String getString(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getString(paramInt);
  }
  
  public boolean getBoolean(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getBoolean(paramInt);
  }
  
  public byte getByte(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getByte(paramInt);
  }
  
  public short getShort(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getShort(paramInt);
  }
  
  public int getInt(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getInt(paramInt);
  }
  
  public long getLong(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getLong(paramInt);
  }
  
  public float getFloat(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getFloat(paramInt);
  }
  
  public double getDouble(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getDouble(paramInt);
  }
  
  /**
   * @deprecated
   */
  public BigDecimal getBigDecimal(int paramInt1, int paramInt2)
    throws SQLException
  {
    checkRegistered(paramInt1);
    return getOpenResultSet().getBigDecimal(paramInt1, paramInt2);
  }
  
  public byte[] getBytes(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getBytes(paramInt);
  }
  
  public Date getDate(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getDate(paramInt);
  }
  
  public Time getTime(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getTime(paramInt);
  }
  
  public Timestamp getTimestamp(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getTimestamp(paramInt);
  }
  
  public Object getObject(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getObject(paramInt);
  }
  
  public BigDecimal getBigDecimal(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getBigDecimal(paramInt);
  }
  
  public Object getObject(int paramInt, Map<String, Class<?>> paramMap)
    throws SQLException
  {
    throw unsupported("map");
  }
  
  public Ref getRef(int paramInt)
    throws SQLException
  {
    throw unsupported("ref");
  }
  
  public Blob getBlob(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getBlob(paramInt);
  }
  
  public Clob getClob(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getClob(paramInt);
  }
  
  public Array getArray(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getArray(paramInt);
  }
  
  public Date getDate(int paramInt, Calendar paramCalendar)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getDate(paramInt, paramCalendar);
  }
  
  public Time getTime(int paramInt, Calendar paramCalendar)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getTime(paramInt, paramCalendar);
  }
  
  public Timestamp getTimestamp(int paramInt, Calendar paramCalendar)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getTimestamp(paramInt, paramCalendar);
  }
  
  public URL getURL(String paramString)
    throws SQLException
  {
    throw unsupported("url");
  }
  
  public Timestamp getTimestamp(String paramString, Calendar paramCalendar)
    throws SQLException
  {
    return getTimestamp(getIndexForName(paramString), paramCalendar);
  }
  
  public Time getTime(String paramString, Calendar paramCalendar)
    throws SQLException
  {
    return getTime(getIndexForName(paramString), paramCalendar);
  }
  
  public Date getDate(String paramString, Calendar paramCalendar)
    throws SQLException
  {
    return getDate(getIndexForName(paramString), paramCalendar);
  }
  
  public Array getArray(String paramString)
    throws SQLException
  {
    return getArray(getIndexForName(paramString));
  }
  
  public Clob getClob(String paramString)
    throws SQLException
  {
    return getClob(getIndexForName(paramString));
  }
  
  public Blob getBlob(String paramString)
    throws SQLException
  {
    return getBlob(getIndexForName(paramString));
  }
  
  public Ref getRef(String paramString)
    throws SQLException
  {
    throw unsupported("ref");
  }
  
  public Object getObject(String paramString, Map<String, Class<?>> paramMap)
    throws SQLException
  {
    throw unsupported("map");
  }
  
  public BigDecimal getBigDecimal(String paramString)
    throws SQLException
  {
    return getBigDecimal(getIndexForName(paramString));
  }
  
  public Object getObject(String paramString)
    throws SQLException
  {
    return getObject(getIndexForName(paramString));
  }
  
  public Timestamp getTimestamp(String paramString)
    throws SQLException
  {
    return getTimestamp(getIndexForName(paramString));
  }
  
  public Time getTime(String paramString)
    throws SQLException
  {
    return getTime(getIndexForName(paramString));
  }
  
  public Date getDate(String paramString)
    throws SQLException
  {
    return getDate(getIndexForName(paramString));
  }
  
  public byte[] getBytes(String paramString)
    throws SQLException
  {
    return getBytes(getIndexForName(paramString));
  }
  
  public double getDouble(String paramString)
    throws SQLException
  {
    return getDouble(getIndexForName(paramString));
  }
  
  public float getFloat(String paramString)
    throws SQLException
  {
    return getFloat(getIndexForName(paramString));
  }
  
  public long getLong(String paramString)
    throws SQLException
  {
    return getLong(getIndexForName(paramString));
  }
  
  public int getInt(String paramString)
    throws SQLException
  {
    return getInt(getIndexForName(paramString));
  }
  
  public short getShort(String paramString)
    throws SQLException
  {
    return getShort(getIndexForName(paramString));
  }
  
  public byte getByte(String paramString)
    throws SQLException
  {
    return getByte(getIndexForName(paramString));
  }
  
  public boolean getBoolean(String paramString)
    throws SQLException
  {
    return getBoolean(getIndexForName(paramString));
  }
  
  public String getString(String paramString)
    throws SQLException
  {
    return getString(getIndexForName(paramString));
  }
  
  public RowId getRowId(int paramInt)
    throws SQLException
  {
    throw unsupported("rowId");
  }
  
  public RowId getRowId(String paramString)
    throws SQLException
  {
    throw unsupported("rowId");
  }
  
  public NClob getNClob(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getNClob(paramInt);
  }
  
  public NClob getNClob(String paramString)
    throws SQLException
  {
    return getNClob(getIndexForName(paramString));
  }
  
  public SQLXML getSQLXML(int paramInt)
    throws SQLException
  {
    throw unsupported("SQLXML");
  }
  
  public SQLXML getSQLXML(String paramString)
    throws SQLException
  {
    throw unsupported("SQLXML");
  }
  
  public String getNString(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getNString(paramInt);
  }
  
  public String getNString(String paramString)
    throws SQLException
  {
    return getNString(getIndexForName(paramString));
  }
  
  public Reader getNCharacterStream(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getNCharacterStream(paramInt);
  }
  
  public Reader getNCharacterStream(String paramString)
    throws SQLException
  {
    return getNCharacterStream(getIndexForName(paramString));
  }
  
  public Reader getCharacterStream(int paramInt)
    throws SQLException
  {
    checkRegistered(paramInt);
    return getOpenResultSet().getCharacterStream(paramInt);
  }
  
  public Reader getCharacterStream(String paramString)
    throws SQLException
  {
    return getCharacterStream(getIndexForName(paramString));
  }
  
  public void setNull(String paramString1, int paramInt, String paramString2)
    throws SQLException
  {
    setNull(getIndexForName(paramString1), paramInt, paramString2);
  }
  
  public void setNull(String paramString, int paramInt)
    throws SQLException
  {
    setNull(getIndexForName(paramString), paramInt);
  }
  
  public void setTimestamp(String paramString, Timestamp paramTimestamp, Calendar paramCalendar)
    throws SQLException
  {
    setTimestamp(getIndexForName(paramString), paramTimestamp, paramCalendar);
  }
  
  public void setTime(String paramString, Time paramTime, Calendar paramCalendar)
    throws SQLException
  {
    setTime(getIndexForName(paramString), paramTime, paramCalendar);
  }
  
  public void setDate(String paramString, Date paramDate, Calendar paramCalendar)
    throws SQLException
  {
    setDate(getIndexForName(paramString), paramDate, paramCalendar);
  }
  
  public void setCharacterStream(String paramString, Reader paramReader, int paramInt)
    throws SQLException
  {
    setCharacterStream(getIndexForName(paramString), paramReader, paramInt);
  }
  
  public void setObject(String paramString, Object paramObject)
    throws SQLException
  {
    setObject(getIndexForName(paramString), paramObject);
  }
  
  public void setObject(String paramString, Object paramObject, int paramInt)
    throws SQLException
  {
    setObject(getIndexForName(paramString), paramObject, paramInt);
  }
  
  public void setObject(String paramString, Object paramObject, int paramInt1, int paramInt2)
    throws SQLException
  {
    setObject(getIndexForName(paramString), paramObject, paramInt1, paramInt2);
  }
  
  public void setBinaryStream(String paramString, InputStream paramInputStream, int paramInt)
    throws SQLException
  {
    setBinaryStream(getIndexForName(paramString), paramInputStream, paramInt);
  }
  
  public void setAsciiStream(String paramString, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    setAsciiStream(getIndexForName(paramString), paramInputStream, paramLong);
  }
  
  public void setTimestamp(String paramString, Timestamp paramTimestamp)
    throws SQLException
  {
    setTimestamp(getIndexForName(paramString), paramTimestamp);
  }
  
  public void setTime(String paramString, Time paramTime)
    throws SQLException
  {
    setTime(getIndexForName(paramString), paramTime);
  }
  
  public void setDate(String paramString, Date paramDate)
    throws SQLException
  {
    setDate(getIndexForName(paramString), paramDate);
  }
  
  public void setBytes(String paramString, byte[] paramArrayOfByte)
    throws SQLException
  {
    setBytes(getIndexForName(paramString), paramArrayOfByte);
  }
  
  public void setString(String paramString1, String paramString2)
    throws SQLException
  {
    setString(getIndexForName(paramString1), paramString2);
  }
  
  public void setBigDecimal(String paramString, BigDecimal paramBigDecimal)
    throws SQLException
  {
    setBigDecimal(getIndexForName(paramString), paramBigDecimal);
  }
  
  public void setDouble(String paramString, double paramDouble)
    throws SQLException
  {
    setDouble(getIndexForName(paramString), paramDouble);
  }
  
  public void setFloat(String paramString, float paramFloat)
    throws SQLException
  {
    setFloat(getIndexForName(paramString), paramFloat);
  }
  
  public void setLong(String paramString, long paramLong)
    throws SQLException
  {
    setLong(getIndexForName(paramString), paramLong);
  }
  
  public void setInt(String paramString, int paramInt)
    throws SQLException
  {
    setInt(getIndexForName(paramString), paramInt);
  }
  
  public void setShort(String paramString, short paramShort)
    throws SQLException
  {
    setShort(getIndexForName(paramString), paramShort);
  }
  
  public void setByte(String paramString, byte paramByte)
    throws SQLException
  {
    setByte(getIndexForName(paramString), paramByte);
  }
  
  public void setBoolean(String paramString, boolean paramBoolean)
    throws SQLException
  {
    setBoolean(getIndexForName(paramString), paramBoolean);
  }
  
  public void setURL(String paramString, URL paramURL)
    throws SQLException
  {
    throw unsupported("url");
  }
  
  public void setRowId(String paramString, RowId paramRowId)
    throws SQLException
  {
    throw unsupported("rowId");
  }
  
  public void setNString(String paramString1, String paramString2)
    throws SQLException
  {
    setNString(getIndexForName(paramString1), paramString2);
  }
  
  public void setNCharacterStream(String paramString, Reader paramReader, long paramLong)
    throws SQLException
  {
    setNCharacterStream(getIndexForName(paramString), paramReader, paramLong);
  }
  
  public void setNClob(String paramString, NClob paramNClob)
    throws SQLException
  {
    setNClob(getIndexForName(paramString), paramNClob);
  }
  
  public void setClob(String paramString, Reader paramReader, long paramLong)
    throws SQLException
  {
    setClob(getIndexForName(paramString), paramReader, paramLong);
  }
  
  public void setBlob(String paramString, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    setBlob(getIndexForName(paramString), paramInputStream, paramLong);
  }
  
  public void setNClob(String paramString, Reader paramReader, long paramLong)
    throws SQLException
  {
    setNClob(getIndexForName(paramString), paramReader, paramLong);
  }
  
  public void setBlob(String paramString, Blob paramBlob)
    throws SQLException
  {
    setBlob(getIndexForName(paramString), paramBlob);
  }
  
  public void setClob(String paramString, Clob paramClob)
    throws SQLException
  {
    setClob(getIndexForName(paramString), paramClob);
  }
  
  public void setAsciiStream(String paramString, InputStream paramInputStream)
    throws SQLException
  {
    setAsciiStream(getIndexForName(paramString), paramInputStream);
  }
  
  public void setAsciiStream(String paramString, InputStream paramInputStream, int paramInt)
    throws SQLException
  {
    setAsciiStream(getIndexForName(paramString), paramInputStream, paramInt);
  }
  
  public void setBinaryStream(String paramString, InputStream paramInputStream)
    throws SQLException
  {
    setBinaryStream(getIndexForName(paramString), paramInputStream);
  }
  
  public void setBinaryStream(String paramString, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    setBinaryStream(getIndexForName(paramString), paramInputStream, paramLong);
  }
  
  public void setBlob(String paramString, InputStream paramInputStream)
    throws SQLException
  {
    setBlob(getIndexForName(paramString), paramInputStream);
  }
  
  public void setCharacterStream(String paramString, Reader paramReader)
    throws SQLException
  {
    setCharacterStream(getIndexForName(paramString), paramReader);
  }
  
  public void setCharacterStream(String paramString, Reader paramReader, long paramLong)
    throws SQLException
  {
    setCharacterStream(getIndexForName(paramString), paramReader, paramLong);
  }
  
  public void setClob(String paramString, Reader paramReader)
    throws SQLException
  {
    setClob(getIndexForName(paramString), paramReader);
  }
  
  public void setNCharacterStream(String paramString, Reader paramReader)
    throws SQLException
  {
    setNCharacterStream(getIndexForName(paramString), paramReader);
  }
  
  public void setNClob(String paramString, Reader paramReader)
    throws SQLException
  {
    setNClob(getIndexForName(paramString), paramReader);
  }
  
  public void setSQLXML(String paramString, SQLXML paramSQLXML)
    throws SQLException
  {
    throw unsupported("SQLXML");
  }
  
  private ResultSetMetaData getCheckedMetaData()
    throws SQLException
  {
    ResultSetMetaData localResultSetMetaData = getMetaData();
    if (localResultSetMetaData == null) {
      throw DbException.getUnsupportedException("Supported only for calling stored procedures");
    }
    return localResultSetMetaData;
  }
  
  private void checkIndexBounds(int paramInt)
  {
    checkClosed();
    if ((paramInt < 1) || (paramInt > this.maxOutParameters)) {
      throw DbException.getInvalidValueException("parameterIndex", Integer.valueOf(paramInt));
    }
  }
  
  private void registerOutParameter(int paramInt)
    throws SQLException
  {
    try
    {
      checkClosed();
      if (this.outParameters == null)
      {
        this.maxOutParameters = Math.min(getParameterMetaData().getParameterCount(), getCheckedMetaData().getColumnCount());
        
        this.outParameters = new BitField();
      }
      checkIndexBounds(paramInt);
      ParameterInterface localParameterInterface = (ParameterInterface)this.command.getParameters().get(--paramInt);
      if (!localParameterInterface.isValueSet()) {
        localParameterInterface.setValue(ValueNull.INSTANCE, false);
      }
      this.outParameters.set(paramInt);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  private void checkRegistered(int paramInt)
    throws SQLException
  {
    try
    {
      checkIndexBounds(paramInt);
      if (!this.outParameters.get(paramInt - 1)) {
        throw DbException.getInvalidValueException("parameterIndex", Integer.valueOf(paramInt));
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  private int getIndexForName(String paramString)
    throws SQLException
  {
    try
    {
      checkClosed();
      if (this.namedParameters == null)
      {
        localObject = getCheckedMetaData();
        int i = ((ResultSetMetaData)localObject).getColumnCount();
        HashMap localHashMap = New.hashMap(i);
        for (int j = 1; j <= i; j++) {
          localHashMap.put(((ResultSetMetaData)localObject).getColumnLabel(j), Integer.valueOf(j));
        }
        this.namedParameters = localHashMap;
      }
      Object localObject = (Integer)this.namedParameters.get(paramString);
      if (localObject == null) {
        throw DbException.getInvalidValueException("parameterName", paramString);
      }
      return ((Integer)localObject).intValue();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  private JdbcResultSet getOpenResultSet()
    throws SQLException
  {
    try
    {
      checkClosed();
      if (this.resultSet == null) {
        throw DbException.get(2000);
      }
      if (this.resultSet.isBeforeFirst()) {
        this.resultSet.next();
      }
      return this.resultSet;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
}
