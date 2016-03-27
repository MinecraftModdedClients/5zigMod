package eu.the5zig.mod.server.bergwerk;

import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;

public class ServerInstanceBergwerk
  extends ServerInstance
{
  private ServerListener listener;
  
  public ServerListener getListener()
  {
    if (this.listener == null) {
      this.listener = new BergwerkListener(this);
    }
    return this.listener;
  }
  
  public String getName()
  {
    return "Bergwerklabs.de";
  }
  
  public String getConfigName()
  {
    return "bergwerk";
  }
  
  public Class<? extends GameServer> getServer()
  {
    return ServerBergwerk.class;
  }
}
