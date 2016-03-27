package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.OpponentStats;
import eu.the5zig.mod.server.timolia.ServerTimolia.PvP;

public class PVPOpponentGames
  extends GameModeItem<ServerTimolia.PvP>
{
  public PVPOpponentGames()
  {
    super(ServerTimolia.class, ServerTimolia.PvP.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(1);
    }
    return ((ServerTimolia.PvP)getGameMode()).getOpponentStats() != null ? Integer.valueOf(((ServerTimolia.PvP)getGameMode()).getOpponentStats().getGamesTotal()) : null;
  }
  
  public String getTranslation()
  {
    return "ingame.opponent.games_total";
  }
}
