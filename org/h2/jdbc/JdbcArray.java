package org.h2.jdbc;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.h2.engine.SessionInterface;
import org.h2.message.DbException;
import org.h2.message.TraceObject;
import org.h2.tools.SimpleResultSet;
import org.h2.value.Value;

public class JdbcArray
  extends TraceObject
  implements Array
{
  private Value value;
  private final JdbcConnection conn;
  
  JdbcArray(JdbcConnection paramJdbcConnection, Value paramValue, int paramInt)
  {
    setTrace(paramJdbcConnection.getSession().getTrace(), 16, paramInt);
    this.conn = paramJdbcConnection;
    this.value = paramValue;
  }
  
  public Object getArray()
    throws SQLException
  {
    try
    {
      debugCodeCall("getArray");
      checkClosed();
      return get();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Object getArray(Map<String, Class<?>> paramMap)
    throws SQLException
  {
    try
    {
      debugCode("getArray(" + quoteMap(paramMap) + ");");
      JdbcConnection.checkMap(paramMap);
      checkClosed();
      return get();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Object getArray(long paramLong, int paramInt)
    throws SQLException
  {
    try
    {
      debugCode("getArray(" + paramLong + ", " + paramInt + ");");
      checkClosed();
      return get(paramLong, paramInt);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Object getArray(long paramLong, int paramInt, Map<String, Class<?>> paramMap)
    throws SQLException
  {
    try
    {
      debugCode("getArray(" + paramLong + ", " + paramInt + ", " + quoteMap(paramMap) + ");");
      checkClosed();
      JdbcConnection.checkMap(paramMap);
      return get(paramLong, paramInt);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getBaseType()
    throws SQLException
  {
    try
    {
      debugCodeCall("getBaseType");
      checkClosed();
      return 0;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getBaseTypeName()
    throws SQLException
  {
    try
    {
      debugCodeCall("getBaseTypeName");
      checkClosed();
      return "NULL";
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getResultSet()
    throws SQLException
  {
    try
    {
      debugCodeCall("getResultSet");
      checkClosed();
      return getResultSet(get(), 0L);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getResultSet(Map<String, Class<?>> paramMap)
    throws SQLException
  {
    try
    {
      debugCode("getResultSet(" + quoteMap(paramMap) + ");");
      checkClosed();
      JdbcConnection.checkMap(paramMap);
      return getResultSet(get(), 0L);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getResultSet(long paramLong, int paramInt)
    throws SQLException
  {
    try
    {
      debugCode("getResultSet(" + paramLong + ", " + paramInt + ");");
      checkClosed();
      return getResultSet(get(paramLong, paramInt), paramLong - 1L);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getResultSet(long paramLong, int paramInt, Map<String, Class<?>> paramMap)
    throws SQLException
  {
    try
    {
      debugCode("getResultSet(" + paramLong + ", " + paramInt + ", " + quoteMap(paramMap) + ");");
      checkClosed();
      JdbcConnection.checkMap(paramMap);
      return getResultSet(get(paramLong, paramInt), paramLong - 1L);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void free()
  {
    debugCodeCall("free");
    this.value = null;
  }
  
  private static ResultSet getResultSet(Object[] paramArrayOfObject, long paramLong)
  {
    SimpleResultSet localSimpleResultSet = new SimpleResultSet();
    localSimpleResultSet.addColumn("INDEX", -5, 0, 0);
    
    localSimpleResultSet.addColumn("VALUE", 0, 0, 0);
    for (int i = 0; i < paramArrayOfObject.length; i++) {
      localSimpleResultSet.addRow(new Object[] { Long.valueOf(paramLong + i + 1L), paramArrayOfObject[i] });
    }
    return localSimpleResultSet;
  }
  
  private void checkClosed()
  {
    this.conn.checkClosed();
    if (this.value == null) {
      throw DbException.get(90007);
    }
  }
  
  private Object[] get()
  {
    return (Object[])this.value.convertTo(17).getObject();
  }
  
  private Object[] get(long paramLong, int paramInt)
  {
    Object[] arrayOfObject1 = get();
    if ((paramInt < 0) || (paramInt > arrayOfObject1.length)) {
      throw DbException.getInvalidValueException("count (1.." + arrayOfObject1.length + ")", Integer.valueOf(paramInt));
    }
    if ((paramLong < 1L) || (paramLong > arrayOfObject1.length)) {
      throw DbException.getInvalidValueException("index (1.." + arrayOfObject1.length + ")", Long.valueOf(paramLong));
    }
    Object[] arrayOfObject2 = new Object[paramInt];
    System.arraycopy(arrayOfObject1, (int)(paramLong - 1L), arrayOfObject2, 0, paramInt);
    return arrayOfObject2;
  }
  
  public String toString()
  {
    return getTraceObjectName() + ": " + this.value.getTraceSQL();
  }
}
