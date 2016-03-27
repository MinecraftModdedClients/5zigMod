package eu.the5zig.mod.chat.entity;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;

public class ConversationGroupChat
  extends Conversation
{
  private final int groupId;
  private String name;
  
  public ConversationGroupChat(int conversationId, int groupId, String name, long lastUsed, boolean read, Message.MessageStatus status, Conversation.Behaviour behaviour)
  {
    super(conversationId, lastUsed, read, status, behaviour);
    this.groupId = groupId;
    this.name = name;
  }
  
  public int getGroupId()
  {
    return this.groupId;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  public int getLineHeight()
  {
    return 18;
  }
  
  public void draw(int x, int y)
  {
    String displayName = this.name;
    boolean changed = false;
    while (The5zigMod.getVars().getStringWidth((!isRead() ? ChatColor.BOLD : "") + displayName) > 92 - The5zigMod.getVars().getStringWidth("..."))
    {
      displayName = displayName.substring(0, displayName.length() - 1);
      changed = true;
    }
    if (changed) {
      displayName = displayName + "...";
    }
    The5zigMod.getVars().drawString((isRead() ? "" : ChatColor.BOLD) + displayName, x + 2, y + 2);
  }
}
