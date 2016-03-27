package org.h2.bnf;

import java.util.HashMap;
import java.util.HashSet;
import org.h2.bnf.context.DbSchema;
import org.h2.bnf.context.DbTableOrView;
import org.h2.util.New;
import org.h2.util.StringUtils;

public class Sentence
{
  public static final int CONTEXT = 0;
  public static final int KEYWORD = 1;
  public static final int FUNCTION = 2;
  private static final long MAX_PROCESSING_TIME = 100L;
  private final HashMap<String, String> next = New.hashMap();
  private String query;
  private String queryUpper;
  private long stopAt;
  private DbSchema lastMatchedSchema;
  private DbTableOrView lastMatchedTable;
  private DbTableOrView lastTable;
  private HashSet<DbTableOrView> tables;
  private HashMap<String, DbTableOrView> aliases;
  
  public void start()
  {
    this.stopAt = (System.currentTimeMillis() + 100L);
  }
  
  public void stopIfRequired()
  {
    if (System.currentTimeMillis() > this.stopAt) {
      throw new IllegalStateException();
    }
  }
  
  public void add(String paramString1, String paramString2, int paramInt)
  {
    this.next.put(paramInt + "#" + paramString1, paramString2);
  }
  
  public void addAlias(String paramString, DbTableOrView paramDbTableOrView)
  {
    if (this.aliases == null) {
      this.aliases = New.hashMap();
    }
    this.aliases.put(paramString, paramDbTableOrView);
  }
  
  public void addTable(DbTableOrView paramDbTableOrView)
  {
    this.lastTable = paramDbTableOrView;
    if (this.tables == null) {
      this.tables = New.hashSet();
    }
    this.tables.add(paramDbTableOrView);
  }
  
  public HashSet<DbTableOrView> getTables()
  {
    return this.tables;
  }
  
  public HashMap<String, DbTableOrView> getAliases()
  {
    return this.aliases;
  }
  
  public DbTableOrView getLastTable()
  {
    return this.lastTable;
  }
  
  public DbSchema getLastMatchedSchema()
  {
    return this.lastMatchedSchema;
  }
  
  public void setLastMatchedSchema(DbSchema paramDbSchema)
  {
    this.lastMatchedSchema = paramDbSchema;
  }
  
  public void setLastMatchedTable(DbTableOrView paramDbTableOrView)
  {
    this.lastMatchedTable = paramDbTableOrView;
  }
  
  public DbTableOrView getLastMatchedTable()
  {
    return this.lastMatchedTable;
  }
  
  public void setQuery(String paramString)
  {
    if (!StringUtils.equals(this.query, paramString))
    {
      this.query = paramString;
      this.queryUpper = StringUtils.toUpperEnglish(paramString);
    }
  }
  
  public String getQuery()
  {
    return this.query;
  }
  
  public String getQueryUpper()
  {
    return this.queryUpper;
  }
  
  public HashMap<String, String> getNext()
  {
    return this.next;
  }
}
