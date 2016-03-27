package eu.the5zig.mod.modules.items.server.gommehd;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.gomme.ServerGommeHD;
import eu.the5zig.mod.server.gomme.ServerGommeHD.EnderGames;

public class EnderGamesCoins
  extends GameModeItem<ServerGommeHD.EnderGames>
{
  public EnderGamesCoins()
  {
    super(ServerGommeHD.class, ServerGommeHD.EnderGames.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return "10.415";
    }
    return ((ServerGommeHD.EnderGames)getGameMode()).getCoins();
  }
  
  public String getTranslation()
  {
    return "ingame.coins";
  }
}
