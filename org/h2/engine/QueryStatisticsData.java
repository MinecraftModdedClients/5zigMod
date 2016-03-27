package org.h2.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class QueryStatisticsData
{
  private static final int MAX_QUERY_ENTRIES = 100;
  private static final Comparator<QueryEntry> QUERY_ENTRY_COMPARATOR = new Comparator()
  {
    public int compare(QueryStatisticsData.QueryEntry paramAnonymousQueryEntry1, QueryStatisticsData.QueryEntry paramAnonymousQueryEntry2)
    {
      return (int)Math.signum((float)(paramAnonymousQueryEntry1.lastUpdateTime - paramAnonymousQueryEntry2.lastUpdateTime));
    }
  };
  private final HashMap<String, QueryEntry> map;
  
  public QueryStatisticsData()
  {
    this.map = new HashMap();
  }
  
  public synchronized List<QueryEntry> getQueries()
  {
    ArrayList localArrayList = new ArrayList();
    localArrayList.addAll(this.map.values());
    
    Collections.sort(localArrayList, QUERY_ENTRY_COMPARATOR);
    return localArrayList.subList(0, Math.min(localArrayList.size(), 100));
  }
  
  public synchronized void update(String paramString, long paramLong, int paramInt)
  {
    QueryEntry localQueryEntry = (QueryEntry)this.map.get(paramString);
    if (localQueryEntry == null)
    {
      localQueryEntry = new QueryEntry();
      localQueryEntry.sqlStatement = paramString;
      this.map.put(paramString, localQueryEntry);
    }
    localQueryEntry.update(paramLong, paramInt);
    if (this.map.size() > 150.0F)
    {
      ArrayList localArrayList = new ArrayList();
      localArrayList.addAll(this.map.values());
      Collections.sort(localArrayList, QUERY_ENTRY_COMPARATOR);
      
      HashSet localHashSet = new HashSet(localArrayList.subList(0, localArrayList.size() / 3));
      
      Iterator localIterator = this.map.entrySet().iterator();
      while (localIterator.hasNext())
      {
        Map.Entry localEntry = (Map.Entry)localIterator.next();
        if (localHashSet.contains(localEntry.getValue())) {
          localIterator.remove();
        }
      }
    }
  }
  
  public static final class QueryEntry
  {
    public String sqlStatement;
    public int count;
    public long lastUpdateTime;
    public long executionTimeMin;
    public long executionTimeMax;
    public long executionTimeCumulative;
    public int rowCountMin;
    public int rowCountMax;
    public long rowCountCumulative;
    public double executionTimeMean;
    public double rowCountMean;
    private double executionTimeM2;
    private double rowCountM2;
    
    void update(long paramLong, int paramInt)
    {
      this.count += 1;
      this.executionTimeMin = Math.min(paramLong, this.executionTimeMin);
      this.executionTimeMax = Math.max(paramLong, this.executionTimeMax);
      this.rowCountMin = Math.min(paramInt, this.rowCountMin);
      this.rowCountMax = Math.max(paramInt, this.rowCountMax);
      
      double d = paramInt - this.rowCountMean;
      this.rowCountMean += d / this.count;
      this.rowCountM2 += d * (paramInt - this.rowCountMean);
      
      d = paramLong - this.executionTimeMean;
      this.executionTimeMean += d / this.count;
      this.executionTimeM2 += d * (paramLong - this.executionTimeMean);
      
      this.executionTimeCumulative += paramLong;
      this.rowCountCumulative += paramInt;
      this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public double getExecutionTimeStandardDeviation()
    {
      return Math.sqrt(this.executionTimeM2 / this.count);
    }
    
    public double getRowCountStandardDeviation()
    {
      return Math.sqrt(this.rowCountM2 / this.count);
    }
  }
}
