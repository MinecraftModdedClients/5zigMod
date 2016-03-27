package eu.the5zig.mod.listener;

import com.google.common.collect.Lists;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.api.ServerAPIListener;
import eu.the5zig.mod.api.SettingListener;
import eu.the5zig.mod.chat.ChatTypingListener;
import eu.the5zig.mod.chat.NetworkTickListener;
import eu.the5zig.mod.chat.entity.Profile;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus.FriendStatus;
import eu.the5zig.mod.gui.ingame.resource.IResourceManager;
import eu.the5zig.mod.manager.ChatFilterManager;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.render.EasterRenderer;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.TabList;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

public class EventListener
{
  private List<IListener> listeners = Lists.newArrayList();
  private String previousTitle;
  private String previousSubTitle;
  private EasterListener easterListener = new EasterListener();
  
  public EventListener()
  {
    register(new ServerAPIListener());
    
    register(new KeybindingListener());
    register(new ToggleScreenListener());
    register(new NetworkTickListener());
    register(new DisplayFocusListener());
    register(new ChatTypingListener());
    register(new ChatFilterManager());
    register(new SettingListener());
    register(new ZoomListener());
    register(new ChatUsernameListener());
    register(The5zigMod.getDataManager().getSearchManager());
    register(The5zigMod.getDataManager().getAfkManager());
    register(The5zigMod.getDataManager().getCrossHairDistanceListener());
    register(this.easterListener);
    
    The5zigMod.logger.debug("Registered {} listeners!", new Object[] { Integer.valueOf(this.listeners.size()) });
  }
  
  public void register(IListener listener)
  {
    this.listeners.add(listener);
  }
  
  public void unregister(IListener listener)
  {
    this.listeners.remove(listener);
  }
  
  public void onServerConnect(String host, int port)
  {
    if ((The5zigMod.getNetworkManager().isConnected()) && (The5zigMod.getDataManager().getProfile().isShowServer())) {
      The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.SERVER, host + ":" + port), new GenericFutureListener[0]);
    }
    for (IListener listener : this.listeners) {
      listener.onServerJoin(host, port);
    }
    for (IListener listener : this.listeners) {
      listener.onServerConnect();
    }
    if (The5zigMod.getDataManager().getServer() == null) {
      The5zigMod.getDataManager().setServer(new Server(host, port));
    }
    if (The5zigMod.getDataManager().tabList != null) {
      onPlayerListHeaderFooter(The5zigMod.getDataManager().tabList);
    }
  }
  
  public void onServerDisconnect()
  {
    The5zigMod.getDataManager().resetServer();
    for (IListener listener : this.listeners) {
      listener.onServerDisconnect();
    }
    if ((The5zigMod.getNetworkManager().isConnected()) && (The5zigMod.getDataManager().getProfile().isShowServer())) {
      The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(PacketFriendStatus.FriendStatus.SERVER, ""), new GenericFutureListener[0]);
    }
  }
  
  public void onRenderOverlay()
  {
    The5zigMod.getVars().renderOverlay();
    this.easterListener.getEasterRenderer().render();
  }
  
  private boolean hadNetworkManager = false;
  
  public void onTick()
  {
    for (IListener listener : this.listeners) {
      listener.onTick();
    }
    if ((this.hadNetworkManager) && (!The5zigMod.getVars().hasNetworkManager())) {
      The5zigMod.getVars().getResourceManager().cleanupTextures();
    }
    this.hadNetworkManager = The5zigMod.getVars().hasNetworkManager();
    if ((The5zigMod.getVars().getServer() != null) && (The5zigMod.getDataManager().getServer() == null) && (!The5zigMod.getVars().isPlayerNull()))
    {
      String ip = The5zigMod.getVars().getServer();
      String host = ip;
      int port = 25565;
      if (host.contains(":"))
      {
        host = ip.split(":")[0];
        try
        {
          port = Integer.parseInt(ip.split(":")[1]);
        }
        catch (NumberFormatException localNumberFormatException) {}
      }
      onServerConnect(host, port);
    }
    else if ((The5zigMod.getDataManager().getServer() != null) && (The5zigMod.getVars().getServer() == null))
    {
      onServerDisconnect();
    }
  }
  
  public void dispatchKeypresses()
  {
    int eventKey = Keyboard.getEventKey();
    int currentcode = eventKey == 0 ? Keyboard.getEventCharacter() : eventKey;
    if ((currentcode == 0) || (Keyboard.isRepeatEvent())) {
      return;
    }
    int keyCode;
    if (Keyboard.getEventKeyState())
    {
      keyCode = currentcode + (eventKey == 0 ? 256 : 0);
      for (IListener listener : this.listeners) {
        listener.onKeyPress(keyCode);
      }
    }
  }
  
  public boolean onServerChat(String message, Object chatComponent)
  {
    if (message == null) {
      return false;
    }
    boolean ignoreMessage = false;
    for (IListener listener : this.listeners) {
      if ((listener.onServerChat(message)) || (listener.onServerChat(message, chatComponent))) {
        ignoreMessage = true;
      }
    }
    return ignoreMessage;
  }
  
  public boolean onActionBar(String message)
  {
    if (message == null) {
      return false;
    }
    boolean ignore = false;
    for (IListener listener : this.listeners) {
      if (listener.onActionBar(message)) {
        ignore = true;
      }
    }
    return ignore;
  }
  
  public void handlePluginMessage(String channel, ByteBuf packetData)
  {
    if (("MC|Brand".equals(channel)) && (The5zigMod.getDataManager().getServer() != null)) {
      for (IListener listener : this.listeners) {
        listener.onServerConnect();
      }
    }
    for (IListener listener : this.listeners) {
      listener.onPayloadReceive(channel, packetData);
    }
  }
  
  public void onPlayerListHeaderFooter(TabList tabList)
  {
    The5zigMod.getDataManager().tabList = tabList;
    
    String headerString = tabList.getHeader().replace(ChatColor.RESET.toString(), "");
    String footerString = tabList.getFooter().replace(ChatColor.RESET.toString(), "");
    for (IListener listener : this.listeners) {
      listener.onPlayerListHeaderFooter(headerString, footerString);
    }
  }
  
  public void onTitle(String title, String subTitle)
  {
    if ((title == null) && (subTitle == null))
    {
      this.previousTitle = null;
      this.previousSubTitle = null;
      return;
    }
    if (title != null) {
      this.previousTitle = title;
    }
    if (subTitle != null) {
      this.previousSubTitle = subTitle;
    }
    if (this.previousTitle != null) {
      for (IListener listener : this.listeners) {
        listener.onTitle(this.previousTitle, this.previousSubTitle);
      }
    }
  }
}
