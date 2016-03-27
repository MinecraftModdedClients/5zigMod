package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Profile;
import eu.the5zig.mod.manager.DataManager;
import java.io.IOException;

public class PacketSettings
  implements Packet
{
  private SettingType settingType;
  private String status;
  private boolean enabled;
  
  public PacketSettings(SettingType settingType, String status)
  {
    this.settingType = settingType;
    this.status = status;
  }
  
  public PacketSettings(boolean enabled, SettingType settingType)
  {
    this.enabled = enabled;
    this.settingType = settingType;
  }
  
  public PacketSettings() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    int i = buffer.readInt();
    if ((i < 0) || (i >= SettingType.values().length)) {
      throw new IllegalArgumentException("Received Integer is out of enum range");
    }
    this.settingType = SettingType.values()[i];
    if (this.settingType == SettingType.PROFILE_MESSAGE) {
      this.status = buffer.readString();
    }
    if ((this.settingType == SettingType.SHOW_CURRENT_SERVER) || (this.settingType == SettingType.SHOW_LAST_SEEN_TIME)) {
      this.enabled = buffer.readBoolean();
    }
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeInt(this.settingType.ordinal());
    if (this.settingType == SettingType.PROFILE_MESSAGE) {
      buffer.writeString(this.status);
    }
    if ((this.settingType == SettingType.SHOW_CURRENT_SERVER) || (this.settingType == SettingType.SHOW_LAST_SEEN_TIME)) {
      buffer.writeBoolean(this.enabled);
    }
  }
  
  public void handle()
  {
    if (this.settingType == SettingType.PROFILE_MESSAGE) {
      The5zigMod.getDataManager().getProfile().setProfileMessage(this.status);
    }
  }
  
  public static enum SettingType
  {
    PROFILE_MESSAGE,  SHOW_LAST_SEEN_TIME,  SHOW_CURRENT_SERVER;
    
    private SettingType() {}
  }
}
