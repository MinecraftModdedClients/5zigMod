import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.IGuiHandle;
import eu.the5zig.mod.gui.elements.IButton;
import java.util.List;

public class GuiHandle
  extends bfb
  implements IGuiHandle
{
  private Gui child;
  
  public GuiHandle(Gui child)
  {
    this.child = child;
  }
  
  public void b()
  {
    this.child.initGui0();
  }
  
  protected void a(bcz button)
  {
    this.child.actionPerformed0((IButton)button);
  }
  
  public void a(int mouseX, int mouseY, float partialTicks)
  {
    this.child.drawScreen0(mouseX, mouseY, partialTicks);
    super.a(mouseX, mouseY, partialTicks);
  }
  
  public void e()
  {
    this.child.tick0();
  }
  
  public void k()
  {
    this.child.handleMouseInput0();
    super.k();
  }
  
  public void m()
  {
    this.child.guiClosed0();
  }
  
  protected void a(char c, int i)
  {
    this.child.keyTyped0(c, i);
  }
  
  protected void a(int mouseX, int mouseY, int button)
  {
    super.a(mouseX, mouseY, button);
    this.child.mouseClicked0(mouseX, mouseY, button);
  }
  
  protected void b(int mouseX, int mouseY, int state)
  {
    super.b(mouseX, mouseY, state);
    this.child.mouseReleased0(mouseX, mouseY, state);
  }
  
  public int getWidth()
  {
    return this.l;
  }
  
  public int getHeight()
  {
    return this.m;
  }
  
  public void setResolution(int width, int height)
  {
    a(((Variables)MinecraftFactory.getVars()).getMinecraft(), width, height);
  }
  
  public void drawDefaultBackground()
  {
    c();
  }
  
  public void drawMenuBackground()
  {
    c(0);
  }
  
  public void drawTexturedModalRect(int x, int y, int texX, int texY, int width, int height)
  {
    b(x, y, texX, texY, width, height);
  }
  
  public void drawHoveringText(List<String> lines, int x, int y)
  {
    a(lines, x, y);
  }
  
  public static void drawModalRectWithCustomSizedTexture(int x, int y, float u, float v, int width, int height, float textureWidth, float textureHeight)
  {
    a(x, y, u, v, width, height, textureWidth, textureHeight);
  }
  
  public static void drawRect(int left, int top, int right, int bottom, int color)
  {
    a(left, top, right, bottom, color);
  }
  
  public Gui getChild()
  {
    return this.child;
  }
}
