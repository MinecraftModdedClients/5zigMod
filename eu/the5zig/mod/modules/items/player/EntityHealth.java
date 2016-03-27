package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.listener.CrossHairDistanceListener;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.modules.items.Item;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;

public class EntityHealth
  extends Item
{
  private static final float SCALE = 0.65F;
  
  public void render(int x, int y, RenderLocation renderLocation, boolean dummy)
  {
    String string = getString(dummy);
    The5zigMod.getDataManager().getCrossHairDistanceListener().drawString(string, x, y, false, 0.65F, false);
  }
  
  public int getWidth(boolean dummy)
  {
    return (int)(The5zigMod.getVars().getStringWidth(getString(dummy)) * 0.65F);
  }
  
  public int getHeight(boolean dummy)
  {
    return 6;
  }
  
  public boolean shouldRender(boolean dummy)
  {
    return (dummy) || ((The5zigMod.getDataManager().getCrossHairDistanceListener().getPointedEntity() != null) && ((The5zigMod.getDataManager().getServer() == null) || (The5zigMod.getDataManager().getServer().isRenderEntityHealth())));
  }
  
  private String getString(boolean dummy)
  {
    return ChatColor.DARK_RED.toString() + Math.ceil(getHealth(dummy)) / 2.0D + " ❤";
  }
  
  private float getHealth(boolean dummy)
  {
    return dummy ? 10.0F : The5zigMod.getVars().getHealth(The5zigMod.getDataManager().getCrossHairDistanceListener().getPointedEntity());
  }
}
