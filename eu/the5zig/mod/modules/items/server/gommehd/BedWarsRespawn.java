package eu.the5zig.mod.modules.items.server.gommehd;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.gomme.ServerGommeHD;
import eu.the5zig.mod.server.gomme.ServerGommeHD.BedWars;

public class BedWarsRespawn
  extends GameModeItem<ServerGommeHD.BedWars>
{
  public BedWarsRespawn()
  {
    super(ServerGommeHD.class, ServerGommeHD.BedWars.class, new GameState[] { GameState.GAME });
  }
  
  protected Object getValue(boolean dummy)
  {
    return (dummy) || (((ServerGommeHD.BedWars)getGameMode()).isCanRespawn()) ? I18n.translate("ingame.can_respawn.true") : I18n.translate("ingame.can_respawn.false");
  }
  
  public String getTranslation()
  {
    return "ingame.can_respawn";
  }
}
