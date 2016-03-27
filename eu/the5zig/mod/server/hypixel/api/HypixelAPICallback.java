package eu.the5zig.mod.server.hypixel.api;

public abstract class HypixelAPICallback
{
  public abstract void call(HypixelAPIResponse paramHypixelAPIResponse);
  
  public void call(HypixelAPIResponseException exception)
  {
    exception.printStackTrace();
  }
}
