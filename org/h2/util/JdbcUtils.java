package org.h2.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import javax.naming.Context;
import javax.sql.DataSource;
import org.h2.api.JavaObjectSerializer;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.store.DataHandler;

public class JdbcUtils
{
  public static JavaObjectSerializer serializer;
  private static final String[] DRIVERS = { "h2:", "org.h2.Driver", "Cache:", "com.intersys.jdbc.CacheDriver", "daffodilDB://", "in.co.daffodil.db.rmi.RmiDaffodilDBDriver", "daffodil", "in.co.daffodil.db.jdbc.DaffodilDBDriver", "db2:", "COM.ibm.db2.jdbc.net.DB2Driver", "derby:net:", "org.apache.derby.jdbc.ClientDriver", "derby://", "org.apache.derby.jdbc.ClientDriver", "derby:", "org.apache.derby.jdbc.EmbeddedDriver", "FrontBase:", "com.frontbase.jdbc.FBJDriver", "firebirdsql:", "org.firebirdsql.jdbc.FBDriver", "hsqldb:", "org.hsqldb.jdbcDriver", "informix-sqli:", "com.informix.jdbc.IfxDriver", "jtds:", "net.sourceforge.jtds.jdbc.Driver", "microsoft:", "com.microsoft.jdbc.sqlserver.SQLServerDriver", "mimer:", "com.mimer.jdbc.Driver", "mysql:", "com.mysql.jdbc.Driver", "odbc:", "sun.jdbc.odbc.JdbcOdbcDriver", "oracle:", "oracle.jdbc.driver.OracleDriver", "pervasive:", "com.pervasive.jdbc.v2.Driver", "pointbase:micro:", "com.pointbase.me.jdbc.jdbcDriver", "pointbase:", "com.pointbase.jdbc.jdbcUniversalDriver", "postgresql:", "org.postgresql.Driver", "sybase:", "com.sybase.jdbc3.jdbc.SybDriver", "sqlserver:", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "teradata:", "com.ncr.teradata.TeraDriver" };
  private static boolean allowAllClasses;
  private static HashSet<String> allowedClassNames;
  private static ArrayList<Utils.ClassFactory> userClassFactories = new ArrayList();
  private static String[] allowedClassNamePrefixes;
  
  public static void addClassFactory(Utils.ClassFactory paramClassFactory)
  {
    getUserClassFactories().add(paramClassFactory);
  }
  
  public static void removeClassFactory(Utils.ClassFactory paramClassFactory)
  {
    getUserClassFactories().remove(paramClassFactory);
  }
  
  private static ArrayList<Utils.ClassFactory> getUserClassFactories()
  {
    if (userClassFactories == null) {
      userClassFactories = new ArrayList();
    }
    return userClassFactories;
  }
  
  static
  {
    String str = SysProperties.JAVA_OBJECT_SERIALIZER;
    if (str != null) {
      try
      {
        serializer = (JavaObjectSerializer)loadUserClass(str).newInstance();
      }
      catch (Exception localException)
      {
        throw DbException.convert(localException);
      }
    }
  }
  
  public static Class<?> loadUserClass(String paramString)
  {
    Object localObject;
    int j;
    if (allowedClassNames == null)
    {
      String str1 = SysProperties.ALLOWED_CLASSES;
      localObject = New.arrayList();
      j = 0;
      HashSet localHashSet = New.hashSet();
      for (String str2 : StringUtils.arraySplit(str1, ',', true)) {
        if (str2.equals("*")) {
          j = 1;
        } else if (str2.endsWith("*")) {
          ((ArrayList)localObject).add(str2.substring(0, str2.length() - 1));
        } else {
          localHashSet.add(str2);
        }
      }
      allowedClassNamePrefixes = new String[((ArrayList)localObject).size()];
      ((ArrayList)localObject).toArray(allowedClassNamePrefixes);
      allowAllClasses = j;
      allowedClassNames = localHashSet;
    }
    if ((!allowAllClasses) && (!allowedClassNames.contains(paramString)))
    {
      int i = 0;
      for (??? : allowedClassNamePrefixes) {
        if (paramString.startsWith(???)) {
          i = 1;
        }
      }
      if (i == 0) {
        throw DbException.get(90134, paramString);
      }
    }
    for (Iterator localIterator = getUserClassFactories().iterator(); localIterator.hasNext();)
    {
      localObject = (Utils.ClassFactory)localIterator.next();
      if (((Utils.ClassFactory)localObject).match(paramString)) {
        try
        {
          Class localClass = ((Utils.ClassFactory)localObject).loadClass(paramString);
          if (localClass != null) {
            return localClass;
          }
        }
        catch (Exception localException2)
        {
          throw DbException.get(90086, localException2, new String[] { paramString });
        }
      }
    }
    try
    {
      return Class.forName(paramString);
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      try
      {
        return Class.forName(paramString, true, Thread.currentThread().getContextClassLoader());
      }
      catch (Exception localException1)
      {
        throw DbException.get(90086, localClassNotFoundException, new String[] { paramString });
      }
    }
    catch (NoClassDefFoundError localNoClassDefFoundError)
    {
      throw DbException.get(90086, localNoClassDefFoundError, new String[] { paramString });
    }
    catch (Error localError)
    {
      throw DbException.get(50000, localError, new String[] { paramString });
    }
  }
  
