package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.PvP;
import eu.the5zig.mod.server.timolia.ServerTimolia.PvPTournament;

public class TournamentBestOf
  extends GameModeItem<ServerTimolia.PvP>
{
  public TournamentBestOf()
  {
    super(ServerTimolia.class, ServerTimolia.PvP.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(3);
    }
    return (((ServerTimolia.PvP)getGameMode()).getTournament() == null) || (((ServerTimolia.PvP)getGameMode()).getTournament().getBestOf() == 0) ? null : Integer.valueOf(((ServerTimolia.PvP)getGameMode()).getTournament().getBestOf());
  }
  
  public String getTranslation()
  {
    return "ingame.best_of";
  }
}
