package eu.the5zig.mod.chat.entity;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.ConfigNew.FriendSortation;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.Locale;
import java.util.UUID;

public class Friend
  implements Row, Comparable<Friend>
{
  private final UUID uuid;
  private String name;
  private long firstOnline;
  private String statusMessage;
  private OnlineStatus status;
  private String server;
  private String lobby;
  private long lastOnline;
  private Rank rank;
  private boolean favorite;
  private String modVersion;
  private Locale locale;
  
  public Friend(String username, UUID uuid)
  {
    this.name = username;
    this.uuid = uuid;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  public String getDisplayName()
  {
    return getRank().getColorCode() + getName();
  }
  
  public UUID getUniqueId()
  {
    return this.uuid;
  }
  
  public String getStatusMessage()
  {
    return this.statusMessage;
  }
  
  public void setStatusMessage(String status)
  {
    this.statusMessage = status;
  }
  
  public OnlineStatus getStatus()
  {
    return The5zigMod.getNetworkManager().isConnected() ? this.status : OnlineStatus.OFFLINE;
  }
  
  public void setStatus(OnlineStatus status)
  {
    this.status = status;
  }
  
  public String getServer()
  {
    return this.server;
  }
  
  public void setServer(String server)
  {
    this.server = ((server == null) || (server.isEmpty()) ? null : server);
    this.lobby = null;
  }
  
  public String getLobby()
  {
    return this.lobby;
  }
  
  public void setLobby(String lobby)
  {
    this.lobby = ((lobby == null) || (lobby.isEmpty()) ? null : lobby);
  }
  
  public String getLastOnline()
  {
    return this.lastOnline == -1L ? I18n.translate("profile.hidden") : Utils.convertToDate(this.lastOnline).replace("Today", I18n.translate("profile.today")).replace("Yesterday", 
      I18n.translate("profile.yesterday"));
  }
  
  public void setLastOnline(long lastOnline)
  {
    this.lastOnline = lastOnline;
  }
  
  public String getFirstOnline()
  {
    return this.firstOnline == -1L ? I18n.translate("profile.hidden") : Utils.convertToDate(this.firstOnline).replace("Today", I18n.translate("profile.today")).replace("Yesterday", 
      I18n.translate("profile.yesterday"));
  }
  
  public void setFirstOnline(long firstOnline)
  {
    this.firstOnline = firstOnline;
  }
  
  public Rank getRank()
  {
    return this.rank;
  }
  
  public void setRank(Rank rank)
  {
    this.rank = rank;
  }
  
  public boolean isFavorite()
  {
    return this.favorite;
  }
  
  public void setFavorite(boolean favorite)
  {
    this.favorite = favorite;
  }
  
  public String getModVersion()
  {
    return this.modVersion;
  }
  
  public void setModVersion(String modVersion)
  {
    this.modVersion = modVersion;
  }
  
  public Locale getLocale()
  {
    return this.locale;
  }
  
  public void setLocale(Locale locale)
  {
    this.locale = locale;
  }
  
  public int getLineHeight()
  {
    return 18;
  }
  
  public void draw(int x, int y)
  {
    int maxWidth = 80;
    if (isFavorite())
    {
      maxWidth = 65;
      GLUtil.color(1.0F, 1.0F, 1.0F);
      GLUtil.enableBlend();
      The5zigMod.getVars().bindTexture(The5zigMod.ITEMS);
      Gui.drawModalRectWithCustomSizedTexture(x + 79, y + 1, 0.0F, 0.0F, 12, 12, 96.0F, 96.0F);
      GLUtil.disableBlend();
    }
    String toDraw = getRank().getColorCode();
    if (The5zigMod.getConfig().getEnum("friendSortation", ConfigNew.FriendSortation.class) == ConfigNew.FriendSortation.STATUS) {
      toDraw = toDraw + (getStatus() != OnlineStatus.OFFLINE ? ChatColor.ITALIC : "");
    }
    toDraw = toDraw + getName();
    toDraw = The5zigMod.getVars().shortenToWidth(toDraw, maxWidth);
    The5zigMod.getVars().drawString(toDraw, x + 2, y + 2);
  }
  
  public boolean equals(Object obj)
  {
    if (!(obj instanceof Friend)) {
      return false;
    }
    Friend friend = (Friend)obj;
    return friend.getName().equals(getName());
  }
  
  public String toString()
  {
    return "Friend{name='" + this.name + '\'' + ", uuid=" + this.uuid + '}';
  }
  
  public int compareTo(Friend friend)
  {
    if ((friend.isFavorite()) && (!isFavorite())) {
      return 1;
    }
    if ((isFavorite()) && (!friend.isFavorite())) {
      return -1;
    }
    if ((The5zigMod.getConfig().getEnum("friendSortation", ConfigNew.FriendSortation.class) == ConfigNew.FriendSortation.STATUS) && (
      (getStatus() != OnlineStatus.OFFLINE) || (friend.getStatus() != OnlineStatus.OFFLINE))) {
      return getStatus().compareTo(friend.getStatus());
    }
    return getName().toLowerCase().compareTo(friend.getName().toLowerCase());
  }
  
  public static enum OnlineStatus
  {
    ONLINE("connection.online", ChatColor.GREEN),  AWAY("connection.away", ChatColor.YELLOW),  OFFLINE("connection.offline", ChatColor.RED);
    
    private String name;
    private ChatColor color;
    
    private OnlineStatus(String name, ChatColor color)
    {
      this.name = name;
      this.color = color;
    }
    
    public OnlineStatus getNext()
    {
      return values()[((ordinal() + 1) % values().length)];
    }
    
    public String getDisplayName()
    {
      return this.color + getName();
    }
    
    public String getName()
    {
      return I18n.translate(this.name);
    }
  }
}