  public static void closeSilently(Statement paramStatement)
  {
    if (paramStatement != null) {
      try
      {
        paramStatement.close();
      }
      catch (SQLException localSQLException) {}
    }
  }
  
  public static void closeSilently(Connection paramConnection)
  {
    if (paramConnection != null) {
      try
      {
        paramConnection.close();
      }
      catch (SQLException localSQLException) {}
    }
  }
  
  public static void closeSilently(ResultSet paramResultSet)
  {
    if (paramResultSet != null) {
      try
      {
        paramResultSet.close();
      }
      catch (SQLException localSQLException) {}
    }
  }
  
  public static Connection getConnection(String paramString1, String paramString2, String paramString3, String paramString4)
    throws SQLException
  {
    Properties localProperties = new Properties();
    if (paramString3 != null) {
      localProperties.setProperty("user", paramString3);
    }
    if (paramString4 != null) {
      localProperties.setProperty("password", paramString4);
    }
    return getConnection(paramString1, paramString2, localProperties);
  }
  
  public static Connection getConnection(String paramString1, String paramString2, Properties paramProperties)
    throws SQLException
  {
    if (StringUtils.isNullOrEmpty(paramString1))
    {
      load(paramString2);
    }
    else
    {
      Class localClass = loadUserClass(paramString1);
      if (Driver.class.isAssignableFrom(localClass)) {
        return DriverManager.getConnection(paramString2, paramProperties);
      }
      if (Context.class.isAssignableFrom(localClass)) {
        try
        {
          Context localContext = (Context)localClass.newInstance();
          DataSource localDataSource = (DataSource)localContext.lookup(paramString2);
          String str1 = paramProperties.getProperty("user");
          String str2 = paramProperties.getProperty("password");
          if ((StringUtils.isNullOrEmpty(str1)) && (StringUtils.isNullOrEmpty(str2))) {
            return localDataSource.getConnection();
          }
          return localDataSource.getConnection(str1, str2);
        }
        catch (Exception localException)
        {
          throw DbException.toSQLException(localException);
        }
      }
      return DriverManager.getConnection(paramString2, paramProperties);
    }
    return DriverManager.getConnection(paramString2, paramProperties);
  }
  
  public static String getDriver(String paramString)
  {
    if (paramString.startsWith("jdbc:"))
    {
      paramString = paramString.substring("jdbc:".length());
      for (int i = 0; i < DRIVERS.length; i += 2)
      {
        String str = DRIVERS[i];
        if (paramString.startsWith(str)) {
          return DRIVERS[(i + 1)];
        }
      }
    }
    return null;
  }
  
  public static void load(String paramString)
  {
    String str = getDriver(paramString);
    if (str != null) {
      loadUserClass(str);
    }
  }
  
  public static byte[] serialize(Object paramObject, DataHandler paramDataHandler)
  {
    try
    {
      JavaObjectSerializer localJavaObjectSerializer = null;
      if (paramDataHandler != null) {
        localJavaObjectSerializer = paramDataHandler.getJavaObjectSerializer();
      }
      if (localJavaObjectSerializer != null) {
        return localJavaObjectSerializer.serialize(paramObject);
      }
      if (serializer != null) {
        return serializer.serialize(paramObject);
      }
      ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
      ObjectOutputStream localObjectOutputStream = new ObjectOutputStream(localByteArrayOutputStream);
      localObjectOutputStream.writeObject(paramObject);
      return localByteArrayOutputStream.toByteArray();
    }
    catch (Throwable localThrowable)
    {
      throw DbException.get(90026, localThrowable, new String[] { localThrowable.toString() });
    }
  }
  
  public static Object deserialize(byte[] paramArrayOfByte, DataHandler paramDataHandler)
  {
    try
    {
      JavaObjectSerializer localJavaObjectSerializer = null;
      if (paramDataHandler != null) {
        localJavaObjectSerializer = paramDataHandler.getJavaObjectSerializer();
      }
      if (localJavaObjectSerializer != null) {
        return localJavaObjectSerializer.deserialize(paramArrayOfByte);
      }
      if (serializer != null) {
        return serializer.deserialize(paramArrayOfByte);
      }
      ByteArrayInputStream localByteArrayInputStream = new ByteArrayInputStream(paramArrayOfByte);
      Object localObject;
      if (SysProperties.USE_THREAD_CONTEXT_CLASS_LOADER)
      {
        final ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
        localObject = new ObjectInputStream(localByteArrayInputStream)
        {
          protected Class<?> resolveClass(ObjectStreamClass paramAnonymousObjectStreamClass)
            throws IOException, ClassNotFoundException
          {
            try
            {
              return Class.forName(paramAnonymousObjectStreamClass.getName(), true, localClassLoader);
            }
            catch (ClassNotFoundException localClassNotFoundException) {}
            return super.resolveClass(paramAnonymousObjectStreamClass);
          }
        };
      }
      else
      {
        localObject = new ObjectInputStream(localByteArrayInputStream);
      }
      return ((ObjectInputStream)localObject).readObject();
    }
    catch (Throwable localThrowable)
    {
      throw DbException.get(90027, localThrowable, new String[] { localThrowable.toString() });
    }
  }
}
