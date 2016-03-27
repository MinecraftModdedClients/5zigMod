package eu.the5zig.util.db;

import java.sql.Connection;
import java.sql.SQLException;

public abstract interface IDatabaseConfiguration
{
  public abstract String getDriver();
  
  public abstract Connection getConnection()
    throws SQLException;
}
