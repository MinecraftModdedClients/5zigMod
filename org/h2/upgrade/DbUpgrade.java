package org.h2.upgrade;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;
import org.h2.engine.ConnectionInfo;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.DbException;
import org.h2.store.fs.FileUtils;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class DbUpgrade
{
  private static final boolean UPGRADE_CLASSES_PRESENT = Utils.isClassPresent("org.h2.upgrade.v1_1.Driver");
  private static boolean scriptInTempDir;
  private static boolean deleteOldDb;
  
  public static Connection connectOrUpgrade(String paramString, Properties paramProperties)
    throws SQLException
  {
    if (!UPGRADE_CLASSES_PRESENT) {
      return null;
    }
    Properties localProperties = new Properties();
    localProperties.putAll(paramProperties);
    
    Object localObject1 = paramProperties.get("password");
    if ((localObject1 instanceof char[])) {
      localProperties.put("password", StringUtils.cloneCharArray((char[])localObject1));
    }
    paramProperties = localProperties;
    ConnectionInfo localConnectionInfo = new ConnectionInfo(paramString, paramProperties);
    if ((localConnectionInfo.isRemote()) || (!localConnectionInfo.isPersistent())) {
      return null;
    }
    String str = localConnectionInfo.getName();
    if (FileUtils.exists(str + ".h2.db")) {
      return null;
    }
    if (!FileUtils.exists(str + ".data.db")) {
      return null;
    }
    if (localConnectionInfo.removeProperty("NO_UPGRADE", false)) {
      return connectWithOldVersion(paramString, paramProperties);
    }
    synchronized (DbUpgrade.class)
    {
      upgrade(localConnectionInfo, paramProperties);
      return null;
    }
  }
  
  public static void setScriptInTempDir(boolean paramBoolean)
  {
    scriptInTempDir = paramBoolean;
  }
  
  public static void setDeleteOldDb(boolean paramBoolean)
  {
    deleteOldDb = paramBoolean;
  }
  
  private static Connection connectWithOldVersion(String paramString, Properties paramProperties)
    throws SQLException
  {
    paramString = "jdbc:h2v1_1:" + paramString.substring("jdbc:h2:".length()) + ";IGNORE_UNKNOWN_SETTINGS=TRUE";
    
    return DriverManager.getConnection(paramString, paramProperties);
  }
  
  private static void upgrade(ConnectionInfo paramConnectionInfo, Properties paramProperties)
    throws SQLException
  {
    String str1 = paramConnectionInfo.getName();
    String str2 = str1 + ".data.db";
    String str3 = str1 + ".index.db";
    String str4 = str1 + ".lobs.db";
    String str5 = str2 + ".backup";
    String str6 = str3 + ".backup";
    String str7 = str4 + ".backup";
    String str8 = null;
    try
    {
      if (scriptInTempDir)
      {
        new File(Utils.getProperty("java.io.tmpdir", ".")).mkdirs();
        str8 = File.createTempFile("h2dbmigration", "backup.sql").getAbsolutePath();
      }
      else
      {
        str8 = str1 + ".script.sql";
      }
      String str9 = "jdbc:h2v1_1:" + str1 + ";UNDO_LOG=0;LOG=0;LOCK_MODE=0";
      
      String str10 = paramConnectionInfo.getProperty("CIPHER", null);
      if (str10 != null) {
        str9 = str9 + ";CIPHER=" + str10;
      }
      Object localObject1 = DriverManager.getConnection(str9, paramProperties);
      Statement localStatement = ((Connection)localObject1).createStatement();
      String str11 = UUID.randomUUID().toString();
      if (str10 != null) {
        localStatement.execute("script to '" + str8 + "' cipher aes password '" + str11 + "' --hide--");
      } else {
        localStatement.execute("script to '" + str8 + "'");
      }
      ((Connection)localObject1).close();
      FileUtils.move(str2, str5);
      FileUtils.move(str3, str6);
      if (FileUtils.exists(str4)) {
        FileUtils.move(str4, str7);
      }
      paramConnectionInfo.removeProperty("IFEXISTS", false);
      localObject1 = new JdbcConnection(paramConnectionInfo, true);
      localStatement = ((Connection)localObject1).createStatement();
      if (str10 != null) {
        localStatement.execute("runscript from '" + str8 + "' cipher aes password '" + str11 + "' --hide--");
      } else {
        localStatement.execute("runscript from '" + str8 + "'");
      }
      localStatement.execute("analyze");
      localStatement.execute("shutdown compact");
      localStatement.close();
      ((Connection)localObject1).close();
      if (deleteOldDb)
      {
        FileUtils.delete(str5);
        FileUtils.delete(str6);
        FileUtils.deleteRecursive(str7, false);
      }
    }
    catch (Exception localException)
    {
      if (FileUtils.exists(str5)) {
        FileUtils.move(str5, str2);
      }
      if (FileUtils.exists(str6)) {
        FileUtils.move(str6, str3);
      }
      if (FileUtils.exists(str7)) {
        FileUtils.move(str7, str4);
      }
      FileUtils.delete(str1 + ".h2.db");
      throw DbException.toSQLException(localException);
    }
    finally
    {
      if (str8 != null) {
        FileUtils.delete(str8);
      }
    }
  }
}
