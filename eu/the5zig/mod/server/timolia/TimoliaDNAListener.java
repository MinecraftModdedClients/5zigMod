package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.util.IVariables;

public class TimoliaDNAListener
  extends GameListener<ServerTimolia.DNA>
{
  private int defHeight = 100;
  
  public TimoliaDNAListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerTimolia.DNA.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerTimolia.DNA gameMode = (ServerTimolia.DNA)getGameMode();
    if (key.equals("starting.actionbar")) {
      gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
    }
    if (key.equals("start"))
    {
      gameMode.setState(GameState.STARTING);
      gameMode.setTime(System.currentTimeMillis() + 7000L);
      this.defHeight = ((int)Math.floor(The5zigMod.getVars().getPlayerPosY()));
    }
  }
  
  public void onTick()
  {
    ServerTimolia.DNA gameMode = (ServerTimolia.DNA)getGameMode();
    gameMode.setHeight(The5zigMod.getVars().getPlayerPosY() - this.defHeight);
    if ((gameMode.getState() == GameState.STARTING) && 
      (System.currentTimeMillis() - gameMode.getTime() > 0L))
    {
      gameMode.setState(GameState.GAME);
      gameMode.setTime(System.currentTimeMillis());
    }
  }
}
