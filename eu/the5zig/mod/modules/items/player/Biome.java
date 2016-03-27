package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.modules.items.StringItem;
import eu.the5zig.mod.util.IVariables;

public class Biome
  extends StringItem
{
  protected Object getValue(boolean dummy)
  {
    return dummy ? "Forest" : The5zigMod.getVars().getBiome();
  }
  
  public String getTranslation()
  {
    return "ingame.biome";
  }
}
