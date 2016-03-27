package eu.the5zig.mod.modules.items.server.bergwerk;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.modules.items.server.LargeTextItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.bergwerk.ServerBergwerk;
import eu.the5zig.mod.server.bergwerk.ServerBergwerk.Duel;

public class DuelTeleportMessage
  extends LargeTextItem<ServerBergwerk.Duel>
{
  public DuelTeleportMessage()
  {
    super(ServerBergwerk.class, ServerBergwerk.Duel.class, new GameState[] { GameState.GAME });
  }
  
  protected String getText()
  {
    if ((((ServerBergwerk.Duel)getGameMode()).getTeleporterTimer() != -1L) && (((ServerBergwerk.Duel)getGameMode()).getTeleporterTimer() - System.currentTimeMillis() > 0L)) {
      return I18n.translate("ingame.duel_teleporter");
    }
    return null;
  }
}
