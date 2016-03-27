package eu.the5zig.mod.server.mineplex;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus.FriendStatus;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.util.concurrent.GenericFutureListener;

public class MineplexListener
  extends ServerListener
{
  public MineplexListener(ServerInstance serverInstance)
  {
    super(serverInstance);
  }
  
  public void onServerJoin(String host, int port)
  {
    if ((host.toLowerCase().endsWith("mineplex.com")) || (host.toLowerCase().endsWith("mineplex.com."))) {
      The5zigMod.getDataManager().setServer(new ServerMineplex(host, 25565));
    }
  }
  
  protected void onMatch(String key, PatternResult match)
  {
    ServerMineplex server = (ServerMineplex)The5zigMod.getDataManager().getServer();
    if (key.equals("lobby"))
    {
      String lobby = match.get(0);
      server.setLobby(lobby);
    }
  }
  
  public void onPlayerListHeaderFooter(String header, String footer)
  {
    super.onPlayerListHeaderFooter(header, footer);
    if (!isCurrent()) {
      return;
    }
    if ((header.startsWith(ChatColor.BOLD + "Mineplex Network   " + ChatColor.GREEN + "Lobby-")) || (header.startsWith(ChatColor.GOLD.toString() + ChatColor.BOLD.toString())))
    {
      ServerMineplex server = (ServerMineplex)The5zigMod.getDataManager().getServer();
      sendAndIgnore("/server", "lobby");
      String gameType = header.split(ChatColor.GREEN.toString() + "|" + ChatColor.GOLD.toString() + ChatColor.BOLD.toString())[1];
      switchMode(server, gameType);
    }
  }
  
  private void switchMode(ServerMineplex server, String gameType)
  {
    if (gameType.equals("Dragon Escape"))
    {
      ServerMineplex tmp15_14 = server;tmp15_14.getClass();server.setGameMode(new ServerMineplex.DragonEscape(tmp15_14));
    }
    else
    {
      server.setGameMode(null);
    }
    The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.LOBBY, server.getLobbyString() == null ? gameType : server.getLobbyString()), new GenericFutureListener[0]);
  }
  
  public boolean isCurrent()
  {
    Server currentServer = The5zigMod.getDataManager().getServer();
    return (currentServer != null) && ((currentServer instanceof ServerMineplex));
  }
}
