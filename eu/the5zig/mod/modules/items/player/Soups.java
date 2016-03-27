package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.modules.items.ItemStackItem;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.ItemStack;

public class Soups
  extends ItemStackItem
{
  protected ItemStack getStack(boolean dummy)
  {
    if (dummy) {
      return The5zigMod.getVars().getItemByName("mushroom_stew");
    }
    int soupCount = The5zigMod.getVars().getItemCount("item.mushroomStew");
    if (soupCount > 0) {
      return The5zigMod.getVars().getItemByName("mushroom_stew", soupCount);
    }
    return null;
  }
}
