package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.ConnectionState;
import eu.the5zig.mod.chat.network.NetworkManager;
import java.io.IOException;
import java.util.UUID;

public class PacketLogin
  implements Packet
{
  private String username;
  private UUID uuid;
  
  public PacketLogin(String username, UUID uuid)
  {
    this.username = username;
    this.uuid = uuid;
  }
  
  public PacketLogin() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {}
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeString(this.username);
    buffer.writeUUID(this.uuid);
  }
  
  public void handle()
  {
    The5zigMod.getNetworkManager().setConnectState(ConnectionState.PLAY);
  }
}
