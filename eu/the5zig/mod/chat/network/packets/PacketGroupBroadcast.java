package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.GroupChatManager;
import eu.the5zig.mod.chat.entity.Group;
import java.io.IOException;

public class PacketGroupBroadcast
  implements Packet
{
  private int groupId;
  private String message;
  private long time;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.groupId = buffer.readVarIntFromBuffer();
    this.message = buffer.readString();
    this.time = buffer.readLong();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    Group group = The5zigMod.getGroupChatManager().getGroup(this.groupId);
    if (group == null) {
      return;
    }
    The5zigMod.getConversationManager().handleGroupBroadcast(group, this.message, this.time);
  }
}
