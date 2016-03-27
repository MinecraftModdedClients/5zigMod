package org.h2.fulltext;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.UUID;
import org.h2.api.Trigger;
import org.h2.command.Parser;
import org.h2.engine.Session;
import org.h2.expression.Comparison;
import org.h2.expression.ConditionAndOr;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.expression.ValueExpression;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.DbException;
import org.h2.tools.SimpleResultSet;
import org.h2.util.IOUtils;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.Value;

public class FullText
{
  private static final String FIELD_SCHEMA = "SCHEMA";
  private static final String FIELD_TABLE = "TABLE";
  private static final String FIELD_COLUMNS = "COLUMNS";
  private static final String FIELD_KEYS = "KEYS";
  private static final String FIELD_SCORE = "SCORE";
  private static final String TRIGGER_PREFIX = "FT_";
  private static final String SCHEMA = "FT";
  private static final String SELECT_MAP_BY_WORD_ID = "SELECT ROWID FROM FT.MAP WHERE WORDID=?";
  private static final String SELECT_ROW_BY_ID = "SELECT KEY, INDEXID FROM FT.ROWS WHERE ID=?";
  private static final String FIELD_QUERY = "QUERY";
  
  public static void init(Connection paramConnection)
    throws SQLException
  {
    Statement localStatement = paramConnection.createStatement();
    localStatement.execute("CREATE SCHEMA IF NOT EXISTS FT");
    localStatement.execute("CREATE TABLE IF NOT EXISTS FT.INDEXES(ID INT AUTO_INCREMENT PRIMARY KEY, SCHEMA VARCHAR, TABLE VARCHAR, COLUMNS VARCHAR, UNIQUE(SCHEMA, TABLE))");
    
    localStatement.execute("CREATE TABLE IF NOT EXISTS FT.WORDS(ID INT AUTO_INCREMENT PRIMARY KEY, NAME VARCHAR, UNIQUE(NAME))");
    
    localStatement.execute("CREATE TABLE IF NOT EXISTS FT.ROWS(ID IDENTITY, HASH INT, INDEXID INT, KEY VARCHAR, UNIQUE(HASH, INDEXID, KEY))");
    
    localStatement.execute("CREATE TABLE IF NOT EXISTS FT.MAP(ROWID INT, WORDID INT, PRIMARY KEY(WORDID, ROWID))");
    
    localStatement.execute("CREATE TABLE IF NOT EXISTS FT.IGNORELIST(LIST VARCHAR)");
    
    localStatement.execute("CREATE TABLE IF NOT EXISTS FT.SETTINGS(KEY VARCHAR PRIMARY KEY, VALUE VARCHAR)");
    
    localStatement.execute("CREATE ALIAS IF NOT EXISTS FT_CREATE_INDEX FOR \"" + FullText.class.getName() + ".createIndex\"");
    
    localStatement.execute("CREATE ALIAS IF NOT EXISTS FT_DROP_INDEX FOR \"" + FullText.class.getName() + ".dropIndex\"");
    
    localStatement.execute("CREATE ALIAS IF NOT EXISTS FT_SEARCH FOR \"" + FullText.class.getName() + ".search\"");
    
    localStatement.execute("CREATE ALIAS IF NOT EXISTS FT_SEARCH_DATA FOR \"" + FullText.class.getName() + ".searchData\"");
    
    localStatement.execute("CREATE ALIAS IF NOT EXISTS FT_REINDEX FOR \"" + FullText.class.getName() + ".reindex\"");
    
    localStatement.execute("CREATE ALIAS IF NOT EXISTS FT_DROP_ALL FOR \"" + FullText.class.getName() + ".dropAll\"");
    
    FullTextSettings localFullTextSettings = FullTextSettings.getInstance(paramConnection);
    ResultSet localResultSet = localStatement.executeQuery("SELECT * FROM FT.IGNORELIST");
    while (localResultSet.next())
    {
      localObject = localResultSet.getString(1);
      setIgnoreList(localFullTextSettings, (String)localObject);
    }
    localResultSet = localStatement.executeQuery("SELECT * FROM FT.SETTINGS");
    String str;
    while (localResultSet.next())
    {
      localObject = localResultSet.getString(1);
      if ("whitespaceChars".equals(localObject))
      {
        str = localResultSet.getString(2);
        localFullTextSettings.setWhitespaceChars(str);
      }
    }
    localResultSet = localStatement.executeQuery("SELECT * FROM FT.WORDS");
    Object localObject = localFullTextSettings.getWordList();
    while (localResultSet.next())
    {
      str = localResultSet.getString("NAME");
      int i = localResultSet.getInt("ID");
      str = localFullTextSettings.convertWord(str);
      if (str != null) {
        ((HashMap)localObject).put(str, Integer.valueOf(i));
      }
    }
    localFullTextSettings.setInitialized(true);
  }
  
