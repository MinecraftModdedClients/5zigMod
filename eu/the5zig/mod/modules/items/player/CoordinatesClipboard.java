package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.modules.items.Item;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.Vector2i;
import org.lwjgl.opengl.GL11;

public class CoordinatesClipboard
  extends Item
{
  public void render(int x, int y, RenderLocation renderLocation, boolean dummy)
  {
    String renderString = getRenderString(dummy);
    int width = getWidth(dummy);
    The5zigMod.getVars().drawString(renderString, x, y);
    
    Vector2i coordinates = dummy ? new Vector2i(10, -53) : The5zigMod.getDataManager().getCoordinatesClipboard();
    
    double dx = coordinates.getX() - (dummy ? 10.0D : The5zigMod.getVars().getPlayerPosX());
    double dz = coordinates.getY() - (dummy ? -10.0D : The5zigMod.getVars().getPlayerPosZ());
    float yaw = (float)Math.atan2(dz, dx);
    float rotateAngle = (float)Math.toDegrees(yaw) - (dummy ? 0.0F : The5zigMod.getVars().getPlayerRotationYaw()) - 90.0F;
    int x2 = x + width - 12;
    int y2 = y - 3;
    int xCentered = x2 + 6;
    int yCentered = y2 + 6;
    
    GLUtil.pushMatrix();
    GLUtil.translate(xCentered, yCentered, 0.0F);
    GL11.glRotatef(rotateAngle, 0.0F, 0.0F, 1.0F);
    GLUtil.translate(x2 - xCentered, y2 - yCentered, 0.0F);
    The5zigMod.getVars().bindTexture(The5zigMod.ITEMS);
    Gui.drawModalRectWithCustomSizedTexture(0, 0, 69.0F, 0.0F, 12, 12, 96.0F, 96.0F);
    GLUtil.popMatrix();
  }
  
  public int getWidth(boolean dummy)
  {
    return The5zigMod.getVars().getStringWidth(getRenderString(dummy)) + 16;
  }
  
  public int getHeight(boolean dummy)
  {
    return 10;
  }
  
  public boolean shouldRender(boolean dummy)
  {
    return (dummy) || ((The5zigMod.getDataManager().getCoordinatesClipboard() != null) && (!The5zigMod.getVars().isTablistShown()));
  }
  
  private String getRenderString(boolean dummy)
  {
    if (dummy) {
      return getPrefix("X") + "10 " + getPrefix("Z") + "-53";
    }
    return getPrefix("X") + The5zigMod.getDataManager().getCoordinatesClipboard().getX() + " " + getPrefix("Z") + The5zigMod.getDataManager().getCoordinatesClipboard().getY();
  }
}
