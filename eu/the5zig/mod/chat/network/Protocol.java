package eu.the5zig.mod.chat.network;

import com.google.common.collect.Maps;
import eu.the5zig.mod.chat.network.packets.Packet;
import eu.the5zig.mod.chat.network.packets.PacketAddBlockedUser;
import eu.the5zig.mod.chat.network.packets.PacketAnnouncement;
import eu.the5zig.mod.chat.network.packets.PacketAnnouncementList;
import eu.the5zig.mod.chat.network.packets.PacketBanned;
import eu.the5zig.mod.chat.network.packets.PacketBlockedUserList;
import eu.the5zig.mod.chat.network.packets.PacketCapeSettings;
import eu.the5zig.mod.chat.network.packets.PacketClientStats;
import eu.the5zig.mod.chat.network.packets.PacketCompression;
import eu.the5zig.mod.chat.network.packets.PacketCreateGroupChat;
import eu.the5zig.mod.chat.network.packets.PacketDeleteBlockedUser;
import eu.the5zig.mod.chat.network.packets.PacketDeleteFriend;
import eu.the5zig.mod.chat.network.packets.PacketDeleteGroupChat;
import eu.the5zig.mod.chat.network.packets.PacketDisconnect;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferAbort;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferChunk;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferId;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferRequest;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferResponse;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferStart;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferStartResponse;
import eu.the5zig.mod.chat.network.packets.PacketFriendList;
import eu.the5zig.mod.chat.network.packets.PacketFriendRequest;
import eu.the5zig.mod.chat.network.packets.PacketFriendRequestList;
import eu.the5zig.mod.chat.network.packets.PacketFriendRequestResponse;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketGroupBroadcast;
import eu.the5zig.mod.chat.network.packets.PacketGroupChatList;
import eu.the5zig.mod.chat.network.packets.PacketGroupChatMessage;
import eu.the5zig.mod.chat.network.packets.PacketGroupChatMessageStatusSent;
import eu.the5zig.mod.chat.network.packets.PacketGroupChatStatus;
import eu.the5zig.mod.chat.network.packets.PacketHandshake;
import eu.the5zig.mod.chat.network.packets.PacketHeartbeat;
import eu.the5zig.mod.chat.network.packets.PacketLeaveGroupChat;
import eu.the5zig.mod.chat.network.packets.PacketLogin;
import eu.the5zig.mod.chat.network.packets.PacketMessageFriend;
import eu.the5zig.mod.chat.network.packets.PacketMessageFriendStatus;
import eu.the5zig.mod.chat.network.packets.PacketNewFriend;
import eu.the5zig.mod.chat.network.packets.PacketNewFriendRequest;
import eu.the5zig.mod.chat.network.packets.PacketOverlay;
import eu.the5zig.mod.chat.network.packets.PacketProfile;
import eu.the5zig.mod.chat.network.packets.PacketServerStats;
import eu.the5zig.mod.chat.network.packets.PacketStartLogin;
import eu.the5zig.mod.chat.network.packets.PacketTyping;
import eu.the5zig.mod.chat.network.packets.PacketWelcome;
import java.util.Map;
import java.util.Map.Entry;

public class Protocol
{
  public static final int VERSION = 2;
  private Map<Integer, Class> packets = Maps.newHashMap();
  private Map<Class, ConnectionState> protocolMap = Maps.newHashMap();
  