  public static void createIndex(Connection paramConnection, String paramString1, String paramString2, String paramString3)
    throws SQLException
  {
    init(paramConnection);
    PreparedStatement localPreparedStatement = paramConnection.prepareStatement("INSERT INTO FT.INDEXES(SCHEMA, TABLE, COLUMNS) VALUES(?, ?, ?)");
    
    localPreparedStatement.setString(1, paramString1);
    localPreparedStatement.setString(2, paramString2);
    localPreparedStatement.setString(3, paramString3);
    localPreparedStatement.execute();
    createTrigger(paramConnection, paramString1, paramString2);
    indexExistingRows(paramConnection, paramString1, paramString2);
  }
  
  public static void reindex(Connection paramConnection)
    throws SQLException
  {
    init(paramConnection);
    removeAllTriggers(paramConnection, "FT_");
    FullTextSettings localFullTextSettings = FullTextSettings.getInstance(paramConnection);
    localFullTextSettings.getWordList().clear();
    Statement localStatement = paramConnection.createStatement();
    localStatement.execute("TRUNCATE TABLE FT.WORDS");
    localStatement.execute("TRUNCATE TABLE FT.ROWS");
    localStatement.execute("TRUNCATE TABLE FT.MAP");
    ResultSet localResultSet = localStatement.executeQuery("SELECT * FROM FT.INDEXES");
    while (localResultSet.next())
    {
      String str1 = localResultSet.getString("SCHEMA");
      String str2 = localResultSet.getString("TABLE");
      createTrigger(paramConnection, str1, str2);
      indexExistingRows(paramConnection, str1, str2);
    }
  }
  
  public static void dropIndex(Connection paramConnection, String paramString1, String paramString2)
    throws SQLException
  {
    init(paramConnection);
    PreparedStatement localPreparedStatement = paramConnection.prepareStatement("SELECT ID FROM FT.INDEXES WHERE SCHEMA=? AND TABLE=?");
    
    localPreparedStatement.setString(1, paramString1);
    localPreparedStatement.setString(2, paramString2);
    ResultSet localResultSet = localPreparedStatement.executeQuery();
    if (!localResultSet.next()) {
      return;
    }
    int i = localResultSet.getInt(1);
    localPreparedStatement = paramConnection.prepareStatement("DELETE FROM FT.INDEXES WHERE ID=?");
    
    localPreparedStatement.setInt(1, i);
    localPreparedStatement.execute();
    createOrDropTrigger(paramConnection, paramString1, paramString2, false);
    localPreparedStatement = paramConnection.prepareStatement("DELETE FROM FT.ROWS WHERE INDEXID=? AND ROWNUM<10000");
    int j;
    for (;;)
    {
      localPreparedStatement.setInt(1, i);
      j = localPreparedStatement.executeUpdate();
      if (j == 0) {
        break;
      }
    }
    localPreparedStatement = paramConnection.prepareStatement("DELETE FROM FT.MAP M WHERE NOT EXISTS (SELECT * FROM FT.ROWS R WHERE R.ID=M.ROWID) AND ROWID<10000");
    for (;;)
    {
      j = localPreparedStatement.executeUpdate();
      if (j == 0) {
        break;
      }
    }
  }
  
  public static void dropAll(Connection paramConnection)
    throws SQLException
  {
    init(paramConnection);
    Statement localStatement = paramConnection.createStatement();
    localStatement.execute("DROP SCHEMA IF EXISTS FT");
    removeAllTriggers(paramConnection, "FT_");
    FullTextSettings localFullTextSettings = FullTextSettings.getInstance(paramConnection);
    localFullTextSettings.removeAllIndexes();
    localFullTextSettings.getIgnoreList().clear();
    localFullTextSettings.getWordList().clear();
  }
  
  public static ResultSet search(Connection paramConnection, String paramString, int paramInt1, int paramInt2)
    throws SQLException
  {
    try
    {
      return search(paramConnection, paramString, paramInt1, paramInt2, false);
    }
    catch (DbException localDbException)
    {
      throw DbException.toSQLException(localDbException);
    }
  }
  
