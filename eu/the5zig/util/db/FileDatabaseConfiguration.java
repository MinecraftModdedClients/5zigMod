package eu.the5zig.util.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class FileDatabaseConfiguration
  implements IDatabaseConfiguration
{
  private final String driver;
  private File file;
  private final String properties;
  
  public FileDatabaseConfiguration(File file, String... properties)
  {
    this.driver = "org.h2.Driver";
    this.file = file;
    StringBuilder stringBuilder = new StringBuilder();
    for (String property : properties) {
      stringBuilder.append(";").append(property);
    }
    this.properties = stringBuilder.toString();
  }
  
  public String getDriver()
  {
    return this.driver;
  }
  
  public void setFile(File file)
  {
    this.file = file;
  }
  
  public File getFile()
  {
    return this.file;
  }
  
  public String getProperties()
  {
    return this.properties;
  }
  
  public Connection getConnection()
    throws SQLException
  {
    return DriverManager.getConnection("jdbc:h2:" + getFile().getAbsolutePath() + getProperties());
  }
  
  public String toString()
  {
    return this.file.toString();
  }
}
