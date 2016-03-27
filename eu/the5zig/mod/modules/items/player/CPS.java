package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.modules.items.StringItem;
import eu.the5zig.mod.util.PreciseCounter;

public class CPS
  extends StringItem
{
  public void render(int x, int y, RenderLocation renderLocation, boolean dummy)
  {
    The5zigMod.getDataManager().getCpsCalculator().update();
    super.render(x, y, renderLocation, dummy);
  }
  
  protected Object getValue(boolean dummy)
  {
    return Integer.valueOf((int)The5zigMod.getDataManager().getCpsCalculator().getCurrentCount());
  }
  
  public String getTranslation()
  {
    return "ingame.cps";
  }
}
