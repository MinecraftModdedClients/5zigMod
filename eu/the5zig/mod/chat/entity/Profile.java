package eu.the5zig.mod.chat.entity;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketCapeSettings;
import eu.the5zig.mod.chat.network.packets.PacketCapeSettings.Action;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus.FriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketProfile;
import eu.the5zig.mod.chat.network.packets.PacketProfile.ProfileType;
import eu.the5zig.mod.manager.DataManager;
import io.netty.util.concurrent.GenericFutureListener;

public class Profile
{
  private final int id;
  private final Rank rank;
  private final long firstTime;
  private String profileMessage;
  private Friend.OnlineStatus onlineStatus;
  private boolean showServer;
  private boolean showMessageRead;
  private boolean showFriendRequests;
  private boolean showCape;
  private boolean showCountry;
  
  public Profile(int id, Rank rank, long firstTime, String profileMessage, Friend.OnlineStatus onlineStatus, boolean showServer, boolean showMessageRead, boolean showFriendRequests, boolean showCape, boolean showCountry)
  {
    this.id = id;
    this.rank = rank;
    this.firstTime = firstTime;
    this.profileMessage = profileMessage;
    this.onlineStatus = onlineStatus;
    this.showServer = showServer;
    this.showMessageRead = showMessageRead;
    this.showFriendRequests = showFriendRequests;
    this.showCape = showCape;
    this.showCountry = showCountry;
  }
  
  public int getId()
  {
    return this.id;
  }
  
  public Rank getRank()
  {
    return this.rank;
  }
  
  public long getFirstTime()
  {
    return this.firstTime;
  }
  
  public String getProfileMessage()
  {
    return this.profileMessage;
  }
  
  public void setProfileMessage(String profileMessage)
  {
    this.profileMessage = profileMessage;
    The5zigMod.getNetworkManager().sendPacket(new PacketProfile(profileMessage), new GenericFutureListener[0]);
  }
  
  public Friend.OnlineStatus getOnlineStatus()
  {
    return this.onlineStatus;
  }
  
  public void setOnlineStatus(Friend.OnlineStatus onlineStatus)
  {
    this.onlineStatus = onlineStatus;
  }
  
  public boolean isShowServer()
  {
    return this.showServer;
  }
  
  public void setShowServer(boolean showServer)
  {
    this.showServer = showServer;
    The5zigMod.getNetworkManager().sendPacket(new PacketProfile(PacketProfile.ProfileType.SHOW_SERVER, showServer), new GenericFutureListener[0]);
    if (!showServer) {
      The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.SERVER, ""), new GenericFutureListener[0]);
    } else {
      The5zigMod.getDataManager().updateCurrentLobby();
    }
  }
  
  public boolean isShowMessageRead()
  {
    return this.showMessageRead;
  }
  
  public void setShowMessageRead(boolean showMessageRead)
  {
    this.showMessageRead = showMessageRead;
    The5zigMod.getNetworkManager().sendPacket(new PacketProfile(PacketProfile.ProfileType.SHOW_MESSAGE_READ, showMessageRead), new GenericFutureListener[0]);
  }
  
  public boolean isShowFriendRequests()
  {
    return this.showFriendRequests;
  }
  
  public void setShowFriendRequests(boolean showFriendRequests)
  {
    this.showFriendRequests = showFriendRequests;
    The5zigMod.getNetworkManager().sendPacket(new PacketProfile(PacketProfile.ProfileType.SHOW_FRIEND_REQUESTS, showFriendRequests), new GenericFutureListener[0]);
  }
  
  public boolean isShowCountry()
  {
    return this.showCountry;
  }
  
  public void setShowCountry(boolean showCountry)
  {
    this.showCountry = showCountry;
    The5zigMod.getNetworkManager().sendPacket(new PacketProfile(PacketProfile.ProfileType.SHOW_COUNTRY, showCountry), new GenericFutureListener[0]);
  }
  
  public boolean isCapeEnabled()
  {
    return (getRank() != Rank.NONE) && (this.showCape);
  }
  
  public void setCapeEnabled(boolean capeEnabled)
  {
    this.showCape = capeEnabled;
    The5zigMod.getNetworkManager().sendPacket(new PacketCapeSettings(PacketCapeSettings.Action.SETTINGS, capeEnabled), new GenericFutureListener[0]);
  }
}
