package org.h2.server.web;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import org.h2.bnf.Bnf;
import org.h2.bnf.context.DbContents;
import org.h2.bnf.context.DbContextRule;
import org.h2.message.DbException;
import org.h2.util.New;

class WebSession
{
  private static final int MAX_HISTORY = 1000;
  long lastAccess;
  final HashMap<String, Object> map = New.hashMap();
  Locale locale;
  Statement executingStatement;
  ResultSet result;
  private final WebServer server;
  private final ArrayList<String> commandHistory;
  private Connection conn;
  private DatabaseMetaData meta;
  private DbContents contents = new DbContents();
  private Bnf bnf;
  private boolean shutdownServerOnDisconnect;
  
  WebSession(WebServer paramWebServer)
  {
    this.server = paramWebServer;
    
    this.commandHistory = paramWebServer.getCommandHistoryList();
  }
  
  void put(String paramString, Object paramObject)
  {
    this.map.put(paramString, paramObject);
  }
  
  Object get(String paramString)
  {
    if ("sessions".equals(paramString)) {
      return this.server.getSessions();
    }
    return this.map.get(paramString);
  }
  
  void remove(String paramString)
  {
    this.map.remove(paramString);
  }
  
  Bnf getBnf()
  {
    return this.bnf;
  }
  
  void loadBnf()
  {
    try
    {
      Bnf localBnf = Bnf.getInstance(null);
      DbContextRule localDbContextRule1 = new DbContextRule(this.contents, 0);
      
      DbContextRule localDbContextRule2 = new DbContextRule(this.contents, 3);
      
      DbContextRule localDbContextRule3 = new DbContextRule(this.contents, 2);
      
      DbContextRule localDbContextRule4 = new DbContextRule(this.contents, 1);
      
      DbContextRule localDbContextRule5 = new DbContextRule(this.contents, 5);
      
      DbContextRule localDbContextRule6 = new DbContextRule(this.contents, 4);
      
      localBnf.updateTopic("column_name", localDbContextRule1);
      localBnf.updateTopic("new_table_alias", localDbContextRule2);
      localBnf.updateTopic("table_alias", localDbContextRule3);
      localBnf.updateTopic("column_alias", localDbContextRule6);
      localBnf.updateTopic("table_name", localDbContextRule4);
      localBnf.updateTopic("schema_name", localDbContextRule5);
      localBnf.linkStatements();
      this.bnf = localBnf;
    }
    catch (Exception localException)
    {
      this.server.traceError(localException);
    }
  }
  
  String getCommand(int paramInt)
  {
    return (String)this.commandHistory.get(paramInt);
  }
  
  void addCommand(String paramString)
  {
    if (paramString == null) {
      return;
    }
    paramString = paramString.trim();
    if (paramString.length() == 0) {
      return;
    }
    if (this.commandHistory.size() > 1000) {
      this.commandHistory.remove(0);
    }
    int i = this.commandHistory.indexOf(paramString);
    if (i >= 0) {
      this.commandHistory.remove(i);
    }
    this.commandHistory.add(paramString);
    if (this.server.isCommandHistoryAllowed()) {
      this.server.saveCommandHistoryList(this.commandHistory);
    }
  }
  
  ArrayList<String> getCommandHistory()
  {
    return this.commandHistory;
  }
  
  HashMap<String, Object> getInfo()
  {
    HashMap localHashMap = New.hashMap();
    localHashMap.putAll(this.map);
    localHashMap.put("lastAccess", new Timestamp(this.lastAccess).toString());
    try
    {
      localHashMap.put("url", this.conn == null ? "${text.admin.notConnected}" : this.conn.getMetaData().getURL());
      
      localHashMap.put("user", this.conn == null ? "-" : this.conn.getMetaData().getUserName());
      
      localHashMap.put("lastQuery", this.commandHistory.size() == 0 ? "" : (String)this.commandHistory.get(0));
      
      localHashMap.put("executing", this.executingStatement == null ? "${text.admin.no}" : "${text.admin.yes}");
    }
    catch (SQLException localSQLException)
    {
      DbException.traceThrowable(localSQLException);
    }
    return localHashMap;
  }
  
  void setConnection(Connection paramConnection)
    throws SQLException
  {
    this.conn = paramConnection;
    if (paramConnection == null) {
      this.meta = null;
    } else {
      this.meta = paramConnection.getMetaData();
    }
    this.contents = new DbContents();
  }
  
  DatabaseMetaData getMetaData()
  {
    return this.meta;
  }
  
  Connection getConnection()
  {
    return this.conn;
  }
  
  DbContents getContents()
  {
    return this.contents;
  }
  
  void setShutdownServerOnDisconnect()
  {
    this.shutdownServerOnDisconnect = true;
  }
  
  boolean getShutdownServerOnDisconnect()
  {
    return this.shutdownServerOnDisconnect;
  }
  
  void close()
  {
    if (this.executingStatement != null) {
      try
      {
        this.executingStatement.cancel();
      }
      catch (Exception localException1) {}
    }
    if (this.conn != null) {
      try
      {
        this.conn.close();
      }
      catch (Exception localException2) {}
    }
  }
}
