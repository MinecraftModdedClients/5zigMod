package org.h2.util;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Task
  implements Runnable
{
  private static AtomicInteger counter = new AtomicInteger();
  protected volatile boolean stop;
  protected Object result;
  private volatile boolean finished;
  private Thread thread;
  private Exception ex;
  
  public abstract void call()
    throws Exception;
  
  public void run()
  {
    try
    {
      call();
    }
    catch (Exception localException)
    {
      this.ex = localException;
    }
    this.finished = true;
  }
  
  public Task execute()
  {
    return execute(getClass().getName() + ":" + counter.getAndIncrement());
  }
  
  public Task execute(String paramString)
  {
    this.thread = new Thread(this, paramString);
    this.thread.setDaemon(true);
    this.thread.start();
    return this;
  }
  
  public Object get()
  {
    Exception localException = getException();
    if (localException != null) {
      throw new RuntimeException(localException);
    }
    return this.result;
  }
  
  public boolean isFinished()
  {
    return this.finished;
  }
  
  public Exception getException()
  {
    this.stop = true;
    if (this.thread == null) {
      throw new IllegalStateException("Thread not started");
    }
    try
    {
      this.thread.join();
    }
    catch (InterruptedException localInterruptedException) {}
    if (this.ex != null) {
      return this.ex;
    }
    return null;
  }
}
