package eu.the5zig.mod.modules.items;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.ItemStack;
import eu.the5zig.util.minecraft.ChatColor;

public abstract class ItemStackItem
  extends Item
{
  public void render(int x, int y, RenderLocation renderLocation, boolean dummy)
  {
    ItemStack itemStack = getStack(dummy);
    if (itemStack == null) {
      return;
    }
    String string = getString(itemStack);
    if ((renderLocation == RenderLocation.LEFT) || (renderLocation == RenderLocation.CENTERED))
    {
      itemStack.render(x, y, renderWithGenericAttributes());
      if (renderWithDurability()) {
        The5zigMod.getVars().drawString(string, x + 18, y + 5);
      }
    }
    else if (renderWithDurability())
    {
      The5zigMod.getVars().drawString(string, x, y + 5);
      int x2 = x + The5zigMod.getVars().getStringWidth(string) + 2;
      itemStack.render(x2, y, renderWithGenericAttributes());
    }
    else
    {
      itemStack.render(x, y, renderWithGenericAttributes());
    }
  }
  
  private String getString(ItemStack itemStack)
  {
    return (itemStack.getMaxDurability() > 0) && (itemStack.getCurrentDurability() >= 0) ? getColorByDurability((itemStack.getMaxDurability() - itemStack.getCurrentDurability()) / itemStack.getMaxDurability()) + (itemStack.getMaxDurability() - itemStack.getCurrentDurability()) + "/" + itemStack.getMaxDurability() : "";
  }
  
  private String getColorByDurability(float value)
  {
    if (!The5zigMod.getConfig().getBool("coloredEquipmentDurability")) {
      return The5zigMod.getRenderer().getMain();
    }
    if (value >= 0.95F) {
      return ChatColor.DARK_GREEN.toString();
    }
    if (value >= 0.8F) {
      return ChatColor.GREEN.toString();
    }
    if (value >= 0.5F) {
      return ChatColor.YELLOW.toString();
    }
    if (value >= 0.1F) {
      return ChatColor.RED.toString();
    }
    return ChatColor.DARK_RED.toString();
  }
  
  public boolean shouldRender(boolean dummy)
  {
    return (getStack(dummy) != null) && ((The5zigMod.getDataManager().getServer() == null) || (The5zigMod.getDataManager().getServer().isRenderArmor()));
  }
  
  public int getWidth(boolean dummy)
  {
    ItemStack stack = getStack(dummy);
    if (stack == null) {
      return 0;
    }
    return renderWithDurability() ? The5zigMod.getVars().getStringWidth(getString(stack)) + 18 : 16;
  }
  
  public int getHeight(boolean dummy)
  {
    return 14;
  }
  
  private boolean renderWithGenericAttributes()
  {
    eu.the5zig.mod.config.items.Item item = getSetting("attributes");
    return (item != null) && (((Boolean)item.get()).booleanValue());
  }
  
  private boolean renderWithDurability()
  {
    eu.the5zig.mod.config.items.Item item = getSetting("durability");
    return (item != null) && (((Boolean)item.get()).booleanValue());
  }
  
  protected abstract ItemStack getStack(boolean paramBoolean);
}
