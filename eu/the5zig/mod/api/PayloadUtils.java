package eu.the5zig.mod.api;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.util.IVariables;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

public class PayloadUtils
{
  public static final String API_CHANNEL = "5zig";
  public static final String API_CHANNEL_REGISTER = "5zig_REG";
  public static final String SETTING_CHANNEL = "5zig_Set";
  
  public static void sendPayload(String message)
  {
    sendPayload("5zig", message);
  }
  
  public static void sendPayload(ByteBuf buf)
  {
    sendPayload("5zig", buf);
  }
  
  public static void sendPayload(String channel, String message)
  {
    sendPayload(channel, Unpooled.buffer().writeBytes(message.getBytes()));
  }
  
  public static void sendPayload(String channel, ByteBuf buf)
  {
    if (The5zigMod.getVars().hasNetworkManager()) {
      The5zigMod.getVars().sendCustomPayload(channel, buf);
    }
  }
  
  public static ByteBuf writeString(ByteBuf byteBuf, String string)
  {
    byte[] bytes = string.getBytes(org.apache.commons.codec.Charsets.UTF_8);
    byteBuf.writeBytes(bytes);
    return byteBuf;
  }
  
  public static String readString(ByteBuf byteBuf, int maxLength)
  {
    int length = readVarInt(byteBuf);
    if (length > maxLength * 4) {
      throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + length + " > " + maxLength * 4 + ")");
    }
    if (length < 0) {
      throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
    }
    String var2 = new String(byteBuf.readBytes(length).array(), com.google.common.base.Charsets.UTF_8);
    if (var2.length() > maxLength) {
      throw new DecoderException("The received string length is longer than maximum allowed (" + length + " > " + maxLength + ")");
    }
    return var2;
  }
  
  public static int readVarInt(ByteBuf byteBuf)
  {
    int var = 0;
    int var1 = 0;
    byte var2;
    do
    {
      var2 = byteBuf.readByte();
      var |= (var2 & 0x7F) << var1++ * 7;
      if (var1 > 5) {
        throw new RuntimeException("VarInt too big");
      }
    } while ((var2 & 0x80) == 128);
    return var;
  }
}
