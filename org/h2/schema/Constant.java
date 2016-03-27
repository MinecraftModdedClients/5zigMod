package org.h2.schema;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.ValueExpression;
import org.h2.message.DbException;
import org.h2.table.Table;
import org.h2.value.Value;

public class Constant
  extends SchemaObjectBase
{
  private Value value;
  private ValueExpression expression;
  
  public Constant(Schema paramSchema, int paramInt, String paramString)
  {
    initSchemaObjectBase(paramSchema, paramInt, paramString, "schema");
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  public String getCreateSQL()
  {
    return "CREATE CONSTANT " + getSQL() + " VALUE " + this.value.getSQL();
  }
  
  public int getType()
  {
    return 11;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    this.database.removeMeta(paramSession, getId());
    invalidate();
  }
  
  public void checkRename() {}
  
  public void setValue(Value paramValue)
  {
    this.value = paramValue;
    this.expression = ValueExpression.get(paramValue);
  }
  
  public ValueExpression getValue()
  {
    return this.expression;
  }
}
