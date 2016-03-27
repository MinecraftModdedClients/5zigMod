import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IResourceLocation;
import eu.the5zig.mod.util.IVariables;

public class IconButton
  extends Button
{
  private IResourceLocation resourceLocation;
  private int u;
  private int v;
  
  public IconButton(IResourceLocation resourceLocation, int u, int v, int id, int x, int y)
  {
    super(id, x, y, 20, 20, "");
    this.resourceLocation = resourceLocation;
    this.u = u;
    this.v = v;
  }
  
  public void draw(int mouseX, int mouseY)
  {
    super.draw(mouseX, mouseY);
    if (isVisible())
    {
      MinecraftFactory.getVars().bindTexture(this.resourceLocation);
      GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
      Gui.drawModalRectWithCustomSizedTexture(getX() + 2, getY() + 2, this.u, this.v, 16, 16, 132.0F, 132.0F);
    }
  }
}
