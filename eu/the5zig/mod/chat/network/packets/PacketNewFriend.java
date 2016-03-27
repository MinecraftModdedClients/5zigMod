package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.Friend;
import java.io.IOException;

public class PacketNewFriend
  implements Packet
{
  private Friend friend;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.friend = buffer.readFriend();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getFriendManager().addFriend(this.friend);
  }
}
