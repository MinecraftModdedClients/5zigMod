package org.h2.command;

import java.util.ArrayList;
import org.h2.engine.Session;
import org.h2.expression.ParameterInterface;
import org.h2.result.ResultInterface;

class CommandList
  extends Command
{
  private final Command command;
  private final String remaining;
  
  CommandList(Parser paramParser, String paramString1, Command paramCommand, String paramString2)
  {
    super(paramParser, paramString1);
    this.command = paramCommand;
    this.remaining = paramString2;
  }
  
  public ArrayList<? extends ParameterInterface> getParameters()
  {
    return this.command.getParameters();
  }
  
  private void executeRemaining()
  {
    Command localCommand = this.session.prepareLocal(this.remaining);
    if (localCommand.isQuery()) {
      localCommand.query(0);
    } else {
      localCommand.update();
    }
  }
  
  public int update()
  {
    int i = this.command.executeUpdate();
    executeRemaining();
    return i;
  }
  
  public ResultInterface query(int paramInt)
  {
    ResultInterface localResultInterface = this.command.query(paramInt);
    executeRemaining();
    return localResultInterface;
  }
  
  public boolean isQuery()
  {
    return this.command.isQuery();
  }
  
  public boolean isTransactional()
  {
    return true;
  }
  
  public boolean isReadOnly()
  {
    return false;
  }
  
  public ResultInterface queryMeta()
  {
    return this.command.queryMeta();
  }
  
  public int getCommandType()
  {
    return this.command.getCommandType();
  }
}
