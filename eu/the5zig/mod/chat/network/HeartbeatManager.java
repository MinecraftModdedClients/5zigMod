package eu.the5zig.mod.chat.network;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.packets.PacketHeartbeat;
import io.netty.util.concurrent.GenericFutureListener;

public class HeartbeatManager
{
  private long lastHeartbeat;
  
  public HeartbeatManager()
  {
    this.lastHeartbeat = System.currentTimeMillis();
  }
  
  public void onTick()
  {
    if (System.currentTimeMillis() - this.lastHeartbeat > 20000L) {
      The5zigMod.getNetworkManager().disconnect(I18n.translate("connection.timed_out"));
    }
  }
  
  public void onHeartbeatReceive(int id)
  {
    this.lastHeartbeat = System.currentTimeMillis();
    The5zigMod.getNetworkManager().sendPacket(new PacketHeartbeat(id), new GenericFutureListener[0]);
  }
}
