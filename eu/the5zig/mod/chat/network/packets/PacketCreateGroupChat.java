package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.GroupChatManager;
import eu.the5zig.mod.chat.entity.Group;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PacketCreateGroupChat
  implements Packet
{
  private List<UUID> players;
  private String name;
  private Group group;
  
  public PacketCreateGroupChat(List<UUID> players, String name)
  {
    this.players = players;
    this.name = name;
  }
  
  public PacketCreateGroupChat() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.group = buffer.readGroup();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeInt(this.players.size());
    for (UUID player : this.players) {
      buffer.writeUUID(player);
    }
    buffer.writeString(this.name);
  }
  
  public void handle()
  {
    The5zigMod.getGroupChatManager().addGroup(this.group);
  }
}
