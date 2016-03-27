package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Friend.OnlineStatus;
import eu.the5zig.mod.chat.entity.Profile;
import eu.the5zig.mod.chat.entity.Rank;
import eu.the5zig.mod.manager.DataManager;
import java.io.IOException;

public class PacketProfile
  implements Packet
{
  private int id;
  private Rank rank;
  private long firstConnectTime;
  private String profileMessage;
  private Friend.OnlineStatus onlineStatus;
  private boolean showServer;
  private boolean showMessageRead;
  private boolean showFriendRequests;
  private boolean showCape;
  private boolean showCountry;
  private ProfileType profileType;
  private boolean show;
  
  public PacketProfile(String profileMessage)
  {
    this.profileType = ProfileType.PROFILE_MESSAGE;
    this.profileMessage = profileMessage;
  }
  
  public PacketProfile(Friend.OnlineStatus onlineStatus)
  {
    this.profileType = ProfileType.ONLINE_STATUS;
    this.onlineStatus = onlineStatus;
  }
  
  public PacketProfile(ProfileType profileType, boolean show)
  {
    this.profileType = profileType;
    this.show = show;
  }
  
  public PacketProfile() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.id = buffer.readVarIntFromBuffer();
    this.rank = buffer.readRank();
    this.firstConnectTime = buffer.readLong();
    this.profileMessage = buffer.readString();
    int ordinal = buffer.readVarIntFromBuffer();
    if ((ordinal < 0) || (ordinal >= Friend.OnlineStatus.values().length)) {
      throw new IllegalArgumentException("Received Integer is out of enum range");
    }
    this.onlineStatus = Friend.OnlineStatus.values()[ordinal];
    this.showServer = buffer.readBoolean();
    this.showMessageRead = buffer.readBoolean();
    this.showFriendRequests = buffer.readBoolean();
    this.showCape = buffer.readBoolean();
    this.showCountry = buffer.readBoolean();
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeVarIntToBuffer(this.profileType.ordinal());
    if (this.profileType == ProfileType.PROFILE_MESSAGE) {
      buffer.writeString(this.profileMessage);
    } else if (this.profileType == ProfileType.ONLINE_STATUS) {
      buffer.writeVarIntToBuffer(this.onlineStatus.ordinal());
    } else {
      buffer.writeBoolean(this.show);
    }
  }
  
  public void handle()
  {
    The5zigMod.getDataManager().setProfile(new Profile(this.id, this.rank, this.firstConnectTime, this.profileMessage, this.onlineStatus, this.showServer, this.showMessageRead, this.showFriendRequests, this.showCape, this.showCountry));
  }
  
  public static enum ProfileType
  {
    PROFILE_MESSAGE,  ONLINE_STATUS,  SHOW_SERVER,  SHOW_MESSAGE_READ,  SHOW_FRIEND_REQUESTS,  SHOW_COUNTRY;
    
    private ProfileType() {}
  }
}
