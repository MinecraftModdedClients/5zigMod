package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import java.io.IOException;

public class PacketDisconnect
  implements Packet
{
  private String reason;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.reason = buffer.readString();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getNetworkManager().disconnect(this.reason);
  }
}
