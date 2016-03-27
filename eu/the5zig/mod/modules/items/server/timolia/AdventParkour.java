package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.Advent;

public class AdventParkour
  extends GameModeItem<ServerTimolia.Advent>
{
  public AdventParkour()
  {
    super(ServerTimolia.class, ServerTimolia.Advent.class, new GameState[] { GameState.GAME, GameState.FINISHED });
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return "Christmas";
    }
    return ((ServerTimolia.Advent)getGameMode()).getParkourName();
  }
  
  public String getTranslation()
  {
    return "ingame.parkour";
  }
}
