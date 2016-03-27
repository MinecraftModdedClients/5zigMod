package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class Timolia4renaListener
  extends GameListener<ServerTimolia.Arena>
{
  public Timolia4renaListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerTimolia.Arena.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerTimolia.Arena gameMode = (ServerTimolia.Arena)getGameMode();
    if (gameMode.getState() == GameState.LOBBY)
    {
      if (key.equals("starting.actionbar")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("start"))
      {
        gameMode.setState(GameState.GAME);
        gameMode.setTime(System.currentTimeMillis());
      }
    }
    if ((gameMode.getState() == GameState.GAME) && 
      (key.equals("4rena.round"))) {
      gameMode.setRound(Integer.parseInt(match.get(0)));
    }
    if ((gameMode.getState() == GameState.GAME) && 
      (key.equals("4rena.win"))) {
      gameMode.setWinner(match.get(0));
    }
  }
  
  public void onTick()
  {
    ServerTimolia.Arena gameMode = (ServerTimolia.Arena)getGameMode();
    if ((gameMode.getState() == GameState.GAME) && 
      (gameMode.getTime() - System.currentTimeMillis() < 30000L)) {
      gameMode.setWinner(null);
    }
  }
}
