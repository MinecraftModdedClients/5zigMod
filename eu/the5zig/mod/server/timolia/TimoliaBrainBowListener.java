package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class TimoliaBrainBowListener
  extends GameListener<ServerTimolia.BrainBow>
{
  public TimoliaBrainBowListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerTimolia.BrainBow.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerTimolia.BrainBow gameMode = (ServerTimolia.BrainBow)getGameMode();
    if (((gameMode.getState() == GameState.LOBBY) || (gameMode.getState() == GameState.STARTING)) && 
      (key.equals("brainbow.team"))) {
      gameMode.setTeam(match.get(0));
    }
    if (gameMode.getState() == GameState.LOBBY)
    {
      if (key.equals("starting.actionbar")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("start"))
      {
        gameMode.setState(GameState.STARTING);
        gameMode.setTime(System.currentTimeMillis() + 4890L);
      }
    }
    if (gameMode.getState() == GameState.GAME)
    {
      if (key.equals("brainbow.win"))
      {
        gameMode.setWinner("Team " + match.get(0));
        gameMode.setState(GameState.FINISHED);
      }
      if ((key.equals("brainbow.score")) && (match.get(0).equals(The5zigMod.getDataManager().getUsername()))) {
        gameMode.setScore(gameMode.getScore() + 1);
      }
    }
  }
  
  public void onTick()
  {
    ServerTimolia.BrainBow gameMode = (ServerTimolia.BrainBow)getGameMode();
    if ((gameMode.getState() == GameState.STARTING) && 
      (System.currentTimeMillis() - gameMode.getTime() > 0L))
    {
      gameMode.setState(GameState.GAME);
      gameMode.setTime(System.currentTimeMillis());
    }
  }
}
