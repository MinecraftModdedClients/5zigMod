package org.h2.store;

import java.lang.ref.WeakReference;
import java.security.AccessControlException;
import org.h2.Driver;
import org.h2.engine.Database;
import org.h2.message.Trace;
import org.h2.message.TraceSystem;

public class WriterThread
  implements Runnable
{
  private volatile WeakReference<Database> databaseRef;
  private int writeDelay;
  private Thread thread;
  private volatile boolean stop;
  
  private WriterThread(Database paramDatabase, int paramInt)
  {
    this.databaseRef = new WeakReference(paramDatabase);
    this.writeDelay = paramInt;
  }
  
  public void setWriteDelay(int paramInt)
  {
    this.writeDelay = paramInt;
  }
  
  public static WriterThread create(Database paramDatabase, int paramInt)
  {
    try
    {
      WriterThread localWriterThread = new WriterThread(paramDatabase, paramInt);
      localWriterThread.thread = new Thread(localWriterThread, "H2 Log Writer " + paramDatabase.getShortName());
      Driver.setThreadContextClassLoader(localWriterThread.thread);
      localWriterThread.thread.setDaemon(true);
      return localWriterThread;
    }
    catch (AccessControlException localAccessControlException) {}
    return null;
  }
  
  public void run()
  {
    while (!this.stop)
    {
      Database localDatabase = (Database)this.databaseRef.get();
      if (localDatabase == null) {
        break;
      }
      int i = this.writeDelay;
      try
      {
        if (localDatabase.isFileLockSerialized())
        {
          i = 5;
          localDatabase.checkpointIfRequired();
        }
        else
        {
          localDatabase.flush();
        }
      }
      catch (Exception localException)
      {
        TraceSystem localTraceSystem = localDatabase.getTraceSystem();
        if (localTraceSystem != null) {
          localTraceSystem.getTrace("database").error(localException, "flush");
        }
      }
      i = Math.max(i, 5);
      synchronized (this)
      {
        while ((!this.stop) && (i > 0))
        {
          int j = Math.min(i, 100);
          try
          {
            wait(j);
          }
          catch (InterruptedException localInterruptedException) {}
          i -= j;
        }
      }
    }
    this.databaseRef = null;
  }
  
  public void stopThread()
  {
    this.stop = true;
    synchronized (this)
    {
      notify();
    }
  }
  
  public void startThread()
  {
    this.thread.start();
    this.thread = null;
  }
}
