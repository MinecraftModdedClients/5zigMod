package eu.the5zig.mod.server.hypixel;

import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;
import org.apache.commons.lang3.text.WordUtils;

public class HypixelPaintballListener
  extends GameListener<ServerHypixel.Paintball>
{
  public HypixelPaintballListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerHypixel.Paintball.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerHypixel.Paintball paintball = (ServerHypixel.Paintball)getGameMode();
    if ((paintball.getState() == GameState.LOBBY) && 
      (key.equals("starting"))) {
      paintball.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000);
    }
    if (paintball.getState() == GameState.GAME)
    {
      if (key.equals("paintball.kill")) {
        paintball.setKills(paintball.getKills() + 1);
      }
      if (key.equals("paintball.death")) {
        paintball.setDeaths(paintball.getDeaths() + 1);
      }
    }
    if (key.equals("paintball.team")) {
      paintball.setTeam(WordUtils.capitalize(match.get(0).toLowerCase()));
    }
  }
  
  public void onTick()
  {
    ServerHypixel.Paintball paintball = (ServerHypixel.Paintball)getGameMode();
    if ((paintball.getState() == GameState.LOBBY) && 
      (paintball.getTime() != -1L) && (paintball.getTime() - System.currentTimeMillis() < 0L))
    {
      paintball.setState(GameState.GAME);
      paintball.setTime(System.currentTimeMillis());
    }
  }
}
