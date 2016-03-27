package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.HeartbeatManager;
import eu.the5zig.mod.chat.network.NetworkManager;
import java.io.IOException;

public class PacketHeartbeat
  implements Packet
{
  private int id;
  
  public PacketHeartbeat(int id)
  {
    this.id = id;
  }
  
  public PacketHeartbeat() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.id = buffer.readInt();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeInt(this.id);
  }
  
  public void handle()
  {
    The5zigMod.getNetworkManager().getHeartbeatManager().onHeartbeatReceive(this.id);
  }
}
