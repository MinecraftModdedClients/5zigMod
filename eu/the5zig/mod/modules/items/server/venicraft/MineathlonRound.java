package eu.the5zig.mod.modules.items.server.venicraft;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.venicraft.ServerVenicraft;
import eu.the5zig.mod.server.venicraft.ServerVenicraft.Mineathlon;

public class MineathlonRound
  extends GameModeItem<ServerVenicraft.Mineathlon>
{
  public MineathlonRound()
  {
    super(ServerVenicraft.class, ServerVenicraft.Mineathlon.class, new GameState[] { GameState.GAME });
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(1);
    }
    return ((ServerVenicraft.Mineathlon)getGameMode()).getRound() > 0 ? Integer.valueOf(((ServerVenicraft.Mineathlon)getGameMode()).getRound()) : null;
  }
  
  public String getTranslation()
  {
    return "ingame.round";
  }
}
