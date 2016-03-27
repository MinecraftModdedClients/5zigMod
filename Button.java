import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.elements.IButton;

public class Button
  extends bcz
  implements IButton
{
  private int disabledTicks = 0;
  
  public Button(int id, int x, int y, String label)
  {
    super(id, x, y, label);
  }
  
  public Button(int id, int x, int y, String label, boolean enabled)
  {
    super(id, x, y, label);
    setEnabled(enabled);
  }
  
  public Button(int id, int x, int y, int width, int height, String label)
  {
    super(id, x, y, width, height, label);
  }
  
  public Button(int id, int x, int y, int width, int height, String label, boolean enabled)
  {
    super(id, x, y, width, height, label);
    setEnabled(enabled);
  }
  
  public int getId()
  {
    return this.k;
  }
  
  public String getLabel()
  {
    return this.j;
  }
  
  public void setLabel(String label)
  {
    this.j = label;
  }
  
  public int getWidth()
  {
    return this.f;
  }
  
  public void setWidth(int width)
  {
    this.f = width;
  }
  
  public int getHeight()
  {
    return this.g;
  }
  
  public void setHeight(int height)
  {
    this.g = height;
  }
  
  public boolean isEnabled()
  {
    return this.l;
  }
  
  public void setEnabled(boolean enabled)
  {
    this.l = enabled;
  }
  
  public boolean isVisible()
  {
    return this.m;
  }
  
  public void setVisible(boolean visible)
  {
    this.m = visible;
  }
  
  public boolean isHovered()
  {
    return this.n;
  }
  
  public void setHovered(boolean hovered)
  {
    this.n = hovered;
  }
  
  public int getX()
  {
    return this.h;
  }
  
  public void setX(int x)
  {
    this.h = x;
  }
  
  public int getY()
  {
    return this.i;
  }
  
  public void setY(int y)
  {
    this.i = y;
  }
  
  public void draw(int mouseX, int mouseY)
  {
    a(((Variables)MinecraftFactory.getVars()).getMinecraft(), mouseX, mouseY);
  }
  
  public void tick()
  {
    if (this.disabledTicks > 0)
    {
      this.disabledTicks -= 1;
      if (this.disabledTicks == 0) {
        setEnabled(true);
      }
    }
    if (this.disabledTicks < 0) {
      this.disabledTicks = 1;
    }
  }
  
  public boolean mouseClicked(int mouseX, int mouseY)
  {
    return c(((Variables)MinecraftFactory.getVars()).getMinecraft(), mouseX, mouseY);
  }
  
  public void mouseReleased(int mouseX, int mouseY)
  {
    a(mouseX, mouseY);
  }
  
  public void playClickSound()
  {
    a(((Variables)MinecraftFactory.getVars()).getMinecraft().U());
  }
  
  public void setTicksDisabled(int ticks)
  {
    setEnabled(false);
    this.disabledTicks = ticks;
  }
  
  public void guiClosed() {}
}
