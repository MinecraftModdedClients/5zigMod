import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.IWrappedGui;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.util.IVariables;

public class WrappedGui
  extends IWrappedGui
{
  private bfb child;
  
  public WrappedGui(bfb gui)
  {
    this.child = gui;
  }
  
  public void initGui()
  {
    MinecraftFactory.getVars().displayScreen(this.child);
  }
  
  protected void actionPerformed(IButton button) {}
  
  public Object getWrapped()
  {
    return this.child;
  }
}
