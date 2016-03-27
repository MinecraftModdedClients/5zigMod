package eu.the5zig.mod.modules.items.server.bergwerk;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.bergwerk.ServerBergwerk;
import eu.the5zig.mod.server.bergwerk.ServerBergwerk.Duel;

public class DuelTeam
  extends GameModeItem<ServerBergwerk.Duel>
{
  public DuelTeam()
  {
    super(ServerBergwerk.class, ServerBergwerk.Duel.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    return dummy ? "Red" : ((ServerBergwerk.Duel)getGameMode()).getTeam();
  }
  
  public String getTranslation()
  {
    return "ingame.team";
  }
}
