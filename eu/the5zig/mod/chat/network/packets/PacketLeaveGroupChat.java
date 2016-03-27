package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import java.io.IOException;

public class PacketLeaveGroupChat
  implements Packet
{
  private int groupId;
  
  public PacketLeaveGroupChat(int groupId)
  {
    this.groupId = groupId;
  }
  
  public PacketLeaveGroupChat() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.groupId = buffer.readVarIntFromBuffer();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeVarIntToBuffer(this.groupId);
  }
  
  public void handle()
  {
    The5zigMod.getConversationManager().deleteGroupConversation(this.groupId);
  }
}
