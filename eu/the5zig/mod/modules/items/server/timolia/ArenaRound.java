package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.Arena;

public class ArenaRound
  extends GameModeItem<ServerTimolia.Arena>
{
  public ArenaRound()
  {
    super(ServerTimolia.class, ServerTimolia.Arena.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(1);
    }
    return Integer.valueOf(((ServerTimolia.Arena)getGameMode()).getRound());
  }
  
  public String getTranslation()
  {
    return "ingame.round";
  }
}
