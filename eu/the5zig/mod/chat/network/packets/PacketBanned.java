package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.GuiBanned;
import eu.the5zig.mod.util.IVariables;
import java.io.IOException;

public class PacketBanned
  implements Packet
{
  private String reason;
  private long time;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.reason = buffer.readString();
    this.time = buffer.readLong();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getVars().displayScreen(new GuiBanned(this.reason, this.time));
  }
}
