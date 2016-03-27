package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.ChatTypingManager;
import eu.the5zig.mod.manager.DataManager;
import java.io.IOException;
import java.util.UUID;

public class PacketTyping
  implements Packet
{
  private UUID friend;
  private boolean typing;
  
  public PacketTyping(UUID friend, boolean typing)
  {
    this.friend = friend;
    this.typing = typing;
  }
  
  public PacketTyping() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.friend = buffer.readUUID();
    this.typing = buffer.readBoolean();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeUUID(this.friend);
    buffer.writeBoolean(this.typing);
  }
  
  public void handle()
  {
    ChatTypingManager manager = The5zigMod.getDataManager().getChatTypingManager();
    if (this.typing) {
      manager.addToTyping(this.friend);
    } else {
      manager.removeFromTyping(this.friend);
    }
  }
}
