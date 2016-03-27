package eu.the5zig.mod.server.playminity;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus.FriendStatus;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.util.concurrent.GenericFutureListener;

public class PlayMinityListener
  extends ServerListener
{
  public PlayMinityListener(ServerInstance serverInstance)
  {
    super(serverInstance);
    
    registerListener(new PlayMinityJumpLeagueListener(serverInstance));
  }
  
  public void onServerJoin(String host, int port)
  {
    super.onServerJoin(host, port);
    if (host.toLowerCase().endsWith(".playminity.com")) {
      The5zigMod.getDataManager().setServer(new ServerPlayMinity(host, port));
    }
  }
  
  private void switchMode(ServerPlayMinity server)
  {
    if (server.getLobby().startsWith("JL"))
    {
      ServerPlayMinity tmp18_17 = server;tmp18_17.getClass();server.setGameMode(new ServerPlayMinity.JumpLeague(tmp18_17));
    }
    else
    {
      server.setGameMode(null);
    }
    The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.LOBBY, server.getLobbyString()), new GenericFutureListener[0]);
  }
  
  public void onPlayerListHeaderFooter(String header, String footer)
  {
    super.onPlayerListHeaderFooter(header, footer);
    if (!isCurrentServerInstance()) {
      return;
    }
    ServerPlayMinity server = (ServerPlayMinity)The5zigMod.getDataManager().getServer();
    footer = ChatColor.stripColor(footer);
    if (!footer.startsWith("Server: ")) {
      return;
    }
    server.setLobby(footer.substring("Server: ".length()));
    switchMode(server);
  }
}
