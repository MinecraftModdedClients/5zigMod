package eu.the5zig.mod.modules.items.server.venicraft;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.venicraft.ServerVenicraft;
import eu.the5zig.mod.server.venicraft.ServerVenicraft.Mineathlon;

public class MineathlonDiscipline
  extends GameModeItem<ServerVenicraft.Mineathlon>
{
  public MineathlonDiscipline()
  {
    super(ServerVenicraft.class, ServerVenicraft.Mineathlon.class, new GameState[] { GameState.STARTING, GameState.GAME, GameState.ENDGAME });
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return "1 (Jump'n'Run)";
    }
    return ((ServerVenicraft.Mineathlon)getGameMode()).getDiscipline();
  }
  
  public String getTranslation()
  {
    return "ingame.discipline";
  }
}
