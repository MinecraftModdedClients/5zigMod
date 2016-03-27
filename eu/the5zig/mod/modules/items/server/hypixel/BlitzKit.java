package eu.the5zig.mod.modules.items.server.hypixel;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.hypixel.ServerHypixel;
import eu.the5zig.mod.server.hypixel.ServerHypixel.Blitz;

public class BlitzKit
  extends GameModeItem<ServerHypixel.Blitz>
{
  public BlitzKit()
  {
    super(ServerHypixel.class, ServerHypixel.Blitz.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    return dummy ? "Starter Kit" : ((ServerHypixel.Blitz)getGameMode()).getKit();
  }
  
  public String getTranslation()
  {
    return "ingame.kit";
  }
}
