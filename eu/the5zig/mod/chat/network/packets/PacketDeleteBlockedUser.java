package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.FriendManager;
import java.io.IOException;
import java.util.UUID;

public class PacketDeleteBlockedUser
  implements Packet
{
  private UUID blockedUser;
  
  public PacketDeleteBlockedUser(UUID blockedUser)
  {
    this.blockedUser = blockedUser;
  }
  
  public PacketDeleteBlockedUser() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.blockedUser = buffer.readUUID();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeUUID(this.blockedUser);
  }
  
  public void handle()
  {
    The5zigMod.getFriendManager().removeBlockedUser(this.blockedUser);
  }
}
