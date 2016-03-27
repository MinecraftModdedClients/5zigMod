package eu.the5zig.mod.config;

import eu.the5zig.mod.server.Server;
import java.util.ArrayList;
import java.util.List;

public class LastServer
{
  private List<Server> lastServers;
  private final int MAX_SERVER_COUNT = 5;
  
  public Server getLastServer()
  {
    return (this.lastServers == null) || (this.lastServers.isEmpty()) ? null : (Server)this.lastServers.get(0);
  }
  
  public void setLastServer(Server lastServer)
  {
    if (this.lastServers == null) {
      this.lastServers = new ArrayList();
    }
    this.lastServers.remove(lastServer);
    this.lastServers.add(0, lastServer);
    while (this.lastServers.size() > 5) {
      this.lastServers.remove(5);
    }
  }
}
