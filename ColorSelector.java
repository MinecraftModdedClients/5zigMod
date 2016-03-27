import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.elements.IColorSelector;
import eu.the5zig.mod.util.ColorSelectorCallback;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;

public class ColorSelector
  extends Button
  implements IColorSelector
{
  private int boxWidth = 14;
  private int boxHeight = 14;
  private boolean selected = false;
  private int selectedX = -1;
  private int selectedY = -1;
  private boolean requireMoveOut = false;
  private final ColorSelectorCallback callback;
  
  public ColorSelector(int id, int x, int y, int width, int height, String label, ColorSelectorCallback callback)
  {
    super(id, x, y, width, height, label);
    this.callback = callback;
  }
  
  public void draw(int mouseX, int mouseY)
  {
    super.draw(mouseX, mouseY);
    
    int boxX = getBoxX();
    int boxY = getBoxY();
    if ((isEnabled()) && (!this.selected) && (!this.requireMoveOut) && (mouseX >= boxX) && (mouseX <= boxX + this.boxWidth) && (mouseY >= boxY) && (mouseY <= boxY + this.boxHeight))
    {
      this.selectedX = (boxX - 96 + 7);
      this.selectedY = (boxY + 3);
      if (boxX + 96 + 2 > MinecraftFactory.getVars().getCurrentScreen().getWidth()) {
        this.selectedX = (MinecraftFactory.getVars().getCurrentScreen().getWidth() - 192 - 2);
      }
      this.selected = true;
    }
    else if (this.requireMoveOut)
    {
      if ((mouseX < boxX) || (mouseX > boxX + this.boxWidth) || (mouseY < this.selectedY) || (mouseY > this.selectedY + 8))
      {
        this.selectedX = (this.selectedY = -1);
        this.selected = false;
        this.requireMoveOut = false;
      }
    }
    else if ((!this.selected) || (mouseX < this.selectedX) || (mouseX > this.selectedX + 192) || (mouseY < this.selectedY - 8) || (mouseY > this.selectedY + 8 + 8))
    {
      this.selectedX = (this.selectedY = -1);
      this.selected = false;
      this.requireMoveOut = false;
    }
    Gui.drawRect(boxX, boxY, boxX + this.boxWidth, boxY + this.boxHeight, ((this.selected) && (!this.requireMoveOut)) || ((mouseX >= boxX) && (mouseX <= boxX + this.boxWidth) && (mouseY >= boxY) && (mouseY <= boxY + this.boxHeight)) ? -12303292 : -14540254);
    
    Gui.drawRect(boxX + 2, boxY + 2, boxX + this.boxWidth - 2, boxY + this.boxHeight - 2, 0xFFFFFF & this.callback.getColor().getColor() | 0xFF000000);
    if (this.selected)
    {
      int width = 192;
      Gui.drawRect(this.selectedX - 2, this.selectedY - 2, this.selectedX + width + 2, this.selectedY + 10, -14540254);
      int x = 0;
      for (ChatColor chatColor : ChatColor.values()) {
        if (chatColor.getColor() != -1)
        {
          Gui.drawRect(this.selectedX + x, this.selectedY, this.selectedX + x + 12, this.selectedY + 8, 0xFFFFFF & chatColor.getColor() | 0xFF000000);
          x += 12;
        }
      }
    }
  }
  
  protected int a(boolean b)
  {
    setHovered(false);
    return isEnabled() ? 1 : 0;
  }
  
  public boolean mouseClicked(int mouseX, int mouseY)
  {
    boolean result = super.mouseClicked(mouseX, mouseY);
    if ((this.selected) && (mouseX >= this.selectedX) && (mouseX <= this.selectedX + 192) && (mouseY >= this.selectedY) && (mouseY <= this.selectedY + 8))
    {
      this.callback.setColor(ChatColor.values()[((int)(16.0D - Math.ceil((this.selectedX + 192 - mouseX) / 12.0D)))]);
      MinecraftFactory.getVars().getCurrentScreen().actionPerformed0(this);
      
      this.requireMoveOut = true;
      this.selected = false;
    }
    return result;
  }
  
  private int getBoxX()
  {
    return getX() + (getWidth() + MinecraftFactory.getVars().getStringWidth(getLabel())) / 2 + 4;
  }
  
  private int getBoxY()
  {
    return getY() + (getHeight() - this.boxHeight) / 2;
  }
}
