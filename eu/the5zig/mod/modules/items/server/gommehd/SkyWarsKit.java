package eu.the5zig.mod.modules.items.server.gommehd;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.gomme.ServerGommeHD;
import eu.the5zig.mod.server.gomme.ServerGommeHD.SkyWars;

public class SkyWarsKit
  extends GameModeItem<ServerGommeHD.SkyWars>
{
  public SkyWarsKit()
  {
    super(ServerGommeHD.class, ServerGommeHD.SkyWars.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    return dummy ? "Archer" : ((ServerGommeHD.SkyWars)getGameMode()).getKit();
  }
  
  public String getTranslation()
  {
    return "ingame.kit";
  }
}
