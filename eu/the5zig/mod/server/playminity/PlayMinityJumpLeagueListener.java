package eu.the5zig.mod.server.playminity;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.util.IVariables;

public class PlayMinityJumpLeagueListener
  extends GameListener<ServerPlayMinity.JumpLeague>
{
  private double lastY;
  
  public PlayMinityJumpLeagueListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerPlayMinity.JumpLeague.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerPlayMinity.JumpLeague jumpLeague = (ServerPlayMinity.JumpLeague)getGameMode();
    if (jumpLeague.getState() == GameState.LOBBY)
    {
      if (key.equals("jumpleague.lobby.starting")) {
        jumpLeague.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000);
      }
      if (key.equals("jumpleague.lobby.start"))
      {
        jumpLeague.setState(GameState.STARTING);
        jumpLeague.setTime(System.currentTimeMillis() + 20000L);
      }
    }
    if (jumpLeague.getState() == GameState.STARTING)
    {
      if (key.equals("jumpleague.starting")) {
        jumpLeague.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000);
      }
      if (key.equals("jumpleague.start"))
      {
        this.lastY = 2.147483647E9D;
        jumpLeague.setState(GameState.GAME);
        jumpLeague.setTime(System.currentTimeMillis());
      }
    }
    if (jumpLeague.getState() == GameState.GAME)
    {
      if (key.equals("jumpleague.checkpoint"))
      {
        jumpLeague.setCheckPoint(Integer.parseInt(match.get(0)));
        jumpLeague.setMaxCheckPoints(Integer.parseInt(match.get(1)));
      }
      if (key.equals("jumpleague.endmatch"))
      {
        long time = jumpLeague.getTime();
        jumpLeague.setState(GameState.ENDGAME);
        jumpLeague.setTime(time);
      }
    }
    if (jumpLeague.getState() == GameState.ENDGAME)
    {
      if (key.equals("jumpleague.kill"))
      {
        if (match.get(0).equals(The5zigMod.getDataManager().getUsername())) {
          jumpLeague.setLives(jumpLeague.getLives() - 1);
        }
        if (match.get(1).equals(The5zigMod.getDataManager().getUsername())) {
          jumpLeague.setKills(jumpLeague.getKills() + 1);
        }
      }
      if (key.equals("jumpleague.lives")) {
        jumpLeague.setLives(Integer.parseInt(match.get(0)));
      }
      if (key.equals("jumpleague.win"))
      {
        jumpLeague.setWinner(match.get(0));
        jumpLeague.setState(GameState.FINISHED);
      }
    }
  }
  
  public void onTick()
  {
    ServerPlayMinity.JumpLeague jumpLeague = (ServerPlayMinity.JumpLeague)getGameMode();
    if (jumpLeague.getState() == GameState.GAME)
    {
      if ((this.lastY <= 45.0D) && (The5zigMod.getVars().getPlayerPosY() - this.lastY > 3.0D)) {
        jumpLeague.setFails(jumpLeague.getFails() + 1);
      }
      this.lastY = The5zigMod.getVars().getPlayerPosY();
    }
  }
}
