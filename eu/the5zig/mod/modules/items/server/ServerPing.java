package eu.the5zig.mod.modules.items.server;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.manager.PingManager;
import eu.the5zig.mod.modules.items.StringItem;

public class ServerPing
  extends StringItem
{
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(127);
    }
    return The5zigMod.getDataManager().getPingManager().getMs() != -1 ? Integer.valueOf(The5zigMod.getDataManager().getPingManager().getMs()) : The5zigMod.getDataManager().getServer() == null ? null : I18n.translate("ingame.pinging");
  }
  
  public String getTranslation()
  {
    return "ingame.ping";
  }
}
