package org.h2.engine;

import java.lang.ref.WeakReference;
import org.h2.message.Trace;

class DatabaseCloser
  extends Thread
{
  private final boolean shutdownHook;
  private final Trace trace;
  private volatile WeakReference<Database> databaseRef;
  private int delayInMillis;
  
  DatabaseCloser(Database paramDatabase, int paramInt, boolean paramBoolean)
  {
    this.databaseRef = new WeakReference(paramDatabase);
    this.delayInMillis = paramInt;
    this.shutdownHook = paramBoolean;
    this.trace = paramDatabase.getTrace("database");
  }
  
  void reset()
  {
    synchronized (this)
    {
      this.databaseRef = null;
    }
  }
  
  public void run()
  {
    while (this.delayInMillis > 0)
    {
      try
      {
        int i = 100;
        Thread.sleep(i);
        this.delayInMillis -= i;
      }
      catch (Exception localException) {}
      if (this.databaseRef == null) {
        return;
      }
    }
    Database localDatabase = null;
    synchronized (this)
    {
      if (this.databaseRef != null) {
        localDatabase = (Database)this.databaseRef.get();
      }
    }
    if (localDatabase != null) {
      try
      {
        localDatabase.close(this.shutdownHook);
      }
      catch (RuntimeException localRuntimeException1)
      {
        try
        {
          this.trace.error(localRuntimeException1, "could not close the database");
        }
        catch (RuntimeException localRuntimeException2)
        {
          throw localRuntimeException1;
        }
      }
    }
  }
}
