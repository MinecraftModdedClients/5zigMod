package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.util.Utils;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.IOException;
import java.util.Locale;

public class PacketClientStats
  implements Packet
{
  public void read(PacketBuffer buffer)
    throws IOException
  {}
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeString("3.5.3");
    buffer.writeString("1.9");
    buffer.writeString(Utils.getOSName());
    buffer.writeString(Utils.getJava());
    buffer.writeString(Locale.getDefault().toString());
  }
  
  public void handle()
  {
    The5zigMod.getNetworkManager().sendPacket(new PacketClientStats(), new GenericFutureListener[0]);
  }
}
