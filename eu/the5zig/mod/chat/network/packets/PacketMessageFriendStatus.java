package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.Friend;
import eu.the5zig.mod.chat.entity.Message.MessageStatus;
import java.io.IOException;
import java.util.UUID;

public class PacketMessageFriendStatus
  implements Packet
{
  private UUID friend;
  private Message.MessageStatus messageStatus;
  
  public PacketMessageFriendStatus(UUID friend, Message.MessageStatus messageStatus)
  {
    this.friend = friend;
    this.messageStatus = messageStatus;
  }
  
  public PacketMessageFriendStatus() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.friend = buffer.readUUID();
    this.messageStatus = Message.MessageStatus.values()[buffer.readVarIntFromBuffer()];
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeUUID(this.friend);
    buffer.writeVarIntToBuffer(this.messageStatus.ordinal());
  }
  
  public void handle()
  {
    Friend f = The5zigMod.getFriendManager().getFriend(this.friend);
    if ((f == null) || (!The5zigMod.getConversationManager().conversationExists(f))) {
      return;
    }
    The5zigMod.getConversationManager().setConversationStatus(The5zigMod.getConversationManager().getConversation(f), this.messageStatus);
  }
}
