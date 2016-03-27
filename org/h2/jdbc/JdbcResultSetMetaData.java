package org.h2.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.message.TraceObject;
import org.h2.result.ResultInterface;
import org.h2.util.MathUtils;
import org.h2.value.DataType;

public class JdbcResultSetMetaData
  extends TraceObject
  implements ResultSetMetaData
{
  private final String catalog;
  private final JdbcResultSet rs;
  private final JdbcPreparedStatement prep;
  private final ResultInterface result;
  private final int columnCount;
  
  JdbcResultSetMetaData(JdbcResultSet paramJdbcResultSet, JdbcPreparedStatement paramJdbcPreparedStatement, ResultInterface paramResultInterface, String paramString, Trace paramTrace, int paramInt)
  {
    setTrace(paramTrace, 5, paramInt);
    this.catalog = paramString;
    this.rs = paramJdbcResultSet;
    this.prep = paramJdbcPreparedStatement;
    this.result = paramResultInterface;
    this.columnCount = paramResultInterface.getVisibleColumnCount();
  }
  
  public int getColumnCount()
    throws SQLException
  {
    try
    {
      debugCodeCall("getColumnCount");
      checkClosed();
      return this.columnCount;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getColumnLabel(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getColumnLabel", paramInt);
      checkColumnIndex(paramInt);
      return this.result.getAlias(--paramInt);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getColumnName(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getColumnName", paramInt);
      checkColumnIndex(paramInt);
      return this.result.getColumnName(--paramInt);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getColumnType(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getColumnType", paramInt);
      checkColumnIndex(paramInt);
      int i = this.result.getColumnType(--paramInt);
      return DataType.convertTypeToSQLType(i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getColumnTypeName(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getColumnTypeName", paramInt);
      checkColumnIndex(paramInt);
      int i = this.result.getColumnType(--paramInt);
      return DataType.getDataType(i).name;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getSchemaName(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getSchemaName", paramInt);
      checkColumnIndex(paramInt);
      String str = this.result.getSchemaName(--paramInt);
      return str == null ? "" : str;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getTableName(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getTableName", paramInt);
      checkColumnIndex(paramInt);
      String str = this.result.getTableName(--paramInt);
      return str == null ? "" : str;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getCatalogName(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getCatalogName", paramInt);
      checkColumnIndex(paramInt);
      return this.catalog == null ? "" : this.catalog;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isAutoIncrement(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("isAutoIncrement", paramInt);
      checkColumnIndex(paramInt);
      return this.result.isAutoIncrement(--paramInt);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isCaseSensitive(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("isCaseSensitive", paramInt);
      checkColumnIndex(paramInt);
      return true;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isSearchable(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("isSearchable", paramInt);
      checkColumnIndex(paramInt);
      return true;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isCurrency(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("isCurrency", paramInt);
      checkColumnIndex(paramInt);
      return false;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int isNullable(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("isNullable", paramInt);
      checkColumnIndex(paramInt);
      return this.result.getNullable(--paramInt);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isSigned(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("isSigned", paramInt);
      checkColumnIndex(paramInt);
      return true;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isReadOnly(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("isReadOnly", paramInt);
      checkColumnIndex(paramInt);
      return false;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isWritable(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("isWritable", paramInt);
      checkColumnIndex(paramInt);
      return true;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isDefinitelyWritable(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("isDefinitelyWritable", paramInt);
      checkColumnIndex(paramInt);
      return false;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getColumnClassName(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getColumnClassName", paramInt);
      checkColumnIndex(paramInt);
      int i = this.result.getColumnType(--paramInt);
      return DataType.getTypeClassName(i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getPrecision(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getPrecision", paramInt);
      checkColumnIndex(paramInt);
      long l = this.result.getColumnPrecision(--paramInt);
      return MathUtils.convertLongToInt(l);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getScale(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getScale", paramInt);
      checkColumnIndex(paramInt);
      return this.result.getColumnScale(--paramInt);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getColumnDisplaySize(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getColumnDisplaySize", paramInt);
      checkColumnIndex(paramInt);
      return this.result.getDisplaySize(--paramInt);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  private void checkClosed()
  {
    if (this.rs != null) {
      this.rs.checkClosed();
    }
    if (this.prep != null) {
      this.prep.checkClosed();
    }
  }
  
  private void checkColumnIndex(int paramInt)
  {
    checkClosed();
    if ((paramInt < 1) || (paramInt > this.columnCount)) {
      throw DbException.getInvalidValueException("columnIndex", Integer.valueOf(paramInt));
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
    return getTraceObjectName() + ": columns=" + this.columnCount;
  }
}
