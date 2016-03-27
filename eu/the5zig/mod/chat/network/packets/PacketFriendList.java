package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.Friend;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketFriendList
  implements Packet
{
  private List<Friend> friendList;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    int size = buffer.readInt();
    this.friendList = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      this.friendList.add(buffer.readFriend());
    }
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getFriendManager().setFriends(this.friendList);
  }
}
