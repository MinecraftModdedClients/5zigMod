package org.h2.expression;

import org.h2.command.Prepared;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueInt;

public class Rownum
  extends Expression
{
  private final Prepared prepared;
  
  public Rownum(Prepared paramPrepared)
  {
    this.prepared = paramPrepared;
  }
  
  public Value getValue(Session paramSession)
  {
    return ValueInt.get(this.prepared.getCurrentRowNumber());
  }
  
  public int getType()
  {
    return 4;
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt) {}
  
  public Expression optimize(Session paramSession)
  {
    return this;
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean) {}
  
  public int getScale()
  {
    return 0;
  }
  
  public long getPrecision()
  {
    return 10L;
  }
  
  public int getDisplaySize()
  {
    return 11;
  }
  
  public String getSQL()
  {
    return "ROWNUM()";
  }
  
  public void updateAggregate(Session paramSession) {}
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    switch (paramExpressionVisitor.getType())
    {
    case 0: 
    case 1: 
    case 2: 
    case 8: 
      return false;
    case 3: 
    case 4: 
    case 5: 
    case 6: 
    case 7: 
    case 9: 
      return true;
    }
    throw DbException.throwInternalError("type=" + paramExpressionVisitor.getType());
  }
  
  public int getCost()
  {
    return 0;
  }
}
