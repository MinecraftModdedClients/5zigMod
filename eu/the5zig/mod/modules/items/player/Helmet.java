package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.items.BoolItem;
import eu.the5zig.mod.modules.items.ItemStackItem;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.ItemStack;

public class Helmet
  extends ItemStackItem
{
  public Helmet()
  {
    addSetting(new BoolItem("attributes", "helmet", Boolean.valueOf(true)));
    addSetting(new BoolItem("durability", "helmet", Boolean.valueOf(true)));
  }
  
  protected ItemStack getStack(boolean dummy)
  {
    return dummy ? The5zigMod.getVars().getItemByName("iron_helmet") : The5zigMod.getVars().getItemInArmorSlot(3);
  }
}
