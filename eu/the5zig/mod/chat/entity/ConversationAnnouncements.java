package eu.the5zig.mod.chat.entity;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;

public class ConversationAnnouncements
  extends Conversation
{
  public ConversationAnnouncements(int conversationId, long lastUsed, boolean read, Conversation.Behaviour behaviour)
  {
    super(conversationId, lastUsed, read, Message.MessageStatus.PENDING, behaviour);
  }
  
  public int getLineHeight()
  {
    return 18;
  }
  
  public void draw(int x, int y)
  {
    The5zigMod.getVars().drawString((isRead() ? "" : ChatColor.BOLD) + I18n.translate("announcement.short_desc"), x + 2, y + 2);
  }
}
