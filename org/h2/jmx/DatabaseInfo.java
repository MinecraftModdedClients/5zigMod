package org.h2.jmx;

import java.lang.management.ManagementFactory;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.h2.command.Command;
import org.h2.engine.ConnectionInfo;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Mode;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.TraceSystem;
import org.h2.mvstore.FileStore;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.db.MVTableEngine.Store;
import org.h2.schema.Schema;
import org.h2.store.PageStore;
import org.h2.table.Table;
import org.h2.util.Cache;
import org.h2.util.New;

public class DatabaseInfo
  implements DatabaseInfoMBean
{
  private static final Map<String, ObjectName> MBEANS = ;
  private final Database database;
  
  private DatabaseInfo(Database paramDatabase)
  {
    if (paramDatabase == null) {
      throw new IllegalArgumentException("Argument 'database' must not be null");
    }
    this.database = paramDatabase;
  }
  
  private static ObjectName getObjectName(String paramString1, String paramString2)
    throws JMException
  {
    paramString1 = paramString1.replace(':', '_');
    paramString2 = paramString2.replace(':', '_');
    Hashtable localHashtable = new Hashtable();
    localHashtable.put("name", paramString1);
    localHashtable.put("path", paramString2);
    return new ObjectName("org.h2", localHashtable);
  }
  
  public static void registerMBean(ConnectionInfo paramConnectionInfo, Database paramDatabase)
    throws JMException
  {
    String str1 = paramConnectionInfo.getName();
    if (!MBEANS.containsKey(str1))
    {
      MBeanServer localMBeanServer = ManagementFactory.getPlatformMBeanServer();
      String str2 = paramDatabase.getShortName();
      ObjectName localObjectName = getObjectName(str2, str1);
      MBEANS.put(str1, localObjectName);
      DatabaseInfo localDatabaseInfo = new DatabaseInfo(paramDatabase);
      DocumentedMBean localDocumentedMBean = new DocumentedMBean(localDatabaseInfo, DatabaseInfoMBean.class);
      localMBeanServer.registerMBean(localDocumentedMBean, localObjectName);
    }
  }
  
  public static void unregisterMBean(String paramString)
    throws Exception
  {
    ObjectName localObjectName = (ObjectName)MBEANS.remove(paramString);
    if (localObjectName != null)
    {
      MBeanServer localMBeanServer = ManagementFactory.getPlatformMBeanServer();
      localMBeanServer.unregisterMBean(localObjectName);
    }
  }
  
  public boolean isExclusive()
  {
    return this.database.getExclusiveSession() != null;
  }
  
  public boolean isReadOnly()
  {
    return this.database.isReadOnly();
  }
  
  public String getMode()
  {
    return this.database.getMode().getName();
  }
  
  public boolean isMultiThreaded()
  {
    return this.database.isMultiThreaded();
  }
  
  public boolean isMvcc()
  {
    return this.database.isMultiVersion();
  }
  
  public int getLogMode()
  {
    return this.database.getLogMode();
  }
  
  public void setLogMode(int paramInt)
  {
    this.database.setLogMode(paramInt);
  }
  
  public int getTraceLevel()
  {
    return this.database.getTraceSystem().getLevelFile();
  }
  
  public void setTraceLevel(int paramInt)
  {
    this.database.getTraceSystem().setLevelFile(paramInt);
  }
  
  public long getFileWriteCountTotal()
  {
    if (!this.database.isPersistent()) {
      return 0L;
    }
    PageStore localPageStore = this.database.getPageStore();
    if (localPageStore != null) {
      return localPageStore.getWriteCountTotal();
    }
    return 0L;
  }
  
  public long getFileWriteCount()
  {
    if (!this.database.isPersistent()) {
      return 0L;
    }
    PageStore localPageStore = this.database.getPageStore();
    if (localPageStore != null) {
      return localPageStore.getWriteCount();
    }
    return this.database.getMvStore().getStore().getFileStore().getReadCount();
  }
  
  public long getFileReadCount()
  {
    if (!this.database.isPersistent()) {
      return 0L;
    }
    PageStore localPageStore = this.database.getPageStore();
    if (localPageStore != null) {
      return localPageStore.getReadCount();
    }
    return this.database.getMvStore().getStore().getFileStore().getReadCount();
  }
  
  public long getFileSize()
  {
    if (!this.database.isPersistent()) {
      return 0L;
    }
    PageStore localPageStore = this.database.getPageStore();
    if (localPageStore != null) {
      return localPageStore.getPageCount() * localPageStore.getPageSize() / 1024;
    }
    return this.database.getMvStore().getStore().getFileStore().size();
  }
  
  public int getCacheSizeMax()
  {
    if (!this.database.isPersistent()) {
      return 0;
    }
    PageStore localPageStore = this.database.getPageStore();
    if (localPageStore != null) {
      return localPageStore.getCache().getMaxMemory();
    }
    return this.database.getMvStore().getStore().getCacheSize() * 1024;
  }
  
  public void setCacheSizeMax(int paramInt)
  {
    if (this.database.isPersistent()) {
      this.database.setCacheSize(paramInt);
    }
  }
  
  public int getCacheSize()
  {
    if (!this.database.isPersistent()) {
      return 0;
    }
    PageStore localPageStore = this.database.getPageStore();
    if (localPageStore != null) {
      return localPageStore.getCache().getMemory();
    }
    return this.database.getMvStore().getStore().getCacheSizeUsed() * 1024;
  }
  
  public String getVersion()
  {
    return Constants.getFullVersion();
  }
  
  public String listSettings()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    for (Map.Entry localEntry : new TreeMap(this.database.getSettings().getSettings()).entrySet()) {
      localStringBuilder.append((String)localEntry.getKey()).append(" = ").append((String)localEntry.getValue()).append('\n');
    }
    return localStringBuilder.toString();
  }
  
  public String listSessions()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    for (Session localSession : this.database.getSessions(false))
    {
      localStringBuilder.append("session id: ").append(localSession.getId());
      localStringBuilder.append(" user: ").append(localSession.getUser().getName()).append('\n');
      
      localStringBuilder.append("connected: ").append(new Timestamp(localSession.getSessionStart())).append('\n');
      
      Command localCommand = localSession.getCurrentCommand();
      if (localCommand != null)
      {
        localStringBuilder.append("statement: ").append(localSession.getCurrentCommand()).append('\n');
        
        long l = localSession.getCurrentCommandStart();
        if (l != 0L) {
          localStringBuilder.append("started: ").append(new Timestamp(l)).append('\n');
        }
      }
      Table[] arrayOfTable1 = localSession.getLocks();
      if (arrayOfTable1.length > 0) {
        for (Table localTable : localSession.getLocks())
        {
          if (localTable.isLockedExclusivelyBy(localSession)) {
            localStringBuilder.append("write lock on ");
          } else {
            localStringBuilder.append("read lock on ");
          }
          localStringBuilder.append(localTable.getSchema().getName()).append('.').append(localTable.getName()).append('\n');
        }
      }
      localStringBuilder.append('\n');
    }
    return localStringBuilder.toString();
  }
}
