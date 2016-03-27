package eu.the5zig.mod.chat.network.packets;

import java.io.IOException;

public abstract interface Packet
{
  public abstract void read(PacketBuffer paramPacketBuffer)
    throws IOException;
  
  public abstract void write(PacketBuffer paramPacketBuffer)
    throws IOException;
  
  public abstract void handle();
}
