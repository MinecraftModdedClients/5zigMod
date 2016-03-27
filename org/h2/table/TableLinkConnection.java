package org.h2.table;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import org.h2.message.DbException;
import org.h2.util.JdbcUtils;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class TableLinkConnection
{
  private final HashMap<TableLinkConnection, TableLinkConnection> map;
  private final String driver;
  private final String url;
  private final String user;
  private final String password;
  private Connection conn;
  private int useCounter;
  
  private TableLinkConnection(HashMap<TableLinkConnection, TableLinkConnection> paramHashMap, String paramString1, String paramString2, String paramString3, String paramString4)
  {
    this.map = paramHashMap;
    this.driver = paramString1;
    this.url = paramString2;
    this.user = paramString3;
    this.password = paramString4;
  }
  
  public static TableLinkConnection open(HashMap<TableLinkConnection, TableLinkConnection> paramHashMap, String paramString1, String paramString2, String paramString3, String paramString4, boolean paramBoolean)
  {
    TableLinkConnection localTableLinkConnection1 = new TableLinkConnection(paramHashMap, paramString1, paramString2, paramString3, paramString4);
    if (!paramBoolean)
    {
      localTableLinkConnection1.open();
      return localTableLinkConnection1;
    }
    synchronized (paramHashMap)
    {
      TableLinkConnection localTableLinkConnection2 = (TableLinkConnection)paramHashMap.get(localTableLinkConnection1);
      if (localTableLinkConnection2 == null)
      {
        localTableLinkConnection1.open();
        
        paramHashMap.put(localTableLinkConnection1, localTableLinkConnection1);
        localTableLinkConnection2 = localTableLinkConnection1;
      }
      localTableLinkConnection2.useCounter += 1;
      return localTableLinkConnection2;
    }
  }
  
  private void open()
  {
    try
    {
      this.conn = JdbcUtils.getConnection(this.driver, this.url, this.user, this.password);
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
  }
  
  public int hashCode()
  {
    return Utils.hashCode(this.driver) ^ Utils.hashCode(this.url) ^ Utils.hashCode(this.user) ^ Utils.hashCode(this.password);
  }
  
  public boolean equals(Object paramObject)
  {
    if ((paramObject instanceof TableLinkConnection))
    {
      TableLinkConnection localTableLinkConnection = (TableLinkConnection)paramObject;
      return (StringUtils.equals(this.driver, localTableLinkConnection.driver)) && (StringUtils.equals(this.url, localTableLinkConnection.url)) && (StringUtils.equals(this.user, localTableLinkConnection.user)) && (StringUtils.equals(this.password, localTableLinkConnection.password));
    }
    return false;
  }
  
  Connection getConnection()
  {
    return this.conn;
  }
  
  void close(boolean paramBoolean)
  {
    int i = 0;
    synchronized (this.map)
    {
      if ((--this.useCounter <= 0) || (paramBoolean))
      {
        i = 1;
        this.map.remove(this);
      }
    }
    if (i != 0) {
      JdbcUtils.closeSilently(this.conn);
    }
  }
}
