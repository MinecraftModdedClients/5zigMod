package eu.the5zig.mod.chat.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.ImageMessage.ImageData;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.util.IVariables;
import java.awt.Desktop;
import java.io.File;
import org.apache.logging.log4j.Logger;

public class GuiViewImage
  extends Gui
{
  private final Object resourceLocation;
  private final ImageMessage.ImageData imageData;
  private final String path;
  private int x;
  private int width;
  private int height;
  
  public GuiViewImage(Gui lastScreen, Object resourceLocation, ImageMessage.ImageData imageData, String path)
  {
    super(lastScreen);
    this.resourceLocation = resourceLocation;
    this.imageData = imageData;
    this.path = path;
  }
  
  public void initGui()
  {
    addButton(MinecraftFactory.getVars().createButton(200, getWidth() / 2 + 5, getHeight() - 32, 150, 20, MinecraftFactory.getVars().translate("gui.done", new Object[0])));
    addButton(MinecraftFactory.getVars().createButton(100, getWidth() / 2 - 155, getHeight() - 32, 150, 20, I18n.translate("file_selector.open")));
    
    int realWidth = this.imageData.getRealWidth();
    int realHeight = this.imageData.getRealHeight();
    int maxWidth = getWidth() - 10;
    int maxHeight = getHeight() - 36 - 30;
    while ((realWidth > maxWidth) || (realHeight > maxHeight))
    {
      realWidth = (int)(realWidth / 1.1D);
      realHeight = (int)(realHeight / 1.1D);
    }
    this.width = realWidth;
    this.height = realHeight;
    this.x = ((getWidth() - this.width) / 2);
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 100)
    {
      Desktop desktop = Desktop.getDesktop();
      try
      {
        File dirToOpen = new File(this.path);
        desktop.open(dirToOpen);
      }
      catch (Exception e)
      {
        The5zigMod.logger.error("Could not open Image", e);
      }
    }
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    The5zigMod.getVars().bindTexture(this.resourceLocation);
    drawModalRectWithCustomSizedTexture(this.x, 30, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
  }
  
  public String getTitleName()
  {
    return "The 5zig Mod - " + this.imageData.getHash();
  }
}
