package eu.the5zig.mod.modules.items.server.gommehd;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.modules.items.server.LargeTextItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.gomme.ServerGommeHD;
import eu.the5zig.mod.server.gomme.ServerGommeHD.SurvivalGames;

public class SGDeathmatchMessage
  extends LargeTextItem<ServerGommeHD.SurvivalGames>
{
  public SGDeathmatchMessage()
  {
    super(ServerGommeHD.class, ServerGommeHD.SurvivalGames.class, new GameState[0]);
  }
  
  protected String getText()
  {
    if ((((ServerGommeHD.SurvivalGames)getGameMode()).getDeathmatchTime() != -1L) && (((ServerGommeHD.SurvivalGames)getGameMode()).getDeathmatchTime() - System.currentTimeMillis() > 0L) && 
      (((ServerGommeHD.SurvivalGames)getGameMode()).getDeathmatchTime() - System.currentTimeMillis() <= 15000L)) {
      return I18n.translate("ingame.deathmatch_in");
    }
    return null;
  }
}
