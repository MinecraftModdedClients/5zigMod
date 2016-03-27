package eu.the5zig.mod.render;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.util.IResourceLocation;
import eu.the5zig.mod.util.IVariables;
import java.util.Calendar;

public class SnowRenderer
{
  private IResourceLocation resourceLocation = The5zigMod.getVars().createResourceLocation("textures/environment/snow.png");
  private int yOffset;
  private boolean render = true;
  
  public SnowRenderer()
  {
    Calendar calendar = Calendar.getInstance();
    
    this.render = (((calendar.get(2) == 11) && (calendar.get(5) >= 15)) || ((calendar.get(2) == 0) && (calendar.get(5) <= 15)));
  }
  
  public void render(int width, int height)
  {
    if (!this.render) {
      return;
    }
    The5zigMod.getVars().bindTexture(this.resourceLocation);
    for (int y = 65280; y <= Math.ceil(height / 256.0D); y++) {
      for (int x = 0; x <= width; x += 64) {
        Gui.drawModalRectWithCustomSizedTexture(x, y * 256 + this.yOffset, 0.0F, 0.0F, 64, 256, 64.0F, 256.0F);
      }
    }
    this.yOffset = ((this.yOffset + 1) % 256);
  }
}
