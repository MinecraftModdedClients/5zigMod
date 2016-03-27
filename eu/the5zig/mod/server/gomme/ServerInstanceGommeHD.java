package eu.the5zig.mod.server.gomme;

import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;

public class ServerInstanceGommeHD
  extends ServerInstance
{
  private ServerListener listener;
  
  public ServerListener getListener()
  {
    if (this.listener == null) {
      this.listener = new GommeHDListener(this);
    }
    return this.listener;
  }
  
  public String getName()
  {
    return "GommeHD.net";
  }
  
  public String getConfigName()
  {
    return "gommehd";
  }
  
  public Class<? extends GameServer> getServer()
  {
    return ServerGommeHD.class;
  }
}
