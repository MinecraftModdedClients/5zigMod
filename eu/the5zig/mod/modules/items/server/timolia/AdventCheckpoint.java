package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.Advent;

public class AdventCheckpoint
  extends GameModeItem<ServerTimolia.Advent>
{
  public AdventCheckpoint()
  {
    super(ServerTimolia.class, ServerTimolia.Advent.class, new GameState[] { GameState.GAME, GameState.FINISHED });
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(1);
    }
    return Integer.valueOf(((ServerTimolia.Advent)getGameMode()).getCurrentCheckpoint());
  }
  
  public String getTranslation()
  {
    return "ingame.checkpoint";
  }
}
