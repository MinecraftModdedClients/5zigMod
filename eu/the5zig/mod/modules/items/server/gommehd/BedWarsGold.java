package eu.the5zig.mod.modules.items.server.gommehd;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.gomme.ServerGommeHD;
import eu.the5zig.mod.server.gomme.ServerGommeHD.BedWars;

public class BedWarsGold
  extends GameModeItem<ServerGommeHD.BedWars>
{
  public BedWarsGold()
  {
    super(ServerGommeHD.class, ServerGommeHD.BedWars.class, new GameState[] { GameState.GAME });
  }
  
  protected Object getValue(boolean dummy)
  {
    return dummy ? shorten(10.0D) : shorten(30.0D - (System.currentTimeMillis() - ((ServerGommeHD.BedWars)getGameMode()).getTime()) % 30000L / 1000.0D);
  }
  
  public String getTranslation()
  {
    return "ingame.gold";
  }
}
