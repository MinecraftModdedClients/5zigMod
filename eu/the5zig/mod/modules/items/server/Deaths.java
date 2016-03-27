package eu.the5zig.mod.modules.items.server;

import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.GameState;

public class Deaths
  extends ServerItem
{
  public Deaths()
  {
    super(new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(1);
    }
    return (getServer().getGameMode() != null) && (getServer().getGameMode().isRespawnable()) && (getServer().getGameMode().getDeaths() > 0) ? Integer.valueOf(getServer().getGameMode().getDeaths()) : null;
  }
  
  public String getTranslation()
  {
    return "ingame.deaths";
  }
}
