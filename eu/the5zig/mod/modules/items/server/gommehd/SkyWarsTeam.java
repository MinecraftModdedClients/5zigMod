package eu.the5zig.mod.modules.items.server.gommehd;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.gomme.ServerGommeHD;
import eu.the5zig.mod.server.gomme.ServerGommeHD.SkyWars;

public class SkyWarsTeam
  extends GameModeItem<ServerGommeHD.SkyWars>
{
  public SkyWarsTeam()
  {
    super(ServerGommeHD.class, ServerGommeHD.SkyWars.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(2);
    }
    return ((ServerGommeHD.SkyWars)getGameMode()).getTeam() > 0 ? Integer.valueOf(((ServerGommeHD.SkyWars)getGameMode()).getTeam()) : null;
  }
  
  public String getTranslation()
  {
    return "ingame.team";
  }
}
