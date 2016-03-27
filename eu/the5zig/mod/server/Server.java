package eu.the5zig.mod.server;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus.FriendStatus;
import eu.the5zig.mod.config.LastServer;
import eu.the5zig.mod.config.LastServerConfiguration;
import eu.the5zig.mod.util.IVariables;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.commons.lang3.StringUtils;

public class Server
{
  private String host;
  private int port;
  private long time;
  private transient boolean renderPotionEffects = true;
  private transient boolean renderArmor = true;
  private transient boolean renderPotionIndicator = true;
  private transient boolean renderSaturation = true;
  private transient boolean renderEntityHealth = false;
  
  public Server() {}
  
  public Server(String host, int port)
  {
    this.host = host;
    this.port = port;
    this.time = System.currentTimeMillis();
    The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.SERVER, host + ":" + port), new GenericFutureListener[0]);
    ((LastServer)The5zigMod.getLastServerConfig().getConfigInstance()).setLastServer(this);
    save();
  }
  
  private void save()
  {
    The5zigMod.getLastServerConfig().saveConfig();
  }
  
  public String getHost()
  {
    return this.host;
  }
  
  public void setHost(String host)
  {
    if (!StringUtils.equals(this.host, host)) {
      The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.SERVER, host + ":" + this.port), new GenericFutureListener[0]);
    }
    this.host = host;
    save();
  }
  
  public int getPort()
  {
    return this.port;
  }
  
  public void setPort(int port)
  {
    if (this.port != port) {
      The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.SERVER, this.host + ":" + port), new GenericFutureListener[0]);
    }
    this.port = port;
    save();
  }
  
  public long getLastTimeJoined()
  {
    return this.time;
  }
  
  public boolean isRenderPotionEffects()
  {
    return this.renderPotionEffects;
  }
  
  public void setRenderPotionEffects(boolean renderPotionEffects)
  {
    this.renderPotionEffects = renderPotionEffects;
  }
  
  public boolean isRenderArmor()
  {
    return this.renderArmor;
  }
  
  public void setRenderArmor(boolean renderArmor)
  {
    this.renderArmor = renderArmor;
  }
  
  public boolean isRenderPotionIndicator()
  {
    return this.renderPotionIndicator;
  }
  
  public void setRenderPotionIndicator(boolean renderPotionIndicator)
  {
    this.renderPotionIndicator = renderPotionIndicator;
  }
  
  public boolean isRenderSaturation()
  {
    return this.renderSaturation;
  }
  
  public void setRenderSaturation(boolean renderSaturation)
  {
    this.renderSaturation = renderSaturation;
  }
  
  public boolean isRenderEntityHealth()
  {
    return this.renderEntityHealth;
  }
  
  public void setRenderEntityHealth(boolean renderEntityHealth)
  {
    this.renderEntityHealth = renderEntityHealth;
  }
  
  public int getPlayers()
  {
    return The5zigMod.getVars().getServerPlayers();
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    Server server = (Server)o;
    if (this.port != server.port) {
      return false;
    }
    return this.host.equals(server.host);
  }
  
  public String toString()
  {
    return "Server{host='" + this.host + '\'' + ", port=" + this.port + '}';
  }
}
