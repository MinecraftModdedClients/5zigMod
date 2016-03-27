package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class TimoliaTSpieleListener
  extends GameListener<ServerTimolia.TSpiele>
{
  public TimoliaTSpieleListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerTimolia.TSpiele.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerTimolia.TSpiele gameMode = (ServerTimolia.TSpiele)getGameMode();
    if (gameMode.getState() == GameState.LOBBY)
    {
      if (key.equals("starting.actionbar")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("start")) {
        gameMode.setState(GameState.STARTING);
      }
    }
    if (gameMode.getState() == GameState.STARTING)
    {
      if (key.equals("tspiele.starting")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("tspiele.start"))
      {
        gameMode.setState(GameState.PREGAME);
        gameMode.setTime(System.currentTimeMillis() + 61000L);
      }
    }
    if (gameMode.getState() == GameState.PREGAME)
    {
      if (key.equals("tspiele.invincibility")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000);
      }
      if (key.equals("tspiele.invincibility_off"))
      {
        gameMode.setState(GameState.GAME);
        gameMode.setTime(System.currentTimeMillis() - 61000L);
      }
    }
  }
}
