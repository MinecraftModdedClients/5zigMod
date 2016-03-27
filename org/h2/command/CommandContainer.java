package org.h2.command;

import java.util.ArrayList;
import org.h2.engine.Session;
import org.h2.expression.Parameter;
import org.h2.expression.ParameterInterface;
import org.h2.result.ResultInterface;
import org.h2.value.Value;
import org.h2.value.ValueNull;

class CommandContainer
  extends Command
{
  private Prepared prepared;
  private boolean readOnlyKnown;
  private boolean readOnly;
  
  CommandContainer(Parser paramParser, String paramString, Prepared paramPrepared)
  {
    super(paramParser, paramString);
    paramPrepared.setCommand(this);
    this.prepared = paramPrepared;
  }
  
  public ArrayList<? extends ParameterInterface> getParameters()
  {
    return this.prepared.getParameters();
  }
  
  public boolean isTransactional()
  {
    return this.prepared.isTransactional();
  }
  
  public boolean isQuery()
  {
    return this.prepared.isQuery();
  }
  
  private void recompileIfRequired()
  {
    if (this.prepared.needRecompile())
    {
      this.prepared.setModificationMetaId(0L);
      String str = this.prepared.getSQL();
      ArrayList localArrayList1 = this.prepared.getParameters();
      Parser localParser = new Parser(this.session);
      this.prepared = localParser.parse(str);
      long l = this.prepared.getModificationMetaId();
      this.prepared.setModificationMetaId(0L);
      ArrayList localArrayList2 = this.prepared.getParameters();
      int i = 0;
      for (int j = localArrayList2.size(); i < j; i++)
      {
        Parameter localParameter1 = (Parameter)localArrayList1.get(i);
        if (localParameter1.isValueSet())
        {
          Value localValue = localParameter1.getValue(this.session);
          Parameter localParameter2 = (Parameter)localArrayList2.get(i);
          localParameter2.setValue(localValue);
        }
      }
      this.prepared.prepare();
      this.prepared.setModificationMetaId(l);
    }
  }
  
  public int update()
  {
    recompileIfRequired();
    setProgress(5);
    start();
    this.session.setLastScopeIdentity(ValueNull.INSTANCE);
    this.prepared.checkParameters();
    int i = this.prepared.update();
    this.prepared.trace(this.startTime, i);
    setProgress(6);
    return i;
  }
  
  public ResultInterface query(int paramInt)
  {
    recompileIfRequired();
    setProgress(5);
    start();
    this.prepared.checkParameters();
    ResultInterface localResultInterface = this.prepared.query(paramInt);
    this.prepared.trace(this.startTime, localResultInterface.getRowCount());
    setProgress(6);
    return localResultInterface;
  }
  
  public boolean isReadOnly()
  {
    if (!this.readOnlyKnown)
    {
      this.readOnly = this.prepared.isReadOnly();
      this.readOnlyKnown = true;
    }
    return this.readOnly;
  }
  
  public ResultInterface queryMeta()
  {
    return this.prepared.queryMeta();
  }
  
  public boolean isCacheable()
  {
    return this.prepared.isCacheable();
  }
  
  public int getCommandType()
  {
    return this.prepared.getType();
  }
}
