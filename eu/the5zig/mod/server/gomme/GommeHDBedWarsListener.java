package eu.the5zig.mod.server.gomme;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class GommeHDBedWarsListener
  extends GameListener<ServerGommeHD.BedWars>
{
  public GommeHDBedWarsListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerGommeHD.BedWars.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerGommeHD.BedWars gameMode = (ServerGommeHD.BedWars)getGameMode();
    if (gameMode.getState() == GameState.LOBBY)
    {
      if (key.equals("bw.lobby.starting")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("bw.start"))
      {
        gameMode.setState(GameState.GAME);
        gameMode.setTime(System.currentTimeMillis());
      }
    }
    if (gameMode.getState() == GameState.GAME)
    {
      if (key.equals("bw.bed.self")) {
        gameMode.setCanRespawn(false);
      }
      if ((key.equals("bw.bed.other")) && (match.get(0).equals(The5zigMod.getDataManager().getUsername()))) {
        gameMode.setBeds(gameMode.getBeds() + 1);
      }
      if (key.equals("bw.win"))
      {
        gameMode.setWinner(match.get(0));
        gameMode.setState(GameState.FINISHED);
      }
    }
    if (key.equals("bw.team")) {
      gameMode.setTeam(match.get(0));
    }
  }
}
