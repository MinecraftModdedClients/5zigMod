package eu.the5zig.mod.render;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.gui.ingame.PotionEffect;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;

public class PotionIndicatorRenderer
{
  public void render()
  {
    if ((!The5zigMod.getConfig().getBool("showPotionIndicator")) || ((The5zigMod.getDataManager().getServer() != null) && (!The5zigMod.getDataManager().getServer().isRenderPotionIndicator()))) {
      return;
    }
    PotionEffect potionEffect = The5zigMod.getVars().getPotionForVignette();
    if (potionEffect == null) {
      return;
    }
    if (potionEffect.isGood())
    {
      int maxTime = 1200;
      double percent = potionEffect.getTime() / maxTime;
      float intensity = (float)(0.3D + percent);
      GLUtil.color(intensity, 0.0F, intensity, 1.0F);
    }
    else
    {
      int maxTime = 1200;
      double percent = potionEffect.getTime() / maxTime;
      float intensity = (float)(0.2D + percent);
      GLUtil.color(0.0F, intensity, intensity, 1.0F);
    }
  }
}
