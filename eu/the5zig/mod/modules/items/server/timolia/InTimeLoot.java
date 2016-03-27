package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.InTime;

public class InTimeLoot
  extends GameModeItem<ServerTimolia.InTime>
{
  public InTimeLoot()
  {
    super(ServerTimolia.class, ServerTimolia.InTime.class, new GameState[] { GameState.GAME });
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return shorten(30.0D);
    }
    return (((ServerTimolia.InTime)getGameMode()).getLoot() != -1L) && (((ServerTimolia.InTime)getGameMode()).getLoot() - System.currentTimeMillis() > 0L) ? shorten((((ServerTimolia.InTime)getGameMode()).getLoot() - System.currentTimeMillis()) / 1000.0D) : null;
  }
  
  public String getTranslation()
  {
    return "ingame.loot";
  }
}
