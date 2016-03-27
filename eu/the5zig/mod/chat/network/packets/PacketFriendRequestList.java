package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.User;
import eu.the5zig.mod.gui.IOverlay;
import eu.the5zig.util.minecraft.ChatColor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketFriendRequestList
  implements Packet
{
  private List<User> friendRequests;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    int size = buffer.readInt();
    this.friendRequests = new ArrayList(size);
    for (int i = 0; i < size; i++)
    {
      User friendRequest = buffer.readUser();
      this.friendRequests.add(friendRequest);
    }
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getFriendManager().setFriendRequests(this.friendRequests);
    if (this.friendRequests.size() > 0) {
      The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.GREEN + I18n.translate("friend.new_requests", new Object[] { Integer.valueOf(this.friendRequests.size()) }));
    }
  }
}
