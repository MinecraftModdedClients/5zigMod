package eu.the5zig.mod.manager;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Friend.OnlineStatus;
import eu.the5zig.mod.chat.entity.Profile;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus.FriendStatus;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.gui.IOverlay;
import eu.the5zig.mod.listener.Listener;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class AFKManager
  extends Listener
{
  private double lastX;
  private double lastY;
  private long afkTime;
  private boolean afk = false;
  private int newMessages;
  private long lastTimeNotMoved = -1L;
  private long lastAfkTime;
  public static final int AFK_COUNTER = 30000;
  
  public AFKManager()
  {
    this.afkTime = System.currentTimeMillis();
  }
  
  public void onTick()
  {
    long goAfkAfter = The5zigMod.getConfig().getInt("afkTime") * 1000 * 60;
    if ((Mouse.getX() != this.lastX) || (Mouse.getY() != this.lastY) || (Keyboard.getEventKeyState()))
    {
      if (this.afk)
      {
        The5zigMod.logger.info("No longer AFK!");
        The5zigMod.getOverlayMessage().displayMessage(I18n.translate("profile.no_longer_afk"));
        if (this.newMessages > 0) {
          The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.YELLOW + I18n.translate("conn.unread_messages", new Object[] { Integer.valueOf(this.newMessages) }));
        }
        if (The5zigMod.getDataManager().getProfile().getOnlineStatus() == Friend.OnlineStatus.ONLINE) {
          The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.ONLINE_STATUS, Friend.OnlineStatus.ONLINE), new GenericFutureListener[0]);
        }
      }
      if (System.currentTimeMillis() - this.afkTime > 30000L)
      {
        this.lastTimeNotMoved = System.currentTimeMillis();
        this.lastAfkTime = (System.currentTimeMillis() - this.afkTime);
      }
      this.afkTime = System.currentTimeMillis();
      this.afk = false;
      this.newMessages = 0;
    }
    if ((goAfkAfter > 0L) && (!this.afk) && (System.currentTimeMillis() - this.afkTime > goAfkAfter))
    {
      The5zigMod.logger.info("Now AFK!");
      The5zigMod.getOverlayMessage().displayMessage(I18n.translate("profile.now_afk"));
      if (The5zigMod.getDataManager().getProfile().getOnlineStatus() == Friend.OnlineStatus.ONLINE) {
        The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.ONLINE_STATUS, Friend.OnlineStatus.AWAY), new GenericFutureListener[0]);
      }
      this.afk = true;
    }
    if (System.currentTimeMillis() - this.lastTimeNotMoved > 3000L)
    {
      this.lastTimeNotMoved = 0L;
      this.lastAfkTime = 0L;
    }
    this.lastX = Mouse.getX();
    this.lastY = Mouse.getY();
  }
  
  public void addNewMessage()
  {
    this.newMessages += 1;
  }
  
  public boolean isAfk()
  {
    return this.afk;
  }
  
  public long getAFKTime()
  {
    return System.currentTimeMillis() - this.afkTime;
  }
  
  public long getLastAfkTime()
  {
    return this.lastAfkTime;
  }
}
