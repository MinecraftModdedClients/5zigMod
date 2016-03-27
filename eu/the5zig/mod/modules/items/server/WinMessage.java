package eu.the5zig.mod.modules.items.server;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.modules.items.Item;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.render.LargeTextRenderer;
import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;

public class WinMessage
  extends Item
{
  public void render(int x, int y, RenderLocation renderLocation, boolean dummy)
  {
    if (((The5zigMod.getDataManager().getServer() instanceof GameServer)) && (((GameServer)The5zigMod.getDataManager().getServer()).getGameMode() != null) && 
      (((GameServer)The5zigMod.getDataManager().getServer()).getGameMode().getWinner() != null)) {
      DisplayRenderer.largeTextRenderer.render(
        The5zigMod.getRenderer().getPrefix() + I18n.translate("ingame.win", new Object[] { ((GameServer)The5zigMod.getDataManager().getServer()).getGameMode().getWinner() }));
    }
  }
  
  public int getWidth(boolean dummy)
  {
    return 0;
  }
  
  public int getHeight(boolean dummy)
  {
    return 0;
  }
}
