package org.h2.server.web;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import org.h2.bnf.Bnf;
import org.h2.bnf.context.DbColumn;
import org.h2.bnf.context.DbContents;
import org.h2.bnf.context.DbSchema;
import org.h2.bnf.context.DbTableOrView;
import org.h2.engine.Constants;
import org.h2.engine.SysProperties;
import org.h2.jdbc.JdbcSQLException;
import org.h2.message.DbException;
import org.h2.security.SHA256;
import org.h2.tools.Backup;
import org.h2.tools.ChangeFileEncryption;
import org.h2.tools.ConvertTraceFile;
import org.h2.tools.CreateCluster;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Recover;
import org.h2.tools.Restore;
import org.h2.tools.RunScript;
import org.h2.tools.Script;
import org.h2.tools.SimpleResultSet;
import org.h2.util.JdbcUtils;
import org.h2.util.New;
import org.h2.util.Profiler;
import org.h2.util.ScriptReader;
import org.h2.util.SortedProperties;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.util.Tool;
import org.h2.util.Utils;

public class WebApp
{
  protected final WebServer server;
  protected WebSession session;
  protected Properties attributes;
  protected String mimeType;
  protected boolean cache;
  protected boolean stop;
  protected String headerLanguage;
  private Profiler profiler;
  
  WebApp(WebServer paramWebServer)
  {
    this.server = paramWebServer;
  }
  
  void setSession(WebSession paramWebSession, Properties paramProperties)
  {
    this.session = paramWebSession;
    this.attributes = paramProperties;
  }
  
  String processRequest(String paramString1, String paramString2)
  {
    int i = paramString1.lastIndexOf('.');
    String str;
    if (i >= 0) {
      str = paramString1.substring(i + 1);
    } else {
      str = "";
    }
    if ("ico".equals(str))
    {
      this.mimeType = "image/x-icon";
      this.cache = true;
    }
    else if ("gif".equals(str))
    {
      this.mimeType = "image/gif";
      this.cache = true;
    }
    else if ("css".equals(str))
    {
      this.cache = true;
      this.mimeType = "text/css";
    }
    else if (("html".equals(str)) || ("do".equals(str)) || ("jsp".equals(str)))
    {
      this.cache = false;
      this.mimeType = "text/html";
      if ((this.session == null) && (!paramString1.startsWith("transfer")))
      {
        this.session = this.server.createNewSession(paramString2);
        if (!"notAllowed.jsp".equals(paramString1)) {
          paramString1 = "index.do";
        }
      }
    }
    else if ("js".equals(str))
    {
      this.cache = true;
      this.mimeType = "text/javascript";
    }
    else
    {
      this.cache = true;
      this.mimeType = "application/octet-stream";
    }
    trace("mimeType=" + this.mimeType);
    trace(paramString1);
    if (paramString1.endsWith(".do")) {
      paramString1 = process(paramString1);
    }
    return paramString1;
  }
  
  private static String getComboBox(String[] paramArrayOfString, String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    for (String str : paramArrayOfString)
    {
      localStringBuilder.append("<option value=\"").append(PageParser.escapeHtmlData(str)).append('"');
      if (str.equals(paramString)) {
        localStringBuilder.append(" selected");
      }
      localStringBuilder.append('>').append(PageParser.escapeHtml(str)).append("</option>");
    }
    return localStringBuilder.toString();
  }
  
  private static String getComboBox(String[][] paramArrayOfString, String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    for (String[] arrayOfString1 : paramArrayOfString)
    {
      localStringBuilder.append("<option value=\"").append(PageParser.escapeHtmlData(arrayOfString1[0])).append('"');
      if (arrayOfString1[0].equals(paramString)) {
        localStringBuilder.append(" selected");
      }
      localStringBuilder.append('>').append(PageParser.escapeHtml(arrayOfString1[1])).append("</option>");
    }
    return localStringBuilder.toString();
  }
  
  private String process(String paramString)
  {
    trace("process " + paramString);
    while (paramString.endsWith(".do")) {
      if ("login.do".equals(paramString)) {
        paramString = login();
      } else if ("index.do".equals(paramString)) {
        paramString = index();
      } else if ("logout.do".equals(paramString)) {
        paramString = logout();
      } else if ("settingRemove.do".equals(paramString)) {
        paramString = settingRemove();
      } else if ("settingSave.do".equals(paramString)) {
        paramString = settingSave();
      } else if ("test.do".equals(paramString)) {
        paramString = test();
      } else if ("query.do".equals(paramString)) {
        paramString = query();
      } else if ("tables.do".equals(paramString)) {
        paramString = tables();
      } else if ("editResult.do".equals(paramString)) {
        paramString = editResult();
      } else if ("getHistory.do".equals(paramString)) {
        paramString = getHistory();
      } else if ("admin.do".equals(paramString)) {
        paramString = admin();
      } else if ("adminSave.do".equals(paramString)) {
        paramString = adminSave();
      } else if ("adminStartTranslate.do".equals(paramString)) {
        paramString = adminStartTranslate();
      } else if ("adminShutdown.do".equals(paramString)) {
        paramString = adminShutdown();
      } else if ("autoCompleteList.do".equals(paramString)) {
        paramString = autoCompleteList();
      } else if ("tools.do".equals(paramString)) {
        paramString = tools();
      } else if ("transfer.do".equals(paramString)) {
        paramString = "transfer.jsp";
      } else {
        paramString = "error.jsp";
      }
    }
    trace("return " + paramString);
    return paramString;
  }
  
  private String autoCompleteList()
  {
    String str1 = (String)this.attributes.get("query");
    int i = 0;
    if ((str1.trim().length() > 0) && (Character.isLowerCase(str1.trim().charAt(0)))) {
      i = 1;
    }
    try
    {
      Object localObject1 = str1;
      if (((String)localObject1).endsWith(";")) {
        localObject1 = (String)localObject1 + " ";
      }
      ScriptReader localScriptReader = new ScriptReader(new StringReader((String)localObject1));
      localScriptReader.setSkipRemarks(true);
      Object localObject2 = "";
      for (;;)
      {
        str2 = localScriptReader.readStatement();
        if (str2 == null) {
          break;
        }
        localObject2 = str2;
      }
      String str2 = "";
      if (localScriptReader.isInsideRemark())
      {
        if (localScriptReader.isBlockRemark()) {
          str2 = "1#(End Remark)# */\n" + str2;
        } else {
          str2 = "1#(Newline)#\n" + str2;
        }
      }
      else
      {
        localObject1 = localObject2;
        while ((((String)localObject1).length() > 0) && (((String)localObject1).charAt(0) <= ' ')) {
          localObject1 = ((String)localObject1).substring(1);
        }
        if ((((String)localObject1).trim().length() > 0) && (Character.isLowerCase(((String)localObject1).trim().charAt(0)))) {
          i = 1;
        }
        Bnf localBnf = this.session.getBnf();
        if (localBnf == null) {
          return "autoCompleteList.jsp";
        }
        HashMap localHashMap = localBnf.getNextTokenList((String)localObject1);
        String str3 = "";
        if (((String)localObject1).length() > 0)
        {
          char c = ((String)localObject1).charAt(((String)localObject1).length() - 1);
          if ((!Character.isWhitespace(c)) && (c != '.') && (c >= ' ') && (c != '\'') && (c != '"')) {
            str3 = " ";
          }
        }
        ArrayList localArrayList = New.arrayList(localHashMap.size());
        for (Object localObject3 = localHashMap.entrySet().iterator(); ((Iterator)localObject3).hasNext();)
        {
          localObject4 = (Map.Entry)((Iterator)localObject3).next();
          str4 = (String)((Map.Entry)localObject4).getKey();
          String str5 = (String)((Map.Entry)localObject4).getValue();
          String str6 = "" + str4.charAt(0);
          if (Integer.parseInt(str6) <= 2)
          {
            str4 = str4.substring(2);
            if ((Character.isLetter(str4.charAt(0))) && (i != 0))
            {
              str4 = StringUtils.toLowerEnglish(str4);
              str5 = StringUtils.toLowerEnglish(str5);
            }
            if ((str4.equals(str5)) && (!".".equals(str5))) {
              str5 = str3 + str5;
            }
            str4 = StringUtils.urlEncode(str4);
            str4 = StringUtils.replaceAll(str4, "+", " ");
            str5 = StringUtils.urlEncode(str5);
            str5 = StringUtils.replaceAll(str5, "+", " ");
            localArrayList.add(str6 + "#" + str4 + "#" + str5);
          }
        }
        String str4;
        Collections.sort(localArrayList);
        if ((str1.endsWith("\n")) || (str1.trim().endsWith(";"))) {
          localArrayList.add(0, "1#(Newline)#\n");
        }
        localObject3 = new StatementBuilder();
        for (Object localObject4 = localArrayList.iterator(); ((Iterator)localObject4).hasNext();)
        {
          str4 = (String)((Iterator)localObject4).next();
          ((StatementBuilder)localObject3).appendExceptFirst("|");
          ((StatementBuilder)localObject3).append(str4);
        }
        str2 = ((StatementBuilder)localObject3).toString();
      }
      this.session.put("autoCompleteList", str2);
    }
    catch (Throwable localThrowable)
    {
      this.server.traceError(localThrowable);
    }
    return "autoCompleteList.jsp";
  }
  
