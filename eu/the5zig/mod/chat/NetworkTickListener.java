package eu.the5zig.mod.chat;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.HeartbeatManager;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.listener.Listener;
import eu.the5zig.mod.manager.DataManager;

public class NetworkTickListener
  extends Listener
{
  public void onTick()
  {
    NetworkManager networkManager = The5zigMod.getNetworkManager();
    networkManager.tick();
    if (networkManager.isConnected())
    {
      if (networkManager.getHeartbeatManager() != null) {
        networkManager.getHeartbeatManager().onTick();
      }
      The5zigMod.getDataManager().getNetworkStats().tick();
    }
  }
}
