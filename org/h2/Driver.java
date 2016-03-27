package org.h2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.DbException;
import org.h2.upgrade.DbUpgrade;

public class Driver
  implements java.sql.Driver
{
  private static final Driver INSTANCE = new Driver();
  private static final String DEFAULT_URL = "jdbc:default:connection";
  private static final ThreadLocal<Connection> DEFAULT_CONNECTION = new ThreadLocal();
  private static volatile boolean registered;
  
  static
  {
    load();
  }
  
  public Connection connect(String paramString, Properties paramProperties)
    throws SQLException
  {
    try
    {
      if (paramProperties == null) {
        paramProperties = new Properties();
      }
      if (!acceptsURL(paramString)) {
        return null;
      }
      if (paramString.equals("jdbc:default:connection")) {
        return (Connection)DEFAULT_CONNECTION.get();
      }
      Connection localConnection = DbUpgrade.connectOrUpgrade(paramString, paramProperties);
      if (localConnection != null) {
        return localConnection;
      }
      return new JdbcConnection(paramString, paramProperties);
    }
    catch (Exception localException)
    {
      throw DbException.toSQLException(localException);
    }
  }
  
  public boolean acceptsURL(String paramString)
  {
    if (paramString != null)
    {
      if (paramString.startsWith("jdbc:h2:")) {
        return true;
      }
      if (paramString.equals("jdbc:default:connection")) {
        return DEFAULT_CONNECTION.get() != null;
      }
    }
    return false;
  }
  
  public int getMajorVersion()
  {
    return 1;
  }
  
  public int getMinorVersion()
  {
    return 4;
  }
  
  public DriverPropertyInfo[] getPropertyInfo(String paramString, Properties paramProperties)
  {
    return new DriverPropertyInfo[0];
  }
  
  public boolean jdbcCompliant()
  {
    return true;
  }
  
  public static synchronized Driver load()
  {
    try
    {
      if (!registered)
      {
        registered = true;
        DriverManager.registerDriver(INSTANCE);
      }
    }
    catch (SQLException localSQLException)
    {
      DbException.traceThrowable(localSQLException);
    }
    return INSTANCE;
  }
  
  public static synchronized void unload()
  {
    try
    {
      if (registered)
      {
        registered = false;
        DriverManager.deregisterDriver(INSTANCE);
      }
    }
    catch (SQLException localSQLException)
    {
      DbException.traceThrowable(localSQLException);
    }
  }
  
  public static void setDefaultConnection(Connection paramConnection)
  {
    if (paramConnection == null) {
      DEFAULT_CONNECTION.remove();
    } else {
      DEFAULT_CONNECTION.set(paramConnection);
    }
  }
  
  public static void setThreadContextClassLoader(Thread paramThread)
  {
    try
    {
      paramThread.setContextClassLoader(Driver.class.getClassLoader());
    }
    catch (Throwable localThrowable) {}
  }
}
