package org.h2.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import org.h2.command.CommandInterface;
import org.h2.engine.SessionInterface;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.message.TraceObject;
import org.h2.result.ResultInterface;
import org.h2.util.New;

public class JdbcStatement
  extends TraceObject
  implements Statement
{
  protected JdbcConnection conn;
  protected SessionInterface session;
  protected JdbcResultSet resultSet;
  protected int maxRows;
  protected int fetchSize = SysProperties.SERVER_RESULT_SET_FETCH_SIZE;
  protected int updateCount;
  protected final int resultSetType;
  protected final int resultSetConcurrency;
  protected final boolean closedByResultSet;
  private CommandInterface executingCommand;
  private int lastExecutedCommandType;
  private ArrayList<String> batchCommands;
  private boolean escapeProcessing = true;
  private boolean cancelled;
  
  JdbcStatement(JdbcConnection paramJdbcConnection, int paramInt1, int paramInt2, int paramInt3, boolean paramBoolean)
  {
    this.conn = paramJdbcConnection;
    this.session = paramJdbcConnection.getSession();
    setTrace(this.session.getTrace(), 8, paramInt1);
    this.resultSetType = paramInt2;
    this.resultSetConcurrency = paramInt3;
    this.closedByResultSet = paramBoolean;
  }
  
  public ResultSet executeQuery(String paramString)
    throws SQLException
  {
    try
    {
      int i = getNextId(4);
      if (isDebugEnabled()) {
        debugCodeAssign("ResultSet", 4, i, "executeQuery(" + quote(paramString) + ")");
      }
      synchronized (this.session)
      {
        checkClosed();
        closeOldResultSet();
        paramString = JdbcConnection.translateSQL(paramString, this.escapeProcessing);
        CommandInterface localCommandInterface = this.conn.prepareCommand(paramString, this.fetchSize);
        
        boolean bool1 = this.resultSetType != 1003;
        boolean bool2 = this.resultSetConcurrency == 1008;
        setExecutingStatement(localCommandInterface);
        ResultInterface localResultInterface;
        try
        {
          localResultInterface = localCommandInterface.executeQuery(this.maxRows, bool1);
        }
        finally
        {
          setExecutingStatement(null);
        }
        localCommandInterface.close();
        this.resultSet = new JdbcResultSet(this.conn, this, localResultInterface, i, this.closedByResultSet, bool1, bool2);
      }
      return this.resultSet;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int executeUpdate(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("executeUpdate", paramString);
      return executeUpdateInternal(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  private int executeUpdateInternal(String paramString)
    throws SQLException
  {
    checkClosedForWrite();
    try
    {
      closeOldResultSet();
      paramString = JdbcConnection.translateSQL(paramString, this.escapeProcessing);
      CommandInterface localCommandInterface = this.conn.prepareCommand(paramString, this.fetchSize);
      synchronized (this.session)
      {
        setExecutingStatement(localCommandInterface);
        try
        {
          this.updateCount = localCommandInterface.executeUpdate();
        }
        finally
        {
          setExecutingStatement(null);
        }
      }
      localCommandInterface.close();
      return this.updateCount;
    }
    finally
    {
      afterWriting();
    }
  }
  
  public boolean execute(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("execute", paramString);
      return executeInternal(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  private boolean executeInternal(String paramString)
    throws SQLException
  {
    int i = getNextId(4);
    checkClosedForWrite();
    try
    {
      closeOldResultSet();
      paramString = JdbcConnection.translateSQL(paramString, this.escapeProcessing);
      CommandInterface localCommandInterface = this.conn.prepareCommand(paramString, this.fetchSize);
      Object localObject1;
      synchronized (this.session)
      {
        setExecutingStatement(localCommandInterface);
        try
        {
          if (localCommandInterface.isQuery())
          {
            localObject1 = 1;
            boolean bool1 = this.resultSetType != 1003;
            boolean bool2 = this.resultSetConcurrency == 1008;
            ResultInterface localResultInterface = localCommandInterface.executeQuery(this.maxRows, bool1);
            this.resultSet = new JdbcResultSet(this.conn, this, localResultInterface, i, this.closedByResultSet, bool1, bool2);
          }
          else
          {
            localObject1 = 0;
            this.updateCount = localCommandInterface.executeUpdate();
          }
        }
        finally
        {
          setExecutingStatement(null);
        }
      }
      localCommandInterface.close();
      return (boolean)localObject1;
    }
    finally
    {
      afterWriting();
    }
  }
  
  public ResultSet getResultSet()
    throws SQLException
  {
    try
    {
      checkClosed();
      if (this.resultSet != null)
      {
        int i = this.resultSet.getTraceId();
        debugCodeAssign("ResultSet", 4, i, "getResultSet()");
      }
      else
      {
        debugCodeCall("getResultSet");
      }
      return this.resultSet;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getUpdateCount()
    throws SQLException
  {
    try
    {
      debugCodeCall("getUpdateCount");
      checkClosed();
      return this.updateCount;
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
      synchronized (this.session)
      {
        closeOldResultSet();
        if (this.conn != null) {
          this.conn = null;
        }
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public Connection getConnection()
  {
    debugCodeCall("getConnection");
    return this.conn;
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
  
  public void setCursorName(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("setCursorName", paramString);
      checkClosed();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setFetchDirection(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("setFetchDirection", paramInt);
      checkClosed();
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
  
  public int getMaxRows()
    throws SQLException
  {
    try
    {
      debugCodeCall("getMaxRows");
      checkClosed();
      return this.maxRows;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setMaxRows(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("setMaxRows", paramInt);
      checkClosed();
      if (paramInt < 0) {
        throw DbException.getInvalidValueException("maxRows", Integer.valueOf(paramInt));
      }
      this.maxRows = paramInt;
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
      if ((paramInt < 0) || ((paramInt > 0) && (this.maxRows > 0) && (paramInt > this.maxRows))) {
        throw DbException.getInvalidValueException("rows", Integer.valueOf(paramInt));
      }
      if (paramInt == 0) {
        paramInt = SysProperties.SERVER_RESULT_SET_FETCH_SIZE;
      }
      this.fetchSize = paramInt;
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
      return this.fetchSize;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getResultSetConcurrency()
    throws SQLException
  {
    try
    {
      debugCodeCall("getResultSetConcurrency");
      checkClosed();
      return this.resultSetConcurrency;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getResultSetType()
    throws SQLException
  {
    try
    {
      debugCodeCall("getResultSetType");
      checkClosed();
      return this.resultSetType;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getMaxFieldSize()
    throws SQLException
  {
    try
    {
      debugCodeCall("getMaxFieldSize");
      checkClosed();
      return 0;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setMaxFieldSize(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("setMaxFieldSize", paramInt);
      checkClosed();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setEscapeProcessing(boolean paramBoolean)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setEscapeProcessing(" + paramBoolean + ");");
      }
      checkClosed();
      this.escapeProcessing = paramBoolean;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void cancel()
    throws SQLException
  {
    try
    {
      debugCodeCall("cancel");
      checkClosed();
      
      CommandInterface localCommandInterface = this.executingCommand;
      try
      {
        if (localCommandInterface != null)
        {
          localCommandInterface.cancel();
          this.cancelled = true;
        }
      }
      finally
      {
        setExecutingStatement(null);
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean wasCancelled()
  {
    return this.cancelled;
  }
  
  public int getQueryTimeout()
    throws SQLException
  {
    try
    {
      debugCodeCall("getQueryTimeout");
      checkClosed();
      return this.conn.getQueryTimeout();
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
      if (paramInt < 0) {
        throw DbException.getInvalidValueException("seconds", Integer.valueOf(paramInt));
      }
      this.conn.setQueryTimeout(paramInt);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void addBatch(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("addBatch", paramString);
      checkClosed();
      paramString = JdbcConnection.translateSQL(paramString, this.escapeProcessing);
      if (this.batchCommands == null) {
        this.batchCommands = New.arrayList();
      }
      this.batchCommands.add(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void clearBatch()
    throws SQLException
  {
    try
    {
      debugCodeCall("clearBatch");
      checkClosed();
      this.batchCommands = null;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  /* Error */
  public int[] executeBatch()
    throws SQLException
  {
    // Byte code:
    //   0: aload_0
    //   1: ldc 90
    //   3: invokevirtual 50	org/h2/jdbc/JdbcStatement:debugCodeCall	(Ljava/lang/String;)V
    //   6: aload_0
    //   7: invokevirtual 40	org/h2/jdbc/JdbcStatement:checkClosedForWrite	()Z
    //   10: pop
    //   11: aload_0
    //   12: getfield 86	org/h2/jdbc/JdbcStatement:batchCommands	Ljava/util/ArrayList;
    //   15: ifnonnull +10 -> 25
    //   18: aload_0
    //   19: invokestatic 87	org/h2/util/New:arrayList	()Ljava/util/ArrayList;
    //   22: putfield 86	org/h2/jdbc/JdbcStatement:batchCommands	Ljava/util/ArrayList;
    //   25: aload_0
    //   26: getfield 86	org/h2/jdbc/JdbcStatement:batchCommands	Ljava/util/ArrayList;
    //   29: invokevirtual 91	java/util/ArrayList:size	()I
    //   32: istore_1
    //   33: iload_1
    //   34: newarray <illegal type>
    //   36: astore_2
    //   37: iconst_0
    //   38: istore_3
    //   39: aconst_null
    //   40: astore 4
    //   42: iconst_0
    //   43: istore 5
    //   45: iload 5
    //   47: iload_1
    //   48: if_icmpge +77 -> 125
    //   51: aload_0
    //   52: getfield 86	org/h2/jdbc/JdbcStatement:batchCommands	Ljava/util/ArrayList;
    //   55: iload 5
    //   57: invokevirtual 92	java/util/ArrayList:get	(I)Ljava/lang/Object;
    //   60: checkcast 93	java/lang/String
    //   63: astore 6
    //   65: aload_2
    //   66: iload 5
    //   68: aload_0
    //   69: aload 6
    //   71: invokespecial 39	org/h2/jdbc/JdbcStatement:executeUpdateInternal	(Ljava/lang/String;)I
    //   74: iastore
    //   75: goto +44 -> 119
    //   78: astore 7
    //   80: aload_0
    //   81: aload 7
    //   83: invokevirtual 36	org/h2/jdbc/JdbcStatement:logAndConvert	(Ljava/lang/Exception;)Ljava/sql/SQLException;
    //   86: astore 8
    //   88: aload 4
    //   90: ifnonnull +10 -> 100
    //   93: aload 8
    //   95: astore 4
    //   97: goto +14 -> 111
    //   100: aload 8
    //   102: aload 4
    //   104: invokevirtual 94	java/sql/SQLException:setNextException	(Ljava/sql/SQLException;)V
    //   107: aload 8
    //   109: astore 4
    //   111: aload_2
    //   112: iload 5
    //   114: bipush -3
    //   116: iastore
    //   117: iconst_1
    //   118: istore_3
    //   119: iinc 5 1
    //   122: goto -77 -> 45
    //   125: aload_0
    //   126: aconst_null
    //   127: putfield 86	org/h2/jdbc/JdbcStatement:batchCommands	Ljava/util/ArrayList;
    //   130: iload_3
    //   131: ifeq +14 -> 145
    //   134: new 95	org/h2/jdbc/JdbcBatchUpdateException
    //   137: dup
    //   138: aload 4
    //   140: aload_2
    //   141: invokespecial 96	org/h2/jdbc/JdbcBatchUpdateException:<init>	(Ljava/sql/SQLException;[I)V
    //   144: athrow
    //   145: aload_2
    //   146: astore 5
    //   148: aload_0
    //   149: invokevirtual 43	org/h2/jdbc/JdbcStatement:afterWriting	()V
    //   152: aload 5
    //   154: areturn
    //   155: astore 9
    //   157: aload_0
    //   158: invokevirtual 43	org/h2/jdbc/JdbcStatement:afterWriting	()V
    //   161: aload 9
    //   163: athrow
    //   164: astore_1
    //   165: aload_0
    //   166: aload_1
    //   167: invokevirtual 36	org/h2/jdbc/JdbcStatement:logAndConvert	(Ljava/lang/Exception;)Ljava/sql/SQLException;
    //   170: athrow
    // Line number table:
    //   Java source line #646	-> byte code offset #0
    //   Java source line #647	-> byte code offset #6
    //   Java source line #649	-> byte code offset #11
    //   Java source line #652	-> byte code offset #18
    //   Java source line #654	-> byte code offset #25
    //   Java source line #655	-> byte code offset #33
    //   Java source line #656	-> byte code offset #37
    //   Java source line #657	-> byte code offset #39
    //   Java source line #658	-> byte code offset #42
    //   Java source line #659	-> byte code offset #51
    //   Java source line #661	-> byte code offset #65
    //   Java source line #672	-> byte code offset #75
    //   Java source line #662	-> byte code offset #78
    //   Java source line #663	-> byte code offset #80
    //   Java source line #664	-> byte code offset #88
    //   Java source line #665	-> byte code offset #93
    //   Java source line #667	-> byte code offset #100
    //   Java source line #668	-> byte code offset #107
    //   Java source line #670	-> byte code offset #111
    //   Java source line #671	-> byte code offset #117
    //   Java source line #658	-> byte code offset #119
    //   Java source line #674	-> byte code offset #125
    //   Java source line #675	-> byte code offset #130
    //   Java source line #676	-> byte code offset #134
    //   Java source line #678	-> byte code offset #145
    //   Java source line #680	-> byte code offset #148
    //   Java source line #682	-> byte code offset #164
    //   Java source line #683	-> byte code offset #165
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	171	0	this	JdbcStatement
    //   32	17	1	i	int
    //   164	3	1	localException1	Exception
    //   36	110	2	arrayOfInt1	int[]
    //   38	93	3	j	int
    //   40	99	4	localObject1	Object
    //   43	77	5	k	int
    //   63	7	6	str	String
    //   78	4	7	localException2	Exception
    //   86	22	8	localSQLException	SQLException
    //   155	7	9	localObject2	Object
    // Exception table:
    //   from	to	target	type
    //   65	75	78	java/lang/Exception
    //   11	148	155	finally
    //   155	157	155	finally
    //   0	152	164	java/lang/Exception
    //   155	164	164	java/lang/Exception
  }
  
  public ResultSet getGeneratedKeys()
    throws SQLException
  {
    try
    {
      int i = getNextId(4);
      if (isDebugEnabled()) {
        debugCodeAssign("ResultSet", 4, i, "getGeneratedKeys()");
      }
      checkClosed();
      return this.conn.getGeneratedKeys(this, i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean getMoreResults()
    throws SQLException
  {
    try
    {
      debugCodeCall("getMoreResults");
      checkClosed();
      closeOldResultSet();
      return false;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean getMoreResults(int paramInt)
    throws SQLException
  {
    try
    {
      debugCodeCall("getMoreResults", paramInt);
      switch (paramInt)
      {
      case 1: 
      case 3: 
        checkClosed();
        closeOldResultSet();
        break;
      case 2: 
        break;
      default: 
        throw DbException.getInvalidValueException("current", Integer.valueOf(paramInt));
      }
      return false;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int executeUpdate(String paramString, int paramInt)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("executeUpdate(" + quote(paramString) + ", " + paramInt + ");");
      }
      return executeUpdateInternal(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int executeUpdate(String paramString, int[] paramArrayOfInt)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("executeUpdate(" + quote(paramString) + ", " + quoteIntArray(paramArrayOfInt) + ");");
      }
      return executeUpdateInternal(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int executeUpdate(String paramString, String[] paramArrayOfString)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("executeUpdate(" + quote(paramString) + ", " + quoteArray(paramArrayOfString) + ");");
      }
      return executeUpdateInternal(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean execute(String paramString, int paramInt)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("execute(" + quote(paramString) + ", " + paramInt + ");");
      }
      return executeInternal(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean execute(String paramString, int[] paramArrayOfInt)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("execute(" + quote(paramString) + ", " + quoteIntArray(paramArrayOfInt) + ");");
      }
      return executeInternal(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean execute(String paramString, String[] paramArrayOfString)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("execute(" + quote(paramString) + ", " + quoteArray(paramArrayOfString) + ");");
      }
      return executeInternal(paramString);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int getResultSetHoldability()
    throws SQLException
  {
    try
    {
      debugCodeCall("getResultSetHoldability");
      checkClosed();
      return 1;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  boolean checkClosed()
  {
    return checkClosed(false);
  }
  
  boolean checkClosedForWrite()
  {
    return checkClosed(true);
  }
  
  protected boolean checkClosed(boolean paramBoolean)
  {
    if (this.conn == null) {
      throw DbException.get(90007);
    }
    this.conn.checkClosed(paramBoolean);
    SessionInterface localSessionInterface = this.conn.getSession();
    if (localSessionInterface != this.session)
    {
      this.session = localSessionInterface;
      this.trace = this.session.getTrace();
      return true;
    }
    return false;
  }
  
  void afterWriting()
  {
    if (this.conn != null) {
      this.conn.afterWriting();
    }
  }
  
  protected void closeOldResultSet()
    throws SQLException
  {
    try
    {
      if ((!this.closedByResultSet) && 
        (this.resultSet != null)) {
        this.resultSet.closeInternal();
      }
    }
    finally
    {
      this.cancelled = false;
      this.resultSet = null;
      this.updateCount = -1;
    }
  }
  
  protected void setExecutingStatement(CommandInterface paramCommandInterface)
  {
    if (paramCommandInterface == null)
    {
      this.conn.setExecutingStatement(null);
    }
    else
    {
      this.conn.setExecutingStatement(this);
      this.lastExecutedCommandType = paramCommandInterface.getCommandType();
    }
    this.executingCommand = paramCommandInterface;
  }
  
  public int getLastExecutedCommandType()
  {
    return this.lastExecutedCommandType;
  }
  
  public boolean isClosed()
    throws SQLException
  {
    try
    {
      debugCodeCall("isClosed");
      return this.conn == null;
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
  
  public boolean isPoolable()
  {
    debugCodeCall("isPoolable");
    return false;
  }
  
  public void setPoolable(boolean paramBoolean)
  {
    if (isDebugEnabled()) {
      debugCode("setPoolable(" + paramBoolean + ");");
    }
  }
  
  public String toString()
  {
    return getTraceObjectName();
  }
}
