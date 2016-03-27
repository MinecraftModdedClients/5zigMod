package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import java.io.IOException;

public class PacketFileTransferAbort
  implements Packet
{
  private int fileId;
  
  public PacketFileTransferAbort() {}
  
  public PacketFileTransferAbort(int fileId)
  {
    this.fileId = fileId;
  }
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.fileId = buffer.readVarIntFromBuffer();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeVarIntToBuffer(this.fileId);
  }
  
  public void handle()
  {
    The5zigMod.getConversationManager().handleFileAbort(this.fileId);
  }
}
