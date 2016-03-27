package eu.the5zig.mod.manager;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.api.ServerAPIBackend;
import eu.the5zig.mod.chat.ChatBackgroundManager;
import eu.the5zig.mod.chat.NetworkStats;
import eu.the5zig.mod.chat.entity.Friend.OnlineStatus;
import eu.the5zig.mod.chat.entity.Profile;
import eu.the5zig.mod.chat.entity.Rank;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.filetransfer.FileTransferManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus.FriendStatus;
import eu.the5zig.mod.gui.ingame.IGui2ndChat;
import eu.the5zig.mod.listener.CrossHairDistanceListener;
import eu.the5zig.mod.listener.InventoryListener;
import eu.the5zig.mod.render.ChatSymbolsRenderer;
import eu.the5zig.mod.render.SnowRenderer;
import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.PreciseCounter;
import eu.the5zig.mod.util.TabList;
import eu.the5zig.mod.util.Vector2i;
import eu.the5zig.util.Utils;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.UUID;

public class DataManager
{
  public TabList tabList;
  private Server server;
  private Profile profile;
  private String session = The5zigMod.getVars().getSession();
  private String username = The5zigMod.getVars().getUsername();
  private String uuid = The5zigMod.getVars().getUUID();
  private Vector2i coordinatesClipboard;
  private NetworkStats networkStats;
  private ChatTypingManager chatTypingManager = new ChatTypingManager();
  private AFKManager afkManager = new AFKManager();
  private ChatBackgroundManager chatBackgroundManager = new ChatBackgroundManager();
  private FPSCalculator fpsCalculator = new FPSCalculator();
  private PreciseCounter cpsCalculator = new PreciseCounter();
  private SpeedCalculator speedCalculator = new SpeedCalculator();
  private InventoryListener inventoryListener = new InventoryListener();
  private PingManager pingManager = new PingManager();
  private FileTransferManager fileTransferManager = new FileTransferManager();
  private AutoReconnectManager autoReconnectManager = new AutoReconnectManager();
  private SearchManager searchManager = new SearchManager();
  private CrossHairDistanceListener crossHairDistanceListener = new CrossHairDistanceListener();
  private WeatherManager weatherManager = new WeatherManager();
  private SnowRenderer snowRenderer = new SnowRenderer();
  
  public DataManager()
  {
    this.profile = new Profile(0, Rank.NONE, System.currentTimeMillis(), "Hey there, I'm using The 5zig Mod!", Friend.OnlineStatus.ONLINE, true, true, true, false, true);
    ChatSymbolsRenderer.load();
  }
  
  public UUID getUniqueId()
  {
    return Utils.getUUID(this.uuid);
  }
  
  public String getColoredName()
  {
    return this.profile.getRank().getColorCode() + this.username;
  }
  
  public Server getServer()
  {
    return this.server;
  }
  
  public void setServer(Server server)
  {
    this.server = server;
  }
  
  public String getSession()
  {
    return this.session;
  }
  
  public String getUsername()
  {
    return this.username;
  }
  
  public String getUniqueIdWithoutDashes()
  {
    return this.uuid;
  }
  
  public void resetServer()
  {
    this.server = null;
    this.tabList = null;
    this.pingManager.reset();
    The5zigMod.getServerAPIBackend().reset();
    The5zigMod.getVars().resetServer();
    The5zigMod.getVars().get2ndChat().clear();
  }
  
  public Profile getProfile()
  {
    return this.profile;
  }
  
  public void setProfile(Profile profile)
  {
    this.profile = profile;
  }
  
  public Vector2i getCoordinatesClipboard()
  {
    return this.coordinatesClipboard;
  }
  
  public void setCoordinatesClipboard(Vector2i coordinatesClipboard)
  {
    this.coordinatesClipboard = coordinatesClipboard;
  }
  
  public void initNetworkStats()
  {
    if (this.networkStats != null) {
      return;
    }
    this.networkStats = new NetworkStats();
  }
  
  public NetworkStats getNetworkStats()
  {
    return this.networkStats;
  }
  
  public ChatTypingManager getChatTypingManager()
  {
    return this.chatTypingManager;
  }
  
  public AFKManager getAfkManager()
  {
    return this.afkManager;
  }
  
  public ChatBackgroundManager getChatBackgroundManager()
  {
    return this.chatBackgroundManager;
  }
  
  public InventoryListener getInventoryListener()
  {
    return this.inventoryListener;
  }
  
  public PingManager getPingManager()
  {
    return this.pingManager;
  }
  
  public FPSCalculator getFpsCalculator()
  {
    return this.fpsCalculator;
  }
  
  public PreciseCounter getCpsCalculator()
  {
    return this.cpsCalculator;
  }
  
  public SpeedCalculator getSpeedCalculator()
  {
    return this.speedCalculator;
  }
  
  public FileTransferManager getFileTransferManager()
  {
    return this.fileTransferManager;
  }
  
  public AutoReconnectManager getAutoReconnectManager()
  {
    return this.autoReconnectManager;
  }
  
  public SearchManager getSearchManager()
  {
    return this.searchManager;
  }
  
  public CrossHairDistanceListener getCrossHairDistanceListener()
  {
    return this.crossHairDistanceListener;
  }
  
  public WeatherManager getWeatherManager()
  {
    return this.weatherManager;
  }
  
  public SnowRenderer getSnowRenderer()
  {
    return this.snowRenderer;
  }
  
  public void updateCurrentLobby()
  {
    Server server = The5zigMod.getDataManager().getServer();
    if ((The5zigMod.getDataManager().getServer() != null) && (The5zigMod.getDataManager().getProfile().isShowServer()))
    {
      The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.SERVER, 
        The5zigMod.getDataManager().getServer().getHost() + ":" + The5zigMod.getDataManager().getServer().getPort()), new GenericFutureListener[0]);
      if ((server instanceof GameServer)) {
        The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.LOBBY, ((GameServer)server).getLobbyString()), new GenericFutureListener[0]);
      }
    }
  }
}
