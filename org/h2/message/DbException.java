package org.h2.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import org.h2.api.ErrorCode;
import org.h2.engine.Constants;
import org.h2.jdbc.JdbcSQLException;
import org.h2.util.SortedProperties;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class DbException
  extends RuntimeException
{
  private static final long serialVersionUID = 1L;
  private static final Properties MESSAGES = new Properties();
  private Object source;
  
  static
  {
    try
    {
      byte[] arrayOfByte1 = Utils.getResource("/org/h2/res/_messages_en.prop");
      if (arrayOfByte1 != null) {
        MESSAGES.load(new ByteArrayInputStream(arrayOfByte1));
      }
      String str1 = Locale.getDefault().getLanguage();
      if (!"en".equals(str1))
      {
        byte[] arrayOfByte2 = Utils.getResource("/org/h2/res/_messages_" + str1 + ".prop");
        if (arrayOfByte2 != null)
        {
          SortedProperties localSortedProperties = SortedProperties.fromLines(new String(arrayOfByte2, Constants.UTF8));
          for (Map.Entry localEntry : localSortedProperties.entrySet())
          {
            String str2 = (String)localEntry.getKey();
            String str3 = (String)localEntry.getValue();
            if ((str3 != null) && (!str3.startsWith("#")))
            {
              String str4 = MESSAGES.getProperty(str2);
              String str5 = str3 + "\n" + str4;
              MESSAGES.put(str2, str5);
            }
          }
        }
      }
    }
    catch (OutOfMemoryError localOutOfMemoryError)
    {
      traceThrowable(localOutOfMemoryError);
    }
    catch (IOException localIOException)
    {
      traceThrowable(localIOException);
    }
  }
  
  private DbException(SQLException paramSQLException)
  {
    super(paramSQLException.getMessage(), paramSQLException);
  }
  
  private static String translate(String paramString, String... paramVarArgs)
  {
    String str1 = null;
    if (MESSAGES != null) {
      str1 = MESSAGES.getProperty(paramString);
    }
    if (str1 == null) {
      str1 = "(Message " + paramString + " not found)";
    }
    if (paramVarArgs != null)
    {
      for (int i = 0; i < paramVarArgs.length; i++)
      {
        String str2 = paramVarArgs[i];
        if ((str2 != null) && (str2.length() > 0)) {
          paramVarArgs[i] = StringUtils.quoteIdentifier(str2);
        }
      }
      str1 = MessageFormat.format(str1, (Object[])paramVarArgs);
    }
    return str1;
  }
  
  public SQLException getSQLException()
  {
    return (SQLException)getCause();
  }
  
  public int getErrorCode()
  {
    return getSQLException().getErrorCode();
  }
  
  public DbException addSQL(String paramString)
  {
    Object localObject = getSQLException();
    if ((localObject instanceof JdbcSQLException))
    {
      JdbcSQLException localJdbcSQLException = (JdbcSQLException)localObject;
      if (localJdbcSQLException.getSQL() == null) {
        localJdbcSQLException.setSQL(paramString);
      }
      return this;
    }
    localObject = new JdbcSQLException(((SQLException)localObject).getMessage(), paramString, ((SQLException)localObject).getSQLState(), ((SQLException)localObject).getErrorCode(), (Throwable)localObject, null);
    
    return new DbException((SQLException)localObject);
  }
  
  public static DbException get(int paramInt)
  {
    return get(paramInt, (String)null);
  }
  
  public static DbException get(int paramInt, String paramString)
  {
    return get(paramInt, new String[] { paramString });
  }
  
  public static DbException get(int paramInt, Throwable paramThrowable, String... paramVarArgs)
  {
    return new DbException(getJdbcSQLException(paramInt, paramThrowable, paramVarArgs));
  }
  
  public static DbException get(int paramInt, String... paramVarArgs)
  {
    return new DbException(getJdbcSQLException(paramInt, null, paramVarArgs));
  }
  
  public static DbException getSyntaxError(String paramString, int paramInt)
  {
    paramString = StringUtils.addAsterisk(paramString, paramInt);
    return get(42000, paramString);
  }
  
  public static DbException getSyntaxError(String paramString1, int paramInt, String paramString2)
  {
    paramString1 = StringUtils.addAsterisk(paramString1, paramInt);
    return new DbException(getJdbcSQLException(42001, null, new String[] { paramString1, paramString2 }));
  }
  
  public static DbException getUnsupportedException(String paramString)
  {
    return get(50100, paramString);
  }
  
  public static DbException getInvalidValueException(String paramString, Object paramObject)
  {
    return get(90008, new String[] { paramObject == null ? "null" : paramObject.toString(), paramString });
  }
  
  public static RuntimeException throwInternalError(String paramString)
  {
    RuntimeException localRuntimeException = new RuntimeException(paramString);
    traceThrowable(localRuntimeException);
    throw localRuntimeException;
  }
  
  public static RuntimeException throwInternalError()
  {
    return throwInternalError("Unexpected code path");
  }
  
  public static SQLException toSQLException(Exception paramException)
  {
    if ((paramException instanceof SQLException)) {
      return (SQLException)paramException;
    }
    return convert(paramException).getSQLException();
  }
  
  public static DbException convert(Throwable paramThrowable)
  {
    if ((paramThrowable instanceof DbException)) {
      return (DbException)paramThrowable;
    }
    if ((paramThrowable instanceof SQLException)) {
      return new DbException((SQLException)paramThrowable);
    }
    if ((paramThrowable instanceof InvocationTargetException)) {
      return convertInvocation((InvocationTargetException)paramThrowable, null);
    }
    if ((paramThrowable instanceof IOException)) {
      return get(90028, paramThrowable, new String[] { paramThrowable.toString() });
    }
    if ((paramThrowable instanceof OutOfMemoryError)) {
      return get(90108, paramThrowable, new String[0]);
    }
    if (((paramThrowable instanceof StackOverflowError)) || ((paramThrowable instanceof LinkageError))) {
      return get(50000, paramThrowable, new String[] { paramThrowable.toString() });
    }
    if ((paramThrowable instanceof Error)) {
      throw ((Error)paramThrowable);
    }
    return get(50000, paramThrowable, new String[] { paramThrowable.toString() });
  }
  
  public static DbException convertInvocation(InvocationTargetException paramInvocationTargetException, String paramString)
  {
    Throwable localThrowable = paramInvocationTargetException.getTargetException();
    if (((localThrowable instanceof SQLException)) || ((localThrowable instanceof DbException))) {
      return convert(localThrowable);
    }
    paramString = paramString + ": " + localThrowable.getMessage();
    return get(90105, localThrowable, new String[] { paramString });
  }
  
  public static DbException convertIOException(IOException paramIOException, String paramString)
  {
    if (paramString == null)
    {
      Throwable localThrowable = paramIOException.getCause();
      if ((localThrowable instanceof DbException)) {
        return (DbException)localThrowable;
      }
      return get(90028, paramIOException, new String[] { paramIOException.toString() });
    }
    return get(90031, paramIOException, new String[] { paramIOException.toString(), paramString });
  }
  
  private static JdbcSQLException getJdbcSQLException(int paramInt, Throwable paramThrowable, String... paramVarArgs)
  {
    String str1 = ErrorCode.getState(paramInt);
    String str2 = translate(str1, paramVarArgs);
    return new JdbcSQLException(str2, null, str1, paramInt, paramThrowable, null);
  }
  
  public static IOException convertToIOException(Throwable paramThrowable)
  {
    if ((paramThrowable instanceof IOException)) {
      return (IOException)paramThrowable;
    }
    if ((paramThrowable instanceof JdbcSQLException))
    {
      JdbcSQLException localJdbcSQLException = (JdbcSQLException)paramThrowable;
      if (localJdbcSQLException.getOriginalCause() != null) {
        paramThrowable = localJdbcSQLException.getOriginalCause();
      }
    }
    return new IOException(paramThrowable.toString(), paramThrowable);
  }
  
  public Object getSource()
  {
    return this.source;
  }
  
  public void setSource(Object paramObject)
  {
    this.source = paramObject;
  }
  
  public static void traceThrowable(Throwable paramThrowable)
  {
    PrintWriter localPrintWriter = DriverManager.getLogWriter();
    if (localPrintWriter != null) {
      paramThrowable.printStackTrace(localPrintWriter);
    }
  }
}
