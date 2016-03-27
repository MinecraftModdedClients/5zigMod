package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.InTime;

public class InTimeInvincibility
  extends GameModeItem<ServerTimolia.InTime>
{
  public InTimeInvincibility()
  {
    super(ServerTimolia.class, ServerTimolia.InTime.class, new GameState[] { GameState.GAME });
  }
  
  protected Object getValue(boolean dummy)
  {
    return The5zigMod.toBoolean((dummy) || (((ServerTimolia.InTime)getGameMode()).isInvincible()));
  }
  
  public String getTranslation()
  {
    return "ingame.invincibility";
  }
}
