package org.h2.jdbc;

import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import org.h2.command.CommandInterface;
import org.h2.expression.ParameterInterface;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.message.TraceObject;
import org.h2.util.MathUtils;
import org.h2.value.DataType;

public class JdbcParameterMetaData
  extends TraceObject
  implements ParameterMetaData
{
  private final JdbcPreparedStatement prep;
  private final int paramCount;
  private final ArrayList<? extends ParameterInterface> parameters;
  
  JdbcParameterMetaData(Trace paramTrace, JdbcPreparedStatement paramJdbcPreparedStatement, CommandInterface paramCommandInterface, int paramInt)
  {
    setTrace(paramTrace, 11, paramInt);
    this.prep = paramJdbcPreparedStatement;
    this.parameters = paramCommandInterface.getParameters();
    this.paramCount = this.parameters.size();
  }
  
  public int getParameterCount()
    throws SQLException
  {
    try
    {
      debugCodeCall("getParameterCount");
      checkClosed();
      return this.paramCount;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getParameterMode(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getParameterMode", paramInt);
      getParameter(paramInt);
      return 1;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getParameterType(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getParameterType", paramInt);
      ParameterInterface localParameterInterface = getParameter(paramInt);
      int i = localParameterInterface.getType();
      if (i == -1) {
        i = 13;
      }
      return DataType.getDataType(i).sqlType;
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
      ParameterInterface localParameterInterface = getParameter(paramInt);
      return MathUtils.convertLongToInt(localParameterInterface.getPrecision());
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
      ParameterInterface localParameterInterface = getParameter(paramInt);
      return localParameterInterface.getScale();
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
      return getParameter(paramInt).getNullable();
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
      getParameter(paramInt);
      return true;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getParameterClassName(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getParameterClassName", paramInt);
      ParameterInterface localParameterInterface = getParameter(paramInt);
      int i = localParameterInterface.getType();
      if (i == -1) {
        i = 13;
      }
      return DataType.getTypeClassName(i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getParameterTypeName(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getParameterTypeName", paramInt);
      ParameterInterface localParameterInterface = getParameter(paramInt);
      int i = localParameterInterface.getType();
      if (i == -1) {
        i = 13;
      }
      return DataType.getDataType(i).name;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  private ParameterInterface getParameter(int paramInt)
  {
    checkClosed();
    if ((paramInt < 1) || (paramInt > this.paramCount)) {
      throw DbException.getInvalidValueException("param", Integer.valueOf(paramInt));
    }
    return (ParameterInterface)this.parameters.get(paramInt - 1);
  }
  
  private void checkClosed()
  {
    this.prep.checkClosed();
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
    return getTraceObjectName() + ": parameterCount=" + this.paramCount;
  }
}
