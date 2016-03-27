package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.ConnectionState;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.manager.DataManager;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.IOException;

public class PacketHandshake
  implements Packet
{
  public void read(PacketBuffer buffer)
    throws IOException
  {}
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeVarIntToBuffer(2);
  }
  
  public void handle()
  {
    The5zigMod.getNetworkManager().setConnectState(ConnectionState.LOGIN);
    The5zigMod.getNetworkManager().sendPacket(new PacketStartLogin(The5zigMod.getDataManager().getUsername()), new GenericFutureListener[0]);
  }
}