  public static ResultSet searchData(Connection paramConnection, String paramString, int paramInt1, int paramInt2)
    throws SQLException
  {
    try
    {
      return search(paramConnection, paramString, paramInt1, paramInt2, true);
    }
    catch (DbException localDbException)
    {
      throw DbException.toSQLException(localDbException);
    }
  }
  
  public static void setIgnoreList(Connection paramConnection, String paramString)
    throws SQLException
  {
    try
    {
      init(paramConnection);
      FullTextSettings localFullTextSettings = FullTextSettings.getInstance(paramConnection);
      setIgnoreList(localFullTextSettings, paramString);
      Statement localStatement = paramConnection.createStatement();
      localStatement.execute("TRUNCATE TABLE FT.IGNORELIST");
      PreparedStatement localPreparedStatement = paramConnection.prepareStatement("INSERT INTO FT.IGNORELIST VALUES(?)");
      
      localPreparedStatement.setString(1, paramString);
      localPreparedStatement.execute();
    }
    catch (DbException localDbException)
    {
      throw DbException.toSQLException(localDbException);
    }
  }
  
  public static void setWhitespaceChars(Connection paramConnection, String paramString)
    throws SQLException
  {
    try
    {
      init(paramConnection);
      FullTextSettings localFullTextSettings = FullTextSettings.getInstance(paramConnection);
      localFullTextSettings.setWhitespaceChars(paramString);
      PreparedStatement localPreparedStatement = paramConnection.prepareStatement("MERGE INTO FT.SETTINGS VALUES(?, ?)");
      
      localPreparedStatement.setString(1, "whitespaceChars");
      localPreparedStatement.setString(2, paramString);
      localPreparedStatement.execute();
    }
    catch (DbException localDbException)
    {
      throw DbException.toSQLException(localDbException);
    }
  }
  
  protected static String asString(Object paramObject, int paramInt)
    throws SQLException
  {
    if (paramObject == null) {
      return "NULL";
    }
    switch (paramInt)
    {
    case -7: 
    case -6: 
    case -5: 
    case -1: 
    case 1: 
    case 2: 
    case 3: 
    case 4: 
    case 5: 
    case 6: 
    case 7: 
    case 8: 
    case 12: 
    case 16: 
    case 91: 
    case 92: 
    case 93: 
      return paramObject.toString();
    case 2005: 
      try
      {
        if ((paramObject instanceof Clob)) {
          paramObject = ((Clob)paramObject).getCharacterStream();
        }
        return IOUtils.readStringAndClose((Reader)paramObject, -1);
      }
      catch (IOException localIOException)
      {
        throw DbException.toSQLException(localIOException);
      }
    case -4: 
    case -3: 
    case -2: 
    case 0: 
    case 70: 
    case 1111: 
    case 2000: 
    case 2001: 
    case 2002: 
    case 2003: 
    case 2004: 
    case 2006: 
      throw throwException("Unsupported column data type: " + paramInt);
    }
    return "";
  }
  
  protected static SimpleResultSet createResultSet(boolean paramBoolean)
  {
    SimpleResultSet localSimpleResultSet = new SimpleResultSet();
    if (paramBoolean)
    {
      localSimpleResultSet.addColumn("SCHEMA", 12, 0, 0);
      localSimpleResultSet.addColumn("TABLE", 12, 0, 0);
      localSimpleResultSet.addColumn("COLUMNS", 2003, 0, 0);
      localSimpleResultSet.addColumn("KEYS", 2003, 0, 0);
    }
    else
    {
      localSimpleResultSet.addColumn("QUERY", 12, 0, 0);
    }
    localSimpleResultSet.addColumn("SCORE", 6, 0, 0);
    return localSimpleResultSet;
  }
  
  protected static Object[][] parseKey(Connection paramConnection, String paramString)
  {
    ArrayList localArrayList1 = New.arrayList();
    ArrayList localArrayList2 = New.arrayList();
    JdbcConnection localJdbcConnection = (JdbcConnection)paramConnection;
    Session localSession = (Session)localJdbcConnection.getSession();
    Parser localParser = new Parser(localSession);
    Expression localExpression = localParser.parseExpression(paramString);
    addColumnData(localArrayList1, localArrayList2, localExpression);
    Object[] arrayOfObject1 = new Object[localArrayList1.size()];
    localArrayList1.toArray(arrayOfObject1);
    Object[] arrayOfObject2 = new Object[localArrayList1.size()];
    localArrayList2.toArray(arrayOfObject2);
    Object[][] arrayOfObject = { arrayOfObject1, arrayOfObject2 };
    return arrayOfObject;
  }
  
