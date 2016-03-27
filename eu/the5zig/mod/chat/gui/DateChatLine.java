package eu.the5zig.mod.chat.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Message;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.GuiChat;
import eu.the5zig.mod.gui.TabConversations;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;
import java.util.List;

public class DateChatLine
  extends CenteredChatLine
{
  public final int LINE_HEIGHT = 8;
  public final int MESSAGE_HEIGHT = 14;
  private long time;
  
  public DateChatLine(Message message)
  {
    super(message);
    this.time = message.getTime();
  }
  
  public void draw(int x, int y)
  {
    int yy = 2;
    float scale = 0.8F;
    String line = Utils.convertToDateWithoutTime(this.time).replace("Today", I18n.translate("profile.today")).replace("Yesterday", I18n.translate("profile.yesterday"));
    Gui.drawScaledString(line, x + 9 + getMaxMessageWidth() / 2 - (int)(The5zigMod.getVars().getStringWidth(line) * scale) / 2, y + yy, scale);
  }
  
  public int getLineHeight()
  {
    if (!(The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) {
      return 8;
    }
    List<?> objects = The5zigMod.getVars().splitStringToWidth(getMessage().getMessage(), getMaxMessageWidth());
    return (objects.size() - 1) * 8 + 14;
  }
  
  public int getMaxMessageWidth()
  {
    if ((!(The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) || (!(((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations))) {
      return 100;
    }
    return ((TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab()).getChatBoxWidth() - 20;
  }
}
