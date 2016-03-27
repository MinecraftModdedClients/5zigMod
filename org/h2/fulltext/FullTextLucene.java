package org.h2.fulltext;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.h2.api.Trigger;
import org.h2.command.Parser;
import org.h2.engine.Session;
import org.h2.expression.ExpressionColumn;
import org.h2.jdbc.JdbcConnection;
import org.h2.store.fs.FileUtils;
import org.h2.tools.SimpleResultSet;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class FullTextLucene
  extends FullText
{
  protected static final boolean STORE_DOCUMENT_TEXT_IN_INDEX = Utils.getProperty("h2.storeDocumentTextInIndex", false);
  private static final HashMap<String, IndexAccess> INDEX_ACCESS = New.hashMap();
  private static final String TRIGGER_PREFIX = "FTL_";
  private static final String SCHEMA = "FTL";
  private static final String LUCENE_FIELD_DATA = "_DATA";
  private static final String LUCENE_FIELD_QUERY = "_QUERY";
  private static final String LUCENE_FIELD_MODIFIED = "_modified";
  private static final String LUCENE_FIELD_COLUMN_PREFIX = "_";
  private static final String IN_MEMORY_PREFIX = "mem:";
  
  public static void init(Connection paramConnection)
    throws SQLException
  {
    Statement localStatement = paramConnection.createStatement();
    localStatement.execute("CREATE SCHEMA IF NOT EXISTS FTL");
    localStatement.execute("CREATE TABLE IF NOT EXISTS FTL.INDEXES(SCHEMA VARCHAR, TABLE VARCHAR, COLUMNS VARCHAR, PRIMARY KEY(SCHEMA, TABLE))");
    
    localStatement.execute("CREATE ALIAS IF NOT EXISTS FTL_CREATE_INDEX FOR \"" + FullTextLucene.class.getName() + ".createIndex\"");
    
    localStatement.execute("CREATE ALIAS IF NOT EXISTS FTL_DROP_INDEX FOR \"" + FullTextLucene.class.getName() + ".dropIndex\"");
    
    localStatement.execute("CREATE ALIAS IF NOT EXISTS FTL_SEARCH FOR \"" + FullTextLucene.class.getName() + ".search\"");
    
    localStatement.execute("CREATE ALIAS IF NOT EXISTS FTL_SEARCH_DATA FOR \"" + FullTextLucene.class.getName() + ".searchData\"");
    
    localStatement.execute("CREATE ALIAS IF NOT EXISTS FTL_REINDEX FOR \"" + FullTextLucene.class.getName() + ".reindex\"");
    
    localStatement.execute("CREATE ALIAS IF NOT EXISTS FTL_DROP_ALL FOR \"" + FullTextLucene.class.getName() + ".dropAll\"");
    try
    {
      getIndexAccess(paramConnection);
    }
    catch (SQLException localSQLException)
    {
      throw convertException(localSQLException);
    }
  }
  
  public static void createIndex(Connection paramConnection, String paramString1, String paramString2, String paramString3)
    throws SQLException
  {
    init(paramConnection);
    PreparedStatement localPreparedStatement = paramConnection.prepareStatement("INSERT INTO FTL.INDEXES(SCHEMA, TABLE, COLUMNS) VALUES(?, ?, ?)");
    
    localPreparedStatement.setString(1, paramString1);
    localPreparedStatement.setString(2, paramString2);
    localPreparedStatement.setString(3, paramString3);
    localPreparedStatement.execute();
    createTrigger(paramConnection, paramString1, paramString2);
    indexExistingRows(paramConnection, paramString1, paramString2);
  }
  
  public static void dropIndex(Connection paramConnection, String paramString1, String paramString2)
    throws SQLException
  {
    init(paramConnection);
    
    PreparedStatement localPreparedStatement = paramConnection.prepareStatement("DELETE FROM FTL.INDEXES WHERE SCHEMA=? AND TABLE=?");
    
    localPreparedStatement.setString(1, paramString1);
    localPreparedStatement.setString(2, paramString2);
    int i = localPreparedStatement.executeUpdate();
    if (i == 0) {
      return;
    }
    reindex(paramConnection);
  }
  
  public static void reindex(Connection paramConnection)
    throws SQLException
  {
    init(paramConnection);
    removeAllTriggers(paramConnection, "FTL_");
    removeIndexFiles(paramConnection);
    Statement localStatement = paramConnection.createStatement();
    ResultSet localResultSet = localStatement.executeQuery("SELECT * FROM FTL.INDEXES");
    while (localResultSet.next())
    {
      String str1 = localResultSet.getString("SCHEMA");
      String str2 = localResultSet.getString("TABLE");
      createTrigger(paramConnection, str1, str2);
      indexExistingRows(paramConnection, str1, str2);
    }
  }
  
  public static void dropAll(Connection paramConnection)
    throws SQLException
  {
    Statement localStatement = paramConnection.createStatement();
    localStatement.execute("DROP SCHEMA IF EXISTS FTL");
    removeAllTriggers(paramConnection, "FTL_");
    removeIndexFiles(paramConnection);
  }
  
  public static ResultSet search(Connection paramConnection, String paramString, int paramInt1, int paramInt2)
    throws SQLException
  {
    return search(paramConnection, paramString, paramInt1, paramInt2, false);
  }
  
  public static ResultSet searchData(Connection paramConnection, String paramString, int paramInt1, int paramInt2)
    throws SQLException
  {
    return search(paramConnection, paramString, paramInt1, paramInt2, true);
  }
  
  protected static SQLException convertException(Exception paramException)
  {
    SQLException localSQLException = new SQLException("Error while indexing document", "FULLTEXT");
    
    localSQLException.initCause(paramException);
    return localSQLException;
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
    String str = StringUtils.quoteIdentifier(paramString1) + "." + StringUtils.quoteIdentifier(new StringBuilder().append("FTL_").append(paramString2).toString());
    
    localStatement.execute("DROP TRIGGER IF EXISTS " + str);
    if (paramBoolean)
    {
      StringBuilder localStringBuilder = new StringBuilder("CREATE TRIGGER IF NOT EXISTS ");
      
      localStringBuilder.append(str).append(" AFTER INSERT, UPDATE, DELETE, ROLLBACK ON ").append(StringUtils.quoteIdentifier(paramString1)).append('.').append(StringUtils.quoteIdentifier(paramString2)).append(" FOR EACH ROW CALL \"").append(FullTextTrigger.class.getName()).append('"');
      
      localStatement.execute(localStringBuilder.toString());
    }
  }
  
  protected static IndexAccess getIndexAccess(Connection paramConnection)
    throws SQLException
  {
    String str = getIndexPath(paramConnection);
    synchronized (INDEX_ACCESS)
    {
      IndexAccess localIndexAccess = (IndexAccess)INDEX_ACCESS.get(str);
      if (localIndexAccess == null)
      {
        try
        {
          FSDirectory localFSDirectory = str.startsWith("mem:") ? new RAMDirectory() : FSDirectory.open(new File(str));
          
          boolean bool = !IndexReader.indexExists(localFSDirectory);
          StandardAnalyzer localStandardAnalyzer = new StandardAnalyzer(Version.LUCENE_30);
          IndexWriter localIndexWriter = new IndexWriter(localFSDirectory, localStandardAnalyzer, bool, IndexWriter.MaxFieldLength.UNLIMITED);
          
          IndexReader localIndexReader = localIndexWriter.getReader();
          localIndexAccess = new IndexAccess();
          localIndexAccess.writer = localIndexWriter;
          localIndexAccess.reader = localIndexReader;
          localIndexAccess.searcher = new IndexSearcher(localIndexReader);
        }
        catch (IOException localIOException)
        {
          throw convertException(localIOException);
        }
        INDEX_ACCESS.put(str, localIndexAccess);
      }
      return localIndexAccess;
    }
  }
  
  protected static String getIndexPath(Connection paramConnection)
    throws SQLException
  {
    Statement localStatement = paramConnection.createStatement();
    ResultSet localResultSet = localStatement.executeQuery("CALL DATABASE_PATH()");
    localResultSet.next();
    String str = localResultSet.getString(1);
    if (str == null) {
      return "mem:" + paramConnection.getCatalog();
    }
    int i = str.lastIndexOf(':');
    if (i > 1) {
      str = str.substring(i + 1);
    }
    localResultSet.close();
    return str;
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
      localFullTextTrigger.insert(arrayOfObject, false);
    }
    localFullTextTrigger.commitIndex();
  }
  
  private static void removeIndexFiles(Connection paramConnection)
    throws SQLException
  {
    String str = getIndexPath(paramConnection);
    IndexAccess localIndexAccess = (IndexAccess)INDEX_ACCESS.get(str);
    if (localIndexAccess != null) {
      removeIndexAccess(localIndexAccess, str);
    }
    if (!str.startsWith("mem:")) {
      FileUtils.deleteRecursive(str, false);
    }
  }
  
  protected static void removeIndexAccess(IndexAccess paramIndexAccess, String paramString)
    throws SQLException
  {
    synchronized (INDEX_ACCESS)
    {
      try
      {
        INDEX_ACCESS.remove(paramString);
        paramIndexAccess.searcher.close();
        paramIndexAccess.reader.close();
        paramIndexAccess.writer.close();
      }
      catch (Exception localException)
      {
        throw convertException(localException);
      }
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
    try
    {
      IndexAccess localIndexAccess = getIndexAccess(paramConnection);
      
      Searcher localSearcher = localIndexAccess.searcher;
      
      Analyzer localAnalyzer = localIndexAccess.writer.getAnalyzer();
      QueryParser localQueryParser = new QueryParser(Version.LUCENE_30, "_DATA", localAnalyzer);
      
      Query localQuery = localQueryParser.parse(paramString);
      
      int i = (paramInt1 == 0 ? 100 : paramInt1) + paramInt2;
      TopDocs localTopDocs = localSearcher.search(localQuery, i);
      if (paramInt1 == 0) {
        paramInt1 = localTopDocs.totalHits;
      }
      int j = 0;int k = localTopDocs.scoreDocs.length;
      for (; (j < paramInt1) && (j + paramInt2 < localTopDocs.totalHits) && (j + paramInt2 < k); j++)
      {
        ScoreDoc localScoreDoc = localTopDocs.scoreDocs[(j + paramInt2)];
        Document localDocument = localSearcher.doc(localScoreDoc.doc);
        float f = localScoreDoc.score;
        String str1 = localDocument.get("_QUERY");
        if (paramBoolean)
        {
          int m = str1.indexOf(" WHERE ");
          JdbcConnection localJdbcConnection = (JdbcConnection)paramConnection;
          Session localSession = (Session)localJdbcConnection.getSession();
          Parser localParser = new Parser(localSession);
          String str2 = str1.substring(0, m);
          ExpressionColumn localExpressionColumn = (ExpressionColumn)localParser.parseExpression(str2);
          String str3 = localExpressionColumn.getOriginalTableAliasName();
          String str4 = localExpressionColumn.getColumnName();
          str1 = str1.substring(m + " WHERE ".length());
          Object[][] arrayOfObject = parseKey(paramConnection, str1);
          localSimpleResultSet.addRow(new Object[] { str3, str4, arrayOfObject[0], arrayOfObject[1], Float.valueOf(f) });
        }
        else
        {
          localSimpleResultSet.addRow(new Object[] { str1, Float.valueOf(f) });
        }
      }
    }
    catch (Exception localException)
    {
      throw convertException(localException);
    }
    return localSimpleResultSet;
  }
  
  static class IndexAccess
  {
    IndexWriter writer;
    IndexReader reader;
    Searcher searcher;
  }
  
  public static class FullTextTrigger
    implements Trigger
  {
    protected String schema;
    protected String table;
    protected int[] keys;
    protected int[] indexColumns;
    protected String[] columns;
    protected int[] columnTypes;
    protected String indexPath;
    protected FullTextLucene.IndexAccess indexAccess;
    
    public void init(Connection paramConnection, String paramString1, String paramString2, String paramString3, boolean paramBoolean, int paramInt)
      throws SQLException
    {
      this.schema = paramString1;
      this.table = paramString3;
      this.indexPath = FullTextLucene.getIndexPath(paramConnection);
      this.indexAccess = FullTextLucene.getIndexAccess(paramConnection);
      ArrayList localArrayList1 = New.arrayList();
      DatabaseMetaData localDatabaseMetaData = paramConnection.getMetaData();
      ResultSet localResultSet = localDatabaseMetaData.getColumns(null, StringUtils.escapeMetaDataPattern(paramString1), StringUtils.escapeMetaDataPattern(paramString3), null);
      
      ArrayList localArrayList2 = New.arrayList();
      while (localResultSet.next()) {
        localArrayList2.add(localResultSet.getString("COLUMN_NAME"));
      }
      this.columnTypes = new int[localArrayList2.size()];
      this.columns = new String[localArrayList2.size()];
      localArrayList2.toArray(this.columns);
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
      PreparedStatement localPreparedStatement = paramConnection.prepareStatement("SELECT COLUMNS FROM FTL.INDEXES WHERE SCHEMA=? AND TABLE=?");
      
      localPreparedStatement.setString(1, paramString1);
      localPreparedStatement.setString(2, paramString3);
      localResultSet = localPreparedStatement.executeQuery();
      if (localResultSet.next())
      {
        String str1 = localResultSet.getString(1);
        if (str1 != null) {
          for (String str2 : StringUtils.arraySplit(str1, ',', true)) {
            localArrayList3.add(str2);
          }
        }
      }
      if (localArrayList3.size() == 0) {
        localArrayList3.addAll(localArrayList2);
      }
      this.keys = new int[localArrayList1.size()];
      FullText.setColumns(this.keys, localArrayList1, localArrayList2);
      this.indexColumns = new int[localArrayList3.size()];
      FullText.setColumns(this.indexColumns, localArrayList3, localArrayList2);
    }
    
    public void fire(Connection paramConnection, Object[] paramArrayOfObject1, Object[] paramArrayOfObject2)
      throws SQLException
    {
      if (paramArrayOfObject1 != null)
      {
        if (paramArrayOfObject2 != null)
        {
          if (FullText.hasChanged(paramArrayOfObject1, paramArrayOfObject2, this.indexColumns))
          {
            delete(paramArrayOfObject1, false);
            insert(paramArrayOfObject2, true);
          }
        }
        else {
          delete(paramArrayOfObject1, true);
        }
      }
      else if (paramArrayOfObject2 != null) {
        insert(paramArrayOfObject2, true);
      }
    }
    
    public void close()
      throws SQLException
    {
      if (this.indexAccess != null)
      {
        FullTextLucene.removeIndexAccess(this.indexAccess, this.indexPath);
        this.indexAccess = null;
      }
    }
    
    public void remove() {}
    
    void commitIndex()
      throws SQLException
    {
      try
      {
        this.indexAccess.writer.commit();
        
        this.indexAccess.searcher.close();
        this.indexAccess.reader.close();
        IndexReader localIndexReader = this.indexAccess.writer.getReader();
        this.indexAccess.reader = localIndexReader;
        this.indexAccess.searcher = new IndexSearcher(localIndexReader);
      }
      catch (IOException localIOException)
      {
        throw FullTextLucene.convertException(localIOException);
      }
    }
    
    protected void insert(Object[] paramArrayOfObject, boolean paramBoolean)
      throws SQLException
    {
      String str1 = getQuery(paramArrayOfObject);
      Document localDocument = new Document();
      localDocument.add(new Field("_QUERY", str1, Field.Store.YES, Field.Index.NOT_ANALYZED));
      
      long l = System.currentTimeMillis();
      localDocument.add(new Field("_modified", DateTools.timeToString(l, DateTools.Resolution.SECOND), Field.Store.YES, Field.Index.NOT_ANALYZED));
      
      StatementBuilder localStatementBuilder = new StatementBuilder();
      for (int k : this.indexColumns)
      {
        String str2 = this.columns[k];
        String str3 = FullText.asString(paramArrayOfObject[k], this.columnTypes[k]);
        if (str2.startsWith("_")) {
          str2 = "_" + str2;
        }
        localDocument.add(new Field(str2, str3, Field.Store.NO, Field.Index.ANALYZED));
        
        localStatementBuilder.appendExceptFirst(" ");
        localStatementBuilder.append(str3);
      }
      ??? = FullTextLucene.STORE_DOCUMENT_TEXT_IN_INDEX ? Field.Store.YES : Field.Store.NO;
      
      localDocument.add(new Field("_DATA", localStatementBuilder.toString(), (Field.Store)???, Field.Index.ANALYZED));
      try
      {
        this.indexAccess.writer.addDocument(localDocument);
        if (paramBoolean) {
          commitIndex();
        }
      }
      catch (IOException localIOException)
      {
        throw FullTextLucene.convertException(localIOException);
      }
    }
    
    protected void delete(Object[] paramArrayOfObject, boolean paramBoolean)
      throws SQLException
    {
      String str = getQuery(paramArrayOfObject);
      try
      {
        Term localTerm = new Term("_QUERY", str);
        this.indexAccess.writer.deleteDocuments(localTerm);
        if (paramBoolean) {
          commitIndex();
        }
      }
      catch (IOException localIOException)
      {
        throw FullTextLucene.convertException(localIOException);
      }
    }
    
    private String getQuery(Object[] paramArrayOfObject)
      throws SQLException
    {
      StatementBuilder localStatementBuilder = new StatementBuilder();
      if (this.schema != null) {
        localStatementBuilder.append(StringUtils.quoteIdentifier(this.schema)).append('.');
      }
      localStatementBuilder.append(StringUtils.quoteIdentifier(this.table)).append(" WHERE ");
      for (int k : this.keys)
      {
        localStatementBuilder.appendExceptFirst(" AND ");
        localStatementBuilder.append(StringUtils.quoteIdentifier(this.columns[k]));
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
}
