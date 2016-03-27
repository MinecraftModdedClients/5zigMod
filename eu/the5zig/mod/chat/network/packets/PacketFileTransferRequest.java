package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.util.IVariables;
import java.io.IOException;
import java.util.UUID;

public class PacketFileTransferRequest
  implements Packet
{
  private UUID uuid;
  private int fileId;
  private PacketFileTransferStart.Type type;
  private long length;
  
  public PacketFileTransferRequest(UUID uuid, int fileId, PacketFileTransferStart.Type type, long length)
  {
    this.uuid = uuid;
    this.fileId = fileId;
    this.type = type;
    this.length = length;
  }
  
  public PacketFileTransferRequest() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.uuid = buffer.readUUID();
    this.fileId = buffer.readVarIntFromBuffer();
    this.type = ((PacketFileTransferStart.Type)buffer.readEnum(PacketFileTransferStart.Type.class));
    this.length = buffer.readLong();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeUUID(this.uuid);
    buffer.writeVarIntToBuffer(this.fileId);
    buffer.writeEnum(this.type);
    buffer.writeLong(this.length);
  }
  
  public void handle()
  {
    if (The5zigMod.getConfig().getBool("playMessageSounds")) {
      The5zigMod.getVars().playSound("the5zigmod", "chat.message.receive", 1.0F);
    }
    The5zigMod.getConversationManager().handleFileRequest(this.uuid, this.fileId, this.type, this.length);
  }
}
