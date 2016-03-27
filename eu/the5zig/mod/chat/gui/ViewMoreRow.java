package eu.the5zig.mod.chat.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Message;
import eu.the5zig.mod.chat.entity.Message.MessageType;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.RowExtended;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callback;

public class ViewMoreRow
  extends ChatLine
  implements RowExtended
{
  private IButton button;
  private Callback<IButton> actionPerformed;
  
  public ViewMoreRow(int center, Callback<IButton> actionPerformed)
  {
    super(new Message(null, -1, "", "", -1L, Message.MessageType.CENTERED));
    String str = I18n.translate("chat.view_more");
    int strWidth = The5zigMod.getVars().getStringWidth(str);
    this.button = The5zigMod.getVars().createStringButton(51, center - strWidth / 2, 0, strWidth, 10, str);
    this.actionPerformed = actionPerformed;
  }
  
  public void draw(int x, int y) {}
  
  public void draw(int x, int y, int slotHeight, int mouseX, int mouseY)
  {
    this.button.setY(y + 2);
    this.button.draw(mouseX, mouseY);
  }
  
  public IButton mousePressed(int mouseX, int mouseY)
  {
    if (this.button.mouseClicked(mouseX, mouseY))
    {
      this.actionPerformed.call(this.button);
      return this.button;
    }
    return null;
  }
}
