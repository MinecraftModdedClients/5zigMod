package eu.the5zig.mod.server.playminity;

import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;

public class ServerInstancePlayMinity
  extends ServerInstance
{
  private ServerListener listener;
  
  public ServerListener getListener()
  {
    if (this.listener == null) {
      this.listener = new PlayMinityListener(this);
    }
    return this.listener;
  }
  
  public String getName()
  {
    return "PlayMinity.com";
  }
  
  public String getConfigName()
  {
    return "playminity";
  }
  
  public Class<? extends GameServer> getServer()
  {
    return ServerPlayMinity.class;
  }
}
