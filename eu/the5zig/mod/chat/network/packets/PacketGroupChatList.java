package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.GroupChatManager;
import eu.the5zig.mod.chat.entity.Group;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketGroupChatList
  implements Packet
{
  private List<Group> groups;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    int size = buffer.readVarIntFromBuffer();
    this.groups = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      this.groups.add(buffer.readGroup());
    }
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getGroupChatManager().setGroups(this.groups);
  }
}
