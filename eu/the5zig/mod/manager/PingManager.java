package eu.the5zig.mod.manager;

import com.mojang.authlib.GameProfile;
import eu.the5zig.mod.The5zigMod;
import java.util.UUID;

public class PingManager
{
  private int ms = -1;
  private UUID serverSideUUID;
  
  public int getMs()
  {
    return this.ms;
  }
  
  public void handlePlayerInfo(boolean updatePing, int ping, GameProfile gameProfile)
  {
    if ((!gameProfile.getId().equals(The5zigMod.getDataManager().getUniqueId())) && (gameProfile.getName() != null) && (gameProfile.getName().equals(
      The5zigMod.getDataManager().getUsername())))
    {
      if (this.serverSideUUID == null) {
        this.serverSideUUID = gameProfile.getId();
      }
    }
    else if (gameProfile.getId().equals(The5zigMod.getDataManager().getUniqueId())) {
      this.serverSideUUID = gameProfile.getId();
    }
    if (!gameProfile.getId().equals(this.serverSideUUID)) {
      return;
    }
    if (updatePing) {
      this.ms = ping;
    }
  }
  
  public void reset()
  {
    this.ms = -1;
    this.serverSideUUID = null;
  }
}
