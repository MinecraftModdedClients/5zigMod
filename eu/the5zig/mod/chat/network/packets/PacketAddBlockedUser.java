package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.User;
import java.io.IOException;

public class PacketAddBlockedUser
  implements Packet
{
  private User blockedUser;
  
  public PacketAddBlockedUser(User blockedUser)
  {
    this.blockedUser = blockedUser;
  }
  
  public PacketAddBlockedUser() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.blockedUser = buffer.readUser();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeUser(this.blockedUser);
  }
  
  public void handle()
  {
    The5zigMod.getFriendManager().addBlockedUser(this.blockedUser);
  }
}
