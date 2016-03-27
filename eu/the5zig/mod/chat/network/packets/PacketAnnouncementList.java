package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.Announcement;
import eu.the5zig.mod.chat.ConversationManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketAnnouncementList
  implements Packet
{
  private List<Announcement> announcements;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    int size = buffer.readVarIntFromBuffer();
    this.announcements = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      this.announcements.add(new Announcement(buffer.readLong(), buffer.readString()));
    }
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getConversationManager().setAnnouncementMessages(this.announcements);
  }
}
