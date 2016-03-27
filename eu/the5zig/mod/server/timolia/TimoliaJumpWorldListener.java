package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.ingame.Scoreboard;
import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import org.lwjgl.util.vector.Vector3f;

public class TimoliaJumpWorldListener
  extends GameListener<ServerTimolia.JumpWorld>
{
  public TimoliaJumpWorldListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerTimolia.JumpWorld.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerTimolia.JumpWorld gameMode = (ServerTimolia.JumpWorld)getGameMode();
    if (gameMode.getState() == GameState.GAME)
    {
      if (key.equals("jumpworld.checkpoint"))
      {
        gameMode.setCheckpoints(gameMode.getCheckpoints() + 1);
        float x = (float)Math.round(The5zigMod.getVars().getPlayerPosX()) + 0.5F;
        float y = (float)Math.round(The5zigMod.getVars().getPlayerPosY());
        float z = (float)Math.round(The5zigMod.getVars().getPlayerPosZ()) + 0.5F;
        gameMode.setLastCheckpoint(new Vector3f(x, y, z));
      }
      if (key.equals("jumpworld.finished")) {
        gameMode.setState(GameState.FINISHED);
      }
    }
  }
  
  public void onTitle(String title, String subTitle)
  {
    ServerTimolia.JumpWorld gameMode = (ServerTimolia.JumpWorld)getGameMode();
    if (((ChatColor.WHITE.toString() + ChatColor.GRAY.toString() + "█ █ █" + ChatColor.RESET).equals(title)) && (subTitle == null))
    {
      gameMode.setState(GameState.STARTING);
      gameMode.setTime(System.currentTimeMillis() + 4000L);
    }
    if (((ChatColor.WHITE.toString() + ChatColor.RED.toString() + "█ " + ChatColor.GRAY + "█ █" + ChatColor.RESET).equals(title)) && (subTitle == null))
    {
      gameMode.setState(GameState.STARTING);
      gameMode.setTime(System.currentTimeMillis() + 3000L);
    }
    if (((ChatColor.WHITE.toString() + ChatColor.RED.toString() + "█ █ " + ChatColor.GRAY + "█" + ChatColor.RESET).equals(title)) && (subTitle == null))
    {
      gameMode.setState(GameState.STARTING);
      gameMode.setTime(System.currentTimeMillis() + 2000L);
    }
    if (((ChatColor.WHITE.toString() + ChatColor.RED.toString() + "█ █ █" + ChatColor.RESET).equals(title)) && (subTitle == null))
    {
      gameMode.setState(GameState.STARTING);
      gameMode.setTime(System.currentTimeMillis() + 1000L);
    }
    if (((ChatColor.WHITE.toString() + ChatColor.GREEN.toString() + "█ █ █" + ChatColor.RESET).equals(title)) && (subTitle == null))
    {
      gameMode.setState(GameState.GAME);
      gameMode.setCheckpoints(0);
      gameMode.setLastCheckpoint(new Vector3f(
        (float)The5zigMod.getVars().getPlayerPosX(), (float)The5zigMod.getVars().getPlayerPosY(), (float)The5zigMod.getVars().getPlayerPosZ()));
      gameMode.setTime(System.currentTimeMillis());
    }
  }
  
  public void onTick()
  {
    ServerTimolia.JumpWorld gameMode = (ServerTimolia.JumpWorld)getGameMode();
    Scoreboard scoreboard = The5zigMod.getVars().getScoreboard();
    if (scoreboard == null) {
      return;
    }
    if ((gameMode.getState() == GameState.GAME) && 
      (scoreboard.getTitle().contains("Fail"))) {
      gameMode.setFails(Integer.parseInt(ChatColor.stripColor(scoreboard.getTitle()).split("- | Fail")[1]));
    }
    if (((gameMode.getState() == GameState.GAME) || (gameMode.getState() == GameState.FINISHED)) && 
      (ChatColor.stripColor(scoreboard.getTitle()).equals("Timolia JumpWorld"))) {
      gameMode.setState(GameState.LOBBY);
    }
  }
}
