package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.JumpWorld;

public class JumpWorldCheckpoint
  extends GameModeItem<ServerTimolia.JumpWorld>
{
  public JumpWorldCheckpoint()
  {
    super(ServerTimolia.class, ServerTimolia.JumpWorld.class, new GameState[] { GameState.GAME, GameState.FINISHED });
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(1);
    }
    return Integer.valueOf(((ServerTimolia.JumpWorld)getGameMode()).getCheckpoints());
  }
  
  public String getTranslation()
  {
    return "ingame.checkpoint";
  }
}