  protected static String quoteSQL(Object paramObject, int paramInt)
    throws SQLException
  {
    if (paramObject == null) {
      return "NULL";
    }
    switch (paramInt)
    {
    case -7: 
    case -6: 
    case -5: 
    case 2: 
    case 3: 
    case 4: 
    case 5: 
    case 6: 
    case 7: 
    case 8: 
    case 16: 
      return paramObject.toString();
    case -1: 
    case 1: 
    case 12: 
    case 91: 
    case 92: 
    case 93: 
      return quoteString(paramObject.toString());
    case -4: 
    case -3: 
    case -2: 
      if ((paramObject instanceof UUID)) {
        return "'" + paramObject.toString() + "'";
      }
      return "'" + StringUtils.convertBytesToHex((byte[])paramObject) + "'";
    case 0: 
    case 70: 
    case 1111: 
    case 2000: 
    case 2001: 
    case 2002: 
    case 2003: 
    case 2004: 
    case 2005: 
    case 2006: 
      throw throwException("Unsupported key data type: " + paramInt);
    }
    return "";
  }
  
  protected static void removeAllTriggers(Connection paramConnection, String paramString)
    throws SQLException
  {
    Statement localStatement1 = paramConnection.createStatement();
    ResultSet localResultSet = localStatement1.executeQuery("SELECT * FROM INFORMATION_SCHEMA.TRIGGERS");
    Statement localStatement2 = paramConnection.createStatement();
    while (localResultSet.next())
    {
      String str1 = localResultSet.getString("TRIGGER_SCHEMA");
      String str2 = localResultSet.getString("TRIGGER_NAME");
      if (str2.startsWith(paramString))
      {
        str2 = StringUtils.quoteIdentifier(str1) + "." + StringUtils.quoteIdentifier(str2);
        
        localStatement2.execute("DROP TRIGGER " + str2);
      }
    }
  }
  
  protected static void setColumns(int[] paramArrayOfInt, ArrayList<String> paramArrayList1, ArrayList<String> paramArrayList2)
    throws SQLException
  {
    int i = 0;
    for (int j = paramArrayList1.size(); i < j; i++)
    {
      String str1 = (String)paramArrayList1.get(i);
      int k = -1;
      int m = paramArrayList2.size();
      for (int n = 0; (k == -1) && (n < m); n++)
      {
        String str2 = (String)paramArrayList2.get(n);
        if (str2.equals(str1)) {
          k = n;
        }
      }
      if (k < 0) {
        throw throwException("Column not found: " + str1);
      }
      paramArrayOfInt[i] = k;
    }
  }
  
