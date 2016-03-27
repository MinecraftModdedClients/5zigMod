package eu.the5zig.mod.server.hypixel;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class HypixelQuakeListener
  extends GameListener<ServerHypixel.Quake>
{
  public HypixelQuakeListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerHypixel.Quake.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerHypixel.Quake quake = (ServerHypixel.Quake)getGameMode();
    if (quake.getState() == GameState.LOBBY)
    {
      if (key.equals("starting")) {
        quake.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000);
      }
      if (key.equals("start"))
      {
        quake.setState(GameState.GAME);
        quake.setTime(System.currentTimeMillis());
      }
    }
    if ((quake.getState() == GameState.GAME) && (
      (key.equals("quake.kill.1")) || (key.equals("quake.kill.2"))))
    {
      if (match.get(0).equals(The5zigMod.getDataManager().getUsername()))
      {
        quake.setKills(quake.getKills() + 1);
        quake.setKillStreak(quake.getKillStreak() + 1);
      }
      if (match.get(1).equals(The5zigMod.getDataManager().getUsername()))
      {
        quake.setDeaths(quake.getDeaths() + 1);
        quake.setKillStreak(0);
      }
    }
  }
  
  public void onTick()
  {
    ServerHypixel.Quake quake = (ServerHypixel.Quake)getGameMode();
    if ((quake.getState() == GameState.LOBBY) && 
      (quake.getTime() != -1L) && (quake.getTime() - System.currentTimeMillis() < 0L))
    {
      quake.setState(GameState.GAME);
      quake.setTime(System.currentTimeMillis());
    }
  }
}
