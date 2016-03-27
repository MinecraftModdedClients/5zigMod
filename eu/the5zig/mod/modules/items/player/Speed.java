package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.manager.SpeedCalculator;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.modules.items.StringItem;

public class Speed
  extends StringItem
{
  public void render(int x, int y, RenderLocation renderLocation, boolean dummy)
  {
    The5zigMod.getDataManager().getSpeedCalculator().update();
    super.render(x, y, renderLocation, dummy);
  }
  
  protected Object getValue(boolean dummy)
  {
    return (dummy ? shorten(2.3D) : shorten(The5zigMod.getDataManager().getSpeedCalculator().getCurrentSpeed())) + " m/s";
  }
  
  public String getTranslation()
  {
    return "ingame.speed";
  }
}
