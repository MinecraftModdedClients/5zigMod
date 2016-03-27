package eu.the5zig.mod.modules.items.server.bergwerk;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.bergwerk.ServerBergwerk;
import eu.the5zig.mod.server.bergwerk.ServerBergwerk.Duel;

public class DuelRespawn
  extends GameModeItem<ServerBergwerk.Duel>
{
  public DuelRespawn()
  {
    super(ServerBergwerk.class, ServerBergwerk.Duel.class, new GameState[] { GameState.GAME });
  }
  
  protected Object getValue(boolean dummy)
  {
    return (dummy) || (((ServerBergwerk.Duel)getGameMode()).isCanRespawn()) ? I18n.translate("ingame.can_respawn.true") : I18n.translate("ingame.can_respawn.false");
  }
  
  public String getTranslation()
  {
    return "ingame.can_respawn";
  }
}