  protected static ResultSet search(Connection paramConnection, String paramString, int paramInt1, int paramInt2, boolean paramBoolean)
    throws SQLException
  {
    SimpleResultSet localSimpleResultSet = createResultSet(paramBoolean);
    if (paramConnection.getMetaData().getURL().startsWith("jdbc:columnlist:")) {
      return localSimpleResultSet;
    }
    if ((paramString == null) || (paramString.trim().length() == 0)) {
      return localSimpleResultSet;
    }
    FullTextSettings localFullTextSettings = FullTextSettings.getInstance(paramConnection);
    if (!localFullTextSettings.isInitialized()) {
      init(paramConnection);
    }
    HashSet localHashSet1 = New.hashSet();
    addWords(localFullTextSettings, localHashSet1, paramString);
    HashSet localHashSet2 = null;HashSet localHashSet3 = null;
    HashMap localHashMap = localFullTextSettings.getWordList();
    
    PreparedStatement localPreparedStatement = localFullTextSettings.prepare(paramConnection, "SELECT ROWID FROM FT.MAP WHERE WORDID=?");
    for (Object localObject1 = localHashSet1.iterator(); ((Iterator)localObject1).hasNext();)
    {
      String str1 = (String)((Iterator)localObject1).next();
      localHashSet3 = localHashSet2;
      localHashSet2 = New.hashSet();
      localObject2 = (Integer)localHashMap.get(str1);
      if (localObject2 != null)
      {
        localPreparedStatement.setInt(1, ((Integer)localObject2).intValue());
        ResultSet localResultSet = localPreparedStatement.executeQuery();
        while (localResultSet.next())
        {
          localObject3 = Integer.valueOf(localResultSet.getInt(1));
          if ((localHashSet3 == null) || (localHashSet3.contains(localObject3))) {
            localHashSet2.add(localObject3);
          }
        }
      }
    }
    Object localObject3;
    if ((localHashSet2 == null) || (localHashSet2.size() == 0)) {
      return localSimpleResultSet;
    }
    localObject1 = localFullTextSettings.prepare(paramConnection, "SELECT KEY, INDEXID FROM FT.ROWS WHERE ID=?");
    int i = 0;
    for (Object localObject2 = localHashSet2.iterator(); ((Iterator)localObject2).hasNext();)
    {
      int j = ((Integer)((Iterator)localObject2).next()).intValue();
      ((PreparedStatement)localObject1).setInt(1, j);
      localObject3 = ((PreparedStatement)localObject1).executeQuery();
      if (((ResultSet)localObject3).next()) {
        if (paramInt2 > 0)
        {
          paramInt2--;
        }
        else
        {
          String str2 = ((ResultSet)localObject3).getString(1);
          int k = ((ResultSet)localObject3).getInt(2);
          IndexInfo localIndexInfo = localFullTextSettings.getIndexInfo(k);
          Object localObject4;
          if (paramBoolean)
          {
            localObject4 = parseKey(paramConnection, str2);
            localSimpleResultSet.addRow(new Object[] { localIndexInfo.schema, localIndexInfo.table, localObject4[0], localObject4[1], Double.valueOf(1.0D) });
          }
          else
          {
            localObject4 = StringUtils.quoteIdentifier(localIndexInfo.schema) + "." + StringUtils.quoteIdentifier(localIndexInfo.table) + " WHERE " + str2;
            
            localSimpleResultSet.addRow(new Object[] { localObject4, Double.valueOf(1.0D) });
          }
          i++;
          if ((paramInt1 > 0) && (i >= paramInt1)) {
            break;
          }
        }
      }
    }
    return localSimpleResultSet;
  }
  
  private static void addColumnData(ArrayList<String> paramArrayList1, ArrayList<String> paramArrayList2, Expression paramExpression)
  {
    Object localObject1;
    Object localObject2;
    Object localObject3;
    if ((paramExpression instanceof ConditionAndOr))
    {
      localObject1 = (ConditionAndOr)paramExpression;
      localObject2 = ((ConditionAndOr)localObject1).getExpression(true);
      localObject3 = ((ConditionAndOr)localObject1).getExpression(false);
      addColumnData(paramArrayList1, paramArrayList2, (Expression)localObject2);
      addColumnData(paramArrayList1, paramArrayList2, (Expression)localObject3);
    }
    else
    {
      localObject1 = (Comparison)paramExpression;
      localObject2 = (ExpressionColumn)((Comparison)localObject1).getExpression(true);
      localObject3 = (ValueExpression)((Comparison)localObject1).getExpression(false);
      String str = ((ExpressionColumn)localObject2).getColumnName();
      paramArrayList1.add(str);
      if (localObject3 == null) {
        paramArrayList2.add(null);
      } else {
        paramArrayList2.add(((ValueExpression)localObject3).getValue(null).getString());
      }
    }
  }
  
