package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import java.io.IOException;

public class PacketAnnouncement
  implements Packet
{
  private String message;
  private long time;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.message = buffer.readString();
    this.time = buffer.readLong();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getConversationManager().addAnnouncementMessage(this.message, this.time);
  }
}
