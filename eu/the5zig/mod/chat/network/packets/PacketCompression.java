package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import java.io.IOException;
import org.apache.logging.log4j.Logger;

public class PacketCompression
  implements Packet
{
  private int threshold;
  
  public PacketCompression(int threshold)
  {
    this.threshold = threshold;
  }
  
  public PacketCompression() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.threshold = buffer.readVarIntFromBuffer();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getNetworkManager().setThreshold(this.threshold);
    The5zigMod.logger.debug("Enabled Compression (threshold=" + this.threshold + ")");
  }
  
  public int getThreshold()
  {
    return this.threshold;
  }
}
