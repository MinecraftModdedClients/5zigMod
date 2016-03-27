package eu.the5zig.mod.server.mineplex;

import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;

public class ServerInstanceMineplex
  extends ServerInstance
{
  private ServerListener listener;
  
  public ServerListener getListener()
  {
    if (this.listener == null) {
      this.listener = new MineplexListener(this);
    }
    return this.listener;
  }
  
  public String getName()
  {
    return "Mineplex.com";
  }
  
  public String getConfigName()
  {
    return "mineplex";
  }
  
  public Class<? extends GameServer> getServer()
  {
    return ServerMineplex.class;
  }
}
