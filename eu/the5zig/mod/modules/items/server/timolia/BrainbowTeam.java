package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.BrainBow;

public class BrainbowTeam
  extends GameModeItem<ServerTimolia.BrainBow>
{
  public BrainbowTeam()
  {
    super(ServerTimolia.class, ServerTimolia.BrainBow.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return "Red";
    }
    return ((ServerTimolia.BrainBow)getGameMode()).getTeam();
  }
  
  public String getTranslation()
  {
    return "ingame.team";
  }
}
