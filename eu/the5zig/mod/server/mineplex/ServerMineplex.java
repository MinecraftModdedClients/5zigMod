package eu.the5zig.mod.server.mineplex;

import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;

public class ServerMineplex
  extends GameServer
{
  public ServerMineplex() {}
  
  public ServerMineplex(String host, int port)
  {
    super(host, port);
  }
  
  public class DragonEscape
    extends GameMode
  {
    public DragonEscape() {}
    
    public String getName()
    {
      return "DragonEscape";
    }
  }
}
