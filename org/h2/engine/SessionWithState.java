package org.h2.engine;

import java.util.ArrayList;
import org.h2.command.CommandInterface;
import org.h2.result.ResultInterface;
import org.h2.util.New;
import org.h2.value.Value;

abstract class SessionWithState
  implements SessionInterface
{
  protected ArrayList<String> sessionState;
  protected boolean sessionStateChanged;
  private boolean sessionStateUpdating;
  
  protected void recreateSessionState()
  {
    if ((this.sessionState != null) && (this.sessionState.size() > 0))
    {
      this.sessionStateUpdating = true;
      try
      {
        for (String str : this.sessionState)
        {
          CommandInterface localCommandInterface = prepareCommand(str, Integer.MAX_VALUE);
          localCommandInterface.executeUpdate();
        }
      }
      finally
      {
        this.sessionStateUpdating = false;
        this.sessionStateChanged = false;
      }
    }
  }
  
  public void readSessionState()
  {
    if ((!this.sessionStateChanged) || (this.sessionStateUpdating)) {
      return;
    }
    this.sessionStateChanged = false;
    this.sessionState = New.arrayList();
    CommandInterface localCommandInterface = prepareCommand("SELECT * FROM INFORMATION_SCHEMA.SESSION_STATE", Integer.MAX_VALUE);
    
    ResultInterface localResultInterface = localCommandInterface.executeQuery(0, false);
    while (localResultInterface.next())
    {
      Value[] arrayOfValue = localResultInterface.currentRow();
      this.sessionState.add(arrayOfValue[1].getString());
    }
  }
}
