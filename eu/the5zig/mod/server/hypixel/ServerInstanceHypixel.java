package eu.the5zig.mod.server.hypixel;

import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;

public class ServerInstanceHypixel
  extends ServerInstance
{
  private ServerListener listener;
  
  public ServerListener getListener()
  {
    if (this.listener == null) {
      this.listener = new HypixelListener(this);
    }
    return this.listener;
  }
  
  public String getName()
  {
    return "Hypixel.net";
  }
  
  public String getConfigName()
  {
    return "hypixel";
  }
  
  public Class<? extends GameServer> getServer()
  {
    return ServerHypixel.class;
  }
}
