package org.h2.command.dml;

import java.util.ArrayList;
import org.h2.command.Prepared;
import org.h2.engine.Procedure;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.expression.Parameter;
import org.h2.result.ResultInterface;
import org.h2.util.New;

public class ExecuteProcedure
  extends Prepared
{
  private final ArrayList<Expression> expressions = New.arrayList();
  private Procedure procedure;
  
  public ExecuteProcedure(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setProcedure(Procedure paramProcedure)
  {
    this.procedure = paramProcedure;
  }
  
  public void setExpression(int paramInt, Expression paramExpression)
  {
    this.expressions.add(paramInt, paramExpression);
  }
  
  private void setParameters()
  {
    Prepared localPrepared = this.procedure.getPrepared();
    ArrayList localArrayList = localPrepared.getParameters();
    for (int i = 0; (localArrayList != null) && (i < localArrayList.size()) && (i < this.expressions.size()); i++)
    {
      Expression localExpression = (Expression)this.expressions.get(i);
      Parameter localParameter = (Parameter)localArrayList.get(i);
      localParameter.setValue(localExpression.getValue(this.session));
    }
  }
  
  public boolean isQuery()
  {
    Prepared localPrepared = this.procedure.getPrepared();
    return localPrepared.isQuery();
  }
  
  public int update()
  {
    setParameters();
    Prepared localPrepared = this.procedure.getPrepared();
    return localPrepared.update();
  }
  
  public ResultInterface query(int paramInt)
  {
    setParameters();
    Prepared localPrepared = this.procedure.getPrepared();
    return localPrepared.query(paramInt);
  }
  
  public boolean isTransactional()
  {
    return true;
  }
  
  public ResultInterface queryMeta()
  {
    Prepared localPrepared = this.procedure.getPrepared();
    return localPrepared.queryMeta();
  }
  
  public int getType()
  {
    return 59;
  }
}
