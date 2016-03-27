package eu.the5zig.mod.modules.items.server.playminity;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.playminity.ServerPlayMinity;
import eu.the5zig.mod.server.playminity.ServerPlayMinity.JumpLeague;

public class JumpLeagueLives
  extends GameModeItem<ServerPlayMinity.JumpLeague>
{
  public JumpLeagueLives()
  {
    super(ServerPlayMinity.class, ServerPlayMinity.JumpLeague.class, new GameState[] { GameState.ENDGAME });
  }
  
  protected Object getValue(boolean dummy)
  {
    return Integer.valueOf(dummy ? 1 : ((ServerPlayMinity.JumpLeague)getGameMode()).getLives());
  }
  
  public String getTranslation()
  {
    return "ingame.lives";
  }
}
