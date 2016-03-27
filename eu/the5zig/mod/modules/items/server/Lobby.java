package eu.the5zig.mod.modules.items.server;

import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.GameState;

public class Lobby
  extends ServerItem
{
  public Lobby()
  {
    super(new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return "Lobby01";
    }
    return getServer().getLobby();
  }
  
  public String getTranslation()
  {
    return "ingame.lobby";
  }
}
