package eu.the5zig.mod.modules.items.server.hypixel;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.hypixel.ServerHypixel;
import eu.the5zig.mod.server.hypixel.ServerHypixel.Blitz;

public class BlitzStar
  extends GameModeItem<ServerHypixel.Blitz>
{
  public BlitzStar()
  {
    super(ServerHypixel.class, ServerHypixel.Blitz.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return shorten(10.0D);
    }
    if ((((ServerHypixel.Blitz)getGameMode()).getWinner() == null) && (((ServerHypixel.Blitz)getGameMode()).getStar() != -1L) && (((ServerHypixel.Blitz)getGameMode()).getStar() - System.currentTimeMillis() > 0L)) {
      return shorten((((ServerHypixel.Blitz)getGameMode()).getStar() - System.currentTimeMillis()) / 1000.0D);
    }
    return null;
  }
  
  public String getTranslation()
  {
    return "ingame.star";
  }
}
