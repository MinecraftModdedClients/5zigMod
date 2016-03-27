package org.h2.engine;

import java.util.ArrayList;
import java.util.HashMap;
import org.h2.command.CommandInterface;
import org.h2.command.Parser;
import org.h2.message.DbException;
import org.h2.store.FileLock;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class Engine
  implements SessionFactory
{
  private static final Engine INSTANCE = new Engine();
  private static final HashMap<String, Database> DATABASES = New.hashMap();
  private volatile long wrongPasswordDelay = SysProperties.DELAY_WRONG_PASSWORD_MIN;
  private boolean jmx;
  
  public static Engine getInstance()
  {
    return INSTANCE;
  }
  
  private Session openSession(ConnectionInfo paramConnectionInfo, boolean paramBoolean, String paramString)
  {
    String str = paramConnectionInfo.getName();
    
    paramConnectionInfo.removeProperty("NO_UPGRADE", false);
    boolean bool = paramConnectionInfo.getProperty("OPEN_NEW", false);
    Database localDatabase;
    if ((bool) || (paramConnectionInfo.isUnnamedInMemory())) {
      localDatabase = null;
    } else {
      localDatabase = (Database)DATABASES.get(str);
    }
    User localUser = null;
    int i = 0;
    if (localDatabase == null)
    {
      if ((paramBoolean) && (!Database.exists(str))) {
        throw DbException.get(90013, str);
      }
      localDatabase = new Database(paramConnectionInfo, paramString);
      i = 1;
      if (localDatabase.getAllUsers().size() == 0)
      {
        localUser = new User(localDatabase, localDatabase.allocateObjectId(), paramConnectionInfo.getUserName(), false);
        
        localUser.setAdmin(true);
        localUser.setUserPasswordHash(paramConnectionInfo.getUserPasswordHash());
        localDatabase.setMasterUser(localUser);
      }
      if (!paramConnectionInfo.isUnnamedInMemory()) {
        DATABASES.put(str, localDatabase);
      }
    }
    synchronized (localDatabase)
    {
      if (i != 0) {
        localDatabase.opened();
      }
      if (localDatabase.isClosing()) {
        return null;
      }
      if (localUser == null)
      {
        if (localDatabase.validateFilePasswordHash(paramString, paramConnectionInfo.getFilePasswordHash()))
        {
          localUser = localDatabase.findUser(paramConnectionInfo.getUserName());
          if ((localUser != null) && 
            (!localUser.validateUserPasswordHash(paramConnectionInfo.getUserPasswordHash()))) {
            localUser = null;
          }
        }
        if ((i != 0) && ((localUser == null) || (!localUser.isAdmin()))) {
          localDatabase.setEventListener(null);
        }
      }
      if (localUser == null)
      {
        localDatabase.removeSession(null);
        throw DbException.get(28000);
      }
      checkClustering(paramConnectionInfo, localDatabase);
      Session localSession = localDatabase.createSession(localUser);
      if (paramConnectionInfo.getProperty("JMX", false))
      {
        try
        {
          Utils.callStaticMethod("org.h2.jmx.DatabaseInfo.registerMBean", new Object[] { paramConnectionInfo, localDatabase });
        }
        catch (Exception localException)
        {
          localDatabase.removeSession(localSession);
          throw DbException.get(50100, localException, new String[] { "JMX" });
        }
        this.jmx = true;
      }
      return localSession;
    }
  }
  
  public Session createSession(ConnectionInfo paramConnectionInfo)
  {
    return INSTANCE.createSessionAndValidate(paramConnectionInfo);
  }
  
  private Session createSessionAndValidate(ConnectionInfo paramConnectionInfo)
  {
    try
    {
      ConnectionInfo localConnectionInfo = null;
      String str = paramConnectionInfo.getProperty("FILE_LOCK", null);
      int i = FileLock.getFileLockMethod(str);
      if (i == 3)
      {
        paramConnectionInfo.setProperty("OPEN_NEW", "TRUE");
        try
        {
          localConnectionInfo = paramConnectionInfo.clone();
        }
        catch (CloneNotSupportedException localCloneNotSupportedException)
        {
          throw DbException.convert(localCloneNotSupportedException);
        }
      }
      Session localSession = openSession(paramConnectionInfo);
      validateUserAndPassword(true);
      if (localConnectionInfo != null) {
        localSession.setConnectionInfo(localConnectionInfo);
      }
      return localSession;
    }
    catch (DbException localDbException)
    {
      if (localDbException.getErrorCode() == 28000) {
        validateUserAndPassword(false);
      }
      throw localDbException;
    }
  }
  
  private synchronized Session openSession(ConnectionInfo paramConnectionInfo)
  {
    boolean bool1 = paramConnectionInfo.removeProperty("IFEXISTS", false);
    boolean bool2 = paramConnectionInfo.removeProperty("IGNORE_UNKNOWN_SETTINGS", false);
    
    String str1 = paramConnectionInfo.removeProperty("CIPHER", null);
    String str2 = paramConnectionInfo.removeProperty("INIT", null);
    Session localSession;
    for (int i = 0;; i++)
    {
      localSession = openSession(paramConnectionInfo, bool1, str1);
      if (localSession != null) {
        break;
      }
      if (i > 60000) {
        throw DbException.get(90020, "Waited for database closing longer than 1 minute");
      }
      try
      {
        Thread.sleep(1L);
      }
      catch (InterruptedException localInterruptedException) {}
    }
    localSession.setAllowLiterals(true);
    DbSettings localDbSettings = DbSettings.getInstance(null);
    for (String str3 : paramConnectionInfo.getKeys()) {
      if (!localDbSettings.containsKey(str3))
      {
        String str4 = paramConnectionInfo.getProperty(str3);
        try
        {
          CommandInterface localCommandInterface = localSession.prepareCommand("SET " + Parser.quoteIdentifier(str3) + " " + str4, Integer.MAX_VALUE);
          
          localCommandInterface.executeUpdate();
        }
        catch (DbException localDbException2)
        {
          if (!bool2)
          {
            localSession.close();
            throw localDbException2;
          }
        }
      }
    }
    if (str2 != null) {
      try
      {
        ??? = localSession.prepareCommand(str2, Integer.MAX_VALUE);
        
        ((CommandInterface)???).executeUpdate();
      }
      catch (DbException localDbException1)
      {
        if (!bool2)
        {
          localSession.close();
          throw localDbException1;
        }
      }
    }
    localSession.setAllowLiterals(false);
    localSession.commit(true);
    return localSession;
  }
  
  private static void checkClustering(ConnectionInfo paramConnectionInfo, Database paramDatabase)
  {
    String str1 = paramConnectionInfo.getProperty(13, null);
    if ("''".equals(str1)) {
      return;
    }
    String str2 = paramDatabase.getCluster();
    if ((!"''".equals(str2)) && 
      (!"TRUE".equals(str1)) && 
      (!StringUtils.equals(str1, str2)))
    {
      if (str2.equals("''")) {
        throw DbException.get(90093);
      }
      throw DbException.get(90094, str2);
    }
  }
  
  void close(String paramString)
  {
    if (this.jmx) {
      try
      {
        Utils.callStaticMethod("org.h2.jmx.DatabaseInfo.unregisterMBean", new Object[] { paramString });
      }
      catch (Exception localException)
      {
        throw DbException.get(50100, localException, new String[] { "JMX" });
      }
    }
    DATABASES.remove(paramString);
  }
  
  private void validateUserAndPassword(boolean paramBoolean)
  {
    int i = SysProperties.DELAY_WRONG_PASSWORD_MIN;
    if (paramBoolean)
    {
      long l1 = this.wrongPasswordDelay;
      if ((l1 > i) && (l1 > 0L)) {
        synchronized (INSTANCE)
        {
          l1 = MathUtils.secureRandomInt((int)l1);
          try
          {
            Thread.sleep(l1);
          }
          catch (InterruptedException localInterruptedException1) {}
          this.wrongPasswordDelay = i;
        }
      }
    }
    else
    {
      synchronized (INSTANCE)
      {
        long l2 = this.wrongPasswordDelay;
        int j = SysProperties.DELAY_WRONG_PASSWORD_MAX;
        if (j <= 0) {
          j = Integer.MAX_VALUE;
        }
        this.wrongPasswordDelay += this.wrongPasswordDelay;
        if ((this.wrongPasswordDelay > j) || (this.wrongPasswordDelay < 0L)) {
          this.wrongPasswordDelay = j;
        }
        if (i > 0)
        {
          l2 += Math.abs(MathUtils.secureRandomLong() % 100L);
          try
          {
            Thread.sleep(l2);
          }
          catch (InterruptedException localInterruptedException2) {}
        }
        throw DbException.get(28000);
      }
    }
  }
}
