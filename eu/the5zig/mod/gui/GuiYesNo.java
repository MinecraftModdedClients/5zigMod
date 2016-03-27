package eu.the5zig.mod.gui;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.util.IVariables;
import java.util.Iterator;
import java.util.List;

public class GuiYesNo
  extends Gui
{
  private final YesNoCallback callback;
  
  public GuiYesNo(Gui lastScreen, YesNoCallback callback)
  {
    super(lastScreen);
    this.callback = callback;
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(1, getWidth() / 2 - 152, getHeight() / 6 + 140, 150, 20, The5zigMod.getVars().translate("gui.yes", new Object[0])));
    addButton(The5zigMod.getVars().createButton(2, getWidth() / 2 + 2, getHeight() / 6 + 140, 150, 20, The5zigMod.getVars().translate("gui.no", new Object[0])));
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    int maxStringWidth = getWidth() / 4 * 3;
    List<String> strings = The5zigMod.getVars().splitStringToWidth(this.callback.title(), maxStringWidth);
    int yOff = 0;
    for (Iterator localIterator = strings.iterator(); localIterator.hasNext(); drawCenteredString(string, getWidth() / 2, getHeight() / 6 + yOff))
    {
      String string = (String)localIterator.next();
      yOff += 12;
    }
  }
  
  protected void actionPerformed(IButton button)
  {
    if ((button.getId() == 1) || (button.getId() == 2))
    {
      this.callback.onDone(button.getId() == 1);
      The5zigMod.getVars().displayScreen(this.lastScreen);
    }
  }
  
  public String getTitleName()
  {
    return "";
  }
}
