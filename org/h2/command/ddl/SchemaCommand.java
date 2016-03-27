package org.h2.command.ddl;

import org.h2.engine.Session;
import org.h2.schema.Schema;

public abstract class SchemaCommand
  extends DefineCommand
{
  private final Schema schema;
  
  public SchemaCommand(Session paramSession, Schema paramSchema)
  {
    super(paramSession);
    this.schema = paramSchema;
  }
  
  protected Schema getSchema()
  {
    return this.schema;
  }
}
