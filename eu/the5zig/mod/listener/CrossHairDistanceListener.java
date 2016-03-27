package eu.the5zig.mod.listener;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.ModuleMaster;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.IVariables.MouseOverObject;
import eu.the5zig.mod.util.IVariables.ObjectType;
import eu.the5zig.util.Utils;
import org.lwjgl.opengl.GL11;

public class CrossHairDistanceListener
  extends Listener
{
  private double distance;
  private Object pointedEntity;
  private long lastPointedEntity;
  
  public void onTick()
  {
    float maxDistance = The5zigMod.getConfig().getFloat("crosshairDistance");
    
    boolean showEntityHealth = ((The5zigMod.getDataManager().getServer() == null) || (The5zigMod.getDataManager().getServer().isRenderEntityHealth())) && (The5zigMod.getModuleMaster().isItemActive("ENTITY_HEALTH"));
    if ((this.pointedEntity != null) && (System.currentTimeMillis() - this.lastPointedEntity > 500L))
    {
      this.pointedEntity = null;
      this.lastPointedEntity = 0L;
    }
    if ((maxDistance > 0.0F) || (showEntityHealth))
    {
      IVariables.MouseOverObject mouseOverObject = The5zigMod.getVars().calculateMouseOverDistance(Math.max(maxDistance, 10.0F));
      if (mouseOverObject != null)
      {
        this.distance = (maxDistance > 0.0F ? mouseOverObject.getDistance() : -1.0D);
        if ((showEntityHealth) && (mouseOverObject.getType() == IVariables.ObjectType.ENTITY))
        {
          this.pointedEntity = mouseOverObject.getObject();
          this.lastPointedEntity = System.currentTimeMillis();
        }
      }
      else
      {
        this.distance = -1.0D;
      }
    }
    else
    {
      this.distance = -1.0D;
    }
  }
  
  public void render()
  {
    if ((The5zigMod.getConfig().getBool("showMod")) && (this.distance != -1.0D)) {
      drawString(Utils.getShortenedDouble(this.distance, The5zigMod.getConfig().getInt("numberPrecision")) + "m", The5zigMod.getVars().getScaledWidth() / 2, 
        The5zigMod.getVars().getScaledHeight() / 2 + 8, true, 0.5F, true);
    }
  }
  
  public void drawString(String string, int x, int y, boolean alpha, float scale, boolean centered)
  {
    if (alpha)
    {
      GLUtil.enableBlend();
      GLUtil.tryBlendFuncSeparate(775, 769, 1, 0);
      GL11.glEnable(3008);
    }
    GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
    GLUtil.pushMatrix();
    if (centered) {
      GLUtil.translate(x - The5zigMod.getVars().getStringWidth(string) / 2 * scale, y, 1.0F);
    } else {
      GLUtil.translate(x, y, 1.0F);
    }
    GLUtil.scale(scale, scale, scale);
    The5zigMod.getVars().drawString(string, 0, 0, 16777215, false);
    GLUtil.popMatrix();
    if (alpha)
    {
      GLUtil.tryBlendFuncSeparate(770, 771, 1, 0);
      GLUtil.disableBlend();
    }
  }
  
  public Object getPointedEntity()
  {
    return this.pointedEntity;
  }
}
