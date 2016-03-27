package eu.the5zig.mod.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.listener.EventListener;
import eu.the5zig.mod.manager.DataManager;
import java.util.List;
import java.util.Map;

public abstract class ServerInstance
{
  private static final Map<String, ServerInstance> BY_CONFIG_NAME = ;
  
  public ServerInstance()
  {
    The5zigMod.getListener().register(getListener());
    BY_CONFIG_NAME.put(getConfigName(), this);
  }
  
  public boolean isConnectedTo()
  {
    return (The5zigMod.getDataManager().getServer() != null) && (getServer().isAssignableFrom(The5zigMod.getDataManager().getServer().getClass()));
  }
  
  public abstract ServerListener getListener();
  
  public abstract String getName();
  
  public abstract String getConfigName();
  
  public abstract Class<? extends GameServer> getServer();
  
  public static ServerInstance byConfigName(String configName)
  {
    return (ServerInstance)BY_CONFIG_NAME.get(configName);
  }
  
  public static List<String> getServerNames()
  {
    return Lists.newArrayList(BY_CONFIG_NAME.keySet());
  }
}
