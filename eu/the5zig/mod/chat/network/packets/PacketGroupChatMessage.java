package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.GroupChatManager;
import eu.the5zig.mod.chat.entity.Group;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.util.IVariables;
import java.io.IOException;

public class PacketGroupChatMessage
  implements Packet
{
  private int id;
  private String username;
  private String message;
  private long time;
  
  public PacketGroupChatMessage(int id, String message, long time)
  {
    this.id = id;
    this.message = message;
    this.time = time;
  }
  
  public PacketGroupChatMessage() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.id = buffer.readVarIntFromBuffer();
    this.username = buffer.readString();
    this.message = buffer.readString();
    this.time = buffer.readLong();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeVarIntToBuffer(this.id);
    buffer.writeString(this.message);
    buffer.writeLong(this.time);
  }
  
  public void handle()
  {
    Group group = The5zigMod.getGroupChatManager().getGroup(this.id);
    if (group == null) {
      return;
    }
    if (The5zigMod.getConfig().getBool("playMessageSounds")) {
      The5zigMod.getVars().playSound("the5zigmod", "chat.message.receive", 1.0F);
    }
    The5zigMod.getConversationManager().handleGroupChatMessage(group, this.username, this.message, this.time);
  }
}
