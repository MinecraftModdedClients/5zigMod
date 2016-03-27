package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import java.io.IOException;

public class PacketFileTransferResponse
  implements Packet
{
  private int fileId;
  private boolean accepted;
  
  public PacketFileTransferResponse(int fileId, boolean accepted)
  {
    this.fileId = fileId;
    this.accepted = accepted;
  }
  
  public PacketFileTransferResponse() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.fileId = buffer.readVarIntFromBuffer();
    this.accepted = buffer.readBoolean();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeVarIntToBuffer(this.fileId);
    buffer.writeBoolean(this.accepted);
  }
  
  public void handle()
  {
    The5zigMod.getConversationManager().handleFileResponse(this.fileId, this.accepted);
  }
}
