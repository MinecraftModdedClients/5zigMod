package eu.the5zig.mod.api;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.IOverlay;
import eu.the5zig.mod.listener.Listener;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.util.IVariables;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.Logger;

public class ServerAPIListener
  extends Listener
{
  public void onPayloadReceive(String channel, ByteBuf packetData)
  {
    if (channel.equals("5zig_REG"))
    {
      int version = packetData.readInt();
      
      The5zigMod.getVars().messagePlayer(The5zigMod.getRenderer().getPrefix("The5zigMod") + I18n.translate("api.connected"));
      The5zigMod.getServerAPIBackend().reset();
      PayloadUtils.sendPayload(Unpooled.buffer().writeByte(2));
    }
    else if (channel.equals("5zig"))
    {
      int ordinal = packetData.readInt();
      if ((ordinal < 0) || (ordinal >= PayloadType.values().length))
      {
        The5zigMod.logger.warn("Could not handle Custom Payload on Channel {}. Could not handle received integer.", new Object[] { "5zig" });
        return;
      }
      PayloadType payloadType = PayloadType.values()[ordinal];
      switch (payloadType)
      {
      case UPDATE: 
        String statName = PayloadUtils.readString(packetData, 100);
        String statScore = PayloadUtils.readString(packetData, 100);
        The5zigMod.getServerAPIBackend().updateStat(statName, statScore);
        break;
      case RESET: 
        String statName = PayloadUtils.readString(packetData, 100);
        The5zigMod.getServerAPIBackend().resetStat(statName);
        break;
      case CLEAR: 
        The5zigMod.getServerAPIBackend().reset();
        break;
      case DISPLAY_NAME: 
        String displayName = PayloadUtils.readString(packetData, 150);
        The5zigMod.getServerAPIBackend().setDisplayName(displayName);
        break;
      case LARGE_TEXT: 
        String largeText = PayloadUtils.readString(packetData, 250);
        The5zigMod.getServerAPIBackend().setLargeText(largeText);
        break;
      case RESET_LARGE_TEXT: 
        The5zigMod.getServerAPIBackend().setLargeText(null);
        break;
      case IMAGE: 
        String base64 = PayloadUtils.readString(packetData, 32767);
        int id = packetData.readInt();
        The5zigMod.getServerAPIBackend().setImage(base64, id);
        break;
      case IMAGE_ID: 
        int id = packetData.readInt();
        The5zigMod.getServerAPIBackend().setImage(id);
        break;
      case RESET_IMAGE: 
        The5zigMod.getServerAPIBackend().resetImage();
        break;
      case OVERLAY: 
        The5zigMod.getOverlayMessage().displayMessageAndSplit(PayloadUtils.readString(packetData, 100));
        break;
      case COUNTDOWN: 
        String name = PayloadUtils.readString(packetData, 50);
        long time = packetData.readLong();
        if (time == 0L) {
          The5zigMod.getServerAPIBackend().resetCountdown();
        } else {
          The5zigMod.getServerAPIBackend().startCountdown(name, time);
        }
        break;
      default: 
        The5zigMod.logger.warn("Could not handle custom server payload " + payloadType.toString());
      }
    }
  }
  
  public static enum PayloadType
  {
    UPDATE,  RESET,  CLEAR,  DISPLAY_NAME,  IMAGE,  IMAGE_ID,  RESET_IMAGE,  LARGE_TEXT,  RESET_LARGE_TEXT,  OVERLAY,  COUNTDOWN;
    
    private PayloadType() {}
  }
}
