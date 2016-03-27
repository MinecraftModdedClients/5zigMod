package eu.the5zig.mod.server.bergwerk;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus.FriendStatus;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.util.concurrent.GenericFutureListener;

public class BergwerkListener
  extends ServerListener
{
  public BergwerkListener(ServerInstance serverInstance)
  {
    super(serverInstance);
    
    registerListener(new BergwerkFlashListener(serverInstance));
    registerListener(new BergwerkDuelListener(serverInstance));
  }
  
  public void onServerJoin(String host, int port)
  {
    super.onServerJoin(host, port);
    if (host.toLowerCase().endsWith("bergwerklabs.de")) {
      The5zigMod.getDataManager().setServer(new ServerBergwerk(host, port));
    }
  }
  
  public void onPlayerListHeaderFooter(String header, String footer)
  {
    super.onPlayerListHeaderFooter(header, footer);
    Server currentServer = The5zigMod.getDataManager().getServer();
    if (!isCurrent()) {
      return;
    }
    ServerBergwerk server = (ServerBergwerk)currentServer;
    footer = ChatColor.stripColor(footer);
    if (!footer.startsWith("Du befindest dich auf: ")) {
      return;
    }
    switchMode(server, footer.split(": ")[1]);
  }
  
  private void switchMode(ServerBergwerk server, String gameType)
  {
    server.setLobby(gameType);
    if (gameType.startsWith("FLASH"))
    {
      ServerBergwerk tmp20_19 = server;tmp20_19.getClass();server.setGameMode(new ServerBergwerk.Flash(tmp20_19));
    }
    else if (gameType.startsWith("DUEL_"))
    {
      ServerBergwerk tmp49_48 = server;tmp49_48.getClass();server.setGameMode(new ServerBergwerk.Duel(tmp49_48));
    }
    else
    {
      server.setGameMode(null);
    }
    The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.LOBBY, server.getLobbyString()), new GenericFutureListener[0]);
  }
  
  public boolean isCurrent()
  {
    Server currentServer = The5zigMod.getDataManager().getServer();
    return (currentServer != null) && ((currentServer instanceof ServerBergwerk));
  }
}
