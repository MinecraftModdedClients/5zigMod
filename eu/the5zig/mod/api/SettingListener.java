package eu.the5zig.mod.api;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.listener.Listener;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.util.IVariables;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.Logger;

public class SettingListener
  extends Listener
{
  public void onServerJoin(String host, int port)
  {
    PayloadUtils.sendPayload("REGISTER", PayloadUtils.writeString(Unpooled.buffer(), "5zig_Set"));
    PayloadUtils.sendPayload("5zig_Set", Unpooled.buffer().writeByte(2));
  }
  
  public void onPayloadReceive(String channel, ByteBuf packetData)
  {
    if ((!"5zig_Set".equals(channel)) || (The5zigMod.getDataManager().getServer() == null)) {
      return;
    }
    byte setting = packetData.readByte();
    The5zigMod.logger.info("Received payload on setting channel: " + setting);
    if ((setting & 0x1) != 0)
    {
      The5zigMod.getDataManager().getServer().setRenderPotionEffects(false);
      if (The5zigMod.getConfig().getBool("renderPotionEffects")) {
        message(I18n.translate("api.setting_disabled.potion_hud"));
      }
    }
    else
    {
      The5zigMod.getDataManager().getServer().setRenderPotionEffects(true);
    }
    if ((setting & 0x2) != 0)
    {
      The5zigMod.getDataManager().getServer().setRenderPotionIndicator(false);
      if (The5zigMod.getConfig().getBool("showPotionIndicator")) {
        message(I18n.translate("api.setting_disabled.potion_indicator"));
      }
    }
    else
    {
      The5zigMod.getDataManager().getServer().setRenderPotionIndicator(true);
    }
    if ((setting & 0x4) != 0)
    {
      The5zigMod.getDataManager().getServer().setRenderArmor(false);
      if (The5zigMod.getConfig().getBool("renderArmor")) {
        message(I18n.translate("api.setting_disabled.armor_hud"));
      }
    }
    else
    {
      The5zigMod.getDataManager().getServer().setRenderArmor(true);
    }
    if ((setting & 0x8) != 0)
    {
      The5zigMod.getDataManager().getServer().setRenderSaturation(false);
      if (The5zigMod.getConfig().getBool("showSaturation")) {
        message(I18n.translate("api.setting_disabled.saturation"));
      }
    }
    else
    {
      The5zigMod.getDataManager().getServer().setRenderSaturation(true);
    }
    if ((setting & 0x10) != 0)
    {
      The5zigMod.getDataManager().getServer().setRenderEntityHealth(false);
      if (The5zigMod.getConfig().getBool("showEntityHealth")) {
        message(I18n.translate("api.setting_disabled.entity_health"));
      }
    }
    else
    {
      The5zigMod.getDataManager().getServer().setRenderEntityHealth(true);
    }
  }
  
  private void message(String setting)
  {
    The5zigMod.getVars().messagePlayer(The5zigMod.getRenderer().getPrefix("The 5zig Mod") + I18n.translate("api.setting_disabled", new Object[] { setting }));
  }
}
