package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.modules.items.StringItem;
import eu.the5zig.mod.util.IVariables;

public class EntityCount
  extends StringItem
{
  protected Object getValue(boolean dummy)
  {
    return The5zigMod.getVars().getEntityCount();
  }
  
  public String getTranslation()
  {
    return "ingame.entities";
  }
}
