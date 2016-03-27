package eu.the5zig.mod.server;

public abstract interface Teamable
{
  public abstract boolean isTeamsAllowed();
  
  public abstract void setTeamsAllowed(boolean paramBoolean);
}
