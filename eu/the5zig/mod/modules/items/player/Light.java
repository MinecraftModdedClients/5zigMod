package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.items.BoolItem;
import eu.the5zig.mod.modules.items.StringItem;
import eu.the5zig.mod.util.IVariables;

public class Light
  extends StringItem
{
  public Light()
  {
    addSetting(new BoolItem("lightLevelPercentage", "light_level", Boolean.valueOf(true)));
  }
  
  protected Object getValue(boolean dummy)
  {
    return dummy ? format(14) : format(The5zigMod.getVars().getLightLevel());
  }
  
  private String format(int lightLevel)
  {
    return lightLevel + "/15";
  }
  
  public String getTranslation()
  {
    return "ingame.light_level";
  }
}
