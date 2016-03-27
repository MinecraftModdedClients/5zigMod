package org.h2.command.dml;

import org.h2.command.ddl.SchemaCommand;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.schema.Sequence;
import org.h2.table.Column;
import org.h2.table.Table;
import org.h2.value.Value;

public class AlterSequence
  extends SchemaCommand
{
  private Table table;
  private Sequence sequence;
  private Expression start;
  private Expression increment;
  private Boolean cycle;
  private Expression minValue;
  private Expression maxValue;
  private Expression cacheSize;
  
  public AlterSequence(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setSequence(Sequence paramSequence)
  {
    this.sequence = paramSequence;
  }
  
  public boolean isTransactional()
  {
    return true;
  }
  
  public void setColumn(Column paramColumn)
  {
    this.table = paramColumn.getTable();
    this.sequence = paramColumn.getSequence();
    if (this.sequence == null) {
      throw DbException.get(90036, paramColumn.getSQL());
    }
  }
  
  public void setStartWith(Expression paramExpression)
  {
    this.start = paramExpression;
  }
  
  public void setIncrement(Expression paramExpression)
  {
    this.increment = paramExpression;
  }
  
  public void setCycle(Boolean paramBoolean)
  {
    this.cycle = paramBoolean;
  }
  
  public void setMinValue(Expression paramExpression)
  {
    this.minValue = paramExpression;
  }
  
  public void setMaxValue(Expression paramExpression)
  {
    this.maxValue = paramExpression;
  }
  
  public void setCacheSize(Expression paramExpression)
  {
    this.cacheSize = paramExpression;
  }
  
  public int update()
  {
    Database localDatabase = this.session.getDatabase();
    if (this.table != null) {
      this.session.getUser().checkRight(this.table, 15);
    }
    if (this.cycle != null) {
      this.sequence.setCycle(this.cycle.booleanValue());
    }
    if (this.cacheSize != null)
    {
      long l = this.cacheSize.optimize(this.session).getValue(this.session).getLong();
      this.sequence.setCacheSize(l);
    }
    if ((this.start != null) || (this.minValue != null) || (this.maxValue != null) || (this.increment != null))
    {
      localObject1 = getLong(this.start);
      Long localLong1 = getLong(this.minValue);
      Long localLong2 = getLong(this.maxValue);
      Long localLong3 = getLong(this.increment);
      this.sequence.modify((Long)localObject1, localLong1, localLong2, localLong3);
    }
    Object localObject1 = localDatabase.getSystemSession();
    synchronized (localObject1)
    {
      synchronized (localDatabase)
      {
        localDatabase.updateMeta((Session)localObject1, this.sequence);
        ((Session)localObject1).commit(true);
      }
    }
    return 0;
  }
  
  private Long getLong(Expression paramExpression)
  {
    if (paramExpression == null) {
      return null;
    }
    return Long.valueOf(paramExpression.optimize(this.session).getValue(this.session).getLong());
  }
  
  public int getType()
  {
    return 54;
  }
}
