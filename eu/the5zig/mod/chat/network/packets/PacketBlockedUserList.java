package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketBlockedUserList
  implements Packet
{
  private List<User> blockedUsers;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    int size = buffer.readInt();
    this.blockedUsers = new ArrayList(size);
    for (int i = 0; i < size; i++)
    {
      User blockedUser = buffer.readUser();
      this.blockedUsers.add(blockedUser);
    }
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getFriendManager().setBlockedUsers(this.blockedUsers);
  }
}
