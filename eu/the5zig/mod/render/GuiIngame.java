package eu.the5zig.mod.render;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.ModuleMaster;
import eu.the5zig.mod.config.items.BoolItem;
import eu.the5zig.mod.listener.CrossHairDistanceListener;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.manager.FPSCalculator;
import eu.the5zig.mod.modules.Module;
import eu.the5zig.mod.modules.items.Item;
import eu.the5zig.mod.modules.items.RegisteredItem;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;

public class GuiIngame
{
  private int hoverTextTime;
  private String hoverText;
  
  public void renderGameOverlay()
  {
    GLUtil.disableBlend();
    
    The5zigMod.getVars().updateScaledResolution();
    if ((The5zigMod.getConfig().getBool("showMod")) && (The5zigMod.getModuleMaster().isItemActive("FPS")))
    {
      boolean isPreciseFPS = false;
      for (Module module : The5zigMod.getModuleMaster().getModules())
      {
        if (isPreciseFPS) {
          break;
        }
        for (Item item : module.getItems())
        {
          if (isPreciseFPS) {
            break;
          }
          if (("FPS".equals(Item.byItem(item.getClass()).getKey())) && 
            (((Boolean)((BoolItem)item.getSetting("preciseFPS")).get()).booleanValue())) {
            isPreciseFPS = true;
          }
        }
      }
      if (isPreciseFPS) {
        The5zigMod.getDataManager().getFpsCalculator().render();
      }
    }
    if (!The5zigMod.getVars().showDebugScreen()) {
      The5zigMod.getRenderer().drawScreen();
    }
    The5zigMod.getDataManager().getCrossHairDistanceListener().render();
    renderTextAboveHotbar();
  }
  
  public void onRenderHotbar()
  {
    if ((The5zigMod.getConfig().getBool("showHotbarNumbers")) && (The5zigMod.getVars().isSpectatingSelf()) && (!The5zigMod.getVars().enableEverythingIsScrewedUpMode())) {
      renderHotbarNumbers();
    }
  }
  
  public void onRenderFood()
  {
    if ((The5zigMod.getConfig().getBool("showSaturation")) && (The5zigMod.getVars().isSpectatingSelf()) && (The5zigMod.getVars().shouldDrawHUD()) && (
      (The5zigMod.getDataManager().getServer() == null) || (The5zigMod.getDataManager().getServer().isRenderSaturation()))) {
      renderSaturation();
    }
  }
  
  public void tick()
  {
    if (this.hoverTextTime > 0) {
      this.hoverTextTime -= 1;
    }
  }
  
  private void renderSaturation()
  {
    int air = The5zigMod.getVars().getAir();
    The5zigMod.getVars().bindTexture(The5zigMod.MINECRAFT_ICONS);
    int y = The5zigMod.getVars().getScaledHeight() - 39 - 10;
    if (air < 300) {
      return;
    }
    for (int i = 0; i < 10; i++)
    {
      int index = 16;
      int x = The5zigMod.getVars().getScaledWidth() / 2 + 91 - i * 8 - 9;
      
      The5zigMod.getVars().drawIngameTexturedModalRect(x, y, 16, 27, 9, 9);
      if (i * 2 + 1 < The5zigMod.getVars().getSaturation()) {
        The5zigMod.getVars().drawIngameTexturedModalRect(x, y, index + 36, 27, 9, 9);
      }
      if (i * 2 + 1 == The5zigMod.getVars().getSaturation()) {
        The5zigMod.getVars().drawIngameTexturedModalRect(x, y, index + 45, 27, 9, 9);
      }
    }
  }
  
  private void renderHotbarNumbers()
  {
    int x = The5zigMod.getVars().getScaledWidth() / 2 - 87;
    int y = The5zigMod.getVars().getScaledHeight() - 18;
    int[] hotbarKeys = The5zigMod.getVars().getHotbarKeys();
    for (int i = 0; i < 9; i++) {
      The5zigMod.getVars().drawString(The5zigMod.getKeyDisplayStringShort(hotbarKeys[i]), x + i * 20, y, 10066329);
    }
  }
  
  private void renderTextAboveHotbar()
  {
    int scaledWidth = The5zigMod.getVars().getScaledWidth();
    int scaledHeight = The5zigMod.getVars().getScaledHeight();
    if (this.hoverTextTime > 0)
    {
      int l3 = (int)(this.hoverTextTime * 256.0F / 10.0F);
      if (l3 > 255) {
        l3 = 255;
      }
      GLUtil.pushMatrix();
      GLUtil.translate(scaledWidth / 2, scaledHeight - 68, 0.0F);
      GLUtil.enableBlend();
      GLUtil.tryBlendFuncSeparate(770, 771, 0, 1);
      
      The5zigMod.getVars().drawString(this.hoverText, -The5zigMod.getVars().getStringWidth(this.hoverText) / 2, -4, 13843770 + (l3 << 24));
      GLUtil.disableBlend();
      GLUtil.popMatrix();
    }
  }
  
  public void showTextAboveHotbar(String str)
  {
    this.hoverTextTime = 600;
    this.hoverText = str;
  }
}
