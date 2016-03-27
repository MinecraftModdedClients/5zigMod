package org.h2.jdbcx;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import org.h2.message.DbException;
import org.h2.util.New;

public class JdbcConnectionPool
  implements DataSource, ConnectionEventListener
{
  private static final int DEFAULT_TIMEOUT = 30;
  private static final int DEFAULT_MAX_CONNECTIONS = 10;
  private final ConnectionPoolDataSource dataSource;
  private final ArrayList<PooledConnection> recycledConnections = New.arrayList();
  private PrintWriter logWriter;
  private int maxConnections = 10;
  private int timeout = 30;
  private int activeConnections;
  private boolean isDisposed;
  
  protected JdbcConnectionPool(ConnectionPoolDataSource paramConnectionPoolDataSource)
  {
    this.dataSource = paramConnectionPoolDataSource;
    if (paramConnectionPoolDataSource != null) {
      try
      {
        this.logWriter = paramConnectionPoolDataSource.getLogWriter();
      }
      catch (SQLException localSQLException) {}
    }
  }
  
  public static JdbcConnectionPool create(ConnectionPoolDataSource paramConnectionPoolDataSource)
  {
    return new JdbcConnectionPool(paramConnectionPoolDataSource);
  }
  
  public static JdbcConnectionPool create(String paramString1, String paramString2, String paramString3)
  {
    JdbcDataSource localJdbcDataSource = new JdbcDataSource();
    localJdbcDataSource.setURL(paramString1);
    localJdbcDataSource.setUser(paramString2);
    localJdbcDataSource.setPassword(paramString3);
    return new JdbcConnectionPool(localJdbcDataSource);
  }
  
  public synchronized void setMaxConnections(int paramInt)
  {
    if (paramInt < 1) {
      throw new IllegalArgumentException("Invalid maxConnections value: " + paramInt);
    }
    this.maxConnections = paramInt;
    
    notifyAll();
  }
  
  public synchronized int getMaxConnections()
  {
    return this.maxConnections;
  }
  
  public synchronized int getLoginTimeout()
  {
    return this.timeout;
  }
  
  public synchronized void setLoginTimeout(int paramInt)
  {
    if (paramInt == 0) {
      paramInt = 30;
    }
    this.timeout = paramInt;
  }
  
  public synchronized void dispose()
  {
    if (this.isDisposed) {
      return;
    }
    this.isDisposed = true;
    ArrayList localArrayList = this.recycledConnections;
    int i = 0;
    for (int j = localArrayList.size(); i < j; i++) {
      closeConnection((PooledConnection)localArrayList.get(i));
    }
  }
  
  public Connection getConnection()
    throws SQLException
  {
    long l = System.currentTimeMillis() + this.timeout * 1000;
    do
    {
      synchronized (this)
      {
        if (this.activeConnections < this.maxConnections) {
          return getConnectionNow();
        }
        try
        {
          wait(1000L);
        }
        catch (InterruptedException localInterruptedException) {}
      }
    } while (System.currentTimeMillis() <= l);
    throw new SQLException("Login timeout", "08001", 8001);
  }
  
  public Connection getConnection(String paramString1, String paramString2)
  {
    throw new UnsupportedOperationException();
  }
  
  private Connection getConnectionNow()
    throws SQLException
  {
    if (this.isDisposed) {
      throw new IllegalStateException("Connection pool has been disposed.");
    }
    PooledConnection localPooledConnection;
    if (!this.recycledConnections.isEmpty()) {
      localPooledConnection = (PooledConnection)this.recycledConnections.remove(this.recycledConnections.size() - 1);
    } else {
      localPooledConnection = this.dataSource.getPooledConnection();
    }
    Connection localConnection = localPooledConnection.getConnection();
    this.activeConnections += 1;
    localPooledConnection.addConnectionEventListener(this);
    return localConnection;
  }
  
  synchronized void recycleConnection(PooledConnection paramPooledConnection)
  {
    if (this.activeConnections <= 0) {
      throw new AssertionError();
    }
    this.activeConnections -= 1;
    if ((!this.isDisposed) && (this.activeConnections < this.maxConnections)) {
      this.recycledConnections.add(paramPooledConnection);
    } else {
      closeConnection(paramPooledConnection);
    }
    if (this.activeConnections >= this.maxConnections - 1) {
      notifyAll();
    }
  }
  
  private void closeConnection(PooledConnection paramPooledConnection)
  {
    try
    {
      paramPooledConnection.close();
    }
    catch (SQLException localSQLException)
    {
      if (this.logWriter != null) {
        localSQLException.printStackTrace(this.logWriter);
      }
    }
  }
  
  public void connectionClosed(ConnectionEvent paramConnectionEvent)
  {
    PooledConnection localPooledConnection = (PooledConnection)paramConnectionEvent.getSource();
    localPooledConnection.removeConnectionEventListener(this);
    recycleConnection(localPooledConnection);
  }
  
  public void connectionErrorOccurred(ConnectionEvent paramConnectionEvent) {}
  
  public synchronized int getActiveConnections()
  {
    return this.activeConnections;
  }
  
  public PrintWriter getLogWriter()
  {
    return this.logWriter;
  }
  
  public void setLogWriter(PrintWriter paramPrintWriter)
  {
    this.logWriter = paramPrintWriter;
  }
  
  public <T> T unwrap(Class<T> paramClass)
    throws SQLException
  {
    throw DbException.getUnsupportedException("unwrap");
  }
  
  public boolean isWrapperFor(Class<?> paramClass)
    throws SQLException
  {
    throw DbException.getUnsupportedException("isWrapperFor");
  }
}
