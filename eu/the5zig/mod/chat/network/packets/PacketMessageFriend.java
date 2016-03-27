package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.util.IVariables;
import java.io.IOException;
import java.util.UUID;

public class PacketMessageFriend
  implements Packet
{
  private UUID friend;
  private String username;
  private String message;
  private long time;
  
  public PacketMessageFriend(UUID friend, String message, long time)
  {
    this.friend = friend;
    this.message = message;
    this.time = time;
  }
  
  public PacketMessageFriend() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.friend = buffer.readUUID();
    this.username = buffer.readString();
    this.message = buffer.readString();
    this.time = buffer.readLong();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeUUID(this.friend);
    buffer.writeString(this.message);
    buffer.writeLong(this.time);
  }
  
  public void handle()
  {
    if (The5zigMod.getConfig().getBool("playMessageSounds")) {
      The5zigMod.getVars().playSound("the5zigmod", "chat.message.receive", 1.0F);
    }
    The5zigMod.getConversationManager().handleFriendMessageReceive(this.friend, this.username, this.message, this.time);
  }
}