  private String admin()
  {
    this.session.put("port", "" + this.server.getPort());
    this.session.put("allowOthers", "" + this.server.getAllowOthers());
    this.session.put("ssl", String.valueOf(this.server.getSSL()));
    this.session.put("sessions", this.server.getSessions());
    return "admin.jsp";
  }
  
  private String adminSave()
  {
    try
    {
      SortedProperties localSortedProperties = new SortedProperties();
      int i = Integer.decode((String)this.attributes.get("port")).intValue();
      localSortedProperties.setProperty("webPort", String.valueOf(i));
      this.server.setPort(i);
      boolean bool1 = Boolean.parseBoolean((String)this.attributes.get("allowOthers"));
      
      localSortedProperties.setProperty("webAllowOthers", String.valueOf(bool1));
      this.server.setAllowOthers(bool1);
      boolean bool2 = Boolean.parseBoolean((String)this.attributes.get("ssl"));
      
      localSortedProperties.setProperty("webSSL", String.valueOf(bool2));
      this.server.setSSL(bool2);
      this.server.saveProperties(localSortedProperties);
    }
    catch (Exception localException)
    {
      trace(localException.toString());
    }
    return admin();
  }
  
  private String tools()
  {
    try
    {
      String str1 = (String)this.attributes.get("tool");
      this.session.put("tool", str1);
      String str2 = (String)this.attributes.get("args");
      String[] arrayOfString = StringUtils.arraySplit(str2, ',', false);
      Object localObject = null;
      if ("Backup".equals(str1)) {
        localObject = new Backup();
      } else if ("Restore".equals(str1)) {
        localObject = new Restore();
      } else if ("Recover".equals(str1)) {
        localObject = new Recover();
      } else if ("DeleteDbFiles".equals(str1)) {
        localObject = new DeleteDbFiles();
      } else if ("ChangeFileEncryption".equals(str1)) {
        localObject = new ChangeFileEncryption();
      } else if ("Script".equals(str1)) {
        localObject = new Script();
      } else if ("RunScript".equals(str1)) {
        localObject = new RunScript();
      } else if ("ConvertTraceFile".equals(str1)) {
        localObject = new ConvertTraceFile();
      } else if ("CreateCluster".equals(str1)) {
        localObject = new CreateCluster();
      } else {
        throw DbException.throwInternalError(str1);
      }
      ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
      PrintStream localPrintStream = new PrintStream(localByteArrayOutputStream, false, "UTF-8");
      ((Tool)localObject).setOut(localPrintStream);
      try
      {
        ((Tool)localObject).runTool(arrayOfString);
        localPrintStream.flush();
        String str3 = new String(localByteArrayOutputStream.toByteArray(), Constants.UTF8);
        String str4 = PageParser.escapeHtml(str3);
        this.session.put("toolResult", str4);
      }
      catch (Exception localException2)
      {
        this.session.put("toolResult", getStackTrace(0, localException2, true));
      }
    }
    catch (Exception localException1)
    {
      this.server.traceError(localException1);
    }
    return "tools.jsp";
  }
  
  private String adminStartTranslate()
  {
    Map localMap1 = (Map)Map.class.cast(this.session.map.get("text"));
    
    Map localMap2 = localMap1;
    String str = this.server.startTranslate(localMap2);
    this.session.put("translationFile", str);
    return "helpTranslate.jsp";
  }
  
  protected String adminShutdown()
  {
    this.server.shutdown();
    return "admin.jsp";
  }
  
  private String index()
  {
    String[][] arrayOfString = WebServer.LANGUAGES;
    String str1 = (String)this.attributes.get("language");
    Locale localLocale = this.session.locale;
    if (str1 != null)
    {
      if ((localLocale == null) || (!StringUtils.toLowerEnglish(localLocale.getLanguage()).equals(str1)))
      {
        localLocale = new Locale(str1, "");
        this.server.readTranslations(this.session, localLocale.getLanguage());
        this.session.put("language", str1);
        this.session.locale = localLocale;
      }
    }
    else {
      str1 = (String)this.session.get("language");
    }
    if (str1 == null) {
      str1 = this.headerLanguage;
    }
    this.session.put("languageCombo", getComboBox(arrayOfString, str1));
    String[] arrayOfString1 = this.server.getSettingNames();
    String str2 = this.attributes.getProperty("setting");
    if ((str2 == null) && (arrayOfString1.length > 0)) {
      str2 = arrayOfString1[0];
    }
    String str3 = getComboBox(arrayOfString1, str2);
    this.session.put("settingsList", str3);
    ConnectionInfo localConnectionInfo = this.server.getSetting(str2);
    if (localConnectionInfo == null) {
      localConnectionInfo = new ConnectionInfo();
    }
    this.session.put("setting", PageParser.escapeHtmlData(str2));
    this.session.put("name", PageParser.escapeHtmlData(str2));
    this.session.put("driver", PageParser.escapeHtmlData(localConnectionInfo.driver));
    this.session.put("url", PageParser.escapeHtmlData(localConnectionInfo.url));
    this.session.put("user", PageParser.escapeHtmlData(localConnectionInfo.user));
    return "index.jsp";
  }
  
  private String getHistory()
  {
    int i = Integer.parseInt(this.attributes.getProperty("id"));
    String str = this.session.getCommand(i);
    this.session.put("query", PageParser.escapeHtmlData(str));
    return "query.jsp";
  }
  
  private static int addColumns(boolean paramBoolean1, DbTableOrView paramDbTableOrView, StringBuilder paramStringBuilder1, int paramInt, boolean paramBoolean2, StringBuilder paramStringBuilder2)
  {
    DbColumn[] arrayOfDbColumn = paramDbTableOrView.getColumns();
    for (int i = 0; (arrayOfDbColumn != null) && (i < arrayOfDbColumn.length); i++)
    {
      DbColumn localDbColumn = arrayOfDbColumn[i];
      if (paramStringBuilder2.length() > 0) {
        paramStringBuilder2.append(' ');
      }
      paramStringBuilder2.append(localDbColumn.getName());
      String str1 = escapeIdentifier(localDbColumn.getName());
      String str2 = paramBoolean1 ? ", 1, 1" : ", 2, 2";
      paramStringBuilder1.append("setNode(" + paramInt + str2 + ", 'column', '" + PageParser.escapeJavaScript(localDbColumn.getName()) + "', 'javascript:ins(\\'" + str1 + "\\')');\n");
      
      paramInt++;
      if ((paramBoolean1) && (paramBoolean2))
      {
        paramStringBuilder1.append("setNode(" + paramInt + ", 2, 2, 'type', '" + PageParser.escapeJavaScript(localDbColumn.getDataType()) + "', null);\n");
        
        paramInt++;
      }
    }
    return paramInt;
  }
  
  private static String escapeIdentifier(String paramString)
  {
    return StringUtils.urlEncode(PageParser.escapeJavaScript(paramString)).replace('+', ' ');
  }
  
  private static int addIndexes(boolean paramBoolean, DatabaseMetaData paramDatabaseMetaData, String paramString1, String paramString2, StringBuilder paramStringBuilder, int paramInt)
    throws SQLException
  {
    ResultSet localResultSet;
    try
    {
      localResultSet = paramDatabaseMetaData.getIndexInfo(null, paramString2, paramString1, false, true);
    }
    catch (SQLException localSQLException)
    {
      return paramInt;
    }
    HashMap localHashMap = New.hashMap();
    String str1;
    Object localObject1;
    Object localObject2;
    while (localResultSet.next())
    {
      str1 = localResultSet.getString("INDEX_NAME");
      localObject1 = (IndexInfo)localHashMap.get(str1);
      if (localObject1 == null)
      {
        int i = localResultSet.getInt("TYPE");
        if (i == 1) {
          localObject2 = "";
        } else if (i == 2) {
          localObject2 = " (${text.tree.hashed})";
        } else if (i == 3) {
          localObject2 = "";
        } else {
          localObject2 = null;
        }
        if ((str1 != null) && (localObject2 != null))
        {
          localObject1 = new IndexInfo();
          ((IndexInfo)localObject1).name = str1;
          localObject2 = (localResultSet.getBoolean("NON_UNIQUE") ? "${text.tree.nonUnique}" : "${text.tree.unique}") + (String)localObject2;
          
          ((IndexInfo)localObject1).type = ((String)localObject2);
          ((IndexInfo)localObject1).columns = localResultSet.getString("COLUMN_NAME");
          localHashMap.put(str1, localObject1);
        }
      }
      else
      {
        Object tmp232_230 = localObject1;tmp232_230.columns = (tmp232_230.columns + ", " + localResultSet.getString("COLUMN_NAME"));
      }
    }
    localResultSet.close();
    String str2;
    if (localHashMap.size() > 0)
    {
      str1 = paramBoolean ? ", 1, 1" : ", 2, 1";
      localObject1 = paramBoolean ? ", 2, 1" : ", 3, 1";
      str2 = paramBoolean ? ", 3, 2" : ", 4, 2";
      paramStringBuilder.append("setNode(" + paramInt + str1 + ", 'index_az', '${text.tree.indexes}', null);\n");
      
      paramInt++;
      for (localObject2 = localHashMap.values().iterator(); ((Iterator)localObject2).hasNext();)
      {
        IndexInfo localIndexInfo = (IndexInfo)((Iterator)localObject2).next();
        paramStringBuilder.append("setNode(" + paramInt + (String)localObject1 + ", 'index', '" + PageParser.escapeJavaScript(localIndexInfo.name) + "', null);\n");
        
        paramInt++;
        paramStringBuilder.append("setNode(" + paramInt + str2 + ", 'type', '" + localIndexInfo.type + "', null);\n");
        
        paramInt++;
        paramStringBuilder.append("setNode(" + paramInt + str2 + ", 'type', '" + PageParser.escapeJavaScript(localIndexInfo.columns) + "', null);\n");
        
        paramInt++;
      }
    }
    return paramInt;
  }
  
