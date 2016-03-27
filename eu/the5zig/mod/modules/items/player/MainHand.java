package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.items.BoolItem;
import eu.the5zig.mod.modules.items.ItemStackItem;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.ItemStack;

public class MainHand
  extends ItemStackItem
{
  public MainHand()
  {
    addSetting(new BoolItem("attributes", "main_hand", Boolean.valueOf(true)));
    addSetting(new BoolItem("durability", "main_hand", Boolean.valueOf(true)));
  }
  
  protected ItemStack getStack(boolean dummy)
  {
    return dummy ? The5zigMod.getVars().getItemByName("diamond_sword") : The5zigMod.getVars().getItemInMainHand();
  }
}
