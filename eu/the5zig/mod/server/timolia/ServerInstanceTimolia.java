package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;

public class ServerInstanceTimolia
  extends ServerInstance
{
  private ServerListener listener;
  
  public ServerListener getListener()
  {
    if (this.listener == null) {
      this.listener = new TimoliaListener(this);
    }
    return this.listener;
  }
  
  public String getName()
  {
    return "Timolia.de";
  }
  
  public String getConfigName()
  {
    return "timolia";
  }
  
  public Class<? extends GameServer> getServer()
  {
    return ServerTimolia.class;
  }
}
