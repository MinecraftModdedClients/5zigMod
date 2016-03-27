package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.GroupChatManager;
import eu.the5zig.mod.chat.entity.Group;
import java.io.IOException;

public class PacketDeleteGroupChat
  implements Packet
{
  private int groupId;
  
  public PacketDeleteGroupChat(int groupId)
  {
    this.groupId = groupId;
  }
  
  public PacketDeleteGroupChat() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.groupId = buffer.readVarIntFromBuffer();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeVarIntToBuffer(this.groupId);
  }
  
  public void handle()
  {
    Group group = The5zigMod.getGroupChatManager().getGroup(this.groupId);
    if (group == null) {
      return;
    }
    The5zigMod.getGroupChatManager().removeGroup(group);
  }
}
