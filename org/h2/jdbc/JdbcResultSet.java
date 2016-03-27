package org.h2.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.h2.engine.SessionInterface;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.message.TraceObject;
import org.h2.result.ResultInterface;
import org.h2.result.UpdatableRow;
import org.h2.util.DateTimeUtils;
import org.h2.util.IOUtils;
import org.h2.util.New;
import org.h2.util.StringUtils;
import org.h2.value.CompareMode;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueByte;
import org.h2.value.ValueBytes;
import org.h2.value.ValueDate;
import org.h2.value.ValueDecimal;
import org.h2.value.ValueDouble;
import org.h2.value.ValueFloat;
import org.h2.value.ValueInt;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;
import org.h2.value.ValueShort;
import org.h2.value.ValueString;
import org.h2.value.ValueTime;
import org.h2.value.ValueTimestamp;

public class JdbcResultSet
  extends TraceObject
  implements ResultSet
{
  private final boolean closeStatement;
  private final boolean scrollable;
  private final boolean updatable;
  private ResultInterface result;
  private JdbcConnection conn;
  private JdbcStatement stat;
  private int columnCount;
  private boolean wasNull;
  private Value[] insertRow;
  private Value[] updateRow;
  private HashMap<String, Integer> columnLabelMap;
  private HashMap<Integer, Value[]> patchedRows;
  private JdbcPreparedStatement preparedStatement;
  
  JdbcResultSet(JdbcConnection paramJdbcConnection, JdbcStatement paramJdbcStatement, ResultInterface paramResultInterface, int paramInt, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3)
  {
    setTrace(paramJdbcConnection.getSession().getTrace(), 4, paramInt);
    this.conn = paramJdbcConnection;
    this.stat = paramJdbcStatement;
    this.result = paramResultInterface;
    this.columnCount = paramResultInterface.getVisibleColumnCount();
    this.closeStatement = paramBoolean1;
    this.scrollable = paramBoolean2;
    this.updatable = paramBoolean3;
  }
  
  JdbcResultSet(JdbcConnection paramJdbcConnection, JdbcPreparedStatement paramJdbcPreparedStatement, ResultInterface paramResultInterface, int paramInt, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3, HashMap<String, Integer> paramHashMap)
  {
    this(paramJdbcConnection, paramJdbcPreparedStatement, paramResultInterface, paramInt, paramBoolean1, paramBoolean2, paramBoolean3);
    
    this.columnLabelMap = paramHashMap;
    this.preparedStatement = paramJdbcPreparedStatement;
  }
  
  public boolean next()
    throws SQLException
  {
    try
    {
      debugCodeCall("next");
      checkClosed();
      return nextRow();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSetMetaData getMetaData()
    throws SQLException
  {
    try
    {
      int i = getNextId(5);
      if (isDebugEnabled()) {
        debugCodeAssign("ResultSetMetaData", 5, i, "getMetaData()");
      }
      checkClosed();
      String str = this.conn.getCatalog();
      return new JdbcResultSetMetaData(this, null, this.result, str, this.conn.getSession().getTrace(), i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean wasNull()
    throws SQLException
  {
    try
    {
      debugCodeCall("wasNull");
      checkClosed();
      return this.wasNull;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int findColumn(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("findColumn", paramString);
      return getColumnIndex(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void close()
    throws SQLException
  {
    try
    {
      debugCodeCall("close");
      closeInternal();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  void closeInternal()
    throws SQLException
  {
    if (this.result != null) {
      try
      {
        this.result.close();
        if ((this.closeStatement) && (this.stat != null)) {
          this.stat.close();
        }
      }
      finally
      {
        this.columnCount = 0;
        this.result = null;
        this.stat = null;
        this.conn = null;
        this.insertRow = null;
        this.updateRow = null;
      }
    }
  }
  
  public Statement getStatement()
    throws SQLException
  {
    try
    {
      debugCodeCall("getStatement");
      checkClosed();
      if (this.closeStatement) {
        return null;
      }
      return this.stat;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public SQLWarning getWarnings()
    throws SQLException
  {
    try
    {
      debugCodeCall("getWarnings");
      checkClosed();
      return null;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void clearWarnings()
    throws SQLException
  {
    try
    {
      debugCodeCall("clearWarnings");
      checkClosed();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getString(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getString", paramInt);
      return get(paramInt).getString();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getString(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getString", paramString);
      return get(paramString).getString();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getInt(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getInt", paramInt);
      return get(paramInt).getInt();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getInt(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getInt", paramString);
      return get(paramString).getInt();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public BigDecimal getBigDecimal(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getBigDecimal", paramInt);
      return get(paramInt).getBigDecimal();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Date getDate(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getDate", paramInt);
      return get(paramInt).getDate();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Time getTime(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getTime", paramInt);
      return get(paramInt).getTime();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Timestamp getTimestamp(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getTimestamp", paramInt);
      return get(paramInt).getTimestamp();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public BigDecimal getBigDecimal(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getBigDecimal", paramString);
      return get(paramString).getBigDecimal();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Date getDate(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getDate", paramString);
      return get(paramString).getDate();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Time getTime(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getTime", paramString);
      return get(paramString).getTime();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Timestamp getTimestamp(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getTimestamp", paramString);
      return get(paramString).getTimestamp();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Object getObject(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getObject", paramInt);
      Value localValue = get(paramInt);
      return this.conn.convertToDefaultObject(localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Object getObject(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getObject", paramString);
      Value localValue = get(paramString);
      return this.conn.convertToDefaultObject(localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean getBoolean(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getBoolean", paramInt);
      Boolean localBoolean = get(paramInt).getBoolean();
      return localBoolean == null ? false : localBoolean.booleanValue();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean getBoolean(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getBoolean", paramString);
      Boolean localBoolean = get(paramString).getBoolean();
      return localBoolean == null ? false : localBoolean.booleanValue();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public byte getByte(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getByte", paramInt);
      return get(paramInt).getByte();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public byte getByte(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getByte", paramString);
      return get(paramString).getByte();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public short getShort(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getShort", paramInt);
      return get(paramInt).getShort();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public short getShort(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getShort", paramString);
      return get(paramString).getShort();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public long getLong(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getLong", paramInt);
      return get(paramInt).getLong();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public long getLong(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getLong", paramString);
      return get(paramString).getLong();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public float getFloat(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getFloat", paramInt);
      return get(paramInt).getFloat();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public float getFloat(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getFloat", paramString);
      return get(paramString).getFloat();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public double getDouble(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getDouble", paramInt);
      return get(paramInt).getDouble();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public double getDouble(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getDouble", paramString);
      return get(paramString).getDouble();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  /**
   * @deprecated
   */
  public BigDecimal getBigDecimal(String paramString, int paramInt)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getBigDecimal(" + StringUtils.quoteJavaString(paramString) + ", " + paramInt + ");");
      }
      if (paramInt < 0) {
        throw DbException.getInvalidValueException("scale", Integer.valueOf(paramInt));
      }
      BigDecimal localBigDecimal = get(paramString).getBigDecimal();
      return localBigDecimal == null ? null : ValueDecimal.setScale(localBigDecimal, paramInt);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  /**
   * @deprecated
   */
  public BigDecimal getBigDecimal(int paramInt1, int paramInt2)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getBigDecimal(" + paramInt1 + ", " + paramInt2 + ");");
      }
      if (paramInt2 < 0) {
        throw DbException.getInvalidValueException("scale", Integer.valueOf(paramInt2));
      }
      BigDecimal localBigDecimal = get(paramInt1).getBigDecimal();
      return localBigDecimal == null ? null : ValueDecimal.setScale(localBigDecimal, paramInt2);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  /**
   * @deprecated
   */
  public InputStream getUnicodeStream(int paramInt)
    throws SQLException
  {
    throw unsupported("unicodeStream");
  }
  
  /**
   * @deprecated
   */
  public InputStream getUnicodeStream(String paramString)
    throws SQLException
  {
    throw unsupported("unicodeStream");
  }
  
  public Object getObject(int paramInt, Map<String, Class<?>> paramMap)
    throws SQLException
  {
    throw unsupported("map");
  }
  
  public Object getObject(String paramString, Map<String, Class<?>> paramMap)
    throws SQLException
  {
    throw unsupported("map");
  }
  
  public Ref getRef(int paramInt)
    throws SQLException
  {
    throw unsupported("ref");
  }
  
  public Ref getRef(String paramString)
    throws SQLException
  {
    throw unsupported("ref");
  }
  
  public Date getDate(int paramInt, Calendar paramCalendar)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getDate(" + paramInt + ", calendar)");
      }
      return DateTimeUtils.convertDate(get(paramInt), paramCalendar);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Date getDate(String paramString, Calendar paramCalendar)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getDate(" + StringUtils.quoteJavaString(paramString) + ", calendar)");
      }
      return DateTimeUtils.convertDate(get(paramString), paramCalendar);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Time getTime(int paramInt, Calendar paramCalendar)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getTime(" + paramInt + ", calendar)");
      }
      return DateTimeUtils.convertTime(get(paramInt), paramCalendar);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Time getTime(String paramString, Calendar paramCalendar)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getTime(" + StringUtils.quoteJavaString(paramString) + ", calendar)");
      }
      return DateTimeUtils.convertTime(get(paramString), paramCalendar);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Timestamp getTimestamp(int paramInt, Calendar paramCalendar)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getTimestamp(" + paramInt + ", calendar)");
      }
      Value localValue = get(paramInt);
      return DateTimeUtils.convertTimestamp(localValue, paramCalendar);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Timestamp getTimestamp(String paramString, Calendar paramCalendar)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getTimestamp(" + StringUtils.quoteJavaString(paramString) + ", calendar)");
      }
      Value localValue = get(paramString);
      return DateTimeUtils.convertTimestamp(localValue, paramCalendar);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Blob getBlob(int paramInt)
    throws SQLException
  {
    try
    {
      int i = getNextId(9);
      debugCodeAssign("Blob", 9, i, "getBlob(" + paramInt + ")");
      
      Value localValue = get(paramInt);
      return localValue == ValueNull.INSTANCE ? null : new JdbcBlob(this.conn, localValue, i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Blob getBlob(String paramString)
    throws SQLException
  {
    try
    {
      int i = getNextId(9);
      debugCodeAssign("Blob", 9, i, "getBlob(" + quote(paramString) + ")");
      
      Value localValue = get(paramString);
      return localValue == ValueNull.INSTANCE ? null : new JdbcBlob(this.conn, localValue, i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public byte[] getBytes(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getBytes", paramInt);
      return get(paramInt).getBytes();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public byte[] getBytes(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getBytes", paramString);
      return get(paramString).getBytes();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public InputStream getBinaryStream(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getBinaryStream", paramInt);
      return get(paramInt).getInputStream();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public InputStream getBinaryStream(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getBinaryStream", paramString);
      return get(paramString).getInputStream();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Clob getClob(int paramInt)
    throws SQLException
  {
    try
    {
      int i = getNextId(10);
      debugCodeAssign("Clob", 10, i, "getClob(" + paramInt + ")");
      Value localValue = get(paramInt);
      return localValue == ValueNull.INSTANCE ? null : new JdbcClob(this.conn, localValue, i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Clob getClob(String paramString)
    throws SQLException
  {
    try
    {
      int i = getNextId(10);
      debugCodeAssign("Clob", 10, i, "getClob(" + quote(paramString) + ")");
      
      Value localValue = get(paramString);
      return localValue == ValueNull.INSTANCE ? null : new JdbcClob(this.conn, localValue, i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Array getArray(int paramInt)
    throws SQLException
  {
    try
    {
      int i = getNextId(16);
      debugCodeAssign("Clob", 16, i, "getArray(" + paramInt + ")");
      Value localValue = get(paramInt);
      return localValue == ValueNull.INSTANCE ? null : new JdbcArray(this.conn, localValue, i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Array getArray(String paramString)
    throws SQLException
  {
    try
    {
      int i = getNextId(16);
      debugCodeAssign("Clob", 16, i, "getArray(" + quote(paramString) + ")");
      
      Value localValue = get(paramString);
      return localValue == ValueNull.INSTANCE ? null : new JdbcArray(this.conn, localValue, i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public InputStream getAsciiStream(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getAsciiStream", paramInt);
      String str = get(paramInt).getString();
      return str == null ? null : IOUtils.getInputStreamFromString(str);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public InputStream getAsciiStream(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getAsciiStream", paramString);
      String str = get(paramString).getString();
      return IOUtils.getInputStreamFromString(str);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Reader getCharacterStream(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getCharacterStream", paramInt);
      return get(paramInt).getReader();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Reader getCharacterStream(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getCharacterStream", paramString);
      return get(paramString).getReader();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public URL getURL(int paramInt)
    throws SQLException
  {
    throw unsupported("url");
  }
  
  public URL getURL(String paramString)
    throws SQLException
  {
    throw unsupported("url");
  }
  
  public void updateNull(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("updateNull", paramInt);
      update(paramInt, ValueNull.INSTANCE);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateNull(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("updateNull", paramString);
      update(paramString, ValueNull.INSTANCE);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateBoolean(int paramInt, boolean paramBoolean)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateBoolean(" + paramInt + ", " + paramBoolean + ");");
      }
      update(paramInt, ValueBoolean.get(paramBoolean));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateBoolean(String paramString, boolean paramBoolean)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateBoolean(" + quote(paramString) + ", " + paramBoolean + ");");
      }
      update(paramString, ValueBoolean.get(paramBoolean));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateByte(int paramInt, byte paramByte)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateByte(" + paramInt + ", " + paramByte + ");");
      }
      update(paramInt, ValueByte.get(paramByte));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateByte(String paramString, byte paramByte)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateByte(" + paramString + ", " + paramByte + ");");
      }
      update(paramString, ValueByte.get(paramByte));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateBytes(int paramInt, byte[] paramArrayOfByte)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateBytes(" + paramInt + ", x);");
      }
      update(paramInt, paramArrayOfByte == null ? ValueNull.INSTANCE : ValueBytes.get(paramArrayOfByte));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateBytes(String paramString, byte[] paramArrayOfByte)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateBytes(" + quote(paramString) + ", x);");
      }
      update(paramString, paramArrayOfByte == null ? ValueNull.INSTANCE : ValueBytes.get(paramArrayOfByte));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateShort(int paramInt, short paramShort)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateShort(" + paramInt + ", (short) " + paramShort + ");");
      }
      update(paramInt, ValueShort.get(paramShort));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateShort(String paramString, short paramShort)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateShort(" + quote(paramString) + ", (short) " + paramShort + ");");
      }
      update(paramString, ValueShort.get(paramShort));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateInt(int paramInt1, int paramInt2)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateInt(" + paramInt1 + ", " + paramInt2 + ");");
      }
      update(paramInt1, ValueInt.get(paramInt2));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateInt(String paramString, int paramInt)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateInt(" + quote(paramString) + ", " + paramInt + ");");
      }
      update(paramString, ValueInt.get(paramInt));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateLong(int paramInt, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateLong(" + paramInt + ", " + paramLong + "L);");
      }
      update(paramInt, ValueLong.get(paramLong));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateLong(String paramString, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateLong(" + quote(paramString) + ", " + paramLong + "L);");
      }
      update(paramString, ValueLong.get(paramLong));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateFloat(int paramInt, float paramFloat)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateFloat(" + paramInt + ", " + paramFloat + "f);");
      }
      update(paramInt, ValueFloat.get(paramFloat));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateFloat(String paramString, float paramFloat)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateFloat(" + quote(paramString) + ", " + paramFloat + "f);");
      }
      update(paramString, ValueFloat.get(paramFloat));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateDouble(int paramInt, double paramDouble)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateDouble(" + paramInt + ", " + paramDouble + "d);");
      }
      update(paramInt, ValueDouble.get(paramDouble));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateDouble(String paramString, double paramDouble)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateDouble(" + quote(paramString) + ", " + paramDouble + "d);");
      }
      update(paramString, ValueDouble.get(paramDouble));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateBigDecimal(int paramInt, BigDecimal paramBigDecimal)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateBigDecimal(" + paramInt + ", " + quoteBigDecimal(paramBigDecimal) + ");");
      }
      update(paramInt, paramBigDecimal == null ? ValueNull.INSTANCE : ValueDecimal.get(paramBigDecimal));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateBigDecimal(String paramString, BigDecimal paramBigDecimal)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateBigDecimal(" + quote(paramString) + ", " + quoteBigDecimal(paramBigDecimal) + ");");
      }
      update(paramString, paramBigDecimal == null ? ValueNull.INSTANCE : ValueDecimal.get(paramBigDecimal));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateString(int paramInt, String paramString)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateString(" + paramInt + ", " + quote(paramString) + ");");
      }
      update(paramInt, paramString == null ? ValueNull.INSTANCE : ValueString.get(paramString));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateString(String paramString1, String paramString2)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateString(" + quote(paramString1) + ", " + quote(paramString2) + ");");
      }
      update(paramString1, paramString2 == null ? ValueNull.INSTANCE : ValueString.get(paramString2));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateDate(int paramInt, Date paramDate)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateDate(" + paramInt + ", x);");
      }
      update(paramInt, paramDate == null ? ValueNull.INSTANCE : ValueDate.get(paramDate));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateDate(String paramString, Date paramDate)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateDate(" + quote(paramString) + ", x);");
      }
      update(paramString, paramDate == null ? ValueNull.INSTANCE : ValueDate.get(paramDate));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateTime(int paramInt, Time paramTime)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateTime(" + paramInt + ", x);");
      }
      update(paramInt, paramTime == null ? ValueNull.INSTANCE : ValueTime.get(paramTime));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateTime(String paramString, Time paramTime)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateTime(" + quote(paramString) + ", x);");
      }
      update(paramString, paramTime == null ? ValueNull.INSTANCE : ValueTime.get(paramTime));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateTimestamp(int paramInt, Timestamp paramTimestamp)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateTimestamp(" + paramInt + ", x);");
      }
      update(paramInt, paramTimestamp == null ? ValueNull.INSTANCE : ValueTimestamp.get(paramTimestamp));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateTimestamp(String paramString, Timestamp paramTimestamp)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateTimestamp(" + quote(paramString) + ", x);");
      }
      update(paramString, paramTimestamp == null ? ValueNull.INSTANCE : ValueTimestamp.get(paramTimestamp));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateAsciiStream(int paramInt1, InputStream paramInputStream, int paramInt2)
    throws SQLException
  {
    updateAsciiStream(paramInt1, paramInputStream, paramInt2);
  }
  
  public void updateAsciiStream(int paramInt, InputStream paramInputStream)
    throws SQLException
  {
    updateAsciiStream(paramInt, paramInputStream, -1);
  }
  
  public void updateAsciiStream(int paramInt, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateAsciiStream(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosed();
      Value localValue = this.conn.createClob(IOUtils.getAsciiReader(paramInputStream), paramLong);
      update(paramInt, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateAsciiStream(String paramString, InputStream paramInputStream, int paramInt)
    throws SQLException
  {
    updateAsciiStream(paramString, paramInputStream, paramInt);
  }
  
  public void updateAsciiStream(String paramString, InputStream paramInputStream)
    throws SQLException
  {
    updateAsciiStream(paramString, paramInputStream, -1);
  }
  
  public void updateAsciiStream(String paramString, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateAsciiStream(" + quote(paramString) + ", x, " + paramLong + "L);");
      }
      checkClosed();
      Value localValue = this.conn.createClob(IOUtils.getAsciiReader(paramInputStream), paramLong);
      update(paramString, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateBinaryStream(int paramInt1, InputStream paramInputStream, int paramInt2)
    throws SQLException
  {
    updateBinaryStream(paramInt1, paramInputStream, paramInt2);
  }
  
  public void updateBinaryStream(int paramInt, InputStream paramInputStream)
    throws SQLException
  {
    updateBinaryStream(paramInt, paramInputStream, -1);
  }
  
  public void updateBinaryStream(int paramInt, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateBinaryStream(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosed();
      Value localValue = this.conn.createBlob(paramInputStream, paramLong);
      update(paramInt, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateBinaryStream(String paramString, InputStream paramInputStream)
    throws SQLException
  {
    updateBinaryStream(paramString, paramInputStream, -1);
  }
  
  public void updateBinaryStream(String paramString, InputStream paramInputStream, int paramInt)
    throws SQLException
  {
    updateBinaryStream(paramString, paramInputStream, paramInt);
  }
  
  public void updateBinaryStream(String paramString, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateBinaryStream(" + quote(paramString) + ", x, " + paramLong + "L);");
      }
      checkClosed();
      Value localValue = this.conn.createBlob(paramInputStream, paramLong);
      update(paramString, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateCharacterStream(int paramInt, Reader paramReader, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateCharacterStream(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosed();
      Value localValue = this.conn.createClob(paramReader, paramLong);
      update(paramInt, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateCharacterStream(int paramInt1, Reader paramReader, int paramInt2)
    throws SQLException
  {
    updateCharacterStream(paramInt1, paramReader, paramInt2);
  }
  
  public void updateCharacterStream(int paramInt, Reader paramReader)
    throws SQLException
  {
    updateCharacterStream(paramInt, paramReader, -1);
  }
  
  public void updateCharacterStream(String paramString, Reader paramReader, int paramInt)
    throws SQLException
  {
    updateCharacterStream(paramString, paramReader, paramInt);
  }
  
  public void updateCharacterStream(String paramString, Reader paramReader)
    throws SQLException
  {
    updateCharacterStream(paramString, paramReader, -1);
  }
  
  public void updateCharacterStream(String paramString, Reader paramReader, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateCharacterStream(" + quote(paramString) + ", x, " + paramLong + "L);");
      }
      checkClosed();
      Value localValue = this.conn.createClob(paramReader, paramLong);
      update(paramString, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateObject(int paramInt1, Object paramObject, int paramInt2)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateObject(" + paramInt1 + ", x, " + paramInt2 + ");");
      }
      update(paramInt1, convertToUnknownValue(paramObject));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateObject(String paramString, Object paramObject, int paramInt)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateObject(" + quote(paramString) + ", x, " + paramInt + ");");
      }
      update(paramString, convertToUnknownValue(paramObject));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateObject(int paramInt, Object paramObject)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateObject(" + paramInt + ", x);");
      }
      update(paramInt, convertToUnknownValue(paramObject));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateObject(String paramString, Object paramObject)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateObject(" + quote(paramString) + ", x);");
      }
      update(paramString, convertToUnknownValue(paramObject));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateRef(int paramInt, Ref paramRef)
    throws SQLException
  {
    throw unsupported("ref");
  }
  
  public void updateRef(String paramString, Ref paramRef)
    throws SQLException
  {
    throw unsupported("ref");
  }
  
  public void updateBlob(int paramInt, InputStream paramInputStream)
    throws SQLException
  {
    updateBlob(paramInt, paramInputStream, -1L);
  }
  
  public void updateBlob(int paramInt, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateBlob(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosed();
      Value localValue = this.conn.createBlob(paramInputStream, paramLong);
      update(paramInt, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateBlob(int paramInt, Blob paramBlob)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateBlob(" + paramInt + ", x);");
      }
      checkClosed();
      Object localObject;
      if (paramBlob == null) {
        localObject = ValueNull.INSTANCE;
      } else {
        localObject = this.conn.createBlob(paramBlob.getBinaryStream(), -1L);
      }
      update(paramInt, (Value)localObject);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateBlob(String paramString, Blob paramBlob)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateBlob(" + quote(paramString) + ", x);");
      }
      checkClosed();
      Object localObject;
      if (paramBlob == null) {
        localObject = ValueNull.INSTANCE;
      } else {
        localObject = this.conn.createBlob(paramBlob.getBinaryStream(), -1L);
      }
      update(paramString, (Value)localObject);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateBlob(String paramString, InputStream paramInputStream)
    throws SQLException
  {
    updateBlob(paramString, paramInputStream, -1L);
  }
  
  public void updateBlob(String paramString, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateBlob(" + quote(paramString) + ", x, " + paramLong + "L);");
      }
      checkClosed();
      Value localValue = this.conn.createBlob(paramInputStream, -1L);
      update(paramString, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateClob(int paramInt, Clob paramClob)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateClob(" + paramInt + ", x);");
      }
      checkClosed();
      Object localObject;
      if (paramClob == null) {
        localObject = ValueNull.INSTANCE;
      } else {
        localObject = this.conn.createClob(paramClob.getCharacterStream(), -1L);
      }
      update(paramInt, (Value)localObject);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateClob(int paramInt, Reader paramReader)
    throws SQLException
  {
    updateClob(paramInt, paramReader, -1L);
  }
  
  public void updateClob(int paramInt, Reader paramReader, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateClob(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosed();
      Value localValue = this.conn.createClob(paramReader, paramLong);
      update(paramInt, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateClob(String paramString, Clob paramClob)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateClob(" + quote(paramString) + ", x);");
      }
      checkClosed();
      Object localObject;
      if (paramClob == null) {
        localObject = ValueNull.INSTANCE;
      } else {
        localObject = this.conn.createClob(paramClob.getCharacterStream(), -1L);
      }
      update(paramString, (Value)localObject);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateClob(String paramString, Reader paramReader)
    throws SQLException
  {
    updateClob(paramString, paramReader, -1L);
  }
  
  public void updateClob(String paramString, Reader paramReader, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateClob(" + quote(paramString) + ", x, " + paramLong + "L);");
      }
      checkClosed();
      Value localValue = this.conn.createClob(paramReader, paramLong);
      update(paramString, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateArray(int paramInt, Array paramArray)
    throws SQLException
  {
    throw unsupported("setArray");
  }
  
  public void updateArray(String paramString, Array paramArray)
    throws SQLException
  {
    throw unsupported("setArray");
  }
  
  public String getCursorName()
    throws SQLException
  {
    throw unsupported("cursorName");
  }
  
  public int getRow()
    throws SQLException
  {
    try
    {
      debugCodeCall("getRow");
      checkClosed();
      int i = this.result.getRowId();
      if (i >= this.result.getRowCount()) {
        return 0;
      }
      return i + 1;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getConcurrency()
    throws SQLException
  {
    try
    {
      debugCodeCall("getConcurrency");
      checkClosed();
      if (!this.updatable) {
        return 1007;
      }
      UpdatableRow localUpdatableRow = new UpdatableRow(this.conn, this.result);
      return localUpdatableRow.isUpdatable() ? 1008 : 1007;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getFetchDirection()
    throws SQLException
  {
    try
    {
      debugCodeCall("getFetchDirection");
      checkClosed();
      return 1000;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getFetchSize()
    throws SQLException
  {
    try
    {
      debugCodeCall("getFetchSize");
      checkClosed();
      return this.result.getFetchSize();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setFetchSize(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("setFetchSize", paramInt);
      checkClosed();
      if (paramInt < 0) {
        throw DbException.getInvalidValueException("rows", Integer.valueOf(paramInt));
      }
      if (paramInt > 0)
      {
        if (this.stat != null)
        {
          int i = this.stat.getMaxRows();
          if ((i > 0) && (paramInt > i)) {
            throw DbException.getInvalidValueException("rows", Integer.valueOf(paramInt));
          }
        }
      }
      else {
        paramInt = SysProperties.SERVER_RESULT_SET_FETCH_SIZE;
      }
      this.result.setFetchSize(paramInt);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setFetchDirection(int paramInt)
    throws SQLException
  {
    throw unsupported("setFetchDirection");
  }
  
  public int getType()
    throws SQLException
  {
    try
    {
      debugCodeCall("getType");
      checkClosed();
      return this.stat == null ? 1003 : this.stat.resultSetType;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isBeforeFirst()
    throws SQLException
  {
    try
    {
      debugCodeCall("isBeforeFirst");
      checkClosed();
      int i = this.result.getRowId();
      int j = this.result.getRowCount();
      return (j > 0) && (i < 0);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isAfterLast()
    throws SQLException
  {
    try
    {
      debugCodeCall("isAfterLast");
      checkClosed();
      int i = this.result.getRowId();
      int j = this.result.getRowCount();
      return (j > 0) && (i >= j);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isFirst()
    throws SQLException
  {
    try
    {
      debugCodeCall("isFirst");
      checkClosed();
      int i = this.result.getRowId();
      return (i == 0) && (i < this.result.getRowCount());
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isLast()
    throws SQLException
  {
    try
    {
      debugCodeCall("isLast");
      checkClosed();
      int i = this.result.getRowId();
      return (i >= 0) && (i == this.result.getRowCount() - 1);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void beforeFirst()
    throws SQLException
  {
    try
    {
      debugCodeCall("beforeFirst");
      checkClosed();
      if (this.result.getRowId() >= 0) {
        resetResult();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void afterLast()
    throws SQLException
  {
    try
    {
      debugCodeCall("afterLast");
      checkClosed();
      while (nextRow()) {}
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean first()
    throws SQLException
  {
    try
    {
      debugCodeCall("first");
      checkClosed();
      if (this.result.getRowId() < 0) {
        return nextRow();
      }
      resetResult();
      return nextRow();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean last()
    throws SQLException
  {
    try
    {
      debugCodeCall("last");
      checkClosed();
      return absolute(-1);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean absolute(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("absolute", paramInt);
      checkClosed();
      if (paramInt < 0) {
        paramInt = this.result.getRowCount() + paramInt + 1;
      } else if (paramInt > this.result.getRowCount() + 1) {
        paramInt = this.result.getRowCount() + 1;
      }
      if (paramInt <= this.result.getRowId()) {
        resetResult();
      }
      while (this.result.getRowId() + 1 < paramInt) {
        nextRow();
      }
      int i = this.result.getRowId();
      return (i >= 0) && (i < this.result.getRowCount());
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean relative(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("relative", paramInt);
      checkClosed();
      int i = this.result.getRowId() + 1 + paramInt;
      if (i < 0) {
        i = 0;
      } else if (i > this.result.getRowCount()) {
        i = this.result.getRowCount() + 1;
      }
      return absolute(i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean previous()
    throws SQLException
  {
    try
    {
      debugCodeCall("previous");
      checkClosed();
      return relative(-1);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void moveToInsertRow()
    throws SQLException
  {
    try
    {
      debugCodeCall("moveToInsertRow");
      checkUpdatable();
      this.insertRow = new Value[this.columnCount];
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void moveToCurrentRow()
    throws SQLException
  {
    try
    {
      debugCodeCall("moveToCurrentRow");
      checkUpdatable();
      this.insertRow = null;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean rowUpdated()
    throws SQLException
  {
    try
    {
      debugCodeCall("rowUpdated");
      return false;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean rowInserted()
    throws SQLException
  {
    try
    {
      debugCodeCall("rowInserted");
      return false;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean rowDeleted()
    throws SQLException
  {
    try
    {
      debugCodeCall("rowDeleted");
      return false;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void insertRow()
    throws SQLException
  {
    try
    {
      debugCodeCall("insertRow");
      checkUpdatable();
      if (this.insertRow == null) {
        throw DbException.get(90029);
      }
      getUpdatableRow().insertRow(this.insertRow);
      this.insertRow = null;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateRow()
    throws SQLException
  {
    try
    {
      debugCodeCall("updateRow");
      checkUpdatable();
      if (this.insertRow != null) {
        throw DbException.get(90029);
      }
      checkOnValidRow();
      if (this.updateRow != null)
      {
        UpdatableRow localUpdatableRow = getUpdatableRow();
        Value[] arrayOfValue1 = new Value[this.columnCount];
        for (int i = 0; i < this.updateRow.length; i++) {
          arrayOfValue1[i] = get(i + 1);
        }
        localUpdatableRow.updateRow(arrayOfValue1, this.updateRow);
        for (i = 0; i < this.updateRow.length; i++) {
          if (this.updateRow[i] == null) {
            this.updateRow[i] = arrayOfValue1[i];
          }
        }
        Value[] arrayOfValue2 = localUpdatableRow.readRow(this.updateRow);
        patchCurrentRow(arrayOfValue2);
        this.updateRow = null;
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void deleteRow()
    throws SQLException
  {
    try
    {
      debugCodeCall("deleteRow");
      checkUpdatable();
      if (this.insertRow != null) {
        throw DbException.get(90029);
      }
      checkOnValidRow();
      getUpdatableRow().deleteRow(this.result.currentRow());
      this.updateRow = null;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void refreshRow()
    throws SQLException
  {
    try
    {
      debugCodeCall("refreshRow");
      checkClosed();
      if (this.insertRow != null) {
        throw DbException.get(2000);
      }
      checkOnValidRow();
      patchCurrentRow(getUpdatableRow().readRow(this.result.currentRow()));
      this.updateRow = null;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void cancelRowUpdates()
    throws SQLException
  {
    try
    {
      debugCodeCall("cancelRowUpdates");
      checkClosed();
      if (this.insertRow != null) {
        throw DbException.get(2000);
      }
      this.updateRow = null;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  private UpdatableRow getUpdatableRow()
    throws SQLException
  {
    UpdatableRow localUpdatableRow = new UpdatableRow(this.conn, this.result);
    if (!localUpdatableRow.isUpdatable()) {
      throw DbException.get(90127);
    }
    return localUpdatableRow;
  }
  
  private int getColumnIndex(String paramString)
  {
    checkClosed();
    if (paramString == null) {
      throw DbException.getInvalidValueException("columnLabel", null);
    }
    String str2;
    if (this.columnCount >= 3)
    {
      if (this.columnLabelMap == null)
      {
        localObject = New.hashMap(this.columnCount);
        for (int j = 0; j < this.columnCount; j++)
        {
          str2 = StringUtils.toUpperEnglish(this.result.getAlias(j));
          mapColumn((HashMap)localObject, str2, j);
        }
        for (j = 0; j < this.columnCount; j++)
        {
          str2 = this.result.getColumnName(j);
          if (str2 != null)
          {
            str2 = StringUtils.toUpperEnglish(str2);
            mapColumn((HashMap)localObject, str2, j);
            String str3 = this.result.getTableName(j);
            if (str3 != null)
            {
              str2 = StringUtils.toUpperEnglish(str3) + "." + str2;
              mapColumn((HashMap)localObject, str2, j);
            }
          }
        }
        this.columnLabelMap = ((HashMap)localObject);
        if (this.preparedStatement != null) {
          this.preparedStatement.setCachedColumnLabelMap(this.columnLabelMap);
        }
      }
      Object localObject = (Integer)this.columnLabelMap.get(StringUtils.toUpperEnglish(paramString));
      if (localObject == null) {
        throw DbException.get(42122, paramString);
      }
      return ((Integer)localObject).intValue() + 1;
    }
    for (int i = 0; i < this.columnCount; i++) {
      if (paramString.equalsIgnoreCase(this.result.getAlias(i))) {
        return i + 1;
      }
    }
    i = paramString.indexOf('.');
    if (i > 0)
    {
      String str1 = paramString.substring(0, i);
      str2 = paramString.substring(i + 1);
      for (int m = 0; m < this.columnCount; m++) {
        if ((str1.equalsIgnoreCase(this.result.getTableName(m))) && (str2.equalsIgnoreCase(this.result.getColumnName(m)))) {
          return m + 1;
        }
      }
    }
    else
    {
      for (int k = 0; k < this.columnCount; k++) {
        if (paramString.equalsIgnoreCase(this.result.getColumnName(k))) {
          return k + 1;
        }
      }
    }
    throw DbException.get(42122, paramString);
  }
  
  private static void mapColumn(HashMap<String, Integer> paramHashMap, String paramString, int paramInt)
  {
    Integer localInteger = (Integer)paramHashMap.put(paramString, Integer.valueOf(paramInt));
    if (localInteger != null) {
      paramHashMap.put(paramString, localInteger);
    }
  }
  
  private void checkColumnIndex(int paramInt)
  {
    checkClosed();
    if ((paramInt < 1) || (paramInt > this.columnCount)) {
      throw DbException.getInvalidValueException("columnIndex", Integer.valueOf(paramInt));
    }
  }
  
  void checkClosed()
  {
    if (this.result == null) {
      throw DbException.get(90007);
    }
    if (this.stat != null) {
      this.stat.checkClosed();
    }
    if (this.conn != null) {
      this.conn.checkClosed();
    }
  }
  
  private void checkOnValidRow()
  {
    if ((this.result.getRowId() < 0) || (this.result.getRowId() >= this.result.getRowCount())) {
      throw DbException.get(2000);
    }
  }
  
  private Value get(int paramInt)
  {
    checkColumnIndex(paramInt);
    checkOnValidRow();
    Value[] arrayOfValue;
    if (this.patchedRows == null)
    {
      arrayOfValue = this.result.currentRow();
    }
    else
    {
      arrayOfValue = (Value[])this.patchedRows.get(Integer.valueOf(this.result.getRowId()));
      if (arrayOfValue == null) {
        arrayOfValue = this.result.currentRow();
      }
    }
    Value localValue = arrayOfValue[(paramInt - 1)];
    this.wasNull = (localValue == ValueNull.INSTANCE);
    return localValue;
  }
  
  private Value get(String paramString)
  {
    int i = getColumnIndex(paramString);
    return get(i);
  }
  
  private void update(String paramString, Value paramValue)
  {
    int i = getColumnIndex(paramString);
    update(i, paramValue);
  }
  
  private void update(int paramInt, Value paramValue)
  {
    checkUpdatable();
    checkColumnIndex(paramInt);
    if (this.insertRow != null)
    {
      this.insertRow[(paramInt - 1)] = paramValue;
    }
    else
    {
      if (this.updateRow == null) {
        this.updateRow = new Value[this.columnCount];
      }
      this.updateRow[(paramInt - 1)] = paramValue;
    }
  }
  
  private boolean nextRow()
  {
    boolean bool = this.result.next();
    if ((!bool) && (!this.scrollable)) {
      this.result.close();
    }
    return bool;
  }
  
  private void resetResult()
  {
    if (!this.scrollable) {
      throw DbException.get(90128);
    }
    this.result.reset();
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
  
  public void updateRowId(int paramInt, RowId paramRowId)
    throws SQLException
  {
    throw unsupported("rowId");
  }
  
  public void updateRowId(String paramString, RowId paramRowId)
    throws SQLException
  {
    throw unsupported("rowId");
  }
  
  public int getHoldability()
    throws SQLException
  {
    try
    {
      debugCodeCall("getHoldability");
      checkClosed();
      return this.conn.getHoldability();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isClosed()
    throws SQLException
  {
    try
    {
      debugCodeCall("isClosed");
      return this.result == null;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateNString(int paramInt, String paramString)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateNString(" + paramInt + ", " + quote(paramString) + ");");
      }
      update(paramInt, paramString == null ? ValueNull.INSTANCE : ValueString.get(paramString));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateNString(String paramString1, String paramString2)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateNString(" + quote(paramString1) + ", " + quote(paramString2) + ");");
      }
      update(paramString1, paramString2 == null ? ValueNull.INSTANCE : ValueString.get(paramString2));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateNClob(int paramInt, NClob paramNClob)
    throws SQLException
  {
    throw unsupported("NClob");
  }
  
  public void updateNClob(int paramInt, Reader paramReader)
    throws SQLException
  {
    updateClob(paramInt, paramReader, -1L);
  }
  
  public void updateNClob(int paramInt, Reader paramReader, long paramLong)
    throws SQLException
  {
    updateClob(paramInt, paramReader, paramLong);
  }
  
  public void updateNClob(String paramString, Reader paramReader)
    throws SQLException
  {
    updateClob(paramString, paramReader, -1L);
  }
  
  public void updateNClob(String paramString, Reader paramReader, long paramLong)
    throws SQLException
  {
    updateClob(paramString, paramReader, paramLong);
  }
  
  public void updateNClob(String paramString, NClob paramNClob)
    throws SQLException
  {
    throw unsupported("NClob");
  }
  
  public NClob getNClob(int paramInt)
    throws SQLException
  {
    try
    {
      int i = getNextId(10);
      debugCodeAssign("NClob", 10, i, "getNClob(" + paramInt + ")");
      Value localValue = get(paramInt);
      return localValue == ValueNull.INSTANCE ? null : new JdbcClob(this.conn, localValue, i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public NClob getNClob(String paramString)
    throws SQLException
  {
    try
    {
      int i = getNextId(10);
      debugCodeAssign("NClob", 10, i, "getNClob(" + paramString + ")");
      Value localValue = get(paramString);
      return localValue == ValueNull.INSTANCE ? null : new JdbcClob(this.conn, localValue, i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
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
  
  public void updateSQLXML(int paramInt, SQLXML paramSQLXML)
    throws SQLException
  {
    throw unsupported("SQLXML");
  }
  
  public void updateSQLXML(String paramString, SQLXML paramSQLXML)
    throws SQLException
  {
    throw unsupported("SQLXML");
  }
  
  public String getNString(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getNString", paramInt);
      return get(paramInt).getString();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getNString(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getNString", paramString);
      return get(paramString).getString();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Reader getNCharacterStream(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getNCharacterStream", paramInt);
      return get(paramInt).getReader();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Reader getNCharacterStream(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("getNCharacterStream", paramString);
      return get(paramString).getReader();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateNCharacterStream(int paramInt, Reader paramReader)
    throws SQLException
  {
    updateNCharacterStream(paramInt, paramReader, -1L);
  }
  
  public void updateNCharacterStream(int paramInt, Reader paramReader, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateNCharacterStream(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosed();
      Value localValue = this.conn.createClob(paramReader, paramLong);
      update(paramInt, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void updateNCharacterStream(String paramString, Reader paramReader)
    throws SQLException
  {
    updateNCharacterStream(paramString, paramReader, -1L);
  }
  
  public void updateNCharacterStream(String paramString, Reader paramReader, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("updateNCharacterStream(" + quote(paramString) + ", x, " + paramLong + "L);");
      }
      checkClosed();
      Value localValue = this.conn.createClob(paramReader, paramLong);
      update(paramString, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public <T> T unwrap(Class<T> paramClass)
    throws SQLException
  {
    if (isWrapperFor(paramClass)) {
      return this;
    }
    throw DbException.getInvalidValueException("iface", paramClass);
  }
  
  public boolean isWrapperFor(Class<?> paramClass)
    throws SQLException
  {
    return (paramClass != null) && (paramClass.isAssignableFrom(getClass()));
  }
  
  public String toString()
  {
    return getTraceObjectName() + ": " + this.result;
  }
  
  private void patchCurrentRow(Value[] paramArrayOfValue)
  {
    int i = 0;
    Value[] arrayOfValue = this.result.currentRow();
    CompareMode localCompareMode = this.conn.getCompareMode();
    for (int j = 0; j < paramArrayOfValue.length; j++) {
      if (paramArrayOfValue[j].compareTo(arrayOfValue[j], localCompareMode) != 0)
      {
        i = 1;
        break;
      }
    }
    if (this.patchedRows == null) {
      this.patchedRows = New.hashMap();
    }
    Integer localInteger = Integer.valueOf(this.result.getRowId());
    if (i == 0) {
      this.patchedRows.remove(localInteger);
    } else {
      this.patchedRows.put(localInteger, paramArrayOfValue);
    }
  }
  
  private Value convertToUnknownValue(Object paramObject)
  {
    checkClosed();
    return DataType.convertToValue(this.conn.getSession(), paramObject, -1);
  }
  
  private void checkUpdatable()
  {
    checkClosed();
    if (!this.updatable) {
      throw DbException.get(90140);
    }
  }
}
