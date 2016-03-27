package eu.the5zig.mod.gui.elements;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.modules.Module;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.modules.items.Item;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;

public class StaticModulePreviewRow
  implements Row
{
  private Module module;
  private final int x;
  private final int y;
  private final int width;
  private final int height;
  
  public StaticModulePreviewRow(Module module, int x, int y, int width, int height)
  {
    this.module = module;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }
  
  public void setModule(Module module)
  {
    this.module = module;
  }
  
  public void draw(int x, int y)
  {
    renderModulePreviewBox(this.x, this.y, this.width, this.height);
    if (this.module != null) {
      renderModulePreview(this.module, x, y);
    }
  }
  
  public int getLineHeight()
  {
    return this.module == null ? 0 : this.module.getTotalHeight(true);
  }
  
  private void renderModulePreviewBox(int x, int y, int width, int height)
  {
    Gui.drawRect(x, y, x + width, y + height, 579373192);
    GLUtil.pushMatrix();
    float scale = 1.6F;
    GLUtil.translate(x + width / 2, y + height / 2 - The5zigMod.getVars().getFontHeight() * scale / 2.0F, 1.0F);
    GLUtil.scale(scale, scale, scale);
    GLUtil.enableBlend();
    MinecraftFactory.getVars().drawCenteredString(I18n.translate("modules.preview"), 0, 0, 1440603613);
    GLUtil.disableBlend();
    GLUtil.popMatrix();
    
    Gui.drawRectOutline(x, y, x + width, y + height, -16777216);
  }
  
  private void renderModulePreview(Module module, int x, int y)
  {
    x += 2;
    int yPos = y + 2;
    if (module.isShowLabel())
    {
      The5zigMod.getVars().drawString(The5zigMod.getRenderer().getPrefix() + module.getDisplayName(), x, yPos);
      yPos += 14;
    }
    for (Item item : module.getItems())
    {
      int itemHeight = item.getHeight(true);
      item.render(x, yPos, RenderLocation.LEFT, true);
      yPos += itemHeight;
    }
  }
}
