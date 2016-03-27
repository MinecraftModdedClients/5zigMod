package eu.the5zig.mod.modules.items.server.gommehd;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.gomme.ServerGommeHD;
import eu.the5zig.mod.server.gomme.ServerGommeHD.EnderGames;

public class EnderGamesKit
  extends GameModeItem<ServerGommeHD.EnderGames>
{
  public EnderGamesKit()
  {
    super(ServerGommeHD.class, ServerGommeHD.EnderGames.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    return dummy ? "Archer" : ((ServerGommeHD.EnderGames)getGameMode()).getKit();
  }
  
  public String getTranslation()
  {
    return "ingame.kit";
  }
}
