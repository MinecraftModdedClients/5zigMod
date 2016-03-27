package eu.the5zig.mod.chat.gui;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Message;
import eu.the5zig.mod.gui.GuiChat;
import eu.the5zig.mod.gui.TabConversations;
import eu.the5zig.mod.util.IVariables;
import java.util.List;

public class CenteredChatLine
  extends ChatLine
{
  public CenteredChatLine(Message message)
  {
    super(message);
  }
  
  public void draw(int x, int y)
  {
    List<?> lines = The5zigMod.getVars().splitStringToWidth(getMessage().getMessage(), getMaxMessageWidth());
    int yy = 2;
    for (Object object : lines)
    {
      String line = String.valueOf(object);
      The5zigMod.getVars().drawString(line, x + 9 + getMaxMessageWidth() / 2 - The5zigMod.getVars().getStringWidth(line) / 2, y + yy);
      yy += 12;
    }
  }
  
  public int getLineHeight()
  {
    if (!(The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) {
      return 12;
    }
    List<?> objects = The5zigMod.getVars().splitStringToWidth(getMessage().getMessage(), getMaxMessageWidth());
    return (objects.size() - 1) * 12 + 18;
  }
  
  public int getMaxMessageWidth()
  {
    if ((!(The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) || (!(((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations))) {
      return 100;
    }
    return ((TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab()).getChatBoxWidth() - 20;
  }
}
