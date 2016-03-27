package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.schema.Sequence;
import org.h2.value.Value;

public class CreateSequence
  extends SchemaCommand
{
  private String sequenceName;
  private boolean ifNotExists;
  private boolean cycle;
  private Expression minValue;
  private Expression maxValue;
  private Expression start;
  private Expression increment;
  private Expression cacheSize;
  private boolean belongsToTable;
  
  public CreateSequence(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setSequenceName(String paramString)
  {
    this.sequenceName = paramString;
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public void setCycle(boolean paramBoolean)
  {
    this.cycle = paramBoolean;
  }
  
  public int update()
  {
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    if (getSchema().findSequence(this.sequenceName) != null)
    {
      if (this.ifNotExists) {
        return 0;
      }
      throw DbException.get(90035, this.sequenceName);
    }
    int i = getObjectId();
    Long localLong1 = getLong(this.start);
    Long localLong2 = getLong(this.increment);
    Long localLong3 = getLong(this.cacheSize);
    Long localLong4 = getLong(this.minValue);
    Long localLong5 = getLong(this.maxValue);
    Sequence localSequence = new Sequence(getSchema(), i, this.sequenceName, localLong1, localLong2, localLong3, localLong4, localLong5, this.cycle, this.belongsToTable);
    
    localDatabase.addSchemaObject(this.session, localSequence);
    return 0;
  }
  
  private Long getLong(Expression paramExpression)
  {
    if (paramExpression == null) {
      return null;
    }
    return Long.valueOf(paramExpression.optimize(this.session).getValue(this.session).getLong());
  }
  
  public void setStartWith(Expression paramExpression)
  {
    this.start = paramExpression;
  }
  
  public void setIncrement(Expression paramExpression)
  {
    this.increment = paramExpression;
  }
  
  public void setMinValue(Expression paramExpression)
  {
    this.minValue = paramExpression;
  }
  
  public void setMaxValue(Expression paramExpression)
  {
    this.maxValue = paramExpression;
  }
  
  public void setBelongsToTable(boolean paramBoolean)
  {
    this.belongsToTable = paramBoolean;
  }
  
  public void setCacheSize(Expression paramExpression)
  {
    this.cacheSize = paramExpression;
  }
  
  public int getType()
  {
    return 29;
  }
}
