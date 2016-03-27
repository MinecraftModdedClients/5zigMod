package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.schema.Constant;
import org.h2.schema.Schema;
import org.h2.value.Value;

public class CreateConstant
  extends SchemaCommand
{
  private String constantName;
  private Expression expression;
  private boolean ifNotExists;
  
  public CreateConstant(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public int update()
  {
    this.session.commit(true);
    this.session.getUser().checkAdmin();
    Database localDatabase = this.session.getDatabase();
    if (getSchema().findConstant(this.constantName) != null)
    {
      if (this.ifNotExists) {
        return 0;
      }
      throw DbException.get(90114, this.constantName);
    }
    int i = getObjectId();
    Constant localConstant = new Constant(getSchema(), i, this.constantName);
    this.expression = this.expression.optimize(this.session);
    Value localValue = this.expression.getValue(this.session);
    localConstant.setValue(localValue);
    localDatabase.addSchemaObject(this.session, localConstant);
    return 0;
  }
  
  public void setConstantName(String paramString)
  {
    this.constantName = paramString;
  }
  
  public void setExpression(Expression paramExpression)
  {
    this.expression = paramExpression;
  }
  
  public int getType()
  {
    return 23;
  }
}
