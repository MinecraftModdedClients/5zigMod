package eu.the5zig.mod.server.gomme;

import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class GommeHDPvPListener
  extends GameListener<ServerGommeHD.PvPMatch>
{
  public GommeHDPvPListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerGommeHD.PvPMatch.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerGommeHD.PvPMatch pvpMatch = (ServerGommeHD.PvPMatch)getGameMode();
    if (key.equals("pvp.starting")) {
      pvpMatch.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
    }
    if (key.equals("pvp.start"))
    {
      pvpMatch.setState(GameState.GAME);
      pvpMatch.setTime(System.currentTimeMillis());
    }
    if (key.equals("pvp.win"))
    {
      pvpMatch.setWinner(match.get(0));
      pvpMatch.setState(GameState.FINISHED);
    }
  }
  
  public void onTick()
  {
    ServerGommeHD.PvPMatch pvpMatch = (ServerGommeHD.PvPMatch)getGameMode();
    if (pvpMatch.getState() == GameState.LOBBY) {
      pvpMatch.setState(GameState.STARTING);
    }
  }
}
