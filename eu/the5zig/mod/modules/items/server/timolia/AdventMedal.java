package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.Advent;
import eu.the5zig.util.minecraft.ChatColor;

public class AdventMedal
  extends GameModeItem<ServerTimolia.Advent>
{
  public AdventMedal()
  {
    super(ServerTimolia.class, ServerTimolia.Advent.class, new GameState[] { GameState.GAME, GameState.FINISHED });
  }
  
  protected Object getValue(boolean dummy)
  {
    String medal = null;
    if (dummy) {
      return "✦";
    }
    long currentTime = System.currentTimeMillis();
    long l = ((ServerTimolia.Advent)getGameMode()).getState() == GameState.GAME ? currentTime - ((ServerTimolia.Advent)getGameMode()).getTime() : ((ServerTimolia.Advent)getGameMode()).getTime();
    if (l <= ((ServerTimolia.Advent)getGameMode()).getTimeGold()) {
      medal = ChatColor.GOLD.toString();
    } else if (l <= ((ServerTimolia.Advent)getGameMode()).getTimeSilver()) {
      medal = ChatColor.GRAY.toString();
    } else if (l <= ((ServerTimolia.Advent)getGameMode()).getTimeBronze()) {
      medal = ChatColor.DARK_AQUA.toString();
    }
    if (medal != null)
    {
      medal = medal + "✦";
      return medal;
    }
    return null;
  }
  
  public String getTranslation()
  {
    return "ingame.medal";
  }
}
