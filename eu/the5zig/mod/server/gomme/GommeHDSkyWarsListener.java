package eu.the5zig.mod.server.gomme;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.ingame.Scoreboard;
import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.HashMap;

public class GommeHDSkyWarsListener
  extends GameListener<ServerGommeHD.SkyWars>
{
  public GommeHDSkyWarsListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerGommeHD.SkyWars.class);
  }
  
  public void onGameModeJoin()
  {
    getServerInstance().getListener().sendAndIgnore("/coins", "sw.coins");
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerGommeHD.SkyWars gameMode = (ServerGommeHD.SkyWars)getGameMode();
    if (key.equals("sw.coins")) {
      gameMode.setCoins(match.get(0));
    }
    if (gameMode.getState() == GameState.LOBBY)
    {
      if (key.equals("sw.lobby.starting")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("sw.lobby.start"))
      {
        gameMode.setState(GameState.STARTING);
        gameMode.setTime(System.currentTimeMillis() + 3000L);
      }
      if (key.equals("sw.team.added")) {
        gameMode.setTeam(Integer.parseInt(match.get(0)));
      }
      if (key.equals("sw.kit")) {
        gameMode.setKit(match.get(0));
      }
    }
    if (gameMode.getState() == GameState.STARTING)
    {
      if (key.equals("sw.starting")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("sw.start"))
      {
        gameMode.setState(GameState.PREGAME);
        gameMode.setTime(System.currentTimeMillis() + 30000L);
        Scoreboard scoreboard = The5zigMod.getVars().getScoreboard();
        gameMode.setKit(ChatColor.stripColor((String)scoreboard.getLines().get(Integer.valueOf(1))));
      }
    }
    if (gameMode.getState() == GameState.PREGAME)
    {
      if (key.equals("sw.invincibility")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("sw.invincibility_off"))
      {
        gameMode.setState(GameState.GAME);
        gameMode.setTime(System.currentTimeMillis() - 30000L);
      }
    }
    if ((gameMode.getState() == GameState.GAME) && 
      (key.equals("sw.win")))
    {
      gameMode.setWinner(match.get(0));
      gameMode.setState(GameState.FINISHED);
    }
    if (((gameMode.getState() == GameState.STARTING) || (gameMode.getState() == GameState.PREGAME) || (gameMode.getState() == GameState.GAME)) && 
      (key.equals("sw.team"))) {
      gameMode.setTeam(Integer.parseInt(match.get(0)));
    }
  }
}
