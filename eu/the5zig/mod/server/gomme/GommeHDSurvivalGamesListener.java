package eu.the5zig.mod.server.gomme;

import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class GommeHDSurvivalGamesListener
  extends GameListener<ServerGommeHD.SurvivalGames>
{
  public GommeHDSurvivalGamesListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerGommeHD.SurvivalGames.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerGommeHD.SurvivalGames gameMode = (ServerGommeHD.SurvivalGames)getGameMode();
    if (gameMode.getState() == GameState.LOBBY)
    {
      if (key.equals("sg.lobby.starting")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("sg.lobby.start")) {
        gameMode.setState(GameState.STARTING);
      }
    }
    if (gameMode.getState() == GameState.STARTING)
    {
      if (key.equals("sg.starting")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("sg.start"))
      {
        gameMode.setState(GameState.PREGAME);
        gameMode.setTime(System.currentTimeMillis() + 22000L);
      }
    }
    if ((gameMode.getState() == GameState.PREGAME) && 
      (key.equals("sg.invincibility")))
    {
      gameMode.setState(GameState.GAME);
      gameMode.setTime(System.currentTimeMillis());
    }
    if (gameMode.getState() == GameState.GAME)
    {
      if (key.equals("sg.deathmatch")) {
        gameMode.setDeathmatchTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000);
      }
      if (key.equals("sg.win"))
      {
        gameMode.setWinner(match.get(0));
        gameMode.setState(GameState.FINISHED);
      }
    }
  }
}
