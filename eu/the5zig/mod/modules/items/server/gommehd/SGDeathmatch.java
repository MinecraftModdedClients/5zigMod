package eu.the5zig.mod.modules.items.server.gommehd;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.gomme.ServerGommeHD;
import eu.the5zig.mod.server.gomme.ServerGommeHD.SurvivalGames;

public class SGDeathmatch
  extends GameModeItem<ServerGommeHD.SurvivalGames>
{
  public SGDeathmatch()
  {
    super(ServerGommeHD.class, ServerGommeHD.SurvivalGames.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return shorten(10.0D);
    }
    return (((ServerGommeHD.SurvivalGames)getGameMode()).getDeathmatchTime() != -1L) && (((ServerGommeHD.SurvivalGames)getGameMode()).getDeathmatchTime() - System.currentTimeMillis() > 0L) ? shorten(
      (((ServerGommeHD.SurvivalGames)getGameMode()).getDeathmatchTime() - System.currentTimeMillis()) / 1000.0D) : null;
  }
  
  public String getTranslation()
  {
    return "ingame.deathmatch";
  }
}
