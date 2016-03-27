package eu.the5zig.mod.config.items;

import com.google.gson.JsonObject;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Friend.OnlineStatus;
import eu.the5zig.mod.chat.entity.Profile;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketProfile;
import eu.the5zig.mod.manager.DataManager;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.UUID;

public class OnlineStatusItem
  extends SliderItem
{
  public OnlineStatusItem(String key, String category, Float defaultValue)
  {
    super(key, "", category, defaultValue.floatValue(), 0.0F, 2.0F, 1);
  }
  
  public Float get()
  {
    return Float.valueOf(The5zigMod.getDataManager().getProfile().getOnlineStatus().ordinal());
  }
  
  public void set(Float value)
  {
    The5zigMod.getDataManager().getProfile().setOnlineStatus(Friend.OnlineStatus.values()[value.intValue()]);
  }
  
  public void serialize(JsonObject object) {}
  
  public void deserialize(JsonObject object) {}
  
  public void action()
  {
    if (this.changed)
    {
      The5zigMod.getNetworkManager().sendPacket(new PacketProfile(The5zigMod.getDataManager().getProfile().getOnlineStatus()), new GenericFutureListener[0]);
      this.changed = false;
    }
  }
  
  public String getCustomValue(float value)
  {
    Friend.OnlineStatus status = Friend.OnlineStatus.values()[((int)(value * 2.0F))];
    return (status == Friend.OnlineStatus.AWAY) && (The5zigMod.getDataManager().getUniqueId().toString().equals("8340212f-d91d-4875-98a2-7a3a16e0c6e5")) ? "Fappen" : I18n.translate(status
      .getName());
  }
}
