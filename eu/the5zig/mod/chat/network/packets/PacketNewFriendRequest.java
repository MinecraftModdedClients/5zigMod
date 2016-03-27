package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.User;
import java.io.IOException;

public class PacketNewFriendRequest
  implements Packet
{
  private User friendRequest;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.friendRequest = buffer.readUser();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeUser(this.friendRequest);
  }
  
  public void handle()
  {
    The5zigMod.getFriendManager().addFriendRequest(this.friendRequest);
  }
}
