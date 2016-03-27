package eu.the5zig.mod.server.bergwerk;

import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class BergwerkFlashListener
  extends GameListener<ServerBergwerk.Flash>
{
  public BergwerkFlashListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerBergwerk.Flash.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerBergwerk.Flash gameMode = (ServerBergwerk.Flash)getGameMode();
    if (gameMode.getState() == GameState.LOBBY)
    {
      if (key.equals("starting")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("start"))
      {
        gameMode.setState(GameState.GAME);
        gameMode.setTime(System.currentTimeMillis());
      }
    }
    if ((gameMode.getState() == GameState.GAME) && 
      (key.equals("flash.countdown")))
    {
      gameMode.setState(GameState.ENDGAME);
      gameMode.setTime(System.currentTimeMillis() + 60000L);
    }
    if (((gameMode.getState() == GameState.GAME) || (gameMode.getState() == GameState.ENDGAME)) && 
      (key.equals("flash.win")))
    {
      gameMode.setWinner(match.get(0));
      gameMode.setState(GameState.FINISHED);
    }
  }
  
  public void onTick()
  {
    ServerBergwerk.Flash gameMode = (ServerBergwerk.Flash)getGameMode();
    if ((gameMode.getState() == GameState.ENDGAME) && 
      (System.currentTimeMillis() - gameMode.getTime() > 0L)) {
      gameMode.setTime(-1L);
    }
  }
}
