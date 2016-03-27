package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.items.BoolItem;
import eu.the5zig.mod.gui.ingame.PotionEffect;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.Arrays;
import java.util.List;

public class Potions
  extends eu.the5zig.mod.modules.items.Item
{
  private static final List<PotionEffect> DUMMY_POTIONS = Arrays.asList(new PotionEffect[] { new PotionEffect("potion.jump", 20, "0:01", 1, 10, true, true), new PotionEffect("potion.moveSpeed", 1000, "0:50", 1, 0, true, true) });
  
  public Potions()
  {
    addSetting(new BoolItem("coloredPotionDurability", "potions", Boolean.valueOf(false)));
  }
  
  public void render(int x, int y, RenderLocation renderLocation, boolean dummy)
  {
    List<PotionEffect> potionEffects = dummy ? DUMMY_POTIONS : The5zigMod.getVars().getActivePotionEffects();
    for (PotionEffect potionEffect : potionEffects)
    {
      if (potionEffect.getIconIndex() != -1)
      {
        String display = toString(potionEffect);
        GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
        The5zigMod.getVars().bindTexture(The5zigMod.INVENTORY_BACKGROUND);
        float scale = 0.7F;
        GLUtil.pushMatrix();
        GLUtil.scale(scale, scale, scale);
        GLUtil.translate(x / scale, (y - 3) / scale, 0.0F);
        The5zigMod.getVars().renderPotionIcon(potionEffect.getIconIndex());
        GLUtil.popMatrix();
        
        The5zigMod.getVars().drawString(display, x + 16, y);
      }
      else
      {
        The5zigMod.getVars().drawString(toString(potionEffect), x, y);
      }
      y += 12;
    }
  }
  
  public int getWidth(boolean dummy)
  {
    int maxWidth = 0;
    for (PotionEffect potionEffect : dummy ? DUMMY_POTIONS : The5zigMod.getVars().getActivePotionEffects())
    {
      int width = The5zigMod.getVars().getStringWidth(toString(potionEffect)) + 10;
      if (potionEffect.getIconIndex() != -1) {
        width += 10;
      }
      if (width > maxWidth) {
        maxWidth = width;
      }
    }
    return maxWidth;
  }
  
  public int getHeight(boolean dummy)
  {
    return dummy ? 24 : The5zigMod.getVars().getActivePotionEffects().size() * 12;
  }
  
  public boolean shouldRender(boolean dummy)
  {
    return (dummy) || ((!The5zigMod.getVars().getActivePotionEffects().isEmpty()) && ((The5zigMod.getDataManager().getServer() == null) || (The5zigMod.getDataManager().getServer().isRenderPotionEffects())));
  }
  
  protected String toString(PotionEffect potionEffect)
  {
    return getColorByDurability(potionEffect.getTime()) + The5zigMod.getVars().translate(potionEffect.getName(), new Object[0]) + " " + potionEffect.getAmplifier() + " - " + potionEffect.getTimeString();
  }
  
  private String getColorByDurability(int time)
  {
    if (!((Boolean)getSetting("coloredPotionDurability").get()).booleanValue()) {
      return The5zigMod.getRenderer().getMain();
    }
    if (time >= 1200) {
      return ChatColor.DARK_GREEN.toString();
    }
    if (time >= 600) {
      return ChatColor.GREEN.toString();
    }
    if (time >= 200) {
      return ChatColor.YELLOW.toString();
    }
    if (time >= 100) {
      return ChatColor.RED.toString();
    }
    return ChatColor.DARK_RED.toString();
  }
}
