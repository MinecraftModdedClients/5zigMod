package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.PvP;

public class PVPOpponent
  extends GameModeItem<ServerTimolia.PvP>
{
  public PVPOpponent()
  {
    super(ServerTimolia.class, ServerTimolia.PvP.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return "5zig";
    }
    return ((ServerTimolia.PvP)getGameMode()).getOpponent();
  }
  
  public String getTranslation()
  {
    return "ingame.opponent";
  }
}
