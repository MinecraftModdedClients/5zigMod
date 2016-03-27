package org.h2.engine;

import org.h2.command.Prepared;

public class Procedure
{
  private final String name;
  private final Prepared prepared;
  
  public Procedure(String paramString, Prepared paramPrepared)
  {
    this.name = paramString;
    this.prepared = paramPrepared;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public Prepared getPrepared()
  {
    return this.prepared;
  }
}
