package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;

public class TimoliaAdventListener
  extends GameListener<ServerTimolia.Advent>
{
  public TimoliaAdventListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerTimolia.Advent.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerTimolia.Advent gameMode = (ServerTimolia.Advent)getGameMode();
    if ((key.equals("advent.start")) || (key.equals("advent.restart")))
    {
      gameMode.setState(GameState.GAME);
      if (key.equals("advent.start")) {
        gameMode.setParkourName(match.get(0));
      }
      gameMode.setCurrentCheckpoint(1);
      gameMode.setTime(System.currentTimeMillis());
    }
    if (((gameMode.getState() == GameState.GAME) || (gameMode.getState() == GameState.FINISHED)) && (key.equals("advent.checkpoint"))) {
      gameMode.setCurrentCheckpoint(gameMode.getCurrentCheckpoint() + 1);
    }
    if (key.equals("advent.checkpoint.time")) {
      if (gameMode.getState() == GameState.GAME) {
        gameMode.setTime(System.currentTimeMillis() - Utils.parseTimeFormatToMillis(match.get(0), "mm:ss.SSS"));
      } else if (gameMode.getState() == GameState.FINISHED) {
        gameMode.setTime(Utils.parseTimeFormatToMillis(match.get(0), "mm:ss"));
      }
    }
    if ((gameMode.getState() == GameState.GAME) && ((key.equals("advent.finished")) || (key.equals("advent.already_finished")))) {
      gameMode.setState(GameState.FINISHED);
    }
    if (key.equals("advent.to_spawn")) {
      gameMode.setState(GameState.LOBBY);
    }
  }
  
  public boolean onServerChat(String message)
  {
    ServerTimolia.Advent gameMode = (ServerTimolia.Advent)getGameMode();
    if (matches(message, ChatColor.GOLD)) {
      gameMode.setTimeGold(parseTime(message));
    } else if (matches(message, ChatColor.GRAY)) {
      gameMode.setTimeSilver(parseTime(message));
    } else if (matches(message, ChatColor.DARK_AQUA)) {
      gameMode.setTimeBronze(parseTime(message));
    }
    return false;
  }
  
  private boolean matches(String message, ChatColor chatColor)
  {
    return (message.startsWith(ChatColor.DARK_BLUE + "│" + ChatColor.GRAY + "   " + chatColor + "✦ ")) || (message.startsWith(ChatColor.DARK_BLUE + "└" + ChatColor.GRAY + "  " + chatColor + "✦ "));
  }
  
  private long parseTime(String message)
  {
    String time = ChatColor.stripColor(message).split("✦ | Minuten")[1];
    return Utils.parseTimeFormatToMillis(time, "mm:ss");
  }
}
