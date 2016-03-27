package eu.the5zig.mod.modules.items.server;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.modules.items.StringItem;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.util.IVariables;

public class ServerIP
  extends StringItem
{
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return "hypixel.net";
    }
    Server server = The5zigMod.getDataManager().getServer();
    if (server == null) {
      return null;
    }
    String ip = server.getHost();
    if (server.getPort() != 25565) {
      ip = ip + ":" + server.getPort();
    }
    return The5zigMod.getVars().shortenToWidth(ip, 150);
  }
  
  public String getTranslation()
  {
    return "ingame.ip";
  }
}
