package eu.the5zig.mod.chat.network;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.NetworkStats;
import eu.the5zig.mod.chat.entity.Profile;
import eu.the5zig.mod.chat.network.packets.Packet;
import eu.the5zig.mod.chat.network.packets.PacketHandshake;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.gui.IOverlay;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.PrintStream;
import java.nio.channels.UnresolvedAddressException;
import java.util.Queue;
import java.util.Random;
import org.apache.logging.log4j.Logger;

public class NetworkManager
  extends SimpleChannelInboundHandler<Packet>
{
  private static final String HOST = The5zigMod.DEBUG ? "localhost" : "5zig.eu";
  private static final int PORT = 28499;
  public static NioEventLoopGroup CLIENT_NIO_EVENTLOOP;
  private static final int reconnectAdd = The5zigMod.random.nextInt(30);
  private static int reconnectTries;
  private static final int MAX_RECONNECT_TIME = 400;
  private final Protocol protocol;
  private final Queue<QueuedPacket> outboundPacketsQueue = Queues.newConcurrentLinkedQueue();
  private Channel channel;
  private boolean disconnected = false;
  private boolean reconnecting = false;
  private String disconnectReason = I18n.translate("connection.closed");
  private ConnectionState connectionState;
  private HeartbeatManager heartbeatManager;
  
  private NetworkManager()
  {
    The5zigMod.logger.debug("Setting up Protocol version {}", new Object[] { Integer.valueOf(2) });
    this.protocol = new Protocol();
  }
  
  public static NetworkManager connect()
  {
    NetworkManager networkManager = new NetworkManager();
    if (The5zigMod.getConfig().getBool("connectToServer")) {
      networkManager.initConnection();
    }
    return networkManager;
  }
  
  private void initConnection()
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        if (NetworkManager.CLIENT_NIO_EVENTLOOP == null) {
          try
          {
            NetworkManager.CLIENT_NIO_EVENTLOOP = new NioEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("5zig Netty Client #%d").setDaemon(true).build());
          }
          catch (Throwable throwable)
          {
            The5zigMod.logger.error("Could not initialize Nio Event Loop Group! Possible Firewall or Antivirus-Software might be blocking outgoing connections!", throwable);
            if (The5zigMod.getConfig().getBool("showConnecting")) {
              The5zigMod.getOverlayMessage().displayMessage("The 5zig Mod", I18n.translate("connection.error"));
            }
            return;
          }
        }
        Bootstrap bootstrap = (Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group(NetworkManager.CLIENT_NIO_EVENTLOOP)).handler(new ChannelInitializer()
        {
          protected void initChannel(Channel channel)
          {
            channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30)).addLast("splitter", new NetworkPrepender()).addLast("decoder", new NetworkDecoder(NetworkManager.this)).addLast("prepender", new NetworkSplitter()).addLast("encoder", new NetworkEncoder(NetworkManager.this)).addLast(new ChannelHandler[] { NetworkManager.this });
          }
        }
        
          )).channel(NioSocketChannel.class);
        try
        {
          The5zigMod.logger.info("Connecting to {}:{}", new Object[] { NetworkManager.HOST, Integer.valueOf(28499) });
          bootstrap.connect(NetworkManager.HOST, 28499).syncUninterruptibly();
          NetworkManager.this.setConnectState(ConnectionState.HANDSHAKE);
          NetworkManager.this.sendPacket(new PacketHandshake(), new GenericFutureListener[0]);
        }
        catch (UnresolvedAddressException e)
        {
          The5zigMod.logger.error("Could not resolve hostname " + NetworkManager.HOST, e);
          if (The5zigMod.getConfig().getBool("showConnecting")) {
            The5zigMod.getOverlayMessage().displayMessage(I18n.translate("connection.error"));
          }
          NetworkManager.this.reconnect(The5zigMod.DEBUG ? 15 : 60);
        }
        catch (Throwable e)
        {
          The5zigMod.logger.error("An Exception occurred while connecting to " + NetworkManager.HOST + ":" + 28499, e);
          if (The5zigMod.getConfig().getBool("showConnecting")) {
            The5zigMod.getOverlayMessage().displayMessage("The 5zig Mod", I18n.translate("connection.error"));
          }
          NetworkManager.this.reconnect(The5zigMod.DEBUG ? 10 : 60);
        }
      }
    })
    
      .start();
  }
  
  protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet packet)
    throws Exception
  {
    if (isChannelOpen()) {
      packet.handle();
    }
  }
  
  public void channelInactive(ChannelHandlerContext ctx)
    throws Exception
  {
    disconnect();
  }
  
  public void channelActive(ChannelHandlerContext channelHandlerContext)
    throws Exception
  {
    super.channelActive(channelHandlerContext);
    this.channel = channelHandlerContext.channel();
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    The5zigMod.logger.error("An Internal Exception occurred", cause);
    if ((cause instanceof ReadTimeoutException)) {
      disconnect(I18n.translate("connection.timed_out"));
    } else {
      disconnect(I18n.translate("connection.internal_error"));
    }
  }
  
  public void disconnect()
  {
    disconnect(I18n.translate("connection.closed"));
  }
  
  public void disconnect(String disconnectReason)
  {
    if (isChannelOpen())
    {
      closeChannel();
      this.disconnectReason = disconnectReason;
    }
  }
  
  public boolean checkDisconnected()
  {
    if ((!hasNoChannel()) && (!isChannelOpen()) && (!this.disconnected))
    {
      this.disconnected = true;
      The5zigMod.logger.info("Disconnected: " + this.disconnectReason);
      if (The5zigMod.getConfig().getBool("showConnecting")) {
        The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.YELLOW + I18n.translate("connection.disconnected", new Object[] { this.disconnectReason }));
      }
      The5zigMod.getDataManager().getNetworkStats().resetCurrent();
      
      reconnect();
      return true;
    }
    return false;
  }
  
  private void reconnect()
  {
    reconnect(The5zigMod.DEBUG ? 10 : 30);
  }
  
  private void reconnect(int time)
  {
    if (this.reconnecting) {
      return;
    }
    this.reconnecting = true;
    final int seconds = reconnectAdd + (int)(400.0D - (400 - time) * Math.pow(2.718281828459045D, -0.1D * reconnectTries));
    reconnectTries += 1;
    The5zigMod.logger.info("Reconnecting in {} seconds...", new Object[] { Integer.valueOf(seconds) });
    new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          Thread.sleep(seconds * 1000);
        }
        catch (InterruptedException e)
        {
          e.printStackTrace();
          return;
        }
        if ((The5zigMod.getNetworkManager() != null) && (The5zigMod.getNetworkManager().isConnected())) {
          return;
        }
        The5zigMod.newNetworkManager();
      }
    })
    
      .start();
  }
  
  public void tick()
  {
    flushOutboundQueue();
    if (isChannelOpen()) {
      this.channel.flush();
    }
    checkDisconnected();
  }
  
  private void flushOutboundQueue()
  {
    if (isChannelOpen()) {
      while (!this.outboundPacketsQueue.isEmpty())
      {
        QueuedPacket packet = (QueuedPacket)this.outboundPacketsQueue.poll();
        dispatchPacket(packet.packet, packet.listeners);
      }
    }
  }
  
  public void sendPacket(Packet packet, GenericFutureListener... listeners)
  {
    if (isChannelOpen())
    {
      flushOutboundQueue();
      dispatchPacket(packet, listeners);
    }
    else
    {
      this.outboundPacketsQueue.add(new QueuedPacket(packet, listeners));
    }
  }
  
  private void dispatchPacket(final Packet packet, final GenericFutureListener[] listeners)
  {
    if (checkDisconnected()) {
      return;
    }
    try
    {
      if ((this.protocol.getProtocol(getProtocol().getPacketId(packet)) != this.connectionState) && (this.protocol.getProtocol(getProtocol().getPacketId(packet)) != ConnectionState.ALL))
      {
        System.err.printf("Tried to send packet %s in wrong connection state (excpected %s, given %s)! Preventing disconnect!", new Object[] { packet.getClass().getSimpleName(), this.connectionState, 
          getProtocol().getProtocol(getProtocol().getPacketId(packet)) });
        return;
      }
    }
    catch (InstantiationException e)
    {
      e.printStackTrace();
    }
    catch (IllegalAccessException e)
    {
      e.printStackTrace();
    }
    if (this.channel.eventLoop().inEventLoop())
    {
      ChannelFuture future = this.channel.writeAndFlush(packet);
      if (listeners != null) {
        future.addListeners(listeners);
      }
      future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
    else
    {
      this.channel.eventLoop().execute(new Runnable()
      {
        public void run()
        {
          ChannelFuture future = NetworkManager.this.channel.writeAndFlush(packet);
          if (listeners != null) {
            future.addListeners(listeners);
          }
          future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
      });
    }
  }
  
  public boolean isChannelOpen()
  {
    return (this.channel != null) && (this.channel.isOpen());
  }
  
  public boolean hasNoChannel()
  {
    return this.channel == null;
  }
  
  public void closeChannel()
  {
    if (this.channel.isOpen()) {
      this.channel.close().awaitUninterruptibly();
    }
  }
  
  public void setThreshold(int treshold)
  {
    if (treshold >= 0)
    {
      if ((this.channel.pipeline().get("decompress") instanceof NetworkCompressionDecoder)) {
        ((NetworkCompressionDecoder)this.channel.pipeline().get("decompress")).setCompressionTreshold(treshold);
      } else {
        this.channel.pipeline().addBefore("decoder", "decompress", new NetworkCompressionDecoder(treshold));
      }
      if ((this.channel.pipeline().get("compress") instanceof NetworkCompressionEncoder)) {
        ((NetworkCompressionEncoder)this.channel.pipeline().get("decompress")).setCompressionTreshold(treshold);
      } else {
        this.channel.pipeline().addBefore("encoder", "compress", new NetworkCompressionEncoder(treshold));
      }
    }
    else
    {
      if ((this.channel.pipeline().get("decompress") instanceof NetworkCompressionDecoder)) {
        this.channel.pipeline().remove("decompress");
      }
      if ((this.channel.pipeline().get("compress") instanceof NetworkCompressionEncoder)) {
        this.channel.pipeline().remove("compress");
      }
    }
  }
  
  public boolean isConnected()
  {
    return (isChannelOpen()) && (this.connectionState == ConnectionState.PLAY);
  }
  
  public Protocol getProtocol()
  {
    return this.protocol;
  }
  
  public HeartbeatManager getHeartbeatManager()
  {
    return this.heartbeatManager;
  }
  
  public ConnectionState getConnectionState()
  {
    return this.connectionState;
  }
  
  public void setConnectState(ConnectionState connectionState)
  {
    switch (connectionState)
    {
    case HANDSHAKE: 
      this.connectionState = ConnectionState.HANDSHAKE;
      The5zigMod.logger.debug("Handshaking...");
      if (The5zigMod.getConfig().getBool("showConnecting")) {
        The5zigMod.getOverlayMessage().displayMessage("The 5zig Mod", I18n.translate("connection.connecting"));
      }
      break;
    case LOGIN: 
      this.connectionState = ConnectionState.LOGIN;
      The5zigMod.logger.debug("Logging in...");
      if (The5zigMod.getConfig().getBool("showConnecting")) {
        The5zigMod.getOverlayMessage().displayMessage("The 5zig Mod", I18n.translate("connection.logging_in"));
      }
      break;
    case PLAY: 
      this.connectionState = ConnectionState.PLAY;
      this.heartbeatManager = new HeartbeatManager();
      The5zigMod.logger.info("Connected after {} tries!", new Object[] { Integer.valueOf(reconnectTries) });
      reconnectTries = 0;
      if (The5zigMod.getConfig().getBool("showConnecting")) {
        The5zigMod.getOverlayMessage().displayMessage("The 5zig Mod", I18n.translate("connection.connected"));
      }
      if (The5zigMod.getDataManager().getProfile().isShowServer()) {
        The5zigMod.getDataManager().updateCurrentLobby();
      }
      break;
    case DISCONNECT: 
      this.connectionState = ConnectionState.DISCONNECT;
      if (The5zigMod.getConfig().getBool("showConnecting")) {
        The5zigMod.getOverlayMessage().displayMessage("The 5zig Mod", this.disconnectReason);
      }
      break;
    }
  }
  
  private class QueuedPacket
  {
    private Packet packet;
    private GenericFutureListener[] listeners;
    
    public QueuedPacket(Packet packet, GenericFutureListener[] listeners)
    {
      this.packet = packet;
      this.listeners = listeners;
    }
  }
}
