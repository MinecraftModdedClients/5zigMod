package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import java.io.IOException;
import java.util.UUID;

public class PacketFileTransferId
  implements Packet
{
  private UUID uuid;
  private int conversationId;
  private int fileId;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.uuid = buffer.readUUID();
    this.conversationId = buffer.readVarIntFromBuffer();
    this.fileId = buffer.readVarIntFromBuffer();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getConversationManager().handleFileId(this.uuid, this.conversationId, this.fileId);
  }
}
