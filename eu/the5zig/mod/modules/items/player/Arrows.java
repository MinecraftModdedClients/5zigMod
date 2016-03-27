package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.modules.items.ItemStackItem;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.ItemStack;

public class Arrows
  extends ItemStackItem
{
  protected ItemStack getStack(boolean dummy)
  {
    if (dummy) {
      return The5zigMod.getVars().getItemByName("arrow", 10);
    }
    int arrowCount = The5zigMod.getVars().getItemCount("item.arrow");
    ItemStack mainHand = The5zigMod.getVars().getItemInMainHand();
    ItemStack offHand = The5zigMod.getVars().getItemInOffHand();
    if (arrowCount > 0)
    {
      if ((mainHand != null) && ("bow".equals(mainHand.getKey()))) {
        return The5zigMod.getVars().getItemByName("arrow", arrowCount);
      }
      if ((offHand != null) && ("bow".equals(offHand.getKey()))) {
        return The5zigMod.getVars().getItemByName("arrow", arrowCount);
      }
    }
    return null;
  }
}
