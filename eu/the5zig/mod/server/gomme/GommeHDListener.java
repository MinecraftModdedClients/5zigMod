package eu.the5zig.mod.server.gomme;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus.FriendStatus;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;
import eu.the5zig.mod.server.Teamable;
import io.netty.util.concurrent.GenericFutureListener;

public class GommeHDListener
  extends ServerListener
{
  public GommeHDListener(ServerInstance serverInstance)
  {
    super(serverInstance);
    registerListener(new GommeHDPvPListener(serverInstance));
    registerListener(new GommeHDPvPFFAListener(serverInstance));
    registerListener(new GommeHDSurvivalGamesListener(serverInstance));
    registerListener(new GommeHDEnderGamesListener(serverInstance));
    registerListener(new GommeHDBedWarsListener(serverInstance));
    registerListener(new GommeHDSkyWarsListener(serverInstance));
  }
  
  public void onServerJoin(String host, int port)
  {
    super.onServerJoin(host, port);
    if ((host.toLowerCase().endsWith("gommehd.net")) || (host.toLowerCase().endsWith("gommehd.de"))) {
      The5zigMod.getDataManager().setServer(new ServerGommeHD(host, port));
    }
  }
  
  protected void onMatch(String key, PatternResult match)
  {
    ServerGommeHD server = (ServerGommeHD)The5zigMod.getDataManager().getServer();
    if (key.equals("lobby"))
    {
      String lobby = match.get(0);
      if (lobby.length() > 36) {
        lobby = lobby.split(":")[1];
      }
      server.setLobby(lobby);
      switchMode((ServerGommeHD)The5zigMod.getDataManager().getServer(), lobby);
    }
    if (server.getGameMode() == null) {
      return;
    }
    GameMode gameMode = server.getGameMode();
    if ((gameMode instanceof Teamable))
    {
      if (key.equals("teams.allowed")) {
        ((Teamable)gameMode).setTeamsAllowed(true);
      }
      if (key.equals("teams.not_allowed")) {
        ((Teamable)gameMode).setTeamsAllowed(false);
      }
    }
    if (gameMode.getState() == GameState.GAME)
    {
      if ((key.equals("kill")) && (match.get(1).equals(The5zigMod.getDataManager().getUsername())))
      {
        gameMode.setKills(gameMode.getKills() + 1);
        gameMode.setKillStreak(gameMode.getKillStreak() + 1);
      }
      if ((key.equals("death")) || ((key.equals("kill")) && (match.get(0).equals(The5zigMod.getDataManager().getUsername()))))
      {
        gameMode.setDeaths(gameMode.getDeaths() + 1);
        gameMode.setKillStreak(0);
      }
    }
  }
  
  public void onServerConnect()
  {
    super.onServerConnect();
    if (isCurrentServerInstance()) {
      sendAndIgnore("/whereami", "lobby");
    }
  }
  
  private void switchMode(ServerGommeHD server, String gameType)
  {
    if ((gameType.startsWith("SG24")) || (gameType.equals("TEAMSG24")) || (gameType.startsWith("SG64")) || (gameType.equals("TEAMSG64")) || (gameType.equals("QSG")) || (gameType.equals("TEAMQSG")))
    {
      ServerGommeHD tmp60_59 = server;tmp60_59.getClass();server.setGameMode(new ServerGommeHD.SurvivalGames(tmp60_59));
    }
    else if ((gameType.equals("BedWars")) || (gameType.equals("BW")))
    {
      ServerGommeHD tmp98_97 = server;tmp98_97.getClass();server.setGameMode(new ServerGommeHD.BedWars(tmp98_97));
    }
    else if ((gameType.equals("EnderGames")) || (gameType.equals("EG")))
    {
      ServerGommeHD tmp136_135 = server;tmp136_135.getClass();server.setGameMode(new ServerGommeHD.EnderGames(tmp136_135));
    }
    else if (gameType.startsWith("PVPM"))
    {
      ServerGommeHD tmp165_164 = server;tmp165_164.getClass();server.setGameMode(new ServerGommeHD.PvPMatch(tmp165_164));
    }
    else if (gameType.startsWith("PVP"))
    {
      ServerGommeHD tmp194_193 = server;tmp194_193.getClass();server.setGameMode(new ServerGommeHD.PvP(tmp194_193));
    }
    else if ((gameType.startsWith("FFACLASSIC")) || (gameType.startsWith("FFAHARDCORE")) || (gameType.startsWith("FFASOUP")) || (gameType.startsWith("FFAGUNGAME")))
    {
      ServerGommeHD tmp250_249 = server;tmp250_249.getClass();server.setGameMode(new ServerGommeHD.FFA(tmp250_249));
    }
    else if (gameType.startsWith("SKYWARS"))
    {
      ServerGommeHD tmp279_278 = server;tmp279_278.getClass();server.setGameMode(new ServerGommeHD.SkyWars(tmp279_278));
    }
    else
    {
      server.setGameMode(null);
    }
    if (server.getGameMode() != null) {
      onGameModeJoin();
    }
    The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.LOBBY, server.getLobbyString() == null ? gameType : server.getLobbyString()), new GenericFutureListener[0]);
  }
  
  public boolean isCurrent()
  {
    Server currentServer = The5zigMod.getDataManager().getServer();
    return (currentServer != null) && ((currentServer instanceof ServerGommeHD));
  }
}
