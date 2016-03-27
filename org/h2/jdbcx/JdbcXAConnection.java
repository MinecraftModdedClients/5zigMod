package org.h2.jdbcx;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.h2.Driver;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.DbException;
import org.h2.message.TraceObject;
import org.h2.util.JdbcUtils;
import org.h2.util.New;

public class JdbcXAConnection
  extends TraceObject
  implements XAConnection, XAResource
{
  private final JdbcDataSourceFactory factory;
  private JdbcConnection physicalConn;
  private volatile Connection handleConn;
  private final ArrayList<ConnectionEventListener> listeners = New.arrayList();
  private Xid currentTransaction;
  private boolean prepared;
  
  static
  {
    Driver.load();
  }
  
  JdbcXAConnection(JdbcDataSourceFactory paramJdbcDataSourceFactory, int paramInt, JdbcConnection paramJdbcConnection)
  {
    this.factory = paramJdbcDataSourceFactory;
    setTrace(paramJdbcDataSourceFactory.getTrace(), 13, paramInt);
    this.physicalConn = paramJdbcConnection;
  }
  
  public XAResource getXAResource()
  {
    debugCodeCall("getXAResource");
    return this;
  }
  
  public void close()
    throws SQLException
  {
    debugCodeCall("close");
    Connection localConnection = this.handleConn;
    if (localConnection != null)
    {
      this.listeners.clear();
      localConnection.close();
    }
    if (this.physicalConn != null) {
      try
      {
        this.physicalConn.close();
      }
      finally
      {
        this.physicalConn = null;
      }
    }
  }
  
  public Connection getConnection()
    throws SQLException
  {
    debugCodeCall("getConnection");
    Connection localConnection = this.handleConn;
    if (localConnection != null) {
      localConnection.close();
    }
    this.physicalConn.rollback();
    this.handleConn = new PooledJdbcConnection(this.physicalConn);
    return this.handleConn;
  }
  
  public void addConnectionEventListener(ConnectionEventListener paramConnectionEventListener)
  {
    debugCode("addConnectionEventListener(listener);");
    this.listeners.add(paramConnectionEventListener);
  }
  
  public void removeConnectionEventListener(ConnectionEventListener paramConnectionEventListener)
  {
    debugCode("removeConnectionEventListener(listener);");
    this.listeners.remove(paramConnectionEventListener);
  }
  
  void closedHandle()
  {
    debugCode("closedHandle();");
    ConnectionEvent localConnectionEvent = new ConnectionEvent(this);
    for (int i = this.listeners.size() - 1; i >= 0; i--)
    {
      ConnectionEventListener localConnectionEventListener = (ConnectionEventListener)this.listeners.get(i);
      localConnectionEventListener.connectionClosed(localConnectionEvent);
    }
    this.handleConn = null;
  }
  
  public int getTransactionTimeout()
  {
    debugCodeCall("getTransactionTimeout");
    return 0;
  }
  
  public boolean setTransactionTimeout(int paramInt)
  {
    debugCodeCall("setTransactionTimeout", paramInt);
    return false;
  }
  
  public boolean isSameRM(XAResource paramXAResource)
  {
    debugCode("isSameRM(xares);");
    return paramXAResource == this;
  }
  
  public Xid[] recover(int paramInt)
    throws XAException
  {
    debugCodeCall("recover", quoteFlags(paramInt));
    checkOpen();
    Statement localStatement = null;
    try
    {
      localStatement = this.physicalConn.createStatement();
      ResultSet localResultSet = localStatement.executeQuery("SELECT * FROM INFORMATION_SCHEMA.IN_DOUBT ORDER BY TRANSACTION");
      
      localObject1 = New.arrayList();
      while (localResultSet.next())
      {
        localObject2 = localResultSet.getString("TRANSACTION");
        int i = getNextId(15);
        JdbcXid localJdbcXid = new JdbcXid(this.factory, i, (String)localObject2);
        ((ArrayList)localObject1).add(localJdbcXid);
      }
      localResultSet.close();
      Object localObject2 = new Xid[((ArrayList)localObject1).size()];
      ((ArrayList)localObject1).toArray((Object[])localObject2);
      if (((ArrayList)localObject1).size() > 0) {
        this.prepared = true;
      }
      return (Xid[])localObject2;
    }
    catch (SQLException localSQLException)
    {
      Object localObject1 = new XAException(-3);
      ((XAException)localObject1).initCause(localSQLException);
      throw ((Throwable)localObject1);
    }
    finally
    {
      JdbcUtils.closeSilently(localStatement);
    }
  }
  
  public int prepare(Xid paramXid)
    throws XAException
  {
    if (isDebugEnabled()) {
      debugCode("prepare(" + JdbcXid.toString(paramXid) + ");");
    }
    checkOpen();
    if (!this.currentTransaction.equals(paramXid)) {
      throw new XAException(-5);
    }
    Statement localStatement = null;
    try
    {
      localStatement = this.physicalConn.createStatement();
      localStatement.execute("PREPARE COMMIT " + JdbcXid.toString(paramXid));
      this.prepared = true;
    }
    catch (SQLException localSQLException)
    {
      throw convertException(localSQLException);
    }
    finally
    {
      JdbcUtils.closeSilently(localStatement);
    }
    return 0;
  }
  
  public void forget(Xid paramXid)
  {
    if (isDebugEnabled()) {
      debugCode("forget(" + JdbcXid.toString(paramXid) + ");");
    }
    this.prepared = false;
  }
  
  public void rollback(Xid paramXid)
    throws XAException
  {
    if (isDebugEnabled()) {
      debugCode("rollback(" + JdbcXid.toString(paramXid) + ");");
    }
    try
    {
      this.physicalConn.rollback();
      this.physicalConn.setAutoCommit(true);
      if (this.prepared)
      {
        Statement localStatement = null;
        try
        {
          localStatement = this.physicalConn.createStatement();
          localStatement.execute("ROLLBACK TRANSACTION " + JdbcXid.toString(paramXid));
        }
        finally
        {
          JdbcUtils.closeSilently(localStatement);
        }
        this.prepared = false;
      }
    }
    catch (SQLException localSQLException)
    {
      throw convertException(localSQLException);
    }
    this.currentTransaction = null;
  }
  
  public void end(Xid paramXid, int paramInt)
    throws XAException
  {
    if (isDebugEnabled()) {
      debugCode("end(" + JdbcXid.toString(paramXid) + ", " + quoteFlags(paramInt) + ");");
    }
    if (paramInt == 33554432) {
      return;
    }
    if (!this.currentTransaction.equals(paramXid)) {
      throw new XAException(-9);
    }
    this.prepared = false;
  }
  
  public void start(Xid paramXid, int paramInt)
    throws XAException
  {
    if (isDebugEnabled()) {
      debugCode("start(" + JdbcXid.toString(paramXid) + ", " + quoteFlags(paramInt) + ");");
    }
    if (paramInt == 134217728) {
      return;
    }
    if (paramInt == 2097152)
    {
      if ((this.currentTransaction != null) && (!this.currentTransaction.equals(paramXid))) {
        throw new XAException(-3);
      }
    }
    else if (this.currentTransaction != null) {
      throw new XAException(-4);
    }
    try
    {
      this.physicalConn.setAutoCommit(false);
    }
    catch (SQLException localSQLException)
    {
      throw convertException(localSQLException);
    }
    this.currentTransaction = paramXid;
    this.prepared = false;
  }
  
  public void commit(Xid paramXid, boolean paramBoolean)
    throws XAException
  {
    if (isDebugEnabled()) {
      debugCode("commit(" + JdbcXid.toString(paramXid) + ", " + paramBoolean + ");");
    }
    Statement localStatement = null;
    try
    {
      if (paramBoolean)
      {
        this.physicalConn.commit();
      }
      else
      {
        localStatement = this.physicalConn.createStatement();
        localStatement.execute("COMMIT TRANSACTION " + JdbcXid.toString(paramXid));
        this.prepared = false;
      }
      this.physicalConn.setAutoCommit(true);
    }
    catch (SQLException localSQLException)
    {
      throw convertException(localSQLException);
    }
    finally
    {
      JdbcUtils.closeSilently(localStatement);
    }
    this.currentTransaction = null;
  }
  
  public void addStatementEventListener(StatementEventListener paramStatementEventListener)
  {
    throw new UnsupportedOperationException();
  }
  
  public void removeStatementEventListener(StatementEventListener paramStatementEventListener)
  {
    throw new UnsupportedOperationException();
  }
  
  public String toString()
  {
    return getTraceObjectName() + ": " + this.physicalConn;
  }
  
  private static XAException convertException(SQLException paramSQLException)
  {
    XAException localXAException = new XAException(paramSQLException.getMessage());
    localXAException.initCause(paramSQLException);
    return localXAException;
  }
  
  private static String quoteFlags(int paramInt)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    if ((paramInt & 0x800000) != 0) {
      localStringBuilder.append("|XAResource.TMENDRSCAN");
    }
    if ((paramInt & 0x20000000) != 0) {
      localStringBuilder.append("|XAResource.TMFAIL");
    }
    if ((paramInt & 0x200000) != 0) {
      localStringBuilder.append("|XAResource.TMJOIN");
    }
    if ((paramInt & 0x40000000) != 0) {
      localStringBuilder.append("|XAResource.TMONEPHASE");
    }
    if ((paramInt & 0x8000000) != 0) {
      localStringBuilder.append("|XAResource.TMRESUME");
    }
    if ((paramInt & 0x1000000) != 0) {
      localStringBuilder.append("|XAResource.TMSTARTRSCAN");
    }
    if ((paramInt & 0x4000000) != 0) {
      localStringBuilder.append("|XAResource.TMSUCCESS");
    }
    if ((paramInt & 0x2000000) != 0) {
      localStringBuilder.append("|XAResource.TMSUSPEND");
    }
    if ((paramInt & 0x3) != 0) {
      localStringBuilder.append("|XAResource.XA_RDONLY");
    }
    if (localStringBuilder.length() == 0) {
      localStringBuilder.append("|XAResource.TMNOFLAGS");
    }
    return localStringBuilder.toString().substring(1);
  }
  
  private void checkOpen()
    throws XAException
  {
    if (this.physicalConn == null) {
      throw new XAException(-3);
    }
  }
  
  class PooledJdbcConnection
    extends JdbcConnection
  {
    private boolean isClosed;
    
    public PooledJdbcConnection(JdbcConnection paramJdbcConnection)
    {
      super();
    }
    
    public synchronized void close()
      throws SQLException
    {
      if (!this.isClosed)
      {
        try
        {
          rollback();
          setAutoCommit(true);
        }
        catch (SQLException localSQLException) {}
        JdbcXAConnection.this.closedHandle();
        this.isClosed = true;
      }
    }
    
    public synchronized boolean isClosed()
      throws SQLException
    {
      return (this.isClosed) || (super.isClosed());
    }
    
    protected synchronized void checkClosed(boolean paramBoolean)
    {
      if (this.isClosed) {
        throw DbException.get(90007);
      }
      super.checkClosed(paramBoolean);
    }
  }
}
