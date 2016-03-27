package eu.the5zig.mod.modules.items.server.gommehd;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.gomme.ServerGommeHD;
import eu.the5zig.mod.server.gomme.ServerGommeHD.BedWars;

public class BedWarsTeam
  extends GameModeItem<ServerGommeHD.BedWars>
{
  public BedWarsTeam()
  {
    super(ServerGommeHD.class, ServerGommeHD.BedWars.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    return dummy ? "Red" : ((ServerGommeHD.BedWars)getGameMode()).getTeam();
  }
  
  public String getTranslation()
  {
    return "ingame.team";
  }
}
