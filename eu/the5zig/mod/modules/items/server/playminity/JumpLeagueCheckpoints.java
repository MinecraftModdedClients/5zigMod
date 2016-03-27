package eu.the5zig.mod.modules.items.server.playminity;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.playminity.ServerPlayMinity;
import eu.the5zig.mod.server.playminity.ServerPlayMinity.JumpLeague;

public class JumpLeagueCheckpoints
  extends GameModeItem<ServerPlayMinity.JumpLeague>
{
  public JumpLeagueCheckpoints()
  {
    super(ServerPlayMinity.class, ServerPlayMinity.JumpLeague.class, new GameState[] { GameState.GAME });
  }
  
  protected Object getValue(boolean dummy)
  {
    return ((ServerPlayMinity.JumpLeague)getGameMode()).getCheckPoint() + "/" + ((ServerPlayMinity.JumpLeague)getGameMode()).getMaxCheckPoints();
  }
  
  public String getTranslation()
  {
    return "ingame.checkpoints";
  }
}
