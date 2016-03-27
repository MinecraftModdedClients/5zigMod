package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.network.filetransfer.FileUploadTask.Chunk;
import java.io.IOException;

public class PacketFileTransferChunk
  implements Packet
{
  private int transferId;
  private int partId;
  private byte[] data;
  private int length;
  
  public PacketFileTransferChunk(int transferId, int partId, FileUploadTask.Chunk chunk)
  {
    this.transferId = transferId;
    this.partId = partId;
    this.data = chunk.getData();
    this.length = chunk.getLength();
  }
  
  public PacketFileTransferChunk() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.transferId = buffer.readVarIntFromBuffer();
    this.partId = buffer.readVarIntFromBuffer();
    this.data = new byte[buffer.readableBytes()];
    buffer.readBytes(this.data);
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeVarIntToBuffer(this.transferId);
    buffer.writeVarIntToBuffer(this.partId);
    buffer.writeBytes(this.data, 0, this.length);
  }
  
  public void handle()
  {
    The5zigMod.getConversationManager().handleFileChunk(this.transferId, this.partId, this.data);
  }
}