  public Protocol()
  {
    register(0, PacketHandshake.class, ConnectionState.HANDSHAKE);
    register(1, PacketStartLogin.class, ConnectionState.LOGIN);
    register(2, PacketLogin.class, ConnectionState.LOGIN);
    register(3, PacketWelcome.class, ConnectionState.PLAY);
    register(4, PacketOverlay.class, ConnectionState.ALL);
    register(5, PacketHeartbeat.class, ConnectionState.ALL);
    register(6, PacketBanned.class, ConnectionState.ALL);
    register(7, PacketCompression.class, ConnectionState.PLAY);
    register(9, PacketDisconnect.class, ConnectionState.ALL);
    
    register(160, PacketClientStats.class, ConnectionState.PLAY);
    register(161, PacketServerStats.class, ConnectionState.PLAY);
    register(162, PacketProfile.class, ConnectionState.PLAY);
    register(163, PacketAnnouncement.class, ConnectionState.PLAY);
    register(164, PacketAnnouncementList.class, ConnectionState.PLAY);
    register(165, PacketCapeSettings.class, ConnectionState.PLAY);
    
    register(16, PacketFriendList.class, ConnectionState.PLAY);
    register(17, PacketMessageFriend.class, ConnectionState.PLAY);
    register(18, PacketFriendStatus.class, ConnectionState.PLAY);
    register(20, PacketMessageFriendStatus.class, ConnectionState.PLAY);
    
    register(32, PacketFriendRequest.class, ConnectionState.PLAY);
    register(33, PacketFriendRequestList.class, ConnectionState.PLAY);
    register(34, PacketFriendRequestResponse.class, ConnectionState.PLAY);
    register(35, PacketNewFriend.class, ConnectionState.PLAY);
    register(36, PacketNewFriendRequest.class, ConnectionState.PLAY);
    register(37, PacketDeleteFriend.class, ConnectionState.PLAY);
    register(38, PacketTyping.class, ConnectionState.PLAY);
    
    register(48, PacketAddBlockedUser.class, ConnectionState.PLAY);
    register(49, PacketDeleteBlockedUser.class, ConnectionState.PLAY);
    register(50, PacketBlockedUserList.class, ConnectionState.PLAY);
    
    register(64, PacketCreateGroupChat.class, ConnectionState.PLAY);
    register(65, PacketGroupChatMessage.class, ConnectionState.PLAY);
    register(66, PacketGroupChatList.class, ConnectionState.PLAY);
    register(67, PacketLeaveGroupChat.class, ConnectionState.PLAY);
    register(68, PacketDeleteGroupChat.class, ConnectionState.PLAY);
    register(69, PacketGroupChatMessageStatusSent.class, ConnectionState.PLAY);
    register(70, PacketGroupChatStatus.class, ConnectionState.PLAY);
    register(71, PacketGroupBroadcast.class, ConnectionState.PLAY);
    
    register(80, PacketFileTransferRequest.class, ConnectionState.PLAY);
    register(81, PacketFileTransferResponse.class, ConnectionState.PLAY);
    register(82, PacketFileTransferId.class, ConnectionState.PLAY);
    register(83, PacketFileTransferStart.class, ConnectionState.PLAY);
    register(84, PacketFileTransferStartResponse.class, ConnectionState.PLAY);
    register(85, PacketFileTransferChunk.class, ConnectionState.PLAY);
    register(89, PacketFileTransferAbort.class, ConnectionState.PLAY);
  }
  
  private void register(int id, Class packet, ConnectionState state)
  {
    if (this.packets.containsKey(Integer.valueOf(id))) {
      throw new RuntimeException("Packet with id " + id + " is already registered!");
    }
    try
    {
      packet.newInstance();
    }
    catch (Exception e)
    {
      throw new RuntimeException("Packet with id " + id + " has no default constructor!");
    }
    this.packets.put(Integer.valueOf(id), packet);
    this.protocolMap.put(packet, state);
  }
  
  public int getPacketId(Packet packet)
  {
    for (Map.Entry<Integer, Class> entry : this.packets.entrySet())
    {
      Class c = (Class)entry.getValue();
      if (c.isInstance(packet)) {
        return ((Integer)entry.getKey()).intValue();
      }
    }
    throw new RuntimeException("Packet " + packet + " is not registered!");
  }
  
  public Packet getPacket(int id)
    throws IllegalAccessException, InstantiationException
  {
    if (!this.packets.containsKey(Integer.valueOf(id))) {
      throw new RuntimeException("Could not get unregistered packet (" + id + ")!");
    }
    return (Packet)((Class)this.packets.get(Integer.valueOf(id))).newInstance();
  }
  
  public ConnectionState getProtocol(int id)
    throws InstantiationException, IllegalAccessException
  {
    for (Map.Entry<Class, ConnectionState> entry : this.protocolMap.entrySet()) {
      if (((Class)entry.getKey()).equals(getPacket(id).getClass())) {
        return (ConnectionState)entry.getValue();
      }
    }
    throw new RuntimeException("Packet " + id + " is not registered!");
  }
}
