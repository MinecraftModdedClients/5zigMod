package org.h2.util;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import org.h2.engine.Constants;
import org.h2.jdbcx.JdbcDataSource;
import org.osgi.framework.BundleContext;
import org.osgi.service.jdbc.DataSourceFactory;

public class OsgiDataSourceFactory
  implements DataSourceFactory
{
  private org.h2.Driver driver;
  
  public OsgiDataSourceFactory(org.h2.Driver paramDriver)
  {
    this.driver = paramDriver;
  }
  
  public DataSource createDataSource(Properties paramProperties)
    throws SQLException
  {
    Properties localProperties = new Properties();
    if (paramProperties != null) {
      localProperties.putAll(paramProperties);
    }
    rejectUnsupportedOptions(localProperties);
    
    rejectPoolingOptions(localProperties);
    
    JdbcDataSource localJdbcDataSource = new JdbcDataSource();
    
    setupH2DataSource(localJdbcDataSource, localProperties);
    
    return localJdbcDataSource;
  }
  
  public ConnectionPoolDataSource createConnectionPoolDataSource(Properties paramProperties)
    throws SQLException
  {
    Properties localProperties = new Properties();
    if (paramProperties != null) {
      localProperties.putAll(paramProperties);
    }
    rejectUnsupportedOptions(localProperties);
    
    rejectPoolingOptions(localProperties);
    
    JdbcDataSource localJdbcDataSource = new JdbcDataSource();
    
    setupH2DataSource(localJdbcDataSource, localProperties);
    
    return localJdbcDataSource;
  }
  
  public XADataSource createXADataSource(Properties paramProperties)
    throws SQLException
  {
    Properties localProperties = new Properties();
    if (paramProperties != null) {
      localProperties.putAll(paramProperties);
    }
    rejectUnsupportedOptions(localProperties);
    
    rejectPoolingOptions(localProperties);
    
    JdbcDataSource localJdbcDataSource = new JdbcDataSource();
    
    setupH2DataSource(localJdbcDataSource, localProperties);
    
    return localJdbcDataSource;
  }
  
  public java.sql.Driver createDriver(Properties paramProperties)
    throws SQLException
  {
    if ((paramProperties != null) && (!paramProperties.isEmpty())) {
      throw new SQLException();
    }
    return this.driver;
  }
  
  private static void rejectUnsupportedOptions(Properties paramProperties)
    throws SQLFeatureNotSupportedException
  {
    if (paramProperties.containsKey("roleName")) {
      throw new SQLFeatureNotSupportedException("The roleName property is not supported by H2");
    }
    if (paramProperties.containsKey("dataSourceName")) {
      throw new SQLFeatureNotSupportedException("The dataSourceName property is not supported by H2");
    }
  }
  
  private static void setupH2DataSource(JdbcDataSource paramJdbcDataSource, Properties paramProperties)
  {
    if (paramProperties.containsKey("user")) {
      paramJdbcDataSource.setUser((String)paramProperties.remove("user"));
    }
    if (paramProperties.containsKey("password")) {
      paramJdbcDataSource.setPassword((String)paramProperties.remove("password"));
    }
    if (paramProperties.containsKey("description")) {
      paramJdbcDataSource.setDescription((String)paramProperties.remove("description"));
    }
    StringBuffer localStringBuffer = new StringBuffer();
    if (paramProperties.containsKey("url"))
    {
      localStringBuffer.append(paramProperties.remove("url"));
      
      paramProperties.remove("networkProtocol");
      paramProperties.remove("serverName");
      paramProperties.remove("portNumber");
      paramProperties.remove("databaseName");
    }
    else
    {
      localStringBuffer.append("jdbc:h2:");
      
      localObject1 = "";
      if (paramProperties.containsKey("networkProtocol"))
      {
        localObject1 = (String)paramProperties.remove("networkProtocol");
        localStringBuffer.append((String)localObject1).append(":");
      }
      if (paramProperties.containsKey("serverName"))
      {
        localStringBuffer.append("//").append(paramProperties.remove("serverName"));
        if (paramProperties.containsKey("portNumber")) {
          localStringBuffer.append(":").append(paramProperties.remove("portNumber"));
        }
        localStringBuffer.append("/");
      }
      else if (paramProperties.containsKey("portNumber"))
      {
        localStringBuffer.append("//localhost:").append(paramProperties.remove("portNumber")).append("/");
      }
      else if ((((String)localObject1).equals("tcp")) || (((String)localObject1).equals("ssl")))
      {
        localStringBuffer.append("//localhost/");
      }
      if (paramProperties.containsKey("databaseName")) {
        localStringBuffer.append(paramProperties.remove("databaseName"));
      }
    }
    for (Object localObject1 = paramProperties.keySet().iterator(); ((Iterator)localObject1).hasNext();)
    {
      Object localObject2 = ((Iterator)localObject1).next();
      localStringBuffer.append(";").append(localObject2).append("=").append(paramProperties.get(localObject2));
    }
    if (localStringBuffer.length() > "jdbc:h2:".length()) {
      paramJdbcDataSource.setURL(localStringBuffer.toString());
    }
  }
  
  private static void rejectPoolingOptions(Properties paramProperties)
    throws SQLFeatureNotSupportedException
  {
    if ((paramProperties.containsKey("initialPoolSize")) || (paramProperties.containsKey("maxIdleTime")) || (paramProperties.containsKey("maxPoolSize")) || (paramProperties.containsKey("maxStatements")) || (paramProperties.containsKey("minPoolSize")) || (paramProperties.containsKey("propertyCycle"))) {
      throw new SQLFeatureNotSupportedException("Pooling properties are not supported by H2");
    }
  }
  
  static void registerService(BundleContext paramBundleContext, org.h2.Driver paramDriver)
  {
    Properties localProperties = new Properties();
    localProperties.put("osgi.jdbc.driver.class", org.h2.Driver.class.getName());
    
    localProperties.put("osgi.jdbc.driver.name", "H2 JDBC Driver");
    
    localProperties.put("osgi.jdbc.driver.version", Constants.getFullVersion());
    
    paramBundleContext.registerService(DataSourceFactory.class.getName(), new OsgiDataSourceFactory(paramDriver), localProperties);
  }
}
