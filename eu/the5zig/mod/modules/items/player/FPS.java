package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.items.BoolItem;
import eu.the5zig.mod.config.items.Item;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.manager.FPSCalculator;
import eu.the5zig.mod.modules.items.StringItem;
import eu.the5zig.mod.util.IVariables;

public class FPS
  extends StringItem
{
  public FPS()
  {
    addSetting(new BoolItem("preciseFPS", "fps", Boolean.valueOf(true)));
  }
  
  protected Object getValue(boolean dummy)
  {
    return ((Boolean)getSetting("preciseFPS").get()).booleanValue() ? Integer.valueOf(The5zigMod.getDataManager().getFpsCalculator().getCurrentFPS()) : dummy ? Integer.valueOf(120) : The5zigMod.getVars().getFPS();
  }
  
  public String getTranslation()
  {
    return "ingame.fps";
  }
}
