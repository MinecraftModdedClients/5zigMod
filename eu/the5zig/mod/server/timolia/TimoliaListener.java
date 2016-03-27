package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus.FriendStatus;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.server.ServerListener;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.util.concurrent.GenericFutureListener;

public class TimoliaListener
  extends ServerListener
{
  public TimoliaListener(ServerInstanceTimolia serverInstance)
  {
    super(serverInstance);
    
    registerListener(new Timolia4renaListener(serverInstance));
    registerListener(new TimoliaDNAListener(serverInstance));
    registerListener(new TimoliaPvPListener(serverInstance));
    registerListener(new TimoliaSplunListener(serverInstance));
    registerListener(new TimoliaBrainBowListener(serverInstance));
    registerListener(new TimoliaTSpieleListener(serverInstance));
    registerListener(new TimoliaInTimeListener(serverInstance));
    registerListener(new TimoliaArcadeListener(serverInstance));
    registerListener(new TimoliaAdventListener(serverInstance));
    registerListener(new TimoliaJumpWorldListener(serverInstance));
  }
  
  public void onServerJoin(String host, int port)
  {
    if (host.toLowerCase().endsWith("timolia.de")) {
      The5zigMod.getDataManager().setServer(new ServerTimolia(host, port));
    }
  }
  
  protected void onMatch(String key, PatternResult match)
  {
    GameMode gameMode = ((ServerTimolia)The5zigMod.getDataManager().getServer()).getGameMode();
    if (gameMode == null) {
      return;
    }
    if ((gameMode.getState() == GameState.LOBBY) && 
      (key.equals("leave"))) {
      gameMode.setTime(-1L);
    }
    if (gameMode.getState() == GameState.GAME)
    {
      if (key.equals("win"))
      {
        gameMode.setWinner(match.get(0));
        gameMode.setState(GameState.FINISHED);
      }
      if (key.equals("kill"))
      {
        if ((match.get(0).equals(The5zigMod.getDataManager().getUsername())) && (!(gameMode instanceof ServerTimolia.TSpiele)) && (!(gameMode instanceof ServerTimolia.InTime)))
        {
          gameMode.setDeaths(gameMode.getDeaths() + 1);
          gameMode.setKillStreak(0);
        }
        if (match.get(1).equals(The5zigMod.getDataManager().getUsername()))
        {
          gameMode.setKills(gameMode.getKills() + 1);
          gameMode.setKillStreak(gameMode.getKillStreak() + 1);
        }
      }
    }
  }
  
  public void onPlayerListHeaderFooter(String header, String footer)
  {
    Server currentServer = The5zigMod.getDataManager().getServer();
    if (currentServer == null) {
      return;
    }
    if (!(currentServer instanceof ServerTimolia)) {
      return;
    }
    if (!header.equals(ChatColor.GOLD + " « " + ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + "Timolia Netzwerk" + ChatColor.GOLD + " » ")) {
      return;
    }
    if (!footer.startsWith(ChatColor.GRAY + " Du spielst auf ")) {
      return;
    }
    ServerTimolia server = (ServerTimolia)currentServer;
    footer = ChatColor.stripColor(footer);
    
    String lobby = footer.split("Du spielst auf |\\.")[1];
    server.setLobby(lobby);
    switchMode(server);
  }
  
  private void switchMode(ServerTimolia server)
  {
    if (server.getLobby().startsWith("splun"))
    {
      ServerTimolia tmp18_17 = server;tmp18_17.getClass();server.setGameMode(new ServerTimolia.Splun(tmp18_17));
    }
    else if (server.getLobby().startsWith("dna"))
    {
      ServerTimolia tmp50_49 = server;tmp50_49.getClass();server.setGameMode(new ServerTimolia.DNA(tmp50_49));
    }
    else if (server.getLobby().startsWith("tspiele"))
    {
      ServerTimolia tmp82_81 = server;tmp82_81.getClass();server.setGameMode(new ServerTimolia.TSpiele(tmp82_81));
    }
    else if (server.getLobby().startsWith("4rena"))
    {
      ServerTimolia tmp114_113 = server;tmp114_113.getClass();server.setGameMode(new ServerTimolia.Arena(tmp114_113));
    }
    else if (server.getLobby().startsWith("brainbow"))
    {
      ServerTimolia tmp146_145 = server;tmp146_145.getClass();server.setGameMode(new ServerTimolia.BrainBow(tmp146_145));
    }
    else if (server.getLobby().startsWith("pvp"))
    {
      ServerTimolia tmp178_177 = server;tmp178_177.getClass();server.setGameMode(new ServerTimolia.PvP(tmp178_177));
    }
    else if (server.getLobby().startsWith("intime"))
    {
      ServerTimolia tmp210_209 = server;tmp210_209.getClass();server.setGameMode(new ServerTimolia.InTime(tmp210_209));
    }
    else if (server.getLobby().startsWith("arcade"))
    {
      ServerTimolia tmp242_241 = server;tmp242_241.getClass();server.setGameMode(new ServerTimolia.Arcade(tmp242_241));
    }
    else if (server.getLobby().startsWith("advent"))
    {
      ServerTimolia tmp274_273 = server;tmp274_273.getClass();server.setGameMode(new ServerTimolia.Advent(tmp274_273));
    }
    else if (server.getLobby().startsWith("jumpworld"))
    {
      ServerTimolia tmp306_305 = server;tmp306_305.getClass();server.setGameMode(new ServerTimolia.JumpWorld(tmp306_305));
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
  
  public boolean isCurrent()
  {
    Server currentServer = The5zigMod.getDataManager().getServer();
    return (currentServer != null) && ((currentServer instanceof ServerTimolia));
  }
}
