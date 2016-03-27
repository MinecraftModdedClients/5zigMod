package org.h2.tools;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import org.h2.message.DbException;
import org.h2.util.JdbcUtils;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.value.DataType;

public class SimpleResultSet
  implements ResultSet, ResultSetMetaData
{
  private ArrayList<Object[]> rows;
  private Object[] currentRow;
  private int rowId = -1;
  private boolean wasNull;
  private SimpleRowSource source;
  private ArrayList<Column> columns = New.arrayList();
  private boolean autoClose = true;
  
  public SimpleResultSet()
  {
    this.rows = New.arrayList();
  }
  
  public SimpleResultSet(SimpleRowSource paramSimpleRowSource)
  {
    this.source = paramSimpleRowSource;
  }
  
  public void addColumn(String paramString, int paramInt1, int paramInt2, int paramInt3)
  {
    int i = DataType.convertSQLTypeToValueType(paramInt1);
    addColumn(paramString, paramInt1, DataType.getDataType(i).name, paramInt2, paramInt3);
  }
  
  public void addColumn(String paramString1, int paramInt1, String paramString2, int paramInt2, int paramInt3)
  {
    if ((this.rows != null) && (this.rows.size() > 0)) {
      throw new IllegalStateException("Cannot add a column after adding rows");
    }
    if (paramString1 == null) {
      paramString1 = "C" + (this.columns.size() + 1);
    }
    Column localColumn = new Column();
    localColumn.name = paramString1;
    localColumn.sqlType = paramInt1;
    localColumn.precision = paramInt2;
    localColumn.scale = paramInt3;
    localColumn.sqlTypeName = paramString2;
    this.columns.add(localColumn);
  }
  
  public void addRow(Object... paramVarArgs)
  {
    if (this.rows == null) {
      throw new IllegalStateException("Cannot add a row when using RowSource");
    }
    this.rows.add(paramVarArgs);
  }
  
  public int getConcurrency()
  {
    return 1007;
  }
  
  public int getFetchDirection()
  {
    return 1000;
  }
  
  public int getFetchSize()
  {
    return 0;
  }
  
  public int getRow()
  {
    return this.currentRow == null ? 0 : this.rowId + 1;
  }
  
  public int getType()
  {
    if (this.autoClose) {
      return 1003;
    }
    return 1004;
  }
  
  public void close()
  {
    this.currentRow = null;
    this.rows = null;
    this.columns = null;
    this.rowId = -1;
    if (this.source != null)
    {
      this.source.close();
      this.source = null;
    }
  }
  
  public boolean next()
    throws SQLException
  {
    if (this.source != null)
    {
      this.rowId += 1;
      this.currentRow = this.source.readRow();
      if (this.currentRow != null) {
        return true;
      }
    }
    else if ((this.rows != null) && (this.rowId < this.rows.size()))
    {
      this.rowId += 1;
      if (this.rowId < this.rows.size())
      {
        this.currentRow = ((Object[])this.rows.get(this.rowId));
        return true;
      }
      this.currentRow = null;
    }
    if (this.autoClose) {
      close();
    }
    return false;
  }
  
  public void beforeFirst()
    throws SQLException
  {
    if (this.autoClose) {
      throw DbException.get(90128);
    }
    this.rowId = -1;
    if (this.source != null) {
      this.source.reset();
    }
  }
  
  public boolean wasNull()
  {
    return this.wasNull;
  }
  
  public int findColumn(String paramString)
    throws SQLException
  {
    if ((paramString != null) && (this.columns != null))
    {
      int i = 0;
      for (int j = this.columns.size(); i < j; i++) {
        if (paramString.equalsIgnoreCase(getColumn(i).name)) {
          return i + 1;
        }
      }
    }
    throw DbException.get(42122, paramString).getSQLException();
  }
  
  public ResultSetMetaData getMetaData()
  {
    return this;
  }
  
  public SQLWarning getWarnings()
  {
    return null;
  }
  
  public Statement getStatement()
  {
    return null;
  }
  
  public void clearWarnings() {}
  
  public Array getArray(int paramInt)
    throws SQLException
  {
    Object[] arrayOfObject = (Object[])get(paramInt);
    return arrayOfObject == null ? null : new SimpleArray(arrayOfObject);
  }
  
  public Array getArray(String paramString)
    throws SQLException
  {
    return getArray(findColumn(paramString));
  }
  
  public InputStream getAsciiStream(int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public InputStream getAsciiStream(String paramString)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public BigDecimal getBigDecimal(int paramInt)
    throws SQLException
  {
    Object localObject = get(paramInt);
    if ((localObject != null) && (!(localObject instanceof BigDecimal))) {
      localObject = new BigDecimal(localObject.toString());
    }
    return (BigDecimal)localObject;
  }
  
  public BigDecimal getBigDecimal(String paramString)
    throws SQLException
  {
    return getBigDecimal(findColumn(paramString));
  }
  
  /**
   * @deprecated
   */
  public BigDecimal getBigDecimal(int paramInt1, int paramInt2)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  /**
   * @deprecated
   */
  public BigDecimal getBigDecimal(String paramString, int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public InputStream getBinaryStream(int paramInt)
    throws SQLException
  {
    return asInputStream(get(paramInt));
  }
  
  private static InputStream asInputStream(Object paramObject)
    throws SQLException
  {
    if (paramObject == null) {
      return null;
    }
    if ((paramObject instanceof Blob)) {
      return ((Blob)paramObject).getBinaryStream();
    }
    return (InputStream)paramObject;
  }
  
  public InputStream getBinaryStream(String paramString)
    throws SQLException
  {
    return getBinaryStream(findColumn(paramString));
  }
  
  public Blob getBlob(int paramInt)
    throws SQLException
  {
    return (Blob)get(paramInt);
  }
  
  public Blob getBlob(String paramString)
    throws SQLException
  {
    return getBlob(findColumn(paramString));
  }
  
  public boolean getBoolean(int paramInt)
    throws SQLException
  {
    Object localObject = get(paramInt);
    if ((localObject != null) && (!(localObject instanceof Boolean))) {
      localObject = Boolean.valueOf(localObject.toString());
    }
    return localObject == null ? false : ((Boolean)localObject).booleanValue();
  }
  
  public boolean getBoolean(String paramString)
    throws SQLException
  {
    return getBoolean(findColumn(paramString));
  }
  
  public byte getByte(int paramInt)
    throws SQLException
  {
    Object localObject = get(paramInt);
    if ((localObject != null) && (!(localObject instanceof Number))) {
      localObject = Byte.decode(localObject.toString());
    }
    return localObject == null ? 0 : ((Number)localObject).byteValue();
  }
  
  public byte getByte(String paramString)
    throws SQLException
  {
    return getByte(findColumn(paramString));
  }
  
  public byte[] getBytes(int paramInt)
    throws SQLException
  {
    Object localObject = get(paramInt);
    if ((localObject == null) || ((localObject instanceof byte[]))) {
      return (byte[])localObject;
    }
    return JdbcUtils.serialize(localObject, null);
  }
  
  public byte[] getBytes(String paramString)
    throws SQLException
  {
    return getBytes(findColumn(paramString));
  }
  
  public Reader getCharacterStream(int paramInt)
    throws SQLException
  {
    return asReader(get(paramInt));
  }
  
  private static Reader asReader(Object paramObject)
    throws SQLException
  {
    if (paramObject == null) {
      return null;
    }
    if ((paramObject instanceof Clob)) {
      return ((Clob)paramObject).getCharacterStream();
    }
    return (Reader)paramObject;
  }
  
  public Reader getCharacterStream(String paramString)
    throws SQLException
  {
    return getCharacterStream(findColumn(paramString));
  }
  
  public Clob getClob(int paramInt)
    throws SQLException
  {
    Clob localClob = (Clob)get(paramInt);
    return localClob == null ? null : localClob;
  }
  
  public Clob getClob(String paramString)
    throws SQLException
  {
    return getClob(findColumn(paramString));
  }
  
  public Date getDate(int paramInt)
    throws SQLException
  {
    return (Date)get(paramInt);
  }
  
  public Date getDate(String paramString)
    throws SQLException
  {
    return getDate(findColumn(paramString));
  }
  
  public Date getDate(int paramInt, Calendar paramCalendar)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public Date getDate(String paramString, Calendar paramCalendar)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public double getDouble(int paramInt)
    throws SQLException
  {
    Object localObject = get(paramInt);
    if ((localObject != null) && (!(localObject instanceof Number))) {
      return Double.parseDouble(localObject.toString());
    }
    return localObject == null ? 0.0D : ((Number)localObject).doubleValue();
  }
  
  public double getDouble(String paramString)
    throws SQLException
  {
    return getDouble(findColumn(paramString));
  }
  
  public float getFloat(int paramInt)
    throws SQLException
  {
    Object localObject = get(paramInt);
    if ((localObject != null) && (!(localObject instanceof Number))) {
      return Float.parseFloat(localObject.toString());
    }
    return localObject == null ? 0.0F : ((Number)localObject).floatValue();
  }
  
  public float getFloat(String paramString)
    throws SQLException
  {
    return getFloat(findColumn(paramString));
  }
  
  public int getInt(int paramInt)
    throws SQLException
  {
    Object localObject = get(paramInt);
    if ((localObject != null) && (!(localObject instanceof Number))) {
      localObject = Integer.decode(localObject.toString());
    }
    return localObject == null ? 0 : ((Number)localObject).intValue();
  }
  
  public int getInt(String paramString)
    throws SQLException
  {
    return getInt(findColumn(paramString));
  }
  
  public long getLong(int paramInt)
    throws SQLException
  {
    Object localObject = get(paramInt);
    if ((localObject != null) && (!(localObject instanceof Number))) {
      localObject = Long.decode(localObject.toString());
    }
    return localObject == null ? 0L : ((Number)localObject).longValue();
  }
  
  public long getLong(String paramString)
    throws SQLException
  {
    return getLong(findColumn(paramString));
  }
  
  public Reader getNCharacterStream(int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public Reader getNCharacterStream(String paramString)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public NClob getNClob(int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public NClob getNClob(String paramString)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public String getNString(int paramInt)
    throws SQLException
  {
    return getString(paramInt);
  }
  
  public String getNString(String paramString)
    throws SQLException
  {
    return getString(paramString);
  }
  
  public Object getObject(int paramInt)
    throws SQLException
  {
    return get(paramInt);
  }
  
  public Object getObject(String paramString)
    throws SQLException
  {
    return getObject(findColumn(paramString));
  }
  
  public Object getObject(int paramInt, Map<String, Class<?>> paramMap)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public Object getObject(String paramString, Map<String, Class<?>> paramMap)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public Ref getRef(int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public Ref getRef(String paramString)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public RowId getRowId(int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public RowId getRowId(String paramString)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public short getShort(int paramInt)
    throws SQLException
  {
    Object localObject = get(paramInt);
    if ((localObject != null) && (!(localObject instanceof Number))) {
      localObject = Short.decode(localObject.toString());
    }
    return localObject == null ? 0 : ((Number)localObject).shortValue();
  }
  
  public short getShort(String paramString)
    throws SQLException
  {
    return getShort(findColumn(paramString));
  }
  
  public SQLXML getSQLXML(int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public SQLXML getSQLXML(String paramString)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public String getString(int paramInt)
    throws SQLException
  {
    Object localObject = get(paramInt);
    if (localObject == null) {
      return null;
    }
    switch (((Column)this.columns.get(paramInt - 1)).sqlType)
    {
    case 2005: 
      Clob localClob = (Clob)localObject;
      return localClob.getSubString(1L, MathUtils.convertLongToInt(localClob.length()));
    }
    return localObject.toString();
  }
  
  public String getString(String paramString)
    throws SQLException
  {
    return getString(findColumn(paramString));
  }
  
  public Time getTime(int paramInt)
    throws SQLException
  {
    return (Time)get(paramInt);
  }
  
  public Time getTime(String paramString)
    throws SQLException
  {
    return getTime(findColumn(paramString));
  }
  
  public Time getTime(int paramInt, Calendar paramCalendar)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public Time getTime(String paramString, Calendar paramCalendar)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public Timestamp getTimestamp(int paramInt)
    throws SQLException
  {
    return (Timestamp)get(paramInt);
  }
  
  public Timestamp getTimestamp(String paramString)
    throws SQLException
  {
    return getTimestamp(findColumn(paramString));
  }
  
  public Timestamp getTimestamp(int paramInt, Calendar paramCalendar)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public Timestamp getTimestamp(String paramString, Calendar paramCalendar)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  /**
   * @deprecated
   */
  public InputStream getUnicodeStream(int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  /**
   * @deprecated
   */
  public InputStream getUnicodeStream(String paramString)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public URL getURL(int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public URL getURL(String paramString)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public void updateArray(int paramInt, Array paramArray)
    throws SQLException
  {
    update(paramInt, paramArray);
  }
  
  public void updateArray(String paramString, Array paramArray)
    throws SQLException
  {
    update(paramString, paramArray);
  }
  
  public void updateAsciiStream(int paramInt, InputStream paramInputStream)
    throws SQLException
  {
    update(paramInt, paramInputStream);
  }
  
  public void updateAsciiStream(String paramString, InputStream paramInputStream)
    throws SQLException
  {
    update(paramString, paramInputStream);
  }
  
  public void updateAsciiStream(int paramInt1, InputStream paramInputStream, int paramInt2)
    throws SQLException
  {
    update(paramInt1, paramInputStream);
  }
  
  public void updateAsciiStream(String paramString, InputStream paramInputStream, int paramInt)
    throws SQLException
  {
    update(paramString, paramInputStream);
  }
  
  public void updateAsciiStream(int paramInt, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    update(paramInt, paramInputStream);
  }
  
  public void updateAsciiStream(String paramString, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    update(paramString, paramInputStream);
  }
  
  public void updateBigDecimal(int paramInt, BigDecimal paramBigDecimal)
    throws SQLException
  {
    update(paramInt, paramBigDecimal);
  }
  
  public void updateBigDecimal(String paramString, BigDecimal paramBigDecimal)
    throws SQLException
  {
    update(paramString, paramBigDecimal);
  }
  
  public void updateBinaryStream(int paramInt, InputStream paramInputStream)
    throws SQLException
  {
    update(paramInt, paramInputStream);
  }
  
  public void updateBinaryStream(String paramString, InputStream paramInputStream)
    throws SQLException
  {
    update(paramString, paramInputStream);
  }
  
  public void updateBinaryStream(int paramInt1, InputStream paramInputStream, int paramInt2)
    throws SQLException
  {
    update(paramInt1, paramInputStream);
  }
  
  public void updateBinaryStream(String paramString, InputStream paramInputStream, int paramInt)
    throws SQLException
  {
    update(paramString, paramInputStream);
  }
  
  public void updateBinaryStream(int paramInt, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    update(paramInt, paramInputStream);
  }
  
  public void updateBinaryStream(String paramString, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    update(paramString, paramInputStream);
  }
  
  public void updateBlob(int paramInt, Blob paramBlob)
    throws SQLException
  {
    update(paramInt, paramBlob);
  }
  
  public void updateBlob(String paramString, Blob paramBlob)
    throws SQLException
  {
    update(paramString, paramBlob);
  }
  
  public void updateBlob(int paramInt, InputStream paramInputStream)
    throws SQLException
  {
    update(paramInt, paramInputStream);
  }
  
  public void updateBlob(String paramString, InputStream paramInputStream)
    throws SQLException
  {
    update(paramString, paramInputStream);
  }
  
  public void updateBlob(int paramInt, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    update(paramInt, paramInputStream);
  }
  
  public void updateBlob(String paramString, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    update(paramString, paramInputStream);
  }
  
  public void updateBoolean(int paramInt, boolean paramBoolean)
    throws SQLException
  {
    update(paramInt, Boolean.valueOf(paramBoolean));
  }
  
  public void updateBoolean(String paramString, boolean paramBoolean)
    throws SQLException
  {
    update(paramString, Boolean.valueOf(paramBoolean));
  }
  
  public void updateByte(int paramInt, byte paramByte)
    throws SQLException
  {
    update(paramInt, Byte.valueOf(paramByte));
  }
  
  public void updateByte(String paramString, byte paramByte)
    throws SQLException
  {
    update(paramString, Byte.valueOf(paramByte));
  }
  
  public void updateBytes(int paramInt, byte[] paramArrayOfByte)
    throws SQLException
  {
    update(paramInt, paramArrayOfByte);
  }
  
  public void updateBytes(String paramString, byte[] paramArrayOfByte)
    throws SQLException
  {
    update(paramString, paramArrayOfByte);
  }
  
  public void updateCharacterStream(int paramInt, Reader paramReader)
    throws SQLException
  {
    update(paramInt, paramReader);
  }
  
  public void updateCharacterStream(String paramString, Reader paramReader)
    throws SQLException
  {
    update(paramString, paramReader);
  }
  
  public void updateCharacterStream(int paramInt1, Reader paramReader, int paramInt2)
    throws SQLException
  {
    update(paramInt1, paramReader);
  }
  
  public void updateCharacterStream(String paramString, Reader paramReader, int paramInt)
    throws SQLException
  {
    update(paramString, paramReader);
  }
  
  public void updateCharacterStream(int paramInt, Reader paramReader, long paramLong)
    throws SQLException
  {
    update(paramInt, paramReader);
  }
  
  public void updateCharacterStream(String paramString, Reader paramReader, long paramLong)
    throws SQLException
  {
    update(paramString, paramReader);
  }
  
  public void updateClob(int paramInt, Clob paramClob)
    throws SQLException
  {
    update(paramInt, paramClob);
  }
  
  public void updateClob(String paramString, Clob paramClob)
    throws SQLException
  {
    update(paramString, paramClob);
  }
  
  public void updateClob(int paramInt, Reader paramReader)
    throws SQLException
  {
    update(paramInt, paramReader);
  }
  
  public void updateClob(String paramString, Reader paramReader)
    throws SQLException
  {
    update(paramString, paramReader);
  }
  
  public void updateClob(int paramInt, Reader paramReader, long paramLong)
    throws SQLException
  {
    update(paramInt, paramReader);
  }
  
  public void updateClob(String paramString, Reader paramReader, long paramLong)
    throws SQLException
  {
    update(paramString, paramReader);
  }
  
  public void updateDate(int paramInt, Date paramDate)
    throws SQLException
  {
    update(paramInt, paramDate);
  }
  
  public void updateDate(String paramString, Date paramDate)
    throws SQLException
  {
    update(paramString, paramDate);
  }
  
  public void updateDouble(int paramInt, double paramDouble)
    throws SQLException
  {
    update(paramInt, Double.valueOf(paramDouble));
  }
  
  public void updateDouble(String paramString, double paramDouble)
    throws SQLException
  {
    update(paramString, Double.valueOf(paramDouble));
  }
  
  public void updateFloat(int paramInt, float paramFloat)
    throws SQLException
  {
    update(paramInt, Float.valueOf(paramFloat));
  }
  
  public void updateFloat(String paramString, float paramFloat)
    throws SQLException
  {
    update(paramString, Float.valueOf(paramFloat));
  }
  
  public void updateInt(int paramInt1, int paramInt2)
    throws SQLException
  {
    update(paramInt1, Integer.valueOf(paramInt2));
  }
  
  public void updateInt(String paramString, int paramInt)
    throws SQLException
  {
    update(paramString, Integer.valueOf(paramInt));
  }
  
  public void updateLong(int paramInt, long paramLong)
    throws SQLException
  {
    update(paramInt, Long.valueOf(paramLong));
  }
  
  public void updateLong(String paramString, long paramLong)
    throws SQLException
  {
    update(paramString, Long.valueOf(paramLong));
  }
  
  public void updateNCharacterStream(int paramInt, Reader paramReader)
    throws SQLException
  {
    update(paramInt, paramReader);
  }
  
  public void updateNCharacterStream(String paramString, Reader paramReader)
    throws SQLException
  {
    update(paramString, paramReader);
  }
  
  public void updateNCharacterStream(int paramInt, Reader paramReader, long paramLong)
    throws SQLException
  {
    update(paramInt, paramReader);
  }
  
  public void updateNCharacterStream(String paramString, Reader paramReader, long paramLong)
    throws SQLException
  {
    update(paramString, paramReader);
  }
  
  public void updateNClob(int paramInt, NClob paramNClob)
    throws SQLException
  {
    update(paramInt, paramNClob);
  }
  
  public void updateNClob(String paramString, NClob paramNClob)
    throws SQLException
  {
    update(paramString, paramNClob);
  }
  
  public void updateNClob(int paramInt, Reader paramReader)
    throws SQLException
  {
    update(paramInt, paramReader);
  }
  
  public void updateNClob(String paramString, Reader paramReader)
    throws SQLException
  {
    update(paramString, paramReader);
  }
  
  public void updateNClob(int paramInt, Reader paramReader, long paramLong)
    throws SQLException
  {
    update(paramInt, paramReader);
  }
  
  public void updateNClob(String paramString, Reader paramReader, long paramLong)
    throws SQLException
  {
    update(paramString, paramReader);
  }
  
  public void updateNString(int paramInt, String paramString)
    throws SQLException
  {
    update(paramInt, paramString);
  }
  
  public void updateNString(String paramString1, String paramString2)
    throws SQLException
  {
    update(paramString1, paramString2);
  }
  
  public void updateNull(int paramInt)
    throws SQLException
  {
    update(paramInt, null);
  }
  
  public void updateNull(String paramString)
    throws SQLException
  {
    update(paramString, null);
  }
  
  public void updateObject(int paramInt, Object paramObject)
    throws SQLException
  {
    update(paramInt, paramObject);
  }
  
  public void updateObject(String paramString, Object paramObject)
    throws SQLException
  {
    update(paramString, paramObject);
  }
  
  public void updateObject(int paramInt1, Object paramObject, int paramInt2)
    throws SQLException
  {
    update(paramInt1, paramObject);
  }
  
  public void updateObject(String paramString, Object paramObject, int paramInt)
    throws SQLException
  {
    update(paramString, paramObject);
  }
  
  public void updateRef(int paramInt, Ref paramRef)
    throws SQLException
  {
    update(paramInt, paramRef);
  }
  
  public void updateRef(String paramString, Ref paramRef)
    throws SQLException
  {
    update(paramString, paramRef);
  }
  
  public void updateRowId(int paramInt, RowId paramRowId)
    throws SQLException
  {
    update(paramInt, paramRowId);
  }
  
  public void updateRowId(String paramString, RowId paramRowId)
    throws SQLException
  {
    update(paramString, paramRowId);
  }
  
  public void updateShort(int paramInt, short paramShort)
    throws SQLException
  {
    update(paramInt, Short.valueOf(paramShort));
  }
  
  public void updateShort(String paramString, short paramShort)
    throws SQLException
  {
    update(paramString, Short.valueOf(paramShort));
  }
  
  public void updateSQLXML(int paramInt, SQLXML paramSQLXML)
    throws SQLException
  {
    update(paramInt, paramSQLXML);
  }
  
  public void updateSQLXML(String paramString, SQLXML paramSQLXML)
    throws SQLException
  {
    update(paramString, paramSQLXML);
  }
  
  public void updateString(int paramInt, String paramString)
    throws SQLException
  {
    update(paramInt, paramString);
  }
  
  public void updateString(String paramString1, String paramString2)
    throws SQLException
  {
    update(paramString1, paramString2);
  }
  
  public void updateTime(int paramInt, Time paramTime)
    throws SQLException
  {
    update(paramInt, paramTime);
  }
  
  public void updateTime(String paramString, Time paramTime)
    throws SQLException
  {
    update(paramString, paramTime);
  }
  
  public void updateTimestamp(int paramInt, Timestamp paramTimestamp)
    throws SQLException
  {
    update(paramInt, paramTimestamp);
  }
  
  public void updateTimestamp(String paramString, Timestamp paramTimestamp)
    throws SQLException
  {
    update(paramString, paramTimestamp);
  }
  
  public int getColumnCount()
  {
    return this.columns.size();
  }
  
  public int getColumnDisplaySize(int paramInt)
  {
    return 15;
  }
  
  public int getColumnType(int paramInt)
    throws SQLException
  {
    return getColumn(paramInt - 1).sqlType;
  }
  
  public int getPrecision(int paramInt)
    throws SQLException
  {
    return getColumn(paramInt - 1).precision;
  }
  
  public int getScale(int paramInt)
    throws SQLException
  {
    return getColumn(paramInt - 1).scale;
  }
  
  public int isNullable(int paramInt)
  {
    return 2;
  }
  
  public boolean isAutoIncrement(int paramInt)
  {
    return false;
  }
  
  public boolean isCaseSensitive(int paramInt)
  {
    return true;
  }
  
  public boolean isCurrency(int paramInt)
  {
    return false;
  }
  
  public boolean isDefinitelyWritable(int paramInt)
  {
    return false;
  }
  
  public boolean isReadOnly(int paramInt)
  {
    return true;
  }
  
  public boolean isSearchable(int paramInt)
  {
    return true;
  }
  
  public boolean isSigned(int paramInt)
  {
    return true;
  }
  
  public boolean isWritable(int paramInt)
  {
    return false;
  }
  
  public String getCatalogName(int paramInt)
  {
    return null;
  }
  
  public String getColumnClassName(int paramInt)
    throws SQLException
  {
    int i = DataType.getValueTypeFromResultSet(this, paramInt);
    return DataType.getTypeClassName(i);
  }
  
  public String getColumnLabel(int paramInt)
    throws SQLException
  {
    return getColumn(paramInt - 1).name;
  }
  
  public String getColumnName(int paramInt)
    throws SQLException
  {
    return getColumnLabel(paramInt);
  }
  
  public String getColumnTypeName(int paramInt)
    throws SQLException
  {
    return getColumn(paramInt - 1).sqlTypeName;
  }
  
  public String getSchemaName(int paramInt)
  {
    return null;
  }
  
  public String getTableName(int paramInt)
  {
    return null;
  }
  
  public void afterLast()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public void cancelRowUpdates()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public void deleteRow()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public void insertRow()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public void moveToCurrentRow()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public void moveToInsertRow()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public void refreshRow()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public void updateRow()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean first()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean isAfterLast()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean isBeforeFirst()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean isFirst()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean isLast()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean last()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean previous()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean rowDeleted()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean rowInserted()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean rowUpdated()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public void setFetchDirection(int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public void setFetchSize(int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean absolute(int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean relative(int paramInt)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public String getCursorName()
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  private void update(int paramInt, Object paramObject)
    throws SQLException
  {
    checkColumnIndex(paramInt);
    this.currentRow[(paramInt - 1)] = paramObject;
  }
  
  private void update(String paramString, Object paramObject)
    throws SQLException
  {
    this.currentRow[(findColumn(paramString) - 1)] = paramObject;
  }
  
  static SQLException getUnsupportedException()
  {
    return DbException.get(50100).getSQLException();
  }
  
  private void checkColumnIndex(int paramInt)
    throws SQLException
  {
    if ((paramInt < 1) || (paramInt > this.columns.size())) {
      throw DbException.getInvalidValueException("columnIndex", Integer.valueOf(paramInt)).getSQLException();
    }
  }
  
  private Object get(int paramInt)
    throws SQLException
  {
    if (this.currentRow == null) {
      throw DbException.get(2000).getSQLException();
    }
    checkColumnIndex(paramInt);
    paramInt--;
    Object localObject = paramInt < this.currentRow.length ? this.currentRow[paramInt] : null;
    
    this.wasNull = (localObject == null);
    return localObject;
  }
  
  private Column getColumn(int paramInt)
    throws SQLException
  {
    checkColumnIndex(paramInt + 1);
    return (Column)this.columns.get(paramInt);
  }
  
  public int getHoldability()
  {
    return 1;
  }
  
  public boolean isClosed()
  {
    return (this.rows == null) && (this.source == null);
  }
  
  public <T> T unwrap(Class<T> paramClass)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public boolean isWrapperFor(Class<?> paramClass)
    throws SQLException
  {
    throw getUnsupportedException();
  }
  
  public void setAutoClose(boolean paramBoolean)
  {
    this.autoClose = paramBoolean;
  }
  
  public boolean getAutoClose()
  {
    return this.autoClose;
  }
  
  public static class SimpleArray
    implements Array
  {
    private final Object[] value;
    
    SimpleArray(Object[] paramArrayOfObject)
    {
      this.value = paramArrayOfObject;
    }
    
    public Object getArray()
    {
      return this.value;
    }
    
    public Object getArray(Map<String, Class<?>> paramMap)
      throws SQLException
    {
      throw SimpleResultSet.getUnsupportedException();
    }
    
    public Object getArray(long paramLong, int paramInt)
      throws SQLException
    {
      throw SimpleResultSet.getUnsupportedException();
    }
    
    public Object getArray(long paramLong, int paramInt, Map<String, Class<?>> paramMap)
      throws SQLException
    {
      throw SimpleResultSet.getUnsupportedException();
    }
    
    public int getBaseType()
    {
      return 0;
    }
    
    public String getBaseTypeName()
    {
      return "NULL";
    }
    
    public ResultSet getResultSet()
      throws SQLException
    {
      throw SimpleResultSet.getUnsupportedException();
    }
    
    public ResultSet getResultSet(Map<String, Class<?>> paramMap)
      throws SQLException
    {
      throw SimpleResultSet.getUnsupportedException();
    }
    
    public ResultSet getResultSet(long paramLong, int paramInt)
      throws SQLException
    {
      throw SimpleResultSet.getUnsupportedException();
    }
    
    public ResultSet getResultSet(long paramLong, int paramInt, Map<String, Class<?>> paramMap)
      throws SQLException
    {
      throw SimpleResultSet.getUnsupportedException();
    }
    
    public void free() {}
  }
  
  static class Column
  {
    String name;
    String sqlTypeName;
    int sqlType;
    int precision;
    int scale;
  }
}
