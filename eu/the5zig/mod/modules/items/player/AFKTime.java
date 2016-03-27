package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.AFKManager;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.modules.items.StringItem;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;

public class AFKTime
  extends StringItem
{
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Utils.convertToClock(60000L);
    }
    if (The5zigMod.getDataManager().getAfkManager().getAFKTime() > 30000L) {
      return Utils.convertToClock(The5zigMod.getDataManager().getAfkManager().getAFKTime());
    }
    if (The5zigMod.getDataManager().getAfkManager().getLastAfkTime() != 0L) {
      return ChatColor.UNDERLINE + Utils.convertToClock(The5zigMod.getDataManager().getAfkManager().getLastAfkTime());
    }
    return null;
  }
  
  public String getTranslation()
  {
    return "ingame.afk";
  }
}
