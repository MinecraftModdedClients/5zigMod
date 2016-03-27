package eu.the5zig.mod.chat.network;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.NetworkStats;
import eu.the5zig.mod.chat.network.packets.Packet;
import eu.the5zig.mod.chat.network.packets.PacketBuffer;
import eu.the5zig.mod.manager.DataManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.Logger;

public class NetworkDecoder
  extends ByteToMessageDecoder
{
  private NetworkManager networkManager;
  
  public NetworkDecoder(NetworkManager networkManager)
  {
    this.networkManager = networkManager;
  }
  
  protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> objects)
    throws Exception
  {
    PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
    The5zigMod.getDataManager().getNetworkStats().onPacketReceive(packetBuffer);
    if (packetBuffer.readableBytes() < 1) {
      return;
    }
    int id = packetBuffer.readVarIntFromBuffer();
    Packet packet = this.networkManager.getProtocol().getPacket(id);
    The5zigMod.logger.debug(The5zigMod.networkMarker, "IN | {} ({} bytes)", new Object[] { packet.getClass().getSimpleName(), Integer.valueOf(packetBuffer.readableBytes() + 1) });
    packet.read(packetBuffer);
    if (packetBuffer.readableBytes() > 0) {
      throw new IOException("Packet  (" + packet.getClass().getSimpleName() + ") was larger than I expected, " + "found " + packetBuffer.readableBytes() + " bytes extra whilst reading packet " + packet);
    }
    objects.add(packet);
  }
}
