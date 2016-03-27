package eu.the5zig.mod.server.venicraft;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class MineathlonListener
  extends GameListener<ServerVenicraft.Mineathlon>
{
  private boolean winAnnounced = false;
  
  public MineathlonListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerVenicraft.Mineathlon.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerVenicraft.Mineathlon gameMode = (ServerVenicraft.Mineathlon)getGameMode();
    if (gameMode.getState() == GameState.LOBBY)
    {
      if (key.equals("lobby.start")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("lobby.stop")) {
        gameMode.setTime(-1L);
      }
    }
    if (gameMode.getState() == GameState.GAME)
    {
      if (key.equals("mineathlon.discipline"))
      {
        gameMode.setDiscipline(Integer.parseInt(match.get(0)) + " (" + match.get(1) + ")");
        gameMode.setRound(0);
      }
      if (key.equals("mineathlon.round")) {
        gameMode.setRound(Integer.parseInt(match.get(0)));
      }
      if ((key.equals("mineathlon.invincibility.min")) || (key.equals("mineathlon.invincibility.sec")))
      {
        gameMode.setState(GameState.STARTING);
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000 * (key.equals("mineathlon.invincibility.min") ? 60 : 1));
      }
    }
    if (gameMode.getState() == GameState.STARTING)
    {
      if ((key.equals("mineathlon.invincibility.min")) || (key.equals("mineathlon.invincibility.sec"))) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000 * (key.equals("mineathlon.invincibility.min") ? 60 : 1));
      }
      if (key.equals("mineathlon.no_invincibility")) {
        gameMode.setState(GameState.ENDGAME);
      }
    }
    if (gameMode.getState() == GameState.ENDGAME)
    {
      if ((key.equals("mineathlon.kill")) && 
        (The5zigMod.getDataManager().getUsername().equals(match.get(1)))) {
        gameMode.setKills(gameMode.getKills() + 1);
      }
      if (key.equals("mineathlon.win.announcement")) {
        this.winAnnounced = true;
      }
      if ((this.winAnnounced) && (key.equals("mineathlon.win.player")))
      {
        this.winAnnounced = false;
        gameMode.setWinner(match.get(0));
        gameMode.setState(GameState.FINISHED);
      }
    }
  }
  
  public void onTick()
  {
    ServerVenicraft.Mineathlon gameMode = (ServerVenicraft.Mineathlon)getGameMode();
    if ((gameMode.getState() == GameState.LOBBY) && 
      (gameMode.getTime() != -1L) && (System.currentTimeMillis() - gameMode.getTime() > 0L))
    {
      gameMode.setState(GameState.GAME);
      gameMode.setTime(System.currentTimeMillis());
    }
  }
}
