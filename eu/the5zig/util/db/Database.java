package eu.the5zig.util.db;

import eu.the5zig.util.db.exceptions.NoConnectionException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Database
{
  private static final Logger LOGGER = LogManager.getLogger("5zig");
  protected final IDatabaseConfiguration databaseConfiguration;
  protected Connection connection;
  protected boolean connected = false;
  
  public Database(IDatabaseConfiguration databaseConfiguration)
    throws NoConnectionException
  {
    this.databaseConfiguration = databaseConfiguration;
    this.connection = openConnection();
  }
  
  public IDatabaseConfiguration getDatabaseConfiguration()
  {
    return this.databaseConfiguration;
  }
  
  protected synchronized Connection openConnection()
    throws NoConnectionException
  {
    try
    {
      try
      {
        if (this.connection != null) {
          this.connection.close();
        }
      }
      catch (Exception localException1) {}
      getLogger().debug("Connecting to {}", new Object[] { this.databaseConfiguration });
      Class.forName(this.databaseConfiguration.getDriver());
      Connection conn = this.databaseConfiguration.getConnection();
      getLogger().debug("Connected to Database!");
      this.connected = true;
      return this.connection = conn;
    }
    catch (Exception e)
    {
      throw new NoConnectionException("Could not connect to " + this.databaseConfiguration, e);
    }
  }
  
  public <T> SQLQuery<T> get(Class<T> entity)
  {
    return new SQLQuery(this, entity);
  }
  
  public int update(String query, Object... fields)
  {
    try
    {
      connection = getConnection();
    }
    catch (NoConnectionException e)
    {
      Connection connection;
      getLogger().debug(e);
      return 0;
    }
    Connection connection;
    PreparedStatement st = null;
    try
    {
      st = connection.prepareStatement(query);
      for (int i = 0; i < fields.length; i++) {
        st.setObject(i + 1, fields[i]);
      }
      return st.executeUpdate();
    }
    catch (SQLException e)
    {
      getLogger().warn("Could not Execute MySQL Update " + query, e);
    }
    finally
    {
      closeResources(st);
    }
    return 0;
  }
  
  public int updateWithGeneratedKeys(String query, Object... fields)
  {
    try
    {
      connection = getConnection();
    }
    catch (NoConnectionException e)
    {
      Connection connection;
      getLogger().debug(e);
      return 1;
    }
    Connection connection;
    PreparedStatement st = null;
    ResultSet rs = null;
    try
    {
      st = connection.prepareStatement(query, 1);
      for (int i = 0; i < fields.length; i++) {
        st.setObject(i + 1, fields[i]);
      }
      int affectedRows = st.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("No rows Affected after Insert Query!");
      }
      rs = st.getGeneratedKeys();
      if (!rs.next()) {
        throw new SQLException("Inserted Id could not be fetched!");
      }
      return rs.getInt(1);
    }
    catch (SQLException e)
    {
      int i;
      getLogger().warn("Could not Execute MySQL Insert " + query, e);
      return 0;
    }
    finally
    {
      closeResources(rs, st);
    }
  }
  
  public void closeResources(ResultSet rs, PreparedStatement st)
  {
    closeResources(rs);
    closeResources(st);
  }
  
  public void closeResources(PreparedStatement st)
  {
    if (st == null) {
      return;
    }
    try
    {
      st.close();
    }
    catch (SQLException localSQLException) {}
  }
  
  public void closeResources(ResultSet rs)
  {
    if (rs == null) {
      return;
    }
    try
    {
      rs.close();
    }
    catch (SQLException localSQLException) {}
  }
  
  public synchronized Connection getConnection()
    throws NoConnectionException
  {
    if (!this.connected) {
      throw new NoConnectionException("No SQL Connection available!");
    }
    if (!hasConnection()) {
      return openConnection();
    }
    return this.connection;
  }
  
  public synchronized boolean hasConnection()
  {
    try
    {
      return (this.connected) && (this.connection != null) && (this.connection.isValid(10)) && (!this.connection.isClosed());
    }
    catch (Exception e) {}
    return false;
  }
  
  public synchronized void closeConnection()
  {
    if (!this.connected) {
      return;
    }
    try
    {
      this.connection.close();
    }
    catch (SQLException e)
    {
      e.printStackTrace();
    }
    finally
    {
      this.connection = null;
    }
  }
  
  public Logger getLogger()
  {
    return LOGGER;
  }
}
