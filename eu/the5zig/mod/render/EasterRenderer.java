package eu.the5zig.mod.render;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.listener.EasterListener;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import java.awt.image.BufferedImage;

public class EasterRenderer
{
  private final int TOTAL_FRAMES = 12;
  private int currentFrame = 0;
  private int xOff;
  private BufferedImage bufferedImage;
  private Object resourceLocation;
  private EasterListener easterListener;
  
  public EasterRenderer(EasterListener easterListener)
  {
    this.easterListener = easterListener;
  }
  
  public void render()
  {
    if (this.easterListener.isRunning())
    {
      if ((this.bufferedImage == null) && (this.easterListener.getBufferedImage() != null)) {
        this.resourceLocation = The5zigMod.getVars().bindDynamicImage("easter", this.bufferedImage = this.easterListener.getBufferedImage());
      }
      if (this.resourceLocation != null)
      {
        The5zigMod.getVars().bindTexture(this.resourceLocation);
        int scaleFactor = 6;
        int width = 400 / scaleFactor;
        int height = 400 / scaleFactor;
        for (int y = 0; y < The5zigMod.getVars().getHeight(); y += height) {
          for (int x = -width; x < The5zigMod.getVars().getWidth(); x += width)
          {
            GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
            Gui.drawModalRectWithCustomSizedTexture(x + this.xOff, y, Math.abs(this.currentFrame) * width, 0.0F, width, height, width * 12, height);
          }
        }
        this.currentFrame += 1;
        this.xOff = ((this.xOff + 1) % width);
      }
    }
  }
}
