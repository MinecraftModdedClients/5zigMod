package org.h2.jdbc;

import java.io.Closeable;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import org.h2.command.CommandInterface;
import org.h2.engine.ConnectionInfo;
import org.h2.engine.SessionInterface;
import org.h2.engine.SessionRemote;
import org.h2.engine.SysProperties;
import org.h2.expression.ParameterInterface;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.message.TraceObject;
import org.h2.result.ResultInterface;
import org.h2.store.DataHandler;
import org.h2.store.LobStorageInterface;
import org.h2.util.CloseWatcher;
import org.h2.util.JdbcUtils;
import org.h2.value.CompareMode;
import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;

public class JdbcConnection
  extends TraceObject
  implements Connection
{
  private static boolean keepOpenStackTrace;
  private final String url;
  private final String user;
  private int holdability = 1;
  private SessionInterface session;
  private CommandInterface commit;
  private CommandInterface rollback;
  private CommandInterface getReadOnly;
  private CommandInterface getGeneratedKeys;
  private CommandInterface setLockMode;
  private CommandInterface getLockMode;
  private CommandInterface setQueryTimeout;
  private CommandInterface getQueryTimeout;
  private int savepointId;
  private String catalog;
  private Statement executingStatement;
  private final CompareMode compareMode = CompareMode.getInstance(null, 0);
  private final CloseWatcher watcher;
  private int queryTimeoutCache = -1;
  
  public JdbcConnection(String paramString, Properties paramProperties)
    throws SQLException
  {
    this(new ConnectionInfo(paramString, paramProperties), true);
  }
  
  public JdbcConnection(ConnectionInfo paramConnectionInfo, boolean paramBoolean)
    throws SQLException
  {
    try
    {
      if (paramBoolean)
      {
        String str = SysProperties.getBaseDir();
        if (str != null) {
          paramConnectionInfo.setBaseDir(str);
        }
      }
      this.session = new SessionRemote(paramConnectionInfo).connectEmbeddedOrServer(false);
      this.trace = this.session.getTrace();
      int i = getNextId(1);
      setTrace(this.trace, 1, i);
      this.user = paramConnectionInfo.getUserName();
      if (isInfoEnabled()) {
        this.trace.infoCode("Connection " + getTraceObjectName() + " = DriverManager.getConnection(" + quote(paramConnectionInfo.getOriginalURL()) + ", " + quote(this.user) + ", \"\");");
      }
      this.url = paramConnectionInfo.getURL();
      closeOld();
      this.watcher = CloseWatcher.register(this, this.session, keepOpenStackTrace);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public JdbcConnection(JdbcConnection paramJdbcConnection)
  {
    this.session = paramJdbcConnection.session;
    this.trace = this.session.getTrace();
    int i = getNextId(1);
    setTrace(this.trace, 1, i);
    this.user = paramJdbcConnection.user;
    this.url = paramJdbcConnection.url;
    this.catalog = paramJdbcConnection.catalog;
    this.commit = paramJdbcConnection.commit;
    this.getGeneratedKeys = paramJdbcConnection.getGeneratedKeys;
    this.getLockMode = paramJdbcConnection.getLockMode;
    this.getQueryTimeout = paramJdbcConnection.getQueryTimeout;
    this.getReadOnly = paramJdbcConnection.getReadOnly;
    this.rollback = paramJdbcConnection.rollback;
    this.watcher = null;
  }
  
  public JdbcConnection(SessionInterface paramSessionInterface, String paramString1, String paramString2)
  {
    this.session = paramSessionInterface;
    this.trace = paramSessionInterface.getTrace();
    int i = getNextId(1);
    setTrace(this.trace, 1, i);
    this.user = paramString1;
    this.url = paramString2;
    this.watcher = null;
  }
  
  private void closeOld()
  {
    for (;;)
    {
      CloseWatcher localCloseWatcher = CloseWatcher.pollUnclosed();
      if (localCloseWatcher == null) {
        break;
      }
      try
      {
        localCloseWatcher.getCloseable().close();
      }
      catch (Exception localException)
      {
        this.trace.error(localException, "closing session");
      }
      keepOpenStackTrace = true;
      String str = localCloseWatcher.getOpenStackTrace();
      DbException localDbException = DbException.get(90018);
      this.trace.error(localDbException, str);
    }
  }
  
  public Statement createStatement()
    throws SQLException
  {
    try
    {
      int i = getNextId(8);
      if (isDebugEnabled()) {
        debugCodeAssign("Statement", 8, i, "createStatement()");
      }
      checkClosed();
      return new JdbcStatement(this, i, 1003, 1007, false);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Statement createStatement(int paramInt1, int paramInt2)
    throws SQLException
  {
    try
    {
      int i = getNextId(8);
      if (isDebugEnabled()) {
        debugCodeAssign("Statement", 8, i, "createStatement(" + paramInt1 + ", " + paramInt2 + ")");
      }
      checkTypeConcurrency(paramInt1, paramInt2);
      checkClosed();
      return new JdbcStatement(this, i, paramInt1, paramInt2, false);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Statement createStatement(int paramInt1, int paramInt2, int paramInt3)
    throws SQLException
  {
    try
    {
      int i = getNextId(8);
      if (isDebugEnabled()) {
        debugCodeAssign("Statement", 8, i, "createStatement(" + paramInt1 + ", " + paramInt2 + ", " + paramInt3 + ")");
      }
      checkTypeConcurrency(paramInt1, paramInt2);
      checkHoldability(paramInt3);
      checkClosed();
      return new JdbcStatement(this, i, paramInt1, paramInt2, false);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public PreparedStatement prepareStatement(String paramString)
    throws SQLException
  {
    try
    {
      int i = getNextId(3);
      if (isDebugEnabled()) {
        debugCodeAssign("PreparedStatement", 3, i, "prepareStatement(" + quote(paramString) + ")");
      }
      checkClosed();
      paramString = translateSQL(paramString);
      return new JdbcPreparedStatement(this, paramString, i, 1003, 1007, false);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  PreparedStatement prepareAutoCloseStatement(String paramString)
    throws SQLException
  {
    try
    {
      int i = getNextId(3);
      if (isDebugEnabled()) {
        debugCodeAssign("PreparedStatement", 3, i, "prepareStatement(" + quote(paramString) + ")");
      }
      checkClosed();
      paramString = translateSQL(paramString);
      return new JdbcPreparedStatement(this, paramString, i, 1003, 1007, true);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public DatabaseMetaData getMetaData()
    throws SQLException
  {
    try
    {
      int i = getNextId(2);
      if (isDebugEnabled()) {
        debugCodeAssign("DatabaseMetaData", 2, i, "getMetaData()");
      }
      checkClosed();
      return new JdbcDatabaseMetaData(this, this.trace, i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public SessionInterface getSession()
  {
    return this.session;
  }
  
  public synchronized void close()
    throws SQLException
  {
    try
    {
      debugCodeCall("close");
      if (this.session == null) {
        return;
      }
      CloseWatcher.unregister(this.watcher);
      this.session.cancel();
      if (this.executingStatement != null) {
        try
        {
          this.executingStatement.cancel();
        }
        catch (NullPointerException localNullPointerException) {}
      }
      synchronized (this.session)
      {
        try
        {
          if (!this.session.isClosed()) {
            try
            {
              if (this.session.hasPendingTransaction())
              {
                if (!this.session.isReconnectNeeded(true)) {
                  try
                  {
                    rollbackInternal();
                  }
                  catch (DbException localDbException)
                  {
                    if (localDbException.getErrorCode() != 90067) {
                      throw localDbException;
                    }
                  }
                }
                this.session.afterWriting();
              }
              closePreparedCommands();
            }
            finally
            {
              this.session.close();
            }
          }
        }
        finally
        {
          this.session = null;
        }
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  private void closePreparedCommands()
  {
    this.commit = closeAndSetNull(this.commit);
    this.rollback = closeAndSetNull(this.rollback);
    this.getReadOnly = closeAndSetNull(this.getReadOnly);
    this.getGeneratedKeys = closeAndSetNull(this.getGeneratedKeys);
    this.getLockMode = closeAndSetNull(this.getLockMode);
    this.setLockMode = closeAndSetNull(this.setLockMode);
    this.getQueryTimeout = closeAndSetNull(this.getQueryTimeout);
    this.setQueryTimeout = closeAndSetNull(this.setQueryTimeout);
  }
  
  private static CommandInterface closeAndSetNull(CommandInterface paramCommandInterface)
  {
    if (paramCommandInterface != null) {
      paramCommandInterface.close();
    }
    return null;
  }
  
  public synchronized void setAutoCommit(boolean paramBoolean)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setAutoCommit(" + paramBoolean + ");");
      }
      checkClosed();
      if ((paramBoolean) && (!this.session.getAutoCommit())) {
        commit();
      }
      this.session.setAutoCommit(paramBoolean);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public synchronized boolean getAutoCommit()
    throws SQLException
  {
    try
    {
      checkClosed();
      debugCodeCall("getAutoCommit");
      return this.session.getAutoCommit();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public synchronized void commit()
    throws SQLException
  {
    try
    {
      debugCodeCall("commit");
      checkClosedForWrite();
      try
      {
        this.commit = prepareCommand("COMMIT", this.commit);
        this.commit.executeUpdate();
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public synchronized void rollback()
    throws SQLException
  {
    try
    {
      debugCodeCall("rollback");
      checkClosedForWrite();
      try
      {
        rollbackInternal();
      }
      finally
      {
        afterWriting();
      }
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
      return (this.session == null) || (this.session.isClosed());
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String nativeSQL(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("nativeSQL", paramString);
      checkClosed();
      return translateSQL(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setReadOnly(boolean paramBoolean)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setReadOnly(" + paramBoolean + ");");
      }
      checkClosed();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isReadOnly()
    throws SQLException
  {
    try
    {
      debugCodeCall("isReadOnly");
      checkClosed();
      this.getReadOnly = prepareCommand("CALL READONLY()", this.getReadOnly);
      ResultInterface localResultInterface = this.getReadOnly.executeQuery(0, false);
      localResultInterface.next();
      return localResultInterface.currentRow()[0].getBoolean().booleanValue();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setCatalog(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("setCatalog", paramString);
      checkClosed();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getCatalog()
    throws SQLException
  {
    try
    {
      debugCodeCall("getCatalog");
      checkClosed();
      if (this.catalog == null)
      {
        CommandInterface localCommandInterface = prepareCommand("CALL DATABASE()", Integer.MAX_VALUE);
        ResultInterface localResultInterface = localCommandInterface.executeQuery(0, false);
        localResultInterface.next();
        this.catalog = localResultInterface.currentRow()[0].getString();
        localCommandInterface.close();
      }
      return this.catalog;
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
  
  public PreparedStatement prepareStatement(String paramString, int paramInt1, int paramInt2)
    throws SQLException
  {
    try
    {
      int i = getNextId(3);
      if (isDebugEnabled()) {
        debugCodeAssign("PreparedStatement", 3, i, "prepareStatement(" + quote(paramString) + ", " + paramInt1 + ", " + paramInt2 + ")");
      }
      checkTypeConcurrency(paramInt1, paramInt2);
      checkClosed();
      paramString = translateSQL(paramString);
      return new JdbcPreparedStatement(this, paramString, i, paramInt1, paramInt2, false);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setTransactionIsolation(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("setTransactionIsolation", paramInt);
      checkClosed();
      int i;
      switch (paramInt)
      {
      case 1: 
        i = 0;
        break;
      case 2: 
        i = 3;
        break;
      case 4: 
      case 8: 
        i = 1;
        break;
      case 3: 
      case 5: 
      case 6: 
      case 7: 
      default: 
        throw DbException.getInvalidValueException("level", Integer.valueOf(paramInt));
      }
      commit();
      this.setLockMode = prepareCommand("SET LOCK_MODE ?", this.setLockMode);
      ((ParameterInterface)this.setLockMode.getParameters().get(0)).setValue(ValueInt.get(i), false);
      this.setLockMode.executeUpdate();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setQueryTimeout(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("setQueryTimeout", paramInt);
      checkClosed();
      this.setQueryTimeout = prepareCommand("SET QUERY_TIMEOUT ?", this.setQueryTimeout);
      ((ParameterInterface)this.setQueryTimeout.getParameters().get(0)).setValue(ValueInt.get(paramInt * 1000), false);
      
      this.setQueryTimeout.executeUpdate();
      this.queryTimeoutCache = paramInt;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  int getQueryTimeout()
    throws SQLException
  {
    try
    {
      if (this.queryTimeoutCache == -1)
      {
        checkClosed();
        this.getQueryTimeout = prepareCommand("SELECT VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME=?", this.getQueryTimeout);
        
        ((ParameterInterface)this.getQueryTimeout.getParameters().get(0)).setValue(ValueString.get("QUERY_TIMEOUT"), false);
        
        ResultInterface localResultInterface = this.getQueryTimeout.executeQuery(0, false);
        localResultInterface.next();
        int i = localResultInterface.currentRow()[0].getInt();
        localResultInterface.close();
        if (i != 0) {
          i = (i + 999) / 1000;
        }
        this.queryTimeoutCache = i;
      }
      return this.queryTimeoutCache;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getTransactionIsolation()
    throws SQLException
  {
    try
    {
      debugCodeCall("getTransactionIsolation");
      checkClosed();
      this.getLockMode = prepareCommand("CALL LOCK_MODE()", this.getLockMode);
      ResultInterface localResultInterface = this.getLockMode.executeQuery(0, false);
      localResultInterface.next();
      int i = localResultInterface.currentRow()[0].getInt();
      localResultInterface.close();
      int j;
      switch (i)
      {
      case 0: 
        j = 1;
        break;
      case 3: 
        j = 2;
        break;
      case 1: 
      case 2: 
        j = 8;
        break;
      default: 
        throw DbException.throwInternalError("lockMode:" + i);
      }
      return j;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setHoldability(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("setHoldability", paramInt);
      checkClosed();
      checkHoldability(paramInt);
      this.holdability = paramInt;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getHoldability()
    throws SQLException
  {
    try
    {
      debugCodeCall("getHoldability");
      checkClosed();
      return this.holdability;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Map<String, Class<?>> getTypeMap()
    throws SQLException
  {
    try
    {
      debugCodeCall("getTypeMap");
      checkClosed();
      return null;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setTypeMap(Map<String, Class<?>> paramMap)
    throws SQLException
  {
    try
    {
      debugCode("setTypeMap(" + quoteMap(paramMap) + ");");
      checkMap(paramMap);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public CallableStatement prepareCall(String paramString)
    throws SQLException
  {
    try
    {
      int i = getNextId(0);
      if (isDebugEnabled()) {
        debugCodeAssign("CallableStatement", 0, i, "prepareCall(" + quote(paramString) + ")");
      }
      checkClosed();
      paramString = translateSQL(paramString);
      return new JdbcCallableStatement(this, paramString, i, 1003, 1007);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public CallableStatement prepareCall(String paramString, int paramInt1, int paramInt2)
    throws SQLException
  {
    try
    {
      int i = getNextId(0);
      if (isDebugEnabled()) {
        debugCodeAssign("CallableStatement", 0, i, "prepareCall(" + quote(paramString) + ", " + paramInt1 + ", " + paramInt2 + ")");
      }
      checkTypeConcurrency(paramInt1, paramInt2);
      checkClosed();
      paramString = translateSQL(paramString);
      return new JdbcCallableStatement(this, paramString, i, paramInt1, paramInt2);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public CallableStatement prepareCall(String paramString, int paramInt1, int paramInt2, int paramInt3)
    throws SQLException
  {
    try
    {
      int i = getNextId(0);
      if (isDebugEnabled()) {
        debugCodeAssign("CallableStatement", 0, i, "prepareCall(" + quote(paramString) + ", " + paramInt1 + ", " + paramInt2 + ", " + paramInt3 + ")");
      }
      checkTypeConcurrency(paramInt1, paramInt2);
      checkHoldability(paramInt3);
      checkClosed();
      paramString = translateSQL(paramString);
      return new JdbcCallableStatement(this, paramString, i, paramInt1, paramInt2);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Savepoint setSavepoint()
    throws SQLException
  {
    try
    {
      int i = getNextId(6);
      if (isDebugEnabled()) {
        debugCodeAssign("Savepoint", 6, i, "setSavepoint()");
      }
      checkClosed();
      CommandInterface localCommandInterface = prepareCommand("SAVEPOINT " + JdbcSavepoint.getName(null, this.savepointId), Integer.MAX_VALUE);
      
      localCommandInterface.executeUpdate();
      JdbcSavepoint localJdbcSavepoint = new JdbcSavepoint(this, this.savepointId, null, this.trace, i);
      this.savepointId += 1;
      return localJdbcSavepoint;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Savepoint setSavepoint(String paramString)
    throws SQLException
  {
    try
    {
      int i = getNextId(6);
      if (isDebugEnabled()) {
        debugCodeAssign("Savepoint", 6, i, "setSavepoint(" + quote(paramString) + ")");
      }
      checkClosed();
      CommandInterface localCommandInterface = prepareCommand("SAVEPOINT " + JdbcSavepoint.getName(paramString, 0), Integer.MAX_VALUE);
      
      localCommandInterface.executeUpdate();
      return new JdbcSavepoint(this, 0, paramString, this.trace, i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void rollback(Savepoint paramSavepoint)
    throws SQLException
  {
    try
    {
      JdbcSavepoint localJdbcSavepoint = convertSavepoint(paramSavepoint);
      debugCode("rollback(" + localJdbcSavepoint.getTraceObjectName() + ");");
      checkClosedForWrite();
      try
      {
        localJdbcSavepoint.rollback();
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void releaseSavepoint(Savepoint paramSavepoint)
    throws SQLException
  {
    try
    {
      debugCode("releaseSavepoint(savepoint);");
      checkClosed();
      convertSavepoint(paramSavepoint).release();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  private static JdbcSavepoint convertSavepoint(Savepoint paramSavepoint)
  {
    if (!(paramSavepoint instanceof JdbcSavepoint)) {
      throw DbException.get(90063, "" + paramSavepoint);
    }
    return (JdbcSavepoint)paramSavepoint;
  }
  
  public PreparedStatement prepareStatement(String paramString, int paramInt1, int paramInt2, int paramInt3)
    throws SQLException
  {
    try
    {
      int i = getNextId(3);
      if (isDebugEnabled()) {
        debugCodeAssign("PreparedStatement", 3, i, "prepareStatement(" + quote(paramString) + ", " + paramInt1 + ", " + paramInt2 + ", " + paramInt3 + ")");
      }
      checkTypeConcurrency(paramInt1, paramInt2);
      checkHoldability(paramInt3);
      checkClosed();
      paramString = translateSQL(paramString);
      return new JdbcPreparedStatement(this, paramString, i, paramInt1, paramInt2, false);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public PreparedStatement prepareStatement(String paramString, int paramInt)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("prepareStatement(" + quote(paramString) + ", " + paramInt + ");");
      }
      return prepareStatement(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public PreparedStatement prepareStatement(String paramString, int[] paramArrayOfInt)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("prepareStatement(" + quote(paramString) + ", " + quoteIntArray(paramArrayOfInt) + ");");
      }
      return prepareStatement(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public PreparedStatement prepareStatement(String paramString, String[] paramArrayOfString)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("prepareStatement(" + quote(paramString) + ", " + quoteArray(paramArrayOfString) + ");");
      }
      return prepareStatement(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  CommandInterface prepareCommand(String paramString, int paramInt)
  {
    return this.session.prepareCommand(paramString, paramInt);
  }
  
  private CommandInterface prepareCommand(String paramString, CommandInterface paramCommandInterface)
  {
    return paramCommandInterface == null ? this.session.prepareCommand(paramString, Integer.MAX_VALUE) : paramCommandInterface;
  }
  
  private static int translateGetEnd(String paramString, int paramInt, char paramChar)
  {
    int i = paramString.length();
    int j;
    switch (paramChar)
    {
    case '$': 
      if ((paramInt < i - 1) && (paramString.charAt(paramInt + 1) == '$') && ((paramInt == 0) || (paramString.charAt(paramInt - 1) <= ' ')))
      {
        j = paramString.indexOf("$$", paramInt + 2);
        if (j < 0) {
          throw DbException.getSyntaxError(paramString, paramInt);
        }
        return j + 1;
      }
      return paramInt;
    case '\'': 
      j = paramString.indexOf('\'', paramInt + 1);
      if (j < 0) {
        throw DbException.getSyntaxError(paramString, paramInt);
      }
      return j;
    case '"': 
      j = paramString.indexOf('"', paramInt + 1);
      if (j < 0) {
        throw DbException.getSyntaxError(paramString, paramInt);
      }
      return j;
    case '/': 
      checkRunOver(paramInt + 1, i, paramString);
      if (paramString.charAt(paramInt + 1) == '*')
      {
        j = paramString.indexOf("*/", paramInt + 2);
        if (j < 0) {
          throw DbException.getSyntaxError(paramString, paramInt);
        }
        paramInt = j + 1;
      }
      else if (paramString.charAt(paramInt + 1) == '/')
      {
        paramInt += 2;
        while ((paramInt < i) && ((paramChar = paramString.charAt(paramInt)) != '\r') && (paramChar != '\n')) {
          paramInt++;
        }
      }
      return paramInt;
    case '-': 
      checkRunOver(paramInt + 1, i, paramString);
      if (paramString.charAt(paramInt + 1) == '-')
      {
        paramInt += 2;
        while ((paramInt < i) && ((paramChar = paramString.charAt(paramInt)) != '\r') && (paramChar != '\n')) {
          paramInt++;
        }
      }
      return paramInt;
    }
    throw DbException.throwInternalError("c=" + paramChar);
  }
  
  private static String translateSQL(String paramString)
  {
    return translateSQL(paramString, true);
  }
  
  static String translateSQL(String paramString, boolean paramBoolean)
  {
    if (paramString == null) {
      throw DbException.getInvalidValueException("SQL", null);
    }
    if (!paramBoolean) {
      return paramString;
    }
    if (paramString.indexOf('{') < 0) {
      return paramString;
    }
    int i = paramString.length();
    char[] arrayOfChar = null;
    int j = 0;
    char c;
    int n;
    for (int k = 0; k < i; k++)
    {
      c = paramString.charAt(k);
      switch (c)
      {
      case '"': 
      case '\'': 
      case '-': 
      case '/': 
        k = translateGetEnd(paramString, k, c);
        break;
      case '{': 
        j++;
        if (arrayOfChar == null) {
          arrayOfChar = paramString.toCharArray();
        }
        arrayOfChar[k] = ' ';
        while (Character.isSpaceChar(arrayOfChar[k]))
        {
          k++;
          checkRunOver(k, i, paramString);
        }
        int m = k;
        if ((arrayOfChar[k] >= '0') && (arrayOfChar[k] <= '9'))
        {
          arrayOfChar[(k - 1)] = '{';
          for (;;)
          {
            checkRunOver(k, i, paramString);
            c = arrayOfChar[k];
            if (c == '}') {
              break;
            }
            switch (c)
            {
            case '"': 
            case '\'': 
            case '-': 
            case '/': 
              k = translateGetEnd(paramString, k, c);
              break;
            }
            k++;
          }
          j--;
        }
        else
        {
          if (arrayOfChar[k] == '?')
          {
            k++;
            checkRunOver(k, i, paramString);
            while (Character.isSpaceChar(arrayOfChar[k]))
            {
              k++;
              checkRunOver(k, i, paramString);
            }
            if (paramString.charAt(k) != '=') {
              throw DbException.getSyntaxError(paramString, k, "=");
            }
            k++;
            checkRunOver(k, i, paramString);
            while (Character.isSpaceChar(arrayOfChar[k]))
            {
              k++;
              checkRunOver(k, i, paramString);
            }
          }
          while (!Character.isSpaceChar(arrayOfChar[k]))
          {
            k++;
            checkRunOver(k, i, paramString);
          }
          n = 0;
          if (found(paramString, m, "fn"))
          {
            n = 2;
          }
          else
          {
            if (found(paramString, m, "escape")) {
              continue;
            }
            if (found(paramString, m, "call")) {
              continue;
            }
            if (found(paramString, m, "oj"))
            {
              n = 2;
            }
            else
            {
              if (found(paramString, m, "ts")) {
                continue;
              }
              if (found(paramString, m, "t")) {
                continue;
              }
              if (found(paramString, m, "d")) {
                continue;
              }
              if (found(paramString, m, "params")) {
                n = "params".length();
              }
            }
          }
          for (k = m; n > 0;)
          {
            arrayOfChar[k] = ' ';k++;n--; continue;
            
            j--;
            if (j < 0) {
              throw DbException.getSyntaxError(paramString, k);
            }
            arrayOfChar[k] = ' ';
            break;
            
            k = translateGetEnd(paramString, k, c);
          }
        }
        break;
      }
    }
    if (j != 0) {
      throw DbException.getSyntaxError(paramString, paramString.length() - 1);
    }
    if (arrayOfChar != null) {
      paramString = new String(arrayOfChar);
    }
    return paramString;
  }
  
  private static void checkRunOver(int paramInt1, int paramInt2, String paramString)
  {
    if (paramInt1 >= paramInt2) {
      throw DbException.getSyntaxError(paramString, paramInt1);
    }
  }
  
  private static boolean found(String paramString1, int paramInt, String paramString2)
  {
    return paramString1.regionMatches(true, paramInt, paramString2, 0, paramString2.length());
  }
  
  private static void checkTypeConcurrency(int paramInt1, int paramInt2)
  {
    switch (paramInt1)
    {
    case 1003: 
    case 1004: 
    case 1005: 
      break;
    default: 
      throw DbException.getInvalidValueException("resultSetType", Integer.valueOf(paramInt1));
    }
    switch (paramInt2)
    {
    case 1007: 
    case 1008: 
      break;
    default: 
      throw DbException.getInvalidValueException("resultSetConcurrency", Integer.valueOf(paramInt2));
    }
  }
  
  private static void checkHoldability(int paramInt)
  {
    if ((paramInt != 1) && (paramInt != 2)) {
      throw DbException.getInvalidValueException("resultSetHoldability", Integer.valueOf(paramInt));
    }
  }
  
  protected void checkClosed()
  {
    checkClosed(false);
  }
  
  private void checkClosedForWrite()
  {
    checkClosed(true);
  }
  
  protected void checkClosed(boolean paramBoolean)
  {
    if (this.session == null) {
      throw DbException.get(90007);
    }
    if (this.session.isClosed()) {
      throw DbException.get(90121);
    }
    if (this.session.isReconnectNeeded(paramBoolean))
    {
      this.trace.debug("reconnect");
      closePreparedCommands();
      this.session = this.session.reconnect(paramBoolean);
      this.trace = this.session.getTrace();
    }
  }
  
  protected void afterWriting()
  {
    if (this.session != null) {
      this.session.afterWriting();
    }
  }
  
  String getURL()
  {
    checkClosed();
    return this.url;
  }
  
  String getUser()
  {
    checkClosed();
    return this.user;
  }
  
  private void rollbackInternal()
  {
    this.rollback = prepareCommand("ROLLBACK", this.rollback);
    this.rollback.executeUpdate();
  }
  
  public int getPowerOffCount()
  {
    return (this.session == null) || (this.session.isClosed()) ? 0 : this.session.getPowerOffCount();
  }
  
  public void setPowerOffCount(int paramInt)
  {
    if (this.session != null) {
      this.session.setPowerOffCount(paramInt);
    }
  }
  
  public void setExecutingStatement(Statement paramStatement)
  {
    this.executingStatement = paramStatement;
  }
  
  ResultSet getGeneratedKeys(JdbcStatement paramJdbcStatement, int paramInt)
  {
    this.getGeneratedKeys = prepareCommand("SELECT SCOPE_IDENTITY() WHERE SCOPE_IDENTITY() IS NOT NULL", this.getGeneratedKeys);
    
    ResultInterface localResultInterface = this.getGeneratedKeys.executeQuery(0, false);
    JdbcResultSet localJdbcResultSet = new JdbcResultSet(this, paramJdbcStatement, localResultInterface, paramInt, false, true, false);
    return localJdbcResultSet;
  }
  
  /* Error */
  public java.sql.Clob createClob()
    throws SQLException
  {
    // Byte code:
    //   0: bipush 10
    //   2: invokestatic 17	org/h2/jdbc/JdbcConnection:getNextId	(I)I
    //   5: istore_1
    //   6: aload_0
    //   7: ldc -24
    //   9: bipush 10
    //   11: iload_1
    //   12: ldc -23
    //   14: invokevirtual 60	org/h2/jdbc/JdbcConnection:debugCodeAssign	(Ljava/lang/String;IILjava/lang/String;)V
    //   17: aload_0
    //   18: invokespecial 108	org/h2/jdbc/JdbcConnection:checkClosedForWrite	()V
    //   21: aload_0
    //   22: getfield 14	org/h2/jdbc/JdbcConnection:session	Lorg/h2/engine/SessionInterface;
    //   25: invokeinterface 234 1 0
    //   30: invokeinterface 235 1 0
    //   35: new 236	java/io/InputStreamReader
    //   38: dup
    //   39: new 237	java/io/ByteArrayInputStream
    //   42: dup
    //   43: getstatic 238	org/h2/util/Utils:EMPTY_BYTES	[B
    //   46: invokespecial 239	java/io/ByteArrayInputStream:<init>	([B)V
    //   49: invokespecial 240	java/io/InputStreamReader:<init>	(Ljava/io/InputStream;)V
    //   52: lconst_0
    //   53: invokeinterface 241 4 0
    //   58: astore_2
    //   59: aload_0
    //   60: getfield 14	org/h2/jdbc/JdbcConnection:session	Lorg/h2/engine/SessionInterface;
    //   63: aload_2
    //   64: invokeinterface 242 2 0
    //   69: new 243	org/h2/jdbc/JdbcClob
    //   72: dup
    //   73: aload_0
    //   74: aload_2
    //   75: iload_1
    //   76: invokespecial 244	org/h2/jdbc/JdbcClob:<init>	(Lorg/h2/jdbc/JdbcConnection;Lorg/h2/value/Value;I)V
    //   79: astore_3
    //   80: aload_0
    //   81: invokevirtual 112	org/h2/jdbc/JdbcConnection:afterWriting	()V
    //   84: aload_3
    //   85: areturn
    //   86: astore 4
    //   88: aload_0
    //   89: invokevirtual 112	org/h2/jdbc/JdbcConnection:afterWriting	()V
    //   92: aload 4
    //   94: athrow
    //   95: astore_1
    //   96: aload_0
    //   97: aload_1
    //   98: invokevirtual 41	org/h2/jdbc/JdbcConnection:logAndConvert	(Ljava/lang/Exception;)Ljava/sql/SQLException;
    //   101: athrow
    // Line number table:
    //   Java source line #1548	-> byte code offset #0
    //   Java source line #1549	-> byte code offset #6
    //   Java source line #1550	-> byte code offset #17
    //   Java source line #1552	-> byte code offset #21
    //   Java source line #1555	-> byte code offset #59
    //   Java source line #1556	-> byte code offset #69
    //   Java source line #1558	-> byte code offset #80
    //   Java source line #1560	-> byte code offset #95
    //   Java source line #1561	-> byte code offset #96
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	102	0	this	JdbcConnection
    //   5	71	1	i	int
    //   95	3	1	localException	Exception
    //   58	17	2	localValue	Value
    //   86	7	4	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   21	80	86	finally
    //   86	88	86	finally
    //   0	84	95	java/lang/Exception
    //   86	95	95	java/lang/Exception
  }
  
  /* Error */
  public java.sql.Blob createBlob()
    throws SQLException
  {
    // Byte code:
    //   0: bipush 9
    //   2: invokestatic 17	org/h2/jdbc/JdbcConnection:getNextId	(I)I
    //   5: istore_1
    //   6: aload_0
    //   7: ldc -11
    //   9: bipush 9
    //   11: iload_1
    //   12: ldc -23
    //   14: invokevirtual 60	org/h2/jdbc/JdbcConnection:debugCodeAssign	(Ljava/lang/String;IILjava/lang/String;)V
    //   17: aload_0
    //   18: invokespecial 108	org/h2/jdbc/JdbcConnection:checkClosedForWrite	()V
    //   21: aload_0
    //   22: getfield 14	org/h2/jdbc/JdbcConnection:session	Lorg/h2/engine/SessionInterface;
    //   25: invokeinterface 234 1 0
    //   30: invokeinterface 235 1 0
    //   35: new 237	java/io/ByteArrayInputStream
    //   38: dup
    //   39: getstatic 238	org/h2/util/Utils:EMPTY_BYTES	[B
    //   42: invokespecial 239	java/io/ByteArrayInputStream:<init>	([B)V
    //   45: lconst_0
    //   46: invokeinterface 246 4 0
    //   51: astore_2
    //   52: aload_0
    //   53: getfield 14	org/h2/jdbc/JdbcConnection:session	Lorg/h2/engine/SessionInterface;
    //   56: aload_2
    //   57: invokeinterface 242 2 0
    //   62: new 247	org/h2/jdbc/JdbcBlob
    //   65: dup
    //   66: aload_0
    //   67: aload_2
    //   68: iload_1
    //   69: invokespecial 248	org/h2/jdbc/JdbcBlob:<init>	(Lorg/h2/jdbc/JdbcConnection;Lorg/h2/value/Value;I)V
    //   72: astore_3
    //   73: aload_0
    //   74: invokevirtual 112	org/h2/jdbc/JdbcConnection:afterWriting	()V
    //   77: aload_3
    //   78: areturn
    //   79: astore 4
    //   81: aload_0
    //   82: invokevirtual 112	org/h2/jdbc/JdbcConnection:afterWriting	()V
    //   85: aload 4
    //   87: athrow
    //   88: astore_1
    //   89: aload_0
    //   90: aload_1
    //   91: invokevirtual 41	org/h2/jdbc/JdbcConnection:logAndConvert	(Ljava/lang/Exception;)Ljava/sql/SQLException;
    //   94: athrow
    // Line number table:
    //   Java source line #1573	-> byte code offset #0
    //   Java source line #1574	-> byte code offset #6
    //   Java source line #1575	-> byte code offset #17
    //   Java source line #1577	-> byte code offset #21
    //   Java source line #1579	-> byte code offset #52
    //   Java source line #1580	-> byte code offset #62
    //   Java source line #1582	-> byte code offset #73
    //   Java source line #1584	-> byte code offset #88
    //   Java source line #1585	-> byte code offset #89
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	95	0	this	JdbcConnection
    //   5	64	1	i	int
    //   88	3	1	localException	Exception
    //   51	17	2	localValue	Value
    //   79	7	4	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   21	73	79	finally
    //   79	81	79	finally
    //   0	77	88	java/lang/Exception
    //   79	88	88	java/lang/Exception
  }
  
  /* Error */
  public java.sql.NClob createNClob()
    throws SQLException
  {
    // Byte code:
    //   0: bipush 10
    //   2: invokestatic 17	org/h2/jdbc/JdbcConnection:getNextId	(I)I
    //   5: istore_1
    //   6: aload_0
    //   7: ldc -7
    //   9: bipush 10
    //   11: iload_1
    //   12: ldc -6
    //   14: invokevirtual 60	org/h2/jdbc/JdbcConnection:debugCodeAssign	(Ljava/lang/String;IILjava/lang/String;)V
    //   17: aload_0
    //   18: invokespecial 108	org/h2/jdbc/JdbcConnection:checkClosedForWrite	()V
    //   21: aload_0
    //   22: getfield 14	org/h2/jdbc/JdbcConnection:session	Lorg/h2/engine/SessionInterface;
    //   25: invokeinterface 234 1 0
    //   30: invokeinterface 235 1 0
    //   35: new 236	java/io/InputStreamReader
    //   38: dup
    //   39: new 237	java/io/ByteArrayInputStream
    //   42: dup
    //   43: getstatic 238	org/h2/util/Utils:EMPTY_BYTES	[B
    //   46: invokespecial 239	java/io/ByteArrayInputStream:<init>	([B)V
    //   49: invokespecial 240	java/io/InputStreamReader:<init>	(Ljava/io/InputStream;)V
    //   52: lconst_0
    //   53: invokeinterface 241 4 0
    //   58: astore_2
    //   59: aload_0
    //   60: getfield 14	org/h2/jdbc/JdbcConnection:session	Lorg/h2/engine/SessionInterface;
    //   63: aload_2
    //   64: invokeinterface 242 2 0
    //   69: new 243	org/h2/jdbc/JdbcClob
    //   72: dup
    //   73: aload_0
    //   74: aload_2
    //   75: iload_1
    //   76: invokespecial 244	org/h2/jdbc/JdbcClob:<init>	(Lorg/h2/jdbc/JdbcConnection;Lorg/h2/value/Value;I)V
    //   79: astore_3
    //   80: aload_0
    //   81: invokevirtual 112	org/h2/jdbc/JdbcConnection:afterWriting	()V
    //   84: aload_3
    //   85: areturn
    //   86: astore 4
    //   88: aload_0
    //   89: invokevirtual 112	org/h2/jdbc/JdbcConnection:afterWriting	()V
    //   92: aload 4
    //   94: athrow
    //   95: astore_1
    //   96: aload_0
    //   97: aload_1
    //   98: invokevirtual 41	org/h2/jdbc/JdbcConnection:logAndConvert	(Ljava/lang/Exception;)Ljava/sql/SQLException;
    //   101: athrow
    // Line number table:
    //   Java source line #1597	-> byte code offset #0
    //   Java source line #1598	-> byte code offset #6
    //   Java source line #1599	-> byte code offset #17
    //   Java source line #1601	-> byte code offset #21
    //   Java source line #1604	-> byte code offset #59
    //   Java source line #1605	-> byte code offset #69
    //   Java source line #1607	-> byte code offset #80
    //   Java source line #1609	-> byte code offset #95
    //   Java source line #1610	-> byte code offset #96
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	102	0	this	JdbcConnection
    //   5	71	1	i	int
    //   95	3	1	localException	Exception
    //   58	17	2	localValue	Value
    //   86	7	4	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   21	80	86	finally
    //   86	88	86	finally
    //   0	84	95	java/lang/Exception
    //   86	95	95	java/lang/Exception
  }
  
  public SQLXML createSQLXML()
    throws SQLException
  {
    throw unsupported("SQLXML");
  }
  
  public Array createArrayOf(String paramString, Object[] paramArrayOfObject)
    throws SQLException
  {
    throw unsupported("createArray");
  }
  
  public Struct createStruct(String paramString, Object[] paramArrayOfObject)
    throws SQLException
  {
    throw unsupported("Struct");
  }
  
  public synchronized boolean isValid(int paramInt)
  {
    try
    {
      debugCodeCall("isValid", paramInt);
      if ((this.session == null) || (this.session.isClosed())) {
        return false;
      }
      getTransactionIsolation();
      return true;
    }
    catch (Exception localException)
    {
      logAndConvert(localException);
    }
    return false;
  }
  
  public void setClientInfo(String paramString1, String paramString2)
    throws SQLClientInfoException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setClientInfo(" + quote(paramString1) + ", " + quote(paramString2) + ");");
      }
      checkClosed();
      
      throw new SQLClientInfoException();
    }
    catch (Exception localException)
    {
      throw convertToClientInfoException(logAndConvert(localException));
    }
  }
  
  private static SQLClientInfoException convertToClientInfoException(SQLException paramSQLException)
  {
    if ((paramSQLException instanceof SQLClientInfoException)) {
      return (SQLClientInfoException)paramSQLException;
    }
    return new SQLClientInfoException(paramSQLException.getMessage(), paramSQLException.getSQLState(), paramSQLException.getErrorCode(), null, null);
  }
  
  public void setClientInfo(Properties paramProperties)
    throws SQLClientInfoException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setClientInfo(properties);");
      }
      checkClosed();
      
      throw new SQLClientInfoException();
    }
    catch (Exception localException)
    {
      throw convertToClientInfoException(logAndConvert(localException));
    }
  }
  
  public Properties getClientInfo()
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getClientInfo();");
      }
      checkClosed();
      ArrayList localArrayList = this.session.getClusterServers();
      Properties localProperties = new Properties();
      
      localProperties.setProperty("numServers", String.valueOf(localArrayList.size()));
      for (int i = 0; i < localArrayList.size(); i++) {
        localProperties.setProperty("server" + String.valueOf(i), (String)localArrayList.get(i));
      }
      return localProperties;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getClientInfo(String paramString)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCodeCall("getClientInfo", paramString);
      }
      checkClosed();
      Properties localProperties = getClientInfo();
      String str = localProperties.getProperty(paramString);
      if (str == null) {
        throw new SQLClientInfoException();
      }
      return str;
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
  
  public Value createClob(Reader paramReader, long paramLong)
  {
    if (paramReader == null) {
      return ValueNull.INSTANCE;
    }
    if (paramLong <= 0L) {
      paramLong = -1L;
    }
    Value localValue = this.session.getDataHandler().getLobStorage().createClob(paramReader, paramLong);
    this.session.addTemporaryLob(localValue);
    return localValue;
  }
  
  public Value createBlob(InputStream paramInputStream, long paramLong)
  {
    if (paramInputStream == null) {
      return ValueNull.INSTANCE;
    }
    if (paramLong <= 0L) {
      paramLong = -1L;
    }
    Value localValue = this.session.getDataHandler().getLobStorage().createBlob(paramInputStream, paramLong);
    this.session.addTemporaryLob(localValue);
    return localValue;
  }
  
  static void checkMap(Map<String, Class<?>> paramMap)
  {
    if ((paramMap != null) && (paramMap.size() > 0)) {
      throw DbException.getUnsupportedException("map.size > 0");
    }
  }
  
  public String toString()
  {
    return getTraceObjectName() + ": url=" + this.url + " user=" + this.user;
  }
  
  Object convertToDefaultObject(Value paramValue)
  {
    int i;
    switch (paramValue.getType())
    {
    case 16: 
      i = getNextId(10);
      localObject = new JdbcClob(this, paramValue, i);
      break;
    case 15: 
      i = getNextId(9);
      localObject = new JdbcBlob(this, paramValue, i);
      break;
    case 19: 
      if (SysProperties.serializeJavaObject) {
        localObject = JdbcUtils.deserialize(paramValue.getBytesNoCopy(), this.session.getDataHandler());
      }
      break;
    }
    Object localObject = paramValue.getObject();
    
    return localObject;
  }
  
  CompareMode getCompareMode()
  {
    return this.compareMode;
  }
  
  public void setTraceLevel(int paramInt)
  {
    this.trace.setLevel(paramInt);
  }
}
