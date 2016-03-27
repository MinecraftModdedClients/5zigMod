package eu.the5zig.mod.gui.elements;

import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.util.IVariables;

public class ButtonRow
  implements RowExtended
{
  public IButton button1;
  public IButton button2;
  
  public ButtonRow(IButton button1, IButton button2)
  {
    this.button1 = button1;
    this.button2 = button2;
  }
  
  public void draw(int x, int y) {}
  
  public void draw(int x, int y, int slotHeight, int mouseX, int mouseY)
  {
    if (this.button1 != null)
    {
      this.button1.setY(y + 2);
      this.button1.draw(mouseX, mouseY);
    }
    if (this.button2 != null)
    {
      this.button2.setY(y + 2);
      this.button2.draw(mouseX, mouseY);
    }
  }
  
  public IButton mousePressed(int mouseX, int mouseY)
  {
    if ((this.button1 != null) && 
      (this.button1.mouseClicked(mouseX, mouseY)))
    {
      this.button1.playClickSound();
      MinecraftFactory.getVars().getCurrentScreen().actionPerformed0(this.button1);
      return this.button1;
    }
    if ((this.button2 != null) && 
      (this.button2.mouseClicked(mouseX, mouseY)))
    {
      this.button2.playClickSound();
      MinecraftFactory.getVars().getCurrentScreen().actionPerformed0(this.button2);
      return this.button2;
    }
    return null;
  }
  
  public int getLineHeight()
  {
    return 24;
  }
}