  private int addTablesAndViews(DbSchema paramDbSchema, boolean paramBoolean, StringBuilder paramStringBuilder, int paramInt)
    throws SQLException
  {
    if (paramDbSchema == null) {
      return paramInt;
    }
    Connection localConnection = this.session.getConnection();
    DatabaseMetaData localDatabaseMetaData = this.session.getMetaData();
    int i = paramBoolean ? 0 : 1;
    int j = (paramBoolean) || (!paramDbSchema.isSystem) ? 1 : 0;
    String str1 = ", " + i + ", " + (j != 0 ? "1" : "2") + ", ";
    String str2 = ", " + (i + 1) + ", 2, ";
    DbTableOrView[] arrayOfDbTableOrView1 = paramDbSchema.getTables();
    if (arrayOfDbTableOrView1 == null) {
      return paramInt;
    }
    boolean bool1 = paramDbSchema.getContents().isOracle();
    boolean bool2 = arrayOfDbTableOrView1.length < SysProperties.CONSOLE_MAX_TABLES_LIST_INDEXES;
    DbTableOrView localDbTableOrView;
    int n;
    String str3;
    StringBuilder localStringBuilder;
    for (localDbTableOrView : arrayOfDbTableOrView1) {
      if (!localDbTableOrView.isView())
      {
        n = paramInt;
        str3 = localDbTableOrView.getQuotedName();
        if (!paramBoolean) {
          str3 = paramDbSchema.quotedName + "." + str3;
        }
        str3 = escapeIdentifier(str3);
        paramStringBuilder.append("setNode(" + paramInt + str1 + " 'table', '" + PageParser.escapeJavaScript(localDbTableOrView.getName()) + "', 'javascript:ins(\\'" + str3 + "\\',true)');\n");
        
        paramInt++;
        if ((paramBoolean) || (j != 0))
        {
          localStringBuilder = new StringBuilder();
          paramInt = addColumns(paramBoolean, localDbTableOrView, paramStringBuilder, paramInt, bool2, localStringBuilder);
          if ((!bool1) && (bool2)) {
            paramInt = addIndexes(paramBoolean, localDatabaseMetaData, localDbTableOrView.getName(), paramDbSchema.name, paramStringBuilder, paramInt);
          }
          paramStringBuilder.append("addTable('" + PageParser.escapeJavaScript(localDbTableOrView.getName()) + "', '" + PageParser.escapeJavaScript(localStringBuilder.toString()) + "', " + n + ");\n");
        }
      }
    }
    arrayOfDbTableOrView1 = paramDbSchema.getTables();
    for (localDbTableOrView : arrayOfDbTableOrView1) {
      if (localDbTableOrView.isView())
      {
        n = paramInt;
        str3 = localDbTableOrView.getQuotedName();
        if (!paramBoolean) {
          str3 = localDbTableOrView.getSchema().quotedName + "." + str3;
        }
        str3 = escapeIdentifier(str3);
        paramStringBuilder.append("setNode(" + paramInt + str1 + " 'view', '" + PageParser.escapeJavaScript(localDbTableOrView.getName()) + "', 'javascript:ins(\\'" + str3 + "\\',true)');\n");
        
        paramInt++;
        if (paramBoolean)
        {
          localStringBuilder = new StringBuilder();
          paramInt = addColumns(paramBoolean, localDbTableOrView, paramStringBuilder, paramInt, bool2, localStringBuilder);
          if (paramDbSchema.getContents().isH2())
          {
            PreparedStatement localPreparedStatement = null;
            try
            {
              localPreparedStatement = localConnection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME=?");
              
              localPreparedStatement.setString(1, localDbTableOrView.getName());
              ResultSet localResultSet = localPreparedStatement.executeQuery();
              if (localResultSet.next())
              {
                String str4 = localResultSet.getString("SQL");
                paramStringBuilder.append("setNode(" + paramInt + str2 + " 'type', '" + PageParser.escapeJavaScript(str4) + "', null);\n");
                
                paramInt++;
              }
              localResultSet.close();
            }
            finally
            {
              JdbcUtils.closeSilently(localPreparedStatement);
            }
          }
          paramStringBuilder.append("addTable('" + PageParser.escapeJavaScript(localDbTableOrView.getName()) + "', '" + PageParser.escapeJavaScript(localStringBuilder.toString()) + "', " + n + ");\n");
        }
      }
    }
    return paramInt;
  }
  
  private String tables()
  {
    DbContents localDbContents = this.session.getContents();
    boolean bool = false;
    try
    {
      String str1 = (String)this.session.get("url");
      Connection localConnection = this.session.getConnection();
      localDbContents.readContents(str1, localConnection);
      this.session.loadBnf();
      bool = localDbContents.isH2();
      
      StringBuilder localStringBuilder = new StringBuilder();
      localStringBuilder.append("setNode(0, 0, 0, 'database', '" + PageParser.escapeJavaScript(str1) + "', null);\n");
      
      int i = 1;
      
      DbSchema localDbSchema = localDbContents.getDefaultSchema();
      i = addTablesAndViews(localDbSchema, true, localStringBuilder, i);
      DbSchema[] arrayOfDbSchema = localDbContents.getSchemas();
      Object localObject3;
      for (localObject3 : arrayOfDbSchema) {
        if ((localObject3 != localDbSchema) && (localObject3 != null))
        {
          localStringBuilder.append("setNode(" + i + ", 0, 1, 'folder', '" + PageParser.escapeJavaScript(((DbSchema)localObject3).name) + "', null);\n");
          
          i++;
          i = addTablesAndViews((DbSchema)localObject3, false, localStringBuilder, i);
        }
      }
      if (bool)
      {
        ??? = null;
        try
        {
          ??? = localConnection.createStatement();
          localObject2 = ((Statement)???).executeQuery("SELECT * FROM INFORMATION_SCHEMA.SEQUENCES ORDER BY SEQUENCE_NAME");
          String str2;
          for (??? = 0; ((ResultSet)localObject2).next(); ???++)
          {
            if (??? == 0)
            {
              localStringBuilder.append("setNode(" + i + ", 0, 1, 'sequences', '${text.tree.sequences}', null);\n");
              
              i++;
            }
            localObject3 = ((ResultSet)localObject2).getString("SEQUENCE_NAME");
            str2 = ((ResultSet)localObject2).getString("CURRENT_VALUE");
            String str3 = ((ResultSet)localObject2).getString("INCREMENT");
            localStringBuilder.append("setNode(" + i + ", 1, 1, 'sequence', '" + PageParser.escapeJavaScript((String)localObject3) + "', null);\n");
            
            i++;
            localStringBuilder.append("setNode(" + i + ", 2, 2, 'type', '${text.tree.current}: " + PageParser.escapeJavaScript(str2) + "', null);\n");
            
            i++;
            if (!"1".equals(str3))
            {
              localStringBuilder.append("setNode(" + i + ", 2, 2, 'type', '${text.tree.increment}: " + PageParser.escapeJavaScript(str3) + "', null);\n");
              
              i++;
            }
          }
          ((ResultSet)localObject2).close();
          localObject2 = ((Statement)???).executeQuery("SELECT * FROM INFORMATION_SCHEMA.USERS ORDER BY NAME");
          for (??? = 0; ((ResultSet)localObject2).next(); ???++)
          {
            if (??? == 0)
            {
              localStringBuilder.append("setNode(" + i + ", 0, 1, 'users', '${text.tree.users}', null);\n");
              
              i++;
            }
            localObject3 = ((ResultSet)localObject2).getString("NAME");
            str2 = ((ResultSet)localObject2).getString("ADMIN");
            localStringBuilder.append("setNode(" + i + ", 1, 1, 'user', '" + PageParser.escapeJavaScript((String)localObject3) + "', null);\n");
            
            i++;
            if (str2.equalsIgnoreCase("TRUE"))
            {
              localStringBuilder.append("setNode(" + i + ", 2, 2, 'type', '${text.tree.admin}', null);\n");
              
              i++;
            }
          }
          ((ResultSet)localObject2).close();
        }
        finally
        {
          JdbcUtils.closeSilently((Statement)???);
        }
      }
      ??? = this.session.getMetaData();
      Object localObject2 = ((DatabaseMetaData)???).getDatabaseProductName() + " " + ((DatabaseMetaData)???).getDatabaseProductVersion();
      
      localStringBuilder.append("setNode(" + i + ", 0, 0, 'info', '" + PageParser.escapeJavaScript((String)localObject2) + "', null);\n");
      
      localStringBuilder.append("refreshQueryTables();");
      this.session.put("tree", localStringBuilder.toString());
    }
    catch (Exception localException)
    {
      this.session.put("tree", "");
      this.session.put("error", getStackTrace(0, localException, bool));
    }
    return "tables.jsp";
  }
  
