package eu.the5zig.mod.chat.network.packets;

import java.io.IOException;
import java.util.UUID;

public class PacketFriendRequestResponse
  implements Packet
{
  private UUID friend;
  private boolean accepted;
  
  public PacketFriendRequestResponse(UUID friend, boolean accepted)
  {
    this.friend = friend;
    this.accepted = accepted;
  }
  
  public PacketFriendRequestResponse() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {}
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeUUID(this.friend);
    buffer.writeBoolean(this.accepted);
  }
  
  public void handle() {}
}
