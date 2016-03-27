package org.h2.fulltext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import org.h2.util.New;
import org.h2.util.SoftHashMap;

class FullTextSettings
{
  private static final HashMap<String, FullTextSettings> SETTINGS = ;
  private boolean initialized;
  private final HashSet<String> ignoreList = New.hashSet();
  private final HashMap<String, Integer> words = New.hashMap();
  private final HashMap<Integer, IndexInfo> indexes = New.hashMap();
  private final SoftHashMap<Connection, SoftHashMap<String, PreparedStatement>> cache = new SoftHashMap();
  private String whitespaceChars = " \t\n\r\f+\"*%&/()=?'!,.;:-_#@|^~`{}[]<>\\";
  
  protected HashSet<String> getIgnoreList()
  {
    return this.ignoreList;
  }
  
  protected HashMap<String, Integer> getWordList()
  {
    return this.words;
  }
  
  protected IndexInfo getIndexInfo(int paramInt)
  {
    return (IndexInfo)this.indexes.get(Integer.valueOf(paramInt));
  }
  
  protected void addIndexInfo(IndexInfo paramIndexInfo)
  {
    this.indexes.put(Integer.valueOf(paramIndexInfo.id), paramIndexInfo);
  }
  
  protected String convertWord(String paramString)
  {
    paramString = paramString.toUpperCase();
    if (this.ignoreList.contains(paramString)) {
      return null;
    }
    return paramString;
  }
  
  protected static FullTextSettings getInstance(Connection paramConnection)
    throws SQLException
  {
    String str = getIndexPath(paramConnection);
    FullTextSettings localFullTextSettings = (FullTextSettings)SETTINGS.get(str);
    if (localFullTextSettings == null)
    {
      localFullTextSettings = new FullTextSettings();
      SETTINGS.put(str, localFullTextSettings);
    }
    return localFullTextSettings;
  }
  
  protected static String getIndexPath(Connection paramConnection)
    throws SQLException
  {
    Statement localStatement = paramConnection.createStatement();
    ResultSet localResultSet = localStatement.executeQuery("CALL IFNULL(DATABASE_PATH(), 'MEM:' || DATABASE())");
    
    localResultSet.next();
    String str = localResultSet.getString(1);
    if ("MEM:UNNAMED".equals(str)) {
      throw FullText.throwException("Fulltext search for private (unnamed) in-memory databases is not supported.");
    }
    localResultSet.close();
    return str;
  }
  
  protected synchronized PreparedStatement prepare(Connection paramConnection, String paramString)
    throws SQLException
  {
    SoftHashMap localSoftHashMap = (SoftHashMap)this.cache.get(paramConnection);
    if (localSoftHashMap == null)
    {
      localSoftHashMap = new SoftHashMap();
      this.cache.put(paramConnection, localSoftHashMap);
    }
    PreparedStatement localPreparedStatement = (PreparedStatement)localSoftHashMap.get(paramString);
    if ((localPreparedStatement != null) && (localPreparedStatement.getConnection().isClosed())) {
      localPreparedStatement = null;
    }
    if (localPreparedStatement == null)
    {
      localPreparedStatement = paramConnection.prepareStatement(paramString);
      localSoftHashMap.put(paramString, localPreparedStatement);
    }
    return localPreparedStatement;
  }
  
  protected void removeAllIndexes()
  {
    this.indexes.clear();
  }
  
  protected void removeIndexInfo(IndexInfo paramIndexInfo)
  {
    this.indexes.remove(Integer.valueOf(paramIndexInfo.id));
  }
  
  protected void setInitialized(boolean paramBoolean)
  {
    this.initialized = paramBoolean;
  }
  
  protected boolean isInitialized()
  {
    return this.initialized;
  }
  
  protected static void closeAll()
  {
    SETTINGS.clear();
  }
  
  protected void setWhitespaceChars(String paramString)
  {
    this.whitespaceChars = paramString;
  }
  
  protected String getWhitespaceChars()
  {
    return this.whitespaceChars;
  }
}