  protected static void addWords(FullTextSettings paramFullTextSettings, HashSet<String> paramHashSet, Reader paramReader)
  {
    StreamTokenizer localStreamTokenizer = new StreamTokenizer(paramReader);
    localStreamTokenizer.resetSyntax();
    localStreamTokenizer.wordChars(33, 255);
    char[] arrayOfChar1 = paramFullTextSettings.getWhitespaceChars().toCharArray();
    for (int m : arrayOfChar1) {
      localStreamTokenizer.whitespaceChars(m, m);
    }
    try
    {
      for (;;)
      {
        int i = localStreamTokenizer.nextToken();
        if (i == -1) {
          break;
        }
        if (i == -3)
        {
          String str = localStreamTokenizer.sval;
          str = paramFullTextSettings.convertWord(str);
          if (str != null) {
            paramHashSet.add(str);
          }
        }
      }
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, "Tokenizer error");
    }
  }
  
  protected static void addWords(FullTextSettings paramFullTextSettings, HashSet<String> paramHashSet, String paramString)
  {
    String str1 = paramFullTextSettings.getWhitespaceChars();
    StringTokenizer localStringTokenizer = new StringTokenizer(paramString, str1);
    while (localStringTokenizer.hasMoreTokens())
    {
      String str2 = localStringTokenizer.nextToken();
      str2 = paramFullTextSettings.convertWord(str2);
      if (str2 != null) {
        paramHashSet.add(str2);
      }
    }
  }
  
  protected static void createTrigger(Connection paramConnection, String paramString1, String paramString2)
    throws SQLException
  {
    createOrDropTrigger(paramConnection, paramString1, paramString2, true);
  }
  
  private static void createOrDropTrigger(Connection paramConnection, String paramString1, String paramString2, boolean paramBoolean)
    throws SQLException
  {
    Statement localStatement = paramConnection.createStatement();
    String str = StringUtils.quoteIdentifier(paramString1) + "." + StringUtils.quoteIdentifier(new StringBuilder().append("FT_").append(paramString2).toString());
    
    localStatement.execute("DROP TRIGGER IF EXISTS " + str);
    if (paramBoolean)
    {
      StringBuilder localStringBuilder = new StringBuilder("CREATE TRIGGER IF NOT EXISTS ");
      
      localStringBuilder.append(str).append(" AFTER INSERT, UPDATE, DELETE, ROLLBACK ON ").append(StringUtils.quoteIdentifier(paramString1)).append('.').append(StringUtils.quoteIdentifier(paramString2)).append(" FOR EACH ROW CALL \"").append(FullTextTrigger.class.getName()).append('"');
      
      localStatement.execute(localStringBuilder.toString());
    }
  }
  
  protected static void indexExistingRows(Connection paramConnection, String paramString1, String paramString2)
    throws SQLException
  {
    FullTextTrigger localFullTextTrigger = new FullTextTrigger();
    localFullTextTrigger.init(paramConnection, paramString1, null, paramString2, false, 1);
    String str = "SELECT * FROM " + StringUtils.quoteIdentifier(paramString1) + "." + StringUtils.quoteIdentifier(paramString2);
    
    ResultSet localResultSet = paramConnection.createStatement().executeQuery(str);
    int i = localResultSet.getMetaData().getColumnCount();
    while (localResultSet.next())
    {
      Object[] arrayOfObject = new Object[i];
      for (int j = 0; j < i; j++) {
        arrayOfObject[j] = localResultSet.getObject(j + 1);
      }
      localFullTextTrigger.fire(paramConnection, null, arrayOfObject);
    }
  }
  
  private static String quoteString(String paramString)
  {
    if (paramString.indexOf('\'') < 0) {
      return "'" + paramString + "'";
    }
    int i = paramString.length();
    StringBuilder localStringBuilder = new StringBuilder(i + 2);
    localStringBuilder.append('\'');
    for (int j = 0; j < i; j++)
    {
      char c = paramString.charAt(j);
      if (c == '\'') {
        localStringBuilder.append(c);
      }
      localStringBuilder.append(c);
    }
    localStringBuilder.append('\'');
    return localStringBuilder.toString();
  }
  
  private static void setIgnoreList(FullTextSettings paramFullTextSettings, String paramString)
  {
    String[] arrayOfString1 = StringUtils.arraySplit(paramString, ',', true);
    HashSet localHashSet = paramFullTextSettings.getIgnoreList();
    for (String str1 : arrayOfString1)
    {
      String str2 = paramFullTextSettings.convertWord(str1);
      if (str2 != null) {
        localHashSet.add(str2);
      }
    }
  }
  
  protected static boolean hasChanged(Object[] paramArrayOfObject1, Object[] paramArrayOfObject2, int[] paramArrayOfInt)
  {
    for (int k : paramArrayOfInt)
    {
      Object localObject1 = paramArrayOfObject1[k];Object localObject2 = paramArrayOfObject2[k];
      if (localObject1 == null)
      {
        if (localObject2 != null) {
          return true;
        }
      }
      else if (!localObject1.equals(localObject2)) {
        return true;
      }
    }
    return false;
  }
  
  public static void closeAll() {}
  
  public static class FullTextTrigger
    implements Trigger
  {
    protected FullTextSettings setting;
    protected IndexInfo index;
    protected int[] columnTypes;
    protected PreparedStatement prepInsertWord;
    protected PreparedStatement prepInsertRow;
    protected PreparedStatement prepInsertMap;
    protected PreparedStatement prepDeleteRow;
    protected PreparedStatement prepDeleteMap;
    protected PreparedStatement prepSelectRow;
    
    public void init(Connection paramConnection, String paramString1, String paramString2, String paramString3, boolean paramBoolean, int paramInt)
      throws SQLException
    {
      this.setting = FullTextSettings.getInstance(paramConnection);
      if (!this.setting.isInitialized()) {
        FullText.init(paramConnection);
      }
      ArrayList localArrayList1 = New.arrayList();
      DatabaseMetaData localDatabaseMetaData = paramConnection.getMetaData();
      ResultSet localResultSet = localDatabaseMetaData.getColumns(null, StringUtils.escapeMetaDataPattern(paramString1), StringUtils.escapeMetaDataPattern(paramString3), null);
      
      ArrayList localArrayList2 = New.arrayList();
      while (localResultSet.next()) {
        localArrayList2.add(localResultSet.getString("COLUMN_NAME"));
      }
      this.columnTypes = new int[localArrayList2.size()];
      this.index = new IndexInfo();
      this.index.schema = paramString1;
      this.index.table = paramString3;
      this.index.columns = new String[localArrayList2.size()];
      localArrayList2.toArray(this.index.columns);
      localResultSet = localDatabaseMetaData.getColumns(null, StringUtils.escapeMetaDataPattern(paramString1), StringUtils.escapeMetaDataPattern(paramString3), null);
      for (int i = 0; localResultSet.next(); i++) {
        this.columnTypes[i] = localResultSet.getInt("DATA_TYPE");
      }
      if (localArrayList1.size() == 0)
      {
        localResultSet = localDatabaseMetaData.getPrimaryKeys(null, StringUtils.escapeMetaDataPattern(paramString1), paramString3);
        while (localResultSet.next()) {
          localArrayList1.add(localResultSet.getString("COLUMN_NAME"));
        }
      }
      if (localArrayList1.size() == 0) {
        throw FullText.throwException("No primary key for table " + paramString3);
      }
      ArrayList localArrayList3 = New.arrayList();
      PreparedStatement localPreparedStatement = paramConnection.prepareStatement("SELECT ID, COLUMNS FROM FT.INDEXES WHERE SCHEMA=? AND TABLE=?");
      
      localPreparedStatement.setString(1, paramString1);
      localPreparedStatement.setString(2, paramString3);
      localResultSet = localPreparedStatement.executeQuery();
      if (localResultSet.next())
      {
        this.index.id = localResultSet.getInt(1);
        String str1 = localResultSet.getString(2);
        if (str1 != null) {
          for (String str2 : StringUtils.arraySplit(str1, ',', true)) {
            localArrayList3.add(str2);
          }
        }
      }
      if (localArrayList3.size() == 0) {
        localArrayList3.addAll(localArrayList2);
      }
      this.index.keys = new int[localArrayList1.size()];
      FullText.setColumns(this.index.keys, localArrayList1, localArrayList2);
      this.index.indexColumns = new int[localArrayList3.size()];
      FullText.setColumns(this.index.indexColumns, localArrayList3, localArrayList2);
      this.setting.addIndexInfo(this.index);
      this.prepInsertWord = paramConnection.prepareStatement("INSERT INTO FT.WORDS(NAME) VALUES(?)");
      
      this.prepInsertRow = paramConnection.prepareStatement("INSERT INTO FT.ROWS(HASH, INDEXID, KEY) VALUES(?, ?, ?)");
      
      this.prepInsertMap = paramConnection.prepareStatement("INSERT INTO FT.MAP(ROWID, WORDID) VALUES(?, ?)");
      
      this.prepDeleteRow = paramConnection.prepareStatement("DELETE FROM FT.ROWS WHERE HASH=? AND INDEXID=? AND KEY=?");
      
      this.prepDeleteMap = paramConnection.prepareStatement("DELETE FROM FT.MAP WHERE ROWID=? AND WORDID=?");
      
      this.prepSelectRow = paramConnection.prepareStatement("SELECT ID FROM FT.ROWS WHERE HASH=? AND INDEXID=? AND KEY=?");
    }
    
    public void fire(Connection paramConnection, Object[] paramArrayOfObject1, Object[] paramArrayOfObject2)
      throws SQLException
    {
      if (paramArrayOfObject1 != null)
      {
        if (paramArrayOfObject2 != null)
        {
          if (FullText.hasChanged(paramArrayOfObject1, paramArrayOfObject2, this.index.indexColumns))
          {
            delete(paramArrayOfObject1);
            insert(paramArrayOfObject2);
          }
        }
        else {
          delete(paramArrayOfObject1);
        }
      }
      else if (paramArrayOfObject2 != null) {
        insert(paramArrayOfObject2);
      }
    }
    
    public void close()
    {
      this.setting.removeIndexInfo(this.index);
    }
    
    public void remove()
    {
      this.setting.removeIndexInfo(this.index);
    }
    
    protected void insert(Object[] paramArrayOfObject)
      throws SQLException
    {
      String str = getKey(paramArrayOfObject);
      int i = str.hashCode();
      this.prepInsertRow.setInt(1, i);
      this.prepInsertRow.setInt(2, this.index.id);
      this.prepInsertRow.setString(3, str);
      this.prepInsertRow.execute();
      ResultSet localResultSet = this.prepInsertRow.getGeneratedKeys();
      localResultSet.next();
      int j = localResultSet.getInt(1);
      this.prepInsertMap.setInt(1, j);
      int[] arrayOfInt1 = getWordIds(paramArrayOfObject);
      for (int n : arrayOfInt1)
      {
        this.prepInsertMap.setInt(2, n);
        this.prepInsertMap.execute();
      }
    }
    
    protected void delete(Object[] paramArrayOfObject)
      throws SQLException
    {
      String str = getKey(paramArrayOfObject);
      int i = str.hashCode();
      this.prepSelectRow.setInt(1, i);
      this.prepSelectRow.setInt(2, this.index.id);
      this.prepSelectRow.setString(3, str);
      ResultSet localResultSet = this.prepSelectRow.executeQuery();
      if (localResultSet.next())
      {
        int j = localResultSet.getInt(1);
        this.prepDeleteMap.setInt(1, j);
        int[] arrayOfInt1 = getWordIds(paramArrayOfObject);
        for (int n : arrayOfInt1)
        {
          this.prepDeleteMap.setInt(2, n);
          this.prepDeleteMap.executeUpdate();
        }
        this.prepDeleteRow.setInt(1, i);
        this.prepDeleteRow.setInt(2, this.index.id);
        this.prepDeleteRow.setString(3, str);
        this.prepDeleteRow.executeUpdate();
      }
    }
    
    private int[] getWordIds(Object[] paramArrayOfObject)
      throws SQLException
    {
      HashSet localHashSet = New.hashSet();
      Object localObject2;
      for (k : this.index.indexColumns)
      {
        int m = this.columnTypes[k];
        localObject2 = paramArrayOfObject[k];
        Object localObject3;
        if ((m == 2005) && (localObject2 != null))
        {
          if ((localObject2 instanceof Reader)) {
            localObject3 = (Reader)localObject2;
          } else {
            localObject3 = ((Clob)localObject2).getCharacterStream();
          }
          FullText.addWords(this.setting, localHashSet, (Reader)localObject3);
        }
        else
        {
          localObject3 = FullText.asString(localObject2, m);
          FullText.addWords(this.setting, localHashSet, (String)localObject3);
        }
      }
      ??? = this.setting.getWordList();
      int[] arrayOfInt = new int[localHashSet.size()];
      Iterator localIterator = localHashSet.iterator();
      for (int k = 0; localIterator.hasNext(); k++)
      {
        String str = (String)localIterator.next();
        localObject2 = (Integer)((HashMap)???).get(str);
        int n;
        if (localObject2 == null)
        {
          this.prepInsertWord.setString(1, str);
          this.prepInsertWord.execute();
          ResultSet localResultSet = this.prepInsertWord.getGeneratedKeys();
          localResultSet.next();
          n = localResultSet.getInt(1);
          ((HashMap)???).put(str, Integer.valueOf(n));
        }
        else
        {
          n = ((Integer)localObject2).intValue();
        }
        arrayOfInt[k] = n;
      }
      Arrays.sort(arrayOfInt);
      return arrayOfInt;
    }
    
    private String getKey(Object[] paramArrayOfObject)
      throws SQLException
    {
      StatementBuilder localStatementBuilder = new StatementBuilder();
      for (int k : this.index.keys)
      {
        localStatementBuilder.appendExceptFirst(" AND ");
        localStatementBuilder.append(StringUtils.quoteIdentifier(this.index.columns[k]));
        Object localObject = paramArrayOfObject[k];
        if (localObject == null) {
          localStatementBuilder.append(" IS NULL");
        } else {
          localStatementBuilder.append('=').append(FullText.quoteSQL(localObject, this.columnTypes[k]));
        }
      }
      return localStatementBuilder.toString();
    }
  }
  
  protected static SQLException throwException(String paramString)
    throws SQLException
  {
    throw new SQLException(paramString, "FULLTEXT");
  }
}
