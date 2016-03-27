package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.items.BoolItem;
import eu.the5zig.mod.modules.items.ItemStackItem;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.ItemStack;

public class Leggings
  extends ItemStackItem
{
  public Leggings()
  {
    addSetting(new BoolItem("attributes", "leggings", Boolean.valueOf(true)));
    addSetting(new BoolItem("durability", "leggings", Boolean.valueOf(true)));
  }
  
  protected ItemStack getStack(boolean dummy)
  {
    return dummy ? The5zigMod.getVars().getItemByName("iron_leggings") : The5zigMod.getVars().getItemInArmorSlot(1);
  }
}
