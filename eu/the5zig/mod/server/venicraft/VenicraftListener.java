package eu.the5zig.mod.server.venicraft;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus.FriendStatus;
import eu.the5zig.mod.gui.ingame.Scoreboard;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VenicraftListener
  extends ServerListener
{
  private static final Pattern GAMEMODE_PATTERN = Pattern.compile("\\sDu spielst auf einem .+ Server » (.+)\\.\\s");
  
  public VenicraftListener(ServerInstance serverInstance)
  {
    super(serverInstance);
    
    registerListener(new MineathlonListener(serverInstance));
  }
  
  public void onServerJoin(String host, int port)
  {
    if (host.toLowerCase().endsWith("venicraft.at")) {
      The5zigMod.getDataManager().setServer(new ServerVenicraft(host, port));
    }
  }
  
  public void onPlayerListHeaderFooter(String header, String footer)
  {
    Server currentServer = The5zigMod.getDataManager().getServer();
    if (currentServer == null) {
      return;
    }
    if (!(currentServer instanceof ServerVenicraft)) {
      return;
    }
    if (!header.equals("  " + ChatColor.GREEN + "◀ " + ChatColor.DARK_PURPLE + ChatColor.BOLD
      .toString() + "VeniCraft " + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "Servernetzwerk" + ChatColor.WHITE + " " + ChatColor.GREEN + "▶   ")) {
      return;
    }
    ServerVenicraft server = (ServerVenicraft)currentServer;
    Matcher matcher = GAMEMODE_PATTERN.matcher(ChatColor.stripColor(footer));
    if (matcher.matches())
    {
      String lobby = matcher.group(1);
      server.setLobby(lobby);
    }
    else
    {
      server.setLobby("");
    }
    switchMode(server);
  }
  
  public void onTick()
  {
    super.onTick();
    if (isCurrentServerInstance())
    {
      ServerVenicraft server = (ServerVenicraft)The5zigMod.getDataManager().getServer();
      if ((server.getLobby() == null) || (server.getLobby().isEmpty()))
      {
        Scoreboard scoreboard = The5zigMod.getVars().getScoreboard();
        if (scoreboard != null)
        {
          String lobby = (String)scoreboard.getLines().get(Integer.valueOf(3));
          if (lobby != null)
          {
            server.setLobby(ChatColor.stripColor(lobby));
            switchMode(server);
          }
        }
      }
    }
  }
  
  private void switchMode(ServerVenicraft server)
  {
    if (server.getLobby().startsWith("ma"))
    {
      ServerVenicraft tmp18_17 = server;tmp18_17.getClass();server.setGameMode(new ServerVenicraft.Mineathlon(tmp18_17));
    }
    else if (server.getLobby().startsWith("cd"))
    {
      ServerVenicraft tmp50_49 = server;tmp50_49.getClass();server.setGameMode(new ServerVenicraft.CrystalDefense(tmp50_49));
    }
    else if (server.getLobby().startsWith("sg"))
    {
      ServerVenicraft tmp82_81 = server;tmp82_81.getClass();server.setGameMode(new ServerVenicraft.SurvivalGames(tmp82_81));
    }
    else
    {
      server.setGameMode(null);
    }
    if (server.getGameMode() != null) {
      onGameModeJoin();
    }
    The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.LOBBY, server.getLobbyString()), new GenericFutureListener[0]);
  }
}
