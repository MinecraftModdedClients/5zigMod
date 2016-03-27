package eu.the5zig.mod.chat.network;

import eu.the5zig.mod.chat.network.packets.PacketBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NetworkSplitter
  extends MessageToByteEncoder<ByteBuf>
{
  protected void encode(ChannelHandlerContext ctx, ByteBuf buffer, ByteBuf byteBuf)
  {
    int var4 = buffer.readableBytes();
    int var5 = PacketBuffer.getVarIntSize(var4);
    if (var5 > 3) {
      throw new IllegalArgumentException("unable to fit " + var4 + " into " + 3);
    }
    PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
    packetBuffer.ensureWritable(var5 + var4);
    packetBuffer.writeVarIntToBuffer(var4);
    packetBuffer.writeBytes(buffer, buffer.readerIndex(), var4);
  }
}
