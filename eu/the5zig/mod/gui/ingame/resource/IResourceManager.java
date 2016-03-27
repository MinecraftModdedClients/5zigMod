package eu.the5zig.mod.gui.ingame.resource;

public abstract interface IResourceManager
{
  public abstract void updateOwnPlayerTextures();
  
  public abstract Object getOwnCapeLocation();
  
  public abstract void cleanupTextures();
}
