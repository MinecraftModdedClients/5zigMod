package eu.the5zig.mod.server.venicraft;

import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;

public class ServerInstanceVenicraft
  extends ServerInstance
{
  private ServerListener listener;
  
  public ServerListener getListener()
  {
    if (this.listener == null) {
      this.listener = new VenicraftListener(this);
    }
    return this.listener;
  }
  
  public String getName()
  {
    return "Venicraft.at";
  }
  
  public String getConfigName()
  {
    return "venicraft";
  }
  
  public Class<? extends GameServer> getServer()
  {
    return ServerVenicraft.class;
  }
}
