package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.NetworkStats;
import eu.the5zig.mod.manager.DataManager;
import java.io.IOException;

public class PacketServerStats
  implements Packet
{
  private int connectedClients;
  private int maxClients;
  private long startTime;
  private int ping;
  private int lag;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.connectedClients = buffer.readInt();
    this.maxClients = buffer.readInt();
    this.startTime = buffer.readLong();
    this.ping = buffer.readInt();
    this.lag = buffer.readInt();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    NetworkStats networkStats = The5zigMod.getDataManager().getNetworkStats();
    networkStats.setConnectedClients(this.connectedClients);
    networkStats.setMaxClients(this.maxClients);
    networkStats.setStartTime(this.startTime);
    networkStats.setPing(this.ping);
    networkStats.setLag(this.lag);
  }
}
