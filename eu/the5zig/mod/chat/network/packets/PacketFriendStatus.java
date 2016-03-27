package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.Friend;
import eu.the5zig.mod.chat.entity.Friend.OnlineStatus;
import eu.the5zig.mod.chat.entity.Rank;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.gui.GuiChat;
import eu.the5zig.mod.gui.IOverlay;
import eu.the5zig.mod.gui.TabFriends;
import eu.the5zig.mod.manager.ChatTypingManager;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.LocaleUtils;

public class PacketFriendStatus
  implements Packet
{
  private UUID uuid;
  private FriendStatus friendStatus;
  private boolean enabled;
  private String status;
  private Rank rank;
  private long time;
  private Friend.OnlineStatus onlineStatus;
  
  public PacketFriendStatus(FriendStatus friendStatus, String status)
  {
    this.friendStatus = friendStatus;
    this.status = status;
  }
  
  public PacketFriendStatus(FriendStatus friendStatus, Friend.OnlineStatus status)
  {
    this.friendStatus = friendStatus;
    this.onlineStatus = status;
  }
  
  public PacketFriendStatus(FriendStatus friendStatus, boolean enabled)
  {
    this.friendStatus = friendStatus;
    this.enabled = enabled;
  }
  
  public PacketFriendStatus(UUID friend, boolean enabled)
  {
    this.friendStatus = FriendStatus.FAVORITE;
    this.uuid = friend;
    this.enabled = enabled;
  }
  
  public PacketFriendStatus() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    int i = buffer.readInt();
    if ((i < 0) || (i >= FriendStatus.values().length)) {
      throw new IllegalArgumentException("Received Integer is out of enum range");
    }
    this.friendStatus = FriendStatus.values()[i];
    this.uuid = buffer.readUUID();
    if (this.friendStatus == FriendStatus.ONLINE_STATUS)
    {
      int ordinal = buffer.readVarIntFromBuffer();
      if ((ordinal < 0) || (ordinal >= Friend.OnlineStatus.values().length)) {
        throw new IllegalArgumentException("Received Integer is out of enum range");
      }
      this.onlineStatus = Friend.OnlineStatus.values()[ordinal];
      if (this.onlineStatus == Friend.OnlineStatus.OFFLINE) {
        this.time = buffer.readLong();
      }
    }
    if ((this.friendStatus == FriendStatus.PROFILE_MESSAGE) || (this.friendStatus == FriendStatus.SERVER) || (this.friendStatus == FriendStatus.LOBBY) || (this.friendStatus == FriendStatus.LOCALE)) {
      this.status = buffer.readString();
    }
    if (this.friendStatus == FriendStatus.FAVORITE) {
      this.enabled = buffer.readBoolean();
    }
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeInt(this.friendStatus.ordinal());
    if ((this.friendStatus == FriendStatus.SERVER) || (this.friendStatus == FriendStatus.LOBBY)) {
      buffer.writeString(this.status);
    }
    if (this.friendStatus == FriendStatus.FAVORITE)
    {
      buffer.writeUUID(this.uuid);
      buffer.writeBoolean(this.enabled);
    }
    if (this.friendStatus == FriendStatus.ONLINE_STATUS) {
      buffer.writeVarIntToBuffer(this.onlineStatus.ordinal());
    }
  }
  
  public void handle()
  {
    Friend friend = The5zigMod.getFriendManager().getFriend(this.uuid);
    if (friend == null) {
      return;
    }
    if (this.friendStatus == FriendStatus.ONLINE_STATUS)
    {
      if (The5zigMod.getConfig().getBool("showOnlineMessages")) {
        if ((friend.getStatus() == Friend.OnlineStatus.AWAY) && (this.onlineStatus == Friend.OnlineStatus.ONLINE))
        {
          String theStatus = friend.getUniqueId().toString().equals("8340212f-d91d-4875-98a2-7a3a16e0c6e5") ? "fappen" : Friend.OnlineStatus.AWAY.getDisplayName().toLowerCase();
          The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.YELLOW + 
            I18n.translate("friend.online_message.no_longer", new Object[] {friend.getName(), theStatus + ChatColor.YELLOW }));
        }
        else
        {
          String theStatus = this.onlineStatus.getDisplayName().toLowerCase() + ChatColor.YELLOW;
          The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.YELLOW + I18n.translate("friend.online_message", new Object[] { friend.getName(), theStatus }));
          if ((this.onlineStatus == Friend.OnlineStatus.OFFLINE) && (The5zigMod.getDataManager().getChatTypingManager().isTyping(this.uuid))) {
            The5zigMod.getDataManager().getChatTypingManager().removeFromTyping(this.uuid);
          }
        }
      }
      friend.setStatus(this.onlineStatus);
      if (this.onlineStatus == Friend.OnlineStatus.OFFLINE) {
        friend.setLastOnline(System.currentTimeMillis());
      }
      The5zigMod.getFriendManager().sortFriends();
    }
    if (this.friendStatus == FriendStatus.PROFILE_MESSAGE) {
      friend.setStatusMessage(this.status);
    }
    if (this.friendStatus == FriendStatus.SERVER) {
      friend.setServer(this.status.isEmpty() ? null : this.status);
    }
    if (this.friendStatus == FriendStatus.LOBBY) {
      friend.setLobby(this.status.isEmpty() ? null : this.status);
    }
    if (this.friendStatus == FriendStatus.CHANGE_NAME)
    {
      friend.setName(this.status);
      The5zigMod.getConversationManager().updateConversationNames(friend);
    }
    if (this.friendStatus == FriendStatus.LOCALE) {
      friend.setLocale(this.status.isEmpty() ? null : LocaleUtils.toLocale(this.status));
    }
    if (this.friendStatus == FriendStatus.FAVORITE)
    {
      friend.setFavorite(this.enabled);
      if (((The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) && ((((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabFriends)))
      {
        TabFriends gui = (TabFriends)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab();
        Friend selectedFriend = gui.getSelectedFriend();
        if (selectedFriend != null)
        {
          The5zigMod.getFriendManager().sortFriends();
          gui.onSelect(The5zigMod.getFriendManager().getFriends().indexOf(friend), friend, false);
        }
      }
      else
      {
        The5zigMod.getFriendManager().sortFriends();
      }
    }
  }
  
  public static enum FriendStatus
  {
    ONLINE_STATUS,  PROFILE_MESSAGE,  SERVER,  LOBBY,  CHANGE_NAME,  FAVORITE,  LOCALE;
    
    private FriendStatus() {}
  }
}
