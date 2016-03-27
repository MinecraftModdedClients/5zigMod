package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.items.BoolItem;
import eu.the5zig.mod.modules.items.ItemStackItem;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.ItemStack;

public class Boots
  extends ItemStackItem
{
  public Boots()
  {
    addSetting(new BoolItem("attributes", "boots", Boolean.valueOf(true)));
    addSetting(new BoolItem("durability", "boots", Boolean.valueOf(true)));
  }
  
  protected ItemStack getStack(boolean dummy)
  {
    return dummy ? The5zigMod.getVars().getItemByName("iron_boots") : The5zigMod.getVars().getItemInArmorSlot(0);
  }
}
