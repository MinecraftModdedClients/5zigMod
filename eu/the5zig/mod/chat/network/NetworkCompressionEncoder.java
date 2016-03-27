package eu.the5zig.mod.chat.network;

import eu.the5zig.mod.chat.network.packets.PacketBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.zip.Deflater;

public class NetworkCompressionEncoder
  extends MessageToByteEncoder<ByteBuf>
{
  private final byte[] buffer = new byte[' '];
  private final Deflater deflater;
  private int threshold;
  
  public NetworkCompressionEncoder(int threshold)
  {
    this.threshold = threshold;
    this.deflater = new Deflater();
  }
  
  protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out)
  {
    int length = in.readableBytes();
    PacketBuffer packetBuffer = new PacketBuffer(out);
    if (length < this.threshold)
    {
      packetBuffer.writeVarIntToBuffer(0);
      packetBuffer.writeBytes(in);
    }
    else
    {
      byte[] uncompressedData = new byte[length];
      in.readBytes(uncompressedData);
      packetBuffer.writeVarIntToBuffer(uncompressedData.length);
      this.deflater.setInput(uncompressedData, 0, length);
      this.deflater.finish();
      while (!this.deflater.finished())
      {
        int compressedLength = this.deflater.deflate(this.buffer);
        packetBuffer.writeBytes(this.buffer, 0, compressedLength);
      }
      this.deflater.reset();
    }
  }
  
  public void setCompressionTreshold(int treshold)
  {
    this.threshold = treshold;
  }
}
