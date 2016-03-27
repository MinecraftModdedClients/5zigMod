package org.h2.command.ddl;

import java.util.ArrayList;
import org.h2.command.Prepared;
import org.h2.engine.Procedure;
import org.h2.engine.Session;
import org.h2.expression.Parameter;
import org.h2.util.New;

public class PrepareProcedure
  extends DefineCommand
{
  private String procedureName;
  private Prepared prepared;
  
  public PrepareProcedure(Session paramSession)
  {
    super(paramSession);
  }
  
  public void checkParameters() {}
  
  public int update()
  {
    Procedure localProcedure = new Procedure(this.procedureName, this.prepared);
    this.prepared.setParameterList(this.parameters);
    this.prepared.setPrepareAlways(this.prepareAlways);
    this.prepared.prepare();
    this.session.addProcedure(localProcedure);
    return 0;
  }
  
  public void setProcedureName(String paramString)
  {
    this.procedureName = paramString;
  }
  
  public void setPrepared(Prepared paramPrepared)
  {
    this.prepared = paramPrepared;
  }
  
  public ArrayList<Parameter> getParameters()
  {
    return New.arrayList();
  }
  
  public int getType()
  {
    return 51;
  }
}
