package org.h2.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.h2.engine.SessionRemote;
import org.h2.engine.SysProperties;
import org.h2.expression.ParameterInterface;
import org.h2.expression.ParameterRemote;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.ResultInterface;
import org.h2.result.ResultRemote;
import org.h2.util.New;
import org.h2.value.Transfer;
import org.h2.value.Value;

public class CommandRemote
  implements CommandInterface
{
  private final ArrayList<Transfer> transferList;
  private final ArrayList<ParameterInterface> parameters;
  private final Trace trace;
  private final String sql;
  private final int fetchSize;
  private SessionRemote session;
  private int id;
  private boolean isQuery;
  private boolean readonly;
  private final int created;
  
  public CommandRemote(SessionRemote paramSessionRemote, ArrayList<Transfer> paramArrayList, String paramString, int paramInt)
  {
    this.transferList = paramArrayList;
    this.trace = paramSessionRemote.getTrace();
    this.sql = paramString;
    this.parameters = New.arrayList();
    prepare(paramSessionRemote, true);
    
    this.session = paramSessionRemote;
    this.fetchSize = paramInt;
    this.created = paramSessionRemote.getLastReconnect();
  }
  
  private void prepare(SessionRemote paramSessionRemote, boolean paramBoolean)
  {
    this.id = paramSessionRemote.getNextId();
    int i = 0;
    for (int j = 0; i < this.transferList.size(); i++) {
      try
      {
        Transfer localTransfer = (Transfer)this.transferList.get(i);
        if (paramBoolean)
        {
          paramSessionRemote.traceOperation("SESSION_PREPARE_READ_PARAMS", this.id);
          localTransfer.writeInt(11).writeInt(this.id).writeString(this.sql);
        }
        else
        {
          paramSessionRemote.traceOperation("SESSION_PREPARE", this.id);
          localTransfer.writeInt(0).writeInt(this.id).writeString(this.sql);
        }
        paramSessionRemote.done(localTransfer);
        this.isQuery = localTransfer.readBoolean();
        this.readonly = localTransfer.readBoolean();
        int k = localTransfer.readInt();
        if (paramBoolean)
        {
          this.parameters.clear();
          for (int m = 0; m < k; m++)
          {
            ParameterRemote localParameterRemote = new ParameterRemote(m);
            localParameterRemote.readMetaData(localTransfer);
            this.parameters.add(localParameterRemote);
          }
        }
      }
      catch (IOException localIOException)
      {
        paramSessionRemote.removeServer(localIOException, i--, ++j);
      }
    }
  }
  
  public boolean isQuery()
  {
    return this.isQuery;
  }
  
  public ArrayList<ParameterInterface> getParameters()
  {
    return this.parameters;
  }
  
  private void prepareIfRequired()
  {
    if (this.session.getLastReconnect() != this.created) {
      this.id = Integer.MIN_VALUE;
    }
    this.session.checkClosed();
    if (this.id <= this.session.getCurrentId() - SysProperties.SERVER_CACHED_OBJECTS) {
      prepare(this.session, false);
    }
  }
  
  public ResultInterface getMetaData()
  {
    synchronized (this.session)
    {
      if (!this.isQuery) {
        return null;
      }
      int i = this.session.getNextId();
      ResultRemote localResultRemote = null;
      int j = 0;
      for (int k = 0; j < this.transferList.size(); j++)
      {
        prepareIfRequired();
        Transfer localTransfer = (Transfer)this.transferList.get(j);
        try
        {
          this.session.traceOperation("COMMAND_GET_META_DATA", this.id);
          localTransfer.writeInt(10).writeInt(this.id).writeInt(i);
          
          this.session.done(localTransfer);
          int m = localTransfer.readInt();
          localResultRemote = new ResultRemote(this.session, localTransfer, i, m, Integer.MAX_VALUE);
        }
        catch (IOException localIOException)
        {
          this.session.removeServer(localIOException, j--, ++k);
        }
      }
      this.session.autoCommitIfCluster();
      return localResultRemote;
    }
  }
  
  public ResultInterface executeQuery(int paramInt, boolean paramBoolean)
  {
    checkParameters();
    synchronized (this.session)
    {
      int i = this.session.getNextId();
      ResultRemote localResultRemote = null;
      int j = 0;
      for (int k = 0; j < this.transferList.size(); j++)
      {
        prepareIfRequired();
        Transfer localTransfer = (Transfer)this.transferList.get(j);
        try
        {
          this.session.traceOperation("COMMAND_EXECUTE_QUERY", this.id);
          localTransfer.writeInt(2).writeInt(this.id).writeInt(i).writeInt(paramInt);
          int m;
          if ((this.session.isClustered()) || (paramBoolean)) {
            m = Integer.MAX_VALUE;
          } else {
            m = this.fetchSize;
          }
          localTransfer.writeInt(m);
          sendParameters(localTransfer);
          this.session.done(localTransfer);
          int n = localTransfer.readInt();
          if (localResultRemote != null)
          {
            localResultRemote.close();
            localResultRemote = null;
          }
          localResultRemote = new ResultRemote(this.session, localTransfer, i, n, m);
          if (this.readonly) {
            break;
          }
        }
        catch (IOException localIOException)
        {
          this.session.removeServer(localIOException, j--, ++k);
        }
      }
      this.session.autoCommitIfCluster();
      this.session.readSessionState();
      return localResultRemote;
    }
  }
  
  public int executeUpdate()
  {
    checkParameters();
    synchronized (this.session)
    {
      int i = 0;
      boolean bool = false;
      int j = 0;
      for (int k = 0; j < this.transferList.size(); j++)
      {
        prepareIfRequired();
        Transfer localTransfer = (Transfer)this.transferList.get(j);
        try
        {
          this.session.traceOperation("COMMAND_EXECUTE_UPDATE", this.id);
          localTransfer.writeInt(3).writeInt(this.id);
          sendParameters(localTransfer);
          this.session.done(localTransfer);
          i = localTransfer.readInt();
          bool = localTransfer.readBoolean();
        }
        catch (IOException localIOException)
        {
          this.session.removeServer(localIOException, j--, ++k);
        }
      }
      this.session.setAutoCommitFromServer(bool);
      this.session.autoCommitIfCluster();
      this.session.readSessionState();
      return i;
    }
  }
  
  private void checkParameters()
  {
    for (ParameterInterface localParameterInterface : this.parameters) {
      localParameterInterface.checkSet();
    }
  }
  
  private void sendParameters(Transfer paramTransfer)
    throws IOException
  {
    int i = this.parameters.size();
    paramTransfer.writeInt(i);
    for (ParameterInterface localParameterInterface : this.parameters) {
      paramTransfer.writeValue(localParameterInterface.getParamValue());
    }
  }
  
  public void close()
  {
    if ((this.session == null) || (this.session.isClosed())) {
      return;
    }
    Object localObject1;
    Object localObject2;
    synchronized (this.session)
    {
      this.session.traceOperation("COMMAND_CLOSE", this.id);
      for (localObject1 = this.transferList.iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (Transfer)((Iterator)localObject1).next();
        try
        {
          ((Transfer)localObject2).writeInt(4).writeInt(this.id);
        }
        catch (IOException localIOException)
        {
          this.trace.error(localIOException, "close");
        }
      }
    }
    this.session = null;
    try
    {
      for (??? = this.parameters.iterator(); ((Iterator)???).hasNext();)
      {
        localObject1 = (ParameterInterface)((Iterator)???).next();
        localObject2 = ((ParameterInterface)localObject1).getParamValue();
        if (localObject2 != null) {
          ((Value)localObject2).close();
        }
      }
    }
    catch (DbException localDbException)
    {
      this.trace.error(localDbException, "close");
    }
    this.parameters.clear();
  }
  
  public void cancel()
  {
    this.session.cancelStatement(this.id);
  }
  
  public String toString()
  {
    return this.sql + Trace.formatParams(getParameters());
  }
  
  public int getCommandType()
  {
    return 0;
  }
}
