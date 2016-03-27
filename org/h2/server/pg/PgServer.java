package org.h2.server.pg;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.h2.Driver;
import org.h2.engine.Constants;
import org.h2.message.DbException;
import org.h2.server.Service;
import org.h2.util.NetUtils;
import org.h2.util.New;
import org.h2.util.Tool;

public class PgServer
  implements Service
{
  public static final int DEFAULT_PORT = 5435;
  public static final int PG_TYPE_VARCHAR = 1043;
  public static final int PG_TYPE_INT2VECTOR = 22;
  public static final int PG_TYPE_BOOL = 16;
  public static final int PG_TYPE_BYTEA = 17;
  public static final int PG_TYPE_BPCHAR = 1042;
  public static final int PG_TYPE_INT8 = 20;
  public static final int PG_TYPE_INT2 = 21;
  public static final int PG_TYPE_INT4 = 23;
  public static final int PG_TYPE_TEXT = 25;
  public static final int PG_TYPE_OID = 26;
  public static final int PG_TYPE_FLOAT4 = 700;
  public static final int PG_TYPE_FLOAT8 = 701;
  public static final int PG_TYPE_UNKNOWN = 705;
  public static final int PG_TYPE_TEXTARRAY = 1009;
  public static final int PG_TYPE_DATE = 1082;
  public static final int PG_TYPE_TIME = 1083;
  public static final int PG_TYPE_TIMESTAMP_NO_TMZONE = 1114;
  public static final int PG_TYPE_NUMERIC = 1700;
  private final HashSet<Integer> typeSet = New.hashSet();
  private int port = 5435;
  private boolean portIsSet;
  private boolean stop;
  private boolean trace;
  private ServerSocket serverSocket;
  private final Set<PgServerThread> running = Collections.synchronizedSet(new HashSet());
  private final AtomicInteger pid = new AtomicInteger();
  private String baseDir;
  private boolean allowOthers;
  private boolean isDaemon;
  private boolean ifExists;
  private String key;
  private String keyDatabase;
  
  public void init(String... paramVarArgs)
  {
    this.port = 5435;
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++)
    {
      String str = paramVarArgs[i];
      if (Tool.isOption(str, "-trace"))
      {
        this.trace = true;
      }
      else if (Tool.isOption(str, "-pgPort"))
      {
        this.port = Integer.decode(paramVarArgs[(++i)]).intValue();
        this.portIsSet = true;
      }
      else if (Tool.isOption(str, "-baseDir"))
      {
        this.baseDir = paramVarArgs[(++i)];
      }
      else if (Tool.isOption(str, "-pgAllowOthers"))
      {
        this.allowOthers = true;
      }
      else if (Tool.isOption(str, "-pgDaemon"))
      {
        this.isDaemon = true;
      }
      else if (Tool.isOption(str, "-ifExists"))
      {
        this.ifExists = true;
      }
      else if (Tool.isOption(str, "-key"))
      {
        this.key = paramVarArgs[(++i)];
        this.keyDatabase = paramVarArgs[(++i)];
      }
    }
    Driver.load();
  }
  
  boolean getTrace()
  {
    return this.trace;
  }
  
  void trace(String paramString)
  {
    if (this.trace) {
      System.out.println(paramString);
    }
  }
  
  synchronized void remove(PgServerThread paramPgServerThread)
  {
    this.running.remove(paramPgServerThread);
  }
  
  void traceError(Exception paramException)
  {
    if (this.trace) {
      paramException.printStackTrace();
    }
  }
  
  public String getURL()
  {
    return "pg://" + NetUtils.getLocalAddress() + ":" + this.port;
  }
  
  public int getPort()
  {
    return this.port;
  }
  
  private boolean allow(Socket paramSocket)
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
  
  public void start()
  {
    this.stop = false;
    try
    {
      this.serverSocket = NetUtils.createServerSocket(this.port, false);
    }
    catch (DbException localDbException)
    {
      if (!this.portIsSet) {
        this.serverSocket = NetUtils.createServerSocket(0, false);
      } else {
        throw localDbException;
      }
    }
    this.port = this.serverSocket.getLocalPort();
  }
  
  public void listen()
  {
    String str = Thread.currentThread().getName();
    try
    {
      while (!this.stop)
      {
        Socket localSocket = this.serverSocket.accept();
        if (!allow(localSocket))
        {
          trace("Connection not allowed");
          localSocket.close();
        }
        else
        {
          PgServerThread localPgServerThread = new PgServerThread(localSocket, this);
          this.running.add(localPgServerThread);
          localPgServerThread.setProcessId(this.pid.incrementAndGet());
          Thread localThread = new Thread(localPgServerThread, str + " thread");
          localThread.setDaemon(this.isDaemon);
          localPgServerThread.setThread(localThread);
          localThread.start();
        }
      }
    }
    catch (Exception localException)
    {
      if (!this.stop) {
        localException.printStackTrace();
      }
    }
  }
  
  public void stop()
  {
    if (!this.stop)
    {
      this.stop = true;
      if (this.serverSocket != null)
      {
        try
        {
          this.serverSocket.close();
        }
        catch (IOException localIOException)
        {
          localIOException.printStackTrace();
        }
        this.serverSocket = null;
      }
    }
    for (PgServerThread localPgServerThread : New.arrayList(this.running))
    {
      localPgServerThread.close();
      try
      {
        Thread localThread = localPgServerThread.getThread();
        if (localThread != null) {
          localThread.join(100L);
        }
      }
      catch (Exception localException)
      {
        localException.printStackTrace();
      }
    }
  }
  
  public boolean isRunning(boolean paramBoolean)
  {
    if (this.serverSocket == null) {
      return false;
    }
    try
    {
      Socket localSocket = NetUtils.createLoopbackSocket(this.serverSocket.getLocalPort(), false);
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
  
  PgServerThread getThread(int paramInt)
  {
    for (PgServerThread localPgServerThread : New.arrayList(this.running)) {
      if (localPgServerThread.getProcessId() == paramInt) {
        return localPgServerThread;
      }
    }
    return null;
  }
  
  String getBaseDir()
  {
    return this.baseDir;
  }
  
  public boolean getAllowOthers()
  {
    return this.allowOthers;
  }
  
  public String getType()
  {
    return "PG";
  }
  
  public String getName()
  {
    return "H2 PG Server";
  }
  
  boolean getIfExists()
  {
    return this.ifExists;
  }
  
  public static String getIndexColumn(Connection paramConnection, int paramInt, Integer paramInteger, Boolean paramBoolean)
    throws SQLException
  {
    if ((paramInteger == null) || (paramInteger.intValue() == 0))
    {
      localPreparedStatement = paramConnection.prepareStatement("select sql from information_schema.indexes where id=?");
      
      localPreparedStatement.setInt(1, paramInt);
      localResultSet = localPreparedStatement.executeQuery();
      if (localResultSet.next()) {
        return localResultSet.getString(1);
      }
      return "";
    }
    PreparedStatement localPreparedStatement = paramConnection.prepareStatement("select column_name from information_schema.indexes where id=? and ordinal_position=?");
    
    localPreparedStatement.setInt(1, paramInt);
    localPreparedStatement.setInt(2, paramInteger.intValue());
    ResultSet localResultSet = localPreparedStatement.executeQuery();
    if (localResultSet.next()) {
      return localResultSet.getString(1);
    }
    return "";
  }
  
  public static String getCurrentSchema(Connection paramConnection)
    throws SQLException
  {
    ResultSet localResultSet = paramConnection.createStatement().executeQuery("call schema()");
    localResultSet.next();
    return localResultSet.getString(1);
  }
  
  public static int getOid(Connection paramConnection, String paramString)
    throws SQLException
  {
    if ((paramString.startsWith("\"")) && (paramString.endsWith("\""))) {
      paramString = paramString.substring(1, paramString.length() - 1);
    }
    PreparedStatement localPreparedStatement = paramConnection.prepareStatement("select oid from pg_class where relName = ?");
    
    localPreparedStatement.setString(1, paramString);
    ResultSet localResultSet = localPreparedStatement.executeQuery();
    if (!localResultSet.next()) {
      return 0;
    }
    return localResultSet.getInt(1);
  }
  
  public static String getEncodingName(int paramInt)
  {
    switch (paramInt)
    {
    case 0: 
      return "SQL_ASCII";
    case 6: 
      return "UTF8";
    case 8: 
      return "LATIN1";
    }
    return paramInt < 40 ? "UTF8" : "";
  }
  
  public static String getVersion()
  {
    return "PostgreSQL 8.1.4  server protocol using H2 " + Constants.getFullVersion();
  }
  
  public static Timestamp getStartTime()
  {
    return new Timestamp(System.currentTimeMillis());
  }
  
  public static String getUserById(Connection paramConnection, int paramInt)
    throws SQLException
  {
    PreparedStatement localPreparedStatement = paramConnection.prepareStatement("SELECT NAME FROM INFORMATION_SCHEMA.USERS WHERE ID=?");
    
    localPreparedStatement.setInt(1, paramInt);
    ResultSet localResultSet = localPreparedStatement.executeQuery();
    if (localResultSet.next()) {
      return localResultSet.getString(1);
    }
    return null;
  }
  
  public static boolean hasDatabasePrivilege(int paramInt, String paramString)
  {
    return true;
  }
  
  public static boolean hasTablePrivilege(String paramString1, String paramString2)
  {
    return true;
  }
  
  public static int getCurrentTid(String paramString1, String paramString2)
  {
    return 1;
  }
  
  public static String getPgExpr(String paramString, int paramInt)
  {
    return null;
  }
  
  public static String formatType(Connection paramConnection, int paramInt1, int paramInt2)
    throws SQLException
  {
    PreparedStatement localPreparedStatement = paramConnection.prepareStatement("select typname from pg_catalog.pg_type where oid = ? and typtypmod = ?");
    
    localPreparedStatement.setInt(1, paramInt1);
    localPreparedStatement.setInt(2, paramInt2);
    ResultSet localResultSet = localPreparedStatement.executeQuery();
    if (localResultSet.next()) {
      return localResultSet.getString(1);
    }
    return null;
  }
  
  public static int convertType(int paramInt)
  {
    switch (paramInt)
    {
    case 16: 
      return 16;
    case 12: 
      return 1043;
    case 2005: 
      return 25;
    case 1: 
      return 1042;
    case 5: 
      return 21;
    case 4: 
      return 23;
    case -5: 
      return 20;
    case 3: 
      return 1700;
    case 7: 
      return 700;
    case 8: 
      return 701;
    case 92: 
      return 1083;
    case 91: 
      return 1082;
    case 93: 
      return 1114;
    case -3: 
      return 17;
    case 2004: 
      return 26;
    case 2003: 
      return 1009;
    }
    return 705;
  }
  
  HashSet<Integer> getTypeSet()
  {
    return this.typeSet;
  }
  
  void checkType(int paramInt)
  {
    if (!this.typeSet.contains(Integer.valueOf(paramInt))) {
      trace("Unsupported type: " + paramInt);
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
