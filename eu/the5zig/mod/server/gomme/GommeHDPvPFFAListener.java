package eu.the5zig.mod.server.gomme;

import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class GommeHDPvPFFAListener
  extends GameListener<ServerGommeHD.FFA>
{
  public GommeHDPvPFFAListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerGommeHD.FFA.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerGommeHD.FFA ffa = (ServerGommeHD.FFA)getGameMode();
    if (key.equals("ffa.kill")) {
      ffa.setKillStreak(ffa.getKillStreak() + 1);
    }
    if (key.equals("ffa.death")) {
      ffa.setKillStreak(0);
    }
  }
}
