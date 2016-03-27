package eu.the5zig.mod.modules.items.server;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.modules.items.StringItem;
import eu.the5zig.mod.server.Server;

public class ServerPlayers
  extends StringItem
{
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(64);
    }
    return The5zigMod.getDataManager().getServer() == null ? null : Integer.valueOf(The5zigMod.getDataManager().getServer().getPlayers());
  }
  
  public String getTranslation()
  {
    return "ingame.players";
  }
}
