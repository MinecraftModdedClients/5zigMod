package eu.the5zig.mod.chat.network.packets;

import java.io.IOException;

public class PacketFriendRequest
  implements Packet
{
  private String friend;
  
  public PacketFriendRequest(String friend)
  {
    this.friend = friend;
  }
  
  public PacketFriendRequest() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {}
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeString(this.friend);
  }
  
  public void handle() {}
}
