package eu.the5zig.mod.server.bergwerk;

import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class BergwerkDuelListener
  extends GameListener<ServerBergwerk.Duel>
{
  public BergwerkDuelListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerBergwerk.Duel.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerBergwerk.Duel gameMode = (ServerBergwerk.Duel)getGameMode();
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
      if (key.equals("duel.team")) {
        gameMode.setTeam(match.get(0));
      }
    }
    if (gameMode.getState() == GameState.GAME)
    {
      if ((key.equals("duel.bed_destroy")) && 
        (match.get(0).equals(gameMode.getTeam()))) {
        gameMode.setCanRespawn(false);
      }
      if (key.equals("duel.teleporter")) {
        gameMode.setTeleporterTimer(System.currentTimeMillis() + 5000L);
      }
      if (key.equals("duel.win")) {
        gameMode.setWinner(match.get(0));
      }
    }
  }
  
  public void onTick()
  {
    ServerBergwerk.Duel gameMode = (ServerBergwerk.Duel)getGameMode();
    if ((gameMode.getTeleporterTimer() != -1L) && 
      (System.currentTimeMillis() - gameMode.getTeleporterTimer() > 0L)) {
      gameMode.setTeleporterTimer(-1L);
    }
  }
}
