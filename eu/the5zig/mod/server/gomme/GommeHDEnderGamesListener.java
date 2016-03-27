package eu.the5zig.mod.server.gomme;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.render.GuiIngame;
import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;

public class GommeHDEnderGamesListener
  extends GameListener<ServerGommeHD.EnderGames>
{
  public GommeHDEnderGamesListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerGommeHD.EnderGames.class);
  }
  
  public void onGameModeJoin()
  {
    getServerInstance().getListener().sendAndIgnore("/coins", "eg.coins");
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerGommeHD.EnderGames gameMode = (ServerGommeHD.EnderGames)((ServerGommeHD)The5zigMod.getDataManager().getServer()).getGameMode();
    if (key.equals("eg.coins")) {
      gameMode.setCoins(match.get(0));
    }
    if (gameMode.getState() == GameState.LOBBY)
    {
      if (key.equals("eg.lobby.starting")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("eg.lobby.start")) {
        gameMode.setState(GameState.STARTING);
      }
    }
    if (((gameMode.getState() == GameState.LOBBY) || (gameMode.getState() == GameState.STARTING)) && 
      (key.equals("eg.kit"))) {
      gameMode.setKit(match.get(0));
    }
    if (gameMode.getState() == GameState.STARTING)
    {
      if (key.equals("eg.starting")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("eg.start"))
      {
        gameMode.setState(GameState.PREGAME);
        gameMode.setTime(System.currentTimeMillis() + 22000L);
      }
    }
    if (gameMode.getState() == GameState.PREGAME)
    {
      if (key.equals("eg.invincibility")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("eg.invincibility_off"))
      {
        gameMode.setState(GameState.GAME);
        gameMode.setTime(System.currentTimeMillis() - 74000L);
      }
    }
    if ((gameMode.getState() == GameState.GAME) && 
      (key.equals("eg.win")))
    {
      gameMode.setWinner(match.get(0));
      gameMode.setState(GameState.FINISHED);
    }
    if (((gameMode.getState() == GameState.PREGAME) || (gameMode.getState() == GameState.GAME)) && 
      (key.equals("eg.track")) && 
      (The5zigMod.getConfig().getBool("showCompassTarget"))) {
      The5zigMod.getGuiIngame().showTextAboveHotbar("Tracking: " + match.get(0) + " (" + match.get(1) + ")");
    }
  }
}
