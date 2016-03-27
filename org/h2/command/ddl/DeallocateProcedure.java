package org.h2.command.ddl;

import org.h2.engine.Session;

public class DeallocateProcedure
  extends DefineCommand
{
  private String procedureName;
  
  public DeallocateProcedure(Session paramSession)
  {
    super(paramSession);
  }
  
  public int update()
  {
    this.session.removeProcedure(this.procedureName);
    return 0;
  }
  
  public void setProcedureName(String paramString)
  {
    this.procedureName = paramString;
  }
  
  public int getType()
  {
    return 35;
  }
}
