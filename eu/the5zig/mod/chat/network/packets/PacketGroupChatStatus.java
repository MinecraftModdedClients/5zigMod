package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.GroupChatManager;
import eu.the5zig.mod.chat.GroupMember;
import eu.the5zig.mod.chat.entity.Conversation;
import eu.the5zig.mod.chat.entity.ConversationGroupChat;
import eu.the5zig.mod.chat.entity.Group;
import eu.the5zig.mod.chat.entity.User;
import java.io.IOException;
import java.util.UUID;

public class PacketGroupChatStatus
  implements Packet
{
  private int groupId;
  private GroupAction groupAction;
  private User user;
  private String player;
  private UUID uuid;
  private boolean enabled;
  
  public PacketGroupChatStatus(int groupId, GroupAction action, String player)
  {
    this.groupId = groupId;
    this.groupAction = action;
    this.player = player;
  }
  
  public PacketGroupChatStatus(int groupId, GroupAction action, UUID player)
  {
    this.groupId = groupId;
    this.groupAction = action;
    this.uuid = player;
  }
  
  public PacketGroupChatStatus(int groupId, GroupAction action, UUID player, boolean enabled)
  {
    this.groupId = groupId;
    this.groupAction = action;
    this.uuid = player;
    this.enabled = enabled;
  }
  
  public PacketGroupChatStatus() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.groupId = buffer.readVarIntFromBuffer();
    int ordinal = buffer.readVarIntFromBuffer();
    if ((ordinal < 0) || (ordinal >= GroupAction.values().length)) {
      throw new IllegalArgumentException("Received Integer is out of enum range");
    }
    this.groupAction = GroupAction.values()[ordinal];
    if ((this.groupAction == GroupAction.ADD_PLAYER) || (this.groupAction == GroupAction.REMOVE_PLAYER) || (this.groupAction == GroupAction.OWNER)) {
      this.user = buffer.readUser();
    }
    if (this.groupAction == GroupAction.ADMIN)
    {
      this.uuid = buffer.readUUID();
      this.enabled = buffer.readBoolean();
    }
    if (this.groupAction == GroupAction.CHANGE_NAME) {
      this.player = buffer.readString();
    }
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeVarIntToBuffer(this.groupId);
    buffer.writeVarIntToBuffer(this.groupAction.ordinal());
    if ((this.groupAction == GroupAction.ADD_PLAYER) || (this.groupAction == GroupAction.CHANGE_NAME)) {
      buffer.writeString(this.player);
    }
    if ((this.groupAction == GroupAction.REMOVE_PLAYER) || (this.groupAction == GroupAction.OWNER)) {
      buffer.writeUUID(this.uuid);
    }
    if (this.groupAction == GroupAction.ADMIN)
    {
      buffer.writeUUID(this.uuid);
      buffer.writeBoolean(this.enabled);
    }
  }
  
  public void handle()
  {
    Group group = The5zigMod.getGroupChatManager().getGroup(this.groupId);
    if (group == null) {
      return;
    }
    switch (this.groupAction)
    {
    case ADD_PLAYER: 
      group.addMember(this.user);
      break;
    case REMOVE_PLAYER: 
      group.removeMember(this.user.getUniqueId());
      break;
    case ADMIN: 
      group.getMember(this.uuid).setType(this.enabled ? 1 : 0);
      break;
    case OWNER: 
      group.getMember(group.getOwner().getUniqueId()).setType(0);
      group.setOwner(this.user);
      group.getMember(this.user.getUniqueId()).setType(2);
      break;
    case CHANGE_NAME: 
      group.setName(this.player);
      synchronized (The5zigMod.getConversationManager().getConversations())
      {
        for (Conversation conversation : The5zigMod.getConversationManager().getConversations())
        {
          if (!(conversation instanceof ConversationGroupChat)) {
            return;
          }
          ConversationGroupChat c = (ConversationGroupChat)conversation;
          if (c.getGroupId() == this.groupId) {
            c.setName(this.player);
          }
        }
      }
      break;
    }
  }
  
  public static enum GroupAction
  {
    ADD_PLAYER,  REMOVE_PLAYER,  OWNER,  ADMIN,  CHANGE_NAME;
    
    private GroupAction() {}
  }
}
