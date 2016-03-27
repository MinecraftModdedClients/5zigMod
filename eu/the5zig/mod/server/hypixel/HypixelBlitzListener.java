package eu.the5zig.mod.server.hypixel;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class HypixelBlitzListener
  extends GameListener<ServerHypixel.Blitz>
{
  public HypixelBlitzListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerHypixel.Blitz.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerHypixel.Blitz blitz = (ServerHypixel.Blitz)getGameMode();
    if ((blitz.getState() == GameState.LOBBY) && (
      (key.equals("starting")) || (key.equals("starting.2")))) {
      blitz.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000);
    }
    if (blitz.getState() == GameState.STARTING)
    {
      if (key.equals("blitz.starting")) {
        blitz.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000);
      }
      if (key.equals("blitz.kit.select")) {
        blitz.setKit(match.get(0));
      }
    }
    if (blitz.getState() == GameState.PREGAME)
    {
      if (key.equals("blitz.no_kit")) {
        blitz.setKit(match.get(0));
      }
      if (key.equals("blitz.items")) {
        blitz.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000);
      }
      if (key.equals("blitz.kit"))
      {
        blitz.setState(GameState.GAME);
        blitz.setTime(System.currentTimeMillis() - 60000L);
      }
    }
    if ((blitz.getState() == GameState.PREGAME) || (blitz.getState() == GameState.GAME))
    {
      if (key.equals("blitz.star.min")) {
        blitz.setStar(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000 * 60);
      }
      if (key.equals("blitz.star.sec")) {
        blitz.setStar(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000);
      }
      if (key.equals("blitz.kill"))
      {
        if (match.get(0).equals(The5zigMod.getDataManager().getUsername())) {
          blitz.setKillStreak(0);
        }
        if (match.get(1).equals(The5zigMod.getDataManager().getUsername()))
        {
          blitz.setKills(blitz.getKills() + 1);
          blitz.setKillStreak(blitz.getKillStreak() + 1);
        }
      }
      if (key.equals("blitz.deathmatch.min"))
      {
        blitz.setStar(-1L);
        blitz.setDeathmatch(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000 * 60);
      }
      if (key.equals("blitz.deathmatch.sec"))
      {
        blitz.setStar(-1L);
        blitz.setDeathmatch(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000);
      }
    }
  }
  
  public void onTick()
  {
    ServerHypixel.Blitz blitz = (ServerHypixel.Blitz)getGameMode();
    if ((blitz.getState() == GameState.LOBBY) && 
      (blitz.getTime() != -1L) && (blitz.getTime() - System.currentTimeMillis() < 0L))
    {
      blitz.setState(GameState.STARTING);
      blitz.setTime(System.currentTimeMillis() + 30000L);
    }
    if ((blitz.getState() == GameState.STARTING) && 
      (blitz.getTime() != -1L) && (blitz.getTime() - System.currentTimeMillis() < 0L))
    {
      blitz.setState(GameState.PREGAME);
      blitz.setTime(System.currentTimeMillis() + 60000L);
    }
    if ((blitz.getState() == GameState.PREGAME) && 
      (blitz.getTime() != -1L) && (blitz.getTime() - System.currentTimeMillis() < 0L))
    {
      blitz.setState(GameState.GAME);
      blitz.setTime(System.currentTimeMillis() - 60000L);
    }
    if (blitz.getDeathmatch() - System.currentTimeMillis() < 0L) {
      blitz.setDeathmatch(-1L);
    }
  }
}
