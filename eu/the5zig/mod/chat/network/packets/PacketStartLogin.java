package eu.the5zig.mod.chat.network.packets;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.IVariables;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.IOException;
import java.util.UUID;

public class PacketStartLogin
  implements Packet
{
  private String username;
  private boolean offlineMode;
  private String key;
  
  public PacketStartLogin(String username)
  {
    this.username = username;
  }
  
  public PacketStartLogin() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    this.offlineMode = buffer.readBoolean();
    if (!this.offlineMode) {
      this.key = buffer.readString();
    }
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeString(this.username);
  }
  
  public void handle()
  {
    if (!this.offlineMode)
    {
      MinecraftSessionService yggdrasil = new YggdrasilAuthenticationService(The5zigMod.getVars().getProxy(), UUID.randomUUID().toString()).createMinecraftSessionService();
      try
      {
        yggdrasil.joinServer(The5zigMod.getVars().getGameProfile(), The5zigMod.getDataManager().getSession(), this.key);
      }
      catch (AuthenticationException e)
      {
        The5zigMod.getNetworkManager().disconnect(I18n.translate("connection.bad_login"));
        throw new RuntimeException(e);
      }
      The5zigMod.getNetworkManager().sendPacket(new PacketLogin(The5zigMod.getDataManager().getUsername(), The5zigMod.getDataManager().getUniqueId()), new GenericFutureListener[0]);
    }
    else
    {
      The5zigMod.getNetworkManager().sendPacket(new PacketLogin(The5zigMod.getDataManager().getUsername(), The5zigMod.getDataManager().getUniqueId()), new GenericFutureListener[0]);
    }
  }
}
