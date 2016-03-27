package eu.the5zig.mod.chat.entity;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.UUID;

public class ConversationChat
  extends Conversation
{
  private final UUID friendUUID;
  private String friendName;
  
  public ConversationChat(int conversationId, String friendName, UUID friendUUID, long lastUsed, boolean read, Message.MessageStatus status, Conversation.Behaviour behaviour)
  {
    super(conversationId, lastUsed, read, status, behaviour);
    this.friendName = friendName;
    this.friendUUID = friendUUID;
  }
  
  public String getFriendName()
  {
    return this.friendName;
  }
  
  public void setFriendName(String friendName)
  {
    this.friendName = friendName;
  }
  
  public UUID getFriendUUID()
  {
    return this.friendUUID;
  }
  
  public int getLineHeight()
  {
    return 18;
  }
  
  public void draw(int x, int y)
  {
    The5zigMod.getVars().drawString((isRead() ? "" : ChatColor.BOLD) + this.friendName, x + 2, y + 2);
  }
}
