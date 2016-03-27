package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.FriendManager;
import java.io.IOException;
import java.util.UUID;

public class PacketDeleteFriend
  implements Packet
{
  private UUID friend;
  
  public PacketDeleteFriend(UUID friend)
  {
    this.friend = friend;
  }
  
  public PacketDeleteFriend() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.friend = buffer.readUUID();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeUUID(this.friend);
  }
  
  public void handle()
  {
    The5zigMod.getFriendManager().removeFriend(this.friend);
  }
}
