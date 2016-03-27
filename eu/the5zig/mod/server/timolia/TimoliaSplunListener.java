package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class TimoliaSplunListener
  extends GameListener<ServerTimolia.Splun>
{
  public TimoliaSplunListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerTimolia.Splun.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerTimolia.Splun gameMode = (ServerTimolia.Splun)getGameMode();
    if (key.equals("starting.actionbar")) {
      gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
    }
    if (key.equals("start"))
    {
      gameMode.setState(GameState.GAME);
      gameMode.setTime(System.currentTimeMillis());
    }
  }
}
