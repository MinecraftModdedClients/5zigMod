package eu.the5zig.util.db;

import eu.the5zig.util.db.exceptions.NoConnectionException;
import java.sql.Connection;

public class DummyDatabase
  extends Database
{
  public DummyDatabase()
    throws NoConnectionException
  {
    super(null);
  }
  
  protected synchronized Connection openConnection()
    throws NoConnectionException
  {
    this.connected = true;
    return null;
  }
  
  public <T> SQLQuery<T> get(Class<T> entity)
  {
    return new DummySQLQuery(this, entity);
  }
  
  public int update(String query, Object... fields)
  {
    return 0;
  }
  
  public int updateWithGeneratedKeys(String query, Object... fields)
  {
    return 0;
  }
  
  public synchronized Connection getConnection()
    throws NoConnectionException
  {
    return null;
  }
  
  public synchronized boolean hasConnection()
  {
    return true;
  }
  
  public synchronized void closeConnection() {}
}
