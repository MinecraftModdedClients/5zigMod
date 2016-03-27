package eu.the5zig.mod.server.hypixel;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus.FriendStatus;
import eu.the5zig.mod.config.LastServer;
import eu.the5zig.mod.config.LastServerConfiguration;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIManager;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HypixelListener
  extends ServerListener
{
  private final Pattern gameModePattern = Pattern.compile("s[0-9]{1,4}|mini[0-9]+.+");
  
  public HypixelListener(ServerInstance serverInstance)
  {
    super(serverInstance);
    
    registerListener(new HypixelQuakeListener(serverInstance));
    registerListener(new HypixelBlitzListener(serverInstance));
    registerListener(new HypixelPaintballListener(serverInstance));
  }
  
  public void onServerJoin(String host, int port)
  {
    super.onServerJoin(host, port);
    if (host.equalsIgnoreCase("mc.hypixel.net"))
    {
      if (!The5zigMod.getHypixelAPIManager().hasKey()) {
        sendAndIgnore("/api", "api");
      }
      LastServer configuration = (LastServer)The5zigMod.getLastServerConfig().getConfigInstance();
      if ((configuration.getLastServer() != null) && ((configuration.getLastServer() instanceof ServerHypixel)))
      {
        The5zigMod.getDataManager().setServer(configuration.getLastServer());
        return;
      }
      The5zigMod.getDataManager().setServer(new ServerHypixel(host, port));
    }
  }
  
  public void onMatch(String key, PatternResult match)
  {
    super.onMatch(key, match);
    if (!isCurrentServerInstance()) {
      return;
    }
    ServerHypixel server = (ServerHypixel)The5zigMod.getDataManager().getServer();
    if (key.equals("lobby"))
    {
      server.setLobby(match.get(0));
      switchMode(server);
    }
    if (server.getGameMode() != null)
    {
      GameMode gameMode = server.getGameMode();
      if ((gameMode.getState() == GameState.GAME) && 
        (key.equals("win")))
      {
        gameMode.setWinner(match.get(0));
        gameMode.setState(GameState.FINISHED);
      }
    }
    if (key.equals("api")) {
      The5zigMod.getHypixelAPIManager().setKey(UUID.fromString(match.get(0)));
    }
  }
  
  public void onServerConnect()
  {
    super.onServerConnect();
    if (isCurrentServerInstance()) {
      sendAndIgnore("/whereami", "lobby");
    }
  }
  
  private void switchMode(ServerHypixel server)
  {
    if (this.gameModePattern.matcher(server.getLobby()).matches())
    {
      The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.LOBBY, server.getLobbyString()), new GenericFutureListener[0]);
      return;
    }
    if (server.getLobby().startsWith("quakelobby"))
    {
      ServerHypixel tmp60_59 = server;tmp60_59.getClass();server.setGameMode(new ServerHypixel.Quake(tmp60_59));
    }
    else if (server.getLobby().startsWith("blitzlobby"))
    {
      ServerHypixel tmp92_91 = server;tmp92_91.getClass();server.setGameMode(new ServerHypixel.Blitz(tmp92_91));
    }
    else if (server.getLobby().startsWith("paintballlobby"))
    {
      ServerHypixel tmp124_123 = server;tmp124_123.getClass();server.setGameMode(new ServerHypixel.Paintball(tmp124_123));
    }
    else
    {
      server.setGameMode(null);
    }
    The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.LOBBY, server.getLobbyString()), new GenericFutureListener[0]);
  }
}
