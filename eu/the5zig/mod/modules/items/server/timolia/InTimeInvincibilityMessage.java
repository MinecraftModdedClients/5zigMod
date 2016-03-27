package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.modules.items.server.LargeTextItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.InTime;

public class InTimeInvincibilityMessage
  extends LargeTextItem<ServerTimolia.InTime>
{
  public InTimeInvincibilityMessage()
  {
    super(ServerTimolia.class, ServerTimolia.InTime.class, new GameState[] { GameState.GAME });
  }
  
  protected String getText()
  {
    if ((((ServerTimolia.InTime)getGameMode()).getInvincibleTimer() != -1L) && (((ServerTimolia.InTime)getGameMode()).getInvincibleTimer() - System.currentTimeMillis() > 0L)) {
      return I18n.translate("ingame.no_longer_invincible");
    }
    if ((((ServerTimolia.InTime)getGameMode()).getSpawnRegenerationTimer() != -1L) && (((ServerTimolia.InTime)getGameMode()).getSpawnRegenerationTimer() - System.currentTimeMillis() > 0L)) {
      return I18n.translate("ingame.now_spawn_regeneration");
    }
    if ((((ServerTimolia.InTime)getGameMode()).getLootTimer() != -1L) && (((ServerTimolia.InTime)getGameMode()).getLootTimer() - System.currentTimeMillis() > 0L)) {
      return I18n.translate("ingame.loot_spawned");
    }
    return null;
  }
}
