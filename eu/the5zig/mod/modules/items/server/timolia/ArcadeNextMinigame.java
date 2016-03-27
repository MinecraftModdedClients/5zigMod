package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.Arcade;

public class ArcadeNextMinigame
  extends GameModeItem<ServerTimolia.Arcade>
{
  public ArcadeNextMinigame()
  {
    super(ServerTimolia.class, ServerTimolia.Arcade.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return "PvP";
    }
    return ((ServerTimolia.Arcade)getGameMode()).getNextMiniGame();
  }
  
  public String getTranslation()
  {
    return "ingame.next_minigame";
  }
}
