package eu.the5zig.mod.modules.items.server.gommehd;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.gomme.ServerGommeHD;
import eu.the5zig.mod.server.gomme.ServerGommeHD.SkyWars;

public class SkyWarsCoins
  extends GameModeItem<ServerGommeHD.SkyWars>
{
  public SkyWarsCoins()
  {
    super(ServerGommeHD.class, ServerGommeHD.SkyWars.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return "51.081";
    }
    return ((ServerGommeHD.SkyWars)getGameMode()).getCoins();
  }
  
  public String getTranslation()
  {
    return "ingame.coins";
  }
}
