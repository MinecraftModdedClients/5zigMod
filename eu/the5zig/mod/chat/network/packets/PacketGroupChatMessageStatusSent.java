package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.GroupChatManager;
import eu.the5zig.mod.chat.entity.Message.MessageStatus;
import java.io.IOException;

public class PacketGroupChatMessageStatusSent
  implements Packet
{
  private int groupId;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.groupId = buffer.readVarIntFromBuffer();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getConversationManager().setConversationStatus(The5zigMod.getConversationManager().getConversation(The5zigMod.getGroupChatManager().getGroup(this.groupId)), Message.MessageStatus.SENT);
  }
}
