package org.h2.server.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import org.h2.Driver;
import org.h2.engine.Constants;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.server.Service;
import org.h2.server.ShutdownHandler;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.JdbcUtils;
import org.h2.util.MathUtils;
import org.h2.util.NetUtils;
import org.h2.util.New;
import org.h2.util.SortedProperties;
import org.h2.util.StringUtils;
import org.h2.util.Tool;
import org.h2.util.Utils;

public class WebServer
  implements Service
{
  static final String TRANSFER = "transfer";
  static final String[][] LANGUAGES = { { "cs", "Čeština" }, { "de", "Deutsch" }, { "en", "English" }, { "es", "Español" }, { "fr", "Français" }, { "hu", "Magyar" }, { "ko", "한국어" }, { "in", "Indonesia" }, { "it", "Italiano" }, { "ja", "日本語" }, { "nl", "Nederlands" }, { "pl", "Polski" }, { "pt_BR", "Português (Brasil)" }, { "pt_PT", "Português (Europeu)" }, { "ru", "русский" }, { "sk", "Slovensky" }, { "tr", "Türkçe" }, { "uk", "Українська" }, { "zh_CN", "中文 (简体)" }, { "zh_TW", "中文 (繁體)" } };
  private static final String COMMAND_HISTORY = "commandHistory";
  private static final String DEFAULT_LANGUAGE = "en";
  private static final String[] GENERIC = { "Generic JNDI Data Source|javax.naming.InitialContext|java:comp/env/jdbc/Test|sa", "Generic Firebird Server|org.firebirdsql.jdbc.FBDriver|jdbc:firebirdsql:localhost:c:/temp/firebird/test|sysdba", "Generic SQLite|org.sqlite.JDBC|jdbc:sqlite:test|sa", "Generic DB2|COM.ibm.db2.jdbc.net.DB2Driver|jdbc:db2://localhost/test|", "Generic Oracle|oracle.jdbc.driver.OracleDriver|jdbc:oracle:thin:@localhost:1521:XE|sa", "Generic MS SQL Server 2000|com.microsoft.jdbc.sqlserver.SQLServerDriver|jdbc:microsoft:sqlserver://localhost:1433;DatabaseName=sqlexpress|sa", "Generic MS SQL Server 2005|com.microsoft.sqlserver.jdbc.SQLServerDriver|jdbc:sqlserver://localhost;DatabaseName=test|sa", "Generic PostgreSQL|org.postgresql.Driver|jdbc:postgresql:test|", "Generic MySQL|com.mysql.jdbc.Driver|jdbc:mysql://localhost:3306/test|", "Generic HSQLDB|org.hsqldb.jdbcDriver|jdbc:hsqldb:test;hsqldb.default_table_type=cached|sa", "Generic Derby (Server)|org.apache.derby.jdbc.ClientDriver|jdbc:derby://localhost:1527/test;create=true|sa", "Generic Derby (Embedded)|org.apache.derby.jdbc.EmbeddedDriver|jdbc:derby:test;create=true|sa", "Generic H2 (Server)|org.h2.Driver|jdbc:h2:tcp://localhost/~/test|sa", "Generic H2 (Embedded)|org.h2.Driver|jdbc:h2:~/test|sa" };
  private static int ticker;
  private static final long SESSION_TIMEOUT = SysProperties.CONSOLE_TIMEOUT;
  private int port;
  private boolean allowOthers;
  private boolean isDaemon;
  private final Set<WebThread> running;
  private boolean ssl;
  private final HashMap<String, ConnectionInfo> connInfoMap;
  private long lastTimeoutCheck;
  private final HashMap<String, WebSession> sessions;
  private final HashSet<String> languages;
  private String startDateTime;
  private ServerSocket serverSocket;
  private String url;
  private ShutdownHandler shutdownHandler;
  private Thread listenerThread;
  private boolean ifExists;
  private boolean trace;
  private TranslateThread translateThread;
  private boolean allowChunked;
  private String serverPropertiesDir;
  private String commandHistoryString;
  
  public WebServer()
  {
    this.running = Collections.synchronizedSet(new HashSet());
    
    this.connInfoMap = New.hashMap();
    
    this.sessions = New.hashMap();
    this.languages = New.hashSet();
    
    this.allowChunked = true;
    this.serverPropertiesDir = "~";
  }
  
  byte[] getFile(String paramString)
    throws IOException
  {
    trace("getFile <" + paramString + ">");
    if ((paramString.startsWith("transfer/")) && (new File("transfer").exists()))
    {
      paramString = paramString.substring("transfer".length() + 1);
      if (!isSimpleName(paramString)) {
        return null;
      }
      localObject = new File("transfer", paramString);
      if (!((File)localObject).exists()) {
        return null;
      }
      return IOUtils.readBytesAndClose(new FileInputStream((File)localObject), -1);
    }
    Object localObject = Utils.getResource("/org/h2/server/web/res/" + paramString);
    if (localObject == null) {
      trace(" null");
    } else {
      trace(" size=" + localObject.length);
    }
    return (byte[])localObject;
  }
  
  static boolean isSimpleName(String paramString)
  {
    for (char c : paramString.toCharArray()) {
      if ((c != '.') && (c != '_') && (c != '-') && (!Character.isLetterOrDigit(c))) {
        return false;
      }
    }
    return true;
  }
  
  synchronized void remove(WebThread paramWebThread)
  {
    this.running.remove(paramWebThread);
  }
  
  private static String generateSessionId()
  {
    byte[] arrayOfByte = MathUtils.secureRandomBytes(16);
    return StringUtils.convertBytesToHex(arrayOfByte);
  }
  
  WebSession getSession(String paramString)
  {
    long l = System.currentTimeMillis();
    if (this.lastTimeoutCheck + SESSION_TIMEOUT < l)
    {
      for (localObject = New.arrayList(this.sessions.keySet()).iterator(); ((Iterator)localObject).hasNext();)
      {
        String str = (String)((Iterator)localObject).next();
        WebSession localWebSession = (WebSession)this.sessions.get(str);
        if (localWebSession.lastAccess + SESSION_TIMEOUT < l)
        {
          trace("timeout for " + str);
          this.sessions.remove(str);
        }
      }
      this.lastTimeoutCheck = l;
    }
    Object localObject = (WebSession)this.sessions.get(paramString);
    if (localObject != null) {
      ((WebSession)localObject).lastAccess = System.currentTimeMillis();
    }
    return (WebSession)localObject;
  }
  
  WebSession createNewSession(String paramString)
  {
    String str;
    do
    {
      str = generateSessionId();
    } while (this.sessions.get(str) != null);
    WebSession localWebSession = new WebSession(this);
    localWebSession.lastAccess = System.currentTimeMillis();
    localWebSession.put("sessionId", str);
    localWebSession.put("ip", paramString);
    localWebSession.put("language", "en");
    localWebSession.put("frame-border", "0");
    localWebSession.put("frameset-border", "4");
    this.sessions.put(str, localWebSession);
    
    readTranslations(localWebSession, "en");
    return getSession(str);
  }
  
  String getStartDateTime()
  {
    if (this.startDateTime == null)
    {
      SimpleDateFormat localSimpleDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", new Locale("en", ""));
      
      localSimpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      this.startDateTime = localSimpleDateFormat.format(Long.valueOf(System.currentTimeMillis()));
    }
    return this.startDateTime;
  }
  
  public void init(String... paramVarArgs)
  {
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++) {
      if ("-properties".equals(paramVarArgs[i])) {
        this.serverPropertiesDir = paramVarArgs[(++i)];
      }
    }
    Properties localProperties = loadProperties();
    this.port = SortedProperties.getIntProperty(localProperties, "webPort", 8082);
    
    this.ssl = SortedProperties.getBooleanProperty(localProperties, "webSSL", false);
    
    this.allowOthers = SortedProperties.getBooleanProperty(localProperties, "webAllowOthers", false);
    
    this.commandHistoryString = localProperties.getProperty("commandHistory");
    for (int j = 0; (paramVarArgs != null) && (j < paramVarArgs.length); j++)
    {
      String str1 = paramVarArgs[j];
      if (Tool.isOption(str1, "-webPort"))
      {
        this.port = Integer.decode(paramVarArgs[(++j)]).intValue();
      }
      else if (Tool.isOption(str1, "-webSSL"))
      {
        this.ssl = true;
      }
      else if (Tool.isOption(str1, "-webAllowOthers"))
      {
        this.allowOthers = true;
      }
      else if (Tool.isOption(str1, "-webDaemon"))
      {
        this.isDaemon = true;
      }
      else if (Tool.isOption(str1, "-baseDir"))
      {
        String str2 = paramVarArgs[(++j)];
        SysProperties.setBaseDir(str2);
      }
      else if (Tool.isOption(str1, "-ifExists"))
      {
        this.ifExists = true;
      }
      else if (Tool.isOption(str1, "-properties"))
      {
        j++;
      }
      else if (Tool.isOption(str1, "-trace"))
      {
        this.trace = true;
      }
    }
    for (String[] arrayOfString1 : LANGUAGES) {
      this.languages.add(arrayOfString1[0]);
    }
    updateURL();
  }
  
  public String getURL()
  {
    updateURL();
    return this.url;
  }
  
  private void updateURL()
  {
    try
    {
      this.url = ((this.ssl ? "https" : "http") + "://" + NetUtils.getLocalAddress() + ":" + this.port);
    }
    catch (NoClassDefFoundError localNoClassDefFoundError) {}
  }
  
  public void start()
  {
    this.serverSocket = NetUtils.createServerSocket(this.port, this.ssl);
    this.port = this.serverSocket.getLocalPort();
    updateURL();
  }
  
  public void listen()
  {
    this.listenerThread = Thread.currentThread();
    try
    {
      while (this.serverSocket != null)
      {
        Socket localSocket = this.serverSocket.accept();
        WebThread localWebThread = new WebThread(localSocket, this);
        this.running.add(localWebThread);
        localWebThread.start();
      }
    }
    catch (Exception localException)
    {
      trace(localException.toString());
    }
  }
  
  public boolean isRunning(boolean paramBoolean)
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
  
  public boolean isStopped()
  {
    return this.serverSocket == null;
  }
  
  public void stop()
  {
    if (this.serverSocket != null)
    {
      try
      {
        this.serverSocket.close();
      }
      catch (IOException localIOException)
      {
        traceError(localIOException);
      }
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
    for (Iterator localIterator = New.arrayList(this.sessions.values()).iterator(); localIterator.hasNext();)
    {
      localObject = (WebSession)localIterator.next();
      ((WebSession)localObject).close();
    }
    Object localObject;
    for (localIterator = New.arrayList(this.running).iterator(); localIterator.hasNext();)
    {
      localObject = (WebThread)localIterator.next();
      try
      {
        ((WebThread)localObject).stopNow();
        ((WebThread)localObject).join(100);
      }
      catch (Exception localException)
      {
        traceError(localException);
      }
    }
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
  
  boolean supportsLanguage(String paramString)
  {
    return this.languages.contains(paramString);
  }
  
  void readTranslations(WebSession paramWebSession, String paramString)
  {
    Object localObject = new Properties();
    try
    {
      trace("translation: " + paramString);
      byte[] arrayOfByte = getFile("_text_" + paramString + ".prop");
      trace("  " + new String(arrayOfByte));
      localObject = SortedProperties.fromLines(new String(arrayOfByte, Constants.UTF8));
      for (Map.Entry localEntry : ((Properties)localObject).entrySet())
      {
        String str = (String)localEntry.getValue();
        if (str.startsWith("#")) {
          localEntry.setValue(str.substring(1));
        }
      }
    }
    catch (IOException localIOException)
    {
      DbException.traceThrowable(localIOException);
    }
    paramWebSession.put("text", new HashMap((Map)localObject));
  }
  
  ArrayList<HashMap<String, Object>> getSessions()
  {
    ArrayList localArrayList = New.arrayList();
    for (WebSession localWebSession : this.sessions.values()) {
      localArrayList.add(localWebSession.getInfo());
    }
    return localArrayList;
  }
  
  public String getType()
  {
    return "Web Console";
  }
  
  public String getName()
  {
    return "H2 Console Server";
  }
  
  void setAllowOthers(boolean paramBoolean)
  {
    this.allowOthers = paramBoolean;
  }
  
  public boolean getAllowOthers()
  {
    return this.allowOthers;
  }
  
  void setSSL(boolean paramBoolean)
  {
    this.ssl = paramBoolean;
  }
  
  void setPort(int paramInt)
  {
    this.port = paramInt;
  }
  
  boolean getSSL()
  {
    return this.ssl;
  }
  
  public int getPort()
  {
    return this.port;
  }
  
  public boolean isCommandHistoryAllowed()
  {
    return this.commandHistoryString != null;
  }
  
  public void setCommandHistoryAllowed(boolean paramBoolean)
  {
    if (paramBoolean)
    {
      if (this.commandHistoryString == null) {
        this.commandHistoryString = "";
      }
    }
    else {
      this.commandHistoryString = null;
    }
  }
  
  public ArrayList<String> getCommandHistoryList()
  {
    ArrayList localArrayList = New.arrayList();
    if (this.commandHistoryString == null) {
      return localArrayList;
    }
    StringBuilder localStringBuilder = new StringBuilder();
    for (int i = 0;; i++) {
      if ((i == this.commandHistoryString.length()) || (this.commandHistoryString.charAt(i) == ';'))
      {
        if (localStringBuilder.length() > 0)
        {
          localArrayList.add(localStringBuilder.toString());
          localStringBuilder.delete(0, localStringBuilder.length());
        }
        if (i == this.commandHistoryString.length()) {
          break;
        }
      }
      else if ((this.commandHistoryString.charAt(i) == '\\') && (i < this.commandHistoryString.length() - 1))
      {
        localStringBuilder.append(this.commandHistoryString.charAt(++i));
      }
      else
      {
        localStringBuilder.append(this.commandHistoryString.charAt(i));
      }
    }
    return localArrayList;
  }
  
  public void saveCommandHistoryList(ArrayList<String> paramArrayList)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    for (String str : paramArrayList)
    {
      if (localStringBuilder.length() > 0) {
        localStringBuilder.append(';');
      }
      localStringBuilder.append(str.replace("\\", "\\\\").replace(";", "\\;"));
    }
    this.commandHistoryString = localStringBuilder.toString();
    saveProperties(null);
  }
  
  ConnectionInfo getSetting(String paramString)
  {
    return (ConnectionInfo)this.connInfoMap.get(paramString);
  }
  
  void updateSetting(ConnectionInfo paramConnectionInfo)
  {
    this.connInfoMap.put(paramConnectionInfo.name, paramConnectionInfo);
    paramConnectionInfo.lastAccess = (ticker++);
  }
  
  void removeSetting(String paramString)
  {
    this.connInfoMap.remove(paramString);
  }
  
  private Properties loadProperties()
  {
    try
    {
      if ("null".equals(this.serverPropertiesDir)) {
        return new Properties();
      }
      return SortedProperties.loadProperties(this.serverPropertiesDir + "/" + ".h2.server.properties");
    }
    catch (Exception localException)
    {
      DbException.traceThrowable(localException);
    }
    return new Properties();
  }
  
  String[] getSettingNames()
  {
    ArrayList localArrayList = getSettings();
    String[] arrayOfString = new String[localArrayList.size()];
    for (int i = 0; i < localArrayList.size(); i++) {
      arrayOfString[i] = ((ConnectionInfo)localArrayList.get(i)).name;
    }
    return arrayOfString;
  }
  
  synchronized ArrayList<ConnectionInfo> getSettings()
  {
    ArrayList localArrayList = New.arrayList();
    if (this.connInfoMap.size() == 0)
    {
      Properties localProperties = loadProperties();
      if (localProperties.size() == 0) {
        for (String str2 : GENERIC)
        {
          ConnectionInfo localConnectionInfo2 = new ConnectionInfo(str2);
          localArrayList.add(localConnectionInfo2);
          updateSetting(localConnectionInfo2);
        }
      } else {
        for (int i = 0;; i++)
        {
          String str1 = localProperties.getProperty(String.valueOf(i));
          if (str1 == null) {
            break;
          }
          ConnectionInfo localConnectionInfo1 = new ConnectionInfo(str1);
          localArrayList.add(localConnectionInfo1);
          updateSetting(localConnectionInfo1);
        }
      }
    }
    else
    {
      localArrayList.addAll(this.connInfoMap.values());
    }
    Collections.sort(localArrayList);
    return localArrayList;
  }
  
  synchronized void saveProperties(Properties paramProperties)
  {
    try
    {
      if (paramProperties == null)
      {
        localObject = loadProperties();
        paramProperties = new SortedProperties();
        paramProperties.setProperty("webPort", "" + SortedProperties.getIntProperty((Properties)localObject, "webPort", this.port));
        
        paramProperties.setProperty("webAllowOthers", "" + SortedProperties.getBooleanProperty((Properties)localObject, "webAllowOthers", this.allowOthers));
        
        paramProperties.setProperty("webSSL", "" + SortedProperties.getBooleanProperty((Properties)localObject, "webSSL", this.ssl));
        if (this.commandHistoryString != null) {
          paramProperties.setProperty("commandHistory", this.commandHistoryString);
        }
      }
      Object localObject = getSettings();
      int i = ((ArrayList)localObject).size();
      for (int j = 0; j < i; j++)
      {
        ConnectionInfo localConnectionInfo = (ConnectionInfo)((ArrayList)localObject).get(j);
        if (localConnectionInfo != null) {
          paramProperties.setProperty(String.valueOf(i - j - 1), localConnectionInfo.getString());
        }
      }
      if (!"null".equals(this.serverPropertiesDir))
      {
        OutputStream localOutputStream = FileUtils.newOutputStream(this.serverPropertiesDir + "/" + ".h2.server.properties", false);
        
        paramProperties.store(localOutputStream, "H2 Server Properties");
        localOutputStream.close();
      }
    }
    catch (Exception localException)
    {
      DbException.traceThrowable(localException);
    }
  }
  
  Connection getConnection(String paramString1, String paramString2, String paramString3, String paramString4)
    throws SQLException
  {
    paramString1 = paramString1.trim();
    paramString2 = paramString2.trim();
    Driver.load();
    Properties localProperties = new Properties();
    localProperties.setProperty("user", paramString3.trim());
    
    localProperties.setProperty("password", paramString4);
    if (paramString2.startsWith("jdbc:h2:"))
    {
      if (this.ifExists) {
        paramString2 = paramString2 + ";IFEXISTS=TRUE";
      }
      return Driver.load().connect(paramString2, localProperties);
    }
    return JdbcUtils.getConnection(paramString1, paramString2, localProperties);
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
  
  public String addSession(Connection paramConnection)
    throws SQLException
  {
    WebSession localWebSession = createNewSession("local");
    localWebSession.setShutdownServerOnDisconnect();
    localWebSession.setConnection(paramConnection);
    localWebSession.put("url", paramConnection.getMetaData().getURL());
    String str = (String)localWebSession.get("sessionId");
    return this.url + "/frame.jsp?jsessionid=" + str;
  }
  
  private class TranslateThread
    extends Thread
  {
    private final File file = new File("translation.properties");
    private final Map<Object, Object> translation;
    private volatile boolean stopNow;
    
    TranslateThread()
    {
      Map localMap;
      this.translation = localMap;
    }
    
    public String getFileName()
    {
      return this.file.getAbsolutePath();
    }
    
    public void stopNow()
    {
      this.stopNow = true;
      try
      {
        join();
      }
      catch (InterruptedException localInterruptedException) {}
    }
    
    public void run()
    {
      while (!this.stopNow) {
        try
        {
          SortedProperties localSortedProperties = new SortedProperties();
          Object localObject;
          if (this.file.exists())
          {
            localObject = FileUtils.newInputStream(this.file.getName());
            localSortedProperties.load((InputStream)localObject);
            this.translation.putAll(localSortedProperties);
          }
          else
          {
            localObject = FileUtils.newOutputStream(this.file.getName(), false);
            localSortedProperties.putAll(this.translation);
            localSortedProperties.store((OutputStream)localObject, "Translation");
          }
          Thread.sleep(1000L);
        }
        catch (Exception localException)
        {
          WebServer.this.traceError(localException);
        }
      }
    }
  }
  
  String startTranslate(Map<Object, Object> paramMap)
  {
    if (this.translateThread != null) {
      this.translateThread.stopNow();
    }
    this.translateThread = new TranslateThread(paramMap);
    this.translateThread.setDaemon(true);
    this.translateThread.start();
    return this.translateThread.getFileName();
  }
  
  public boolean isDaemon()
  {
    return this.isDaemon;
  }
  
  void setAllowChunked(boolean paramBoolean)
  {
    this.allowChunked = paramBoolean;
  }
  
  boolean getAllowChunked()
  {
    return this.allowChunked;
  }
}
