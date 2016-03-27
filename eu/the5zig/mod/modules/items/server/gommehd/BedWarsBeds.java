package eu.the5zig.mod.modules.items.server.gommehd;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.gomme.ServerGommeHD;
import eu.the5zig.mod.server.gomme.ServerGommeHD.BedWars;

public class BedWarsBeds
  extends GameModeItem<ServerGommeHD.BedWars>
{
  public BedWarsBeds()
  {
    super(ServerGommeHD.class, ServerGommeHD.BedWars.class, new GameState[] { GameState.GAME });
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(5);
    }
    return ((ServerGommeHD.BedWars)getGameMode()).getBeds() > 0 ? Integer.valueOf(((ServerGommeHD.BedWars)getGameMode()).getBeds()) : null;
  }
  
  public String getTranslation()
  {
    return "ingame.beds";
  }
}
