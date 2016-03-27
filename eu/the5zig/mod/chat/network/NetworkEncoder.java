package eu.the5zig.mod.chat.network;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.NetworkStats;
import eu.the5zig.mod.chat.network.packets.Packet;
import eu.the5zig.mod.chat.network.packets.PacketBuffer;
import eu.the5zig.mod.manager.DataManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.Logger;

public class NetworkEncoder
  extends MessageToByteEncoder<Packet>
{
  private NetworkManager networkManager;
  
  public NetworkEncoder(NetworkManager networkManager)
  {
    this.networkManager = networkManager;
  }
  
  protected void encode(ChannelHandlerContext channelHandlerContext, Packet packet, ByteBuf byteBuf)
    throws Exception
  {
    PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
    packetBuffer.writeVarIntToBuffer(this.networkManager.getProtocol().getPacketId(packet));
    packet.write(packetBuffer);
    The5zigMod.logger.debug(The5zigMod.networkMarker, "OUT| {} ({} bytes)", new Object[] { packet.getClass().getSimpleName(), Integer.valueOf(packetBuffer.readableBytes()) });
    The5zigMod.getDataManager().getNetworkStats().onPacketSend(packetBuffer);
  }
}
