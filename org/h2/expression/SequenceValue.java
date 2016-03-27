package org.h2.expression;

import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.schema.Sequence;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueLong;

public class SequenceValue
  extends Expression
{
  private final Sequence sequence;
  
  public SequenceValue(Sequence paramSequence)
  {
    this.sequence = paramSequence;
  }
  
  public Value getValue(Session paramSession)
  {
    long l = this.sequence.getNext(paramSession);
    paramSession.setLastIdentity(ValueLong.get(l));
    return ValueLong.get(l);
  }
  
  public int getType()
  {
    return 5;
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
    return "(NEXT VALUE FOR " + this.sequence.getSQL() + ")";
  }
  
  public void updateAggregate(Session paramSession) {}
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    switch (paramExpressionVisitor.getType())
    {
    case 1: 
    case 3: 
    case 6: 
    case 9: 
      return true;
    case 0: 
    case 2: 
    case 5: 
    case 8: 
      return false;
    case 4: 
      paramExpressionVisitor.addDataModificationId(this.sequence.getModificationId());
      return true;
    case 7: 
      paramExpressionVisitor.addDependency(this.sequence);
      return true;
    }
    throw DbException.throwInternalError("type=" + paramExpressionVisitor.getType());
  }
  
  public int getCost()
  {
    return 1;
  }
}
