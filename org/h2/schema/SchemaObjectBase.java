package org.h2.schema;

import org.h2.engine.DbObjectBase;

public abstract class SchemaObjectBase
  extends DbObjectBase
  implements SchemaObject
{
  private Schema schema;
  
  protected void initSchemaObjectBase(Schema paramSchema, int paramInt, String paramString1, String paramString2)
  {
    initDbObjectBase(paramSchema.getDatabase(), paramInt, paramString1, paramString2);
    this.schema = paramSchema;
  }
  
  public Schema getSchema()
  {
    return this.schema;
  }
  
  public String getSQL()
  {
    return this.schema.getSQL() + "." + super.getSQL();
  }
  
  public boolean isHidden()
  {
    return false;
  }
}
