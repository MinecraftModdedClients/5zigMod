package eu.the5zig.util.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class BasicDatabaseConfiguration
  implements IDatabaseConfiguration
{
  private final String driver;
  private final String host;
  private final int port;
  private final String user;
  private final String pass;
  private final String database;
  
  public BasicDatabaseConfiguration(String host, int port, String user, String pass, String database)
  {
    this("com.mysql.jdbc.Driver", host, port, user, pass, database);
  }
  
  public BasicDatabaseConfiguration(String driver, String host, int port, String user, String pass, String database)
  {
    this.driver = driver;
    this.host = host;
    this.port = port;
    this.user = user;
    this.pass = pass;
    this.database = database;
  }
  
  public String getDriver()
  {
    return this.driver;
  }
  
  public String getHost()
  {
    return this.host;
  }
  
  public int getPort()
  {
    return this.port;
  }
  
  public String getUser()
  {
    return this.user;
  }
  
  public String getPass()
  {
    return this.pass;
  }
  
  public String getDatabase()
  {
    return this.database;
  }
  
  public String getURL()
  {
    return String.format("jdbc:mysql://%s:%s/%s", new Object[] { getHost(), Integer.valueOf(getPort()), getDatabase() });
  }
  
  public Connection getConnection()
    throws SQLException
  {
    return DriverManager.getConnection(getURL(), getUser(), getPass());
  }
  
  public String toString()
  {
    return this.host + ":" + this.port;
  }
}
