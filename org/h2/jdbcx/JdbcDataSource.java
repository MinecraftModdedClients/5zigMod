package org.h2.jdbcx;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import org.h2.Driver;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.TraceObject;
import org.h2.util.StringUtils;

public class JdbcDataSource
  extends TraceObject
  implements XADataSource, DataSource, ConnectionPoolDataSource, Serializable, Referenceable
{
  private static final long serialVersionUID = 1288136338451857771L;
  private transient JdbcDataSourceFactory factory;
  private transient PrintWriter logWriter;
  private int loginTimeout;
  private String userName = "";
  private char[] passwordChars = new char[0];
  private String url = "";
  private String description;
  
  static
  {
    Driver.load();
  }
  
  public JdbcDataSource()
  {
    initFactory();
    int i = getNextId(12);
    setTrace(this.factory.getTrace(), 12, i);
  }
  
  private void readObject(ObjectInputStream paramObjectInputStream)
    throws IOException, ClassNotFoundException
  {
    initFactory();
    paramObjectInputStream.defaultReadObject();
  }
  
  private void initFactory()
  {
    this.factory = new JdbcDataSourceFactory();
  }
  
  public int getLoginTimeout()
  {
    debugCodeCall("getLoginTimeout");
    return this.loginTimeout;
  }
  
  public void setLoginTimeout(int paramInt)
  {
    debugCodeCall("setLoginTimeout", paramInt);
    this.loginTimeout = paramInt;
  }
  
  public PrintWriter getLogWriter()
  {
    debugCodeCall("getLogWriter");
    return this.logWriter;
  }
  
  public void setLogWriter(PrintWriter paramPrintWriter)
  {
    debugCodeCall("setLogWriter(out)");
    this.logWriter = paramPrintWriter;
  }
  
  public Connection getConnection()
    throws SQLException
  {
    debugCodeCall("getConnection");
    return getJdbcConnection(this.userName, StringUtils.cloneCharArray(this.passwordChars));
  }
  
  public Connection getConnection(String paramString1, String paramString2)
    throws SQLException
  {
    if (isDebugEnabled()) {
      debugCode("getConnection(" + quote(paramString1) + ", \"\");");
    }
    return getJdbcConnection(paramString1, convertToCharArray(paramString2));
  }
  
  private JdbcConnection getJdbcConnection(String paramString, char[] paramArrayOfChar)
    throws SQLException
  {
    if (isDebugEnabled()) {
      debugCode("getJdbcConnection(" + quote(paramString) + ", new char[0]);");
    }
    Properties localProperties = new Properties();
    localProperties.setProperty("user", paramString);
    localProperties.put("password", paramArrayOfChar);
    Connection localConnection = Driver.load().connect(this.url, localProperties);
    if (localConnection == null) {
      throw new SQLException("No suitable driver found for " + this.url, "08001", 8001);
    }
    if (!(localConnection instanceof JdbcConnection)) {
      throw new SQLException("Connecting with old version is not supported: " + this.url, "08001", 8001);
    }
    return (JdbcConnection)localConnection;
  }
  
  public String getURL()
  {
    debugCodeCall("getURL");
    return this.url;
  }
  
  public void setURL(String paramString)
  {
    debugCodeCall("setURL", paramString);
    this.url = paramString;
  }
  
  public String getUrl()
  {
    debugCodeCall("getUrl");
    return this.url;
  }
  
  public void setUrl(String paramString)
  {
    debugCodeCall("setUrl", paramString);
    this.url = paramString;
  }
  
  public void setPassword(String paramString)
  {
    debugCodeCall("setPassword", "");
    this.passwordChars = convertToCharArray(paramString);
  }
  
  public void setPasswordChars(char[] paramArrayOfChar)
  {
    if (isDebugEnabled()) {
      debugCode("setPasswordChars(new char[0]);");
    }
    this.passwordChars = paramArrayOfChar;
  }
  
  private static char[] convertToCharArray(String paramString)
  {
    return paramString == null ? null : paramString.toCharArray();
  }
  
  private static String convertToString(char[] paramArrayOfChar)
  {
    return paramArrayOfChar == null ? null : new String(paramArrayOfChar);
  }
  
  public String getPassword()
  {
    debugCodeCall("getPassword");
    return convertToString(this.passwordChars);
  }
  
  public String getUser()
  {
    debugCodeCall("getUser");
    return this.userName;
  }
  
  public void setUser(String paramString)
  {
    debugCodeCall("setUser", paramString);
    this.userName = paramString;
  }
  
  public String getDescription()
  {
    debugCodeCall("getDescription");
    return this.description;
  }
  
  public void setDescription(String paramString)
  {
    debugCodeCall("getDescription", paramString);
    this.description = paramString;
  }
  
  public Reference getReference()
  {
    debugCodeCall("getReference");
    String str = JdbcDataSourceFactory.class.getName();
    Reference localReference = new Reference(getClass().getName(), str, null);
    localReference.add(new StringRefAddr("url", this.url));
    localReference.add(new StringRefAddr("user", this.userName));
    localReference.add(new StringRefAddr("password", convertToString(this.passwordChars)));
    localReference.add(new StringRefAddr("loginTimeout", String.valueOf(this.loginTimeout)));
    localReference.add(new StringRefAddr("description", this.description));
    return localReference;
  }
  
  public XAConnection getXAConnection()
    throws SQLException
  {
    debugCodeCall("getXAConnection");
    int i = getNextId(13);
    return new JdbcXAConnection(this.factory, i, getJdbcConnection(this.userName, StringUtils.cloneCharArray(this.passwordChars)));
  }
  
  public XAConnection getXAConnection(String paramString1, String paramString2)
    throws SQLException
  {
    if (isDebugEnabled()) {
      debugCode("getXAConnection(" + quote(paramString1) + ", \"\");");
    }
    int i = getNextId(13);
    return new JdbcXAConnection(this.factory, i, getJdbcConnection(paramString1, convertToCharArray(paramString2)));
  }
  
  public PooledConnection getPooledConnection()
    throws SQLException
  {
    debugCodeCall("getPooledConnection");
    return getXAConnection();
  }
  
  public PooledConnection getPooledConnection(String paramString1, String paramString2)
    throws SQLException
  {
    if (isDebugEnabled()) {
      debugCode("getPooledConnection(" + quote(paramString1) + ", \"\");");
    }
    return getXAConnection(paramString1, paramString2);
  }
  
  public <T> T unwrap(Class<T> paramClass)
    throws SQLException
  {
    throw unsupported("unwrap");
  }
  
  public boolean isWrapperFor(Class<?> paramClass)
    throws SQLException
  {
    throw unsupported("isWrapperFor");
  }
  
  public String toString()
  {
    return getTraceObjectName() + ": url=" + this.url + " user=" + this.userName;
  }
}
