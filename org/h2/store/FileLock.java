package org.h2.store;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import org.h2.Driver;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.message.TraceSystem;
import org.h2.store.fs.FileUtils;
import org.h2.util.MathUtils;
import org.h2.util.NetUtils;
import org.h2.util.SortedProperties;
import org.h2.util.StringUtils;
import org.h2.value.Transfer;

public class FileLock
  implements Runnable
{
  public static final int LOCK_NO = 0;
  public static final int LOCK_FILE = 1;
  public static final int LOCK_SOCKET = 2;
  public static final int LOCK_SERIALIZED = 3;
  public static final int LOCK_FS = 4;
  private static final String MAGIC = "FileLock";
  private static final String FILE = "file";
  private static final String SOCKET = "socket";
  private static final String SERIALIZED = "serialized";
  private static final int RANDOM_BYTES = 16;
  private static final int SLEEP_GAP = 25;
  private static final int TIME_GRANULARITY = 2000;
  private volatile String fileName;
  private volatile ServerSocket serverSocket;
  private volatile boolean locked;
  private final int sleep;
  private final Trace trace;
  private long lastWrite;
  private String method;
  private String ipAddress;
  private Properties properties;
  private String uniqueId;
  private Thread watchdog;
  
  public FileLock(TraceSystem paramTraceSystem, String paramString, int paramInt)
  {
    this.trace = (paramTraceSystem == null ? null : paramTraceSystem.getTrace("fileLock"));
    
    this.fileName = paramString;
    this.sleep = paramInt;
  }
  
  public synchronized void lock(int paramInt)
  {
    checkServer();
    if (this.locked) {
      DbException.throwInternalError("already locked");
    }
    switch (paramInt)
    {
    case 1: 
      lockFile();
      break;
    case 2: 
      lockSocket();
      break;
    case 3: 
      lockSerialized();
      break;
    }
    this.locked = true;
  }
  
  public synchronized void unlock()
  {
    if (!this.locked) {
      return;
    }
    this.locked = false;
    try
    {
      if (this.watchdog != null) {
        this.watchdog.interrupt();
      }
    }
    catch (Exception localException1)
    {
      this.trace.debug(localException1, "unlock");
    }
    try
    {
      if ((this.fileName != null) && 
        (load().equals(this.properties))) {
        FileUtils.delete(this.fileName);
      }
      if (this.serverSocket != null) {
        this.serverSocket.close();
      }
    }
    catch (Exception localException2)
    {
      this.trace.debug(localException2, "unlock");
    }
    finally
    {
      this.fileName = null;
      this.serverSocket = null;
    }
    try
    {
      if (this.watchdog != null) {
        this.watchdog.join();
      }
    }
    catch (Exception localException3)
    {
      this.trace.debug(localException3, "unlock");
    }
    finally
    {
      this.watchdog = null;
    }
  }
  
  public void setProperty(String paramString1, String paramString2)
  {
    if (paramString2 == null) {
      this.properties.remove(paramString1);
    } else {
      this.properties.put(paramString1, paramString2);
    }
  }
  
  public Properties save()
  {
    try
    {
      OutputStream localOutputStream = FileUtils.newOutputStream(this.fileName, false);
      try
      {
        this.properties.store(localOutputStream, "FileLock");
      }
      finally
      {
        localOutputStream.close();
      }
      this.lastWrite = FileUtils.lastModified(this.fileName);
      if (this.trace.isDebugEnabled()) {
        this.trace.debug("save " + this.properties);
      }
      return this.properties;
    }
    catch (IOException localIOException)
    {
      throw getExceptionFatal("Could not save properties " + this.fileName, localIOException);
    }
  }
  
  private void checkServer()
  {
    Properties localProperties = load();
    String str1 = localProperties.getProperty("server");
    if (str1 == null) {
      return;
    }
    int i = 0;
    String str2 = localProperties.getProperty("id");
    try
    {
      Socket localSocket = NetUtils.createSocket(str1, 9092, false);
      
      Transfer localTransfer = new Transfer(null);
      localTransfer.setSocket(localSocket);
      localTransfer.init();
      localTransfer.writeInt(6);
      localTransfer.writeInt(15);
      localTransfer.writeString(null);
      localTransfer.writeString(null);
      localTransfer.writeString(str2);
      localTransfer.writeInt(14);
      localTransfer.flush();
      int j = localTransfer.readInt();
      if (j == 1) {
        i = 1;
      }
      localTransfer.close();
      localSocket.close();
    }
    catch (IOException localIOException)
    {
      return;
    }
    if (i != 0)
    {
      DbException localDbException = DbException.get(90020, "Server is running");
      
      throw localDbException.addSQL(str1 + "/" + str2);
    }
  }
  
  public Properties load()
  {
    Object localObject = null;
    for (int i = 0; i < 5; i++) {
      try
      {
        SortedProperties localSortedProperties = SortedProperties.loadProperties(this.fileName);
        if (this.trace.isDebugEnabled()) {
          this.trace.debug("load " + localSortedProperties);
        }
        return localSortedProperties;
      }
      catch (IOException localIOException)
      {
        localObject = localIOException;
      }
    }
    throw getExceptionFatal("Could not load properties " + this.fileName, (Throwable)localObject);
  }
  
  private void waitUntilOld()
  {
    for (int i = 0; i < 160; i++)
    {
      long l1 = FileUtils.lastModified(this.fileName);
      long l2 = System.currentTimeMillis() - l1;
      if (l2 < -2000L)
      {
        try
        {
          Thread.sleep(2L * this.sleep);
        }
        catch (Exception localException1)
        {
          this.trace.debug(localException1, "sleep");
        }
        return;
      }
      if (l2 > 2000L) {
        return;
      }
      try
      {
        Thread.sleep(25L);
      }
      catch (Exception localException2)
      {
        this.trace.debug(localException2, "sleep");
      }
    }
    throw getExceptionFatal("Lock file recently modified", null);
  }
  
  private void setUniqueId()
  {
    byte[] arrayOfByte = MathUtils.secureRandomBytes(16);
    String str = StringUtils.convertBytesToHex(arrayOfByte);
    this.uniqueId = (Long.toHexString(System.currentTimeMillis()) + str);
    this.properties.setProperty("id", this.uniqueId);
  }
  
  private void lockSerialized()
  {
    this.method = "serialized";
    FileUtils.createDirectories(FileUtils.getParent(this.fileName));
    if (FileUtils.createFile(this.fileName))
    {
      this.properties = new SortedProperties();
      this.properties.setProperty("method", String.valueOf(this.method));
      setUniqueId();
      save();
    }
    else
    {
      try
      {
        this.properties = load();
      }
      catch (DbException localDbException) {}
      return;
    }
  }
  
  private void lockFile()
  {
    this.method = "file";
    this.properties = new SortedProperties();
    this.properties.setProperty("method", String.valueOf(this.method));
    setUniqueId();
    FileUtils.createDirectories(FileUtils.getParent(this.fileName));
    if (!FileUtils.createFile(this.fileName))
    {
      waitUntilOld();
      String str = load().getProperty("method", "file");
      if (!str.equals("file")) {
        throw getExceptionFatal("Unsupported lock method " + str, null);
      }
      save();
      sleep(2 * this.sleep);
      if (!load().equals(this.properties)) {
        throw getExceptionAlreadyInUse("Locked by another process");
      }
      FileUtils.delete(this.fileName);
      if (!FileUtils.createFile(this.fileName)) {
        throw getExceptionFatal("Another process was faster", null);
      }
    }
    save();
    sleep(25);
    if (!load().equals(this.properties))
    {
      this.fileName = null;
      throw getExceptionFatal("Concurrent update", null);
    }
    this.watchdog = new Thread(this, "H2 File Lock Watchdog " + this.fileName);
    Driver.setThreadContextClassLoader(this.watchdog);
    this.watchdog.setDaemon(true);
    this.watchdog.setPriority(9);
    this.watchdog.start();
  }
  
  private void lockSocket()
  {
    this.method = "socket";
    this.properties = new SortedProperties();
    this.properties.setProperty("method", String.valueOf(this.method));
    setUniqueId();
    
    this.ipAddress = NetUtils.getLocalAddress();
    FileUtils.createDirectories(FileUtils.getParent(this.fileName));
    if (!FileUtils.createFile(this.fileName))
    {
      waitUntilOld();
      long l = FileUtils.lastModified(this.fileName);
      Properties localProperties = load();
      String str1 = localProperties.getProperty("method", "socket");
      if (str1.equals("file"))
      {
        lockFile();
        return;
      }
      if (!str1.equals("socket")) {
        throw getExceptionFatal("Unsupported lock method " + str1, null);
      }
      String str2 = localProperties.getProperty("ipAddress", this.ipAddress);
      if (!this.ipAddress.equals(str2)) {
        throw getExceptionAlreadyInUse("Locked by another computer: " + str2);
      }
      String str3 = localProperties.getProperty("port", "0");
      int j = Integer.parseInt(str3);
      InetAddress localInetAddress;
      try
      {
        localInetAddress = InetAddress.getByName(str2);
      }
      catch (UnknownHostException localUnknownHostException)
      {
        throw getExceptionFatal("Unknown host " + str2, localUnknownHostException);
      }
      for (int k = 0; k < 3; k++) {
        try
        {
          Socket localSocket = new Socket(localInetAddress, j);
          localSocket.close();
          throw getExceptionAlreadyInUse("Locked by another process");
        }
        catch (BindException localBindException)
        {
          throw getExceptionFatal("Bind Exception", null);
        }
        catch (ConnectException localConnectException)
        {
          this.trace.debug(localConnectException, "socket not connected to port " + str3);
        }
        catch (IOException localIOException)
        {
          throw getExceptionFatal("IOException", null);
        }
      }
      if (l != FileUtils.lastModified(this.fileName)) {
        throw getExceptionFatal("Concurrent update", null);
      }
      FileUtils.delete(this.fileName);
      if (!FileUtils.createFile(this.fileName)) {
        throw getExceptionFatal("Another process was faster", null);
      }
    }
    try
    {
      this.serverSocket = NetUtils.createServerSocket(0, false);
      int i = this.serverSocket.getLocalPort();
      this.properties.setProperty("ipAddress", this.ipAddress);
      this.properties.setProperty("port", String.valueOf(i));
    }
    catch (Exception localException)
    {
      this.trace.debug(localException, "lock");
      this.serverSocket = null;
      lockFile();
      return;
    }
    save();
    this.watchdog = new Thread(this, "H2 File Lock Watchdog (Socket) " + this.fileName);
    
    this.watchdog.setDaemon(true);
    this.watchdog.start();
  }
  
  private static void sleep(int paramInt)
  {
    try
    {
      Thread.sleep(paramInt);
    }
    catch (InterruptedException localInterruptedException)
    {
      throw getExceptionFatal("Sleep interrupted", localInterruptedException);
    }
  }
  
  private static DbException getExceptionFatal(String paramString, Throwable paramThrowable)
  {
    return DbException.get(8000, paramThrowable, new String[] { paramString });
  }
  
  private DbException getExceptionAlreadyInUse(String paramString)
  {
    DbException localDbException1 = DbException.get(90020, paramString);
    if (this.fileName != null) {
      try
      {
        Properties localProperties = load();
        String str1 = localProperties.getProperty("server");
        if (str1 != null)
        {
          String str2 = str1 + "/" + localProperties.getProperty("id");
          localDbException1 = localDbException1.addSQL(str2);
        }
      }
      catch (DbException localDbException2) {}
    }
    return localDbException1;
  }
  
  public static int getFileLockMethod(String paramString)
  {
    if ((paramString == null) || (paramString.equalsIgnoreCase("FILE"))) {
      return 1;
    }
    if (paramString.equalsIgnoreCase("NO")) {
      return 0;
    }
    if (paramString.equalsIgnoreCase("SOCKET")) {
      return 2;
    }
    if (paramString.equalsIgnoreCase("SERIALIZED")) {
      return 3;
    }
    if (paramString.equalsIgnoreCase("FS")) {
      return 4;
    }
    throw DbException.get(90060, paramString);
  }
  
  public String getUniqueId()
  {
    return this.uniqueId;
  }
  
  public void run()
  {
    try
    {
      while ((this.locked) && (this.fileName != null)) {
        try
        {
          if ((!FileUtils.exists(this.fileName)) || (FileUtils.lastModified(this.fileName) != this.lastWrite)) {
            save();
          }
          Thread.sleep(this.sleep);
        }
        catch (OutOfMemoryError localOutOfMemoryError) {}catch (InterruptedException localInterruptedException) {}catch (NullPointerException localNullPointerException) {}catch (Exception localException1)
        {
          this.trace.debug(localException1, "watchdog");
        }
      }
      while (this.serverSocket != null) {
        try
        {
          this.trace.debug("watchdog accept");
          Socket localSocket = this.serverSocket.accept();
          localSocket.close();
        }
        catch (Exception localException2)
        {
          this.trace.debug(localException2, "watchdog");
        }
      }
    }
    catch (Exception localException3)
    {
      this.trace.debug(localException3, "watchdog");
    }
    this.trace.debug("watchdog end");
  }
}