  private String getStackTrace(int paramInt, Throwable paramThrowable, boolean paramBoolean)
  {
    try
    {
      StringWriter localStringWriter = new StringWriter();
      paramThrowable.printStackTrace(new PrintWriter(localStringWriter));
      String str1 = localStringWriter.toString();
      str1 = PageParser.escapeHtml(str1);
      if (paramBoolean) {
        str1 = linkToSource(str1);
      }
      str1 = StringUtils.replaceAll(str1, "\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
      
      String str2 = PageParser.escapeHtml(paramThrowable.getMessage());
      String str3 = "<a class=\"error\" href=\"#\" onclick=\"var x=document.getElementById('st" + paramInt + "').style;x.display=x.display==''?'none':'';\">" + str2 + "</a>";
      if ((paramThrowable instanceof SQLException))
      {
        SQLException localSQLException = (SQLException)paramThrowable;
        str3 = str3 + " " + localSQLException.getSQLState() + "/" + localSQLException.getErrorCode();
        if (paramBoolean)
        {
          int i = localSQLException.getErrorCode();
          str3 = str3 + " <a href=\"http://h2database.com/javadoc/org/h2/constant/ErrorCode.html#c" + i + "\">(${text.a.help})</a>";
        }
      }
      str3 = str3 + "<span style=\"display: none;\" id=\"st" + paramInt + "\"><br />" + str1 + "</span>";
      
      return formatAsError(str3);
    }
    catch (OutOfMemoryError localOutOfMemoryError)
    {
      this.server.traceError(paramThrowable);
    }
    return paramThrowable.toString();
  }
  
  private static String linkToSource(String paramString)
  {
    try
    {
      StringBuilder localStringBuilder = new StringBuilder(paramString.length());
      int i = paramString.indexOf("<br />");
      localStringBuilder.append(paramString.substring(0, i));
      for (;;)
      {
        int j = paramString.indexOf("org.h2.", i);
        if (j < 0)
        {
          localStringBuilder.append(paramString.substring(i));
          break;
        }
        localStringBuilder.append(paramString.substring(i, j));
        int k = paramString.indexOf(')', j);
        if (k < 0)
        {
          localStringBuilder.append(paramString.substring(i));
          break;
        }
        String str1 = paramString.substring(j, k);
        int m = str1.lastIndexOf('(');
        int n = str1.lastIndexOf('.', m - 1);
        int i1 = str1.lastIndexOf('.', n - 1);
        String str2 = str1.substring(0, i1);
        int i2 = str1.lastIndexOf(':');
        String str3 = str1.substring(m + 1, i2);
        String str4 = str1.substring(i2 + 1, str1.length());
        String str5 = str2.replace('.', '/') + "/" + str3;
        localStringBuilder.append("<a href=\"http://h2database.com/html/source.html?file=");
        localStringBuilder.append(str5);
        localStringBuilder.append("&line=");
        localStringBuilder.append(str4);
        localStringBuilder.append("&build=");
        localStringBuilder.append(183);
        localStringBuilder.append("\">");
        localStringBuilder.append(str1);
        localStringBuilder.append("</a>");
        i = k;
      }
      return localStringBuilder.toString();
    }
    catch (Throwable localThrowable) {}
    return paramString;
  }
  
  private static String formatAsError(String paramString)
  {
    return "<div class=\"error\">" + paramString + "</div>";
  }
  
  private String test()
  {
    String str1 = this.attributes.getProperty("driver", "");
    String str2 = this.attributes.getProperty("url", "");
    String str3 = this.attributes.getProperty("user", "");
    String str4 = this.attributes.getProperty("password", "");
    this.session.put("driver", str1);
    this.session.put("url", str2);
    this.session.put("user", str3);
    boolean bool = str2.startsWith("jdbc:h2:");
    try
    {
      long l1 = System.currentTimeMillis();
      String str5 = "";String str6 = "";
      Profiler localProfiler = new Profiler();
      localProfiler.startCollecting();
      Connection localConnection;
      try
      {
        localConnection = this.server.getConnection(str1, str2, str3, str4);
      }
      finally
      {
        localProfiler.stopCollecting();
        str5 = localProfiler.getTop(3);
      }
      localProfiler = new Profiler();
      localProfiler.startCollecting();
      try
      {
        JdbcUtils.closeSilently(localConnection);
      }
      finally
      {
        localProfiler.stopCollecting();
        str6 = localProfiler.getTop(3);
      }
      long l2 = System.currentTimeMillis() - l1;
      String str7;
      if (l2 > 1000L) {
        str7 = "<a class=\"error\" href=\"#\" onclick=\"var x=document.getElementById('prof').style;x.display=x.display==''?'none':'';\">${text.login.testSuccessful}</a><span style=\"display: none;\" id=\"prof\"><br />" + PageParser.escapeHtml(str5) + "<br />" + PageParser.escapeHtml(str6) + "</span>";
      } else {
        str7 = "${text.login.testSuccessful}";
      }
      this.session.put("error", str7);
      
      return "login.jsp";
    }
    catch (Exception localException)
    {
      this.session.put("error", getLoginError(localException, bool));
    }
    return "login.jsp";
  }
  
  private String getLoginError(Exception paramException, boolean paramBoolean)
  {
    if (((paramException instanceof JdbcSQLException)) && (((JdbcSQLException)paramException).getErrorCode() == 90086)) {
      return "${text.login.driverNotFound}<br />" + getStackTrace(0, paramException, paramBoolean);
    }
    return getStackTrace(0, paramException, paramBoolean);
  }
  
  private String login()
  {
    String str1 = this.attributes.getProperty("driver", "");
    String str2 = this.attributes.getProperty("url", "");
    String str3 = this.attributes.getProperty("user", "");
    String str4 = this.attributes.getProperty("password", "");
    this.session.put("autoCommit", "checked");
    this.session.put("autoComplete", "1");
    this.session.put("maxrows", "1000");
    boolean bool = str2.startsWith("jdbc:h2:");
    try
    {
      Connection localConnection = this.server.getConnection(str1, str2, str3, str4);
      this.session.setConnection(localConnection);
      this.session.put("url", str2);
      this.session.put("user", str3);
      this.session.remove("error");
      settingSave();
      return "frame.jsp";
    }
    catch (Exception localException)
    {
      this.session.put("error", getLoginError(localException, bool));
    }
    return "login.jsp";
  }
  
  private String logout()
  {
    try
    {
      Connection localConnection = this.session.getConnection();
      this.session.setConnection(null);
      this.session.remove("conn");
      this.session.remove("result");
      this.session.remove("tables");
      this.session.remove("user");
      this.session.remove("tool");
      if (localConnection != null) {
        if (this.session.getShutdownServerOnDisconnect()) {
          this.server.shutdown();
        } else {
          localConnection.close();
        }
      }
    }
    catch (Exception localException)
    {
      trace(localException.toString());
    }
    return "index.do";
  }
  
  private String query()
  {
    String str1 = this.attributes.getProperty("sql").trim();
    try
    {
      ScriptReader localScriptReader = new ScriptReader(new StringReader(str1));
      final ArrayList localArrayList = New.arrayList();
      for (;;)
      {
        localObject = localScriptReader.readStatement();
        if (localObject == null) {
          break;
        }
        localArrayList.add(localObject);
      }
      final Object localObject = this.session.getConnection();
      if ((SysProperties.CONSOLE_STREAM) && (this.server.getAllowChunked()))
      {
        str2 = new String(this.server.getFile("result.jsp"), Constants.UTF8);
        int i = str2.indexOf("${result}");
        
        localArrayList.add(0, str2.substring(0, i));
        localArrayList.add(str2.substring(i + "${result}".length()));
        this.session.put("chunks", new Iterator()
        {
          private int i;
          
          public boolean hasNext()
          {
            return this.i < localArrayList.size();
          }
          
          public String next()
          {
            String str = (String)localArrayList.get(this.i++);
            if ((this.i == 1) || (this.i == localArrayList.size())) {
              return str;
            }
            StringBuilder localStringBuilder = new StringBuilder();
            WebApp.this.query(localObject, str, this.i - 1, localArrayList.size() - 2, localStringBuilder);
            return localStringBuilder.toString();
          }
          
          public void remove()
          {
            throw new UnsupportedOperationException();
          }
        });
        return "result.jsp";
      }
      StringBuilder localStringBuilder = new StringBuilder();
      for (int j = 0; j < localArrayList.size(); j++)
      {
        String str3 = (String)localArrayList.get(j);
        query((Connection)localObject, str3, j, localArrayList.size(), localStringBuilder);
      }
      String str2 = localStringBuilder.toString();
      this.session.put("result", str2);
    }
    catch (Throwable localThrowable)
    {
      this.session.put("result", getStackTrace(0, localThrowable, this.session.getContents().isH2()));
    }
    return "result.jsp";
  }
  
  void query(Connection paramConnection, String paramString, int paramInt1, int paramInt2, StringBuilder paramStringBuilder)
  {
    if ((!paramString.startsWith("@")) || (!paramString.endsWith("."))) {
      paramStringBuilder.append(PageParser.escapeHtml(paramString + ";")).append("<br />");
    }
    boolean bool = paramString.startsWith("@edit");
    paramStringBuilder.append(getResult(paramConnection, paramInt1 + 1, paramString, paramInt2 == 1, bool)).append("<br />");
  }
  
  private String editResult()
  {
    ResultSet localResultSet = this.session.result;
    int i = Integer.parseInt(this.attributes.getProperty("row"));
    int j = Integer.parseInt(this.attributes.getProperty("op"));
    String str1 = "";String str2 = "";
    try
    {
      if (j == 1)
      {
        int k = i < 0 ? 1 : 0;
        if (k != 0) {
          localResultSet.moveToInsertRow();
        } else {
          localResultSet.absolute(i);
        }
        for (int m = 0; m < localResultSet.getMetaData().getColumnCount(); m++)
        {
          String str4 = this.attributes.getProperty("r" + i + "c" + (m + 1));
          unescapeData(str4, localResultSet, m + 1);
        }
        if (k != 0) {
          localResultSet.insertRow();
        } else {
          localResultSet.updateRow();
        }
      }
      else if (j == 2)
      {
        localResultSet.absolute(i);
        localResultSet.deleteRow();
      }
      else if (j != 3) {}
      str3 = "@edit " + (String)this.session.get("resultSetSQL");
    }
    catch (Throwable localThrowable)
    {
      str1 = "<br />" + getStackTrace(0, localThrowable, this.session.getContents().isH2());
      str2 = formatAsError(localThrowable.getMessage());
    }
    String str3;
    Connection localConnection = this.session.getConnection();
    str1 = str2 + getResult(localConnection, -1, str3, true, true) + str1;
    this.session.put("result", str1);
    return "result.jsp";
  }
  
  private ResultSet getMetaResultSet(Connection paramConnection, String paramString)
    throws SQLException
  {
    DatabaseMetaData localDatabaseMetaData = paramConnection.getMetaData();
    Object localObject1;
    boolean bool2;
    if (isBuiltIn(paramString, "@best_row_identifier"))
    {
      localObject1 = split(paramString);
      int i = localObject1[4] == null ? 0 : Integer.parseInt(localObject1[4]);
      bool2 = localObject1[5] == null ? false : Boolean.parseBoolean(localObject1[5]);
      return localDatabaseMetaData.getBestRowIdentifier(localObject1[1], localObject1[2], localObject1[3], i, bool2);
    }
    if (isBuiltIn(paramString, "@catalogs")) {
      return localDatabaseMetaData.getCatalogs();
    }
    if (isBuiltIn(paramString, "@columns"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getColumns(localObject1[1], localObject1[2], localObject1[3], localObject1[4]);
    }
    if (isBuiltIn(paramString, "@column_privileges"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getColumnPrivileges(localObject1[1], localObject1[2], localObject1[3], localObject1[4]);
    }
    if (isBuiltIn(paramString, "@cross_references"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getCrossReference(localObject1[1], localObject1[2], localObject1[3], localObject1[4], localObject1[5], localObject1[6]);
    }
    if (isBuiltIn(paramString, "@exported_keys"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getExportedKeys(localObject1[1], localObject1[2], localObject1[3]);
    }
    if (isBuiltIn(paramString, "@imported_keys"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getImportedKeys(localObject1[1], localObject1[2], localObject1[3]);
    }
    if (isBuiltIn(paramString, "@index_info"))
    {
      localObject1 = split(paramString);
      boolean bool1 = localObject1[4] == null ? false : Boolean.parseBoolean(localObject1[4]);
      bool2 = localObject1[5] == null ? false : Boolean.parseBoolean(localObject1[5]);
      return localDatabaseMetaData.getIndexInfo(localObject1[1], localObject1[2], localObject1[3], bool1, bool2);
    }
    if (isBuiltIn(paramString, "@primary_keys"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getPrimaryKeys(localObject1[1], localObject1[2], localObject1[3]);
    }
    if (isBuiltIn(paramString, "@procedures"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getProcedures(localObject1[1], localObject1[2], localObject1[3]);
    }
    if (isBuiltIn(paramString, "@procedure_columns"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getProcedureColumns(localObject1[1], localObject1[2], localObject1[3], localObject1[4]);
    }
    if (isBuiltIn(paramString, "@schemas")) {
      return localDatabaseMetaData.getSchemas();
    }
    Object localObject2;
    if (isBuiltIn(paramString, "@tables"))
    {
      localObject1 = split(paramString);
      localObject2 = localObject1[4] == null ? null : StringUtils.arraySplit(localObject1[4], ',', false);
      return localDatabaseMetaData.getTables(localObject1[1], localObject1[2], localObject1[3], (String[])localObject2);
    }
    if (isBuiltIn(paramString, "@table_privileges"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getTablePrivileges(localObject1[1], localObject1[2], localObject1[3]);
    }
    if (isBuiltIn(paramString, "@table_types")) {
      return localDatabaseMetaData.getTableTypes();
    }
    if (isBuiltIn(paramString, "@type_info")) {
      return localDatabaseMetaData.getTypeInfo();
    }
    if (isBuiltIn(paramString, "@udts"))
    {
      localObject1 = split(paramString);
      if (localObject1[4] == null)
      {
        localObject2 = null;
      }
      else
      {
        String[] arrayOfString = StringUtils.arraySplit(localObject1[4], ',', false);
        localObject2 = new int[arrayOfString.length];
        for (int j = 0; j < arrayOfString.length; j++) {
          localObject2[j] = Integer.parseInt(arrayOfString[j]);
        }
      }
      return localDatabaseMetaData.getUDTs(localObject1[1], localObject1[2], localObject1[3], (int[])localObject2);
    }
    if (isBuiltIn(paramString, "@version_columns"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getVersionColumns(localObject1[1], localObject1[2], localObject1[3]);
    }
    if (isBuiltIn(paramString, "@memory"))
    {
      localObject1 = new SimpleResultSet();
      ((SimpleResultSet)localObject1).addColumn("Type", 12, 0, 0);
      ((SimpleResultSet)localObject1).addColumn("KB", 12, 0, 0);
      ((SimpleResultSet)localObject1).addRow(new Object[] { "Used Memory", "" + Utils.getMemoryUsed() });
      ((SimpleResultSet)localObject1).addRow(new Object[] { "Free Memory", "" + Utils.getMemoryFree() });
      return (ResultSet)localObject1;
    }
    if (isBuiltIn(paramString, "@info"))
    {
      localObject1 = new SimpleResultSet();
      ((SimpleResultSet)localObject1).addColumn("KEY", 12, 0, 0);
      ((SimpleResultSet)localObject1).addColumn("VALUE", 12, 0, 0);
      ((SimpleResultSet)localObject1).addRow(new Object[] { "conn.getCatalog", paramConnection.getCatalog() });
      ((SimpleResultSet)localObject1).addRow(new Object[] { "conn.getAutoCommit", "" + paramConnection.getAutoCommit() });
      ((SimpleResultSet)localObject1).addRow(new Object[] { "conn.getTransactionIsolation", "" + paramConnection.getTransactionIsolation() });
      ((SimpleResultSet)localObject1).addRow(new Object[] { "conn.getWarnings", "" + paramConnection.getWarnings() });
      try
      {
        localObject2 = "" + paramConnection.getTypeMap();
      }
      catch (SQLException localSQLException)
      {
        localObject2 = localSQLException.toString();
      }
      ((SimpleResultSet)localObject1).addRow(new Object[] { "conn.getTypeMap", "" + (String)localObject2 });
      ((SimpleResultSet)localObject1).addRow(new Object[] { "conn.isReadOnly", "" + paramConnection.isReadOnly() });
      ((SimpleResultSet)localObject1).addRow(new Object[] { "conn.getHoldability", "" + paramConnection.getHoldability() });
      addDatabaseMetaData((SimpleResultSet)localObject1, localDatabaseMetaData);
      return (ResultSet)localObject1;
    }
    if (isBuiltIn(paramString, "@attributes"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getAttributes(localObject1[1], localObject1[2], localObject1[3], localObject1[4]);
    }
    if (isBuiltIn(paramString, "@super_tables"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getSuperTables(localObject1[1], localObject1[2], localObject1[3]);
    }
    if (isBuiltIn(paramString, "@super_types"))
    {
      localObject1 = split(paramString);
      return localDatabaseMetaData.getSuperTypes(localObject1[1], localObject1[2], localObject1[3]);
    }
    if ((isBuiltIn(paramString, "@prof_stop")) && 
      (this.profiler != null))
    {
      this.profiler.stopCollecting();
      localObject1 = new SimpleResultSet();
      ((SimpleResultSet)localObject1).addColumn("Top Stack Trace(s)", 12, 0, 0);
      ((SimpleResultSet)localObject1).addRow(new Object[] { this.profiler.getTop(3) });
      this.profiler = null;
      return (ResultSet)localObject1;
    }
    return null;
  }
  
  private static void addDatabaseMetaData(SimpleResultSet paramSimpleResultSet, DatabaseMetaData paramDatabaseMetaData)
  {
    Method[] arrayOfMethod1 = DatabaseMetaData.class.getDeclaredMethods();
    Arrays.sort(arrayOfMethod1, new Comparator()
    {
      public int compare(Method paramAnonymousMethod1, Method paramAnonymousMethod2)
      {
        return paramAnonymousMethod1.toString().compareTo(paramAnonymousMethod2.toString());
      }
    });
    for (Method localMethod : arrayOfMethod1) {
      if (localMethod.getParameterTypes().length == 0) {
        try
        {
          Object localObject = localMethod.invoke(paramDatabaseMetaData, new Object[0]);
          paramSimpleResultSet.addRow(new Object[] { "meta." + localMethod.getName(), "" + localObject });
        }
        catch (InvocationTargetException localInvocationTargetException)
        {
          paramSimpleResultSet.addRow(new Object[] { "meta." + localMethod.getName(), localInvocationTargetException.getTargetException().toString() });
        }
        catch (Exception localException)
        {
          paramSimpleResultSet.addRow(new Object[] { "meta." + localMethod.getName(), localException.toString() });
        }
      }
    }
  }
  
  private static String[] split(String paramString)
  {
    String[] arrayOfString1 = new String[10];
    String[] arrayOfString2 = StringUtils.arraySplit(paramString, ' ', true);
    System.arraycopy(arrayOfString2, 0, arrayOfString1, 0, arrayOfString2.length);
    for (int i = 0; i < arrayOfString1.length; i++) {
      if ("null".equals(arrayOfString1[i])) {
        arrayOfString1[i] = null;
      }
    }
    return arrayOfString1;
  }
  
  private int getMaxrows()
  {
    String str = (String)this.session.get("maxrows");
    int i = str == null ? 0 : Integer.parseInt(str);
    return i;
  }
  
  private String getResult(Connection paramConnection, int paramInt, String paramString, boolean paramBoolean1, boolean paramBoolean2)
  {
    try
    {
      paramString = paramString.trim();
      StringBuilder localStringBuilder = new StringBuilder();
      str1 = StringUtils.toUpperEnglish(paramString);
      Object localObject1;
      if ((str1.contains("CREATE")) || (str1.contains("DROP")) || (str1.contains("ALTER")) || (str1.contains("RUNSCRIPT")))
      {
        localObject1 = this.attributes.getProperty("jsessionid");
        localStringBuilder.append("<script type=\"text/javascript\">parent['h2menu'].location='tables.do?jsessionid=" + (String)localObject1 + "';</script>");
      }
      DbContents localDbContents = this.session.getContents();
      if ((paramBoolean2) || ((paramBoolean1) && (localDbContents.isH2()))) {
        localObject1 = paramConnection.createStatement(1004, 1008);
      } else {
        localObject1 = paramConnection.createStatement();
      }
      long l = System.currentTimeMillis();
      boolean bool1 = false;
      int i = 0;
      boolean bool2 = false;
      boolean bool3 = false;
      String str2;
      if (isBuiltIn(paramString, "@autocommit_true"))
      {
        paramConnection.setAutoCommit(true);
        return "${text.result.autoCommitOn}";
      }
      if (isBuiltIn(paramString, "@autocommit_false"))
      {
        paramConnection.setAutoCommit(false);
        return "${text.result.autoCommitOff}";
      }
      if (isBuiltIn(paramString, "@cancel"))
      {
        localObject1 = this.session.executingStatement;
        if (localObject1 != null)
        {
          ((Statement)localObject1).cancel();
          localStringBuilder.append("${text.result.statementWasCanceled}");
        }
        else
        {
          localStringBuilder.append("${text.result.noRunningStatement}");
        }
        return localStringBuilder.toString();
      }
      if (isBuiltIn(paramString, "@edit"))
      {
        bool2 = true;
        paramString = paramString.substring("@edit".length()).trim();
        this.session.put("resultSetSQL", paramString);
      }
      if (isBuiltIn(paramString, "@list"))
      {
        bool3 = true;
        paramString = paramString.substring("@list".length()).trim();
      }
      if (isBuiltIn(paramString, "@meta"))
      {
        bool1 = true;
        paramString = paramString.substring("@meta".length()).trim();
      }
      String str5;
      Object localObject2;
      if (isBuiltIn(paramString, "@generated"))
      {
        i = 1;
        paramString = paramString.substring("@generated".length()).trim();
      }
      else
      {
        if (isBuiltIn(paramString, "@history"))
        {
          localStringBuilder.append(getCommandHistoryString());
          return localStringBuilder.toString();
        }
        int j;
        if (isBuiltIn(paramString, "@loop"))
        {
          paramString = paramString.substring("@loop".length()).trim();
          j = paramString.indexOf(' ');
          int m = Integer.decode(paramString.substring(0, j)).intValue();
          paramString = paramString.substring(j).trim();
          return executeLoop(paramConnection, m, paramString);
        }
        String str4;
        if (isBuiltIn(paramString, "@maxrows"))
        {
          j = (int)Double.parseDouble(paramString.substring("@maxrows".length()).trim());
          
          this.session.put("maxrows", "" + j);
          return "${text.result.maxrowsSet}";
        }
        if (isBuiltIn(paramString, "@parameter_meta"))
        {
          paramString = paramString.substring("@parameter_meta".length()).trim();
          localObject2 = paramConnection.prepareStatement(paramString);
          localStringBuilder.append(getParameterResultSet(((PreparedStatement)localObject2).getParameterMetaData()));
          return localStringBuilder.toString();
        }
        if (isBuiltIn(paramString, "@password_hash"))
        {
          paramString = paramString.substring("@password_hash".length()).trim();
          localObject2 = split(paramString);
          return StringUtils.convertBytesToHex(SHA256.getKeyPasswordHash(localObject2[0], localObject2[1].toCharArray()));
        }
        if (isBuiltIn(paramString, "@prof_start"))
        {
          if (this.profiler != null) {
            this.profiler.stopCollecting();
          }
          this.profiler = new Profiler();
          this.profiler.startCollecting();
          return "Ok";
        }
        int n;
        if (isBuiltIn(paramString, "@sleep"))
        {
          localObject2 = paramString.substring("@sleep".length()).trim();
          n = 1;
          if (((String)localObject2).length() > 0) {
            n = Integer.parseInt((String)localObject2);
          }
          Thread.sleep(n * 1000);
          return "Ok";
        }
        if (isBuiltIn(paramString, "@transaction_isolation"))
        {
          localObject2 = paramString.substring("@transaction_isolation".length()).trim();
          if (((String)localObject2).length() > 0)
          {
            n = Integer.parseInt((String)localObject2);
            paramConnection.setTransactionIsolation(n);
          }
          localStringBuilder.append("Transaction Isolation: " + paramConnection.getTransactionIsolation() + "<br />");
          
          localStringBuilder.append("1: read_uncommitted<br />");
          
          localStringBuilder.append("2: read_committed<br />");
          
          localStringBuilder.append("4: repeatable_read<br />");
          
          localStringBuilder.append("8: serializable");
        }
      }
      ResultSet localResultSet;
      if (paramString.startsWith("@"))
      {
        localResultSet = getMetaResultSet(paramConnection, paramString);
        if (localResultSet == null)
        {
          localStringBuilder.append("?: " + paramString);
          return localStringBuilder.toString();
        }
      }
      else
      {
        int k = getMaxrows();
        ((Statement)localObject1).setMaxRows(k);
        this.session.executingStatement = ((Statement)localObject1);
        boolean bool4 = ((Statement)localObject1).execute(paramString);
        this.session.addCommand(paramString);
        if (i != 0)
        {
          localResultSet = null;
          localResultSet = ((Statement)localObject1).getGeneratedKeys();
        }
        else
        {
          if (!bool4)
          {
            localStringBuilder.append("${text.result.updateCount}: " + ((Statement)localObject1).getUpdateCount());
            l = System.currentTimeMillis() - l;
            localStringBuilder.append("<br />(").append(l).append(" ms)");
            ((Statement)localObject1).close();
            return localStringBuilder.toString();
          }
          localResultSet = ((Statement)localObject1).getResultSet();
        }
      }
      l = System.currentTimeMillis() - l;
      localStringBuilder.append(getResultSet(paramString, localResultSet, bool1, bool3, bool2, l, paramBoolean1));
      if (!bool2) {
        ((Statement)localObject1).close();
      }
      return localStringBuilder.toString();
    }
    catch (Throwable localThrowable)
    {
      String str1;
      return getStackTrace(paramInt, localThrowable, this.session.getContents().isH2());
    }
    finally
    {
      this.session.executingStatement = null;
    }
  }
  
  private static boolean isBuiltIn(String paramString1, String paramString2)
  {
    return StringUtils.startsWithIgnoreCase(paramString1, paramString2);
  }
  
  private String executeLoop(Connection paramConnection, int paramInt, String paramString)
    throws SQLException
  {
    ArrayList localArrayList = New.arrayList();
    int i = 0;
    while (!this.stop)
    {
      i = paramString.indexOf('?', i);
      if (i < 0) {
        break;
      }
      if (isBuiltIn(paramString.substring(i), "?/*rnd*/"))
      {
        localArrayList.add(Integer.valueOf(1));
        paramString = paramString.substring(0, i) + "?" + paramString.substring(i + "/*rnd*/".length() + 1);
      }
      else
      {
        localArrayList.add(Integer.valueOf(0));
      }
      i++;
    }
    Random localRandom = new Random(1L);
    long l = System.currentTimeMillis();
    int j;
    int k;
    Object localObject2;
    if (isBuiltIn(paramString, "@statement"))
    {
      paramString = paramString.substring("@statement".length()).trim();
      j = 0;
      localObject1 = paramConnection.createStatement();
      for (k = 0; (!this.stop) && (k < paramInt); k++)
      {
        String str = paramString;
        for (localObject2 = localArrayList.iterator(); ((Iterator)localObject2).hasNext();)
        {
          Integer localInteger = (Integer)((Iterator)localObject2).next();
          i = str.indexOf('?');
          if (localInteger.intValue() == 1) {
            str = str.substring(0, i) + localRandom.nextInt(paramInt) + str.substring(i + 1);
          } else {
            str = str.substring(0, i) + k + str.substring(i + 1);
          }
        }
        if (((Statement)localObject1).execute(str))
        {
          while ((!this.stop) && (((ResultSet)localObject2).next())) {}
          ((ResultSet)localObject2).close();
        }
      }
    }
    else
    {
      j = 1;
      localObject1 = paramConnection.prepareStatement(paramString);
      for (k = 0; (!this.stop) && (k < paramInt); k++)
      {
        for (int m = 0; m < localArrayList.size(); m++)
        {
          localObject2 = (Integer)localArrayList.get(m);
          if (((Integer)localObject2).intValue() == 1) {
            ((PreparedStatement)localObject1).setInt(m + 1, localRandom.nextInt(paramInt));
          } else {
            ((PreparedStatement)localObject1).setInt(m + 1, k);
          }
        }
        if (this.session.getContents().isSQLite())
        {
          ((PreparedStatement)localObject1).executeUpdate();
        }
        else if (((PreparedStatement)localObject1).execute())
        {
          ResultSet localResultSet = ((PreparedStatement)localObject1).getResultSet();
          while ((!this.stop) && (localResultSet.next())) {}
          localResultSet.close();
        }
      }
    }
    l = System.currentTimeMillis() - l;
    Object localObject1 = new StatementBuilder();
    ((StatementBuilder)localObject1).append(l).append(" ms: ").append(paramInt).append(" * ");
    if (j != 0) {
      ((StatementBuilder)localObject1).append("(Prepared) ");
    } else {
      ((StatementBuilder)localObject1).append("(Statement) ");
    }
    ((StatementBuilder)localObject1).append('(');
    for (Iterator localIterator = localArrayList.iterator(); localIterator.hasNext();)
    {
      int n = ((Integer)localIterator.next()).intValue();
      ((StatementBuilder)localObject1).appendExceptFirst(", ");
      ((StatementBuilder)localObject1).append(n == 0 ? "i" : "rnd");
    }
    return ((StatementBuilder)localObject1).append(") ").append(paramString).toString();
  }
  
  private String getCommandHistoryString()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    ArrayList localArrayList = this.session.getCommandHistory();
    localStringBuilder.append("<table cellspacing=0 cellpadding=0><tr><th></th><th>Command</th></tr>");
    for (int i = localArrayList.size() - 1; i >= 0; i--)
    {
      String str = (String)localArrayList.get(i);
      localStringBuilder.append("<tr><td><a href=\"getHistory.do?id=").append(i).append("&jsessionid=${sessionId}\" target=\"h2query\" >").append("<img width=16 height=16 src=\"ico_write.gif\" onmouseover = \"this.className ='icon_hover'\" ").append("onmouseout = \"this.className ='icon'\" class=\"icon\" alt=\"${text.resultEdit.edit}\" ").append("title=\"${text.resultEdit.edit}\" border=\"1\"/></a>").append("</td><td>").append(PageParser.escapeHtml(str)).append("</td></tr>");
    }
    localStringBuilder.append("</table>");
    return localStringBuilder.toString();
  }
  
  private static String getParameterResultSet(ParameterMetaData paramParameterMetaData)
    throws SQLException
  {
    StringBuilder localStringBuilder = new StringBuilder();
    if (paramParameterMetaData == null) {
      return "No parameter meta data";
    }
    localStringBuilder.append("<table cellspacing=0 cellpadding=0>").append("<tr><th>className</th><th>mode</th><th>type</th>").append("<th>typeName</th><th>precision</th><th>scale</th></tr>");
    for (int i = 0; i < paramParameterMetaData.getParameterCount(); i++) {
      localStringBuilder.append("</tr><td>").append(paramParameterMetaData.getParameterClassName(i + 1)).append("</td><td>").append(paramParameterMetaData.getParameterMode(i + 1)).append("</td><td>").append(paramParameterMetaData.getParameterType(i + 1)).append("</td><td>").append(paramParameterMetaData.getParameterTypeName(i + 1)).append("</td><td>").append(paramParameterMetaData.getPrecision(i + 1)).append("</td><td>").append(paramParameterMetaData.getScale(i + 1)).append("</td></tr>");
    }
    localStringBuilder.append("</table>");
    return localStringBuilder.toString();
  }
  
  private String getResultSet(String paramString, ResultSet paramResultSet, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3, long paramLong, boolean paramBoolean4)
    throws SQLException
  {
    int i = getMaxrows();
    paramLong = System.currentTimeMillis() - paramLong;
    StringBuilder localStringBuilder = new StringBuilder();
    if (paramBoolean3) {
      localStringBuilder.append("<form id=\"editing\" name=\"editing\" method=\"post\" action=\"editResult.do?jsessionid=${sessionId}\" id=\"mainForm\" target=\"h2result\"><input type=\"hidden\" name=\"op\" value=\"1\" /><input type=\"hidden\" name=\"row\" value=\"\" /><table cellspacing=0 cellpadding=0 id=\"editTable\">");
    } else {
      localStringBuilder.append("<table cellspacing=0 cellpadding=0>");
    }
    if (paramBoolean1)
    {
      localObject = new SimpleResultSet();
      ((SimpleResultSet)localObject).addColumn("#", 4, 0, 0);
      ((SimpleResultSet)localObject).addColumn("label", 12, 0, 0);
      ((SimpleResultSet)localObject).addColumn("catalog", 12, 0, 0);
      ((SimpleResultSet)localObject).addColumn("schema", 12, 0, 0);
      ((SimpleResultSet)localObject).addColumn("table", 12, 0, 0);
      ((SimpleResultSet)localObject).addColumn("column", 12, 0, 0);
      ((SimpleResultSet)localObject).addColumn("type", 4, 0, 0);
      ((SimpleResultSet)localObject).addColumn("typeName", 12, 0, 0);
      ((SimpleResultSet)localObject).addColumn("class", 12, 0, 0);
      ((SimpleResultSet)localObject).addColumn("precision", 4, 0, 0);
      ((SimpleResultSet)localObject).addColumn("scale", 4, 0, 0);
      ((SimpleResultSet)localObject).addColumn("displaySize", 4, 0, 0);
      ((SimpleResultSet)localObject).addColumn("autoIncrement", 16, 0, 0);
      ((SimpleResultSet)localObject).addColumn("caseSensitive", 16, 0, 0);
      ((SimpleResultSet)localObject).addColumn("currency", 16, 0, 0);
      ((SimpleResultSet)localObject).addColumn("nullable", 4, 0, 0);
      ((SimpleResultSet)localObject).addColumn("readOnly", 16, 0, 0);
      ((SimpleResultSet)localObject).addColumn("searchable", 16, 0, 0);
      ((SimpleResultSet)localObject).addColumn("signed", 16, 0, 0);
      ((SimpleResultSet)localObject).addColumn("writable", 16, 0, 0);
      ((SimpleResultSet)localObject).addColumn("definitelyWritable", 16, 0, 0);
      ResultSetMetaData localResultSetMetaData = paramResultSet.getMetaData();
      for (k = 1; k <= localResultSetMetaData.getColumnCount(); k++) {
        ((SimpleResultSet)localObject).addRow(new Object[] { Integer.valueOf(k), localResultSetMetaData.getColumnLabel(k), localResultSetMetaData.getCatalogName(k), localResultSetMetaData.getSchemaName(k), localResultSetMetaData.getTableName(k), localResultSetMetaData.getColumnName(k), Integer.valueOf(localResultSetMetaData.getColumnType(k)), localResultSetMetaData.getColumnTypeName(k), localResultSetMetaData.getColumnClassName(k), Integer.valueOf(localResultSetMetaData.getPrecision(k)), Integer.valueOf(localResultSetMetaData.getScale(k)), Integer.valueOf(localResultSetMetaData.getColumnDisplaySize(k)), Boolean.valueOf(localResultSetMetaData.isAutoIncrement(k)), Boolean.valueOf(localResultSetMetaData.isCaseSensitive(k)), Boolean.valueOf(localResultSetMetaData.isCurrency(k)), Integer.valueOf(localResultSetMetaData.isNullable(k)), Boolean.valueOf(localResultSetMetaData.isReadOnly(k)), Boolean.valueOf(localResultSetMetaData.isSearchable(k)), Boolean.valueOf(localResultSetMetaData.isSigned(k)), Boolean.valueOf(localResultSetMetaData.isWritable(k)), Boolean.valueOf(localResultSetMetaData.isDefinitelyWritable(k)) });
      }
      paramResultSet = (ResultSet)localObject;
    }
    Object localObject = paramResultSet.getMetaData();
    int j = ((ResultSetMetaData)localObject).getColumnCount();
    int k = 0;
    if (paramBoolean2)
    {
      localStringBuilder.append("<tr><th>Column</th><th>Data</th></tr><tr>");
      while ((paramResultSet.next()) && (
        (i <= 0) || (k < i)))
      {
        k++;
        localStringBuilder.append("<tr><td>Row #</td><td>").append(k).append("</tr>");
        for (m = 0; m < j; m++) {
          localStringBuilder.append("<tr><td>").append(PageParser.escapeHtml(((ResultSetMetaData)localObject).getColumnLabel(m + 1))).append("</td><td>").append(escapeData(paramResultSet, m + 1)).append("</td></tr>");
        }
      }
    }
    localStringBuilder.append("<tr>");
    if (paramBoolean3) {
      localStringBuilder.append("<th>${text.resultEdit.action}</th>");
    }
    for (int m = 0; m < j; m++) {
      localStringBuilder.append("<th>").append(PageParser.escapeHtml(((ResultSetMetaData)localObject).getColumnLabel(m + 1))).append("</th>");
    }
    localStringBuilder.append("</tr>");
    while ((paramResultSet.next()) && (
      (i <= 0) || (k < i)))
    {
      k++;
      localStringBuilder.append("<tr>");
      if (paramBoolean3) {
        localStringBuilder.append("<td>").append("<img onclick=\"javascript:editRow(").append(paramResultSet.getRow()).append(",'${sessionId}', '${text.resultEdit.save}', '${text.resultEdit.cancel}'").append(")\" width=16 height=16 src=\"ico_write.gif\" onmouseover = \"this.className ='icon_hover'\" onmouseout = \"this.className ='icon'\" class=\"icon\" alt=\"${text.resultEdit.edit}\" title=\"${text.resultEdit.edit}\" border=\"1\"/>").append("<a href=\"editResult.do?op=2&row=").append(paramResultSet.getRow()).append("&jsessionid=${sessionId}\" target=\"h2result\" ><img width=16 height=16 src=\"ico_remove.gif\" onmouseover = \"this.className ='icon_hover'\" onmouseout = \"this.className ='icon'\" class=\"icon\" alt=\"${text.resultEdit.delete}\" title=\"${text.resultEdit.delete}\" border=\"1\" /></a>").append("</td>");
      }
      for (m = 0; m < j; m++) {
        localStringBuilder.append("<td>").append(escapeData(paramResultSet, m + 1)).append("</td>");
      }
      localStringBuilder.append("</tr>");
    }
    m = 0;
    try
    {
      m = (paramResultSet.getConcurrency() == 1008) && (paramResultSet.getType() != 1003) ? 1 : 0;
    }
    catch (NullPointerException localNullPointerException) {}
    if (paramBoolean3)
    {
      ResultSet localResultSet = this.session.result;
      if (localResultSet != null) {
        localResultSet.close();
      }
      this.session.result = paramResultSet;
    }
    else
    {
      paramResultSet.close();
    }
    if (paramBoolean3)
    {
      localStringBuilder.append("<tr><td>").append("<img onclick=\"javascript:editRow(-1, '${sessionId}', '${text.resultEdit.save}', '${text.resultEdit.cancel}'").append(")\" width=16 height=16 src=\"ico_add.gif\" onmouseover = \"this.className ='icon_hover'\" onmouseout = \"this.className ='icon'\" class=\"icon\" alt=\"${text.resultEdit.add}\" title=\"${text.resultEdit.add}\" border=\"1\"/>").append("</td>");
      for (int n = 0; n < j; n++) {
        localStringBuilder.append("<td></td>");
      }
      localStringBuilder.append("</tr>");
    }
    localStringBuilder.append("</table>");
    if (paramBoolean3) {
      localStringBuilder.append("</form>");
    }
    if (k == 0) {
      localStringBuilder.append("(${text.result.noRows}");
    } else if (k == 1) {
      localStringBuilder.append("(${text.result.1row}");
    } else {
      localStringBuilder.append('(').append(k).append(" ${text.result.rows}");
    }
    localStringBuilder.append(", ");
    paramLong = System.currentTimeMillis() - paramLong;
    localStringBuilder.append(paramLong).append(" ms)");
    if ((!paramBoolean3) && (m != 0) && (paramBoolean4)) {
      localStringBuilder.append("<br /><br /><form name=\"editResult\" method=\"post\" action=\"query.do?jsessionid=${sessionId}\" target=\"h2result\"><input type=\"submit\" class=\"button\" value=\"${text.resultEdit.editResult}\" /><input type=\"hidden\" name=\"sql\" value=\"@edit ").append(PageParser.escapeHtmlData(paramString)).append("\" /></form>");
    }
    return localStringBuilder.toString();
  }
  
  private String settingSave()
  {
    ConnectionInfo localConnectionInfo = new ConnectionInfo();
    localConnectionInfo.name = this.attributes.getProperty("name", "");
    localConnectionInfo.driver = this.attributes.getProperty("driver", "");
    localConnectionInfo.url = this.attributes.getProperty("url", "");
    localConnectionInfo.user = this.attributes.getProperty("user", "");
    this.server.updateSetting(localConnectionInfo);
    this.attributes.put("setting", localConnectionInfo.name);
    this.server.saveProperties(null);
    return "index.do";
  }
  
  private static String escapeData(ResultSet paramResultSet, int paramInt)
    throws SQLException
  {
    String str1 = paramResultSet.getString(paramInt);
    if (str1 == null) {
      return "<i>null</i>";
    }
    if (str1.length() > 100000)
    {
      String str2;
      if (isBinary(paramResultSet.getMetaData().getColumnType(paramInt))) {
        str2 = PageParser.escapeHtml(str1.substring(0, 6)) + "... (" + str1.length() / 2 + " ${text.result.bytes})";
      } else {
        str2 = PageParser.escapeHtml(str1.substring(0, 100)) + "... (" + str1.length() + " ${text.result.characters})";
      }
      return "<div style='display: none'>=+</div>" + str2;
    }
    if ((str1.equals("null")) || (str1.startsWith("= ")) || (str1.startsWith("=+"))) {
      return "<div style='display: none'>= </div>" + PageParser.escapeHtml(str1);
    }
    if (str1.equals("")) {
      return "";
    }
    return PageParser.escapeHtml(str1);
  }
  
  private static boolean isBinary(int paramInt)
  {
    switch (paramInt)
    {
    case -4: 
    case -3: 
    case -2: 
    case 1111: 
    case 2000: 
    case 2004: 
      return true;
    }
    return false;
  }
  
  private void unescapeData(String paramString, ResultSet paramResultSet, int paramInt)
    throws SQLException
  {
    if (paramString.equals("null"))
    {
      paramResultSet.updateNull(paramInt);
      return;
    }
    if (paramString.startsWith("=+")) {
      return;
    }
    if (paramString.equals("=*"))
    {
      int i = paramResultSet.getMetaData().getColumnType(paramInt);
      switch (i)
      {
      case 92: 
        paramResultSet.updateString(paramInt, "12:00:00");
        break;
      case 91: 
      case 93: 
        paramResultSet.updateString(paramInt, "2001-01-01");
        break;
      default: 
        paramResultSet.updateString(paramInt, "1");
      }
      return;
    }
    if (paramString.startsWith("= ")) {
      paramString = paramString.substring(2);
    }
    ResultSetMetaData localResultSetMetaData = paramResultSet.getMetaData();
    int j = localResultSetMetaData.getColumnType(paramInt);
    if (this.session.getContents().isH2())
    {
      paramResultSet.updateString(paramInt, paramString);
      return;
    }
    switch (j)
    {
    case -5: 
      paramResultSet.updateLong(paramInt, Long.decode(paramString).longValue());
      break;
    case 3: 
      paramResultSet.updateBigDecimal(paramInt, new BigDecimal(paramString));
      break;
    case 6: 
    case 8: 
      paramResultSet.updateDouble(paramInt, Double.parseDouble(paramString));
      break;
    case 7: 
      paramResultSet.updateFloat(paramInt, Float.parseFloat(paramString));
      break;
    case 4: 
      paramResultSet.updateInt(paramInt, Integer.decode(paramString).intValue());
      break;
    case -6: 
      paramResultSet.updateShort(paramInt, Short.decode(paramString).shortValue());
      break;
    case -4: 
    case -3: 
    case -2: 
    case -1: 
    case 0: 
    case 1: 
    case 2: 
    case 5: 
    default: 
      paramResultSet.updateString(paramInt, paramString);
    }
  }
  
  private String settingRemove()
  {
    String str = this.attributes.getProperty("name", "");
    this.server.removeSetting(str);
    ArrayList localArrayList = this.server.getSettings();
    if (localArrayList.size() > 0) {
      this.attributes.put("setting", localArrayList.get(0));
    }
    this.server.saveProperties(null);
    return "index.do";
  }
  
  String getMimeType()
  {
    return this.mimeType;
  }
  
  boolean getCache()
  {
    return this.cache;
  }
  
  WebSession getSession()
  {
    return this.session;
  }
  
  private void trace(String paramString)
  {
    this.server.trace(paramString);
  }
  
  static class IndexInfo
  {
    String name;
    String type;
    String columns;
  }
}
