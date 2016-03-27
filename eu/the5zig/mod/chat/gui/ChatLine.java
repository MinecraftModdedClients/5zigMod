package eu.the5zig.mod.chat.gui;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Conversation;
import eu.the5zig.mod.chat.entity.FileMessage;
import eu.the5zig.mod.chat.entity.FileMessage.FileData;
import eu.the5zig.mod.chat.entity.Message;
import eu.the5zig.mod.chat.entity.Message.MessageType;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.GuiChat;
import eu.the5zig.mod.gui.TabConversations;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.List;

public class ChatLine
  implements Row
{
  public final float STATUS_SCALE = 0.6F;
  public final int LINE_HEIGHT = 12;
  public final int MESSAGE_HEIGHT = 18;
  private final Message message;
  
  public ChatLine(Message message)
  {
    this.message = message;
  }
  
  public static ChatLine fromMessage(Message message)
  {
    switch (message.getMessageType())
    {
    case CENTERED: 
      return new CenteredChatLine(message);
    case DATE: 
      return new DateChatLine(message);
    case IMAGE: 
      return new ImageChatLine(message);
    case AUDIO: 
      return new AudioChatLine(message);
    }
    return new ChatLine(message);
  }
  
  public void draw(int x, int y)
  {
    GuiChat gui = (GuiChat)The5zigMod.getVars().getCurrentScreen();
    
    String time = ChatColor.GRAY + Utils.convertToTimeWithMinutes(this.message.getTime());
    int timeWidth = (int)(The5zigMod.getVars().getStringWidth(time) * 0.6F);
    
    List<String> lines = The5zigMod.getVars().splitStringToWidth(this.message.toString(), getMaxMessageWidth());
    int yy = 2;
    int lineWidth;
    for (int i = 0; i < lines.size(); i++)
    {
      String line = (String)lines.get(i);
      lineWidth = The5zigMod.getVars().getStringWidth(line);
      int xOff = 0;
      if (i + 1 == lines.size()) {
        xOff += timeWidth;
      }
      if (this.message.getMessageType() == Message.MessageType.LEFT)
      {
        The5zigMod.getVars().drawString(line, x + 2, y + yy);
        if (xOff > 0) {
          Gui.drawScaledString(time, x + 2 + lineWidth + 6, y + yy + 3, 0.6F);
        }
      }
      if (this.message.getMessageType() == Message.MessageType.RIGHT) {
        if (xOff > 0)
        {
          The5zigMod.getVars().drawString(line, gui.getWidth() - 22 - lineWidth - xOff - 6, y + yy);
          Gui.drawScaledString(time, gui.getWidth() - 22 - timeWidth, y + yy + 3, 0.6F);
        }
        else
        {
          The5zigMod.getVars().drawString(line, gui.getWidth() - 22 - lineWidth, y + yy);
        }
      }
      yy += 12;
    }
    if (this.message.getMessageType() == Message.MessageType.RIGHT)
    {
      Message lastMessage = null;
      List<Message> messages = Lists.newArrayList(this.message.getConversation().getMessages());
      for (Message conversationMessage : messages) {
        if ((conversationMessage.getMessageType() == Message.MessageType.RIGHT) || (((conversationMessage instanceof FileMessage)) && 
          (((FileMessage)conversationMessage).getFileData().isOwn()))) {
          lastMessage = conversationMessage;
        }
      }
      if ((lastMessage == null) || (!lastMessage.equals(this.message))) {
        return;
      }
      String status;
      String status;
      String status;
      String status;
      switch (this.message.getConversation().getStatus())
      {
      case SENT: 
        status = I18n.translate("chat.status.sent");
        break;
      case DELIVERED: 
        status = I18n.translate("chat.status.delivered");
        break;
      case READ: 
        status = I18n.translate("chat.status.read");
        break;
      default: 
        status = I18n.translate("chat.status.pending");
      }
      String string = ChatColor.ITALIC.toString() + status;
      int stringWidth = (int)(0.6F * The5zigMod.getVars().getStringWidth(string));
      Gui.drawScaledString(string, gui.getWidth() - 22 - stringWidth, y + yy, 0.6F);
    }
  }
  
  public int getMaxMessageWidth()
  {
    if ((!(The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) || (!(((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations))) {
      return 100;
    }
    return ((TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab()).getChatBoxWidth() / 3 * 2;
  }
  
  public int getLineHeight()
  {
    List<?> objects = The5zigMod.getVars().splitStringToWidth(this.message.toString(), getMaxMessageWidth());
    return (objects.size() - 1) * 12 + 18;
  }
  
  public Message getMessage()
  {
    return this.message;
  }
}
