package org.h2.server;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.h2.Driver;
import org.h2.message.DbException;
import org.h2.util.JdbcUtils;
import org.h2.util.NetUtils;
import org.h2.util.New;
import org.h2.util.Tool;

public class TcpServer
  implements Service
{
  private static final int SHUTDOWN_NORMAL = 0;
  private static final int SHUTDOWN_FORCE = 1;
  private static final String MANAGEMENT_DB_PREFIX = "management_db_";
  private static final Map<Integer, TcpServer> SERVERS = Collections.synchronizedMap(new HashMap());
  private int port;
  private boolean portIsSet;
  private boolean trace;
  private boolean ssl;
  private boolean stop;
  private ShutdownHandler shutdownHandler;
  private ServerSocket serverSocket;
  private final Set<TcpServerThread> running = Collections.synchronizedSet(new HashSet());
  private String baseDir;
  private boolean allowOthers;
  private boolean isDaemon;
  private boolean ifExists;
  private Connection managementDb;
  private PreparedStatement managementDbAdd;
  private PreparedStatement managementDbRemove;
  private String managementPassword = "";
  private Thread listenerThread;
  private int nextThreadId;
  private String key;
  private String keyDatabase;
  
  public static String getManagementDbName(int paramInt)
  {
    return "mem:management_db_" + paramInt;
  }
  
  private void initManagementDb()
    throws SQLException
  {
    Properties localProperties = new Properties();
    localProperties.setProperty("user", "");
    localProperties.setProperty("password", this.managementPassword);
    
    Connection localConnection = Driver.load().connect("jdbc:h2:" + getManagementDbName(this.port), localProperties);
    
    this.managementDb = localConnection;
    Statement localStatement = null;
    try
    {
      localStatement = localConnection.createStatement();
      localStatement.execute("CREATE ALIAS IF NOT EXISTS STOP_SERVER FOR \"" + TcpServer.class.getName() + ".stopServer\"");
      
      localStatement.execute("CREATE TABLE IF NOT EXISTS SESSIONS(ID INT PRIMARY KEY, URL VARCHAR, USER VARCHAR, CONNECTED TIMESTAMP)");
      
      this.managementDbAdd = localConnection.prepareStatement("INSERT INTO SESSIONS VALUES(?, ?, ?, NOW())");
      
      this.managementDbRemove = localConnection.prepareStatement("DELETE FROM SESSIONS WHERE ID=?");
    }
    finally
    {
      JdbcUtils.closeSilently(localStatement);
    }
    SERVERS.put(Integer.valueOf(this.port), this);
  }
  
  void shutdown()
  {
    if (this.shutdownHandler != null) {
      this.shutdownHandler.shutdown();
    }
  }
  
  public void setShutdownHandler(ShutdownHandler paramShutdownHandler)
  {
    this.shutdownHandler = paramShutdownHandler;
  }
  
  synchronized void addConnection(int paramInt, String paramString1, String paramString2)
  {
    try
    {
      this.managementDbAdd.setInt(1, paramInt);
      this.managementDbAdd.setString(2, paramString1);
      this.managementDbAdd.setString(3, paramString2);
      this.managementDbAdd.execute();
    }
    catch (SQLException localSQLException)
    {
      DbException.traceThrowable(localSQLException);
    }
  }
  
  synchronized void removeConnection(int paramInt)
  {
    try
    {
      this.managementDbRemove.setInt(1, paramInt);
      this.managementDbRemove.execute();
    }
    catch (SQLException localSQLException)
    {
      DbException.traceThrowable(localSQLException);
    }
  }
  
  private synchronized void stopManagementDb()
  {
    if (this.managementDb != null)
    {
      try
      {
        this.managementDb.close();
      }
      catch (SQLException localSQLException)
      {
        DbException.traceThrowable(localSQLException);
      }
      this.managementDb = null;
    }
  }
  
  public void init(String... paramVarArgs)
  {
    this.port = 9092;
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++)
    {
      String str = paramVarArgs[i];
      if (Tool.isOption(str, "-trace"))
      {
        this.trace = true;
      }
      else if (Tool.isOption(str, "-tcpSSL"))
      {
        this.ssl = true;
      }
      else if (Tool.isOption(str, "-tcpPort"))
      {
        this.port = Integer.decode(paramVarArgs[(++i)]).intValue();
        this.portIsSet = true;
      }
      else if (Tool.isOption(str, "-tcpPassword"))
      {
        this.managementPassword = paramVarArgs[(++i)];
      }
      else if (Tool.isOption(str, "-baseDir"))
      {
        this.baseDir = paramVarArgs[(++i)];
      }
      else if (Tool.isOption(str, "-key"))
      {
        this.key = paramVarArgs[(++i)];
        this.keyDatabase = paramVarArgs[(++i)];
      }
      else if (Tool.isOption(str, "-tcpAllowOthers"))
      {
        this.allowOthers = true;
      }
      else if (Tool.isOption(str, "-tcpDaemon"))
      {
        this.isDaemon = true;
      }
      else if (Tool.isOption(str, "-ifExists"))
      {
        this.ifExists = true;
      }
    }
    Driver.load();
  }
  
  public String getURL()
  {
    return (this.ssl ? "ssl" : "tcp") + "://" + NetUtils.getLocalAddress() + ":" + this.port;
  }
  
  public int getPort()
  {
    return this.port;
  }
  
  boolean allow(Socket paramSocket)
  {
    if (this.allowOthers) {
      return true;
    }
    try
    {
      return NetUtils.isLocalAddress(paramSocket);
    }
    catch (UnknownHostException localUnknownHostException)
    {
      traceError(localUnknownHostException);
    }
    return false;
  }
  
  public synchronized void start()
    throws SQLException
  {
    this.stop = false;
    try
    {
      this.serverSocket = NetUtils.createServerSocket(this.port, this.ssl);
    }
    catch (DbException localDbException)
    {
      if (!this.portIsSet) {
        this.serverSocket = NetUtils.createServerSocket(0, this.ssl);
      } else {
        throw localDbException;
      }
    }
    this.port = this.serverSocket.getLocalPort();
    initManagementDb();
  }
  
  public void listen()
  {
    this.listenerThread = Thread.currentThread();
    String str = this.listenerThread.getName();
    try
    {
      while (!this.stop)
      {
        Socket localSocket = this.serverSocket.accept();
        TcpServerThread localTcpServerThread = new TcpServerThread(localSocket, this, this.nextThreadId++);
        this.running.add(localTcpServerThread);
        Thread localThread = new Thread(localTcpServerThread, str + " thread");
        localThread.setDaemon(this.isDaemon);
        localTcpServerThread.setThread(localThread);
        localThread.start();
      }
      this.serverSocket = NetUtils.closeSilently(this.serverSocket);
    }
    catch (Exception localException)
    {
      if (!this.stop) {
        DbException.traceThrowable(localException);
      }
    }
    stopManagementDb();
  }
  
  public synchronized boolean isRunning(boolean paramBoolean)
  {
    if (this.serverSocket == null) {
      return false;
    }
    try
    {
      Socket localSocket = NetUtils.createLoopbackSocket(this.port, this.ssl);
      localSocket.close();
      return true;
    }
    catch (Exception localException)
    {
      if (paramBoolean) {
        traceError(localException);
      }
    }
    return false;
  }
  
  public void stop()
  {
    SERVERS.remove(Integer.valueOf(this.port));
    if (!this.stop)
    {
      stopManagementDb();
      this.stop = true;
      if (this.serverSocket != null)
      {
        try
        {
          this.serverSocket.close();
        }
        catch (IOException localIOException)
        {
          DbException.traceThrowable(localIOException);
        }
        catch (NullPointerException localNullPointerException) {}
        this.serverSocket = null;
      }
      if (this.listenerThread != null) {
        try
        {
          this.listenerThread.join(1000L);
        }
        catch (InterruptedException localInterruptedException)
        {
          DbException.traceThrowable(localInterruptedException);
        }
      }
    }
    for (TcpServerThread localTcpServerThread : New.arrayList(this.running)) {
      if (localTcpServerThread != null)
      {
        localTcpServerThread.close();
        try
        {
          localTcpServerThread.getThread().join(100L);
        }
        catch (Exception localException)
        {
          DbException.traceThrowable(localException);
        }
      }
    }
  }
  
  public static void stopServer(int paramInt1, String paramString, int paramInt2)
  {
    if (paramInt1 == 0)
    {
      localObject = (Integer[])SERVERS.keySet().toArray(new Integer[0]);int i = localObject.length;
      for (int j = 0; j < i; j++)
      {
        int k = localObject[j].intValue();
        if (k != 0) {
          stopServer(k, paramString, paramInt2);
        }
      }
      return;
    }
    Object localObject = (TcpServer)SERVERS.get(Integer.valueOf(paramInt1));
    if (localObject == null) {
      return;
    }
    if (!((TcpServer)localObject).managementPassword.equals(paramString)) {
      return;
    }
    if (paramInt2 == 0)
    {
      ((TcpServer)localObject).stopManagementDb();
      ((TcpServer)localObject).stop = true;
      try
      {
        Socket localSocket = NetUtils.createLoopbackSocket(paramInt1, false);
        localSocket.close();
      }
      catch (Exception localException) {}
    }
    else if (paramInt2 == 1)
    {
      ((TcpServer)localObject).stop();
    }
    ((TcpServer)localObject).shutdown();
  }
  
  void remove(TcpServerThread paramTcpServerThread)
  {
    this.running.remove(paramTcpServerThread);
  }
  
  String getBaseDir()
  {
    return this.baseDir;
  }
  
  void trace(String paramString)
  {
    if (this.trace) {
      System.out.println(paramString);
    }
  }
  
  void traceError(Throwable paramThrowable)
  {
    if (this.trace) {
      paramThrowable.printStackTrace();
    }
  }
  
  public boolean getAllowOthers()
  {
    return this.allowOthers;
  }
  
  public String getType()
  {
    return "TCP";
  }
  
  public String getName()
  {
    return "H2 TCP Server";
  }
  
  boolean getIfExists()
  {
    return this.ifExists;
  }
  
  /* Error */
  public static synchronized void shutdown(String paramString1, String paramString2, boolean paramBoolean1, boolean paramBoolean2)
    throws SQLException
  {
    // Byte code:
    //   0: sipush 9092
    //   3: istore 4
    //   5: aload_0
    //   6: bipush 58
    //   8: invokevirtual 134	java/lang/String:lastIndexOf	(I)I
    //   11: istore 5
    //   13: iload 5
    //   15: iflt +31 -> 46
    //   18: aload_0
    //   19: iload 5
    //   21: iconst_1
    //   22: iadd
    //   23: invokevirtual 135	java/lang/String:substring	(I)Ljava/lang/String;
    //   26: astore 6
    //   28: aload 6
    //   30: invokestatic 136	org/h2/util/StringUtils:isNumber	(Ljava/lang/String;)Z
    //   33: ifeq +13 -> 46
    //   36: aload 6
    //   38: invokestatic 55	java/lang/Integer:decode	(Ljava/lang/String;)Ljava/lang/Integer;
    //   41: invokevirtual 56	java/lang/Integer:intValue	()I
    //   44: istore 4
    //   46: iload 4
    //   48: invokestatic 22	org/h2/server/TcpServer:getManagementDbName	(I)Ljava/lang/String;
    //   51: astore 6
    //   53: invokestatic 19	org/h2/Driver:load	()Lorg/h2/Driver;
    //   56: pop
    //   57: goto +11 -> 68
    //   60: astore 7
    //   62: aload 7
    //   64: invokestatic 138	org/h2/message/DbException:convert	(Ljava/lang/Throwable;)Lorg/h2/message/DbException;
    //   67: athrow
    //   68: iconst_0
    //   69: istore 7
    //   71: iload 7
    //   73: iconst_2
    //   74: if_icmpge +192 -> 266
    //   77: aconst_null
    //   78: astore 8
    //   80: aconst_null
    //   81: astore 9
    //   83: new 8	java/lang/StringBuilder
    //   86: dup
    //   87: invokespecial 9	java/lang/StringBuilder:<init>	()V
    //   90: ldc 20
    //   92: invokevirtual 11	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   95: aload_0
    //   96: invokevirtual 11	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   99: ldc -117
    //   101: invokevirtual 11	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   104: aload 6
    //   106: invokevirtual 11	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   109: invokevirtual 13	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   112: ldc 6
    //   114: aload_1
    //   115: invokestatic 140	java/sql/DriverManager:getConnection	(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/sql/Connection;
    //   118: astore 8
    //   120: aload 8
    //   122: ldc -115
    //   124: invokeinterface 33 2 0
    //   129: astore 9
    //   131: aload 9
    //   133: iconst_1
    //   134: iload_3
    //   135: ifeq +7 -> 142
    //   138: iconst_0
    //   139: goto +5 -> 144
    //   142: iload 4
    //   144: invokeinterface 43 3 0
    //   149: aload 9
    //   151: iconst_2
    //   152: aload_1
    //   153: invokeinterface 44 3 0
    //   158: aload 9
    //   160: iconst_3
    //   161: iload_2
    //   162: ifeq +7 -> 169
    //   165: iconst_1
    //   166: goto +4 -> 170
    //   169: iconst_0
    //   170: invokeinterface 43 3 0
    //   175: aload 9
    //   177: invokeinterface 45 1 0
    //   182: pop
    //   183: goto +25 -> 208
    //   186: astore 10
    //   188: iload_2
    //   189: ifeq +6 -> 195
    //   192: goto +16 -> 208
    //   195: aload 10
    //   197: invokevirtual 142	java/sql/SQLException:getErrorCode	()I
    //   200: ldc -113
    //   202: if_icmpeq +6 -> 208
    //   205: aload 10
    //   207: athrow
    //   208: aload 9
    //   210: invokestatic 37	org/h2/util/JdbcUtils:closeSilently	(Ljava/sql/Statement;)V
    //   213: aload 8
    //   215: invokestatic 144	org/h2/util/JdbcUtils:closeSilently	(Ljava/sql/Connection;)V
    //   218: goto +48 -> 266
    //   221: astore 10
    //   223: iload 7
    //   225: iconst_1
    //   226: if_icmpne +6 -> 232
    //   229: aload 10
    //   231: athrow
    //   232: aload 9
    //   234: invokestatic 37	org/h2/util/JdbcUtils:closeSilently	(Ljava/sql/Statement;)V
    //   237: aload 8
    //   239: invokestatic 144	org/h2/util/JdbcUtils:closeSilently	(Ljava/sql/Connection;)V
    //   242: goto +18 -> 260
    //   245: astore 11
    //   247: aload 9
    //   249: invokestatic 37	org/h2/util/JdbcUtils:closeSilently	(Ljava/sql/Statement;)V
    //   252: aload 8
    //   254: invokestatic 144	org/h2/util/JdbcUtils:closeSilently	(Ljava/sql/Connection;)V
    //   257: aload 11
    //   259: athrow
    //   260: iinc 7 1
    //   263: goto -192 -> 71
    //   266: goto +11 -> 277
    //   269: astore 4
    //   271: aload 4
    //   273: invokestatic 145	org/h2/message/DbException:toSQLException	(Ljava/lang/Exception;)Ljava/sql/SQLException;
    //   276: athrow
    //   277: return
    // Line number table:
    //   Java source line #433	-> byte code offset #0
    //   Java source line #434	-> byte code offset #5
    //   Java source line #435	-> byte code offset #13
    //   Java source line #436	-> byte code offset #18
    //   Java source line #437	-> byte code offset #28
    //   Java source line #438	-> byte code offset #36
    //   Java source line #441	-> byte code offset #46
    //   Java source line #443	-> byte code offset #53
    //   Java source line #446	-> byte code offset #57
    //   Java source line #444	-> byte code offset #60
    //   Java source line #445	-> byte code offset #62
    //   Java source line #447	-> byte code offset #68
    //   Java source line #448	-> byte code offset #77
    //   Java source line #449	-> byte code offset #80
    //   Java source line #451	-> byte code offset #83
    //   Java source line #452	-> byte code offset #120
    //   Java source line #453	-> byte code offset #131
    //   Java source line #454	-> byte code offset #149
    //   Java source line #455	-> byte code offset #158
    //   Java source line #457	-> byte code offset #175
    //   Java source line #466	-> byte code offset #183
    //   Java source line #458	-> byte code offset #186
    //   Java source line #459	-> byte code offset #188
    //   Java source line #462	-> byte code offset #195
    //   Java source line #463	-> byte code offset #205
    //   Java source line #473	-> byte code offset #208
    //   Java source line #474	-> byte code offset #213
    //   Java source line #468	-> byte code offset #221
    //   Java source line #469	-> byte code offset #223
    //   Java source line #470	-> byte code offset #229
    //   Java source line #473	-> byte code offset #232
    //   Java source line #474	-> byte code offset #237
    //   Java source line #475	-> byte code offset #242
    //   Java source line #473	-> byte code offset #245
    //   Java source line #474	-> byte code offset #252
    //   Java source line #447	-> byte code offset #260
    //   Java source line #479	-> byte code offset #266
    //   Java source line #477	-> byte code offset #269
    //   Java source line #478	-> byte code offset #271
    //   Java source line #480	-> byte code offset #277
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	278	0	paramString1	String
    //   0	278	1	paramString2	String
    //   0	278	2	paramBoolean1	boolean
    //   0	278	3	paramBoolean2	boolean
    //   3	140	4	i	int
    //   269	3	4	localException	Exception
    //   11	12	5	j	int
    //   26	79	6	str	String
    //   60	3	7	localThrowable	Throwable
    //   69	192	7	k	int
    //   78	175	8	localConnection	Connection
    //   81	167	9	localPreparedStatement	PreparedStatement
    //   186	20	10	localSQLException1	SQLException
    //   221	9	10	localSQLException2	SQLException
    //   245	13	11	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   53	57	60	java/lang/Throwable
    //   175	183	186	java/sql/SQLException
    //   83	208	221	java/sql/SQLException
    //   83	208	245	finally
    //   221	232	245	finally
    //   245	247	245	finally
    //   0	266	269	java/lang/Exception
  }
  
  void cancelStatement(String paramString, int paramInt)
  {
    for (TcpServerThread localTcpServerThread : New.arrayList(this.running)) {
      if (localTcpServerThread != null) {
        localTcpServerThread.cancelStatement(paramString, paramInt);
      }
    }
  }
  
  public String checkKeyAndGetDatabaseName(String paramString)
  {
    if (this.key == null) {
      return paramString;
    }
    if (this.key.equals(paramString)) {
      return this.keyDatabase;
    }
    throw DbException.get(28000);
  }
  
  public boolean isDaemon()
  {
    return this.isDaemon;
  }
}
