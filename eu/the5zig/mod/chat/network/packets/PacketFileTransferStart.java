package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import java.io.IOException;

public class PacketFileTransferStart
  implements Packet
{
  private int fileId;
  private int parts;
  private int chunkSize;
  
  public PacketFileTransferStart(int fileId, int parts, int chunkSize)
  {
    this.fileId = fileId;
    this.parts = parts;
    this.chunkSize = chunkSize;
  }
  
  public PacketFileTransferStart() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.fileId = buffer.readVarIntFromBuffer();
    this.parts = buffer.readVarIntFromBuffer();
    this.chunkSize = buffer.readVarIntFromBuffer();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeVarIntToBuffer(this.fileId);
    buffer.writeVarIntToBuffer(this.parts);
    buffer.writeVarIntToBuffer(this.chunkSize);
  }
  
  public void handle()
  {
    The5zigMod.getConversationManager().handleFileStart(this.fileId, this.parts, this.chunkSize);
  }
  
  public static enum Type
  {
    IMAGE,  AUDIO;
    
    private Type() {}
  }
}
