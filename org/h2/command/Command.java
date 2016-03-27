package org.h2.command;

import java.sql.SQLException;
import java.util.ArrayList;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.Session.Savepoint;
import org.h2.expression.ParameterInterface;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.ResultInterface;
import org.h2.util.MathUtils;

public abstract class Command
  implements CommandInterface
{
  protected final Session session;
  protected long startTime;
  private final Trace trace;
  private volatile boolean cancel;
  private final String sql;
  private boolean canReuse;
  
  Command(Parser paramParser, String paramString)
  {
    this.session = paramParser.getSession();
    this.sql = paramString;
    this.trace = this.session.getDatabase().getTrace("command");
  }
  
  public abstract boolean isTransactional();
  
  public abstract boolean isQuery();
  
  public abstract ArrayList<? extends ParameterInterface> getParameters();
  
  public abstract boolean isReadOnly();
  
  public abstract ResultInterface queryMeta();
  
  public int update()
  {
    throw DbException.get(90001);
  }
  
  public ResultInterface query(int paramInt)
  {
    throw DbException.get(90002);
  }
  
  public final ResultInterface getMetaData()
  {
    return queryMeta();
  }
  
  void start()
  {
    if (this.trace.isInfoEnabled()) {
      this.startTime = System.currentTimeMillis();
    }
  }
  
  void setProgress(int paramInt)
  {
    this.session.getDatabase().setProgress(paramInt, this.sql, 0, 0);
  }
  
  protected void checkCanceled()
  {
    if (this.cancel)
    {
      this.cancel = false;
      throw DbException.get(57014);
    }
  }
  
  private void stop()
  {
    this.session.endStatement();
    this.session.setCurrentCommand(null);
    if (!isTransactional())
    {
      this.session.commit(true);
    }
    else if (this.session.getAutoCommit())
    {
      this.session.commit(false);
    }
    else if (this.session.getDatabase().isMultiThreaded())
    {
      Database localDatabase = this.session.getDatabase();
      if ((localDatabase != null) && 
        (localDatabase.getLockMode() == 3)) {
        this.session.unlockReadLocks();
      }
    }
    if ((this.trace.isInfoEnabled()) && (this.startTime > 0L))
    {
      long l = System.currentTimeMillis() - this.startTime;
      if (l > 100L) {
        this.trace.info("slow query: {0} ms", new Object[] { Long.valueOf(l) });
      }
    }
  }
  
  public ResultInterface executeQuery(int paramInt, boolean paramBoolean)
  {
    this.startTime = 0L;
    long l = 0L;
    Database localDatabase1 = this.session.getDatabase();
    Database localDatabase2 = localDatabase1.isMultiThreaded() ? this.session : localDatabase1;
    this.session.waitIfExclusiveModeEnabled();
    int i = 1;
    int j = !isReadOnly() ? 1 : 0;
    while ((j != 0) && 
      (!localDatabase1.beforeWriting())) {}
    synchronized (localDatabase2)
    {
      this.session.setCurrentCommand(this);
      try
      {
        for (;;)
        {
          localDatabase1.checkPowerOff();
          try
          {
            ResultInterface localResultInterface = query(paramInt);
            if (i != 0) {
              stop();
            }
            if (j != 0) {
              localDatabase1.afterWriting();
            }
            return localResultInterface;
          }
          catch (DbException localDbException1)
          {
            l = filterConcurrentUpdate(localDbException1, l);
          }
          catch (OutOfMemoryError localOutOfMemoryError)
          {
            i = 0;
            
            localDatabase1.shutdownImmediately();
            throw DbException.convert(localOutOfMemoryError);
          }
          catch (Throwable localThrowable)
          {
            throw DbException.convert(localThrowable);
          }
        }
        DbException localDbException3;
        SQLException localSQLException;
        localObject2 = finally;
      }
      catch (DbException localDbException2)
      {
        localDbException3 = localDbException2.addSQL(this.sql);
        localSQLException = localDbException3.getSQLException();
        localDatabase1.exceptionThrown(localSQLException, this.sql);
        if (localSQLException.getErrorCode() == 90108)
        {
          i = 0;
          localDatabase1.shutdownImmediately();
          throw localDbException3;
        }
        localDatabase1.checkPowerOff();
        throw localDbException3;
      }
      finally
      {
        if (i != 0) {
          stop();
        }
        if (j != 0) {
          localDatabase1.afterWriting();
        }
      }
    }
  }
  
  public int executeUpdate()
  {
    long l = 0L;
    Database localDatabase1 = this.session.getDatabase();
    Database localDatabase2 = localDatabase1.isMultiThreaded() ? this.session : localDatabase1;
    this.session.waitIfExclusiveModeEnabled();
    int i = 1;
    int j = !isReadOnly() ? 1 : 0;
    while ((j != 0) && 
      (!localDatabase1.beforeWriting())) {}
    synchronized (localDatabase2)
    {
      Session.Savepoint localSavepoint = this.session.setSavepoint();
      this.session.setCurrentCommand(this);
      try
      {
        for (;;)
        {
          localDatabase1.checkPowerOff();
          try
          {
            int k = update();
            try
            {
              if (i != 0) {
                stop();
              }
            }
            finally
            {
              if (j != 0) {
                localDatabase1.afterWriting();
              }
            }
            return k;
          }
          catch (DbException localDbException1)
          {
            l = filterConcurrentUpdate(localDbException1, l);
          }
          catch (OutOfMemoryError localOutOfMemoryError)
          {
            i = 0;
            localDatabase1.shutdownImmediately();
            throw DbException.convert(localOutOfMemoryError);
          }
          catch (Throwable localThrowable)
          {
            throw DbException.convert(localThrowable);
          }
        }
        DbException localDbException3;
        SQLException localSQLException;
        localObject4 = finally;
      }
      catch (DbException localDbException2)
      {
        localDbException3 = localDbException2.addSQL(this.sql);
        localSQLException = localDbException3.getSQLException();
        localDatabase1.exceptionThrown(localSQLException, this.sql);
        if (localSQLException.getErrorCode() == 90108)
        {
          i = 0;
          localDatabase1.shutdownImmediately();
          throw localDbException3;
        }
        localDatabase1.checkPowerOff();
        if (localSQLException.getErrorCode() == 40001) {
          this.session.rollback();
        } else {
          this.session.rollbackTo(localSavepoint, false);
        }
        throw localDbException3;
      }
      finally
      {
        try
        {
          if (i != 0) {
            stop();
          }
        }
        finally
        {
          if (j != 0) {
            localDatabase1.afterWriting();
          }
        }
      }
    }
  }
  
  private long filterConcurrentUpdate(DbException paramDbException, long paramLong)
  {
    if (paramDbException.getErrorCode() != 90131) {
      throw paramDbException;
    }
    long l1 = System.nanoTime() / 1000000L;
    if ((paramLong != 0L) && (l1 - paramLong > this.session.getLockTimeout())) {
      throw DbException.get(50200, paramDbException.getCause(), new String[] { "" });
    }
    Database localDatabase = this.session.getDatabase();
    int i = 1 + MathUtils.randomInt(10);
    for (;;)
    {
      try
      {
        if (localDatabase.isMultiThreaded()) {
          Thread.sleep(i);
        } else {
          localDatabase.wait(i);
        }
      }
      catch (InterruptedException localInterruptedException) {}
      long l2 = System.nanoTime() / 1000000L - l1;
      if (l2 >= i) {
        break;
      }
    }
    return paramLong == 0L ? l1 : paramLong;
  }
  
  public void close()
  {
    this.canReuse = true;
  }
  
  public void cancel()
  {
    this.cancel = true;
  }
  
  public String toString()
  {
    return this.sql + Trace.formatParams(getParameters());
  }
  
  public boolean isCacheable()
  {
    return false;
  }
  
  public boolean canReuse()
  {
    return this.canReuse;
  }
  
  public void reuse()
  {
    this.canReuse = false;
    ArrayList localArrayList = getParameters();
    int i = 0;
    for (int j = localArrayList.size(); i < j; i++)
    {
      ParameterInterface localParameterInterface = (ParameterInterface)localArrayList.get(i);
      localParameterInterface.setValue(null, true);
    }
  }
}
